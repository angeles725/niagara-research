package com.tridium.workbench.fieldeditors;

import javax.baja.gx.BBrush;
import javax.baja.gx.BImage;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.list.ListRenderer;
import javax.baja.ui.list.ListRenderer.Item;
import javax.baja.util.BFormat;
import javax.baja.util.LexiconModule;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Boolean"}
   )}
)
public class BBooleanFE extends BWbFieldEditor {
   public static final Type TYPE = Sys.loadType(BBooleanFE.class);
   private static final LexiconModule LEX = LexiconModule.make("baja");
   private static final String TRUE_ICON_FACET_KEY = "trueIcon";
   private static final String FALSE_ICON_FACET_KEY = "falseIcon";
   private static final String TRUE_ICON_LEX_KEY = "true.icon";
   private static final String FALSE_ICON_LEX_KEY = "false.icon";
   String trueText = BBoolean.toString(true, null);
   String falseText = BBoolean.toString(false, null);
   BBrush fg = BBrush.NULL;
   BBrush bg = BBrush.NULL;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBooleanFE() {
      BListDropDown combo = new BListDropDown();
      combo.getList().setRenderer(new BBooleanFE.Renderer());
      this.setContent(combo);
      this.linkTo("lk0", combo, BListDropDown.valueModified, setModified);
      this.linkTo("lk1", combo, BListDropDown.actionPerformed, actionPerformed);
   }

   @Override
   protected void doSetReadonly(boolean readonly) {
      BListDropDown combo = (BListDropDown)this.getContent();
      combo.setDropDownEnabled(!readonly);
   }

   @Override
   protected void doLoadValue(BObject value, Context cx) {
      BImage trueIcon = null;
      BImage falseIcon = null;
      if (cx != null) {
         BObject t = cx.getFacet("trueText");
         BObject f = cx.getFacet("falseText");
         if (t != null) {
            this.trueText = BFormat.format(t.toString(), null, cx);
         }

         if (f != null) {
            this.falseText = BFormat.format(f.toString(), null, cx);
         }

         BObject ti = cx.getFacet("trueIcon");
         BObject fi = cx.getFacet("falseIcon");
         if (ti != null) {
            trueIcon = BImage.make(BFormat.format(ti.toString(), null, cx));
         }

         if (fi != null) {
            falseIcon = BImage.make(BFormat.format(fi.toString(), null, cx));
         }
      }

      if (trueIcon == null) {
         trueIcon = BImage.make(LEX.getText("true.icon", cx));
      }

      if (falseIcon == null) {
         falseIcon = BImage.make(LEX.getText("false.icon", cx));
      }

      boolean b = ((BIBoolean)value).getBoolean();
      BListDropDown combo = (BListDropDown)this.getContent();
      combo.getList().removeAllItems();
      combo.getList().addItem(falseIcon, this.falseText);
      combo.getList().addItem(trueIcon, this.trueText);
      combo.setSelectedItem(b ? this.trueText : this.falseText);
   }

   @Override
   protected BObject doSaveValue(BObject value, Context cx) {
      BListDropDown combo = (BListDropDown)this.getContent();
      boolean b = combo.getSelectedItem().equals(this.trueText);
      return BBoolean.make(b);
   }

   @Override
   public Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 306:
            this.fg = (BBrush)a;
            this.bg = (BBrush)b;
            this.repaint();
            return null;
         default:
            return super.fw(x, a, b, c, d);
      }
   }

   class Renderer extends ListRenderer {
      public BBrush getBackground(Item item) {
         return BBooleanFE.this.bg.isNull() ? super.getBackground(item) : BBooleanFE.this.bg;
      }

      public BBrush getForeground(Item item) {
         return BBooleanFE.this.fg.isNull() ? super.getForeground(item) : BBooleanFE.this.fg;
      }
   }
}
