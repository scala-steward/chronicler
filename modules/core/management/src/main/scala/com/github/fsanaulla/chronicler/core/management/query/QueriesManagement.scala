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

package com.github.fsanaulla.chronicler.core.management.query

import com.github.fsanaulla.chronicler.core.alias.{ErrorOr, ResponseCode}
import com.github.fsanaulla.chronicler.core.components._
import com.github.fsanaulla.chronicler.core.management.ManagementResponseHandler
import com.github.fsanaulla.chronicler.core.typeclasses.{FunctionK, MonadError}

/** Created by Author: fayaz.sanaulla@gmail.com Date: 19.08.17
  */
trait QueriesManagement[F[_], G[_], Req, Resp, U, E] extends QueriesManagementQuery[U] {
  implicit val qb: QueryBuilder[U]
  implicit val rb: RequestBuilder[Req, U, E]
  implicit val re: RequestExecutor[F, Req, Resp]
  implicit val rh: ManagementResponseHandler[G, Resp]
  implicit val ME: MonadError[F, Throwable]

  implicit val FK: FunctionK[G, F]

  /** Show list of queries */
  final def showQueries: F[ErrorOr[Array[QueryInfo]]] = {
    val uri  = showQuerysQuery
    val req  = rb.get(uri, compress = false)
    val resp = re.execute(req)

    ME.flatMap(resp)(resp => FK(rh.queryResult[QueryInfo](resp)))
  }

  /** Kill query */
  final def killQuery(queryId: Int): F[ErrorOr[ResponseCode]] = {
    val uri  = killQueryQuery(queryId)
    val req  = rb.get(uri, compress = false)
    val resp = re.execute(req)

    ME.flatMap(resp)(resp => FK(rh.writeResult(resp)))
  }
}
