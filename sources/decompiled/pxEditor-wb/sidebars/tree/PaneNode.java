package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.Reflector;
import java.util.ArrayList;
import java.util.List;
import javax.baja.chart.BChartPane;
import javax.baja.sys.BValue;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.ui.BWidget;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.tree.TreeNode;

class PaneNode extends WidgetNode {
   private List<WidgetNode> kids = new ArrayList<>();

   PaneNode(BPxEditorPane editorPane, PxTreeModel model, BPane pane) {
      super(editorPane, model, pane);
      this.build();
   }

   PaneNode(BPxEditorPane editorPane, WidgetNode parent, BPane pane) {
      super(editorPane, parent, pane);
      this.build();
   }

   public int getChildCount() {
      return this.kids.size();
   }

   public TreeNode getChild(int index) {
      return this.kids.get(index);
   }

   @Override
   int dragOver() {
      return Reflector.isFreeFormPane(this.getWidget()) ? 16 : 0;
   }

   void deleteNode(WidgetNode node) {
      int idx = this.kids.indexOf(node);
      if (idx == -1) {
         throw new IllegalStateException();
      } else {
         this.kids.remove(idx);
      }
   }

   void build() {
      this.doBuild(childWidgets(this.getWidget()));
   }

   void updateChildNodes(BWidget compare) {
      if (this.getWidget() != compare) {
         throw new IllegalStateException();
      } else {
         BWidget[] cw = childWidgets(compare);
         if (this.getChildCount() != cw.length) {
            this.doBuild(cw);
         } else {
            for (int i = 0; i < cw.length; i++) {
               WidgetNode wn = (WidgetNode)this.getChild(i);
               if (wn.getWidget() != cw[i]) {
                  this.doBuild(cw);
                  return;
               }
            }

            for (int ix = 0; ix < cw.length; ix++) {
               TreeNode tn = this.getChild(ix);
               if (tn instanceof PaneNode) {
                  ((PaneNode)tn).updateChildNodes(cw[ix]);
               }
            }
         }
      }
   }

   void doBuild(BWidget[] cw) {
      this.kids.clear();

      for (int i = 0; i < cw.length; i++) {
         this.kids.add(this.makeNode(cw[i]));
      }
   }

   private WidgetNode makeNode(BWidget w) {
      return (WidgetNode)(Reflector.isLeaf(w) ? new LeafNode(this.editorPane, this, w) : new PaneNode(this.editorPane, this, (BPane)w));
   }

   private static BWidget[] childWidgets(BWidget widget) {
      return Reflector.isFreeFormPane(widget) ? freeFormChildren(widget) : cannedChildren(widget);
   }

   private static BWidget[] cannedChildren(BWidget widget) {
      List<BWidget> arr = new ArrayList<>();
      Property[] props = widget.getPropertiesArray();

      for (int i = props.length - 1; i >= 0; i--) {
         Property prop = props[i];
         if (!prop.isDynamic() && !Flags.isHidden(widget, prop)) {
            BValue val = widget.get(prop);
            if (val instanceof BWidget) {
               arr.add((BWidget)val);
            }
         }
      }

      return arr.toArray(new BWidget[0]);
   }

   private static BWidget[] freeFormChildren(BWidget widget) {
      List<BWidget> arr = new ArrayList<>();
      Property[] props = widget.getPropertiesArray();
      if (widget instanceof BCanvasPane) {
         for (int i = props.length - 1; i >= 0; i--) {
            Property prop = props[i];
            if (!Flags.isHidden(widget, prop) && !prop.isFrozen()) {
               BValue val = widget.get(prop);
               if (val instanceof BWidget) {
                  arr.add((BWidget)val);
               }
            }
         }
      } else {
         for (int ix = 0; ix < props.length; ix++) {
            Property prop = props[ix];
            if (!Flags.isHidden(widget, prop) && (widget instanceof BChartPane || !prop.isFrozen())) {
               BValue val = widget.get(prop);
               if (val instanceof BWidget) {
                  arr.add((BWidget)val);
               }
            }
         }
      }

      return arr.toArray(new BWidget[0]);
   }
}
