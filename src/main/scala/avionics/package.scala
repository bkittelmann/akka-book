package avionics

import akka.actor.{Actor, ActorContext, ActorRef, ActorRefFactory}
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._


package object avionics {

  implicit val timeout = Timeout(10 seconds)

  def resolve(factory: ActorRefFactory, path: String): ActorRef = {
    val selection = factory.actorSelection(path)
    Await.result(selection.resolveOne(), timeout.duration).asInstanceOf[ActorRef]
  }
}