package energymonitor.persistence

import cats.effect.Concurrent
import cats.syntax.flatMap._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl

class Routes[F[_]: Concurrent](
    energyDiffRepository: EnergyDiffRepository[F]
) extends Http4sDsl[F] {

  implicit val energyDiffCodec = jsonOf[F, EnergyDiff]

  private object BranchParam
      extends OptionalQueryParamDecoderMatcher[String]("branch")

  private object FromRunParam
      extends OptionalQueryParamDecoderMatcher[Int]("from-run")

  private object ToRunParam
      extends OptionalQueryParamDecoderMatcher[Int]("to-run")

  val routes: HttpRoutes[F] = HttpRoutes.of {
    case req @ POST -> Root =>
      req
        .as[EnergyDiff]
        .flatMap { energyDiffRepository.storeEnergyConsumptionMeasurement(_) }
        .flatMap { diff => Created(diff) }

    case GET -> Root / organization / repository :? BranchParam(
          branch
        ) :? FromRunParam(
          fromRun
        ) :? ToRunParam(toRun) =>
      energyDiffRepository
        .listEnergyConsumptionMeasurements(
          organization,
          repository,
          branch,
          fromRun,
          toRun
        )
        .flatMap { Ok(_) }

    case GET -> Root / organization / repository / branch :? FromRunParam(
          fromRun
        ) :? ToRunParam(toRun) =>
      energyDiffRepository
        .meanEnergyConsumption(organization, repository, branch, fromRun, toRun)
        .flatMap { mean =>
          Ok(Map("meanConsumption" -> mean))
        }

  }
}
