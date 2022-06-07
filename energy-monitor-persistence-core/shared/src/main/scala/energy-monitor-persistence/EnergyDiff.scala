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

package energymonitor.persistence

import cats.kernel.Eq
import io.circe.{Decoder, Encoder}
import squants.energy.{Energy, Joules}
import squants.time.{Seconds, Time}

import java.time.Instant

final case class EnergyDiff(
    joules: Energy,
    seconds: Time,
    recordedAt: Instant,
    run: Int,
    organization: String,
    repository: String,
    branch: String,
    tag: Option[String]
)

object EnergyDiff {
  implicit val decEnergyDiff: Decoder[EnergyDiff] = Decoder.forProduct8(
    "joules",
    "seconds",
    "recordedAt",
    "run",
    "organization",
    "repository",
    "branch",
    "tag"
  )({
    (
        joules: Double,
        seconds: Double,
        recordedAt: Instant,
        run: Int,
        organization: String,
        repository: String,
        branch: String,
        tag: Option[String]
    ) =>
      EnergyDiff(
        Joules(joules),
        Seconds(seconds),
        recordedAt,
        run,
        organization,
        repository,
        branch,
        tag
      )
  })

  implicit val encEnergyDiff: Encoder[EnergyDiff] =
    Encoder.forProduct8(
      "joules",
      "seconds",
      "recordedAt",
      "run",
      "organization",
      "repository",
      "branch",
      "tag"
    )(diff =>
      (
        diff.joules.toJoules,
        diff.seconds.toSeconds,
        diff.recordedAt,
        diff.run,
        diff.organization,
        diff.repository,
        diff.branch,
        diff.tag
      )
    )

  implicit val eqEnergyDiff: Eq[EnergyDiff] = Eq.fromUniversalEquals
}
