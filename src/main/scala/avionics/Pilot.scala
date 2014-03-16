package avionics

import akka.actor.{Actor, ActorContext, ActorRef, ActorRefFactory, Terminated}
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object Pilots {
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot(plane: ActorRef, autopilot: ActorRef, var controls: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilots._
  import Plane._

  var copilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo => 
      plane ! GiveMeControl
      copilot = avionics.resolve(context, s"../$copilotName")

    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }  
}

class Copilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilots._
  import Plane._

  val pilotName = context.system.settings.config.getString("akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo => 
      val pilot = avionics.resolve(context, s"../$pilotName")
      context.watch(pilot)
    case Terminated(_) =>
      // pilot died
      plane ! GiveMeControl
  }  
}