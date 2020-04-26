// build.sc
import mill._, scalalib._, scalafmt._

object ecs extends ScalaModule with ScalafmtModule {
  def name                  = "elevator-control-system"
  override def scalaVersion = "2.13.1"

  override def mainClass = Some("com.ecs.ECSApp")

  val catscoreVersion   = "2.0.0"
  val pureconfigVersion = "0.12.3"
  val scalatestVersion  = "3.1.1"
  val zioVersion        = "1.0.0-RC18-2"

  override def ivyDeps =
    Agg(
      ivy"dev.zio::zio:$zioVersion",
      ivy"org.typelevel::cats-core:$catscoreVersion",
      ivy"com.github.pureconfig::pureconfig:$pureconfigVersion"
    )

  object test extends Tests {
    override def ivyDeps =
      Agg(
        ivy"org.scalatest::scalatest:${scalatestVersion}"
      )
    override def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}
