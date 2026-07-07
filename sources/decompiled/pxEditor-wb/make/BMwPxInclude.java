package com.tridium.px.editor.make;

import com.tridium.px.editor.sidebars.cellsheet.celleditors.BVariablesCE;
import com.tridium.util.ClassUtil;
import com.tridium.workbench.fieldeditors.BOrdFE;
import com.tridium.workbench.util.PropertyManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.Action;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BLabel;
import javax.baja.ui.BLayout;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.list.BList;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.px.BPxInclude;
import javax.baja.ui.px.PxDecoder;
import javax.baja.workbench.BWbPlugin;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
@NiagaraAction(
   name = "fileOrdFeModified"
)
public class BMwPxInclude extends BMwConfig {
   public static final Action fileOrdFeModified = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BMwPxInclude.class);
   private BOrd fileOrd;
   private final BLabel ordLabel = new BLabel(BMakeWidget.text("pxFile"));
   private final BLabel listLabel = new BLabel(BMakeWidget.text("chooseVariables"));
   private final BOrdFE ordFe;
   private final BList list = new BList();
   private BWidget root = null;
   private String[] keys = null;
   private String unpickleKeys;
   private String unpickleKeysSelected;

   public void fileOrdFeModified() {
      this.invoke(fileOrdFeModified, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwPxInclude() {
      this.command = new BMwPxInclude.Cmd();
      this.ordFe = new BOrdFE();
      this.ordFe.allowLinkButton(false);
      this.ordFe.getTextField().setVisibleColumns(35);
      this.ordFe.loadValue(BOrd.NULL);
      BGridPane grid = new BGridPane(1);
      grid.setValign(BValign.top);
      grid.setHalign(BHalign.left);
      grid.add(null, this.ordLabel);
      grid.add(null, this.ordFe);
      grid.add(null, this.listLabel);
      BEdgePane edge = new BEdgePane();
      edge.setTop(grid);
      edge.setCenter(this.list);
      this.setContent(new BBorderPane(edge, BBorder.groove));
      this.linkTo(this.ordFe, BWbPlugin.pluginModified, fileOrdFeModified);
   }

   public void doFileOrdFeModified() {
      this.root = null;
      this.keys = null;

      try {
         this.fileOrd = (BOrd)this.ordFe.saveValue();
         if (!this.fileOrd.isNull()) {
            BOrd baseOrd = this.mw.getEditor().getWbShell().getActiveOrd();
            BOrd ord = BOrd.make(baseOrd, this.fileOrd);
            BIFile file = (BIFile)ord.resolve().get();
            PxDecoder decoder = new PxDecoder(file);
            this.root = decoder.decodeDocument();
            this.keys = BVariablesCE.getVariableKeys(this.root);
            this.list.getSelection().deselectAll();
            this.list.removeAllItems();

            for (int i = 0; i < this.keys.length; i++) {
               this.list.addItem(this.keys[i]);
            }

            if (this.unpickleKeys != null) {
               if (TextUtil.join(this.keys, ';').equals(this.unpickleKeys)) {
                  String[] vtokens = TextUtil.split(this.unpickleKeysSelected, ';');

                  for (int i = 0; i < this.keys.length; i++) {
                     if (Boolean.valueOf(vtokens[i])) {
                        this.list.getSelection().select(i);
                     }
                  }
               }

               this.unpickleKeys = null;
               this.unpickleKeysSelected = null;
            }
         }
      } catch (Exception var7) {
         Logger.getLogger("pxEditor").warning("BMwPxInclude: " + var7.getMessage());
      }
   }

   @Override
   public void load() {
      BObject[] objects = this.mw.getDrawingObjects();
      BMwConfig config = this.mw.getConfig(BMwBoundLabel.TYPE);
      if (null != config) {
         config.getCommand().setEnabled(!ClassUtil.anyNull(objects));
      }
   }

   @Override
   public void setWorkingWidget() {
      if (!this.mw.isConstructing()) {
         BPxInclude inc = new BPxInclude();
         Property prop = BPxInclude.ord;
         inc.setOrd(BMakeWidget.placeholder);
         inc.setFlags(prop, inc.getFlags(prop) | 1);
         this.mw.setWorkingWidget(inc);
      }
   }

   @Override
   public BWidget[] makePxWidgets(WidgetCopier wc) {
      if (this.fileOrd == null) {
         return new BWidget[0];
      } else {
         BOrd baseOrd = this.mw.getEditor().getWbShell().getActiveOrd();
         List<String> keyArr = new ArrayList<>();
         if (this.keys != null && this.keys.length > 0) {
            for (int i = 0; i < this.keys.length; i++) {
               if (this.list.isSelected(i)) {
                  keyArr.add(this.keys[i]);
               }
            }
         }

         String[] facKeys = keyArr.toArray(new String[0]);
         double w = 100.0;
         double h = 100.0;
         if (this.root != null) {
            this.root.computePreferredSize();
            w = this.root.getPreferredWidth();
            h = this.root.getPreferredHeight();
         }

         BPxInclude working = (BPxInclude)this.mw.getWorkingWidget();
         working.setOrd(BOrd.NULL);
         BObject[] objects = this.mw.getDrawingObjects();
         BOrd[] ords = this.mw.getOrds();
         BWidget[] widgets = new BWidget[objects.length];
         double y = 0.0;
         int n = 0;

         for (int ix = 0; ix < objects.length; ix++) {
            BPxInclude inc = (BPxInclude)wc.copyWidget(working);
            inc.fw(205, baseOrd, null, null, null);
            inc.setOrd(this.fileOrd);
            if (facKeys.length > 0) {
               inc.setVariables(makeVariables(facKeys, ords[ix]));
            }

            inc.setLayout(BLayout.make(0.0, 0, y, 0, w, 0, h, 0));
            widgets[n++] = inc;
            y += h;
         }

         return widgets;
      }
   }

   private static BFacets makeVariables(String[] keys, BOrd ord) {
      BString[] values = new BString[keys.length];

      for (int i = 0; i < keys.length; i++) {
         values[i] = BString.make(ord.toString());
      }

      return BFacets.make(keys, values);
   }

   @Override
   public void pickle(PropertyManager propMgr) {
      if (this.fileOrd != null) {
         propMgr.set("mwPxIncludeFileOrd", this.fileOrd.toString());
      }

      if (this.keys != null) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < this.keys.length; i++) {
            if (i > 0) {
               sb.append(";");
            }

            sb.append(this.list.isSelected(i) ? "true" : "false");
         }

         propMgr.set("mwPxIncludeKeys", TextUtil.join(this.keys, ';'));
         propMgr.set("mwPxIncludeKeysSelected", sb.toString());
      }
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
      this.fileOrd = BOrd.make(propMgr.get("mwPxIncludeFileOrd", "null"));
      this.ordFe.loadValue(this.fileOrd);
      this.unpickleKeys = propMgr.get("mwPxIncludeKeys", "");
      this.unpickleKeysSelected = propMgr.get("mwPxIncludeKeysSelected", "");
   }

   private class Cmd extends ToggleCommand {
      Cmd() {
         super(BMwPxInclude.this, BMakeWidget.text("fromPxInclude"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwPxInclude.this.mw.getRightPane().setContent(BMwPxInclude.this.mw.sheet());
            BMwPxInclude.this.mw.getLeftPane().setContent(BMwPxInclude.this);
            BMwPxInclude.this.setWorkingWidget();
            BMwPxInclude.this.mw.editWorkingWidget();
         }

         return null;
      }
   }
}
