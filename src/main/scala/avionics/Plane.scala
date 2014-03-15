package avionics

import akka.actor.{Props, Actor, ActorRef, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object Plane {
  case object GiveMeControl
  case class Controls(controls: ActorRef)
}

class Plane extends Actor with ActorLogging with PilotProvider with LeadFlightAttendantProvider {
  import Altimeter._
  import EventSource._
  import IsolatedLifeCycleSupervisor._
  import Plane._

  implicit val timeout = Timeout(10 second)

  // getting names for actors  
  val config = context.system.settings.config
  val flightcrew = "akka.avionics.flightcrew"

  val pilotName = config.getString(s"$flightcrew.pilotName")
  val copilotName = config.getString(s"$flightcrew.copilotName")
  val attendantName = config.getString(s"$flightcrew.leadAttendantName")

  override def preStart() = {
    import EventSource.RegisterListener
    import Pilots.ReadyToGo

    startEquipment()  
    startPeople()

    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
  }

  def receive = {
    case GiveMeControl => 
      log.info("Plane giving control.")
      sender ! actorForControls("ControlSurfaces")

    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }

  def actorForControls(name: String) = {
    val selection = context.actorSelection("equipment/" + name)
    Await.result(selection.resolveOne(), timeout.duration).asInstanceOf[ActorRef]
  }

  def actorForPilots(name: String) = {
    val selection = context.actorSelection("pilots/" + name)
    Await.result(selection.resolveOne(), timeout.duration).asInstanceOf[ActorRef]
  }

  def startEquipment() {
    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        
        def childStarter() = {
          val alt = context.actorOf(Props(Altimeter()), "Altimeter")
          // context.actorOf(Props[Autopilot], "Autopilot") // ???
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
        
      }), 
      "equipment"
    ) 
    Await.result(controls ? WaitForStart, 1 second)
  }

  def startPeople() {
    val plane = self

    val controls = actorForControls("ControlSurfaces")
    val autopilot = actorForControls("Autopilot")
    val altimeter = actorForControls("Altimeter")

    val people = context.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        
        def childStarter() = {
          context.actorOf(Props(newCopilot(plane, autopilot, altimeter)), copilotName)
          context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
        }
        
      }), 
      "pilots"
    ) 
    context.actorOf(Props(newLeadFlightAttendant), attendantName)
    Await.result(people ? WaitForStart, 1 second)
  }
}