package avionics

import akka.actor.{Props, Actor, ActorSystem, ActorLogging}
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy

import scala.concurrent.duration._

object Altimeter {
  // sent to the altimeter to inform it about rate-of-climb changes
  case class RateChange(amount: Float)
  case class AltitudeUpdate(altitude: Double)
  case class CalculateAltitude(lastTick: Long, tick: Long, roc: Double)
  case class AltitudeCalculated(newTick: Long, altitude: Double)

  def apply() = new Altimeter with ProductionEventSource
}

class Altimeter extends Actor with ActorLogging {
  this: EventSource =>

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

  // pg.196, the risky calculation is moved into a separate, restartable actor
  val altitudeCalculator = context.actorOf(
    Props(
      new Actor {
        def receive = {
          case CalculateAltitude(lastTick, tick, roc) => 
            val alt = if (roc == 0)
              0 // returning zero, comment line out to see the restarts
              // throw new ArithmeticException("Divide by zero")
            else 
              ((tick - lastTick) / 60000.0) * (roc * roc) / roc
            sender ! AltitudeCalculated(tick, alt)
        }
      }
    ), "AltitudeCalculator"
  )

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
      altitudeCalculator ! CalculateAltitude(lastTick, tick, rateOfClimb)
      lastTick = tick
      
    case AltitudeCalculated(tick, altdelta) =>
      altitude += altdelta
      sendEvent(AltitudeUpdate(altitude))
  }

  def receive = eventSourceReceive orElse altimeterReceive

  override def postStop(): Unit = ticker.cancel

  override val supervisorStrategy = OneForOneStrategy(-1, Duration.Inf, loggingEnabled=true) {
    case _ => {
      Restart
    }
  }
}