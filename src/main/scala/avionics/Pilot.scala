package avionics

import akka.actor.{Actor, ActorContext, ActorRef}
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object Pilots {
  case object ReadyToGo
  case object RelinquishControl

  implicit val timeout = Timeout(10 seconds)

  def resolve(context: ActorContext, path: String): ActorRef = {
    val selection = context.actorSelection(path)
    Await.result(selection.resolveOne(), timeout.duration).asInstanceOf[ActorRef]
  }
}

class Pilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilots._
  import Plane._

  var copilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo => 
      plane ! GiveMeControl
      copilot = resolve(context, "../" + copilotName)

    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }  
}

class Copilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilots._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters

  val pilotName = context.system.settings.config.getString("akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo => 
      pilot = resolve(context, s"../$pilotName")
  }  
}