package energymonitor

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.apply._
import github4s.GithubConfig
import github4s.http.HttpClient
import github4s.interpreters.IssuesInterpreter
import github4s.interpreters.StaticAccessToken
import jRAPL.EnergyDiff
import org.http4s.blaze.client.BlazeClientBuilder
import sbt.Keys.streams
import sbt._
import sbt.plugins.JvmPlugin

import java.nio.file.Paths

import sRAPL.{postSample, preSample}

object EnergyMonitorPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    val energyMonitorOutputFile = settingKey[String](
      "Where to write energy sampling results"
    )
    val energyMonitorDisableSampling = settingKey[Boolean](
      "Disable energy monitoring. The task keys will be available, but on invocation, they won't cause samples to be collected."
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
    val watts = totalJoules / duration.getSeconds().toDouble
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

  override lazy val projectSettings = Seq(
    energyMonitorDisableSampling := energyMonitorDisableSampling.value || false,
    energyMonitorPreSample := preSampleTask.value,
    energyMonitorPostSample := postSampleTask.value,
    energyMonitorPostSampleGitHub := postSampleGitHubTask.value,
    energyMonitorOutputFile := energyMonitorOutputFile.value
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq(
    energyMonitorOutputFile := "target/energy-sample",
    energyMonitorDisableSampling := false
  )
}
