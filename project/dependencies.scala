import sbt._
import Keys._

object Dependencies {

  val gson = "com.google.code.gson" % "gson" % "2.2.4"
  val log4jVersion = "2.3"
  val log4ja = "org.apache.logging.log4j" % "log4j-api" % log4jVersion
  val log4jc = "org.apache.logging.log4j" % "log4j-core" % log4jVersion
  val jline2 = "jline" % "jline" % "2.12"

  val standardDependencies: Seq[ModuleID] = Seq(
    gson,
    log4ja, 
    log4jc   
  )
}
