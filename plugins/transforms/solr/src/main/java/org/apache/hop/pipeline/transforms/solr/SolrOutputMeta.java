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

import lombok.Getter;
import lombok.Setter;
import org.apache.hop.core.annotations.Transform;
import org.apache.hop.core.gui.plugin.GuiElementType;
import org.apache.hop.core.gui.plugin.GuiPlugin;
import org.apache.hop.core.gui.plugin.GuiWidgetElement;
import org.apache.hop.core.gui.plugin.GuiWidgetGroupType;
import org.apache.hop.metadata.api.HopMetadataProperty;
import org.apache.hop.pipeline.transform.BaseTransformMeta;

@Getter
@Setter
@GuiPlugin
@Transform(
    id = "SolrOutput",
    image = "solr-output.svg",
    name = "i18n::SolrOutput.Name",
    description = "i18n::SolrOutput.Description",
    categoryDescription = "i18n:org.apache.hop.pipeline.transform:BaseTransform.Category.Output",
    keywords = "i18n::SolrOutput.Keywords",
    documentationUrl = "/pipeline/transforms/solroutput.html")
public class SolrOutputMeta extends BaseTransformMeta<SolrOutput, SolrOutputData> {
  public static final String PARENT_ID = "SOLR_OUTPUT_OPTIONS";
  public static final String DOCUMENT_FIELD = "SOLR_OUTPUT_DOCUMENT_FIELD";

  @GuiWidgetElement(
      id = "SOLR_OUTPUT_URL",
      order = "0100",
      type = GuiElementType.TEXT,
      label = "i18n::SolrOutput.Url.Label",
      toolTip = "i18n::SolrOutput.Url.Tooltip",
      parentId = PARENT_ID,
      groupType = GuiWidgetGroupType.BOXES,
      group = "Solr")
  @HopMetadataProperty(key = "url", injectionKey = "URL")
  private String url;

  @GuiWidgetElement(
      id = DOCUMENT_FIELD,
      order = "0200",
      type = GuiElementType.COMBO,
      label = "i18n::SolrOutput.DocumentField.Label",
      toolTip = "i18n::SolrOutput.DocumentField.Tooltip",
      parentId = PARENT_ID,
      groupType = GuiWidgetGroupType.BOXES,
      group = "Solr")
  @HopMetadataProperty(key = "documentField", injectionKey = "DOCUMENT_FIELD")
  private String documentField;

  @GuiWidgetElement(
      id = "SOLR_OUTPUT_COMMIT_WITHIN",
      order = "0300",
      type = GuiElementType.TEXT,
      label = "i18n::SolrOutput.CommitWithin.Label",
      toolTip = "i18n::SolrOutput.CommitWithin.Tooltip",
      parentId = PARENT_ID,
      groupType = GuiWidgetGroupType.BOXES,
      group = "Solr")
  @HopMetadataProperty(key = "commitWithin", injectionKey = "COMMIT_WITHIN")
  private String commitWithin;
}
