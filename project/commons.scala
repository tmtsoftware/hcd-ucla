import sbt._
import Keys._

object Commons {
  val appVersion = "0.1"

  val settings: Seq[Def.Setting[_]] = Seq(
    organization := "edu.ucla.astro.irlab",

    // remove scala version from artifact names
    crossPaths := false

  )

}