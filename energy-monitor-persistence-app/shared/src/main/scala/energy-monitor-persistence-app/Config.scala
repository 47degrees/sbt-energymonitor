package energymonitor.app

import cats.effect.std
import cats.effect.{Concurrent, Resource}
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import skunk.Session

final case class DatabaseConfig(
    dbHost: String,
    dbUser: String,
    dbPassword: String,
    dbPort: Int,
    dbName: String,
    poolSize: Int
) {
  def makePool[F[_]: Concurrent: Network: std.Console]
      : Resource[F, Resource[F, Session[F]]] =
    Session.pooled[F](
      dbHost,
      dbPort,
      dbUser,
      dbName,
      Some(dbPassword),
      poolSize
    )
}
