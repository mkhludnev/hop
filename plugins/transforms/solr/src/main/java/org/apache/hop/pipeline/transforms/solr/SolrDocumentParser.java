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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.solr.common.SolrInputDocument;

final class SolrDocumentParser {
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  private SolrDocumentParser() {}

  static SolrInputDocument parse(String json) throws IOException {
    if (json == null) {
      throw new IOException("The document is null");
    }
    JsonNode root = MAPPER.readTree(json);
    if (root == null || !root.isObject()) {
      throw new IOException("The document must be a JSON object");
    }
    return document(root);
  }

  private static SolrInputDocument document(JsonNode node) throws IOException {
    SolrInputDocument doc = new SolrInputDocument();
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      JsonNode value = field.getValue();
      if ("_childDocuments_".equals(field.getKey())) {
        if (!value.isArray()) {
          throw new IOException("_childDocuments_ must be an array of objects");
        }
        for (JsonNode child : value) {
          if (!child.isObject()) {
            throw new IOException("_childDocuments_ must contain only objects");
          }
          doc.addChildDocument(document(child));
        }
      } else {
        doc.setField(field.getKey(), fieldValue(value));
      }
    }
    return doc;
  }

  private static Object fieldValue(JsonNode value) throws IOException {
    if (value.isObject()) {
      return document(value);
    }
    if (value.isArray()) {
      List<Object> values = new ArrayList<>();
      Boolean documents = null;
      for (JsonNode element : value) {
        if (element.isArray()) {
          throw new IOException("Nested arrays are not supported");
        }
        boolean isDocument = element.isObject();
        if (documents != null && documents != isDocument) {
          throw new IOException("A field array cannot mix objects and scalar values");
        }
        documents = isDocument;
        values.add(fieldValue(element));
      }
      return values;
    }
    if (value.isTextual()) {
      return value.textValue();
    }
    if (value.isBoolean()) {
      return value.booleanValue();
    }
    if (value.isNumber()) {
      if (value.isIntegralNumber() && !value.canConvertToLong()) {
        throw new IOException("Integer is outside the 64-bit range");
      }
      if (value.isFloatingPointNumber() && !Double.isFinite(value.doubleValue())) {
        throw new IOException("Floating-point number is outside the supported range");
      }
      return value.numberValue();
    }
    if (value.isNull()) {
      return null;
    }
    throw new IOException("Unsupported JSON value: " + value.getNodeType());
  }
}
