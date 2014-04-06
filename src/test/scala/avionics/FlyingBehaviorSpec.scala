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

  import FlyingBehavior._

  def nilActor: ActorRef = TestProbe().ref

  def fsm(plane: ActorRef = nilActor, heading: ActorRef = nilActor, altimeter: ActorRef = nilActor) = {
    TestFSMRef(new FlyingBehavior(plane, heading, altimeter))
  }

  "FlyingBehavior" should {
    "start in the Idle state and with Unitialized data" in {
      val a = fsm()
      a.stateName should be (Idle)
      a.stateData should be (Unitialized)
    }
  }
}