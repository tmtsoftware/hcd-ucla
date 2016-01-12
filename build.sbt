import Dependencies._
import Commons._

lazy val hcd_ucla = (project in file(".")).
  settings(commonSettings:_*).
  settings(
	name := "hcd-ucla"
  )

lazy val commander_io = (project in file("commander_io")).
  settings(commonSettings: _*).
  settings(
	name := "commander_io",
	libraryDependencies ++= standardDependencies,
	libraryDependencies += jssc,
	libraryDependencies += jline,
	unmanagedJars in Compile += file("lib/Ice/Ice-java2.jar"),
	mainClass in Compile := Some("edu.ucla.astro.irlab.io.CommanderConsole")
  )
