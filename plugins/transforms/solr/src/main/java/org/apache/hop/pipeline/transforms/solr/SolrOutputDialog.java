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

import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.gui.GuiCompositeWidgets;
import org.apache.hop.ui.core.gui.GuiCompositeWidgetsAdapter;
import org.apache.hop.ui.core.widget.ComboVar;
import org.apache.hop.ui.pipeline.transform.BaseTransformDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class SolrOutputDialog extends BaseTransformDialog {
  private static final Class<?> PKG = SolrOutputMeta.class;
  private final SolrOutputMeta input;
  private GuiCompositeWidgets widgets;

  public SolrOutputDialog(
      Shell parent, IVariables variables, SolrOutputMeta input, PipelineMeta pipelineMeta) {
    super(parent, variables, input, pipelineMeta);
    this.input = input;
  }

  @Override
  public String open() {
    createShell(BaseMessages.getString(PKG, "SolrOutput.Dialog.Title"));
    buildButtonBar().ok(e -> ok()).cancel(e -> cancel()).build();
    changed = input.hasChanged();
    widgets =
        GuiCompositeWidgets.addScrolledComposite(
            shell, variables, wTransformName, wOk, SolrOutputMeta.PARENT_ID, input);
    try {
      IRowMeta fields = pipelineMeta.getPrevTransformFields(variables, transformName);
      widgets.setComboValues(
          SolrOutputMeta.DOCUMENT_FIELD, fields == null ? new String[0] : fields.getFieldNames());
      if (!Utils.isEmpty(input.getDocumentField())) {
        Control control = widgets.getWidgetsMap().get(SolrOutputMeta.DOCUMENT_FIELD);
        if (control instanceof ComboVar combo) {
          combo.setText(input.getDocumentField());
        } else if (control instanceof Combo combo) {
          combo.setText(input.getDocumentField());
        }
      }
    } catch (HopException e) {
      log.logError(BaseMessages.getString(PKG, "SolrOutput.Error.GetFields"), e);
    }
    widgets.setWidgetsListener(
        new GuiCompositeWidgetsAdapter() {
          @Override
          public void widgetModified(
              GuiCompositeWidgets compositeWidgets, Control changedWidget, String widgetId) {
            input.setChanged();
          }
        });
    input.setChanged(changed);
    focusTransformName();
    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());
    return transformName;
  }

  private void ok() {
    if (Utils.isEmpty(wTransformName.getText())) {
      return;
    }
    widgets.getWidgetsContents(input, SolrOutputMeta.PARENT_ID);
    transformName = wTransformName.getText();
    input.setChanged();
    dispose();
  }

  private void cancel() {
    transformName = null;
    input.setChanged(changed);
    dispose();
  }
}
