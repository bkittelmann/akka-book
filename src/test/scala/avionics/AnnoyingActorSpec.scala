package avionics

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestLatch, TestProbe, ImplicitSender}
import org.scalatest.fixture.{UnitFixture, WordSpec}
import org.scalatest.{WordSpecLike, BeforeAndAfterAll}
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution


class AnnoyingActorSpec extends TestKit(ActorSystem("AnnoyingActorSpec")) 
      with ImplicitSender
      with WordSpecLike
      with Matchers {

  "The AnnoyingActor" should {
    "say Hello!!!" in {
      // if we don't use probes, the messages will be received by future checks
      // on the supplied testActor reference
      val p = TestProbe()
      val a = system.actorOf(Props(new AnnoyingActor(p.ref)))
      p.expectMsg("Hello!!!")
      system.stop(a)
    }
  }
  "The NiceActor" should {
    "say Hi" in {
      val p = TestProbe()
      val a = system.actorOf(Props(new NiceActor(p.ref)))
      p.expectMsg("Hi")
      system.stop(a)
    }
  }
}

class AnnoyingActor(snooper: ActorRef) extends Actor {
  override def preStart() {
    self ! 'send
  }
  def receive = {
    case 'send => 
      snooper ! "Hello!!!"
      self ! 'send
  }
}


class NiceActor(snooper: ActorRef) extends Actor {
  override def preStart() {
    snooper ! "Hi"
  }
  def receive = {
    case _ =>
  }
}