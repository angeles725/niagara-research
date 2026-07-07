package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.Reflector;
import javax.baja.gx.BImage;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.tree.TreeNode;

abstract class WidgetNode extends TreeNode {
   protected final BPxEditorPane editorPane;
   private BWidget widget;
   private String text;
   private BImage image;
   private PxTreeModel model;

   WidgetNode(BPxEditorPane editorPane, PxTreeModel model, BWidget widget) {
      super(model);
      this.model = model;
      this.editorPane = editorPane;
      this.widget = widget;
      this.image = BImage.make(widget.getIcon());
      this.text = this.makeText();
      model.putNode(this);
   }

   WidgetNode(BPxEditorPane editorPane, WidgetNode parent, BWidget widget) {
      super(parent);
      this.model = parent.model;
      this.editorPane = editorPane;
      this.widget = widget;
      this.image = BImage.make(widget.getIcon());
      this.text = this.makeText();
      this.model.putNode(this);
   }

   void refreshText() {
      this.text = this.makeText();

      for (int i = 0; i < this.getChildCount(); i++) {
         ((WidgetNode)this.getChild(i)).refreshText();
      }
   }

   private String makeText() {
      return this.editorPane.getOptions().getPreserveIdentities()
         ? Reflector.displayName(this.widget, this.widget.getName(), true)
         : (this.widget instanceof BNullWidget ? this.widget.getName() : Reflector.displayName(this.widget, true));
   }

   public String toString() {
      return "(WidgetNode: " + this.widget + ")";
   }

   abstract int dragOver();

   public final Object getSubject() {
      return this.widget;
   }

   public final String getText() {
      return this.text;
   }

   public final BImage getIcon() {
      return this.image;
   }

   BWidget getWidget() {
      return this.widget;
   }
}
