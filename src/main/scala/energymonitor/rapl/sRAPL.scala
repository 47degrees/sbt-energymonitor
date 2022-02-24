// MIT License

// Copyright (c) 2017-Present Rui Pereira, Marco Couto, Francisco Ribeiro, Rui Rua, Jácome Cunha, João Paulo Fernandes, João Saraiva

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

// Original source: https://github.com/WojciechMazur/Energy-Languages/blob/5ff4c3563d7437b98e00e895c9f414a10a74bc8c/Scala/sRAPL/sRAPL.scala

/** This module converts the Scala RAPL code present in
  * https://github.com/WojciechMazur/Energy-Languages to Scala 2.x. Scala 2.x is
  * required to let sbt pick the appropriate Scala version for plugin
  * compatibility.
  */

package energymonitor

import cats.effect.{Resource, IO}
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
