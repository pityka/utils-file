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

scalaVersion := "2.13.5"

crossScalaVersions := Seq("2.12.13", "2.13.5")

name := "fileutils"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.7" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

mimaPreviousArtifacts := Set(organization.value %% moduleName.value % "1.2.5")
