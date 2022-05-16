package energymonitor.persistence

import energymonitor.persistence.Implicits._
import io.circe.testing.{
  ArbitraryInstances => CirceArbitraryInstances,
  CodecTests
}
import munit.DisciplineSuite

class CodecSuite extends DisciplineSuite with CirceArbitraryInstances {
  checkAll("EnergyDiff Codec", CodecTests[EnergyDiff].codec)
}
