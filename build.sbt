name := """petricaep"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

resolvers += "ICM repository" at "http://maven.icm.edu.pl/artifactory/repo"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  "log4j" % "log4j" % "1.2.17",
  "pl.edu.icm.cermine" % "cermine-impl" % "1.9",
  "colt" % "colt" % "1.2.0",
  "org.postgresql" % "postgresql" % "42.0.0",
  "net.arnx" % "jsonic" % "1.3.5",
  "junit" % "junit" % "4.8.2",
  "commons-pool" % "commons-pool" % "1.6",
  "commons-io" % "commons-io" % "2.0.1",
  "commons-logging" % "commons-logging" % "1.1.1",
  "org.apache.commons" % "commons-lang3" % "3.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "xerces" % "xercesImpl" % "2.11.0",
  "directory-naming" % "naming-java" % "0.8",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.7",
  "org.jblas" % "jblas" % "1.2.4"
//  "com.google.guava" % "guava" % "16.0.1"
)

//dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.4"
