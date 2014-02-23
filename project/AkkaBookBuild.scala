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
      scalacOptions ++= Seq("-language:postfixOps"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq( 
        "org.scalatest" %% "scalatest" % "2.0" % "test",
        "com.typesafe.akka" %% "akka-actor" % "2.2.3",
        "com.typesafe.akka" %% "akka-actor" % "2.2.3"
      )
    )
  )
}
