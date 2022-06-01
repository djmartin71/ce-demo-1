package com.ngc.tracksim;

import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.Behaviors;

import java.time.Duration;
import java.util.List;

public final class Guardian {

  public static Behavior<Void> create(List<Integer> sensorPorts) {
    return Behaviors.setup(context -> {
      SimulatorSettings settings = SimulatorSettings.create(context.getSystem());

      for (int i = 1; i <= settings.sensorPlatforms; i++) {
        String wsid = Integer.toString(i);
        // choose one of the HTTP API nodes to report to
        int sensorPort = sensorPorts.get(i % sensorPorts.size());

        context.spawn(
            Behaviors.supervise(
                TrackSensor.create(wsid, settings, sensorPort)
            ).onFailure(
                RuntimeException.class,
                SupervisorStrategy.restartWithBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5), 0.5)
            ),
            "sensor-platform-" + wsid);

      }

      return Behaviors.empty();
    });
  }
}
