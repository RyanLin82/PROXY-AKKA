import sbt._

object Dependencies {

  object Versions {
    val cats = "2.12.0"
    val catsEffect = "3.5.4"
    val fs2 = "3.10.2"
    val circe = "0.14.2"
    val pureConfig = "0.17.4"
    val akka = "2.8.6"
    val akkaHttp = "10.5.3"

    val kindProjector = "0.13.2"
    val logback = "1.2.3"
    val scalaCheck = "1.15.3"
    val scalaTest = "3.2.7"
    val catsScalaCheck = "0.3.2"
  }

  object Libraries {
    def circe(artifact: String): ModuleID = "io.circe" %% artifact % Versions.circe

    def akka(artifact: String): ModuleID = "com.typesafe.akka" %% artifact % Versions.akka

    lazy val cats = "org.typelevel" %% "cats-core" % Versions.cats
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    lazy val fs2 = "co.fs2" %% "fs2-core" % Versions.fs2
    lazy val akkaActor = akka("akka-actor")
    lazy val akkaStream = akka("akka-stream")
    lazy val typesafeConfig = "com.typesafe" % "config" % "1.4.3"
    lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.5.3"
    lazy val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % "1.39.2"
    lazy val caffeine = "com.github.ben-manes.caffeine" % "caffeine" % "2.9.3"
    lazy val spray = "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3"
    lazy val mockito = "org.mockito" %% "mockito-scala" % "1.17.37"
    lazy val akkaTestKit ="com.typesafe.akka" %% "akka-http-testkit" % "10.5.3"
    lazy val akkaActorTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.6"




    lazy val circeCore = circe("circe-core")
    lazy val circeGeneric = circe("circe-generic")
    lazy val circeGenericExt = circe("circe-generic-extras")
    lazy val circeParser = circe("circe-parser")
    lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig

    // Compiler plugins
    lazy val kindProjector = "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest
    lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % Versions.scalaCheck
    lazy val catsScalaCheck = "io.chrisdavenport" %% "cats-scalacheck" % Versions.catsScalaCheck
  }

}
