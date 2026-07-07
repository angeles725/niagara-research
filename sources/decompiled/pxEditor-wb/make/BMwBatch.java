package com.tridium.px.editor.make;

import java.util.ArrayList;
import java.util.List;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.pane.BTreePane;
import javax.baja.ui.tree.BTree;
import javax.baja.ui.tree.TreeNode;

@NiagaraType
public abstract class BMwBatch extends BMwConfig {
   public static final Type TYPE = Sys.loadType(BMwBatch.class);
   protected BTree tree = new BTree();
   protected BTreePane treePane = new BTreePane(this.tree);
   protected TreeNode[] roots;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void setWorkingWidget() {
      throw new IllegalStateException();
   }

   protected TreeNode[] orderedSelectedNodes() {
      List<TreeNode> arr = new ArrayList<>();

      for (int i = 0; i < this.roots.length; i++) {
         this.findSelected(this.roots[i], arr);
      }

      return arr.toArray(new TreeNode[0]);
   }

   private void findSelected(TreeNode node, List<TreeNode> arr) {
      if (node.isSelected()) {
         arr.add(node);
      }

      for (int i = 0; i < node.getChildCount(); i++) {
         this.findSelected(node.getChild(i), arr);
      }
   }
}
