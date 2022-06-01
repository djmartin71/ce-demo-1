package com.ngc.tracksim;

import akka.actor.typed.ActorSystem;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In another terminal start the Simulator.
 * Starts the simulator network, simulating sensors and tracks.
 */
public class TrackSimulator {

  public static void main(String[] args) {
    final List<Integer> sensorApiPorts;
    if (args.length == 0) {
      sensorApiPorts = Arrays.asList(12553, 12554);
    } else {
      sensorApiPorts = Arrays.asList(args).stream().map(arg -> Integer.parseInt(arg)).collect(Collectors.toList());
    }

    ActorSystem.create(Guardian.create(sensorApiPorts), "TrackSimulator");
  }
}
