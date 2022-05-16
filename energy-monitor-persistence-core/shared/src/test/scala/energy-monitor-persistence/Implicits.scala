package energymonitor.persistence

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import squants.energy.Joules
import squants.time.Seconds

import java.time.Instant

trait Implicits {
  private val genEnergyDiff: Gen[EnergyDiff] = for {
    joules <- Gen.double.filter(_.isFinite)
    seconds <- Gen.double.filter(_.isFinite)
    recordedAt <- Gen.choose(0L, Int.MaxValue).map(Instant.ofEpochSecond(_))
    run <- arbitrary[Int]
    organization <- Gen.alphaStr.filter(_.nonEmpty)
    repository <- Gen.alphaStr.filter(_.nonEmpty)
    branch <- Gen.alphaStr.filter(_.nonEmpty)
    tag <- Gen.option(Gen.alphaStr.filter(_.nonEmpty))
  } yield EnergyDiff(
    Joules(joules),
    Seconds(seconds),
    recordedAt,
    run,
    organization,
    repository,
    branch,
    tag
  )

  implicit val arbEnergyDiff: Arbitrary[EnergyDiff] = Arbitrary {
    genEnergyDiff
  }
}

object Implicits extends Implicits {}
