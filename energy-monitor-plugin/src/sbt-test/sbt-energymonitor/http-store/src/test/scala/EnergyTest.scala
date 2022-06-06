package com.fortyseven.energymonitor

import org.scalatest._
import org.scalatest.flatspec._
import org.scalatest.matchers._

class EnergyTest extends AnyFlatSpec with should.Matchers {
  private def countUpTo(acc: Long, n: Long): Unit = 
    if (acc >= n) { () } else {
      countUpTo(acc + 1L, n)
    }

  "A meaningless loop" should "consume some energy" in {
    countUpTo(0L, 100000L)
  }
}
