package com.tridium.px.editor.make;

import javax.baja.gx.BImage;
import javax.baja.sys.BObject;
import javax.baja.ui.tree.TreeModel;
import javax.baja.ui.tree.TreeNode;

class PropertyNode extends TreeNode {
   private BObject object;
   private String name;
   private String displayName;
   private BImage icon;
   private String namePath;
   private String displayNamePath;
   private PropertyNode[] kids;
   private PropertyNodeFactory factory = new PropertyNodeFactory() {
      @Override
      protected PropertyNode make(BObject object, String name, String displayName) {
         return new PropertyNode(PropertyNode.this, object, name, displayName);
      }
   };

   PropertyNode(TreeModel model, BObject object, String name, String displayName) {
      super(model);
      this.object = object;
      this.name = name;
      this.displayName = displayName;
      this.icon = BImage.make(object.getIcon());
   }

   private PropertyNode(TreeNode parent, BObject object, String name, String displayName) {
      super(parent);
      this.object = object;
      this.name = name;
      this.displayName = displayName;
      this.icon = BImage.make(object.getIcon());
   }

   public boolean equals(Object o) {
      PropertyNode pm = (PropertyNode)o;
      if (!this.name.equals(pm.name)) {
         return false;
      } else {
         return !this.displayName.equals(pm.displayName) ? false : this.object.getClass().equals(pm.object.getClass());
      }
   }

   public int hashCode() {
      throw new UnsupportedOperationException();
   }

   public Object getSubject() {
      return this.object;
   }

   public String getText() {
      return this.displayName;
   }

   public BImage getIcon() {
      return this.icon;
   }

   public boolean hasChildren() {
      return this.kids == null ? true : this.kids.length > 0;
   }

   public final int getChildCount() {
      if (this.kids == null) {
         this.kids = this.factory.makeAll(this.object);
      }

      return this.kids.length;
   }

   public final TreeNode getChild(int index) {
      if (this.kids == null) {
         this.kids = this.factory.makeAll(this.object);
      }

      return this.kids[index];
   }

   String namePath() {
      if (this.namePath == null) {
         StringBuilder sb = new StringBuilder(this.name);

         for (PropertyNode parent = (PropertyNode)this.getParent(); parent != null; parent = (PropertyNode)parent.getParent()) {
            sb.insert(0, "/").insert(0, parent.name);
         }

         this.namePath = sb.toString();
      }

      return this.namePath;
   }

   String displayNamePath() {
      if (this.displayNamePath == null) {
         StringBuilder sb = new StringBuilder(this.displayName);

         for (PropertyNode parent = (PropertyNode)this.getParent(); parent != null; parent = (PropertyNode)parent.getParent()) {
            sb.insert(0, " ").insert(0, parent.displayName);
         }

         this.displayNamePath = sb.toString();
      }

      return this.displayNamePath;
   }

   BObject object() {
      return this.object;
   }
}
