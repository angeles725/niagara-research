package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.tree.TreeNode;

public class ExpandAll extends Command {
   private BPxTree tree;
   private TreeNode node;

   public ExpandAll(BPxEditorPane editorPane, BPxTree tree, WidgetNode w) {
      super(editorPane, BPxEditorPane.lexicon(), "commands.expandAll");
      this.tree = tree;
      if (w instanceof LeafNode) {
         this.setEnabled(false);
      } else {
         this.node = w;
      }
   }

   public CommandArtifact doInvoke() throws Exception {
      expand(this.node);
      this.tree.scrollNodeToVisible(this.node);
      this.tree.repaint();
      return null;
   }

   private static void expand(TreeNode n) {
      n.setExpanded(true);

      for (int i = 0; i < n.getChildCount(); i++) {
         expand(n.getChild(i));
      }
   }
}
