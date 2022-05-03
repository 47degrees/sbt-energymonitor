package energymonitor.persistence

import io.circe.Decoder
import io.circe.Encoder
import squants.energy.Energy
import squants.energy.Joules
import squants.time.Seconds
import squants.time.Time
import cats.kernel.Eq
import java.time.Instant

final case class EnergyDiff(
    joules: Energy,
    seconds: Time,
    recordedAt: Instant,
    run: Int,
    repository: String,
    branch: String,
    tag: Option[String]
)

object EnergyDiff {
  implicit val decEnergyDiff: Decoder[EnergyDiff] = Decoder.forProduct7(
    "joules",
    "seconds",
    "recordedAt",
    "run",
    "repository",
    "branch",
    "tag"
  )({
    (
        joules: Double,
        seconds: Double,
        recordedAt: Instant,
        run: Int,
        repository: String,
        branch: String,
        tag: Option[String]
    ) =>
      EnergyDiff(
        Joules(joules),
        Seconds(seconds),
        recordedAt,
        run,
        repository,
        branch,
        tag
      )
  })

  implicit val encEnergyDiff: Encoder[EnergyDiff] =
    Encoder.forProduct7(
      "joules",
      "seconds",
      "recordedAt",
      "run",
      "repository",
      "branch",
      "tag"
    )(diff =>
      (
        diff.joules.toJoules,
        diff.seconds.toSeconds,
        diff.recordedAt,
        diff.run,
        diff.repository,
        diff.branch,
        diff.tag
      )
    )

  implicit val eqEnergyDiff: Eq[EnergyDiff] = Eq.fromUniversalEquals
}
