package avionics

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestLatch, ImplicitSender}
import org.scalatest.{WordSpecLike, Matchers}
import scala.concurrent.duration._
import scala.concurrent.Await

trait TestDrinkRequestProbability extends DrinkRequestProbability {
  override val askThreshold = 0f
  override val requestMin = 0.millis
  override val requestUpper = 2.millis
}

class PassengerSpec extends TestKit(ActorSystem()) 
      with ImplicitSender 
      with WordSpecLike
      with Matchers {

  import akka.event.Logging.{Info, InfoLevel}
  import akka.testkit.TestProbe
  import Passenger._

  // might be that a better way to do this is through a TestEventListener
  // http://doc.akka.io/docs/akka/2.3.2/scala/testing.html#Expecting_Log_Messages
  system.eventStream.setLogLevel(InfoLevel)

  var seatNumber = 9

  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(new Passenger(Some(testActor)) with TestDrinkRequestProbability), s"Bob-$seatNumber-B")
  }
 
  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      // this only works if the log level is INFO or less, hence we set it explicitly above
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF() {
        case Info(_, _, m) =>
          m.toString should include (" fastening seatbelt")
      }
    }
  }
}