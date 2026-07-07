package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.tree.TreeNode;
import javax.baja.ui.tree.TreeSelection;

class PxTreeSelection extends TreeSelection {
   private BPxEditorPane editorPane;
   private SelectedWidgets selected;

   PxTreeSelection(BPxEditorPane editorPane, SelectedWidgets selected) {
      this.editorPane = editorPane;
      this.selected = editorPane.getSelectedWidgets();
   }

   void superDeselectAll() {
      super.deselectAll();
   }

   void superSelect(TreeNode node) {
      super.select(node);
   }

   public void select(TreeNode tnode) {
      WidgetNode node = (WidgetNode)tnode;
      if (node.getWidget() instanceof BNullWidget) {
         this.deselectAll();
      } else if (!this.selected.canSelect(node.getWidget())) {
         this.deselectAll();
      }

      this.selected.select(node.getWidget());
      this.editorPane.getPxEditor().firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      super.select(node);
   }

   public void deselectAll() {
      this.selected.deselectAll();
      this.editorPane.getPxEditor().firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      super.deselectAll();
   }

   public void deselect(TreeNode node) {
      this.selected.deselect(((WidgetNode)node).getWidget());
      this.editorPane.getPxEditor().firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      super.deselect(node);
   }
}
