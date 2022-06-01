package com.cetracker;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Counter extends AbstractBehavior<Counter.Command> {

    public interface Command extends CborSerializable {}

    public enum Increment implements Command {
      INSTANCE
    }

    public static class GetValue implements Command {
      private final ActorRef<Integer> replyTo;

      public GetValue(ActorRef<Integer> replyTo) {
        this.replyTo = replyTo;
      }
    }

    public enum GoodByeCounter implements Command {
      INSTANCE
    }

    public static Behavior<Command> create() {
      return Behaviors.setup(Counter::new);
    }

    private int value = 0;

    private Counter(ActorContext<Command> context) {
      super(context);
    }

    @Override
    public Receive<Command> createReceive() {
      return newReceiveBuilder()
          .onMessage(Increment.class, msg -> onIncrement())
          .onMessage(GetValue.class, this::onGetValue)
          .onMessage(GoodByeCounter.class, msg -> onGoodByCounter())
          .build();
    }

    private Behavior<Command> onIncrement() {
      value++;
      System.out.println("value = " + value);
      return this;
    }

    private Behavior<Command> onGetValue(GetValue msg) {
      msg.replyTo.tell(value);
      return this;
    }

    private Behavior<Command> onGoodByCounter() {
      // Possible async action then stop
      return this;
    }
  }