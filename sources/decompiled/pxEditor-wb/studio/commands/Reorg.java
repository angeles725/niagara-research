package com.tridium.px.editor.studio.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.Reflector;
import java.util.ArrayList;
import java.util.List;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BPane;

public class Reorg extends Command {
   private BPxEditorPane editorPane;
   private BPane freeForm;
   private boolean bringToFront;
   private boolean byOne;
   private BWidget[] origSel;
   private Property[] oldProps;
   private Property[] newProps;

   public Reorg(BPxEditorPane editorPane, boolean bringToFront, boolean byOne) {
      super(
         editorPane,
         BPxEditorPane.lexicon(),
         byOne ? (bringToFront ? "commands.bringForwardOne" : "commands.sendBackOne") : (bringToFront ? "commands.bringToFront" : "commands.sendToBack")
      );
      this.editorPane = editorPane;
      this.bringToFront = bringToFront;
      this.byOne = byOne;
   }

   public CommandArtifact doInvoke() throws Exception {
      this.freeForm = this.editorPane.getCommandStudio().getCurrentFreeForm();
      this.origSel = this.editorPane.getSelectedWidgets().getWidgets();
      this.oldProps = Reflector.dynamicProperties(this.freeForm);
      if (this.byOne) {
         this.newProps = new Property[this.oldProps.length];
         if (this.freeForm instanceof BCanvasPane) {
            if (this.bringToFront) {
               this.bringForwardOne();
            } else {
               this.sendBackOne();
            }
         } else if (this.bringToFront) {
            this.sendBackOne();
         } else {
            this.bringForwardOne();
         }
      } else {
         List<Property> a = new ArrayList<>();
         List<Property> b = new ArrayList<>();

         for (int i = 0; i < this.oldProps.length; i++) {
            if (this.isSelected(this.freeForm.get(this.oldProps[i]))) {
               b.add(this.oldProps[i]);
            } else {
               a.add(this.oldProps[i]);
            }
         }

         if (this.freeForm instanceof BCanvasPane) {
            if (this.bringToFront) {
               a.addAll(b);
               this.newProps = a.toArray(new Property[0]);
            } else {
               b.addAll(a);
               this.newProps = b.toArray(new Property[0]);
            }
         } else if (this.bringToFront) {
            b.addAll(a);
            this.newProps = b.toArray(new Property[0]);
         } else {
            a.addAll(b);
            this.newProps = a.toArray(new Property[0]);
         }
      }

      Reorg.Artifact artifact = new Reorg.Artifact();
      artifact.redo();
      return artifact;
   }

   private boolean isSelected(BValue val) {
      return val instanceof BWidget && this.editorPane.getSelectedWidgets().isSelected((BWidget)val);
   }

   private void bringForwardOne() {
      int n = 0;
      List<Property> queue = new ArrayList<>();

      for (int i = 0; i < this.oldProps.length; i++) {
         if (this.isSelected(this.freeForm.get(this.oldProps[i]))) {
            queue.add(this.oldProps[i]);
         } else {
            this.newProps[n++] = this.oldProps[i];

            for (int j = 0; j < queue.size(); j++) {
               this.newProps[n++] = queue.get(j);
            }

            queue.clear();
         }
      }

      for (int j = 0; j < queue.size(); j++) {
         this.newProps[n++] = queue.get(j);
      }
   }

   private void sendBackOne() {
      int n = this.oldProps.length - 1;
      List<Property> queue = new ArrayList<>();

      for (int i = this.oldProps.length - 1; i >= 0; i--) {
         if (this.isSelected(this.freeForm.get(this.oldProps[i]))) {
            queue.add(this.oldProps[i]);
         } else {
            this.newProps[n--] = this.oldProps[i];

            for (int j = 0; j < queue.size(); j++) {
               this.newProps[n--] = queue.get(j);
            }

            queue.clear();
         }
      }

      for (int j = 0; j < queue.size(); j++) {
         this.newProps[n--] = queue.get(j);
      }
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         this.reorder(Reorg.this.newProps);
      }

      public void undo() throws Exception {
         this.reorder(Reorg.this.oldProps);
      }

      private void reorder(Property[] props) {
         Reorg.this.freeForm.reorder(props);
         Reorg.this.editorPane.getSelectedWidgets().setWidgets(Reorg.this.origSel);
         String[] names = new String[props.length];

         for (int i = 0; i < props.length; i++) {
            names[i] = props[i].getName();
         }

         Reorg.this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(4, Reorg.this.freeForm, names));
      }
   }
}
