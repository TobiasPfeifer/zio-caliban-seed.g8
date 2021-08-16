ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "ch.tpfeifer.demo"

val zioVersion       = "1.0.10"
val zioOpticsVersion = "0.1.0"
val zioJsonVerion    = "0.1.5"
val zioMagicVersion  = "0.3.6"
val calibanVersion   = "1.1.1"
val zioHttpVersion   = "1.0.0.0-RC17"

lazy val server = (project in file("server"))
  .settings(
    name := "server",
    scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused",
      "-Xlint:_,-missing-interpolator,-type-parameter-shadow",
      "-explaintypes",
      "-Yrangepos"
    ),
    libraryDependencies ++= Seq(
      "dev.zio"               %% "zio"              % zioVersion,
      "dev.zio"               %% "zio-streams"      % zioVersion,
      "dev.zio"               %% "zio-json"         % zioJsonVerion,
      "dev.zio"               %% "zio-optics"       % zioOpticsVersion,
      "io.github.kitlangton"  %% "zio-magic"        % zioMagicVersion,
      "com.github.ghostdogpr" %% "caliban"          % calibanVersion,
      "com.github.ghostdogpr" %% "caliban-zio-http" % calibanVersion,
      "io.d11"                %% "zhttp"            % zioHttpVersion
    )
  )
