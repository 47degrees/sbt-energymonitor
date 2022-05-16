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

package energymonitor

import energymonitor.rapl.implicits._

import cats.MonadError
import cats.effect.{IO, Resource}
import cats.syntax.all._
import io.circe.parser._
import io.circe.syntax._
import jRAPL._

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.Base64

package object sRAPL {

  /** Obtain a managed energy monitor
    *
    * This method creates a new SyncEnergyMonitor (from jRAPL), activates it,
    * and, using `cats-effect`'s `Resource` abstraction, will deactivate the
    * monitor after using it in some effectful action.
    *
    * Since jRAPL requires a Linux system in order to do any work, it might not
    * be possible to acquire a monitor, (e.g. if you invoke the command on a
    * Mac). Since this is likely to be a pretty common occurrence, the method
    * doesn't throw in such a case, but returns a None. It's up to callers of
    * this method to determine what to do in that case -- this plugin will log a
    * message and continue.
    */
  def monitorResource: Resource[IO, Option[SyncEnergyMonitor]] =
    Resource.make({
      for {
        os <- IO { System.getProperty("os.name").toLowerCase() }
        monitor <- (os === "linux").guard[Option] traverse { _ =>
          IO {
            val monitor = new SyncEnergyMonitor()
            monitor.activate()
            monitor
          }
        }
      } yield monitor
    })(monitor => IO(monitor map { _.deactivate() }).void)

  private def sample: IO[Option[EnergyStats]] = monitorResource.use {
    _ traverse { monitor =>
      IO {
        monitor
          .getSample()
      }
    }
  }

  /** Sample energy statistics and serialize results to disk
    *
    * This method is written to be called *before* doing other work. For
    * example, when invoked via sbt as the energy-pre-sample command, you might
    * have it in a set of commands like:
    *
    * {{{
    *       ****
    * sbt> pre-sample-energy; scalafmtCheck; scalafix --check; test; post-sample-energy
    * }}}
    *
    * @param outputPath
    *   a path to serialize results to
    */
  def preSample(outputPath: Path): IO[Unit] =
    sample.flatMap {
      _ traverse_ { stats =>
        IO {
          Files.write(
            outputPath,
            Base64
              .getEncoder()
              .encode(
                stats.asJson.noSpaces
                  .getBytes(StandardCharsets.UTF_8)
              )
          )
        }
      }
    }

  /** Sample energy after having done other work
    *
    * This method is written to be called *after* doing other work. For example,
    * when invoked via sbt as the energy-post-sample command, you might have it
    * in a set of commands like:
    * {{{
    *                                                                   ****
    * sbt> pre-sample-energy; scalafmtCheck; scalafix; --check; test; post-sample-energy
    * }}}
    *
    * @param previousResultsPath
    *   where results from a previous `preSample` run were written
    * @return
    *   energy consumption statistics since the previous run
    */
  def postSample(previousResultsPath: Path): IO[EnergyDiff] = for {
    input <- IO { Files.readAllBytes(previousResultsPath) }
    decoded = new String(
      Base64.getDecoder().decode(input),
      StandardCharsets.UTF_8
    )
    priorStats <- MonadError[IO, Throwable].fromEither(
      decode[EnergyStats](decoded)
    )
    newStatsO <- sample
    newStats <- MonadError[IO, Throwable].fromOption(
      newStatsO,
      new Exception(
        """
        | Unable to fetch new energy statistics. if this is running on a Mac, that's fine, otherwise,
        | please open an issue at
        | https://github.com/47degrees/sbt-energymonitor/issues""".trim.stripMargin
      )
    )
  } yield {
    EnergyDiff.between(priorStats, newStats)
  }
}
