package avionics

import akka.actor.{Props, Actor, ActorLogging}
import akka.actor.ActorRef

object Plane {
  case object GiveMeControl
  case class Controls(controls: ActorRef)
}

class Plane extends Actor with ActorLogging {
  import Altimeter._
  import EventSource._
  import Plane._

  val flightcrew = "akka.avionics.flightcrew"
  val altimeter = context.actorOf(Props(Altimeter()), "Altimeter")
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")
  
  val config = context.system.settings.config
  val pilot = context.actorOf(Props[Pilot], config.getString(s"$flightcrew.pilotName"))
  val copilot = context.actorOf(Props[Copilot], config.getString(s"$flightcrew.copilotName"))
  val flightAttendant = context.actorOf(Props(LeadFlightAttendant()), config.getString(s"$flightcrew.leadAttendantName"))

  override def preStart() = {
    altimeter ! RegisterListener(self)
    List(pilot, copilot) foreach { _ ! Pilots.ReadyToGo }
  }

  def receive = {
    case GiveMeControl => 
      log.info("Plane giving control.")
      sender ! Controls(controls)

    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }
}