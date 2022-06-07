/*
 * Copyright 2022 47 Degrees Open Source <https://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
