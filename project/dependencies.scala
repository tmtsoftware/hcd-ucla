import sbt._
import Keys._

object Dependencies {

  val gsonVersion = "2.2.4"
  val log4jVersion = "2.3"
  val jlineVersion = "2.12"
  val jsscVersion = "2.8.0"
  val akkaVersion = "2.4.0"
  val cswVersion = "0.2-SNAPSHOT"

  val gson = "com.google.code.gson" % "gson" % gsonVersion
  val log4ja = "org.apache.logging.log4j" % "log4j-api" % log4jVersion
  val log4jc = "org.apache.logging.log4j" % "log4j-core" % log4jVersion
  val jline = "jline" % "jline" % jlineVersion
  val jssc = "org.scream3r" % "jssc" % jsscVersion  
  val akka = "com.typesafe.akka" % "akka-actor_2.11" % akkaVersion
  val akka_remote = "com.typesafe.akka" % "akka-remote_2.11" % akkaVersion
  
  val ccs = "org.tmt" %% "ccs" % cswVersion
  val pkg = "org.tmt" %% "pkg" % cswVersion
  val ts = "org.tmt" %% "ts" % cswVersion
  val event = "org.tmt" %% "event" % cswVersion
  val loc = "org.tmt" %% "loc" % cswVersion  

  val standardDependencies: Seq[ModuleID] = Seq(
    gson,
    log4ja, 
    log4jc   
  )
  
  val akkaDependencies: Seq[ModuleID] = Seq(
    akka,
    akka_remote
  )

  val cswDependencies : Seq[ModuleID] = Seq(
    ccs,
    pkg,
    ts,
    event,
    loc
  )
}