package com.tridium.template.ui;

import com.tridium.template.application.NameTree;
import com.tridium.template.manifest.TemplateManifest;
import javax.baja.nav.BINavNode;
import javax.baja.ui.tree.TreeNode;
import javax.baja.workbench.nav.tree.DefaultNavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

public class ApplicationNavTreeModel extends DefaultNavTreeModel {
   private final ApplicationNavTreeNode root;
   private final TemplateManifest manifest;

   public ApplicationNavTreeModel(BINavNode rootNavNode, NameTree exclusions, TemplateManifest manifest) {
      super(rootNavNode);
      this.root = new MergedContainerNavTreeNode(this, null, rootNavNode, exclusions);
      this.manifest = manifest;
   }

   public NavTreeNode makeNavTreeNode(NavTreeNode parent, BINavNode navNode) {
      ApplicationNavTreeNode appParent = null;
      if (parent instanceof ApplicationNavTreeNode) {
         appParent = (ApplicationNavTreeNode)parent;
      } else if (parent.getNavNode() == this.root.getNavNode()) {
         appParent = this.root;
      }

      return (NavTreeNode)(appParent != null ? appParent.makeChildNode(this, navNode) : super.makeNavTreeNode(parent, navNode));
   }

   public TreeNode getRoot(int index) {
      NavTreeNode superRoot = (NavTreeNode)super.getRoot(index);
      return (TreeNode)(superRoot.getNavNode() == this.root.getWrappedNode() ? this.root : superRoot);
   }

   TemplateManifest getManifest() {
      return this.manifest;
   }
}
