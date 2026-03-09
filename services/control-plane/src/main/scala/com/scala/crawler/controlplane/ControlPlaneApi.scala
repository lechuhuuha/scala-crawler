package com.scala.crawler.controlplane

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.typesafe.config.Config
import spray.json.DefaultJsonProtocol
import spray.json.JsObject
import spray.json.JsonParser
import spray.json.RootJsonFormat

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.util.Using

final case class HealthResponse(service: String, status: String)
final case class HomeResponse(service: String, message: String)
final case class ErrorResponse(error: String, message: String)

final case class CreateJobRequest(domain: String)
final case class JobResponse(
  id: Long,
  domain: String,
  status: String,
  run_no: Int,
  config: JsObject
)
final case class PlaceholderResponse(job_id: Long, status: String, message: String)

final case class LeaseTaskRequest(worker_id: Option[String], lease_ttl_seconds: Option[Int])
final case class TaskLeaseResponse(
  id: Long,
  job_id: Long,
  status: String,
  task_type: String,
  payload_json: JsObject
)
final case class LeaseTaskResponse(task: Option[TaskLeaseResponse], message: String)

final case class CompleteTaskRequest(result_json: Option[JsObject], last_error: Option[String])
final case class CompleteTaskResponse(task_id: Long, status: String, message: String)

object ApiJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val healthResponseFormat: RootJsonFormat[HealthResponse] = jsonFormat2(HealthResponse)
  implicit val homeResponseFormat: RootJsonFormat[HomeResponse] = jsonFormat2(HomeResponse)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat2(ErrorResponse)

  implicit val createJobRequestFormat: RootJsonFormat[CreateJobRequest] = jsonFormat1(CreateJobRequest)
  implicit val jobResponseFormat: RootJsonFormat[JobResponse] = jsonFormat5(JobResponse)
  implicit val placeholderResponseFormat: RootJsonFormat[PlaceholderResponse] =
    jsonFormat3(PlaceholderResponse)

  implicit val leaseTaskRequestFormat: RootJsonFormat[LeaseTaskRequest] = jsonFormat2(LeaseTaskRequest)
  implicit val taskLeaseResponseFormat: RootJsonFormat[TaskLeaseResponse] = jsonFormat5(TaskLeaseResponse)
  implicit val leaseTaskResponseFormat: RootJsonFormat[LeaseTaskResponse] = jsonFormat2(LeaseTaskResponse)

  implicit val completeTaskRequestFormat: RootJsonFormat[CompleteTaskRequest] =
    jsonFormat2(CompleteTaskRequest)
  implicit val completeTaskResponseFormat: RootJsonFormat[CompleteTaskResponse] =
    jsonFormat3(CompleteTaskResponse)
}

trait ControlPlaneApi {
  def createJob(domain: String): Future[JobResponse]
  def getJob(jobId: Long): Future[Option[JobResponse]]
  def leaseTask(request: LeaseTaskRequest): Future[Option[TaskLeaseResponse]]
  def completeTask(taskId: Long, request: CompleteTaskRequest): Future[Boolean]
}

