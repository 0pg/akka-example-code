package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.ServiceKey;

import java.util.Objects;

// #greeter
public class Greeter extends AbstractBehavior<Greeter.Message> {

  public interface Message {
  }

  public static final class Count implements Message {
    public final int num;

    public Count(int num) {
      this.num = num;
    }
  }

  public static final class Greet implements Message {
    public final String whom;

    public Greet(String whom) {
      this.whom = whom;
    }
  }

  public static final class Bye implements Message {
    public final String whom;

    public Bye(String whom) {
      this.whom = whom;
    }
  }

  public static final class GreetException implements Message {
    public GreetException() {
    }
  }

  public static final class Byed implements Message {
    public final String whom;
    public final ActorRef<Message> from;

    public Byed(String whom, ActorRef<Message> from) {
      this.whom = whom;
      this.from = from;
    }
  }

  public static final class Greeted implements Message {
    public final String whom;
    public final ActorRef<Message> from;

    public Greeted(String whom, ActorRef<Message> from) {
      this.whom = whom;
      this.from = from;
    }

// #greeter
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Greeted greeted = (Greeted) o;
      return Objects.equals(whom, greeted.whom) &&
              Objects.equals(from, greeted.from);
    }

    @Override
    public int hashCode() {
      return Objects.hash(whom, from);
    }

    @Override
    public String toString() {
      return "Greeted{" +
              "whom='" + whom + '\'' +
              ", from=" + from +
              '}';
    }
// #greeter
  }

  private final ActorRef<Message> replyTo;
  private final ServiceKey<Message> key;

  public static Behavior<Message> create(ServiceKey<Message> replyTo) {
    return Behaviors.supervise(Behaviors.setup((ActorContext<Message> context) -> new Greeter(context, replyTo)))
            .onFailure(RuntimeException.class, SupervisorStrategy.restart());
  }

  private Greeter(ActorContext<Message> context, ServiceKey<Message> replyTo) {
    super(context);
    key = replyTo;
    GroupRouter<Message> group = Routers.group(replyTo);
    this.replyTo = getContext().spawn(group.withRoundRobinRouting(), "replayTo");

    context.getLog().info("Group Router:" + this.replyTo.path());
    context.getLog().info("Greeter started: " + context.getSelf().path());
  }

  @Override
  public Receive<Message> createReceive() {
    return newReceiveBuilder()
            .onMessage(Greet.class, this::onGreet)
            .onMessage(Bye.class, this::onBye)
            .onMessage(Count.class, count -> {
              getContext().getLog().error("Count : " + count.num);
              return Behaviors.same();
            })
            .onMessage(GreetException.class, this::onGreetException)
            .build();

  }

  private Behavior<Message> onGreet(Greet command) throws InterruptedException {
    getContext().getLog().info("Hello {}! this is {}", command.whom, getContext().getSelf().path());
    replyTo.tell(new Greeted(command.whom, getContext().getSelf()));
    Thread.sleep(1000);
    return this;
  }

  private Behavior<Message> onBye(Bye command) {
    getContext().getLog().info("Bye {}!", command.whom);
    replyTo.tell(new Byed(command.whom, getContext().getSelf()));
    return this;
  }

  private Behavior<Message> onGreetException(GreetException command) {
    getContext().getLog().error("Throw Exception");
    throw new RuntimeException();
  }
}
// #greeter

