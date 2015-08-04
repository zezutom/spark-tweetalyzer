lazy val sparkVersion = "1.4.0"

lazy val commonSettings = Seq(
  version := "0.1.0",
  organization := "org.zezutom",
  scalaVersion := "2.11.7"
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
      "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-streaming" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-streaming-twitter" % sparkVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
      "org.scalatest" %% "scalatest" % "3.0.0-M4" % "test" 
    )
  )
