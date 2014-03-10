package avionics

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike, BeforeAndAfterAll}
import scala.concurrent.duration._


object TestFlightAttendant {
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTime = 1 millisecond 
  }
}

class FlightAttendantSpec extends TestKit(
  ActorSystem("flightAttendantSpec", ConfigFactory.parseString("akka.scheduler.tick-duration = 1ms")))
      with ImplicitSender 
      with WordSpecLike 
      with Matchers {

  import FlightAttendant._

  "FlightAttendant" should {
    "get a drink when asked" in {
      val a = TestActorRef(Props(TestFlightAttendant()))
      a ! GetDrink("Soda")
      expectMsg(Drink("Soda"))
    }
  }
}
