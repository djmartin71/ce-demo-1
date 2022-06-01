package com.cetracker;

import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.serialization.jackson.JacksonObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.PathMatchers.*;


import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class SensorRoutes {

  private final ClusterSharding sharding;
  private final Duration timeout;
  private final ObjectMapper objectMapper;
  private final Unmarshaller<HttpEntity, Tracker.Data> dataUnmarshaller;

  public SensorRoutes(ActorSystem<?> system) {
    sharding = ClusterSharding.get(system);
    timeout = system.settings().config().getDuration("killrweather.routes.ask-timeout");
    // use a pre-configured object mapper from akka-jackson also for HTTP JSON
    // this lets us use the -parameters compiler argument to skip annotating field names on immutable classes
    objectMapper = JacksonObjectMapperProvider.get(system).getOrCreate("jackson-json", Optional.empty());
    dataUnmarshaller = Jackson.unmarshaller(objectMapper, Tracker.Data.class);
  }

  private CompletionStage<Tracker.DataRecorded> recordData(long wsid, Tracker.Data data) {
    EntityRef<Tracker.Command> ref = sharding.entityRefFor(Tracker.TypeKey, Long.toString(wsid));
    return ref.ask(replyTo -> new Tracker.Record(data, System.currentTimeMillis(), replyTo), timeout);
  }

  private CompletionStage<Tracker.QueryResult> query(long wsid, Tracker.DataType dataType, Tracker.Function function) {
    EntityRef<Tracker.Command> ref = sharding.entityRefFor(Tracker.TypeKey, Long.toString(wsid));
    return ref.ask(replyTo -> new Tracker.Query(dataType, function, replyTo), timeout);
  }

  // unmarshallers for the query parameters
  private final Unmarshaller<String, Tracker.Function> functionUnmarshaller = Unmarshaller.sync(text -> {
    String lcText = text.toLowerCase();
    switch(lcText) {
      case "current": return Tracker.Function.Current;
      case "highlow": return Tracker.Function.HighLow;
      case "average": return Tracker.Function.Average;
      default: throw new IllegalArgumentException("Unknown function " + lcText);
    }
  });
  private final Unmarshaller<String, Tracker.DataType> dataTypeUnmarshaller = Unmarshaller.sync(text -> {
    String lcText = text.toLowerCase();
    switch(lcText) {
      case "temperature": return Tracker.DataType.Temperature;
      case "dewpoint": return Tracker.DataType.DewPoint;
      case "pressure": return Tracker.DataType.Pressure;
      default: throw new IllegalArgumentException("Unknown data type " + lcText);
    }
  });



  public Route tracker() {
    return path(segment("tracker").slash().concat(longSegment()), wsid ->
      concat(
        get(() ->
          parameter(dataTypeUnmarshaller, "type", (dataType ->
            parameter(functionUnmarshaller, "function", (function ->
              completeOKWithFuture(query(wsid, dataType, function), Jackson.marshaller())
            ))
          ))
        ),
        post(() ->
          entity(dataUnmarshaller, data ->
            onSuccess(recordData(wsid, data), performed ->
                complete(StatusCodes.ACCEPTED, performed + " from event time: " + data.eventTime)
            )
          )
        )
      )
    );
  }

}
