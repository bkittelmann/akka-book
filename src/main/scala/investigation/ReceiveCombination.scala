package investigation

import akka.actor.{Actor, ActorSystem, Props}

case class MessageForB(payload: String)

class MyWeirdReceiver extends Actor {

  def behaviorA: Receive = {
    case m =>
      println(m)
  }  

  def behaviorB: Receive = {
    case MessageForB(payload) =>
      println(payload)
  }

  // OOOOPS: behaviorA will always match, the orElse is never used
  def receive = behaviorA orElse behaviorB
}