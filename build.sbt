// If you have JDK 6 and not JDK 7 then replace all three instances of the number 7 to the number 6

organization := "com.micronautics"

name := "vfs-s3Test"

version := "0.1.1-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.7", "-unchecked",
    "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")

scalacOptions in (Compile, doc) <++= baseDirectory.map {
  (bd: File) => Seq[String](
     "-sourcepath", bd.getAbsolutePath,
     "-doc-source-url", "https://github.com/mslinn/changeMe/tree/master€{FILE_PATH}.scala"
  )
}

javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-g:vars")

resolvers ++= Seq(
  "Typesafe Releases"   at "http://repo.typesafe.com/typesafe/releases",
  bintray.Opts.resolver.mavenRepo("abashev")
)

libraryDependencies ++= Seq(
  "com.intridea.io"    %  "vfs-s3"      % "2.0.0" withSources(),
  "org.apache.commons" % "commons-vfs2" % "2.0"  withSources(),
  "commons-io"         % "commons-io"   % "2.4" withSources(),
//
  "org.scalatest"      %% "scalatest"       % "2.2.0" % "test" withSources()
)

logLevel := Level.Warn

// define the statements initially evaluated when entering 'console', 'console-quick', or 'console-project'
initialCommands := """
                     |""".stripMargin

// Only show warnings and errors on the screen for compilations.
// This applies to both test:compile and compile and is Info by default
logLevel in compile := Level.Warn

cancelable := true

sublimeTransitive := true

seq(bintrayResolverSettings:_*)
