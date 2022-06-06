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

package energymonitor

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.apply._
import github4s.GithubConfig
import github4s.http.HttpClient
import github4s.interpreters.IssuesInterpreter
import github4s.interpreters.StaticAccessToken
import io.circe.Encoder
import io.circe.syntax._
import jRAPL.EnergyDiff
import org.http4s.Method
import org.http4s.Request
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import sbt.Keys.streams
import sbt._
import sbt.plugins.JvmPlugin

import java.nio.file.Paths
import java.time.Instant

import sRAPL.{postSample, preSample}

object EnergyMonitorPlugin extends AutoPlugin {

  /** Record an energy measurement with metadata
    *
    * This class mirrors the EnergyDiff class defined in the persistence
    * submodules, but because plugins must use the same Scala version as that
    * used to build sbt, adding a dependency would introduce additional
    * cross-publication overhead in the other submodules.
    *
    * Additionally, this is a bit looser with the types (not using squants
    * energy and time unit types), since it's private to the plugin so no one
    * can accidentally depend on it, and to avoid adding an additional
    * dependency to the plugin.
    */
  private case class EnergyMeasurement(
      joules: Double,
      seconds: Double,
      recordedAt: Instant,
      run: Int,
      organization: String,
      repository: String,
      branch: String,
      tag: Option[String]
  )

