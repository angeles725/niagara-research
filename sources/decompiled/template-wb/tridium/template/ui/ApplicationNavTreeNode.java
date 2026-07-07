package com.tridium.template.ui;

import javax.baja.gx.BImage;
import javax.baja.nav.BINavNode;
import javax.baja.space.BISpaceNode;
import javax.baja.sys.BIcon;
import javax.baja.workbench.nav.tree.NavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

public abstract class ApplicationNavTreeNode extends NavTreeNode {
   private static final BIcon DEFAULT_ICON = BIcon.std("object.png");
   static final BIcon ADD_BADGE_ICON = BIcon.std("badges/add.png");
   static final BIcon REMOVE_BADGE_ICON = BIcon.std("badges/remove.png");
   static final BIcon OPTIONAL_BADGE_ICON = BIcon.std("badges/ellipsis.png");
   private final BINavNode wrappedNode;
   private BIcon icon;
   private BImage iconImage;

   ApplicationNavTreeNode(NavTreeModel model, NavTreeNode parent, BINavNode wrappedNode) {
      super(model, parent, wrappedNode);
      this.wrappedNode = wrappedNode;
   }

   public abstract ApplicationNavTreeNode makeChildNode(NavTreeModel var1, BINavNode var2);

   protected abstract BIcon getBadge();

   public BImage getIcon() {
      BIcon icon = this.wrappedNode.getNavIcon();
      if (icon == null) {
         icon = DEFAULT_ICON;
      }

      BIcon badge = this.getBadge();
      if (badge != null) {
         icon = BIcon.make(icon, badge);
      }

      if (icon != this.icon) {
         this.icon = icon;
         this.iconImage = BImage.make(icon);
         this.iconImage.sync();
      }

      return this.wrappedNode instanceof BISpaceNode && ((BISpaceNode)this.wrappedNode).isPendingMove() ? this.iconImage.getDisabledImage() : this.iconImage;
   }

   BINavNode getWrappedNode() {
      return this.wrappedNode;
   }
}
