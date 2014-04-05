package avionics

import akka.actor.{Actor, ActorRef}


object ControlSurfaces {
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)
  case class StickLeft(amount: Float)
  case class StickRight(amount: Float)
  case class HasControl(somePilot: ActorRef)
}

class ControlSurfaces(plane: ActorRef, altimeter: ActorRef, heading: ActorRef) extends Actor {
  import ControlSurfaces._
  import Altimeter._
  import HeadingIndicator._

  // Instantiate the method by saying that the ControlSurfaces are controlled
  // by the dead letter actor. Effectively, this says that nothing's currently
  // in control
  def receive = controlledBy(context.system.deadLetters)

  // As control is transferred between different entities, we will change the
  // instantiated receive function with new variants. This closure ensures 
  // that onlz the assigned pilot can control the plane
  def controlledBy(somePilot: ActorRef): Receive = {
    
    // pilot pulled the stick back by a certain amount,
    // and we inform the Altimeter that we're climbing
    case StickBack(amount) if sender == somePilot => 
      altimeter ! RateChange(amount)

    // pilot pushes stick forward, we're descending
    case StickForward(amount) if sender == somePilot => 
      altimeter ! RateChange(-1 * amount)

    case StickLeft(amount) if sender == somePilot =>
      heading ! BankChange(-1 * amount)

    case StickRight(amount) if sender == somePilot =>
      heading ! BankChange(amount)

    // only the plane can tell us who's in control
    case HasControl(entity) if sender == plane =>
      context.become(controlledBy(entity))
  }
}