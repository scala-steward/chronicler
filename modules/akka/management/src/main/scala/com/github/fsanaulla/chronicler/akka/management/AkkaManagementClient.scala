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

package com.github.fsanaulla.chronicler.akka.management

import _root_.akka.actor.ActorSystem
import _root_.akka.http.scaladsl.HttpsConnectionContext
import com.github.fsanaulla.chronicler.akka.shared.InfluxAkkaClient
import com.github.fsanaulla.chronicler.akka.shared.alias.Request
import com.github.fsanaulla.chronicler.akka.shared.handlers.{AkkaQueryBuilder, AkkaRequestExecutor, AkkaResponseHandler}
import com.github.fsanaulla.chronicler.akka.shared.implicits._
import com.github.fsanaulla.chronicler.core.ManagementClient
import com.github.fsanaulla.chronicler.core.model._
import com.softwaremill.sttp.{Response, Uri}
import jawn.ast.JValue

import scala.concurrent.{ExecutionContext, Future}

final class AkkaManagementClient(host: String,
                                 port: Int,
                                 val credentials: Option[InfluxCredentials],
                                 httpsContext: Option[HttpsConnectionContext])
                                (implicit val ex: ExecutionContext, val system: ActorSystem)
  extends InfluxAkkaClient(httpsContext) with ManagementClient[Future, Request, Response[JValue], Uri, String] {

  implicit val qb: AkkaQueryBuilder = new AkkaQueryBuilder(host, port, credentials)
  implicit val re: AkkaRequestExecutor = new AkkaRequestExecutor
  implicit val rh: AkkaResponseHandler = new AkkaResponseHandler

  override def ping(isVerbose: Boolean = false): Future[PingResult] = {
    val queryParams = if (isVerbose) Map("verbose" -> "true") else Map.empty[String, String]
    re
      .executeRequest(re.makeRequest(qb.buildQuery("/ping", queryParams)))
      .map(rh.toPingResult)
  }
}
