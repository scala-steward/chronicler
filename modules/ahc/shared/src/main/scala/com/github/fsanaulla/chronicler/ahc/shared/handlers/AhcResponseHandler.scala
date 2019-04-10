/*
 * Copyright 2017-2019 Faiaz Sanaulla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.fsanaulla.chronicler.ahc.shared.handlers

import com.github.fsanaulla.chronicler.ahc.shared.implicits._
import com.github.fsanaulla.chronicler.core.either
import com.github.fsanaulla.chronicler.core.model._
import com.github.fsanaulla.chronicler.core.typeclasses.ResponseHandler
import com.softwaremill.sttp.Response
import jawn.ast.{JArray, JValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

private[ahc] class AhcResponseHandler(jsHandler: AhcJsonHandler)(implicit ex: ExecutionContext)
  extends ResponseHandler[Future, Response[JValue]] {

  override def toPingResult(response: Response[JValue]): Future[PingResult] = {
    response.code match {
      case code if isPingCode(code) =>
        jsHandler.pingHeaders(response).map { case (build, version) =>
          PingResult.successful(code, code == 200, build, version)
        }
      case other =>
        errorHandler(response, other)
          .map(ex => PingResult.failed(other, ex))
    }
  }

  // Simply result's
  override def toResult(response: Response[JValue]): Future[Either[Throwable, Int]] = {
    response.code match {
      case code if isSuccessful(code) && code != 204 =>
        jsHandler.responseErrorOpt(response) map {
          case Some(msg) =>
            WriteResult.failed(code, new OperationException(msg))
          case _ =>
            WriteResult.successful(code)
        }
      case 204 =>
        WriteResult.successfulFuture(204)
      case other =>
        errorHandler(response, other)
          .map(ex => WriteResult.failed(other, ex))
    }
  }

  override def toComplexQueryResult[A: ClassTag: InfluxReader, B: ClassTag](response: Response[JValue],
                                                                            f: (String, Array[A]) => B): Future[QueryResult[B]] = {
    response.code match {
      case code if isSuccessful(code) =>
        jsHandler.responseBody(response)
          .map(jsHandler.groupedSystemInfo[A])
          .map {
            case Some(arr) =>
              QueryResult.successful[B](
                code,
                arr.map { case (dbName, queries) => f(dbName, queries) }
              )
            case _ =>
              QueryResult.empty[B](code)
          }
      case other =>
        queryErrorHandler[B](response, other)
    }
  }

  override def toQueryJsResult(response: Response[JValue]): Future[QueryResult[JArray]] = {
    response.code.intValue() match {
      case code if isSuccessful(code) =>
        jsHandler.responseBody(response)
          .map(jsHandler.queryResult)
          .map {
            case Some(arr) =>
              QueryResult.successful[JArray](code, arr)
            case _ =>
              QueryResult.empty[JArray](code)
          }
      case other =>
        queryErrorHandler[JArray](response, other)
    }
  }

  override def toGroupedJsResult(response: Response[JValue]): Future[GroupedResult[JArray]] = {
    response.code.intValue() match {
      case code if isSuccessful(code) =>
        jsHandler.responseBody(response)
          .map(jsHandler.gropedResult)
          .map {
            case Some(arr) =>
              GroupedResult.successful[JArray](code, arr)
            case _ =>
              GroupedResult.empty[JArray](code)}
      case other =>
        errorHandler(response, other)
          .map(ex => GroupedResult.failed[JArray](other, ex))
    }
  }

  override def toBulkQueryJsResult(response: Response[JValue]): Future[QueryResult[Array[JArray]]] = {
    response.code.intValue() match {
      case code if isSuccessful(code) =>
        jsHandler.responseBody(response)
          .map(jsHandler.bulkResult)
          .map {
            case Some(seq) =>
              QueryResult.successful[Array[JArray]](code, seq)
            case _ =>
              QueryResult.empty[Array[JArray]](code)
          }
      case other =>
        queryErrorHandler[Array[JArray]](response, other)
    }
  }

  override def toQueryResult[A: ClassTag: InfluxReader](response: Response[JValue]): Future[Either[Throwable, QueryResult[A]]] =
    toQueryJsResult(response)
      .map(_.map(InfluxReader[A].read))
      .map(either.array(_))


  override def errorHandler(response: Response[JValue],
                            code: Int): Future[InfluxException] = code match {
    case 400 =>
      jsHandler.responseError(response).map(errMsg => new BadRequestException(errMsg))
    case 401 =>
      jsHandler.responseError(response).map(errMsg => new AuthorizationException(errMsg))
    case 404 =>
      jsHandler.responseError(response).map(errMsg => new ResourceNotFoundException(errMsg))
    case code: Int if code < 599 && code >= 500 =>
      jsHandler.responseError(response).map(errMsg => new InternalServerError(errMsg))
    case _ =>
      jsHandler.responseError(response).map(errMsg => new UnknownResponseException(errMsg))
  }

  private[this] def queryErrorHandler[A: ClassTag](response: Response[JValue],
                                                   code: Int): Future[QueryResult[A]] =
    errorHandler(response, code).map(ex => QueryResult.failed[A](code, ex))
}
