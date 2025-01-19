inThisBuild(
  List(
    organization := "io.github.pityka",
    homepage := Some(url("https://pityka.github.io/utils-file/")),
    licenses := List(("MIT", url("https://opensource.org/licenses/MIT"))),
    developers := List(
      Developer(
        "pityka",
        "Istvan Bartha",
        "bartha.pityu@gmail.com",
        url("https://github.com/pityka/utils-file")
      )
    )
  )
)

scalaVersion := "2.13.16"

crossScalaVersions := Seq("2.12.15", "2.13.16")

name := "fileutils"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.18" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

mimaPreviousArtifacts := Set(organization.value %% moduleName.value % "1.2.5")
