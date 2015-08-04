lazy val scalaVersionMajor = "2.11"

lazy val sparkVersion = "1.4.0"

lazy val commonSettings = Seq(
  version := "0.1.0",
  organization := "org.zezutom",
  scalaVersion := scalaVersionMajor + ".7"
)

/** Assembly Plugin Configuration **/

// Skip tests when packaging
test in assembly := {}

// Conflicting path resolution
assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "spark", "unused", xs @ _*)         => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "tweetalyzer",
    libraryDependencies ++= Seq(
      "org.apache.spark" % ("spark-core_" + scalaVersionMajor) % sparkVersion % "provided",
      "org.apache.spark" % ("spark-streaming_" + scalaVersionMajor) % sparkVersion % "provided",
      "org.apache.spark" % ("spark-streaming-twitter_" + scalaVersionMajor) % sparkVersion,
      "org.scalatest" % ("scalatest_" + scalaVersionMajor) % "3.0.0-M4" % "test" 
    )
  )
