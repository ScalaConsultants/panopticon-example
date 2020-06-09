lazy val akkaHttpVersion   = "10.1.12"
lazy val akkaVersion       = "2.6.5"
lazy val slickVersion      = "3.3.2"
lazy val zioVersion        = "1.0.0-RC20"
lazy val zioLoggingVersion = "0.3.0"
lazy val zioConfigVersion  = "1.0.0-RC20"
lazy val calibanVersion    = "0.8.1"
lazy val zioZMXVersion     = "0.0.2"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.example",
      scalaVersion := "2.13.1"
    )
  ),
  name := "panopticon-example",
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  libraryDependencies ++= Seq(
    "com.typesafe.akka"     %% "akka-http"                   % akkaHttpVersion,
    "de.heikoseeberger"     %% "akka-http-play-json"         % "1.32.0",
    "com.typesafe.akka"     %% "akka-actor-typed"            % akkaVersion,
    "com.typesafe.akka"     %% "akka-stream"                 % akkaVersion,
    "com.typesafe.slick"    %% "slick"                       % slickVersion,
    "com.typesafe.slick"    %% "slick-hikaricp"              % slickVersion,
    "dev.zio"               %% "zio"                         % zioVersion,
    "dev.zio"               %% "zio-config"                  % zioConfigVersion,
    "dev.zio"               %% "zio-config-magnolia"         % zioConfigVersion,
    "dev.zio"               %% "zio-config-typesafe"         % zioConfigVersion,
    "dev.zio"               %% "zio-zmx"                     % zioZMXVersion,
    "io.scalac"             %% "zio-akka-http-interop"       % "0.1.0",
    "io.scalac"             %% "zio-slick-interop"           % "0.1.0",
    "io.scalac"             %% "akka-periscope-core"         % "0.3.1",
    "io.scalac"             %% "akka-periscope-akka-http"    % "0.3.1",
    "dev.zio"               %% "zio-interop-reactivestreams" % "1.0.3.5-RC8",
    "ch.qos.logback"        % "logback-classic"              % "1.2.3",
    "dev.zio"               %% "zio-logging"                 % zioLoggingVersion,
    "dev.zio"               %% "zio-logging-slf4j"           % zioLoggingVersion,
    "com.h2database"        % "h2"                           % "1.4.200",
    "com.github.ghostdogpr" %% "caliban"                     % calibanVersion,
    "com.github.ghostdogpr" %% "caliban-akka-http"           % calibanVersion,
    "com.typesafe.akka"     %% "akka-http-testkit"           % akkaHttpVersion % Test,
    "com.typesafe.akka"     %% "akka-actor-testkit-typed"    % akkaVersion % Test,
    "dev.zio"               %% "zio-test-sbt"                % zioVersion % Test
  ),
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
)
