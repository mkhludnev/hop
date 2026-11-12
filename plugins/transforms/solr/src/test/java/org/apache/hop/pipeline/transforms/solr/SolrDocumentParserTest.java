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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.JavaBinCodec;
import org.junit.jupiter.api.Test;

class SolrDocumentParserTest {
  @Test
  void convertsNamedAndUnnamedNestedDocumentsAndScalarArrays() throws Exception {
    SolrInputDocument root =
        SolrDocumentParser.parse(
            """
            {"id":"p1","price":3.5,"tags":["a","b"],"available":true,
             "skus":[{"id":"s1","manual":{"id":"m1"}},{"id":"s2"}],
             "_childDocuments_":[{"id":"legacy"}]}
            """);

    assertEquals("p1", root.getFieldValue("id"));
    assertEquals(3.5, root.getFieldValue("price"));
    assertEquals(true, root.getFieldValue("available"));
    assertEquals(List.of("a", "b"), new ArrayList<>(root.getFieldValues("tags")));
    List<Object> skus = new ArrayList<>(root.getFieldValues("skus"));
    assertEquals(2, skus.size());
    SolrInputDocument sku = assertInstanceOf(SolrInputDocument.class, skus.get(0));
    assertEquals("s1", sku.getFieldValue("id"));
    assertEquals(
        "m1",
        assertInstanceOf(SolrInputDocument.class, sku.getFieldValue("manual")).getFieldValue("id"));
    assertEquals("legacy", root.getChildDocuments().get(0).getFieldValue("id"));
  }

  @Test
  void nestedDocumentsSurviveSolrJWireSerialization() throws Exception {
    SolrInputDocument input =
        SolrDocumentParser.parse(
            "{\"id\":\"p\",\"children\":[{\"id\":\"c\",\"nested\":{\"id\":\"n\"}}]}");
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (JavaBinCodec codec = new JavaBinCodec()) {
      codec.marshal(input, bytes);
    }
    SolrInputDocument output;
    try (JavaBinCodec codec = new JavaBinCodec()) {
      output = assertInstanceOf(SolrInputDocument.class, codec.unmarshal(bytes.toByteArray()));
    }
    SolrInputDocument child =
        assertInstanceOf(
            SolrInputDocument.class, output.getFieldValues("children").iterator().next());
    assertEquals(
        "n",
        assertInstanceOf(SolrInputDocument.class, child.getFieldValue("nested"))
            .getFieldValue("id"));
  }

  @Test
  void rejectsInvalidDocumentsWithoutSilentlyChangingTheirShape() {
    for (String json :
        new String[] {
          null,
          "",
          "[]",
          "{} {}",
          "{\"id\":1,\"id\":2}",
          "{\"id\":9223372036854775808}",
          "{\"number\":1e9999}",
          "{\"children\":[{\"id\":\"a\"},\"b\"]}",
          "{\"x\":[[1]]}",
          "{\"_childDocuments_\":[1]}"
        }) {
      assertThrows(IOException.class, () -> SolrDocumentParser.parse(json), String.valueOf(json));
    }
  }
}
