package com.cetracker;

import akka.actor.CoordinatedShutdown;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.server.Route;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;

import java.net.InetSocketAddress;
import java.time.Duration;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;
import akka.actor.Cancellable;
import akka.actor.Props;
import java.time.Duration;

import static akka.Done.done;

final class SensorHttpServer {

  public static void start(Route routes, int port, ActorSystem<?> system) {

    ClusterSingleton singleton = ClusterSingleton.get(system);
    // Start if needed and provide a proxy to a named singleton
    ActorRef<Counter.Command> proxy =
        singleton.init(SingletonActor.of(Counter.create(), "GlobalCounter"));

    // This will schedule to send the Tick-message
    // to the tickActor after 0ms repeating every 50ms
    Cancellable cancellable =
        system
            .scheduler().scheduleAtFixedRate(
              Duration.ofSeconds(2),
              Duration.ofSeconds(2),
              new Runnable() {
                @Override
                public void run() {
                  proxy.tell(Counter.Increment.INSTANCE);
                }
              },
              system.executionContext()
            );
              
    akka.actor.ActorSystem classicActorSystem = Adapter.toClassic(system);

    Materializer materializer = SystemMaterializer.get(system).materializer();

    Http.get(classicActorSystem).bindAndHandle(
        routes.flow(classicActorSystem, materializer),
        ConnectHttp.toHost("localhost", port),
        materializer
    ).whenComplete((binding, failure) -> {
      if (failure == null) {
        final InetSocketAddress address = binding.localAddress();
        system.log().info(
            "TrackServer online at http://{}:{}/",
            address.getHostString(),
            address.getPort());

        CoordinatedShutdown.get(classicActorSystem).addTask(
            CoordinatedShutdown.PhaseServiceRequestsDone(),
            "http-graceful-terminate",
            () ->
              binding.terminate(Duration.ofSeconds(10)).thenApply(terminated -> {
                system.log().info( "TrackServer http://{}:{}/ graceful shutdown completed",
                    address.getHostString(),
                    address.getPort()
                );
                return done();
              })
        );
      } else {
        system.log().error("Failed to bind HTTP endpoint, terminating system", failure);
        system.terminate();
      }
    });
  }
}
