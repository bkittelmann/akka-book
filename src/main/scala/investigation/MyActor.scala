package investigation

import akka.actor.{Actor, ActorSystem, Props}

case class Gamma(g: String)
case class Beta(b: String, g: Gamma)
case class Alpha(b1: Beta, b2: Beta)

class MyActor extends Actor {
  def receive = {
    // literal String match
    case "Hello" => 
      println("Hi")

    case 42 =>
      println("I don't know the question")

    case s: String =>
      println(s"You sent me a string: $s")

    case Alpha(Beta(b1, Gamma(g1)), Beta(b2, Gamma(g2))) =>
      println(s"beta1: $b1, beta2: $b2, gamma1: $g1, gamma2: $g2")

    case _ => 
      println("Huh?")
  }
}