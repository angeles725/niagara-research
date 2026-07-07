package com.tridium.template.ui;

import javax.baja.nav.BINavNode;
import javax.baja.sys.BIcon;
import javax.baja.workbench.nav.tree.NavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

public class OptionalComponentNavTreeNode extends ApplicationNavTreeNode {
   OptionalComponentNavTreeNode(NavTreeModel model, NavTreeNode parent, BINavNode navNode) {
      super(model, parent, navNode);
   }

   @Override
   public ApplicationNavTreeNode makeChildNode(NavTreeModel model, BINavNode navNode) {
      return new OptionalDescendantNavTreeNode(model, this, navNode);
   }

   @Override
   protected BIcon getBadge() {
      return OPTIONAL_BADGE_ICON;
   }
}
