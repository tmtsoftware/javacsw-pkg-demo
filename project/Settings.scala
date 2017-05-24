import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import sbt.Keys._
import sbt._


//noinspection TypeAnnotation
// Defines the global build settings so they don't need to be edited everywhere
object Settings {

  val buildSettings = Seq(
    organization := "org.tmt",
    organizationName := "TMT",
    organizationHomepage := Some(url("http://www.tmt.org")),
    version := Dependencies.Version,
    scalaVersion := Dependencies.ScalaVersion,
    crossPaths := true,
    isSnapshot := true,
    isSnapshot in ThisBuild := true,
    parallelExecution in Test := false,
    fork := true,
    resolvers += Resolver.typesafeRepo("releases"),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += sbtResolver.value
  )

  lazy val defaultSettings = buildSettings ++ Seq(
    // compile options ScalaUnidoc, unidoc
    scalacOptions ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-feature", "-deprecation", "-unchecked"),
    javacOptions in Compile ++= Seq("-source", "1.8"),
    javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in (Test, run) ++= Seq("-Djava.net.preferIPv4Stack=true")  // For location service use
  )

  // For standalone applications
  def packageSettings(name: String, summary: String, desc: String) = defaultSettings ++ Seq(
    version in Rpm := Dependencies.Version,
    rpmRelease := "0",
    rpmVendor := "TMT Common Software",
    rpmUrl := Some("http://www.tmt.org"),
    rpmLicense := Some("ApacheV2"),
    rpmGroup := Some("CSW"),
    packageSummary := summary,
    packageDescription := desc,
    bashScriptExtraDefines ++= Seq(s"addJava -DCSW_VERSION=${Dependencies.Version}"),
    bashScriptExtraDefines ++= Seq(s"addJava -Dapplication-name=$name")
  )

}
