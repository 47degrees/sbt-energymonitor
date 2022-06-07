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

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import squants.energy.Joules
import squants.time.Seconds

import java.time.Instant

trait Implicits {
  private val genEnergyDiff: Gen[EnergyDiff] = for {
    joules <- Gen.double.filter(_.isFinite)
    seconds <- Gen.double.filter(_.isFinite)
    recordedAt <- Gen
      .choose(0L, Int.MaxValue.toLong)
      .map(Instant.ofEpochSecond(_))
    run <- arbitrary[Int]
    organization <- Gen.alphaStr.filter(_.nonEmpty)
    repository <- Gen.alphaStr.filter(_.nonEmpty)
    branch <- Gen.alphaStr.filter(_.nonEmpty)
    tag <- Gen.option(Gen.alphaStr.filter(_.nonEmpty))
  } yield EnergyDiff(
    Joules(joules),
    Seconds(seconds),
    recordedAt,
    run,
    organization,
    repository,
    branch,
    tag
  )

  implicit val arbEnergyDiff: Arbitrary[EnergyDiff] = Arbitrary {
    genEnergyDiff
  }
}

object Implicits extends Implicits {}
