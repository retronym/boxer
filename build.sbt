lazy val root = Project(
  id = "root",
  base = file(".")
).aggregate(plugin, asyncMacro, main)

lazy val sharedSettings = Seq(
  scalaVersion  := "2.12.12-bin-e53019e-SNAPSHOT",
  organization  := "demo",
  name          := "boxer",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")
)

resolvers in Global += "pr-scala snapshots" at "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots/"

// This subproject contains a Scala compiler plugin that checks for
// value class boxing after Erasure.
lazy val plugin = Project(
  id   = "plugin",
  base = file("plugin")
) settings (
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  publishArtifact in Compile := false,
  scalacOptions += "-Xfatal-warnings"
) settings (sharedSettings : _*)

// Scalac command line options to install our compiler plugin.
lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= {
    val jar = (Keys.`package` in (plugin, Compile)).value
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

lazy val asyncMacro = Project(
  id   = "async-macro",
  base = file("async-macro")
).settings(sharedSettings).settings(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

// A regular module with the application code.
lazy val main = Project(
  id   = "main",
  base = file("main")
).dependsOn(asyncMacro).settings(sharedSettings ++ usePluginSettings: _*).settings(scalacOptions += "-Xasync")
