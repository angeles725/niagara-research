package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.Reflector;
import com.tridium.px.editor.util.SelectedWidgets;
import com.tridium.ui.px.PxPropertyComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;

public class Delete extends Command {
   private BPxEditorPane editorPane;
   private SelectedWidgets selected;
   private BWidget parent;
   private BWidget[] children;
   private PxPropertyComponent[] propComponents;
   private boolean dynamic;
   private String[] names;
   private int[] reorder;

   public Delete(BPxEditorPane editorPane) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.delete");
      this.editorPane = editorPane;
      this.selected = editorPane.getSelectedWidgets();
      this.children = this.selected.getWidgets();
      if (this.children.length == 0) {
         throw new IllegalStateException();
      } else {
         this.parent = this.children[0].getParentWidget();
         this.propComponents = editorPane.getPxPropertyComponents().forWidgets(this.children);
         this.names = new String[this.children.length];
         Set<String> nameSet = new HashSet<>();

         for (int i = 0; i < this.children.length; i++) {
            this.names[i] = this.children[i].getName();
            nameSet.add(this.names[i]);
         }

         if (this.children.length <= 1 && !this.children[0].getPropertyInParent().isDynamic()) {
            this.dynamic = false;
         } else {
            this.dynamic = true;
            Property[] dyn = Reflector.dynamicProperties(this.parent);
            List<Integer> a1 = new ArrayList<>();
            List<Integer> a2 = new ArrayList<>();

            for (int i = 0; i < dyn.length; i++) {
               if (nameSet.contains(dyn[i].getName())) {
                  a2.add(i);
               } else {
                  a1.add(i);
               }
            }

            a1.addAll(a2);
            this.reorder = new int[a1.size()];

            for (int ix = 0; ix < a1.size(); ix++) {
               this.reorder[ix] = a1.get(ix);
            }
         }
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      Delete.Artifact artifact = new Delete.Artifact();
      artifact.redo();
      return artifact;
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         Delete.this.selected.deselectAll();
         if (Delete.this.dynamic) {
            for (int i = 0; i < Delete.this.children.length; i++) {
               Delete.this.parent.remove(Delete.this.children[i].getName());
            }
         } else {
            BWidget inst = (BWidget)Delete.this.parent.getClass().getDeclaredConstructor().newInstance();
            BValue val = (BValue)inst.get(Delete.this.names[0]).getClass().getDeclaredConstructor().newInstance();
            Delete.this.parent.set(Delete.this.names[0], val);
         }

         Delete.this.editorPane.getPxPropertyComponents().removeAll(Delete.this.propComponents);
         Delete.this.editorPane
            .getPxPropertyComponents()
            .updateTargets(Delete.this.editorPane.getPxEditor().getWidget(), Delete.this.editorPane.getPxEditor().getPxProperties());
         Delete.this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(1, Delete.this.parent, Delete.this.names, Delete.this.children));
      }

      public void undo() throws Exception {
         if (Delete.this.dynamic) {
            for (int i = 0; i < Delete.this.children.length; i++) {
               Delete.this.parent.add(Delete.this.names[i], Delete.this.children[i]);
            }

            Property[] dyn1 = Reflector.dynamicProperties(Delete.this.parent);
            Property[] dyn2 = new Property[dyn1.length];

            for (int i = 0; i < dyn1.length; i++) {
               dyn2[Delete.this.reorder[i]] = dyn1[i];
            }

            Delete.this.parent.reorder(dyn2);
         } else {
            Delete.this.parent.set(Delete.this.names[0], Delete.this.children[0]);
         }

         Delete.this.selected.setWidgets(Delete.this.children);
         Delete.this.editorPane.getPxPropertyComponents().addAll(Delete.this.propComponents);
         Delete.this.editorPane
            .getPxPropertyComponents()
            .updateTargets(Delete.this.editorPane.getPxEditor().getWidget(), Delete.this.editorPane.getPxEditor().getPxProperties());
         Delete.this.editorPane.getPxEditor().firePxEvent(new PxWidgetEvent(0, Delete.this.parent, Delete.this.names, Delete.this.children));
      }
   }
}
