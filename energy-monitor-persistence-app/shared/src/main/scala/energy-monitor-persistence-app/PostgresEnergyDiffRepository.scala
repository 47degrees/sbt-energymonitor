package energymonitor.app

import energymonitor.persistence.EnergyDiff
import energymonitor.persistence.EnergyDiffRepository

import cats.effect.Concurrent
import cats.effect.std.Console
import cats.syntax.functor._
import fs2.io.net.Network
import natchez.Trace
import skunk.codec.all._
import skunk.implicits._
import skunk.{Decoder, Encoder, Session, ~}
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

    val decEnergyDiff: Decoder[EnergyDiff] =
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

    def storeEnergyConsumptionMeasurement(
        energyDiff: EnergyDiff
    ): F[EnergyDiff] = pool.use { session =>
      session.use { s =>
        s.prepare(
          sql"""
              insert into measurements (id, joules, duration_seconds, recorded_at, run, organization, repository, branch, tag)
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
    ): F[List[EnergyDiff]] = ???

    def meanEnergyConsumption(
        organization: String,
        repository: String,
        branch: String,
        fromRun: Option[Int],
        toRun: Option[Int]
    ): F[Option[Double]] = ???

  }

}
