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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILoggingObject;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.RowMeta;
import org.apache.hop.core.row.value.ValueMetaString;
import org.apache.hop.core.variables.Variables;
import org.apache.hop.pipeline.transforms.mock.TransformMockHelper;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SolrOutputTest {
  private TransformMockHelper<SolrOutputMeta, SolrOutputData> helper;
  private SolrClient client;
  private SolrOutput transform;
  private IRowMeta rowMeta;

  @BeforeEach
  void setUp() throws Exception {
    helper = new TransformMockHelper<>("SolrOutput", SolrOutputMeta.class, SolrOutputData.class);
    when(helper.logChannelFactory.create(any(), any(ILoggingObject.class)))
        .thenReturn(helper.iLogChannel);
    when(helper.pipeline.isRunning()).thenReturn(true);
    when(helper.transformMeta.isPartitioned()).thenReturn(false);
    helper.pipeline.setVariables(new Variables());
    SolrOutputMeta meta = new SolrOutputMeta();
    meta.setDocumentField("json");
    SolrOutputData data = new SolrOutputData();
    data.commitWithin = 1000;
    client = mock(SolrClient.class);
    data.client = client;
    transform =
        spy(
            new SolrOutput(
                helper.transformMeta, meta, data, 0, helper.pipelineMeta, helper.pipeline));
    rowMeta = new RowMeta();
    rowMeta.addValueMeta(new ValueMetaString("json"));
    doReturn(rowMeta).when(transform).getInputRowMeta();
    doNothing().when(transform).putRow(any(IRowMeta.class), any(Object[].class));
  }

  @AfterEach
  void tearDown() {
    helper.cleanUp();
  }

  @Test
  void sendsNestedDocumentBeforePassingRowOn() throws Exception {
    Object[] row = {"{\"id\":\"p\",\"child\":{\"id\":\"c\"}}"};
    doReturn(row).when(transform).getRow();

    assertTrue(transform.processRow());
    ArgumentCaptor<SolrInputDocument> document = ArgumentCaptor.forClass(SolrInputDocument.class);
    verify(client).add(document.capture(), eq(1000));
    assertEquals(
        "c",
        assertInstanceOf(SolrInputDocument.class, document.getValue().getFieldValue("child"))
            .getFieldValue("id"));
    verify(transform).putRow(rowMeta, row);
  }

  @Test
  void failedWritesAndInvalidJsonDoNotPassRowsOn() throws Exception {
    Object[] row = {"{\"id\":\"p\"}"};
    doReturn(row).when(transform).getRow();
    when(client.add(any(SolrInputDocument.class), eq(1000)))
        .thenThrow(new IOException("server unavailable"));
    assertThrows(HopException.class, () -> transform.processRow());
    verify(transform, never()).putRow(any(), any());
  }

  @Test
  void invalidJsonIsNotSent() throws Exception {
    doReturn(new Object[] {"not json"}).when(transform).getRow();
    assertThrows(HopException.class, () -> transform.processRow());
    verify(client, never()).add(any(SolrInputDocument.class), eq(1000));
    verify(transform, never()).putRow(any(), any());
  }

  @Test
  void missingInputFieldFailsBeforeSendingAndDisposeClosesClient() throws Exception {
    doReturn(new RowMeta()).when(transform).getInputRowMeta();
    doReturn(new Object[] {"{}"}).when(transform).getRow();
    assertThrows(HopException.class, () -> transform.processRow());
    verify(client, never()).add(any(SolrInputDocument.class), eq(1000));
    transform.dispose();
    verify(client).close();
  }
}
