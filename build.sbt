import sys.process._
import java.net.URL
import java.io.File
import java.nio.file.StandardCopyOption
import java.nio.file.{Files, Paths}

ThisBuild / scalaVersion := "2.13.3"

// automatically reload the build when source changes are detected
Global / onChangedBuildSource := ReloadOnSourceChanges

def fileDownloader(url: String, filename: String) = {

  /** Download a file from a URL to the specified path. */
  new URL(url) #> new File(filename) !!
}

lazy val downloadExtern = taskKey[Unit]("Download external static resources.")

downloadExtern := {
  val resources = Map(
    // Bootstrap JS & its JS deps
    "https://code.jquery.com/jquery-3.3.1.slim.min.js" -> "static/assets/extern/js/jquery-3.3.1.slim.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js" -> "static/assets/extern/js/popper-1.14.3.min.js",
    "https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js" -> "static/assets/extern/js/bootstrap-4.1.3.min.js",
    // Bootstrap CSS & its dark theme
    "https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css" -> "static/assets/extern/css/bootstrap-4.1.3.min.css",
    "https://cdn.jsdelivr.net/npm/@forevolve/bootstrap-dark@1.0.0/dist/css/bootstrap-dark.min.css" -> "static/assets/extern/css/bootstrap-dark.min.css",
    "https://cdn.jsdelivr.net/npm/@forevolve/bootstrap-dark@1.0.0/dist/css/toggle-bootstrap.min.css" -> "static/assets/extern/css/toggle-bootstrap.min.css",
    "https://cdn.jsdelivr.net/npm/@forevolve/bootstrap-dark@1.0.0/dist/css/toggle-bootstrap-dark.min.css" -> "static/assets/extern/css/toggle-bootstrap-dark.min.css"
  )

  // First, make the needed directories if they don't exist
  val paths = Seq("static/assets/extern/js", "static/assets/extern/css")
  paths.foreach(path => {
    val directory = new File(String.valueOf(path))
    if (!directory.exists()) {
      directory.mkdirs()
      println(s"Created $path.")
    }
  })

  for ((uri, path) <- resources) {
    if (java.nio.file.Files.notExists(new File(path).toPath())) {
      println(s"$path does not exist, downloading...")
      fileDownloader(
        uri,
        path
      )
    }
  }
}

lazy val root = project
  .in(file("."))
  .aggregate(ryw.js, ryw.jvm)
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val ryw = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .settings(
    name := "ryw-web",
    version := "0.1-SNAPSHOT",
    scalacOptions ++= Seq(
      "-Ywarn-unused",
      "-Yrangepos"
    ),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % "1.4.3",
      "com.lihaoyi" %% "scalatags" % "0.8.2",
      "com.thedeanda" % "lorem" % "2.1"
    )
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "cask" % "0.8.0",
      "com.lihaoyi" %% "os-lib" % "0.8.0",
      "com.zaxxer" % "HikariCP" % "5.0.0",
      "io.getquill" %% "quill-jdbc" % "3.12.0",
      "joda-time" % "joda-time" % "2.10.13",
      "net.harawata" % "appdirs" % "1.2.1",
      "org.apache.lucene" % "lucene-core" % "9.0.0",
      "org.apache.lucene" % "lucene-queryparser" % "9.0.0",
      "org.postgresql" % "postgresql" % "42.2.8",
      "org.wvlet.airframe" %% "airframe-log" % "21.12.1"
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.querki" %%% "jquery-facade" % "2.0",
      "org.scala-js" %%% "scalajs-dom" % "1.1.0"
    ),
    npmDependencies in Compile ++= Seq(
      "jquery" -> "3.6.0"
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    webpackBundlingMode := scalajsbundler.BundlingMode.LibraryAndApplication()
  )
  .enablePlugins(ScalaJSBundlerPlugin)

addCompilerPlugin("org.scalameta" % "semanticdb-scalac_2.13.3" % "4.4.30")

// scalafix
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
