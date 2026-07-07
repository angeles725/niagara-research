package com.tridium.px.editor.sidebars.binding;

import com.tridium.px.editor.util.EventUtil;
import java.util.ArrayList;
import java.util.List;
import javax.baja.naming.BOrd;
import javax.baja.px.editor.BPxEditor;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.util.Lexicon;

class ChangeOrds extends Command {
   BPxEditor editor;
   ChangeOrds.Entry[] entries;

   ChangeOrds(BPxEditor editor, ChangeOrds.Entry[] entries) {
      super(editor, Lexicon.make("pxEditor"), "commands.changeOrds");
      this.editor = editor;
      this.entries = entries;
   }

   public CommandArtifact doInvoke() throws Exception {
      ChangeOrds.Artifact artifact = new ChangeOrds.Artifact();
      artifact.redo();
      return artifact;
   }

   private class Artifact implements CommandArtifact {
      ChangeOrds.Entry[] bindingEntries;
      ChangeOrds.Entry[] widgetEntries;

      Artifact() {
         List<ChangeOrds.Entry> bindingArray = new ArrayList<>();
         List<ChangeOrds.Entry> widgetArray = new ArrayList<>();

         for (int i = 0; i < ChangeOrds.this.entries.length; i++) {
            if (ChangeOrds.this.entries[i].component instanceof BBinding) {
               bindingArray.add(ChangeOrds.this.entries[i]);
            } else if (ChangeOrds.this.entries[i].component instanceof BWidget) {
               widgetArray.add(ChangeOrds.this.entries[i]);
            }
         }

         this.bindingEntries = bindingArray.toArray(new ChangeOrds.Entry[0]);
         this.widgetEntries = widgetArray.toArray(new ChangeOrds.Entry[0]);
      }

      public void redo() throws Exception {
         if (this.bindingEntries.length > 0) {
            String[] props = new String[this.bindingEntries.length];
            BBinding[] bindings = new BBinding[this.bindingEntries.length];

            for (int i = 0; i < this.bindingEntries.length; i++) {
               ChangeOrds.Entry e = this.bindingEntries[i];
               e.component.set(e.prop, e.newOrd);
               props[i] = e.prop.getName();
               bindings[i] = (BBinding)e.component;
            }

            ChangeOrds.this.editor.firePxEvent(EventUtil.bindingsChanged(bindings, props));
         }

         if (this.widgetEntries.length > 0) {
            String[] props = new String[this.widgetEntries.length];
            BWidget[] widgets = new BWidget[this.widgetEntries.length];

            for (int i = 0; i < this.widgetEntries.length; i++) {
               ChangeOrds.Entry e = this.widgetEntries[i];
               e.component.set(e.prop, e.newOrd);
               props[i] = e.prop.getName();
               widgets[i] = (BWidget)e.component;
            }

            ChangeOrds.this.editor.firePxEvent(EventUtil.widgetsChanged(widgets, props));
         }
      }

      public void undo() throws Exception {
         if (this.bindingEntries.length > 0) {
            String[] props = new String[this.bindingEntries.length];
            BBinding[] bindings = new BBinding[this.bindingEntries.length];

            for (int i = 0; i < this.bindingEntries.length; i++) {
               ChangeOrds.Entry e = this.bindingEntries[i];
               e.component.set(e.prop, e.oldOrd);
               props[i] = e.prop.getName();
               bindings[i] = (BBinding)e.component;
            }

            ChangeOrds.this.editor.firePxEvent(EventUtil.bindingsChanged(bindings, props));
         }

         if (this.widgetEntries.length > 0) {
            String[] props = new String[this.widgetEntries.length];
            BWidget[] widgets = new BWidget[this.widgetEntries.length];

            for (int i = 0; i < this.widgetEntries.length; i++) {
               ChangeOrds.Entry e = this.widgetEntries[i];
               e.component.set(e.prop, e.oldOrd);
               props[i] = e.prop.getName();
               widgets[i] = (BWidget)e.component;
            }

            ChangeOrds.this.editor.firePxEvent(EventUtil.widgetsChanged(widgets, props));
         }
      }
   }

   static class Entry {
      BComponent component;
      Property prop;
      BOrd oldOrd;
      BOrd newOrd;

      Entry(BComponent component, Property prop, BOrd oldOrd, BOrd newOrd) {
         this.component = component;
         this.prop = prop;
         this.oldOrd = oldOrd;
         this.newOrd = newOrd;
      }
   }
}
