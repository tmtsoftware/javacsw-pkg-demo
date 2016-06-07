import sbt.Keys._
import sbt._

import Dependencies._
import Settings._

lazy val assembly1Java = project
  .enablePlugins(JavaAppPackaging)
  .settings(packageSettings("assembly1Java", "Assembly Java Demo", "Example Java assembly"): _*)
  .settings(libraryDependencies ++= Seq(pkg, javacsw))
  .dependsOn(hcd2Java)

lazy val container1Java = project
  .enablePlugins(JavaAppPackaging)
  .settings(packageSettings("container1Java", "Container Java demo", "Example Java container"): _*)
  .settings(libraryDependencies ++= Seq(containerCmd, javacsw)) dependsOn assembly1Java

lazy val hcd2Java = project
  .enablePlugins(JavaAppPackaging)
  .settings(packageSettings("hcd2Java", "HCD Java demo", "Example Java HCD"): _*)
  .settings(libraryDependencies ++= Seq(pkg, jeromq, javacsw))

lazy val container2Java = project
  .enablePlugins(JavaAppPackaging)
  .settings(packageSettings("container2Java", "Container Java demo", "Example Java container"): _*)
  .settings(libraryDependencies ++= Seq(containerCmd, javacsw)) dependsOn hcd2Java


// -- Root Project --
lazy val cswPkgDemo = (project in file("."))
  .settings(name := "Java CSW Package Demo")
  .aggregate(assembly1Java, container1Java, hcd2Java, container2Java)
