package avionics

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestActorRef, ImplicitSender}
import org.scalatest.fixture
import org.scalatest.Matchers
import org.scalatest.fixture.UnitFixture
import org.scalatest.ParallelTestExecution


class MyActorSpec extends fixture.WordSpec
      with Matchers
      with UnitFixture
      with ParallelTestExecution {

  "My Actor" should {
    "construct without exception" in new ActorSys {
      val a = system.actorOf(Props[MyActor], "MyActor")
    }
    "respond with a Pong to a Ping" in new ActorSys {
      val a = system.actorOf(Props[MyActor], "MyActor")
      a ! Ping
      expectMsg(Pong)
    }
  }
}

class MyActor extends Actor {
  def receive = {
    case Ping => sender ! Pong
  }
}

case object Ping
case object Pong