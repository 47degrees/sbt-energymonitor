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

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.apply._
import com.monovore.decline
import natchez.Trace.Implicits.noop
import skunk.implicits._
import skunk.codec.numeric.int4

object Opts {
  def opts: decline.Opts[DatabaseConfig] = {
    val dbHostEnv =
      decline.Opts
        .env[String]("DB_HOST", help = "host location for the database")
    val dbUserEnv = decline.Opts.env[String](
      "DB_USER",
      help = "user to use when connecting to the database"
    )
    val dbPasswordEnv = decline.Opts.env[String](
      "DB_PASSWORD",
      help = "password for user connecting to the database"
    )

    val dbPortEnv =
      decline.Opts
        .env[Int]("DB_PORT", help = "port the database is listening on")

    val dbNameEnv =
      decline.Opts
        .env[String]("DB_NAME", help = "name of the database to connect to")

    val poolSizeEnv = decline.Opts
      .env[Int](
        "DB_CONNECTION_POOL_SIZE",
        help = "how many sessions to maintain in the database connection pool"
      )
      .withDefault(8)

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
      .validate(
        "💥 Unable to connect to database, please ensure it is listening on the configured port"
      )(config =>
        skunk.Session
          .single[IO](
            config.dbHost,
            config.dbPort,
            config.dbUser,
            config.dbName,
            Some(config.dbPassword)
          )
          .use { session =>
            val testQuery = void"""select 1 from energy_measurements"""
            session
              .prepare({
                testQuery.fragment.query(int4)
              })
              .use { _.stream(testQuery.argument, 1).compile.drain }
              .attempt
              .map { _.isRight }
          }
          .unsafeRunSync()
      )
  }
}
