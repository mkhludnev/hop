/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hop.pipeline.transforms.solr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.JavaBinUpdateRequestCodec;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.Test;

class SolrOutputHttpTest {
  @Test
  void solrJTransmitsNestedDocumentsToTheCollectionUpdateEndpoint() throws Exception {
    LinkedBlockingQueue<SolrInputDocument> documents = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<String> paths = new LinkedBlockingQueue<>();
    HttpServer server =
        HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/",
        exchange -> {
          paths.add(exchange.getRequestURI().getPath());
          new JavaBinUpdateRequestCodec()
              .unmarshal(
                  exchange.getRequestBody(),
                  (document, request, commitWithin, override) -> documents.add(document));
          NamedList<Object> header = new NamedList<>();
          header.add("status", 0);
          NamedList<Object> response = new NamedList<>();
          response.add("responseHeader", header);
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          try (JavaBinCodec codec = new JavaBinCodec()) {
            codec.marshal(response, bytes);
          }
          byte[] body = bytes.toByteArray();
          exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
          exchange.sendResponseHeaders(200, body.length);
          try (var stream = exchange.getResponseBody()) {
            stream.write(body);
          }
        });
    server.start();
    try (HttpSolrClient client =
        new HttpSolrClient.Builder(
                "http://localhost:" + server.getAddress().getPort() + "/solr/products")
            .build()) {
      client.add(SolrDocumentParser.parse("{\"id\":\"p\",\"skus\":[{\"id\":\"s\"}]}"), 1000);
      assertEquals("/solr/products/update", paths.poll(5, TimeUnit.SECONDS));
      SolrInputDocument root = documents.poll(5, TimeUnit.SECONDS);
      assertEquals("p", root.getFieldValue("id"));
      assertEquals(
          "s",
          assertInstanceOf(SolrInputDocument.class, root.getFieldValues("skus").iterator().next())
              .getFieldValue("id"));
    } finally {
      server.stop(0);
    }
  }
}
