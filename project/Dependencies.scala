import sbt._

object Dependencies {

  val Version = "0.4-SNAPSHOT"
  val ScalaVersion = "2.12.1"

  val pkg = "org.tmt" %% "pkg" % Version
  val ccs = "org.tmt" %% "ccs" % Version
  val javacsw = "org.tmt" %% "javacsw" % Version
  val containerCmd = "org.tmt" %% "containercmd" % Version
  val jeromq = "org.zeromq" % "jeromq" % "0.3.6"
}

