package investigation

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestLatch, ImplicitSender}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.{WordSpecLike, Matchers}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.util.{Success, Failure}


class FuturesActorSpec extends TestKit(ActorSystem()) 
      with WordSpecLike
      with Matchers {

  import ExecutionContext.Implicits.global

  "Using futures with actors" should {

    "allow using a future as a sender to an actor" in {
      implicit val askTimeout = Timeout(1 second)

      // interesting, defining the class inline here will make this
      // init call fail because no matching constructor will be found
      val a = system.actorOf(Props[EchoActor])

      val f = a  ? "Echo this back"

      Await.result(f, 1 second) should be ("Echo this back")
    }

    "use different delays" in {
      val a = system.actorOf(Props[DelayingActor])
      val futures = (1 to 10) map { i =>
        val delay = 10 + i * 2
        val str = s"Delayed for $delay milliseconds"
        a.ask(DelayedEcho(str, delay))(
          Timeout((delay * 1.2).toInt)) andThen {
          case Success(m) => println(s"$m - succeeded")
          case Failure(e) => println(s"$str - failed")
        }
      }

      Await.ready(Future.sequence(futures), 1 second)

      val failed = futures filter { f =>
        f.value.isEmpty || f.value.get.isFailure
      }

      println(failed.size + " failed")
    }
  }
}

class EchoActor extends Actor {
  def receive = {
    case m => sender ! m
  }
}

case class DelayedEcho(msg: String, millis: Long)

class DelayingActor extends Actor {
  def receive = {
    case DelayedEcho(msg, millis) =>
      Thread.sleep(millis)
      sender ! msg
  }
}
