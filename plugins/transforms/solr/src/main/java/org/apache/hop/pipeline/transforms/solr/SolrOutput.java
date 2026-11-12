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

import java.io.IOException;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.util.Utils;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.Pipeline;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.transform.BaseTransform;
import org.apache.hop.pipeline.transform.TransformMeta;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

public class SolrOutput extends BaseTransform<SolrOutputMeta, SolrOutputData> {
  private static final Class<?> PKG = SolrOutputMeta.class;

  public SolrOutput(
      TransformMeta transformMeta,
      SolrOutputMeta meta,
      SolrOutputData data,
      int copyNr,
      PipelineMeta pipelineMeta,
      Pipeline pipeline) {
    super(transformMeta, meta, data, copyNr, pipelineMeta, pipeline);
  }

  @Override
  public boolean init() {
    if (!super.init()) {
      return false;
    }
    String url = resolve(meta.getUrl());
    String field = resolve(meta.getDocumentField());
    if (Utils.isEmpty(url) || Utils.isEmpty(field)) {
      logError(BaseMessages.getString(PKG, "SolrOutput.Error.MissingConfiguration"));
      return false;
    }
    String commitWithin = resolve(meta.getCommitWithin());
    try {
      if (!Utils.isEmpty(commitWithin)) {
        data.commitWithin = Integer.parseInt(commitWithin);
        if (data.commitWithin <= 0) {
          throw new IllegalArgumentException("commitWithin must be a positive integer");
        }
      }
      data.client = new HttpSolrClient.Builder(url).build();
      return true;
    } catch (IllegalArgumentException e) {
      logError(
          BaseMessages.getString(PKG, "SolrOutput.Error.InvalidConfiguration", e.getMessage()), e);
      return false;
    }
  }

  @Override
  public boolean processRow() throws HopException {
    Object[] row = getRow();
    if (row == null) {
      setOutputDone();
      return false;
    }
    if (first) {
      String field = resolve(meta.getDocumentField());
      data.documentFieldIndex = getInputRowMeta().indexOfValue(field);
      if (data.documentFieldIndex < 0) {
        throw new HopException(
            BaseMessages.getString(PKG, "SolrOutput.Error.FieldNotFound", field));
      }
      first = false;
    }
    try {
      String json = getInputRowMeta().getString(row, data.documentFieldIndex);
      SolrInputDocument document = SolrDocumentParser.parse(json);
      data.client.add(document, data.commitWithin);
    } catch (IOException | SolrServerException e) {
      throw new HopException(
          BaseMessages.getString(PKG, "SolrOutput.Error.WriteFailed", getLinesRead()), e);
    }
    incrementLinesOutput();
    putRow(getInputRowMeta(), row);
    return true;
  }

  @Override
  public void dispose() {
    if (data.client != null) {
      try {
        data.client.close();
      } catch (IOException e) {
        logError(BaseMessages.getString(PKG, "SolrOutput.Error.CloseFailed"), e);
        setErrors(1);
      }
      data.client = null;
    }
    super.dispose();
  }
}
