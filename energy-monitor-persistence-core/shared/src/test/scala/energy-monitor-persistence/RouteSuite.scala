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

import energymonitor.persistence.Implicits._

import cats.effect.{IO, Ref}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits._
import org.http4s.{EntityDecoder, Response, Status, Uri}
import org.http4s.{Method, Request}
import org.scalacheck.effect.PropF
import squants.energy.Joules

class RouteSuite extends CatsEffectSuite with ScalaCheckEffectSuite {

  private def makeRoutes(diffs: List[EnergyDiff]) = Ref
    .of[IO, List[EnergyDiff]](diffs)
    .map(ref => new Routes[IO](TestRepository.ofDiffsRef(ref)).routes)

  private def check[A](
      resp: Response[IO],
      expectedStatus: Status,
      expectedBody: Option[A]
  )(implicit
      ev: EntityDecoder[IO, A]
  ): IO[Boolean] = for {
    decoded <- resp.as[A]
  } yield {
    resp.status == expectedStatus
    expectedBody.fold(true)(body => decoded == body)

  }

  test(
    "posting a new energy diff returns a 201 Created and the created energy diff measurement"
  ) {

    PropF.forAllF { (diff: EnergyDiff) =>
      for {
        routes <- makeRoutes(Nil)
        request = Request[IO](
          Method.POST,
          uri"/"
        ).withEntity(diff)
        resp <- routes.run(request).value
        result <- resp match {
          case None => IO(fail("service did not handle request"))
          case Some(response) =>
            check(response, Status.Created, Some(diff)).map(assert(_))
        }
      } yield result
    }

  }

  test(
    "listing energy diffs for an organization and repository returns them all with a 200 OK"
  ) {
    PropF.forAllF { (diff: EnergyDiff) =>
      for {
        routes <- makeRoutes(List(diff))
        request = Request[IO](
          Method.GET,
          Uri.unsafeFromString(s"/${diff.organization}/${diff.repository}")
        )
        resp <- routes.run(request).value
        result <- resp match {
          case None => IO(fail("service did not handle request"))
          case Some(response) =>
            check(response, Status.Ok, Some(List(diff))).map(assert(_))
        }
      } yield result
    }
  }

  test(
    "listing energy diffs with filters that exclude them all returns a 200 OK and an empty list"
  ) {
    for {
      routes <- makeRoutes(Nil)
      request = Request[IO](
        Method.GET,
        uri"/bogus/bogus?branch=bogus"
      )
      resp <- routes.run(request).value
      result <- resp match {
        case None => IO(fail("service did not handle request"))
        case Some(response) =>
          check(response, Status.Ok, Some(List.empty[EnergyDiff]))
            .map(assert(_))
      }
    } yield result
  }

  test(
    "calculating the mean returns a 200 OK in a JSON object"
  ) {
    PropF.forAllF { (diff: EnergyDiff) =>
      for {
        routes <- makeRoutes(
          List(diff, diff.copy(joules = diff.joules + Joules(1d)))
        )
        request = Request[IO](
          Method.GET,
          Uri.unsafeFromString(
            s"/${diff.organization}/${diff.repository}/mean/${diff.branch}"
          )
        )
        resp <- routes.run(request).value
        result <- resp match {
          case None => IO(fail("service did not handle request"))
          case Some(response) =>
            response.as[Map[String, Double]] map { decoded =>
              val meanO = decoded.get("meanConsumption")
              // check is no good here, since it wants exact equality, which is
              // hard for doubles, so instead this makes sure that the difference from expectation
              // is very small
              assert(
                response.status == Status.Ok &&
                  meanO.isDefined &&
                  diff.joules.toJoules + 0.5 - meanO.getOrElse(0d) < 0.00000001
              )
            }
        }
      } yield result
    }
  }
}
