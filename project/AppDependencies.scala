import sbt._

object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "9.19.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"               %% s"bootstrap-backend-$playVersion" % bootstrapVersion,
    "com.github.java-json-tools" % "json-schema-validator"           % "2.2.14",
    "org.graalvm.js"             % "js"                              % "25.0.1"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-test-$playVersion" % bootstrapVersion,
    "org.scalamock" %% "scalamock"                    % "7.5.0",
    "org.scalatestplus" %% "mockito-4-11"             % "3.2.18.0"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test

}
