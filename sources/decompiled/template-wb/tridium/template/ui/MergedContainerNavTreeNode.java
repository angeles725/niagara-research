package com.tridium.template.ui;

import com.tridium.template.application.NameTree;
import javax.baja.nav.BINavNode;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.workbench.nav.tree.NavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

public class MergedContainerNavTreeNode extends ApplicationNavTreeNode {
   private final NameTree exclusions;

   MergedContainerNavTreeNode(NavTreeModel model, NavTreeNode parent, BINavNode navNode, NameTree exclusions) {
      super(model, parent, navNode);
      this.exclusions = exclusions;
   }

   @Override
   public ApplicationNavTreeNode makeChildNode(NavTreeModel model, BINavNode navNode) {
      String navName = navNode.getNavName();
      if (!this.exclusions.has(navName)) {
         BObject navNodeObject = navNode.asObject();
         if (navNodeObject.isComplex()) {
            Property navNodeProperty = navNodeObject.asComplex().getPropertyInParent();
            if (navNodeProperty != null && navNodeProperty.isFrozen()) {
               return new ExcludedDescendantNavTreeNode(model, this, navNode);
            }
         }

         return new IncludedComponentNavTreeNode(model, this, navNode);
      } else {
         NameTree childExclusions = this.exclusions.fetch(navName);
         return (ApplicationNavTreeNode)(childExclusions == null
            ? new ExcludedComponentNavTreeNode(model, this, navNode)
            : new MergedContainerNavTreeNode(model, this, navNode, childExclusions));
      }
   }

   @Override
   protected BIcon getBadge() {
      return null;
   }
}
