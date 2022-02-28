package energymonitor

import cats.effect.unsafe.IORuntime
import jRAPL.EnergyDiff
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
  }

  import autoImport._
  implicit val runtime = IORuntime.global
  val disabledSamplingMessage =
    "Sampling disabled, not attempting to collect energy consumption stats"

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
        .map(Some(_))
        .unsafeRunSync()
    }
  }

  override lazy val projectSettings = Seq(
    energyMonitorDisableSampling := energyMonitorDisableSampling.value || false,
    energyMonitorPreSample := preSampleTask.value,
    energyMonitorPostSample := postSampleTask.value,
    energyMonitorOutputFile := energyMonitorOutputFile.value
  )

  override lazy val buildSettings = Seq()

  override lazy val globalSettings = Seq(
    energyMonitorOutputFile := "target/energy-sample"
  )
}
