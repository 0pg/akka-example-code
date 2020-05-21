package com.example;

import java.util.stream.IntStream;

import com.example.Greeter.Greet;
import com.example.Greeter.Message;
import com.example.GreeterMain.Command;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.PoolRouter;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.Routers;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;

public class GreeterMain extends AbstractBehavior<Command> {
    private static final ServiceKey<Message> REPLY_TO = ServiceKey.create(Message.class, "replyTo");

    public interface Command {
    }

    public static class SayHello implements Command {
        public final String name;

        public SayHello(String name) {
            this.name = name;
        }
    }

    public static class SayException implements Command {
        public SayException() {
        }
    }

    private final ActorRef<Greeter.Message> greeter;
    private ActorRef<Greeter.Message> replyTo;

    public static Behavior<Command> create(int poolSize) {
        return Behaviors.supervise(Behaviors.setup((ActorContext<Command> context) -> new GreeterMain(context, poolSize)))
                .onFailure(SupervisorStrategy.restart().withStopChildren(false));
    }

    private GreeterMain(ActorContext<Command> context, int poolSize) {
        super(context);
        PoolRouter<Message> pool = Routers.pool(poolSize, Behaviors.supervise(Greeter.create(REPLY_TO))
                                                                   .onFailure(SupervisorStrategy.restart()));

        replyTo = getContext().spawn(GreeterBot.create(3), "greeterBot");
        getContext().getSystem().receptionist().tell(Receptionist.register(REPLY_TO, replyTo));
        getContext().watch(replyTo);

        greeter = context.spawn(pool, "greeter");
        context.getLog().info("Pool Router: " + greeter.path());
        context.watch(greeter);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SayHello.class, this::onSayHello)
                .onMessage(SayException.class, this::onSayException)
                .onSignal(Terminated.class, signal -> {
                    getContext().getLog().error(getContext().getSelf().path().toString());
                    getContext().getLog().error("Terminate Signaled: " + signal.getRef().path());
                    return Behaviors.same();
                })
                .build();
    }

    private Behavior<Command> onSayException(SayException command) {
        ActorRef<Message> replyTo = getContext().spawn(GreeterBot.create(3), "greeterBot2");
        getContext().getSystem().receptionist().tell(Receptionist.register(REPLY_TO, replyTo));
        getContext().watch(replyTo);
        getContext().getLog().info("greeterBot2 Registered");
        return Behaviors.same();
//        throw new RuntimeException();
    }

    private Behavior<Command> onSayHello(SayHello command) {
        IntStream.range(0, 6)
                 .forEach(num -> {
                     greeter.tell(new Greet(command.name));
                 });
        return this;
    }
}
