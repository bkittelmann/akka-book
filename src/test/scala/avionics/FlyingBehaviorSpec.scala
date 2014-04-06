package avionics

import akka.actor.{Actor, ActorRef, ActorSystem, Props, PoisonPill}
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestFSMRef, TestKit, TestLatch, TestProbe, ImplicitSender}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import scala.concurrent.Await
import scala.concurrent.duration._


class FlyingBehaviorSpec extends TestKit(ActorSystem("FlyingBehaviorSpec"))
                              with WordSpecLike
                              with Matchers{

  import Plane._
  import FlyingBehavior._
  import Altimeter._
  import HeadingIndicator._

  def nilActor: ActorRef = TestProbe().ref

  def fsm(plane: ActorRef = nilActor, heading: ActorRef = nilActor, altimeter: ActorRef = nilActor) = {
    TestFSMRef(new FlyingBehavior(plane, heading, altimeter))
  }

  val target = CourseTarget(0, 0, 0)

  "FlyingBehavior" should {
    "start in the Idle state and with Unitialized data" in {
      val a = fsm()
      a.stateName should be (Idle)
      a.stateData should be (Unitialized)
    }
  }

  "PreparingToFly state" should {
    "stay in PreparingToFly state only when a HeadingUpdate is received" in {
      val a = fsm()
      a ! Fly(target)
      a ! HeadingUpdate(20)
      a.stateName should be (PreparingToFly)
      val sd = a.stateData.asInstanceOf[FlightData]
      sd.status.altitude should be (-1)
      sd.status.heading should be (20)
    }
    "move to Flying state when all parts are received" in {
      val a = fsm()
      a ! Fly(target)
      a ! HeadingUpdate(20)
      a ! AltitudeUpdate(20)
      a ! Controls(testActor)
      a.stateName should be (Flying)
      val sd = a.stateData.asInstanceOf[FlightData]
      sd.controls should be (testActor)
      sd.status.altitude should be (20)
      sd.status.heading should be (20)
    }
  }

  "transitioning to Flying state" should {
    "create the Adjustment timer" in {
      val a = fsm()
      a.setState(PreparingToFly)
      a.setState(Flying)
      a.isTimerActive("Adjustment") should be (true)
      a.cancelTimer("Adjustment")
    }
  }
}