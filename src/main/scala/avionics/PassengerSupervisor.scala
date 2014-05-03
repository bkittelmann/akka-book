package avionics

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.actor._
import akka.actor.SupervisorStrategy._
import akka.routing.BroadcastRouter
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.duration.Duration


object PassengerSupervisor {
  // allows someone to request the BroadcastRouter
  case object GetPassengerBroadcaster

  // returns the broadcast router to the requester
  case class PassengerBroadcaster(broadcaster: ActorRef)

  def apply(callButton: ActorRef) = new PassengerSupervisor(callButton) with PassengerProvider
}

class PassengerSupervisor(callButton: ActorRef) extends Actor {
  this: PassengerProvider =>

  import PassengerSupervisor._

  // we'll resume our immediate children instead of restarting them on an Exception
  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => Escalate
    case _: ActorInitializationException => Escalate
    case _ => Resume
  }

  // internal messages
  case class GetChildren(forSomeone: ActorRef)
  case class Children(children: Iterable[ActorRef], childrenFor: ActorRef)

  override def preStart() {
    context.actorOf(Props(new Actor {
      val config = context.system.settings.config
      
      override val supervisorStrategy = OneForOneStrategy() {
        case _: ActorKilledException => Escalate
        case _: ActorInitializationException => Escalate
        case _ => Stop
      }

      override def preStart() {
        import scala.collection.JavaConverters._
        import com.typesafe.config.ConfigList

        val passengers = config.getList("akka.avionics.passengers")

        passengers.asScala.foreach { nameWithSeat =>
          val id = nameWithSeat.asInstanceOf[ConfigList].unwrapped()
                   .asScala.mkString("-").replaceAllLiterally(" ", "_")
          context.actorOf(Props(newPassenger(callButton)), id)
        }
      }

      def receive = {
        case GetChildren => sender ! context.children.toSeq
      }
    }), "PassengerSupervisor")
  }

  implicit val askTimeout = Timeout(5 seconds)
  import ExecutionContext.Implicits.global

  def noRouter: Receive = {
    case GetPassengerBroadcaster => {
      val destinedFor = sender
      val actor = context.actorSelection("PassengerSupervisor")
      (actor ? GetChildren).mapTo[Seq[ActorRef]] map {
        passengers => (Props[Passenger].withRouter(BroadcastRouter(passengers.toList)), destinedFor)
      } pipeTo self
    }
    case (props: Props, destinedFor: ActorRef) => {
      val router = context.actorOf(props, "Passengers")
      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
    }
  }

  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster => 
    case Children(_, destinedFor) => destinedFor ! PassengerBroadcaster(router)
  }

  def receive = noRouter
}