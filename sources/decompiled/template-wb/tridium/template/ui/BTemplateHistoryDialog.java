package com.tridium.template.ui;

import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.manifest.TemplateManifest.Revision;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.util.UiLexicon;

@NiagaraType
public class BTemplateHistoryDialog extends BDialog {
   public static final Type TYPE = Sys.loadType(BTemplateHistoryDialog.class);
   BTextEditorPane historyPane;
   Command closeCommand;

   public Type getType() {
      return TYPE;
   }

   public BTemplateHistoryDialog() {
      throw new IllegalStateException();
   }

   public BTemplateHistoryDialog(BWidget parent, String title, TemplateManifest manifest) {
      super(parent, title, true);
      this.historyPane = new BTextEditorPane("", 5, 60, false);
      this.closeCommand = new BTemplateHistoryDialog.CloseCommand(this);
      BButton closeButton = new BButton(this.closeCommand, true, true);
      BGridPane toolBar = new BGridPane(1);
      toolBar.add("close", closeButton);

      for (Revision rev : manifest.revisionHistory) {
         this.historyPane.setText(this.historyPane.getText() + rev.version + " : " + rev.description + '\n');
      }

      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(this.historyPane));
      pane.setBottom(new BBorderPane(toolBar));
      this.setContent(pane);
   }

   class CloseCommand extends Command {
      CloseCommand(BWidget owner) {
         super(owner, UiLexicon.bajaui(), "commands.close");
      }

      public CommandArtifact doInvoke() {
         BTemplateHistoryDialog.this.close();
         return null;
      }
   }
}
