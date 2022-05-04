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
