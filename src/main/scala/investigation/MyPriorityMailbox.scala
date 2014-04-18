package investigation

import akka.actor.ActorSystem
import akka.dispatch.PriorityGenerator
import akka.dispatch.UnboundedPriorityMailbox
import com.typesafe.config.Config

case class HighPriority(work: String)
case class LowPriority(work: String)

object MyPriorityMailbox {
  val myPrioComparator = PriorityGenerator {
    // lower number means higher prio
    case HighPriority(_) => 0
    case LowPriority(_) => 2
    // default to medium
    case otherwise => 1
  }
}

class MyPriorityMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedPriorityMailbox(MyPriorityMailbox.myPrioComparator)