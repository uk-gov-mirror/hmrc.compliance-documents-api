import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

val appName = "compliance-documents-api"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.6.4"


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
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(scoverageSettings: _*)
  .settings(
    libraryDependencies ++= AppDependencies.all,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += s"-Wconf:msg=unused import:s,msg=unused explicit parameter:s,src=.*[\\\\/]routes[\\\\/].*:s",
    Compile / scalacOptions --= Seq("-deprecation","-unchecked","-encoding","UTF-8"),
    Test    / scalacOptions --= Seq("-deprecation","-unchecked","-encoding","UTF-8")
  )
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
  )
  .settings(PlayKeys.playDefaultPort := 7053)
addCommandAlias("testAll", "; test ; it/test")


lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "compile->compile,test;test->compile,test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings( Test / scalaSource := baseDirectory.value / "test" / "scala")