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

import cats.syntax.apply._
import com.monovore.decline

object Opts {

  def opts: decline.Opts[DatabaseConfig] = {
    val dbHostEnv =
      decline.Opts
        .option[String](
          "db-host",
          short = "h",
          help = "host location for the database"
        )

    val dbUserEnv = decline.Opts.option[String](
      "db-user",
      short = "u",
      help = "user to use when connecting to the database"
    )
    val dbPasswordEnv = decline.Opts.option[String](
      "db-password",
      help = "password for user connecting to the database"
    )

    val dbPortEnv =
      decline.Opts
        .option[Int](
          "db-port",
          short = "p",
          help = "port the database is listening on"
        )

    val dbNameEnv =
      decline.Opts
        .option[String](
          "db-name",
          short = "n",
          help = "name of the database to connect to"
        )

    val poolSizeEnv = decline.Opts
      .option[Int](
        "db-connection-pool-size",
        short = "s",
        help = "how many sessions to maintain in the database connection pool"
      )
      .withDefault(8)

    // It's impossible to unsafely run this synchronously in the JS app because
    // JS doesn't support blocking (and attempting to block or call Await.result transitively
    // is expected to result in a linking error:
    // https://www.scala-js.org/doc/project/linking-errors.html#using-blocking-apis-eg-awaitresult)
    // As a result the JS app just won't check that the database parameters are correct.
    // Unfortunate, but maybe a sign that this validation should move into an app startup database
    // healthcheck instead of command line config parsing
    // https://github.com/47degrees/sbt-energymonitor/issues/36
    (dbHostEnv, dbUserEnv, dbPasswordEnv, dbPortEnv, dbNameEnv, poolSizeEnv)
      .mapN { case (host, user, password, port, name, pool) =>
        DatabaseConfig(
          host,
          user,
          password,
          port,
          name,
          pool
        )
      }
  }
}
