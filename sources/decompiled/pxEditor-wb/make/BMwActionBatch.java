package com.tridium.px.editor.make;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.util.ClassUtil;
import com.tridium.workbench.util.PropertyManager;
import java.util.ArrayList;
import java.util.List;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Flags;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BButton;
import javax.baja.ui.BLabel;
import javax.baja.ui.BLayout;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.tree.TreeModel;
import javax.baja.ui.tree.TreeNode;
import javax.baja.util.BTypeSpec;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
public class BMwActionBatch extends BMwBatch {
   public static final Type TYPE = Sys.loadType(BMwActionBatch.class);
   private BEdgePane wrapper = new BEdgePane();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwActionBatch() {
      this.command = new BMwActionBatch.Cmd();
      this.tree.setMultipleSelection(true);
      this.tree.setModel(new TreeModel() {
         public int getRootCount() {
            return BMwActionBatch.this.roots.length;
         }

         public TreeNode getRoot(int index) {
            return BMwActionBatch.this.roots[index];
         }
      });
      this.setContent(new BNullWidget());
      this.wrapper.setTop(new BLabel(BMakeWidget.text("actionBatch"), BHalign.left));
      this.wrapper.setCenter(this.treePane);
   }

   @Override
   public void load() {
      BObject[] drawingObjects = this.mw.getDrawingObjects();
      if (ClassUtil.anyNull(drawingObjects)) {
         this.command.setEnabled(false);
      } else {
         this.command.setEnabled(true);
         TreeModel model = this.tree.getModel();
         Array<BMwActionBatch.Node> arr = new Array(actions(drawingObjects[0], model));

         for (int i = 1; i < drawingObjects.length; i++) {
            arr = arr.intersection(actions(drawingObjects[i], model));
         }

         this.roots = (TreeNode[])arr.trim();
         if (this.roots.length == 0) {
            this.command.setEnabled(false);
            if (this.command.isSelected()) {
               BMwConfig config = this.mw.getConfig(BMwBoundLabel.TYPE);
               if (null != config) {
                  config.getCommand().setSelected(true);
               }
            }
         }
      }
   }

   @Override
   public BWidget[] makePxWidgets(WidgetCopier wc) {
      TreeNode[] sel = this.orderedSelectedNodes();
      if (sel == null) {
         return null;
      } else if (sel.length == 0) {
         return null;
      } else {
         List<BWidget> arrButtons = new ArrayList<>();
         BObject[] objects = this.mw.getDrawingObjects();
         BOrd[] ords = this.mw.getOrds();
         int x = 0;

         for (int i = 0; i < objects.length; i++) {
            BComponent comp = (BComponent)objects[i];
            int y = 0;

            for (int j = 0; j < sel.length; j++) {
               BMwActionBatch.Node node = (BMwActionBatch.Node)sel[j];
               BButton btn = (BButton)BTypeSpec.make("kitPx:ImageButton").getInstance();
               btn.setLayout(BLayout.make(x + "," + y + ",100,20"));
               btn.setText(node.displayName);
               BBinding bnd = (BBinding)BTypeSpec.make("kitPx:ActionBinding").getInstance();

               try {
                  bnd.getClass().getMethod("setWidgetEvent", String.class).invoke(bnd, "actionPerformed");
               } catch (Exception var15) {
                  throw new BajaRuntimeException(var15);
               }

               addBinding(btn, bnd, BOrd.make(ords[i] + "|slot:" + node.name).normalize());
               arrButtons.add(btn);
               y += 20;
            }

            x += 100;
         }

         return arrButtons.toArray(new BWidget[0]);
      }
   }

   @Override
   public void pickle(PropertyManager propMgr) {
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
   }

   private static BMwActionBatch.Node[] actions(BObject obj, TreeModel model) {
      List<BMwActionBatch.Node> arr = new ArrayList<>();
      if (obj instanceof BComplex) {
         BComplex comp = (BComplex)obj;
         Action[] a = comp.getActionsArray();

         for (int i = 0; i < a.length; i++) {
            if (!Flags.isHidden(comp, a[i])) {
               arr.add(new BMwActionBatch.Node(model, a[i].getName(), comp.getDisplayName(a[i], null)));
            }
         }
      }

      return arr.toArray(new BMwActionBatch.Node[0]);
   }

   class Cmd extends ToggleCommand {
      Cmd() {
         super(BMwActionBatch.this, BMakeWidget.text("actionBatch"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwActionBatch.this.mw.getRightPane().setContent(BMwActionBatch.this.wrapper);
            BMwActionBatch.this.mw.getLeftPane().setContent(BMwActionBatch.this);
         }

         return null;
      }
   }

   static class Node extends TreeNode {
      private static final BImage icon = BPxEditorPane.icon("makeWidget.actionIcon");
      private String name;
      private String displayName;

      Node(TreeModel model, String name, String displayName) {
         super(model);
         this.name = name;
         this.displayName = displayName;
      }

      public boolean equals(Object o) {
         BMwActionBatch.Node pm = (BMwActionBatch.Node)o;
         return !this.name.equals(pm.name) ? false : this.displayName.equals(pm.displayName);
      }

      public int hashCode() {
         throw new UnsupportedOperationException();
      }

      public String getText() {
         return this.displayName;
      }

      public BImage getIcon() {
         return icon;
      }

      public final int getChildCount() {
         return 0;
      }

      public final TreeNode getChild(int index) {
         throw new IllegalStateException();
      }
   }
}
