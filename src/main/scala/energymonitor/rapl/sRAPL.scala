package energymonitor

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import jRAPL._

import java.io._
import java.nio.file.Path

package object sRAPL {

  /** Obtain a managed energy monitor
    *
    * This method creates a new SyncEnergyMonitor (from jRAPL), activates it,
    * and, using `cats-effect`'s `Resource` abstraction, will deactivate the
    * monitor after using it in some effectful action.
    *
    * Since jRAPL requires a Linux system in order to do any work, it might not
    * be possible to acquire a monitor, (e.g. if you invoke the command on a
    * Mac). Since this is likely to be a pretty common occurrence, the method
    * doesn't throw in such a case, but returns a None. It's up to callers of
    * this method to determine what to do in that case -- this plugin will log a
    * message and continue.
    */
  def monitorResource: Resource[IO, Option[SyncEnergyMonitor]] =
    Resource.make({
      for {
        os <- IO { System.getProperty("os.name").toLowerCase() }
        monitor <- (os === "linux").guard[Option] traverse { _ =>
          IO {
            val monitor = new SyncEnergyMonitor()
            monitor.activate()
            monitor
          }
        }
      } yield monitor
    })(monitor => IO(monitor map { _.deactivate() }).void)

  /** Sample energy statistics and serialize results to disk
    *
    * This method is written to be called *before* doing other work. For
    * example, when invoked via sbt as the energy-pre-sample command, you might
    * have it in a set of commands like:
    *
    * {{{
    *       ****
    * sbt> pre-sample-energy; scalafmtCheck; scalafix --check; test; post-sample-energy
    * }}}
    *
    * @param outputPath
    *   a path to serialize results to
    */
  def preSample(outputPath: Path): IO[Unit] = ???

  /** Sample energy after having done other work
    *
    * This method is written to be called *after* doing other work. For example,
    * when invoked via sbt as the energy-post-sample command, you might have it
    * in a set of commands like:
    * {{{
    *                                                                   ****
    * sbt> pre-sample-energy; scalafmtCheck; scalafix; --check; test; post-sample-energy
    * }}}
    *
    * @param previousResultsPath
    *   where results from a previous `preSample` run were written
    * @return
    *   energy consumption statistics since the previous run
    */
  def postSample(previousResultsPath: Path): IO[EnergyDiff] = ???
}
