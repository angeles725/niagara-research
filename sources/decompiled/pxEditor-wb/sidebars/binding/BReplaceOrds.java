package com.tridium.px.editor.sidebars.binding;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.px.editor.BPxEditor;
import javax.baja.sys.Action;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLabel;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.options.BMruTextDropDown;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.util.UiLexicon;

@NiagaraType
@NiagaraAction(
   name = "textChanged"
)
public class BReplaceOrds extends BOrdChanger {
   public static final Action textChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BReplaceOrds.class);
   BMruTextDropDown from = new BMruTextDropDown("ordReplaceFrom", 25);
   BMruTextDropDown to = new BMruTextDropDown("ordReplaceTo", 25);

   public void textChanged() {
      this.invoke(textChanged, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BReplaceOrds(BPxEditor editor, BOrd[] before) {
      super(editor, before);
      BGridPane finder = new BGridPane(1);
      finder.setHalign(BHalign.left);
      finder.setColumnAlign(BHalign.fill);
      finder.setStretchColumn(0);
      finder.add(null, new BLabel(UiLexicon.bajaui().getText("search.string"), BHalign.left));
      finder.add(null, this.from);
      finder.add(null, new BLabel(UiLexicon.bajaui().getText("replace.with"), BHalign.left));
      finder.add(null, this.to);
      this.edge.setTop(new BBorderPane(finder, 0.0, 0.0, 10.0, 0.0));
      this.linkTo(null, this.from, BMruTextDropDown.valueModified, textChanged);
      this.linkTo(null, this.to, BMruTextDropDown.valueModified, textChanged);
   }

   @Override
   protected BOrd after(BOrd ord) {
      String f = this.from.getText();
      String t = this.to.getText();
      return f.equals("") ? ord : BOrd.make(TextUtil.replace(ord.toString(), f, t));
   }

   @Override
   protected boolean selectable(int row) {
      return true;
   }

   public void doTextChanged() {
      for (int i = 0; i < this.before.length; i++) {
         if (this.selected[i]) {
            this.after[i] = this.after(this.before[i]);
         }
      }

      this.repaint();
   }
}
