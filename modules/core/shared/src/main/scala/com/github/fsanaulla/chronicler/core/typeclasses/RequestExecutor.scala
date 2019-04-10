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

package com.github.fsanaulla.chronicler.core.typeclasses

/**
  * This trait define functionality for build and executing HTTP requests
  *
  * @tparam F    - Container
  * @tparam Req  - Request type
  * @tparam Resp - Response type
  */
trait RequestExecutor[F[_], Req, Resp, Uri] {

  /**
    * Implicit conversion from Uri to Request, provided to reduce boilerplate
    *
    * @param uri - Uri parameter
    * @return    - request entity
    */
  def makeRequest(uri: Uri): Req

  /**
    * Execute request
    *
    * @param request - request entity
    * @return        - Return wrapper response
    */
  def executeRequest(request: Req): F[Resp]
}
