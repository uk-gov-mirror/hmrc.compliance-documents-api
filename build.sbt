import scoverage.ScoverageKeys

val appName = "compliance-documents-api"

majorVersion := 0
scalaVersion := "3.3.4"

scalacOptions ++= Seq(
  "-Wconf:msg=unused import*:s",
  "-Wconf:msg=routes/.*:s",
  "-Wconf:msg=Flag.*repeatedly:s",
  "-Wconf:msg=unused private member*:s"
)

libraryDependencies ++= AppDependencies.all

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*Routes.*;.*GuiceInjector;$anon;.*javascript;testOnlyDoNotUseInAppConf.*",
    ScoverageKeys.coverageMinimumStmtTotal := 91.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

javaOptions ++= Seq(
  "-Dpolyglot.js.nashorn-compat=true"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .configs(IntegrationTest)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 7053)
  .settings(scoverageSettings: _*)
