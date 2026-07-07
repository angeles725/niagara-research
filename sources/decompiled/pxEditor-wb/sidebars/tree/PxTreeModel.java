package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import java.util.HashMap;
import java.util.Map;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.tree.TreeModel;
import javax.baja.ui.tree.TreeNode;

class PxTreeModel extends TreeModel {
   private BPxEditorPane editorPane;
   private Map<BWidget, WidgetNode> hash = new HashMap<>();
   private WidgetNode rootNode = null;

   PxTreeModel(BPxEditorPane editorPane) {
      this.editorPane = editorPane;
   }

   public int getRootCount() {
      return this.rootNode == null ? 0 : 1;
   }

   public TreeNode getRoot(int index) {
      if (index != 0) {
         throw new IllegalStateException();
      } else {
         return this.rootNode;
      }
   }

   void putNode(WidgetNode node) {
      this.hash.put(node.getWidget(), node);
   }

   WidgetNode getNode(BWidget widget) {
      return this.hash.get(widget);
   }

   void updateNodes(BWidget rootWidget) {
      if (this.rootNode.getWidget() == rootWidget) {
         if (this.rootNode instanceof PaneNode) {
            ((PaneNode)this.rootNode).updateChildNodes(rootWidget);
         }
      } else {
         this.buildRoot(rootWidget);
      }
   }

   void buildRoot(BWidget rootWidget) {
      if (rootWidget instanceof BPane) {
         this.rootNode = new PaneNode(this.editorPane, this, (BPane)rootWidget);
      } else {
         this.rootNode = new LeafNode(this.editorPane, this, rootWidget);
      }
   }

   void refreshNodeText() {
      if (this.rootNode != null) {
         this.rootNode.refreshText();
      }
   }
}
