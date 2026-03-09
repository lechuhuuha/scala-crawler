package com.scala.crawler.controlplane

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ControlPlaneRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  "ControlPlaneRoutes" should {
    "return health response" in {
      Get("/health") ~> ControlPlaneRoutes.routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] should include("\"status\":\"ok\"")
      }
    }
  }
}
