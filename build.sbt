import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "compliance-documents-api"

majorVersion := 0
scalaVersion := "3.3.4"

scalacOptions += "-Xlint:-missing-interpolator"
scalacOptions += "-Wconf:src=routes/.*:s"
scalacOptions +=  "-Wconf:cat=unused-imports&src=html/.*:s"

libraryDependencies ++= AppDependencies.all

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*Routes.*;.*GuiceInjector;$anon;.*javascript;testOnlyDoNotUseInAppConf.*",
    ScoverageKeys.coverageMinimumStmtTotal := 91.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

integrationTestSettings()

javaOptions ++= Seq(
  "-Dpolyglot.js.nashorn-compat=true"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .configs(IntegrationTest)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 7053)
  .settings(scoverageSettings: _*)
