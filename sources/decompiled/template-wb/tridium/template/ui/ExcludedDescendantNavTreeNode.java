package com.tridium.template.ui;

import javax.baja.nav.BINavNode;
import javax.baja.sys.BIcon;
import javax.baja.workbench.nav.tree.NavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

public class ExcludedDescendantNavTreeNode extends ApplicationNavTreeNode {
   ExcludedDescendantNavTreeNode(NavTreeModel model, NavTreeNode parent, BINavNode navNode) {
      super(model, parent, navNode);
   }

   @Override
   public ApplicationNavTreeNode makeChildNode(NavTreeModel model, BINavNode navNode) {
      return new ExcludedDescendantNavTreeNode(model, this, navNode);
   }

   @Override
   protected BIcon getBadge() {
      return REMOVE_BADGE_ICON;
   }
}
