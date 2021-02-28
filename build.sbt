version := "1.2.3"

organization := "io.github.pityka"

scalaVersion := "2.13.5"

crossScalaVersions := Seq("2.12.13")

name := "fileutils"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.5" % "test"
)

publishTo := sonatypePublishTo.value

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

pomExtra in Global := {
  <url>https://pityka.github.io/utils-file</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>https://opensource.org/licenses/MIT</url>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/pityka/utils-file</connection>
    <developerConnection>scm:git:git@github.com:pityka/utils-file</developerConnection>
    <url>github.com/pityka/utils-file</url>
  </scm>
  <developers>
    <developer>
      <id>pityka</id>
      <name>Istvan Bartha</name>
      <url>https://pityka.github.io/utils-file/</url>
    </developer>
  </developers>
}

scalafmtOnCompile in ThisBuild := true
