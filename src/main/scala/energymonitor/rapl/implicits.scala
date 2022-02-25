package energymonitor.rapl

import cats.Eq
import cats.Show
import cats.syntax.all._
import io.circe.Decoder
import io.circe.Encoder
import jRAPL.EnergyStats

import java.time.Instant

object implicits {

  private def energyStatsFromCatsCompat(
      tup: (List[Double], Long)
  ) =
    new EnergyStats(tup._1.toArray, Instant.ofEpochSecond(tup._2))

  private def energyStatsToCatsCompat(
      stats: EnergyStats
  ) = (stats.getPrimitiveSample().toList, stats.getTimestamp().getEpochSecond())

  implicit val energyStatsEncoder: Encoder[EnergyStats] = Encoder.forProduct2(
    "samples",
    "timestamp"
  )(stats => (stats.getPrimitiveSample(), stats.getTimestamp()))

  implicit val energyStatsDecoder: Decoder[EnergyStats] = Decoder.forProduct2(
    "samples",
    "timestamp"
  )((samples: Array[Double], timestamp: Instant) =>
    new EnergyStats(samples, timestamp)
  )

  implicit val showEnergyStats: Show[EnergyStats] =
    Show[(List[Double], Long)]
      .imap(energyStatsFromCatsCompat)(energyStatsToCatsCompat)

  implicit val eqEnergyStats: Eq[EnergyStats] = Eq[(List[Double], Long)]
    .imap(energyStatsFromCatsCompat)(energyStatsToCatsCompat)
}
