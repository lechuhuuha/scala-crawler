package com.scala.crawler.controlplane

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success

object ControlPlaneServer {
  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    runMigrations(config)

    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "control-plane")
    implicit val ec: ExecutionContext = system.executionContext

    val host = config.getString("control-plane.http.host")
    val port = config.getInt("control-plane.http.port")

    val api = JdbcControlPlaneApi.fromConfig(config)
    val bindingFuture = Http().newServerAt(host, port).bind(ControlPlaneRoutes.routes(api))

    bindingFuture.onComplete {
      case Success(binding) =>
        logger.info("Control-plane started at {}", binding.localAddress)
      case Failure(ex) =>
        logger.error("Control-plane failed to start", ex)
        system.terminate()
    }

    Await.result(system.whenTerminated, Duration.Inf)
  }

  private def runMigrations(config: Config): Unit = {
    val url = config.getString("control-plane.db.url")
    val user = config.getString("control-plane.db.user")
    val password = config.getString("control-plane.db.password")

    val flyway = Flyway
      .configure()
      .baselineOnMigrate(true)
      .dataSource(url, user, password)
      .load()

    val result = flyway.migrate()
    logger.info(
      "Flyway migration completed: applied={}, schema={}",
      Int.box(result.migrationsExecuted),
      result.schemaName
    )
  }
}