final class JdbcControlPlaneApi(dbUrl: String, dbUser: String, dbPassword: String)(
  implicit ec: ExecutionContext
) extends ControlPlaneApi {

  override def createJob(domain: String): Future[JobResponse] = Future(blocking {
    val normalizedDomain = domain.trim
    require(normalizedDomain.nonEmpty, "domain must not be empty")

    withConnection { conn =>
      val sql =
        """INSERT INTO jobs(domain, status)
          |VALUES (?, 'CREATED')
          |RETURNING id, domain, status, run_no, config_json
          |""".stripMargin

      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setString(1, normalizedDomain)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) toJobResponse(rs)
          else throw new IllegalStateException("failed to create job")
        }
      }
    }
  })

  override def getJob(jobId: Long): Future[Option[JobResponse]] = Future(blocking {
    withConnection { conn =>
      val sql = "SELECT id, domain, status, run_no, config_json FROM jobs WHERE id = ?"
      Using.resource(conn.prepareStatement(sql)) { ps =>
        ps.setLong(1, jobId)
        Using.resource(ps.executeQuery()) { rs =>
          if (rs.next()) Some(toJobResponse(rs)) else None
        }
      }
    }
  })

  override def leaseTask(request: LeaseTaskRequest): Future[Option[TaskLeaseResponse]] = Future(blocking {
    val workerId = request.worker_id.map(_.trim).filter(_.nonEmpty).getOrElse("worker-unknown")
    val leaseTtlSeconds = request.lease_ttl_seconds.filter(_ > 0).getOrElse(300)

    withConnection { conn =>
      conn.setAutoCommit(false)
      try {
        val maybeTask = selectPendingTask(conn)

        maybeTask.foreach { task =>
          val updateSql =
            """UPDATE tasks
              |SET status = 'LEASED',
              |    lease_owner = ?,
              |    leased_at = NOW(),
              |    lease_expires_at = NOW() + (? * INTERVAL '1 second'),
              |    updated_at = NOW()
              |WHERE id = ?
              |""".stripMargin

          Using.resource(conn.prepareStatement(updateSql)) { ps =>
            ps.setString(1, workerId)
            ps.setInt(2, leaseTtlSeconds)
            ps.setLong(3, task.id)
            ps.executeUpdate()
          }
        }

        conn.commit()
        maybeTask.map(t => t.copy(status = "LEASED"))
      } catch {
        case ex: Throwable =>
          conn.rollback()
          throw ex
      } finally {
        conn.setAutoCommit(true)
      }
    }
  })

  override def completeTask(taskId: Long, request: CompleteTaskRequest): Future[Boolean] = Future(blocking {
    withConnection { conn =>
      val sql =
        """UPDATE tasks
          |SET status = 'DONE',
          |    result_json = ?::jsonb,
          |    last_error = ?,
          |    completed_at = NOW(),
          |    updated_at = NOW()
          |WHERE id = ?
          |""".stripMargin

      Using.resource(conn.prepareStatement(sql)) { ps =>
        request.result_json match {
          case Some(result) => ps.setString(1, result.compactPrint)
          case None         => ps.setNull(1, Types.OTHER)
        }

        request.last_error match {
          case Some(error) => ps.setString(2, error)
          case None        => ps.setNull(2, Types.VARCHAR)
        }

        ps.setLong(3, taskId)
        ps.executeUpdate() > 0
      }
    }
  })

  private def selectPendingTask(conn: Connection): Option[TaskLeaseResponse] = {
    val sql =
      """SELECT id, job_id, status, task_type, payload_json
        |FROM tasks
        |WHERE status = 'PENDING'
        |ORDER BY id ASC
        |FOR UPDATE SKIP LOCKED
        |LIMIT 1
        |""".stripMargin

    Using.resource(conn.prepareStatement(sql)) { ps =>
      Using.resource(ps.executeQuery()) { rs =>
        if (rs.next()) {
          Some(
            TaskLeaseResponse(
              id = rs.getLong("id"),
              job_id = rs.getLong("job_id"),
              status = rs.getString("status"),
              task_type = rs.getString("task_type"),
              payload_json = parseJsonObject(rs.getString("payload_json"))
            )
          )
        } else None
      }
    }
  }

  private def toJobResponse(rs: ResultSet): JobResponse = {
    JobResponse(
      id = rs.getLong("id"),
      domain = rs.getString("domain"),
      status = rs.getString("status"),
      run_no = rs.getInt("run_no"),
      config = parseJsonObject(rs.getString("config_json"))
    )
  }

  private def parseJsonObject(raw: String): JsObject = {
    val text = Option(raw).map(_.trim).filter(_.nonEmpty)
    text
      .flatMap { value =>
        try {
          JsonParser(value) match {
            case obj: JsObject => Some(obj)
            case _             => None
          }
        } catch {
          case _: Throwable => None
        }
      }
      .getOrElse(JsObject.empty)
  }

  private def withConnection[T](f: Connection => T): T = {
    Using.resource(DriverManager.getConnection(dbUrl, dbUser, dbPassword)) { conn =>
      f(conn)
    }
  }
}

object JdbcControlPlaneApi {
  def fromConfig(config: Config)(implicit ec: ExecutionContext): JdbcControlPlaneApi = {
    new JdbcControlPlaneApi(
      dbUrl = config.getString("control-plane.db.url"),
      dbUser = config.getString("control-plane.db.user"),
      dbPassword = config.getString("control-plane.db.password")
    )
  }
}
