package com.tridium.template.ui;

import com.tridium.template.manifest.TemplateManifest;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.nav.BINavNode;
import javax.baja.sys.BIcon;
import javax.baja.workbench.nav.tree.NavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

public class IncludedDescendantNavTreeNode extends ApplicationNavTreeNode {
   IncludedDescendantNavTreeNode(NavTreeModel model, NavTreeNode parent, BINavNode navNode) {
      super(model, parent, navNode);
   }

   @Override
   public ApplicationNavTreeNode makeChildNode(NavTreeModel model, BINavNode navNode) {
      OrdQuery[] queries = navNode.getNavOrd().parse();

      for (OrdQuery query : queries) {
         if (query instanceof SlotPath && query != null) {
            TemplateManifest manifest = ((ApplicationNavTreeModel)this.getModel()).getManifest();
            if (manifest.optional.contains(BOrd.make(query))) {
               return new OptionalComponentNavTreeNode(model, this, navNode);
            }
         }
      }

      return new IncludedDescendantNavTreeNode(model, this, navNode);
   }

   @Override
   protected BIcon getBadge() {
      return ADD_BADGE_ICON;
   }
}
