package avionics

import akka.actor._

trait PilotProvider {
  def newPilot(plane: ActorRef, autopilot: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = 
    new Pilot(plane, autopilot, controls, altimeter)

  def newCopilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef): Actor = 
    new Copilot(plane, autopilot, altimeter)
    
  // def newAutopilot: Actor = new Autopilot
}
