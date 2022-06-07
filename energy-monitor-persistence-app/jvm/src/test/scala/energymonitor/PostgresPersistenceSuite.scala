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

package energymonitor.app

import cats.effect.{IO, Resource}
import munit.CatsEffectSuite
import energymonitor.persistence.EnergyDiff
import squants.energy.Joules
import squants.time.Seconds
import java.time.Instant
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import org.flywaydb.core.Flyway

class PostgresPersistenceSuite extends CatsEffectSuite {

  test("database interaction functions work") {
    val diff = EnergyDiff(
      Joules(23d),
      Seconds(19d),
      Instant.now,
      5,
      "org",
      "repo",
      "branch",
      Some("a tag")
    )

    useMigratedPostgresContainer.use { container =>
      val databaseConfig = DatabaseConfig(
        "localhost",
        "energy_monitor",
        "energy_monitor",
        container.mappedPort(5432),
        "energy_monitor",
        1
      )

      databaseConfig.makePool[IO].use { pool =>
        val repo = PostgresEnergyDiffRepository.repository[IO](pool)
        for {
          stored <- repo.storeEnergyConsumptionMeasurement(diff)
          retrieved <- repo.listEnergyConsumptionMeasurements(
            diff.organization,
            diff.repository,
            None,
            None,
            None
          )
          mean <- repo.meanEnergyConsumption(
            diff.organization,
            diff.repository,
            diff.branch,
            None,
            None
          )
        } yield {
          assertEquals(stored, diff)
          assertEquals(retrieved, List(diff))
          assert(mean == Some(23d))
        }
      }
    }
  }

  private def useMigratedPostgresContainer: Resource[IO, PostgreSQLContainer] =
    Resource.make[IO, PostgreSQLContainer]({
      val container = PostgreSQLContainer(
        DockerImageName.parse("postgres:14.2"),
        databaseName = "energy_monitor",
        username = "energy_monitor",
        password = "energy_monitor"
      )
      IO(container.start()) *> IO(
        Flyway
          .configure()
          .dataSource(
            s"jdbc:postgresql://localhost:${container.mappedPort(5432)}/energy_monitor",
            "energy_monitor",
            "energy_monitor"
          )
          .failOnMissingLocations(true)
          .locations({
            val loc =
              s"filesystem:./energy-monitor-persistence-app/shared/src/main/resources/migrations"
            loc
          })
          .load()
          .migrate()
      ) map { _ => container }
    })(container => IO(container.close()))
}
