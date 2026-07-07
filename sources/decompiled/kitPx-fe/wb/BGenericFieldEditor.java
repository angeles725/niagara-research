package com.tridium.kitpx;

import javax.baja.gx.BBrush;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.workbench.CannotSaveException;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "foreground",
      type = "BBrush",
      defaultValue = "BBrush.NULL"
   ), @NiagaraProperty(
      name = "background",
      type = "BBrush",
      defaultValue = "BBrush.NULL"
   )})
public class BGenericFieldEditor extends BWbFieldEditor {
   public static final Property foreground = newProperty(0, BBrush.NULL, null);
   public static final Property background = newProperty(0, BBrush.NULL, null);
   public static final Type TYPE = Sys.loadType(BGenericFieldEditor.class);

   public BBrush getForeground() {
      return (BBrush)this.get(foreground);
   }

   public void setForeground(BBrush v) {
      this.set(foreground, v, null);
   }

   public BBrush getBackground() {
      return (BBrush)this.get(background);
   }

   public void setBackground(BBrush v) {
      this.set(background, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BWbFieldEditor getInnerEditor() {
      BWidget content = this.getContent();
      return content instanceof BWbFieldEditor ? (BWbFieldEditor)content : null;
   }

   protected void doSetReadonly(boolean readonly) {
      BWbFieldEditor inner = this.getInnerEditor();
      if (inner != null) {
         inner.setReadonly(readonly);
      }
   }

   protected void doLoadValue(BObject value, Context context) throws Exception {
      BWbFieldEditor inner = this.getInnerEditor();
      if (inner == null || inner.getCurrentValue() == null || inner.getCurrentValue().getClass() != value.getClass()) {
         inner = makeFor(value, context);
         inner.setReadonly(this.isReadonly());
         this.linkTo(inner, actionPerformed, actionPerformed);
         this.linkTo(inner, setModified, setModified);
         this.setContent(inner);
      }

      inner.loadValue(value, context);
      this.syncColors();
   }

   protected BObject doSaveValue(BObject value, Context cx) throws CannotSaveException, Exception {
      BWbFieldEditor inner = this.getInnerEditor();
      return inner != null ? inner.saveValue(value, cx) : value;
   }

   public void changed(Property prop, Context cx) {
      if (prop == foreground || prop == background) {
         this.syncColors();
      }

      super.changed(prop, cx);
   }

   void syncColors() {
      BWbFieldEditor editor = this.getInnerEditor();
      if (editor != null) {
         editor.fw(306, this.getForeground(), this.getBackground(), null, null);
      }
   }
}
