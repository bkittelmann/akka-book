import sbt._
import sbt.Keys._

object AkkaBookBuild extends Build {

  lazy val akkaBook = Project(
    id = "akka-book",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Akka Book",
      organization := "zzz.akka",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.3",
      // explanation for why the -no-specialization flag is necessary for ScalaTest
      // https://groups.google.com/forum/#!topic/scalatest-users/hr9vPIb_OkA
      scalacOptions ++= Seq("-language:postfixOps", "-deprecation", "-no-specialization"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq( 
        "org.scalatest" %% "scalatest" % "2.0" % "test",
        "com.typesafe.akka" %% "akka-actor" % "2.2.3",
        "com.typesafe.akka" %% "akka-testkit" % "2.2.3"
      )
    )
  )
}
