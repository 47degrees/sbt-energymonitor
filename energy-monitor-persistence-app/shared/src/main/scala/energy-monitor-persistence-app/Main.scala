package energymonitor.app

import energymonitor.persistence.Routes

import cats.effect.std
import cats.effect.{ExitCode, IO, IOApp}
import com.monovore.decline.Command
import natchez.Trace.Implicits.noop
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.{AutoSlash, Logger}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val appCommand = Command(
      "energymonitor-server",
      "Store enegy measurements for a GitHub repository behind a REST API"
    )(DatabaseConfig.opts)
    appCommand
      .parse(args, sys.env) match {
      case Right(config) =>
        val repo = new PostgresEnergyDiffRepository[IO](
          config.dbHost,
          config.dbUser,
          config.dbPassword,
          config.dbName,
          config.dbPort,
          config.poolSize
        ).repository
        val router =
          Logger.httpApp[IO](false, false)(
            Router(
              "/" -> AutoSlash(new Routes[IO](repo).routes)
            ).orNotFound
          )

        EmberServerBuilder.default[IO].withHttpApp(router).build.use { _ =>
          IO.never
        }
      case Left(help) =>
        std.Console[IO].println(help.toString()).as(ExitCode.Error)
    }

  }

}
