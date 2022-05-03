package energymonitor.app

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = IO.unit.as(ExitCode.Success)
}
