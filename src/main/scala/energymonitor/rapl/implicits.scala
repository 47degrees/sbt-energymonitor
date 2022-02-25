package energymonitor.rapl

import cats.Show
import cats.syntax.all._
import jRAPL.EnergyStats
import scodec.codecs
import scodec.codecs.implicits._

import java.time.Instant

object implicits {

  implicit val instantCodec: scodec.Codec[Instant] = scodec
    .Codec[Long]
    .xmap(
      Instant.ofEpochSecond,
      _.getEpochSecond()
    )

  implicit val listDoubleCodec: scodec.Codec[List[Double]] =
    codecs.list(scodec.Codec[Double])

  implicit val energyStatsCodec: scodec.Codec[EnergyStats] =
    scodec
      .Codec[(List[Double], Instant)]
      .xmap(
        { case (samples, timestamp) =>
          new EnergyStats(samples.toArray, timestamp)
        },
        stats => (stats.getPrimitiveSample().toList, stats.getTimestamp())
      )

  implicit val showEnergyStats: Show[EnergyStats] =
    Show[(List[Double], Long)].imap(
      { case (samples, timestamp) =>
        new EnergyStats(samples.toArray, Instant.ofEpochSecond(timestamp))
      }
    )(stats =>
      (stats.getPrimitiveSample().toList, stats.getTimestamp().getEpochSecond())
    )
}
