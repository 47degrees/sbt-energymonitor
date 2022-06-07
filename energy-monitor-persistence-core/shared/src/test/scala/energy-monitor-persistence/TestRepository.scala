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

import energymonitor.persistence.EnergyDiffRepository
import cats.effect.{Concurrent, Ref}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._

object TestRepository {
  def ofDiffsRef[F[_]: Concurrent](
      diffs: Ref[F, List[EnergyDiff]]
  ): EnergyDiffRepository[F] =
    new EnergyDiffRepository[F] {

      override def storeEnergyConsumptionMeasurement(
          energyDiff: EnergyDiff
      ): F[EnergyDiff] = diffs.update(_ :+ energyDiff) >> energyDiff.pure[F]

      override def listEnergyConsumptionMeasurements(
          organization: String,
          repository: String,
          branch: Option[String],
          fromRun: Option[Int],
          toRun: Option[Int]
      ): F[List[EnergyDiff]] = diffs.get.map(
        _.filter { diff =>
          diff.organization == organization &&
          diff.repository == repository &&
          branch.fold(true)(diff.branch == _) &&
          fromRun.fold(true)(diff.run >= _) &&
          toRun.fold(true)(diff.run <= _)
        }
      )

      override def meanEnergyConsumption(
          organization: String,
          repository: String,
          branch: String,
          fromRun: Option[Int],
          toRun: Option[Int]
      ): F[Option[Double]] = listEnergyConsumptionMeasurements(
        organization,
        repository,
        Some(branch),
        fromRun,
        toRun
      ).map { diffs =>
        diffs.headOption.fold(Option.empty[Double])(_ =>
          Some(diffs.map(_.joules.toJoules).sum / diffs.size.toDouble)
        )
      }
    }
}
