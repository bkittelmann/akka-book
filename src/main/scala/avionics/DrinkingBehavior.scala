package avionics

import akka.actor.{Actor, ActorRef, FSM}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._


object DrinkingBehavior {
  // inbound
  case class LevelChanged(level: Float)

  // outbound
  case object FeelingSober
  case object FeelingTipsy
  case object FeelingLikeZaphod

  def apply(drinker: ActorRef) = new DrinkingBehavior(drinker) with DrinkingResolution
}

trait DrinkingResolution {
  import scala.util.Random
  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkInterval(): FiniteDuration = 
    Random.nextInt(300).seconds
}

class DrinkingBehavior(drinker: ActorRef) extends Actor {
  this: DrinkingResolution =>
  import DrinkingBehavior._
  import context.dispatcher

  // stores the current blood alcohol level
  var currentLevel = 0f

  // just provides shorter access to the scheduler
  val scheduler = context.system.scheduler

  // as time passes our Pilot sobers up
  val sobering = scheduler.schedule(
    initialSobering, soberingInterval, self, LevelChanged(-0.0001f)
  )

  // timer needs to be stopped when Actor shuts down
  override def postStop() {
    sobering.cancel()
  }

  // we've got to start the ball rolling with a single drink
  override def preStart() {
    drink()
  }

  // the call to drink() is scheduling a single event to self that
  // will increase the blood alcohol level
  def drink() = scheduler.scheduleOnce(drinkInterval(), self, LevelChanged(0.005f))

  def receive = {
    case LevelChanged(amount) =>
      drinker ! (if (currentLevel <= 0.01) {
          drink()
          FeelingSober
        } else if (currentLevel <= 0.03) {
          drink()
          FeelingTipsy
        } else FeelingLikeZaphod
      )
  }
}