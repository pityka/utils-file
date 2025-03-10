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

scalaVersion := "2.13.10"

crossScalaVersions := Seq("2.12.13", "2.13.16","3.3.5")

name := "fileutils"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

mimaPreviousArtifacts := Set(organization.value %% moduleName.value % "1.2.5").take(CrossVersion
      .partialVersion(scalaVersion.value) match {
        case Some((2, _)) => 1
        case Some((3,_)) => 0
      })
