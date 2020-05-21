package com.example;

import akka.Done;
import akka.actor.typed.ActorSystem;

import java.io.IOException;

import com.example.GreeterMain.SayException;

public class AkkaQuickstart {
  public static void main(String[] args) throws InterruptedException {
    //#actor-system
    final ActorSystem<GreeterMain.Command> greeterMain = ActorSystem.create(GreeterMain.create(2), "helloakka");
    //#actor-system

    //#main-send-messages
    greeterMain.tell(new GreeterMain.SayHello("MediaPlatform"));
    greeterMain.tell(new GreeterMain.SayException());
    //#main-send-messages


    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {

    } finally {
      greeterMain.terminate();
    }
  }
}
