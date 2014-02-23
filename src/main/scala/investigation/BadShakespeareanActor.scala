package investigation

import akka.actor.{Actor, Props, ActorSystem}

class BadShakespeareanActor extends Actor {

  def receive = {
    case "Good Morning" => println("Him:  Forsooth 'tis the morn, but mourneth")
    case "You're terrible" => println("Him:  Yup")
  }
}

object BadShakespeareanMain {
  val system = ActorSystem("BadShakespearean")
  val actor = system.actorOf(Props[BadShakespeareanActor], "Shake")

  def send(msg: String) {
    println(s"Me:   $msg")
    actor ! msg
    Thread.sleep(100)
  }

  def main(args: Array[String]): Unit = {
    send("Good Morning")
    send("You're terrible")
    system.shutdown()
  }
}