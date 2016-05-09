import sbt._

object Dependencies {

  val Version = "0.2-SNAPSHOT"
  val ScalaVersion = "2.11.8"

  val pkg = "org.tmt" %% "pkg" % Version
  val javacsw = "org.tmt" %% "javacsw" % Version
  val containerCmd = "org.tmt" %% "containercmd" % Version
  val jeromq = "org.zeromq" % "jeromq" % "0.3.5"
}

