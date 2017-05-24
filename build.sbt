name := """petricaep"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

//resolvers += "ICM repository" at "http://maven.icm.edu.pl/artifactory/repo"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  "colt" % "colt" % "1.2.0",
  "org.postgresql" % "postgresql" % "42.0.0",
  "org.apache.commons" % "commons-math3" % "3.6.1"
)
