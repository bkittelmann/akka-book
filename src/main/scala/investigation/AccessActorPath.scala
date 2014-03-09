package investigation

import akka.actor._

// p.136
object AccessActorPath extends App {
  val system = ActorSystem("test")

  val a = system.actorOf(
    Props(
      new Actor {
        def receive = Actor.emptyBehavior
      }
    ), 
    "anonymous"
  )

  println(a.path)

  println(a.path.elements.mkString("/", "/", ""))

  println(a.path.name)

  system.shutdown()
}
