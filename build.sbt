version := "1.1.1"

organization := "io.github.pityka"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.11.11", "2.12.4")

name := "fileutils"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

reformatOnCompileSettings

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
