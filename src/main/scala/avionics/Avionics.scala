package avionics

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object Avionics {
  // needed for the '?' below
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(Props[Plane], "Plane")

  def main(args: Array[String]): Unit = {
    // grab the controls
    val control = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef], 5 seconds)

    // takeoff!
    system.scheduler.scheduleOnce(200 millis) {
      control ! ControlSurfaces.StickBack(1f)
    }

    // level out
    system.scheduler.scheduleOnce(1 second) {
      control ! ControlSurfaces.StickBack(0f)
    }    

    // climb
    system.scheduler.scheduleOnce(3 seconds) {
      control ! ControlSurfaces.StickBack(0.5f)
    }

    // level out
    system.scheduler.scheduleOnce(4 seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }

    // shout down
    system.scheduler.scheduleOnce(5 seconds) {
      system.shutdown()
    }
  }
}