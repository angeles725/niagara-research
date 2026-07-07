package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.tree.TreeNode;

class LeafNode extends WidgetNode {
   LeafNode(BPxEditorPane editorPane, PxTreeModel model, BWidget widget) {
      super(editorPane, model, widget);
   }

   LeafNode(BPxEditorPane editorPane, WidgetNode parent, BWidget widget) {
      super(editorPane, parent, widget);
   }

   public int getChildCount() {
      return 0;
   }

   public TreeNode getChild(int index) {
      throw new IllegalStateException();
   }

   @Override
   int dragOver() {
      return this.getWidget() instanceof BNullWidget ? 16 : 0;
   }
}
