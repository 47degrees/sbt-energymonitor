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

import cats.effect.Concurrent
import cats.syntax.flatMap._
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl

class Routes[F[_]: Concurrent](
    energyDiffRepository: EnergyDiffRepository[F]
) extends Http4sDsl[F] {

  implicit val energyDiffCodec: EntityDecoder[F, EnergyDiff] =
    jsonOf[F, EnergyDiff]

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

    case GET -> Root / organization / repository / "mean" / branch :? FromRunParam(
          fromRun
        ) :? ToRunParam(toRun) =>
      energyDiffRepository
        .meanEnergyConsumption(organization, repository, branch, fromRun, toRun)
        .flatMap { mean =>
          Ok(Map("meanConsumption" -> mean))
        }
  }
}
