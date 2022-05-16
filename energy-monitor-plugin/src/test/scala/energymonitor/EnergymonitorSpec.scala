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

import cats.effect.IO
import cats.syntax.all._
import energymonitor.rapl.implicits._
import io.circe.syntax._
import jRAPL.EnergyStats
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

object EnergyMonitorSpec extends SimpleIOSuite with Checkers {

  private def spin(n: Long): IO[Unit] =
    n match {
      case 0L => IO.unit
      case n  => IO.unit flatMap { _ => spin(n - 1) }
    }

  test("codec round trip") {
    forall(
      (
        Gen.listOfN[Double](7, Gen.double.filterNot(_.isNaN())),
        Gen.choose(0L, 1645818969L) // right now, while writing this test
      ).mapN { case (samples, timestamp) =>
        new EnergyStats(samples.toArray, Instant.ofEpochSecond(timestamp))
      }
    ) { stats =>
      assert(
        stats.asJson.as[EnergyStats] === Right(stats)
      )
    }
  }

  test("sampling moves forward through time") {
    val lagTime = 0.05.seconds
    val outputPath = Paths.get("./energy-test-time")
    sRAPL.preSample(outputPath) >> IO.sleep(lagTime) >> sRAPL.postSample(
      outputPath
    ) map { diff =>
      val elapsed =
        FiniteDuration(diff.getTimeElapsed().toNanos(), TimeUnit.NANOSECONDS)
      assert((elapsed - lagTime).toNanos > 0L)
    }
  }

  test("sampling registers some energy use") {
    sys.env.get("GITHUB_ACTIONS") match {
      case Some("true") =>
        IO.consoleForIO.error(
          "Sampling test skipped in CI for hardware reasons (https://github.com/47degrees/sbt-energymonitor/pull/6#issuecomment-1054567642)"
        ) >> IO { assert(true) }
      case _ =>
        val outputPath = Paths.get("./energy-test-samples-non-empty")
        sRAPL.preSample(outputPath) >> spin(1000000000L) >> sRAPL.postSample(
          outputPath
        ) map { diff =>
          assert(!diff.getPrimitiveSample().filter(_ > 0).isEmpty)
        }
    }
  }
}
