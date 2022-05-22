import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import sbtwelcome._

name := "hadoukenify"

addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")

logo :=
  s"""| _   _           _             _              _  __
      || | | |         | |           | |            (_)/ _|
      || |_| | __ _  __| | ___  _   _| | _____ _ __  _| |_ _   _
      ||  _  |/ _` |/ _` |/ _ \\| | | | |/ / _ \\ '_ \\| |  _| | | |
      || | | | (_| | (_| | (_) | |_| |   <  __/ | | | | | | |_| |
      |\\_| |_/\\__,_|\\__,_|\\___/ \\__,_|_|\\_\\___|_| |_|_|_|  \\__, |
      |                                                     __/ |
      |                                                    |___/
      |""".stripMargin

usefulTasks := Seq(
  UsefulTask("a", "start", "Start the local webpack server at http://localhost:8888"),
  UsefulTask("b", "fastOptJS", "Compiles Scala.js files and live reloads if the app is open in the browser"),
  UsefulTask("c", "~compile", "Compile all modules with file-watch enabled"),
  UsefulTask("d", "fmt", "Run scalafmt on the entire project")
)

lazy val start = TaskKey[Unit]("start")

lazy val dist = TaskKey[File]("dist")

lazy val hadoukenify =
  project
    .enablePlugins(ScalablyTypedConverterPlugin)
    .configure(baseSettings, bundlerSettings, browserProject, withCssLoading)
    .settings(
      Compile / npmDependencies ++= Seq(
        "gif.js"        -> "0.2.0",
        "@types/gif.js" -> "0.2.2"
      ),
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.2.0",
      useYarn                                := true,
      webpackDevServerPort                   := 8888,
      webpackDevServerExtraArgs              := Seq("--host", "0.0.0.0")
    )

def baseSettings(project: Project) =
  project
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaVersion                    := "2.13.8",
      version                         := "0.1.0",
      scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= (_.withSourceMap(false).withModuleKind(ModuleKind.CommonJSModule))
    )

def bundlerSettings(project: Project) =
  project.settings(
    Compile / fastOptJS / webpackExtraArgs += "--mode=development",
    Compile / fullOptJS / webpackExtraArgs += "--mode=production",
    Compile / fastOptJS / webpackDevServerExtraArgs += "--mode=development",
    Compile / fullOptJS / webpackDevServerExtraArgs += "--mode=production"
  )

def withCssLoading(project: Project) =
  project.settings(
    /* custom webpack file to include css */
    webpackConfigFile := Some((ThisBuild / baseDirectory).value / "custom.webpack.config.js"),
    Compile / npmDevDependencies ++= Seq(
      "webpack-merge" -> "4.1",
      "css-loader"    -> "2.1.0",
      "style-loader"  -> "0.23.1",
      "file-loader"   -> "3.0.1",
      "url-loader"    -> "1.1.2"
    )
  )

// These tasks come from https://github.com/ScalablyTyped/Demos
def browserProject(project: Project) =
  project.settings(
    start := {
      (Compile / fastOptJS / startWebpackDevServer).value
    },
    dist  := {
      val artifacts      = (Compile / fullOptJS / webpack).value
      val artifactFolder = (Compile / fullOptJS / crossTarget).value
      val distFolder     = (ThisBuild / baseDirectory).value / "docs" / moduleName.value

      distFolder.mkdirs()
      artifacts.foreach { artifact =>
        val target = artifact.data.relativeTo(artifactFolder) match {
          case None          => distFolder / artifact.data.name
          case Some(relFile) => distFolder / relFile.toString
        }

        Files.copy(artifact.data.toPath, target.toPath, REPLACE_EXISTING)
      }

      val indexFrom = baseDirectory.value / "src/main/js/index.html"
      val indexTo   = distFolder / "index.html"

      val indexPatchedContent = {
        import collection.JavaConverters._
        Files
          .readAllLines(indexFrom.toPath, IO.utf8)
          .asScala
          .map(_.replaceAllLiterally("-fastopt-", "-opt-"))
          .mkString("\n")
      }

      Files.write(indexTo.toPath, indexPatchedContent.getBytes(IO.utf8))
      distFolder
    }
  )
