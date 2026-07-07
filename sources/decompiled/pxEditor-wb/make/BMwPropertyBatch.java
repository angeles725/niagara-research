package com.tridium.px.editor.make;

import com.tridium.util.ClassUtil;
import com.tridium.workbench.sidebars.BPaletteSideBar;
import com.tridium.workbench.util.PropertyManager;
import javax.baja.gx.BInsets;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Action;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BBorder;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BLabel;
import javax.baja.ui.BLayout;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BRadioButton;
import javax.baja.ui.BValueBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.tree.TreeModel;
import javax.baja.ui.tree.TreeNode;
import javax.baja.ui.tree.TreeSelection;
import javax.baja.util.BTypeSpec;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;
import javax.baja.workbench.fieldeditor.BWbFieldEditorBinding;
import javax.baja.workbench.nav.tree.BNavTree;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
@NiagaraActions({@NiagaraAction(
      name = "palettesToggled"
   ), @NiagaraAction(
      name = "frozenToggled"
   )})
public class BMwPropertyBatch extends BMwBatch {
   public static final Action palettesToggled = newAction(0, null);
   public static final Action frozenToggled = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BMwPropertyBatch.class);
   private BEdgePane wrapper = new BEdgePane();
   private PropertyNodeFactory rootFactory = new PropertyNodeFactory() {
      @Override
      protected PropertyNode make(BObject object, String name, String displayName) {
         return new PropertyNode(BMwPropertyBatch.this.tree.getModel(), object, name, displayName);
      }
   };
   private ToggleCommand togLabels = new ToggleCommand(this, BMakeWidget.text("propertyLabels"));
   private ToggleCommand togFEs = new ToggleCommand(this, BMakeWidget.text("propertyFieldEditors"));
   private ToggleCommand togPalettes = new ToggleCommand(this, BMakeWidget.text("propertyPalettes"));
   private BCheckBox chkLabels;
   private BCheckBox chkFEs;
   private BCheckBox chkPalettes;
   private BPaletteSideBar palette = new BPaletteSideBar();
   private BNavTree paletteTree = this.palette.getNavTree();
   private BRadioButton rbLabelRow;
   private BRadioButton rbLabelCol;
   private BRadioButton rbFieldEditorRow;
   private BRadioButton rbFieldEditorCol;

   public void palettesToggled() {
      this.invoke(palettesToggled, null, null);
   }

   public void frozenToggled() {
      this.invoke(frozenToggled, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwPropertyBatch() {
      this.command = new BMwPropertyBatch.Cmd();
      this.tree.setMultipleSelection(true);
      this.tree.setModel(new TreeModel() {
         public int getRootCount() {
            return BMwPropertyBatch.this.roots.length;
         }

         public TreeNode getRoot(int index) {
            return BMwPropertyBatch.this.roots[index];
         }
      });
      this.tree.setSelection(new BMwPropertyBatch.Selection());
      this.chkLabels = new BCheckBox(this.togLabels);
      this.chkFEs = new BCheckBox(this.togFEs);
      this.chkPalettes = new BCheckBox(this.togPalettes);
      this.togLabels.setSelected(true);
      this.togFEs.setSelected(true);
      this.togPalettes.setSelected(false);
      BConstrainedPane consPalette = new BConstrainedPane(this.palette);
      consPalette.setMinHeight(200.0);
      consPalette.setMaxHeight(200.0);
      this.palette.setVisible(false);
      ToggleCommandGroup<ToggleCommand> grp = new ToggleCommandGroup();
      this.rbLabelRow = new BRadioButton(grp, BMakeWidget.text("byRow"));
      this.rbLabelCol = new BRadioButton(grp, BMakeWidget.text("byCol"));
      grp = new ToggleCommandGroup();
      this.rbFieldEditorRow = new BRadioButton(grp, BMakeWidget.text("byRow"));
      this.rbFieldEditorCol = new BRadioButton(grp, BMakeWidget.text("byCol"));
      BLabel lab1 = new BLabel(BMakeWidget.text("propertyLabelLayout"), BHalign.left);
      BLabel lab2 = new BLabel(BMakeWidget.text("propertyFieldEditorLayout"), BHalign.left);
      lab1.computePreferredSize();
      lab2.computePreferredSize();
      double max = Math.max(lab1.getPreferredWidth(), lab2.getPreferredWidth());
      BConstrainedPane cons1 = new BConstrainedPane(lab1);
      BConstrainedPane cons2 = new BConstrainedPane(lab2);
      cons1.setMinWidth(max);
      cons2.setMinWidth(max);
      BGridPane g1 = new BGridPane(3);
      g1.add(null, cons1);
      g1.add(null, this.rbLabelRow);
      g1.add(null, this.rbLabelCol);
      BGridPane g2 = new BGridPane(3);
      g2.add(null, cons2);
      g2.add(null, this.rbFieldEditorRow);
      g2.add(null, this.rbFieldEditorCol);
      BGridPane grid = new BGridPane(1);
      grid.setValign(BValign.top);
      grid.setHalign(BHalign.left);
      grid.add(null, this.chkLabels);
      grid.add(null, this.chkFEs);
      grid.add(null, this.chkPalettes);
      grid.add(null, g1);
      grid.add(null, g2);
      BEdgePane edge = new BEdgePane();
      edge.setTop(grid);
      edge.setCenter(new BBorderPane(consPalette, BBorder.none, BInsets.make(5.0, 0.0, 0.0, 0.0)));
      this.setContent(new BBorderPane(edge, BBorder.groove));
      this.wrapper.setTop(new BLabel(BMakeWidget.text("propertyBatch"), BHalign.left));
      this.wrapper.setCenter(this.treePane);
      this.linkTo(this.chkPalettes, BCheckBox.actionPerformed, palettesToggled);
   }

   public void doFrozenToggled() {
      this.load();
      this.mw.relayout();
      this.mw.repaint();
   }

   public void doPalettesToggled() {
      this.palette.setVisible(this.togPalettes.isSelected());
      this.mw.repaint();
   }

   @Override
   public void load() {
      BObject[] objects = this.mw.getDrawingObjects();
      if (ClassUtil.anyNull(objects)) {
         this.command.setEnabled(false);
      } else {
         this.command.setEnabled(true);
         Array<PropertyNode> arr = new Array(this.rootFactory.makeAll(objects[0]));

         for (int i = 1; i < objects.length; i++) {
            arr = arr.intersection(this.rootFactory.makeAll(objects[i]));
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
      TreeNode[] nodes = this.orderedSelectedNodes();
      if (nodes == null) {
         return null;
      } else if (nodes.length == 0) {
         return null;
      } else if (!this.togLabels.isSelected() && !this.togFEs.isSelected() && !this.togPalettes.isSelected()) {
         return null;
      } else {
         BObject[] objects = this.mw.getDrawingObjects();
         BWidget[] widgets = new BWidget[objects.length];

         for (int i = 0; i < objects.length; i++) {
            var pane = (BGridPane & BGridPane)(widgets[i] = new BGridPane());
            if (this.rbLabelRow.isSelected()) {
               if (this.rbFieldEditorRow.isSelected()) {
                  int columns = 0;
                  if (this.togLabels.isSelected()) {
                     columns++;
                  }

                  if (this.togFEs.isSelected()) {
                     columns++;
                  }

                  if (this.togPalettes.isSelected()) {
                     columns++;
                  }

                  pane.setColumnCount(columns);
               } else {
                  pane.setColumnCount(1);
               }
            } else if (this.rbFieldEditorRow.isSelected()) {
               int columnsx = 0;
               if (this.togLabels.isSelected()) {
                  columnsx += nodes.length;
               }

               if (this.togFEs.isSelected()) {
                  columnsx += nodes.length;
               }

               if (this.togPalettes.isSelected()) {
                  columnsx += nodes.length;
               }

               pane.setColumnCount(columnsx);
            } else {
               pane.setColumnCount(nodes.length);
            }

            if (!this.rbLabelRow.isSelected() && !this.rbFieldEditorRow.isSelected()) {
               if (this.togLabels.isSelected()) {
                  for (int j = 0; j < nodes.length; j++) {
                     pane.add(null, this.getLabel((PropertyNode)nodes[j]));
                  }
               }

               if (this.togPalettes.isSelected()) {
                  for (int j = 0; j < nodes.length; j++) {
                     pane.add(null, this.getPalette(i, (PropertyNode)nodes[j]));
                  }
               }

               if (this.togFEs.isSelected()) {
                  for (int j = 0; j < nodes.length; j++) {
                     pane.add(null, this.getFieldEditor(i, (PropertyNode)nodes[j]));
                  }
               }
            } else {
               for (int j = 0; j < nodes.length; j++) {
                  PropertyNode node = (PropertyNode)nodes[j];
                  if (this.togLabels.isSelected()) {
                     pane.add(null, this.getLabel(node));
                  }

                  if (this.togPalettes.isSelected()) {
                     pane.add(null, this.getPalette(i, node));
                  }

                  if (this.togFEs.isSelected()) {
                     pane.add(null, this.getFieldEditor(i, node));
                  }
               }
            }

            if (pane.getChildWidgets().length == 1) {
               widgets[i] = pane.getChildWidgets()[0];
               pane.remove(widgets[i]);
            }
         }

         for (int i = 0; i < widgets.length; i++) {
            widgets[i].computePreferredSize();
            if (i == 0) {
               widgets[i].setLayout(BLayout.make("0,0,pref,pref"));
            } else {
               widgets[i].setLayout(BLayout.make("0," + (widgets[i - 1].getLayout().getY() + widgets[i - 1].getPreferredHeight()) + ",pref,pref"));
               widgets[i].setLocation(0.0, widgets[i - 1].getLayout().getY() + widgets[i - 1].getPreferredHeight());
            }
         }

         return widgets;
      }
   }

   private BLabel getLabel(PropertyNode node) {
      BLabel label = new BLabel(node.displayNamePath());
      label.setHalign(BHalign.left);
      label.setLayout(BLayout.make("0,0,pref,pref"));
      return label;
   }

   private BWidget getPalette(int index, PropertyNode node) {
      BObject obj = this.paletteTree.getSelectedObject();
      if (obj != null && obj instanceof BWidget) {
         BWidget widget = this.mw.getEditorPane().getPxEditor().cloneWidget((BWidget)obj);
         BOrd[] ords = this.mw.getOrds();
         BBinding[] bindings = (BBinding[])widget.getChildren(BBinding.class);
         if (bindings.length > 0) {
            bindings[0].setOrd(ords[index]);
            if (bindings[0] instanceof BValueBinding) {
               BValueBinding v = (BValueBinding)bindings[0];
               if (!v.getHyperlink().equals(BOrd.NULL)) {
                  v.setHyperlink(ords[index]);
               }
            }
         }

         widget.setLayout(BLayout.make("0,0,pref,pref"));
         return widget;
      } else {
         return new BNullWidget();
      }
   }

   private BWbFieldEditor getFieldEditor(int index, PropertyNode node) {
      BOrd[] ords = this.mw.getOrds();
      BWbFieldEditor editor = (BWbFieldEditor)BTypeSpec.make("kitPx:GenericFieldEditor").getInstance();
      addBinding(editor, new BWbFieldEditorBinding(), BOrd.make(ords[index] + "|slot:" + node.namePath()).normalize());
      editor.setLayout(BLayout.make("0,0,pref,pref"));
      editor.loadValue(node.object(), null);
      return editor;
   }

   @Override
   public void pickle(PropertyManager propMgr) {
      propMgr.setb("mwPropertyBatchLabels", this.togLabels.isSelected());
      propMgr.setb("mwPropertyBatchFEs", this.togFEs.isSelected());
      propMgr.setb("mwPropertyBatchPalettes", this.togPalettes.isSelected());
      propMgr.setb("mwPropertyBatchOrdRow", this.rbLabelRow.isSelected());
      propMgr.setb("mwPropertyBatchPropRow", this.rbFieldEditorRow.isSelected());
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
      this.togLabels.setSelected(propMgr.getb("mwPropertyBatchLabels", true));
      this.togFEs.setSelected(propMgr.getb("mwPropertyBatchFEs", true));
      this.togPalettes.setSelected(propMgr.getb("mwPropertyBatchPalettes", false));
      this.palette.setVisible(this.togPalettes.isSelected());
      if (propMgr.getb("mwPropertyBatchOrdRow", true)) {
         this.rbLabelRow.setSelected(true);
      } else {
         this.rbLabelCol.setSelected(true);
      }

      if (propMgr.getb("mwPropertyBatchPropRow", true)) {
         this.rbFieldEditorRow.setSelected(true);
      } else {
         this.rbFieldEditorCol.setSelected(true);
      }
   }

   class Cmd extends ToggleCommand {
      Cmd() {
         super(BMwPropertyBatch.this, BMakeWidget.text("propertyBatch"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwPropertyBatch.this.mw.getRightPane().setContent(BMwPropertyBatch.this.wrapper);
            BMwPropertyBatch.this.mw.getLeftPane().setContent(BMwPropertyBatch.this);
         }

         return null;
      }
   }

   class Selection extends TreeSelection {
      public void select(TreeNode node) {
         super.select(node);
         this.deselectNonSiblings(this.root(node), this.siblings(node));
      }

      private void deselectNonSiblings(TreeNode node, TreeNode[] siblings) {
         if (!this.isSibling(node, siblings)) {
            this.deselect(node);
         }

         for (int i = 0; i < node.getChildCount(); i++) {
            this.deselectNonSiblings(node.getChild(i), siblings);
         }
      }

      private boolean isSibling(TreeNode node, TreeNode[] siblings) {
         for (int i = 0; i < siblings.length; i++) {
            if (node == siblings[i]) {
               return true;
            }
         }

         return false;
      }

      private TreeNode[] siblings(TreeNode node) {
         TreeNode parent = node.getParent();
         if (parent == null) {
            return new TreeNode[]{node};
         } else {
            int len = parent.getChildCount();
            TreeNode[] arr = new TreeNode[len];

            for (int i = 0; i < len; i++) {
               arr[i] = parent.getChild(i);
            }

            return arr;
         }
      }

      private TreeNode root(TreeNode node) {
         for (TreeNode parent = node.getParent(); parent != null; parent = parent.getParent()) {
            node = parent;
         }

         return node;
      }
   }
}
