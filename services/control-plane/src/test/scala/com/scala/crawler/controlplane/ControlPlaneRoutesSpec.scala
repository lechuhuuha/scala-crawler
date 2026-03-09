package com.scala.crawler.controlplane

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import spray.json.JsObject

import scala.concurrent.Future
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ControlPlaneRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  private val stubApi = new ControlPlaneApi {
    override def createJob(domain: String): Future[JobResponse] =
      Future.successful(
        JobResponse(
          id = 1L,
          domain = domain,
          status = "CREATED",
          run_no = 1,
          config = JsObject("max_audit_urls" -> spray.json.JsNumber(10), "audit_runs_per_url" -> spray.json.JsNumber(3))
        )
      )

    override def getJob(jobId: Long): Future[Option[JobResponse]] =
      Future.successful(
        Some(
          JobResponse(
            id = jobId,
            domain = "example.com",
            status = "CREATED",
            run_no = 1,
            config = JsObject("max_audit_urls" -> spray.json.JsNumber(10), "audit_runs_per_url" -> spray.json.JsNumber(3))
          )
        )
      )

    override def leaseTask(request: LeaseTaskRequest): Future[Option[TaskLeaseResponse]] =
      Future.successful(None)

    override def completeTask(taskId: Long, request: CompleteTaskRequest): Future[Boolean] =
      Future.successful(true)
  }

  private val routeUnderTest = ControlPlaneRoutes.routes(stubApi)

  "ControlPlaneRoutes" should {
    "return health response" in {
      Get("/health") ~> routeUnderTest ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] should include("\"status\":\"ok\"")
      }
    }

    "create a job" in {
      val body = """{"domain":"example.com"}"""
      Post("/jobs", HttpEntity(ContentTypes.`application/json`, body)) ~> routeUnderTest ~> check {
        status shouldBe StatusCodes.Created
        responseAs[String] should include("\"domain\":\"example.com\"")
      }
    }

    "lease with no tasks without errors" in {
      Post("/tasks/lease", HttpEntity(ContentTypes.`application/json`, "{}")) ~> routeUnderTest ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] should include("\"message\":\"no_tasks\"")
      }
    }
  }
}
