package investigation

import akka.actor._
import avionics._

// p.144
object FlightAttendantPathChecker extends App {
  val system = ActorSystem("planeSimulation")
  val lead = system.actorOf(
    // Props(new LeadFlightAttendant with AttendantCreationPolicy),
    Props[Plane],
    "plane"
  )
  Thread.sleep(2000)
  system.shutdown()
}
