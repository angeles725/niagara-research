package com.tridium.kitpx;

import com.tridium.hx.px.HxShellManager;
import java.util.Optional;
import java.util.logging.Level;
import javax.baja.control.BStringPoint;
import javax.baja.hx.px.BHxPxView;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BValueBinding;
import javax.baja.ui.BWidget;
import javax.baja.workbench.CannotSaveException;

@NiagaraType(
   agent = {@AgentOn(
      types = {"bajaui:Widget"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "widgetEvent",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "widgetProperty",
      type = "String",
      defaultValue = ""
   )})
public class BSetPointBinding extends BValueBinding {
   public static final Property widgetEvent = newProperty(0, "", null);
   public static final Property widgetProperty = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BSetPointBinding.class);

   public String getWidgetEvent() {
      return this.getString(widgetEvent);
   }

   public void setWidgetEvent(String v) {
      this.setString(widgetEvent, v, null);
   }

   public String getWidgetProperty() {
      return this.getString(widgetProperty);
   }

   public void setWidgetProperty(String v) {
      this.setString(widgetProperty, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean invokedOnWidget(Action action, BValue value, Context context) {
      if (action.getName().equals(this.getWidgetEvent())) {
         try {
            this.saveSetPoint(context);
            return true;
         } catch (CannotSaveException var5) {
            BDialog.error(this.getWidget(), BDialog.TITLE_ERROR, var5.getLocalizedMessage(), var5);
         }
      }

      return false;
   }

   public boolean firedOnWidget(Topic topic, BValue event, Context context) {
      if (topic.getName().equals(this.getWidgetEvent())) {
         try {
            this.saveSetPoint(context);
            return true;
         } catch (CannotSaveException var5) {
            BDialog.error(this.getWidget(), BDialog.TITLE_ERROR, var5.getLocalizedMessage(), var5);
         }
      }

      return false;
   }

   public boolean isDegraded() {
      return !this.isBound() || !this.getTarget().canWrite();
   }

   public void targetChanged() {
      super.targetChanged();
      this.loadSetPoint();
   }

   public void save(Context cx) throws Exception {
      if (this.isBound()) {
         BWidget widget = this.getWidget();
         if (widget instanceof BSetPointFieldEditor) {
            ((BSetPointFieldEditor)widget).saveSetPoint(this, cx);
         }
      }
   }

   public void loadSetPoint() {
      BWidget widget = this.getWidget();
      if (widget instanceof BSetPointFieldEditor) {
         ((BSetPointFieldEditor)widget).loadSetPoint(this);
      } else {
         Property prop = widget.getProperty(this.getWidgetProperty());
         if (prop != null) {
            BValue w = widget.get(prop);
            if (this.isBound()) {
               BValue t = (BValue)this.get();
               BValue s = this.convert(t, w);
               if (s != null) {
                  widget.set(prop, s);
               }
            }
         }
      }
   }

   @Deprecated
   public void saveSetPoint() throws CannotSaveException {
      BValue w = this.saveWidgetProperty();
      if (w != null) {
         this.saveSetPoint(w);
      }
   }

   public void saveSetPoint(Context ctx) throws CannotSaveException {
      BValue w = this.saveWidgetProperty();
      if (w != null) {
         this.saveSetPoint(w, ctx);
      }
   }

   @Deprecated
   public void saveSetPoint(BValue w) throws CannotSaveException {
      if (this.isBound()) {
         BValue t = (BValue)this.get();
         if (!t.isComponent()) {
            BValue s = this.convert(w, t);
            if (s == null) {
               return;
            }

            try {
               if (this.saveProperty(s, null)) {
                  return;
               }
            } catch (CannotSaveException var5) {
               return;
            }
         }

         if (!this.saveAction(w, null)) {
            System.out.println("WARNING: No mechanism found to save " + this.getOrd());
         }
      }
   }

   public void saveSetPoint(BValue w, Context ctx) throws CannotSaveException {
      if (this.isBound()) {
         BValue t = (BValue)this.get();
         if (!t.isComponent()) {
            BValue s = this.convert(w, t);
            if (s == null) {
               return;
            }

            if (this.saveProperty(s, ctx)) {
               return;
            }
         }

         if (!this.saveAction(w, ctx)) {
            if (this.getWidget() != null && this.getWidget().getShellManager() instanceof HxShellManager) {
               BHxPxView.log.fine("No mechanism found to save BSetPointBinding" + this.getOrd());
            } else {
               throw new CannotSaveException("No mechanism found to save " + this.getOrd());
            }
         }
      }
   }

   BValue saveWidgetProperty() {
      BWidget widget = this.getWidget();
      Property prop = widget.getProperty(this.getWidgetProperty());
      return prop == null ? null : widget.get(prop);
   }

   private boolean saveProperty(BValue v, Context ctx) throws CannotSaveException {
      BComponent c = this.getTarget().getComponent();
      Property[] path = this.getTarget().getPropertyPathInComponent();
      if (path == null || path.length == 0) {
         return false;
      } else if (Flags.isReadonly(c, path[0])) {
         return false;
      } else {
         BComplex parent = c;

         for (int i = 0; i < path.length - 1; i++) {
            parent = (BComplex)parent.get(path[i]);
         }

         this.verifyBounds(v, c.getSlotFacets(path[0]));
         parent.set(path[path.length - 1], v, ctx);
         return true;
      }
   }

   private boolean saveAction(BValue v, Context ctx) throws CannotSaveException {
      BComponent c = this.getTarget().getComponent();

      try {
         BValue existingValue = this.convert(c, v);
         if (existingValue != null && existingValue.equivalent(v)) {
            return true;
         }
      } catch (Exception var8) {
         LOGGER.log(Level.WARNING, "error in SetPointBinding checking for existing value", (Throwable)var8);
      }

      Object[] facetsArray = c.getChildren(BFacets.class);
      if (facetsArray.length > 0) {
         BFacets facets = (BFacets)facetsArray[0];
         this.verifyBounds(v, facets);
      }

      Action action = c.getAction("set");
      if (action == null) {
         return false;
      } else if (action.getParameterType() != v.getType()) {
         return false;
      } else {
         try {
            c.invoke(action, v, ctx);
            return true;
         } catch (Exception var7) {
            return false;
         }
      }
   }

   private void verifyBounds(BValue v, BFacets facets) throws CannotSaveException {
      if (facets != null) {
         BNumber min = (BNumber)facets.get("min");
         BNumber max = (BNumber)facets.get("max");
         if (v instanceof BStatusNumeric) {
            v = ((BStatusNumeric)v).getValueValue();
         }

         if (v instanceof BDouble) {
            double d = ((BDouble)v).getDouble();
            if (min != null && d < min.getDouble()) {
               throw new CannotSaveException(d + " < " + min + " [" + min + "," + max + "]");
            }

            if (max != null && d > max.getDouble()) {
               throw new CannotSaveException(d + " > " + max + " [" + min + "," + max + "]");
            }
         } else if (v instanceof BInteger) {
            int i = ((BInteger)v).getInt();
            if (min != null && i < min.getInt()) {
               throw new CannotSaveException(i + " < " + min + " [" + min + "," + max + "]");
            }

            if (max != null && i > max.getInt()) {
               throw new CannotSaveException(i + " > " + max + " [" + min + "," + max + "]");
            }
         }
      }
   }

   public BValue convert(BValue from, BValue to) {
      if (from.getType() == to.getType()) {
         return from;
      } else {
         if (from instanceof BINumeric && to instanceof BNumber) {
            double x = ((BINumeric)from).getNumeric();
            switch (to.getType().getDataTypeSymbol()) {
               case 'd':
                  return BDouble.make((float)x);
               case 'e':
               case 'g':
               case 'h':
               case 'j':
               case 'k':
               default:
                  break;
               case 'f':
                  return BFloat.make((float)x);
               case 'i':
                  return BInteger.make((int)x);
               case 'l':
                  return BLong.make((long)x);
            }
         }

         if (from instanceof BINumeric && to instanceof BStatusNumeric) {
            double x = ((BINumeric)from).getNumeric();
            return new BStatusNumeric(x);
         } else if (from instanceof BIBoolean && to instanceof BBoolean) {
            return BBoolean.make(((BIBoolean)from).getBoolean());
         } else if (from instanceof BIBoolean && to instanceof BStatusBoolean) {
            return new BStatusBoolean(((BIBoolean)from).getBoolean());
         } else if (from instanceof BIEnum && to instanceof BDynamicEnum) {
            return ((BIEnum)from).getEnum();
         } else if (from instanceof BIEnum && to instanceof BStatusEnum) {
            return new BStatusEnum(((BIEnum)from).getEnum());
         } else {
            BValue stringValue = getFromStringlike(from).map(bString -> toStringLike(bString, to)).orElse(null);
            if (stringValue != null) {
               return stringValue;
            } else {
               System.out.println("WARNING: SetPointBinding could not convert " + from.getType() + " -> " + to.getType());
               return null;
            }
         }
      }
   }

   private static BValue toStringLike(BString stringValue, BValue target) {
      if (target instanceof BString) {
         return stringValue;
      } else {
         return target instanceof BStatusString ? new BStatusString(stringValue.getString()) : null;
      }
   }

   private static Optional<BString> getFromStringlike(BValue from) {
      if (from instanceof BString) {
         return Optional.of((BString)from);
      } else if (from instanceof BStatusString) {
         return Optional.of((BString)((BStatusString)from).getValueValue());
      } else {
         return from instanceof BStringPoint ? Optional.of((BString)((BStringPoint)from).getOut().getValueValue()) : Optional.empty();
      }
   }
}
