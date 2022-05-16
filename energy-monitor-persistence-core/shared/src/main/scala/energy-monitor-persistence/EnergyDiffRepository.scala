package energymonitor.persistence

trait EnergyDiffRepository[F[_]] {

  /** Persist a new measurement in the repository.
    *
    * Measurements (energy consumption in joules and runtime in seconds) are
    * tagged with GitHub organizations and repositories and branch information.
    */
  def storeEnergyConsumptionMeasurement(energyDiff: EnergyDiff): F[EnergyDiff]

  def listEnergyConsumptionMeasurements(
      organization: String,
      repository: String,
      branch: Option[String],
      fromRun: Option[Int],
      toRun: Option[Int]
  ): F[List[EnergyDiff]]

  def meanEnergyConsumption(
      organization: String,
      repository: String,
      branch: String,
      fromRun: Option[Int],
      toRun: Option[Int]
  ): F[Option[Double]]
}
