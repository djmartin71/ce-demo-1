package com.ngc.tracksim;

import akka.actor.typed.ActorSystem;
import com.typesafe.config.Config;

import java.time.Duration;

public class SimulatorSettings {
  public final int sensorPlatforms;
  public final String host;
  public final Duration sampleInterval;

  public SimulatorSettings(int sensorPlatforms, String host, Duration sampleInterval) {
    this.sensorPlatforms = sensorPlatforms;
    this.host = host;
    this.sampleInterval = sampleInterval;
  }

  public static SimulatorSettings create(ActorSystem<?> system) {
    return create(system.settings().config().getConfig("combatedge.tracksimulator"));
  }

  public static SimulatorSettings create(Config config) {
    return new SimulatorSettings(
        config.getInt("initial-sensor-platforms"),
        config.getString("sensor-platform.hostname"),
        config.getDuration("sensor-platform.sample-interval")
    );
  }
}
