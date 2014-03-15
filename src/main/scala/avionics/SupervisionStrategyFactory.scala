package avionics

import akka.actor.{SupervisorStrategy, OneForOneStrategy, AllForOneStrategy}
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._


trait SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider): SupervisorStrategy
}

trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider) = {
    OneForOneStrategy(maxNrRetries, withinTimeRange)(decider)
  }  
}

trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(maxNrRetries: Int, withinTimeRange: Duration)(decider: Decider) = {
    AllForOneStrategy(maxNrRetries, withinTimeRange)(decider)
  }  
}