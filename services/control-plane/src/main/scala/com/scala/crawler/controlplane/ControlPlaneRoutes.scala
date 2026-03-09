package com.scala.crawler.controlplane

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.scala.crawler.controlplane.ApiJsonProtocol._

import scala.util.Failure
import scala.util.Success

object ControlPlaneRoutes {
  private val healthResponse = HealthResponse(service = "control-plane", status = "ok")
  private val homeResponse = HomeResponse(service = "control-plane", message = "running")

  val healthRoutes: Route =
    pathPrefix("api") {
      path("health") {
        get {
          complete(StatusCodes.OK, healthResponse)
        }
      }
    } ~
      path("health") {
        get {
          complete(StatusCodes.OK, healthResponse)
        }
      } ~
      pathEndOrSingleSlash {
        get {
          complete(StatusCodes.OK, homeResponse)
        }
      }

  def routes(api: ControlPlaneApi): Route =
    healthRoutes ~
      pathPrefix("jobs") {
        pathEndOrSingleSlash {
          post {
            entity(as[CreateJobRequest]) { request =>
              val domain = request.domain.trim
              if (domain.isEmpty) {
                complete(
                  StatusCodes.BadRequest,
                  ErrorResponse("invalid_request", "domain must not be empty")
                )
              } else {
                onComplete(api.createJob(domain)) {
                  case Success(job) =>
                    complete(StatusCodes.Created, job)
                  case Failure(ex) =>
                    complete(
                      StatusCodes.InternalServerError,
                      ErrorResponse("create_job_failed", Option(ex.getMessage).getOrElse("unknown error"))
                    )
                }
              }
            }
          }
        } ~
          pathPrefix(LongNumber) { jobId =>
            pathEndOrSingleSlash {
              get {
                onComplete(api.getJob(jobId)) {
                  case Success(Some(job)) =>
                    complete(StatusCodes.OK, job)
                  case Success(None) =>
                    complete(
                      StatusCodes.NotFound,
                      ErrorResponse("not_found", s"job $jobId not found")
                    )
                  case Failure(ex) =>
                    complete(
                      StatusCodes.InternalServerError,
                      ErrorResponse("get_job_failed", Option(ex.getMessage).getOrElse("unknown error"))
                    )
                }
              }
            } ~
              path("report") {
                get {
                  onComplete(api.getJob(jobId)) {
                    case Success(Some(job)) =>
                      complete(
                        StatusCodes.OK,
                        PlaceholderResponse(
                          job_id = jobId,
                          status = job.status,
                          message = "report endpoint placeholder"
                        )
                      )
                    case Success(None) =>
                      complete(
                        StatusCodes.NotFound,
                        ErrorResponse("not_found", s"job $jobId not found")
                      )
                    case Failure(ex) =>
                      complete(
                        StatusCodes.InternalServerError,
                        ErrorResponse(
                          "report_failed",
                          Option(ex.getMessage).getOrElse("unknown error")
                        )
                      )
                  }
                }
              } ~
              path("resume") {
                post {
                  onComplete(api.getJob(jobId)) {
                    case Success(Some(job)) =>
                      complete(
                        StatusCodes.Accepted,
                        PlaceholderResponse(
                          job_id = jobId,
                          status = job.status,
                          message = "resume endpoint placeholder"
                        )
                      )
                    case Success(None) =>
                      complete(
                        StatusCodes.NotFound,
                        ErrorResponse("not_found", s"job $jobId not found")
                      )
                    case Failure(ex) =>
                      complete(
                        StatusCodes.InternalServerError,
                        ErrorResponse(
                          "resume_failed",
                          Option(ex.getMessage).getOrElse("unknown error")
                        )
                      )
                  }
                }
              }
          }
      } ~
      pathPrefix("tasks") {
        path("lease") {
          post {
            entity(as[LeaseTaskRequest]) { request =>
              onComplete(api.leaseTask(request)) {
                case Success(Some(task)) =>
                  complete(StatusCodes.OK, LeaseTaskResponse(task = Some(task), message = "leased"))
                case Success(None) =>
                  complete(StatusCodes.OK, LeaseTaskResponse(task = None, message = "no_tasks"))
                case Failure(ex) =>
                  complete(
                    StatusCodes.InternalServerError,
                    ErrorResponse("lease_failed", Option(ex.getMessage).getOrElse("unknown error"))
                  )
              }
            }
          }
        } ~
          path(LongNumber / "complete") { taskId =>
            post {
              entity(as[CompleteTaskRequest]) { request =>
                onComplete(api.completeTask(taskId, request)) {
                  case Success(true) =>
                    complete(
                      StatusCodes.OK,
                      CompleteTaskResponse(task_id = taskId, status = "DONE", message = "task completed")
                    )
                  case Success(false) =>
                    complete(
                      StatusCodes.NotFound,
                      ErrorResponse("not_found", s"task $taskId not found")
                    )
                  case Failure(ex) =>
                    complete(
                      StatusCodes.InternalServerError,
                      ErrorResponse("complete_failed", Option(ex.getMessage).getOrElse("unknown error"))
                    )
                }
              }
            }
          }
      }
}
