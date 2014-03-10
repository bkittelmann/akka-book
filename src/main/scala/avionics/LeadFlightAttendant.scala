package avionics

import akka.actor.{Actor, ActorRef, Props}

trait AttendantCreationPolicy {
  val numberOfAttendants = 8
  def createAttendant: Actor = FlightAttendant()
}

trait LeadFlightAttendantProvider {
  def newLeadFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {
  case object GetFlightAttentdant
  case class Attendant(a: ActorRef)

  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor {
  this: AttendantCreationPolicy =>
  import LeadFlightAttendant._

  override def preStart() {
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList("akka.avionics.flightcrew.attendantNames").asScala  
    attendantNames take numberOfAttendants foreach { name =>
      context.actorOf(Props(createAttendant), name)
    }
  }

  def randomAttendant(): ActorRef = {
    context.children.take(scala.util.Random.nextInt(numberOfAttendants) + 1).last
  }

  def receive = {
    case GetFlightAttentdant =>
      sender ! Attendant(randomAttendant())
    case m => 
      randomAttendant() forward m
  }
}