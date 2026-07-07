package com.tridium.px.editor.sidebars.cellsheet.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import com.tridium.ui.BOptionDialog;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.list.BList;
import javax.baja.ui.list.ListController;

public class AddBinding extends Command {
   private BPxEditorPane editorPane;
   private BPxCellSheet sheet;
   private BOrd ord;

   public AddBinding(BPxEditorPane editorPane, BPxCellSheet sheet) {
      this(editorPane, sheet, null);
   }

   public AddBinding(BPxEditorPane editorPane, BPxCellSheet sheet, BOrd ord) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.addBinding");
      this.editorPane = editorPane;
      this.sheet = sheet;
      this.ord = ord;
   }

   public CommandArtifact doInvoke() throws Exception {
      BWidget w = this.sheet.getWidget();
      BBinding b = this.newBinding(w, bindingTypes(w));
      if (b == null) {
         return null;
      } else {
         if (this.ord != null) {
            b.setOrd(this.ord);
         }

         AddBinding.Artifact artifact = new AddBinding.Artifact(w, b);
         artifact.redo();
         return artifact;
      }
   }

   private static TypeInfo[] bindingTypes(BWidget w) {
      Array<AgentInfo> arr = new Array(w.getAgents().filter(AgentFilter.is(BBinding.TYPE)).list());
      return (TypeInfo[])arr.apply(TypeInfo.class, obj -> ((AgentInfo)obj).getAgentType()).trim();
   }

   private BBinding newBinding(BWidget w, TypeInfo[] types) {
      if (types.length == 1) {
         return (BBinding)types[0].getInstance();
      } else {
         BList list = new BList();
         list.setMultipleSelection(false);
         list.setController(new ListController() {
            protected void itemDoubleClicked(BMouseEvent event, int index) {
               ((BOptionDialog)this.getList().getShell()).close(1);
            }
         });

         for (int i = 0; i < types.length; i++) {
            list.addItem(types[i].getModuleName() + ":" + types[i].getDisplayName(null));
         }

         list.setSelectedIndex(0);
         int result = BDialog.open(this.editorPane, BPxEditorPane.text("commands.addBinding.label"), list, 3);
         if (result == 2) {
            return null;
         } else {
            int n = list.getSelectedIndex();
            if (n == -1) {
               n = 0;
            }

            return (BBinding)types[n].getInstance();
         }
      }
   }

   private class Artifact implements CommandArtifact {
      private BWidget widget;
      private BBinding binding;

      private Artifact(BWidget widget, BBinding binding) {
         this.widget = widget;
         this.binding = binding;
      }

      public void redo() throws Exception {
         Property prop = this.widget.add(null, this.binding);
         AddBinding.this.sheet.getCellContext().bindingAdded(this.widget, prop, this.binding);
      }

      public void undo() throws Exception {
         Property prop = this.binding.getPropertyInParent();
         this.widget.remove(this.binding);
         AddBinding.this.sheet.getCellContext().bindingDeleted(this.widget, prop, this.binding);
      }
   }
}
