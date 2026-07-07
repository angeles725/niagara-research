package com.tridium.template.ui;

import javax.baja.nav.BINavNode;
import javax.baja.sys.BIcon;
import javax.baja.workbench.nav.tree.NavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

public class IncludedComponentNavTreeNode extends ApplicationNavTreeNode {
   IncludedComponentNavTreeNode(NavTreeModel model, NavTreeNode parent, BINavNode navNode) {
      super(model, parent, navNode);
   }

   @Override
   public ApplicationNavTreeNode makeChildNode(NavTreeModel model, BINavNode navNode) {
      return new IncludedDescendantNavTreeNode(model, this, navNode);
   }

   @Override
   protected BIcon getBadge() {
      return ADD_BADGE_ICON;
   }
}
