package avionics

import akka.actor.{Actor, ActorRef, ActorSystem, Props, PoisonPill}
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit, TestLatch, TestProbe, ImplicitSender}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import scala.concurrent.Await
import scala.concurrent.duration._

class FakePilot extends Actor {
  override def receive = {
    case _ =>
  }
}

object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
    akka.avionics.flightcrew.pilotName = "$pilotName"
    akka.avionics.flightcrew.copilotName = "$copilotName"
  """
}

class PilotsSpec extends TestKit(ActorSystem("PilotsSpec", ConfigFactory.parseString(PilotsSpec.configStr))) 
      with ImplicitSender
      with WordSpecLike
      with Matchers {
 
  import PilotsSpec._
  import Plane._

  def nilActor: ActorRef = TestProbe().ref

  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilots/$copilotName"

  def pilotsReadyToGo() = {
    implicit val timeout = Timeout(4 seconds)

    val a = system.actorOf(Props(new IsolatedLifeCycleSupervisor with OneForOneStrategyFactory {
      def childStarter() {
        context.actorOf(Props[FakePilot], pilotName)
        context.actorOf(Props(new Copilot(testActor, nilActor, nilActor)), copilotName)
      }
    }), "TestPilots")

    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3 seconds)
    avionics.resolve(system, copilotPath) ! Pilots.ReadyToGo
  }

  "The Copilot" should {
    "take control when the Pilot dies" in {
      pilotsReadyToGo()
      // kill the pilot
      avionics.resolve(system, pilotPath) ! PoisonPill
      // our test class is acting as the Plane
      expectMsg(GiveMeControl)
      // and it should have been coping from the copilot
      lastSender should be (avionics.resolve(system, copilotPath))
    }
  }
}