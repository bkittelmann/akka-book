package avionics

import akka.actor.{Actor, ActorContext, ActorRef, ActorRefFactory, FSM, Props, Terminated}
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._

object Pilots {
  case object ReadyToGo
  case object RelinquishControl
}

object Pilot {
  import FlyingBehavior._
  import ControlSurfaces._

  val tipsyCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickForward(amt * 1.03f)
      case StickBack(amt) => StickBack(amt * 1.03f)
      case m => m
    }
  }

  val tipsyCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickForward(amt * 1.03f)
      case StickRight(amt) => StickBack(amt * 1.03f)
      case m => m
    }
  }

  val zaphodCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickForward(1f)
      case StickBack(amt) => StickBack(1f)
      case m => m
    }
  }

  val zaphodCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickForward(1f)
      case StickRight(amt) => StickBack(1f)
      case m => m
    }
  }
}

trait DrinkingProvider {
  def newDrinkingBehavior(drinker: ActorRef): Props = Props(DrinkingBehavior(drinker))
}

trait FlyingProvider {
  def newFlyingBehavior(plane: ActorRef,
                        heading: ActorRef,
                        altimeter: ActorRef): Props = 
    Props(new FlyingBehavior(plane, heading, altimeter))
}

class Pilot(plane: ActorRef, autopilot: ActorRef, heading: ActorRef, altimeter: ActorRef) extends Actor {
  this: DrinkingProvider with FlyingProvider =>

  import Pilots._
  import Pilot._
  import Plane._
  import Altimeter._
  import ControlSurfaces._
  import DrinkingBehavior._
  import FlyingBehavior._
  import FSM._

  val copilotName = context.system.settings.config.getString("akka.avionics.flightcrew.copilotName")

  def setCourse(flyer: ActorRef) {
    flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000))
  }

  override def preStart() {
    context.actorOf(newDrinkingBehavior(self), "DrinkingBehavior")
    context.actorOf(newFlyingBehavior(plane, heading, altimeter), "FlyingBehavior")
  }

  def bootstrap: Receive = {
    case ReadyToGo => 
      val copilot = avionics.resolve(context, s"../$copilotName")
      val flyer = avionics.resolve(context, s"FlyingBehavior")
      flyer ! SubscribeTransitionCallBack(self)
      setCourse(flyer)
      context.become(sober(copilot, flyer))
  }

  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => // we're already sober
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => // already tipsy
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }

  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => // already zaphod
  }

  def idle: Receive = {
    case _ =>
  }

  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }

  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }

  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

  override def unhandled(msg: Any): Unit = {
    msg match {
      case Transition(_, _, Flying) => 
        setCourse(sender)
      case Transition(_, _, Idle) =>
        context.become(idle)
      case Transition(_, _, _) =>
      case CurrentState(_, _) => 
      case m => super.unhandled(m)
    }
  }

  var copilot: ActorRef = context.system.deadLetters

  def receive = bootstrap
}

class Copilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilots._
  import Plane._

  val pilotName = context.system.settings.config.getString("akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo => 
      val pilot = avionics.resolve(context, s"../$pilotName")
      context.watch(pilot)
    case Terminated(_) =>
      // pilot died
      plane ! GiveMeControl
  }  
}