package avionics

import akka.actor.Actor
import scala.concurrent.duration._
import scala.concurrent.duration.Duration

trait AttendantResponsiveness {
  val maxResponseTime: Duration
  def responseDuration = {
    val delay = maxResponseTime.toMillis.toInt
    scala.util.Random.nextInt(delay).millis
  }
}

object FlightAttendant {
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)

  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTime = 5 minutes
  }
}

class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>
  import FlightAttendant._

  implicit val ec = context.dispatcher

  def receive = {
    case GetDrink(drinkname) => {
      context.system.scheduler.scheduleOnce(responseDuration, sender, Drink(drinkname))
    }
  }
}