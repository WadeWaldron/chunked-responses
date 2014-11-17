import sbt._
import sbt.Keys._

object Projects extends Build {

  object Versions {
    val scala = "2.10.4"
    val spray = "1.3.2"
    val akka = "2.3.6"
    val scalatest = "2.0.M5b"
    val mockito = "1.9.5"
    val play = "2.3.5"
  }


  val root = Project("ChunkedResponses",file("."), settings = Defaults.defaultSettings ++ Seq(
	  scalaVersion := Versions.scala,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % Versions.akka,
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka,
      "com.typesafe.play" %% "play-iteratees" % Versions.play,
      "io.spray" %% "spray-can" % Versions.spray,
      "io.spray" %% "spray-routing" % Versions.spray,
      "io.spray" %% "spray-util" % Versions.spray,
      "io.spray" %% "spray-httpx" % Versions.spray,

      "org.scalatest" %% "scalatest" % Versions.scalatest % "test",
      "org.mockito"  % "mockito-all" % Versions.mockito % "test"
    )
  ))
}