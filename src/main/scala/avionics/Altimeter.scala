package avionics

import akka.actor.{Props, Actor, ActorSystem, ActorLogging}

import scala.concurrent.duration._

object Altimeter {
  // sent to the altimeter to inform it about rate-of-climb changes
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)
}

class Altimeter extends Actor with ActorLogging
                              with EventSource {
  import Altimeter._

  // need an execution context for the scheduler
  implicit val ec = context.dispatcher

  // maxiumum ceiling of our plane in 'feet'
  val ceiling = 43000

  // maximum rate of climb for our plane in 'feet per minute'
  val maxRateOfClimb = 5000

  // rate of climb, depending on movement of the stick
  var rateOfClimb = 0f

  // our current altitude
  var altitude = 0d

  // how much time has passed
  var lastTick = System.currentTimeMillis

  // periodically update altitude
  val ticker = context.system.scheduler.schedule(100 millis, 100 millis, self, Tick)

  // internal message
  case object Tick

  def altimeterReceive: Receive = {
    // our rate of climb has changed
    case RateChange(amount) => 
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      // log.info(s"Altimeter changed rate of climb to $rateOfClimb, altitude is %5.2f ft.".format(altitude))
      log.info(s"Altimeter changed rate of climb to $rateOfClimb")

    case Tick => 
      val tick = System.currentTimeMillis
      altitude = altitude + ((tick - lastTick) / 60000.0) * rateOfClimb
      lastTick = tick
      sendEvent(AltitudeUpdate(altitude))
  }

  def receive = eventSourceReceive orElse altimeterReceive

  override def postStop(): Unit = ticker.cancel
}