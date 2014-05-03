package avionics

import akka.actor.{Props, Actor, ActorSystem, ActorLogging}
import akka.actor.SupervisorStrategy._
import akka.actor.OneForOneStrategy

import scala.concurrent.duration._


object StatusReporter {
  case object ReportStatus

  sealed trait Status
  case object StatusOK extends Status
  case object StatusNotGreat extends Status
  case object StatusBAD extends Status
}

trait StatusReporter { this: Actor => 
  import StatusReporter._

  def currentStatus: Status

  def statusReceive: Receive = {
    case ReportStatus => sender ! currentStatus
  }
}
