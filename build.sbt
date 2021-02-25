lazy val commonSettings = Seq(
  organization := "com.marvel",
  version := "0.1.0-SNAPSHOT"
)

scalaVersion := "2.12.2"

val sparkVersion = "3.0.1"

resolvers += Resolver.sbtPluginRepo("releases")

assemblyJarName in assembly := "marvel-scala-sbt-assembly-fatjar-1.0.jar"

lazy val app = (project in file(".")).
settings(commonSettings: _*).
settings(
      name := "marvel"
).enablePlugins(AssemblyPlugin)

libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

libraryDependencies += "org.apache.spark" %% "spark-hive" % sparkVersion % "provided"

