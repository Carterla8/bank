ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "bank"
  )

resolvers += "Apache Nexus Repository" at "https://repository.apache.org/content/repositories/releases"

val pekkoVersion = "1.0.2"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence" % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster" % pekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-tools" % pekkoVersion,
  "org.apache.pekko" %% "pekko-coordination" % pekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-cassandra" % "1.0.0",
  "org.apache.pekko" %% "pekko-persistence-testkit" % pekkoVersion % Test,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "org.apache.pekko" %% "pekko-http" % "1.0.1",
)



val circeVersion = "0.14.9" // Latest stable version
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "com.github.pjfanning" %% "pekko-http-circe" % "2.6.0",
)
