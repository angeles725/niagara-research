package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.CellSheetContext;
import com.tridium.px.editor.util.Reflector;
import com.tridium.util.ClassUtil;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BDialog;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.util.BConverter;

public class AddConverter extends Command {
   private BPxEditorPane editorPane;
   private CellSheetContext context;
   private String propName;
   private BBinding[] bindings;
   private BObject[] from;
   private BObject to;

   public AddConverter(BPxEditorPane editorPane, CellSheetContext context, boolean enabled, String propName, BBinding[] bindings, BObject[] from, BObject to) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.animate");
      this.editorPane = editorPane;
      this.propName = propName;
      this.bindings = bindings;
      this.from = from;
      this.to = to;
      this.context = context;
      this.setEnabled(enabled);
   }

   public CommandArtifact doInvoke() throws Exception {
      BConverterConfig config = (BConverterConfig)(ClassUtil.allNull(this.from)
         ? new BOfflineConverterConfig(this.bindings, this.to)
         : new BOnlineConverterConfig(this.bindings, this.from, this.to));
      int result = BDialog.open(this.editorPane, BPxEditorPane.text("commands.animate.label"), config, 3);
      if (result == 2) {
         return null;
      } else {
         AddConverter.Artifact artifact = new AddConverter.Artifact(config.binding(), config.converter());
         artifact.redo();
         return artifact;
      }
   }

   class Artifact implements CommandArtifact {
      BBinding newBinding;
      BBinding oldBinding;
      BConverter newConverter;
      BConverter oldConverter;

      Artifact(BBinding newBinding, BConverter newConverter) {
         this.newBinding = newBinding;
         this.newConverter = newConverter;
         this.oldConverter = Reflector.converter(AddConverter.this.propName, AddConverter.this.bindings);
         if (this.oldConverter != null) {
            this.oldBinding = (BBinding)this.oldConverter.getParent();
         }
      }

      public void redo() throws Exception {
         if (this.oldBinding != null) {
            this.oldBinding.remove(AddConverter.this.propName);
         }

         Property prop = this.newBinding.add(AddConverter.this.propName, this.newConverter);
         AddConverter.this.context.converterAdded(this.newBinding, prop, this.newConverter);
      }

      public void undo() throws Exception {
         Property prop = this.newBinding.getProperty(AddConverter.this.propName);
         this.newBinding.remove(AddConverter.this.propName);
         if (this.oldBinding != null) {
            this.oldBinding.add(AddConverter.this.propName, this.oldConverter);
         }

         AddConverter.this.context.converterDeleted(this.newBinding, prop, this.newConverter);
      }
   }
}
