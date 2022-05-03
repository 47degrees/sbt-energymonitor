package energymonitor.persistence

import org.scalacheck.{Gen, Arbitrary}
import org.scalacheck.Arbitrary.arbitrary
import squants.energy.Joules
import squants.time.Seconds
import java.time.Instant

trait Implicits {
  private val genEnergyDiff: Gen[EnergyDiff] = for {
    joules <- Gen.double.filter(_.isFinite)
    seconds <- Gen.double.filter(_.isFinite)
    recordedAt <- Gen.choose(0L, Int.MaxValue).map(Instant.ofEpochSecond(_))
    run <- arbitrary[Int]
    repository <- Gen.alphaStr
    branch <- Gen.alphaStr
    tag <- Gen.option(Gen.alphaStr)
  } yield EnergyDiff(
    Joules(joules),
    Seconds(seconds),
    recordedAt,
    run,
    repository,
    branch,
    tag
  )

  implicit val arbEnergyDiff: Arbitrary[EnergyDiff] = Arbitrary {
    genEnergyDiff
  }
}

object Implicits extends Implicits {}
