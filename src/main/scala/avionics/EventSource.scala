package avionics

import akka.actor.{Actor, ActorRef}

object EventSource {
  case class RegisterListener(listener: ActorRef)
  case class UnregisterListener(listener: ActorRef)
}

trait EventSource {
  def sendEvent[T](event: T): Unit
  def eventSourceReceive: Actor.Receive
}

trait ProductionEventSource extends EventSource { this: Actor =>
  import EventSource._

  var listeners = Vector.empty[ActorRef]

  def sendEvent[T](event: T): Unit = listeners foreach { _ ! event }

  // we create a specific partial function to handle the messages
  // for our event listener. anything that mixes in our trait will
  // need to compose this receiver
  def eventSourceReceive: Receive = {
    case RegisterListener(listener) => listeners = listeners :+ listener
    case UnregisterListener(listener) => listeners = listeners filter { _ != listener }
  }
}