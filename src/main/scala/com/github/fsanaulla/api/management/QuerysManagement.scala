package com.github.fsanaulla.api.management

import com.github.fsanaulla.handlers.{QueryHandler, RequestHandler, ResponseHandler}
import com.github.fsanaulla.model.{QueryInfo, QueryResult, Result}
import com.github.fsanaulla.query.QuerysManagementQuery

import scala.concurrent.Future

/**
  * Created by
  * Author: fayaz.sanaulla@gmail.com
  * Date: 19.08.17
  */
private[fsanaulla] trait QuerysManagement[R, U, M, E] extends QuerysManagementQuery[U] {
  self: RequestHandler[R, U, M, E] with ResponseHandler[R] with QueryHandler[U] =>

  def showQueries(): Future[QueryResult[QueryInfo]] = {
    buildRequest(showQuerysQuery()).flatMap(toQueryResult[QueryInfo])
  }

  def killQuery(queryId: Int): Future[Result] = {
    buildRequest(killQueryQuery(queryId)).flatMap(toResult)
  }
}
