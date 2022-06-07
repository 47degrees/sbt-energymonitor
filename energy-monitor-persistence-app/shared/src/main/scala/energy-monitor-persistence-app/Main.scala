/*
 * Copyright 2022 47 Degrees Open Source <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
