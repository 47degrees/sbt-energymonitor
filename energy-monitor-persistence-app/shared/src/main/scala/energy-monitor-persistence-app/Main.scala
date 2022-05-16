package energymonitor.app

import energymonitor.persistence.Routes

import cats.effect.std
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s._
import com.monovore.decline.Command
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val appCommand = Command(
      "energymonitor-server",
      "Store enegy measurements for a GitHub repository behind a REST API"
    )(Opts.opts)

    appCommand
      .parse(args, sys.env) match {
      case Right(config) =>
        (for {
          pool <- config.makePool[IO]
          repo = PostgresEnergyDiffRepository.repository[IO](pool)
          router = Logger.httpApp[IO](false, false)(
            new Routes[IO](repo).routes.orNotFound
          )
          server <- EmberServerBuilder
            .default[IO]
            .withHttpApp(router)
            .withHost(ipv4"0.0.0.0")
            .build

        } yield server)
          .use { _ => IO.never[ExitCode] }

      case Left(help) =>
        std.Console[IO].println(help.toString()).as(ExitCode.Error)
    }

  }

}
