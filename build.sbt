lazy val root = Project(
  id = "root",
  base = file(".")
).aggregate(plugin, macros, support, useMacro, usePlugin)

lazy val sharedSettings = Seq(
  scalaVersion  := "2.12.8-bin-2e1cbe9-SNAPSHOT",
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

lazy val usePlugin = Project(
  id   = "use-plugin",
  base = file("use-plugin")
).dependsOn(support)
  .settings (sharedSettings ++ usePluginSettings: _*)

lazy val useMacro = Project(
  id   = "use-macro",
  base = file("use-macro")
).dependsOn(support, macros).settings (sharedSettings: _*)
  .settings(  {
    def includeOnMacroClasspath(element: sbt.Attributed[java.io.File]): Boolean = {
      val fileName = element.data.getAbsolutePath
      fileName.contains("macros")
    }
    scalacOptions in Compile ++= List("-Ymacro-classpath", (internalDependencyClasspath in Compile).value.filter(includeOnMacroClasspath).map(_.data.toString).mkString(java.io.File.pathSeparator))
  }
)
