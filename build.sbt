import play.core.PlayVersion.current
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "compliance-documents-api"
val silencerVersion = "1.7.0"

majorVersion := 0
scalaVersion := "2.12.11"

lazy val microservice = Project(appName, file("."))
  .configs(IntegrationTest)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 7053)


libraryDependencies ++= Seq(
  "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "2.14.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "com.typesafe.play" %% "play-test" % current % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test, it",
  "com.github.java-json-tools"  % "json-schema-validator"     % "2.2.13",
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % "test, it",
  "org.mockito" %% "mockito-scala" % "1.8.0" % "test",
  "com.github.tomakehurst" % "wiremock-standalone" % "2.26.3" % "test, it",
  compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
  "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
)

ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*Routes.*;.*GuiceInjector;"
ScoverageKeys.coverageMinimum := 100
ScoverageKeys.coverageFailOnMinimum := true
ScoverageKeys.coverageHighlighting := true

publishingSettings
resolvers += Resolver.jcenterRepo
integrationTestSettings
coverageEnabled in(Test, compile) := true

unmanagedResourceDirectories in IntegrationTest += baseDirectory ( _ /"target/web/public/test" ).value

enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
scalacOptions ++= Seq(
  "-P:silencer:pathFilters=views;routes"
)

