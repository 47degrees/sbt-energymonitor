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

package energymonitor.rapl

import cats.Eq
import cats.Show
import cats.syntax.all._
import io.circe.Decoder
import io.circe.Encoder
import jRAPL.EnergyStats

import java.time.Instant

object implicits {

  private def energyStatsFromCatsCompat(
      tup: (List[Double], Long)
  ) =
    new EnergyStats(tup._1.toArray, Instant.ofEpochSecond(tup._2))

  private def energyStatsToCatsCompat(
      stats: EnergyStats
  ) = (stats.getPrimitiveSample().toList, stats.getTimestamp().getEpochSecond())

  implicit val energyStatsEncoder: Encoder[EnergyStats] = Encoder.forProduct2(
    "samples",
    "timestamp"
  )(stats => (stats.getPrimitiveSample(), stats.getTimestamp()))

  implicit val energyStatsDecoder: Decoder[EnergyStats] = Decoder.forProduct2(
    "samples",
    "timestamp"
  )((samples: Array[Double], timestamp: Instant) =>
    new EnergyStats(samples, timestamp)
  )

  implicit val showEnergyStats: Show[EnergyStats] =
    Show[(List[Double], Long)]
      .imap(energyStatsFromCatsCompat)(energyStatsToCatsCompat)

  implicit val eqEnergyStats: Eq[EnergyStats] = Eq[(List[Double], Long)]
    .imap(energyStatsFromCatsCompat)(energyStatsToCatsCompat)
}
