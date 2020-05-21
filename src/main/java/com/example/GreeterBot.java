package com.example;

import com.example.Greeter.Bye;
import com.example.Greeter.Byed;
import com.example.Greeter.Greet;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.*;

public class GreeterBot extends AbstractBehavior<Greeter.Message> {

    public static Behavior<Greeter.Message> create(int max) {
        return Behaviors.supervise(
                Behaviors.setup((ActorContext<Greeter.Message> context) -> new GreeterBot(context, max))
        ).onFailure(SupervisorStrategy.restart());
    }

    private final int max;
    private int greetingCounter;

    private GreeterBot(ActorContext<Greeter.Message> context, int max) {
        super(context);
        this.max = max;
    }

    @Override
    public Receive<Greeter.Message> createReceive() {
        return newReceiveBuilder().onMessage(Greeter.Greeted.class, this::onGreeted)
                                  .onMessage(Greeter.Byed.class, this::onByed)
                                  .onSignal(PreRestart.class, this::onPreRestart)
                                  .onSignal(PostStop.class, this::onPostStop)
                                  .build();
    }

    private Behavior<Greeter.Message> onPreRestart(PreRestart preRestart) {
        this.greetingCounter = 0;
        return Behaviors.same();
    }

    private Behavior<Greeter.Message> onPostStop(PostStop postStop) {
        getContext().getLog().info("Post Stop Signaled");
        return Behaviors.same();
    }

    private Behavior<Greeter.Message> onGreeted(Greeter.Greeted message) {
        greetingCounter++;
        getContext().getLog().info("{} Greeting {} from {}", getContext().getSelf().path(), message.whom, message.from);

        if (greetingCounter == max) {
            getContext().getLog().info("{} Bye {} from {}", getContext().getSelf().path(), message.whom, message.from);
            message.from.tell(new Bye(message.whom));
            throw new RuntimeException("counter reached to max");
        }

        return this;
    }

    private Behavior<Greeter.Message> onByed(Byed message) {
        getContext().getLog().info("Byed to {}", message.whom);

        return Behaviors.same();
    }
}
