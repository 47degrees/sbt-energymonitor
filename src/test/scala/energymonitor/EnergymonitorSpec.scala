package energymonitor

import cats.effect.IO
import cats.syntax.all._
import energymonitor.rapl.implicits._
import io.circe.syntax._
import jRAPL.EnergyStats
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

object EnergyMonitorSpec extends SimpleIOSuite with Checkers {
  test("codec round trip") {
    forall(
      (
        Gen.listOfN[Double](7, Gen.double.filterNot(_.isNaN())),
        Gen.choose(0L, 1645818969L) // right now, while writing this test
      ).mapN { case (samples, timestamp) =>
        new EnergyStats(samples.toArray, Instant.ofEpochSecond(timestamp))
      }
    ) { stats =>
      assert(
        stats.asJson.as[EnergyStats] === Right(stats)
      )
    }
  }

  test("sampling moves forward through time") {
    val lagTime = 0.05.seconds
    val outputPath = Path.of("./energy-test")
    sRAPL.preSample(outputPath) >> IO.sleep(lagTime) >> sRAPL.postSample(
      outputPath
    ) map { diff =>
      val elapsed =
        FiniteDuration(diff.getTimeElapsed().toNanos(), TimeUnit.NANOSECONDS)
      assert((elapsed - lagTime).toNanos > 0L)
    }
  }
}
