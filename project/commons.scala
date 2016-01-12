import sbt._
import Keys._

object Commons {
  val appVersion = "0.1-SNAPSHOT"
  
  val commonSettings: Seq[Def.Setting[_]] = Seq(
    organization := "edu.ucla.astro.irlab",
    scalaVersion := "2.11.7",
    version := appVersion
  )

}