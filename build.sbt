lazy val root = Project(
  id = "root",
  base = file(".")
).aggregate(plugin, main, macros, support)

lazy val sharedSettings = Seq(
  scalaVersion  := "2.12.4",
  organization  := "demo",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")
)

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

// This subproject contains a macro definition
lazy val macros = Project(
  id   = "macros",
  base = file("macros")
) settings (
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  scalacOptions += "-Xfatal-warnings"
) settings (sharedSettings : _*)

lazy val support = Project(
  id   = "support",
  base = file("support")
) settings (
  scalacOptions += "-Xfatal-warnings"
) settings (sharedSettings : _*)


// Scalac command line options to install our compiler plugin.
lazy val usePluginSettings = Seq(
  scalacOptions in Compile ++= {
    val classDir = (Keys.classDirectory in (plugin, Compile)).value
    val jar = (Keys.`package` in (plugin, Compile)).value
    val addPlugin = "-Xplugin:" + classDir.getAbsolutePath
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

// A regular module with the application code.
lazy val main = Project(
  id   = "main",
  base = file("main")
).dependsOn(support, macros).settings (sharedSettings ++ usePluginSettings: _*)
