package com.tridium.px.editor.make;

import com.tridium.workbench.sidebars.BPaletteSideBar;
import com.tridium.workbench.util.PropertyManager;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BValueBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.tree.TreeModel;
import javax.baja.workbench.nav.tree.BNavTree;
import javax.baja.workbench.nav.tree.NavTreeController;
import javax.baja.workbench.nav.tree.NavTreeNode;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
@NiagaraActions({@NiagaraAction(
      name = "paletteChanged"
   ), @NiagaraAction(
      name = "hyperlinkToggled"
   ), @NiagaraAction(
      name = "palettePreviewToggled",
      parameterType = "BBoolean",
      defaultValue = "BBoolean.FALSE"
   )})
public class BMwFromPalette extends BMwConfig {
   public static final Action paletteChanged = newAction(0, null);
   public static final Action hyperlinkToggled = newAction(0, null);
   public static final Action palettePreviewToggled = newAction(0, BBoolean.FALSE, null);
   public static final Type TYPE = Sys.loadType(BMwFromPalette.class);
   private static boolean palettePreviewSelected = false;
   private BPaletteSideBar palette = new BPaletteSideBar(palettePreviewSelected);
   private BNavTree paletteTree = this.palette.getNavTree();
   private BCheckBox chkHyperlink;
   private ToggleCommand hyperlink;
   private boolean hyperlinkState;
   private boolean ignoreToggle = false;

   public void paletteChanged() {
      this.invoke(paletteChanged, null, null);
   }

   public void hyperlinkToggled() {
      this.invoke(hyperlinkToggled, null, null);
   }

   public void palettePreviewToggled(BBoolean parameter) {
      this.invoke(palettePreviewToggled, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwFromPalette() {
      this.paletteTree.setMultipleSelection(false);
      this.paletteTree.setController(new NavTreeController() {
         public void mouseReleased(BMouseEvent evt) {
         }
      });
      this.command = new BMwFromPalette.Cmd();
   }

   @Override
   public void setMakeWidget(BMakeWidget mw) {
      super.setMakeWidget(mw);
      this.hyperlink = new ToggleCommand(mw, BMakeWidget.text("hyperlink"));
      this.chkHyperlink = new BCheckBox(this.hyperlink);
      BEdgePane edge = new BEdgePane();
      edge.setCenter(this.palette);
      edge.setBottom(this.chkHyperlink);
      this.setContent(edge);
      this.linkTo(this.chkHyperlink, BCheckBox.actionPerformed, hyperlinkToggled);
      this.linkTo(this.paletteTree, BNavTree.selectionModified, paletteChanged);
      this.linkTo(this.palette, BPaletteSideBar.previewToggled, palettePreviewToggled);
   }

   public void doPaletteChanged() {
      this.setWorkingWidget();
      this.mw.editWorkingWidget();
   }

   public void doHyperlinkToggled() {
      if (!this.ignoreToggle) {
         BBinding[] bindings = (BBinding[])this.mw.getWorkingWidget().getChildren(BBinding.class);
         BValueBinding bnd = (BValueBinding)bindings[0];
         if (this.hyperlink.isSelected()) {
            bnd.setHyperlink(BMakeWidget.placeholder);
         } else {
            bnd.setHyperlink(BOrd.NULL);
         }

         this.mw.editWorkingWidget();
      }
   }

   public void doPalettePreviewToggled(BBoolean selected) {
      palettePreviewSelected = selected.getBoolean();
   }

   @Override
   public void load() {
   }

   @Override
   public void setWorkingWidget() {
      if (!this.mw.isConstructing()) {
         this.ignoreToggle = true;
         BObject o = this.paletteTree.getSelectedObject();
         if (o != null && o instanceof BWidget) {
            BWidget widget = this.mw.getEditorPane().getPxEditor().cloneWidget((BWidget)o);
            BBinding[] bindings = (BBinding[])widget.getChildren(BBinding.class);
            if (bindings.length > 0) {
               BBinding bnd = bindings[0];
               bnd.setOrd(BMakeWidget.placeholder);
               Slot slot = bnd.getSlot("ord");
               bnd.setFlags(slot, bnd.getFlags(slot) | 1);
               if (bnd instanceof BValueBinding) {
                  this.chkHyperlink.setEnabled(true);
                  this.hyperlink.setSelected(this.hyperlinkState);
                  slot = bnd.getSlot("hyperlink");
                  bnd.setFlags(slot, bnd.getFlags(slot) | 1);
                  if (this.hyperlink.isSelected()) {
                     ((BValueBinding)bnd).setHyperlink(BMakeWidget.placeholder);
                  }
               } else {
                  this.disableHyperlink();
               }
            } else {
               this.disableHyperlink();
            }

            this.ignoreToggle = false;
            this.mw.setWorkingWidget(widget);
         } else {
            this.disableHyperlink();
            this.mw.setWorkingWidget(null);
         }
      }
   }

   @Override
   public BWidget[] makePxWidgets(WidgetCopier wc) {
      return this.makeDefaultWidgets(wc);
   }

   @Override
   public void pickle(PropertyManager propMgr) {
      try {
         propMgr.setb("mwPaletteHyperlink", this.hyperlink.isSelected());
         propMgr.setb("mwPaletteChkHyperlink", this.chkHyperlink.isEnabled());
         BINavNode n = (BINavNode)this.paletteTree.getSelectedObject();
         propMgr.set("mwPaletteOrd", n == null ? "" : n.getNavOrd().toString());
      } catch (Exception var3) {
         throw new BajaRuntimeException(var3);
      }
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
      this.hyperlink.setSelected(propMgr.getb("mwPaletteHyperlink", false));
      this.chkHyperlink.setEnabled(propMgr.getb("mwPaletteChkHyperlink", false));
      this.hyperlinkState = this.hyperlink.isSelected();
      BOrd ord = BOrd.make(propMgr.get("mwPaletteOrd", ""));
      this.paletteTree.getSelection().deselectAll();
      TreeModel model = this.paletteTree.getModel();

      for (int i = 0; i < model.getRootCount(); i++) {
         this.checkNode((NavTreeNode)model.getRoot(i), ord);
      }
   }

   private void checkNode(NavTreeNode node, BOrd ord) {
      if (node.getNavNode().getNavOrd().equals(ord)) {
         this.paletteTree.getSelection().select(node);
         this.paletteTree.expandToNode(node);
         this.paletteTree.scrollNodeToVisible(node);
      } else {
         NavTreeNode[] kids = node.getChildren();

         for (int i = 0; i < node.getChildCount(); i++) {
            this.checkNode(kids[i], ord);
         }
      }
   }

   private void disableHyperlink() {
      this.hyperlinkState = this.hyperlink.isSelected();
      this.hyperlink.setSelected(false);
      this.chkHyperlink.setEnabled(false);
   }

   private class Cmd extends ToggleCommand {
      Cmd() {
         super(BMwFromPalette.this, BMakeWidget.text("fromPalette"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwFromPalette.this.mw.getRightPane().setContent(BMwFromPalette.this.mw.sheet());
            BMwFromPalette.this.mw.getLeftPane().setContent(BMwFromPalette.this);
            BMwFromPalette.this.setWorkingWidget();
            BMwFromPalette.this.mw.editWorkingWidget();
         }

         return null;
      }
   }
}
