import mill._, scalalib._, scalafmt._

object riscvMini extends SbtModule with ScalafmtModule {
  def scalaVersion = "2.13.18"
  def chiselVersion = "7.9.0"
  def sources = Task.Sources(moduleDir / os.up / "src" / "main" / "scala")

  def mvnDeps = Seq(
    mvn"org.chipsalliance::chisel:$chiselVersion"
  )

  def scalacPluginMvnDeps = Seq(
    mvn"org.chipsalliance:::chisel-plugin:$chiselVersion"
  )

  def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
    "-Ymacro-annotations",
    "-Ytasty-reader"
  )
}