  private object EnergyMeasurement {
    // This is definitely derivable, but I don't want to play with derivation
    // in plugins.
    implicit val encEnergyMeasurement: Encoder[EnergyMeasurement] =
      Encoder.forProduct8(
        "joules",
        "seconds",
        "recordedAt",
        "run",
        "organization",
        "repository",
        "branch",
        "tag"
      )(em =>
        (
          em.joules,
          em.seconds,
          em.recordedAt,
          em.run,
          em.organization,
          em.repository,
          em.branch,
          em.tag
        )
      )
  }

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    val energyMonitorOutputFile = settingKey[String](
      "Where to write energy sampling results"
    )
    val energyMonitorDisableSampling = settingKey[Boolean](
      "Disable energy monitoring. The task keys will be available, but on invocation, they won't cause samples to be collected."
    )
    val energyMonitorPersistenceServerUrl = settingKey[String](
      """
      | Server location to post sampling results to.
      | For an example server implementation, see the energyMonitorPersistenceApp subproject
      | By default, this is set to localhost:8080, assuming that the server is running on the same host
      | as the tests. This is a very bad idea for getting reliable energy consumption results, since CPUs will consume
      | energy for the server, the tests, and anything else running on the machine where you're running the tests. So,
      | if you want to persist resluts to an HTTP server, you should run it somewhere else.""".trim.stripMargin
    )
    val energyMonitorPersistenceTag = settingKey[Option[String]](
      """
      | Tag to describe this energy monitoring run. If you're doing a lot of measurement, this can be useful
      | to provide some kind of narrative about what changed that you think might cause some significant difference.
      | """.trim.stripMargin
    )
    val energyMonitorPreSample = taskKey[Unit](
      "Collect power consumption statistics before doing work. This task writes result to the value of energyMonitorOutputFile"
    )
    val energyMonitorPostSample = taskKey[Option[EnergyDiff]](
      "Collect power consumption statistics after doing work. This task reads a previous sample from the value of energyMonitorOutputFile"
    )
    val energyMonitorPostSampleGitHub = taskKey[Unit](
      """
      | Collect power consumption statistics after doing work, and send them to a GitHub Pull Request as a comment.
      | Pull request, repository, and authentication information will be pulled from the environment.
      """.trim().stripMargin
    )
    val energyMonitorPostSampleHttp = taskKey[Unit](
      """
      | Collect power consumption statistics after doing work, and send them to an http server matching the demo implementation
      | provided in this repo. Metadata about the organization, repository, branch, and run number will be included in the request.
      | The location of the server can be controlled with the energyMonitorPersistenceServerUrl setting key.
      """.trim.stripMargin
    )

  }

  import autoImport._

  implicit val runtime = IORuntime.global

  val disabledSamplingMessage =
    "Sampling disabled, not attempting to collect energy consumption stats"

  def readPRNumberFromEnv: Option[Int] =
    sys.env.get("GITHUB_REF").flatMap { ref =>
      "refs/pull/([0-9]+)/merge".r
        .findAllIn(ref)
        .matchData
        .toList
        .headOption
        .map { m =>
          m.group(1).toInt
        }
    }

  private def buildComment(diff: EnergyDiff, attemptNumber: Int): String = {
    val samples = diff.getPrimitiveSample()
    val duration = diff.getTimeElapsed()
    val totalJoules = samples.sum
    val watts = totalJoules / (duration.toMillis().toDouble / 1000)
    f"""
  | During CI attempt ${attemptNumber}%d, this run consumed power from ${samples.size}%d CPU cores.
  |
  | The total energy consumed in joules was ${totalJoules}%.2f.
  |
  | In the sampling period, mean power consumption was ${watts}%.2f watts.
  """.trim().stripMargin
  }

  private def postComment(
      owner: String,
      repo: String,
      number: Int,
      comment: String,
      token: String
  ): IO[Unit] = {
    BlazeClientBuilder[IO].resource.use { client =>
      implicit val httpClient: HttpClient[IO] = new HttpClient(
        client,
        GithubConfig.default,
        new StaticAccessToken(Some(token))
      )
      val interpreter = new IssuesInterpreter[IO]
      interpreter.createComment(owner, repo, number, comment, Map.empty).void
    }
  }

  private def postMeasurement(
      measurement: EnergyMeasurement,
      serverUrl: Uri
  ): IO[Unit] =
    BlazeClientBuilder[IO].resource.use { client =>
      val request = Request[IO](
        Method.POST,
        serverUrl
      ).withEntity(measurement.asJson.noSpaces)
      client.run(request).use_
    }

  def preSampleTask = Def.task[Unit] {
    val log = streams.value.log
    if (energyMonitorDisableSampling.value) {
      log.info(disabledSamplingMessage)
    } else {
      preSample(Paths.get(energyMonitorOutputFile.value)).unsafeRunSync()
    }
  }

  def postSampleTask = Def.task[Option[EnergyDiff]] {
    val log = streams.value.log
    if (energyMonitorDisableSampling.value) {
      log.info(disabledSamplingMessage)
      Option.empty[EnergyDiff]
    } else {
      postSample(Paths.get(energyMonitorOutputFile.value))
        .map({ diff =>
          // logging unsafely here since otherwise there's nothing to do with the information
          log.info(buildComment(diff, -1))
          Some(diff)
        })
        .unsafeRunSync()
    }
  }

  def postSampleGitHubTask = Def.task[Unit] {
    val log = streams.value.log
    val env = sys.env
    (
      readPRNumberFromEnv,
      env.get("GITHUB_REPOSITORY"),
      env.get("GITHUB_TOKEN"),
      env.get("GITHUB_RUN_ATTEMPT") map { _.toInt }
    ).mapN { case (prNumber, repository, token, attemptNum) =>
      if (!energyMonitorDisableSampling.value) {
        val owner :: repo :: Nil = repository.split("/").toList
        postSample(Paths.get(energyMonitorOutputFile.value)) flatMap { diff =>
          val comment = buildComment(diff, attemptNum)
          postComment(owner, repo, prNumber, comment, token)
        }
      } else {
        IO {
          log.info(
            "Sampling is disabled, not attempting to POST an energy diff to GitHub"
          )
        }
      }
    }.fold(
      log.warn(
        "Could not obtain GitHub information from the environment. Check GITHUB_REF, GITHUB_REPOSITORY, and GITHUB_TOKEN env variables."
      )
    )(_.unsafeRunSync)
  }

  val postSampleHttpTask = Def.task[Unit] {
    val log = streams.value.log
    val env = sys.env
    (
      env.get("GITHUB_REPOSITORY"),
      env.get("GITHUB_RUN_ATTEMPT") map { _.toInt },
      env.get("GITHUB_REF_NAME")
    ).mapN { case (orgRepo, runAttempt, refName) =>
      val persistenceTag = energyMonitorPersistenceTag.value
      if (!energyMonitorDisableSampling.value) {
        val owner :: repo :: Nil = orgRepo.split("/").toList
        postSample(Paths.get(energyMonitorOutputFile.value)) flatMap { diff =>
          val samples = diff.getPrimitiveSample()
          val duration = diff.getTimeElapsed()
          val totalJoules = samples.sum
          val payload = EnergyMeasurement(
            totalJoules,
            duration.toMillis().toDouble / 1000d,
            Instant.now(),
            runAttempt,
            owner,
            repo,
            refName,
            persistenceTag
          )
          val serverLocation = energyMonitorPersistenceServerUrl.value
          Uri.fromString(serverLocation) match {
            case Left(e) =>
              IO(
                log.warn(
                  s"Couldn't convert provided server url to a URI. You provided: $serverLocation. The error was: $e"
                )
              ) >> IO.raiseError(e)
            case Right(uri) =>
              postMeasurement(payload, uri)

          }
        }
      } else {
        IO {
          log.info(
            "Sampling is disabled, not attempting to POST an energy diff to GitHub"
          )
        }
      }

    }.fold(
      log.warn(
        "Could not obtain GitHub information from the environment. Check GITHUB_REF, GITHUB_REPOSITORY, and GITHUB_TOKEN env variables."
      )
    )(_.unsafeRunSync())
  }

  override lazy val projectSettings = Seq(
    energyMonitorDisableSampling := energyMonitorDisableSampling.value || false,
    energyMonitorPreSample := preSampleTask.value,
    energyMonitorPostSample := postSampleTask.value,
    energyMonitorPostSampleGitHub := postSampleGitHubTask.value,
    energyMonitorPostSampleHttp := postSampleHttpTask.value,
    energyMonitorOutputFile := energyMonitorOutputFile.value,
    energyMonitorPersistenceServerUrl := energyMonitorPersistenceServerUrl.value
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq(
    energyMonitorOutputFile := "target/energy-sample",
    energyMonitorDisableSampling := false,
    energyMonitorPersistenceServerUrl := "http://localhost:8080",
    energyMonitorPersistenceTag := None
  )
}
