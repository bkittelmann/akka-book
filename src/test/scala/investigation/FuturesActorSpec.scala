package investigation

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestLatch, ImplicitSender}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{WordSpecLike, Matchers}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Await}


class FuturesActorSpec extends TestKit(ActorSystem()) 
      with WordSpecLike
      with Matchers {

  "Using futures with actors" should {

    "allow using a future as a sender to an actor" in {
      implicit val askTimeout = Timeout(1 second)

      // interesting, defining the class inline here will make this
      // init call fail because no matching constructor will be found
      val a = system.actorOf(Props[EchoActor])

      val f = a  ? "Echo this back"

      Await.result(f, 1 second) should be ("Echo this back")
    }
  }
}

class EchoActor extends Actor {
  def receive = {
    case m => sender ! m
  }
}