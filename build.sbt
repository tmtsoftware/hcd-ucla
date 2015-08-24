import Dependencies._

lazy val hcd_ucla = (project in file(".")).
  settings(Commons.settings: _*).
  settings(
	libraryDependencies ++= standardDependencies,
	libraryDependencies += "org.scream3r" % "jssc" % "2.8.0",
	libraryDependencies += "jline" % "jline" % "2.12",
	name := "hcd-ucla",
	version := "0.1",
	unmanagedJars in Compile += file("lib/Ice/Ice.jar"),
	unmanagedJars in Compile += file("lib/Ice/Ice-java2.jar"),
	mainClass in Compile := Some("edu.ucla.astro.irlab.io.CommanderConsole")
  )