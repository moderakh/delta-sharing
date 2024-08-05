/*
 * Copyright (2021) The Delta Lake Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.delta.sharing.client.auth

import java.io.IOException

import org.apache.http.{HttpException, HttpResponse}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.bootstrap.{HttpServer, ServerBootstrap}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.protocol.{HttpContext, HttpRequestHandler}
import org.apache.spark.SparkFunSuite

class OAuthClientSuite extends SparkFunSuite {

  var server: HttpServer = _

  def startServer(): Unit = {
    val handler = new HttpRequestHandler {
      @throws[HttpException]
      @throws[IOException]
      override def handle(request: org.apache.http.HttpRequest,
                          response: HttpResponse, context: HttpContext): Unit = {
        val responseBody =
          """{
            | "access_token": "test-access-token",
            | "expires_in": 3600,
            | "token_type": "bearer"
            |}""".stripMargin
        response.setEntity(new StringEntity(responseBody, ContentType.APPLICATION_JSON))
        response.setStatusCode(200)
      }
    }

    server = ServerBootstrap.bootstrap()
      .setListenerPort(1080)
      .registerHandler("/token", handler)
      .create()

    server.start()
  }

  def stopServer(): Unit = {
    if (server != null) server.stop()
  }

  override def beforeAll(): Unit = {
    startServer()
  }

  override def afterAll(): Unit = {
    stopServer()
  }

  test("OAuthClient should parse token response correctly") {
    val httpClient: CloseableHttpClient = HttpClients.createDefault()
    val oauthClient = new OAuthClient(httpClient,
      "http://localhost:1080/token", "client-id", "client-secret")

    val start = System.currentTimeMillis()

    val token = oauthClient.clientCredentials()

    val end = System.currentTimeMillis()

    assert(token.accessToken == "test-access-token")
    assert(token.expiresIn == 3600)
    assert(token.creationTimestamp >= start && token.creationTimestamp <= end)
  }
}
