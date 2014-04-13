package avionics

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.actor.Cancellable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.duration.Duration


object Passenger {
  case object FastenSeatbelts
  case object UnfastenSeatbelts

  val SeatAssignment = """([\w\s_]+)-(\d+)-([A-Z])""".r
}

trait DrinkRequestProbability {
  // limits likelihood of asking for a drink
  val askThreshold = 0.9f

  // minimum time between drink requests
  val requestMin = 20.minutes

  // some portion of this is added on to requestMin
  val requestUpper = 30.minutes

  // gives us a random time within the previous two bounds
  def randomishTime(): FiniteDuration = {
    requestMin + scala.util.Random.nextInt(requestUpper.toMillis.toInt).millis
  } 
}

trait PassengerProvider {
  def newPassenger(callButton: ActorRef): Actor = 
    new Passenger(callButton) with DrinkRequestProbability
}

class Passenger(callButton: ActorRef) extends Actor with ActorLogging {
  this: DrinkRequestProbability =>

  import Passenger._
  import FlightAttendant.{GetDrink, Drink}
  import scala.collection.JavaConverters._

  val r = scala.util.Random

  case object CallForDrink

  val SeatAssignment(myname, _, _) = self.path.name.replaceAllLiterally("_", " ")

  val drinks = context.system.settings.config.getStringList("akka.avionics.drinks").asScala.toIndexedSeq

  val scheduler = context.system.scheduler

  override def preStart() {
    self ! CallForDrink
  }

  def maybeSendDrinkRequest(): Unit = {
    if (r.nextFloat() > askThreshold) {
      val drinkname = drinks(r.nextInt(drinks.length))
      callButton ! GetDrink(drinkname)
    }
    import ExecutionContext.Implicits.global
    scheduler.scheduleOnce(randomishTime(), self, CallForDrink)
  }

  def receive = {
    case CallForDrink => maybeSendDrinkRequest()

    case Drink(drinkname) => log.info(s"$myname received a $drinkname - yum!")

    case FastenSeatbelts => log.info(s"$myname fastening seatbelt")

    case UnfastenSeatbelts => log.info(s"$myname unfastening seatbelt")
  }
}