package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.TreeStudio;
import com.tridium.px.editor.studio.commands.Reorg;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.ui.BMenu;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.tree.TreeController;
import javax.baja.ui.tree.TreeNode;

public class PxTreeController extends TreeController {
   private BPxEditor editor;
   private BPxEditorPane editorPane;
   private BPxTree tree;
   private SelectedWidgets selected;
   private TreeStudio studio;
   private BWidget[] oldSel;

   public PxTreeController(BPxEditor editor, BPxEditorPane editorPane, BPxTree tree, SelectedWidgets selected) {
      this.editor = editor;
      this.editorPane = editorPane;
      this.tree = tree;
      this.selected = selected;
      this.studio = editorPane.getTreeStudio();
   }

   public TreeNode getFocus() {
      return super.getFocus();
   }

   public void setFocus(TreeNode node) {
      super.setFocus(node);
   }

   protected void popup(BMouseEvent event, TreeNode tnode) {
      if (tnode != null) {
         WidgetNode node = (WidgetNode)tnode;
         this.tree.setPopupNode(node);
         BMenu menu = this.editor.getController().getPopupMenu(this.tree, event);
         if (menu != null) {
            menu.open(this.tree, event.getX(), event.getY());
         }
      }
   }

   protected void doSelectAction(TreeNode node, double x, double y) {
   }

   public void mousePressed(BMouseEvent evt) {
      if (evt.getClickCount() == 2 && evt.isButton1Down() && this.selected.size() > 0) {
         Command cmd = this.editor.getController().getDoubleClickCommand(this.tree, evt);
         if (cmd != null) {
            cmd.invoke();
         }
      } else {
         this.oldSel = this.selected.getWidgets();
         super.mousePressed(evt);
      }
   }

   public void mouseReleased(BMouseEvent evt) {
      super.mouseReleased(evt);
      BWidget[] newSel = this.selected.getWidgets();
      if (!this.selEquals(this.oldSel, newSel)) {
         this.editorPane.getPxEditor().firePxEvent(new PxSelectionEvent(this.editorPane.getSelectedWidgets().getWidgets()));
      }
   }

   public void keyPressed(BKeyEvent evt) {
      if (evt.isControlDown()) {
         BPane freeForm = this.studio.getCurrentFreeForm();
         switch (evt.getKeyCode()) {
            case 38:
               if (this.studio.reorgable(freeForm)) {
                  new Reorg(this.editorPane, true, true).invoke();
               }

               evt.consume();
               break;
            case 40:
               if (this.studio.reorgable(freeForm)) {
                  new Reorg(this.editorPane, false, true).invoke();
               }

               evt.consume();
               break;
            default:
               super.keyPressed(evt);
         }
      } else {
         super.keyPressed(evt);
      }
   }

   private boolean selEquals(BWidget[] a, BWidget[] b) {
      if (a.length != b.length) {
         return false;
      } else {
         for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
               return false;
            }
         }

         return true;
      }
   }
}
