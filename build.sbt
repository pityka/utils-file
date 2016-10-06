version := "1.0.0"

organization := "io.github.pityka"

scalaVersion := "2.11.8"

name := "fileutils"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.2" % "test"
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
