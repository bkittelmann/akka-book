package avionics

import akka.actor.{Props, Actor, ActorSystem, ActorLogging}
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy

import scala.concurrent.duration._

object HeadingIndicator {
  // indicates that something has changed, how fast we're changing direction
  case class BankChange(amount: Float)
  // event publish to listeners so they know where we're headed
  case class HeadingUpdate(heading: Float)

  def apply() = new HeadingIndicator with ProductionEventSource
}

class HeadingIndicator extends Actor with ActorLogging {
  this: EventSource =>

  import HeadingIndicator._
  import context._

  // internal message we use to recalculate our heading
  case object Tick

  // maximum degrees-per-second that our plane can move
  val maxDegPerSec = 5

  // our timer that schedules our updates
  val ticker = system.scheduler.schedule(100 millis, 100 millis, self, Tick)

  // last tick which we can use to calculate our changes
  var lastTick = System.currentTimeMillis

  // the current rate of our bank
  var rateOfBank = 0f

  // holds our current direction
  var heading = 0f

  def headingIndicatorReceive: Receive = {

    case BankChange(amount) => 
      rateOfBank = amount.min(1.0f).max(-1.0f)

    case Tick => 
      val tick = System.currentTimeMillis
      val timeDelta = (tick - lastTick) / 1000f
      val degs = rateOfBank * maxDegPerSec
      heading = (heading + (360 + (timeDelta * degs))) % 360
      lastTick = tick
      //  send the update envet to our listeners
      sendEvent(HeadingUpdate(heading))
  }

  def receive = eventSourceReceive orElse headingIndicatorReceive

  override def postStop(): Unit = ticker.cancel
}
