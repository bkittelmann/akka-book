package avionics

import akka.actor.{Actor, Props, ActorRef}

trait AirportSpecifics {
  lazy val headingTo: Float = 0.0f
  lazy val altitude: Double = 0
}

object Airport {
  // messages consumed by the Airport
  case class DirectFlyerToAirport(flyingBehaviour: ActorRef)
  case class StopDirectingFlyer(flyingBehaviour: ActorRef)

  def toronto(): Props = Props(new Airport with BeaconProvider with AirportSpecifics {
    override lazy val headingTo: Float = 314.3f
    override lazy val altitude: Double = 26000
  })

}

class Airport extends Actor {
  this: AirportSpecifics with BeaconProvider =>

  import Airport._

  val beacon = context.actorOf(Props(newBeacon(headingTo)), "Beacon")

  def receive = {
    case DirectFlyerToAirport(flyingBehaviour) =>
      val oneHourFromNow = System.currentTimeMillis() + 60 * 60 * 1000
      val when = oneHourFromNow

      context.actorOf(Props(
//        new MessageTransformer()
      ))

    case StopDirectingFlyer(_) => context.children.foreach { context.stop }
  }
}