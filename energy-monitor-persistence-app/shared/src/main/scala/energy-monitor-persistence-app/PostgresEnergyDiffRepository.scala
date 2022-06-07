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

import energymonitor.persistence.{EnergyDiff, EnergyDiffRepository}

import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import skunk.codec.all._
import skunk.implicits._
import skunk.{AppliedFragment, Decoder, Encoder, Session, Void, ~}
import squants.energy.Joules
import squants.time.Seconds

import java.time.{OffsetDateTime, ZoneOffset}

object PostgresEnergyDiffRepository {

  def repository[F[_]: Concurrent](
      sessionResource: Resource[F, Session[F]]
  ) =
    new EnergyDiffRepository[F] {
      private val energyDiffCols =
        (float8 ~ float8 ~ timestamptz ~ int4 ~ text ~ text ~ text ~ text.opt)
      private val encEnergyDiff: Encoder[EnergyDiff] =
        energyDiffCols.contramap(diff =>
          diff.joules.toJoules ~ diff.seconds.toSeconds ~ OffsetDateTime
            .ofInstant(
              diff.recordedAt,
              ZoneOffset.UTC
            ) ~ diff.run ~ diff.organization ~ diff.repository ~ diff.branch ~ diff.tag
        )

      private val decEnergyDiff: Decoder[EnergyDiff] =
        energyDiffCols.map {
          case joules ~ seconds ~ localDateTime ~ run ~ organization ~ repository ~ branch ~ tag =>
            EnergyDiff(
              Joules(joules),
              Seconds(seconds),
              localDateTime.toInstant,
              run,
              organization,
              repository,
              branch,
              tag
            )
        }

      private def energyDiffsFragment(
          organization: String,
          repository: String,
          branch: Option[String],
          fromRun: Option[Int],
          toRun: Option[Int]
      ) = {
        val base = sql"""
            select joules, duration_seconds, recorded_at, run, organization, repository, branch, tag
            from energy_measurements
          """
        val orgFilter = sql"organization = $text"
        val repositoryFilter = sql"repository = $text"
        val branchFilter = sql"branch = $text"
        val fromRunFilter = sql"run >= $int4"
        val toRunFilter = sql"run <= $int4"
        val conditions = List(
          Some(orgFilter(organization)),
          Some(repositoryFilter(repository)),
          branch.map(branchFilter),
          fromRun.map(fromRunFilter),
          toRun.map(toRunFilter)
        ).flatten

        val filter = conditions.foldSmash(
          void" WHERE ",
          void" AND ",
          AppliedFragment.empty
        )

        base(Void) |+| filter
      }

      def storeEnergyConsumptionMeasurement(
          energyDiff: EnergyDiff
      ): F[EnergyDiff] =
        sessionResource.use { s =>
          s.prepare(
            sql"""
              insert into energy_measurements (id, joules, duration_seconds, recorded_at, run, organization, repository, branch, tag)
              values (
                  gen_random_uuid(), $encEnergyDiff
              )
            """.command
          ).use { statement =>
            statement.execute(energyDiff).void.map(_ => energyDiff)
          }
        }

      def listEnergyConsumptionMeasurements(
          organization: String,
          repository: String,
          branch: Option[String],
          fromRun: Option[Int],
          toRun: Option[Int]
      ): F[List[EnergyDiff]] =
        sessionResource.use { s =>
          val measurementsFragment =
            energyDiffsFragment(
              organization,
              repository,
              branch,
              fromRun,
              toRun
            )

          val query = measurementsFragment.fragment.query(decEnergyDiff)

          s.prepare(query)
            .use { preparedQuery =>
              preparedQuery
                .stream(measurementsFragment.argument, 64)
                .compile
                .to(List)
            }
        }

      def meanEnergyConsumption(
          organization: String,
          repository: String,
          branch: String,
          fromRun: Option[Int],
          toRun: Option[Int]
      ): F[Option[Double]] =
        sessionResource.use { s =>
          val measurementsFragment: AppliedFragment =
            energyDiffsFragment(
              organization,
              repository,
              Some(branch),
              fromRun,
              toRun
            )

          s.prepare(measurementsFragment.fragment.query(decEnergyDiff))
            .use {
              _.stream(measurementsFragment.argument, 256)
                .fold((0, 0d))((acc: (Int, Double), diff: EnergyDiff) => {
                  val (count, sum) = acc
                  (count + 1, sum + diff.joules.toJoules)
                })
                .compile
                .to(List)
                .map(_.headOption.flatMap {
                  case (0, _) => None
                  case (c, s) => Some(s / c.toDouble)
                })
            }
        }

    }

}
