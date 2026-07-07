package com.tridium.px.editor.make;

import com.tridium.workbench.util.PropertyManager;
import javax.baja.agent.BIAgent;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BLayout;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BValueBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.ToggleCommand;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperty(
   name = "content",
   type = "BWidget",
   defaultValue = "new BNullWidget()"
)
public abstract class BMwConfig extends BWidget implements BIAgent {
   public static final Property content = newProperty(0, new BNullWidget(), null);
   public static final Type TYPE = Sys.loadType(BMwConfig.class);
   protected BMakeWidget mw;
   protected ToggleCommand command;

   public BWidget getContent() {
      return (BWidget)this.get(content);
   }

   public void setContent(BWidget v) {
      this.set(content, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BMwConfig() {
   }

   protected BMwConfig(BMakeWidget mw) {
      this.mw = mw;
   }

   public void computePreferredSize() {
      this.getContent().computePreferredSize();
      this.setPreferredSize(this.getContent().getPreferredWidth(), this.getContent().getPreferredHeight());
   }

   public void doLayout(BWidget[] children) {
      this.getContent().setBounds(0.0, 0.0, this.getWidth(), this.getHeight());
   }

   public final ToggleCommand getCommand() {
      return this.command;
   }

   public void setMakeWidget(BMakeWidget mw) {
      this.mw = mw;
   }

   public BTypeSpec getSuperceedingType() {
      return null;
   }

   public abstract void load();

   public abstract void setWorkingWidget();

   public int columnCount() {
      return 1;
   }

   public abstract BWidget[] makePxWidgets(WidgetCopier var1);

   public abstract void pickle(PropertyManager var1);

   public abstract void unpickle(PropertyManager var1);

   public static void addBinding(BWidget widget, BBinding bnd, BOrd ord) {
      bnd.setOrd(ord);
      Slot slot = bnd.getSlot("ord");
      bnd.setFlags(slot, bnd.getFlags(slot) | 1);
      if (bnd instanceof BValueBinding) {
         slot = bnd.getSlot("hyperlink");
         bnd.setFlags(slot, bnd.getFlags(slot) | 1);
      }

      widget.add(null, bnd);
   }

   public final BWidget[] makeDefaultWidgets(WidgetCopier wc) {
      if (this.mw.getWorkingWidget() == null) {
         return null;
      } else {
         BObject[] objects = this.mw.getDrawingObjects();
         BOrd[] ords = this.mw.getOrds();
         BWidget[] widgets = new BWidget[objects.length];

         for (int i = 0; i < objects.length; i++) {
            widgets[i] = wc.copyWidget(this.mw.getWorkingWidget());
            BBinding[] bindings = (BBinding[])widgets[i].getChildren(BBinding.class);
            if (bindings.length > 0) {
               bindings[0].setOrd(ords[i]);
               if (bindings[0] instanceof BValueBinding) {
                  BValueBinding v = (BValueBinding)bindings[0];
                  if (!v.getHyperlink().equals(BOrd.NULL)) {
                     v.setHyperlink(ords[i]);
                  }
               }
            }
         }

         double y = 0.0;

         for (int ix = 0; ix < widgets.length; ix++) {
            BLayout ly = widgets[ix].getLayout();
            widgets[ix].setLayout(BLayout.make(0.0, 0, y, 0, ly.getWidth(), ly.getWidthUnit(), ly.getHeight(), ly.getHeightUnit()));
            y += ly.getHeight();
         }

         return widgets;
      }
   }
}
