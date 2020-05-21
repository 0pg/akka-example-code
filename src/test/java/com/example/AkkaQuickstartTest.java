package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

import com.example.Greeter.Bye;

//#definition
public class AkkaQuickstartTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();
//#definition

    //#test
    @Test
    public void testGreeterActorSendingOfGreeting() {
        TestProbe<Greeter.Greeted> testProbe = testKit.createTestProbe();
        ActorRef<Bye> underTest = testKit.spawn(Greeter.create(), "greeter");
        underTest.tell(new Bye("Charles", testProbe.getRef()));
        testProbe.expectMessage(new Greeter.Greeted("Charles", underTest));
    }
    //#test
}
