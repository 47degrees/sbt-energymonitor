package energymonitor.app

import energymonitor.persistence.{EnergyDiff, EnergyDiffRepository}

import cats.effect.Concurrent
import cats.effect.std.Console
import cats.syntax.all._
import fs2.io.net.Network
import natchez.Trace
import skunk.codec.all._
import skunk.implicits._
import skunk.{AppliedFragment, Decoder, Encoder, Session, Void, ~}
import squants.energy.Joules
import squants.time.Seconds

import java.time.{LocalDateTime, ZoneOffset}

class PostgresEnergyDiffRepository[F[
    _
]: Concurrent: Trace: Network: Console](
    dbHost: String,
    dbUser: String,
    dbPassword: String,
    dbName: String,
    dbPort: Int,
    poolSize: Int
) {

  private val pool = Session.pooled[F](
    host = dbHost,
    port = dbPort,
    user = dbUser,
    database = dbName,
    password = Some(dbPassword),
    max = poolSize
  )

  def repository = new EnergyDiffRepository[F] {
    private val energyDiffCols =
      (float8 ~ float8 ~ timestamp ~ int4 ~ text ~ text ~ text ~ text.opt)
    private val encEnergyDiff: Encoder[EnergyDiff] =
      energyDiffCols.contramap(diff =>
        diff.joules.toJoules ~ diff.seconds.toSeconds ~ LocalDateTime
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
            localDateTime.toInstant(ZoneOffset.UTC),
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
    ): F[EnergyDiff] = pool.use { session =>
      session.use { s =>
        s.prepare(
          sql"""
              insert into energy_measurements (id, joules, duration_seconds, recorded_at, run, organization, repository, branch, tag)
              values (
                  uuid_generate_v4(), $encEnergyDiff
              )
            """.command
        ).use { statement =>
          statement.execute(energyDiff).void.map(_ => energyDiff)
        }
      }
    }

    def listEnergyConsumptionMeasurements(
        organization: String,
        repository: String,
        branch: Option[String],
        fromRun: Option[Int],
        toRun: Option[Int]
    ): F[List[EnergyDiff]] =
      pool.use { session =>
        session.use { s =>
          val measurementsFragment =
            energyDiffsFragment(
              organization,
              repository,
              branch,
              fromRun,
              toRun
            )

          val query = measurementsFragment.fragment.query(decEnergyDiff)

          s.prepare { query }
            .use {
              _.stream(measurementsFragment.argument, 64).compile.to(List)
            }
        }

      }

    def meanEnergyConsumption(
        organization: String,
        repository: String,
        branch: String,
        fromRun: Option[Int],
        toRun: Option[Int]
    ): F[Option[Double]] = pool.use { session =>
      session.use { s =>
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

}
