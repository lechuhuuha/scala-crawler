package com.scala.crawler.controlplane

import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object ControlPlaneRoutes {
  val routes: Route =
    pathPrefix("api") {
      path("health") {
        get {
          complete(
            StatusCodes.OK,
            HttpEntity(
              ContentTypes.`application/json`,
              """{"service":"control-plane","status":"ok"}"""
            )
          )
        }
      }
    } ~
      path("health") {
        get {
          complete(
            StatusCodes.OK,
            HttpEntity(
              ContentTypes.`application/json`,
              """{"service":"control-plane","status":"ok"}"""
            )
          )
        }
      } ~
      pathEndOrSingleSlash {
        get {
          complete(
            StatusCodes.OK,
            HttpEntity(
              ContentTypes.`application/json`,
              """{"service":"control-plane","message":"running"}"""
            )
          )
        }
      }
}
