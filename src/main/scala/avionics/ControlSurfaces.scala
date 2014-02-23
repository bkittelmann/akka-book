package avionics

import akka.actor.{Actor, ActorRef}


object ControlSurfaces {
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)
}

class ControlSurfaces(altimeter: ActorRef) extends Actor {
  import ControlSurfaces._
  import Altimeter._

  def receive = {
    // pilot pulled the stick back by a certain amount,
    // and we inform the Altimeter that we're climbing
    case StickBack(amount) => altimeter ! RateChange(amount)

    // pilot pushes stick forward, we're descending
    case StickForward(amount) => altimeter ! RateChange(-1 * amount)
  }
}