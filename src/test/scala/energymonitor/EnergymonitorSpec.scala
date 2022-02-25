package energymonitor

import cats.syntax.all._
import energymonitor.rapl.implicits._
import jRAPL.EnergyStats
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.cats.implicits._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import java.time.Instant

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
        scodec.Codec.decode[EnergyStats](
          scodec.Codec.encode(stats).require
        ) == Right(stats)
      )
    }
  }
}
