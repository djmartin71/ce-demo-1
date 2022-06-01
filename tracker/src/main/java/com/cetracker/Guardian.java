package com.cetracker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;


/**
 * Root actor bootstrapping the application
 */
final class Guardian {

  public static Behavior<Void> create(int httpPort) {
    return Behaviors.setup(context -> {
      Tracker.initSharding(context.getSystem());

      // TODO: Add pre-/post-processing here
      // TODO: Add policy here

      ClusterSingleton singleton = ClusterSingleton.get(context.getSystem());
      // Start if needed and provide a proxy to a named singleton
      ActorRef<Counter.Command> proxy =
          singleton.init(SingletonActor.of(Counter.create(), "GlobalCounter"));
      
      proxy.tell(Counter.Increment.INSTANCE);      

      SensorRoutes routes = new SensorRoutes(context.getSystem());
      SensorHttpServer.start(routes.tracker(), httpPort, context.getSystem());

      return Behaviors.empty();
    });
  }
}
