package avionics

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestLatch, ImplicitSender}
import org.scalatest.{WordSpecLike, Matchers, BeforeAndAfterAll}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.concurrent.Await


object PassengerSupervisorSpec {
  val config = ConfigFactory.parseString("""
    akka.avionics.passengers = [
      [ "Kelly Franqui",      "01", "A" ],
      [ "Tyrone Dotts",       "02", "B" ],
      [ "Malinda Class",      "03", "C" ],
      [ "Kenya Jolicoeur",    "04", "A" ],
      [ "Christian Piche",    "10", "B" ]
    ]
  """)
}

trait TestPassengerProvider extends PassengerProvider {
  override def newPassenger(callButton: ActorRef): Actor = 
    new Actor {
      def receive = {
        case m => callButton ! m
      }
    }
}

class PassengerSupervisorSpec extends TestKit(
  ActorSystem("PassengerSupervisorSpec", PassengerSupervisorSpec.config))
      with ImplicitSender 
      with WordSpecLike
      with BeforeAndAfterAll
      with Matchers {

  import PassengerSupervisor._

  override def afterAll() {
    system.shutdown()
  } 

  "PassengerSupervisor" should {
    "work" in {
      // val a = system.actorOf(Props(new PassengerSupervisor(testActor) with TestPassengerProvider))
      // a ! GetPassengerBroadcaster
      // val broadcaster = expectMsgType[PassengerBroadcaster].broadcaster
    }
  }
}