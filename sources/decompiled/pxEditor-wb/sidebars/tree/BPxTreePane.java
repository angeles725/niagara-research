package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.studio.commands.Reorg;
import javax.baja.gx.BImage;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BPxSideBar;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BToolBar;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BTreePane;
import javax.baja.util.Lexicon;

@NiagaraType
public class BPxTreePane extends BPxSideBar {
   public static final Type TYPE = Sys.loadType(BPxTreePane.class);
   private static Lexicon LEX = Lexicon.make("pxEditor");
   private static String DESC = LEX.getText("tree.label");
   private static BImage ICON = BImage.make(LEX.getText("tree.icon"));
   private BAbstractButton bringToFront;
   private BAbstractButton bringForwardOne;
   private BAbstractButton sendToBack;
   private BAbstractButton sendBackOne;
   private BPxTree tree;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPxTreePane(BPxEditorPane editorPane) {
      super(editorPane.getPxEditor());
      BToolBar toolbar = new BToolBar();
      toolbar.add(null, this.bringToFront = this.newButton(new Reorg(editorPane, true, false)));
      toolbar.add(null, this.bringForwardOne = this.newButton(new Reorg(editorPane, true, true)));
      toolbar.add(null, this.sendBackOne = this.newButton(new Reorg(editorPane, false, true)));
      toolbar.add(null, this.sendToBack = this.newButton(new Reorg(editorPane, false, false)));
      this.bringToFront.setEnabled(false);
      this.bringForwardOne.setEnabled(false);
      this.sendToBack.setEnabled(false);
      this.sendBackOne.setEnabled(false);
      BEdgePane edge = new BEdgePane();
      edge.setRight(toolbar);
      BEdgePane contents = new BEdgePane();
      contents.setTop(edge);
      contents.setCenter(new BTreePane(this.tree = new BPxTree(editorPane.getPxEditor())));
      this.setContent(contents);
   }

   public void updateEnabledButtons(boolean enabled) {
      this.bringToFront.setEnabled(enabled);
      this.bringForwardOne.setEnabled(enabled);
      this.sendToBack.setEnabled(enabled);
      this.sendBackOne.setEnabled(enabled);
   }

   @Override
   public BImage getSideBarIcon() {
      return ICON;
   }

   @Override
   public String getSideBarDescription() {
      return DESC;
   }
}
