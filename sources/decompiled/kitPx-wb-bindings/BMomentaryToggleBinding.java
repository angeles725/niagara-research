package com.tridium.kitpx;

import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.util.BTypeSpec;

@NiagaraType(
   agent = {@AgentOn(
      types = {"bajaui:AbstractButton"}
   )}
)
public class BMomentaryToggleBinding extends BBinding {
   public static final Type TYPE = Sys.loadType(BMomentaryToggleBinding.class);
   private boolean targetPressed = false;
   private long lastMillis = 0L;

   public Type getType() {
      return TYPE;
   }

   public boolean firedOnWidget(Topic topic, BValue value, Context context) {
      if (topic.equals(BWidget.mouseEvent) && (((BMouseEvent)value).getId() == 501 || ((BMouseEvent)value).getId() == 502)) {
         this.setValueOnTarget(((BAbstractButton)this.getWidget()).isPressed());
      }

      return false;
   }

   public boolean isDegraded() {
      if (super.isDegraded()) {
         return true;
      } else {
         OrdTarget target = this.getTarget();
         if (target.getSlotInComponent() == null) {
            return !target.getComponent().getType().getTypeSpec().equals(BTypeSpec.make("control:BooleanWritable"));
         } else if (target.getSlotInComponent().isProperty()) {
            Type propertyType = target.getSlotInComponent().asProperty().getType();
            return !propertyType.is(BBoolean.TYPE) && !propertyType.is(BStatusBoolean.TYPE);
         } else if (!target.getSlotInComponent().isAction()) {
            return true;
         } else {
            Type parameterType = target.getSlotInComponent().asAction().getParameterType();
            return parameterType == null || !parameterType.is(BBoolean.TYPE) && !parameterType.is(BStatusBoolean.TYPE);
         }
      }
   }

   public void targetChanged() {
      if (!Sys.isStation()) {
         this.setValueOnTarget(((BAbstractButton)this.getWidget()).isPressed());
      }

      super.targetChanged();
   }

   public synchronized void setValueOnTarget(boolean value) {
      if (value || this.targetPressed) {
         this.targetPressed = value;
         BOrd ord = this.getOrd();
         if (!ord.isNull()) {
            if (this.isBound()) {
               long currentMillis = Clock.ticks();
               long diff = currentMillis - this.lastMillis;
               if (this.lastMillis > 0L && diff < 100L && diff > -1L) {
                  try {
                     Thread.sleep(100L - diff);
                  } catch (InterruptedException var10) {
                  }
               }

               this.lastMillis = Clock.ticks();
               OrdTarget target = this.getTarget();
               if (target.getSlotInComponent() != null) {
                  if (target.getSlotInComponent().isProperty()) {
                     Type propertyType = target.getSlotInComponent().asProperty().getType();
                     if (propertyType.is(BBoolean.TYPE)) {
                        target.getComponent().set(target.getSlotInComponent().asProperty(), BBoolean.make(value));
                     } else if (propertyType.is(BStatusBoolean.TYPE)) {
                        BStatusBoolean statusBoolean = (BStatusBoolean)target.getComponent().get(target.getSlotInComponent().asProperty());
                        statusBoolean.setValue(value);
                        statusBoolean.setStatus(BStatus.makeNull(statusBoolean.getStatus(), false));
                     }
                  } else if (target.getSlotInComponent().isAction()) {
                     Type actionType = target.getSlotInComponent().asAction().getParameterType();
                     if (actionType != null && actionType.is(BBoolean.TYPE)) {
                        target.getComponent().invoke(target.getSlotInComponent().asAction(), BBoolean.make(value));
                     }
                  }
               } else if (target.getComponent().getType().getTypeSpec().equals(BTypeSpec.make("control:BooleanWritable"))) {
                  target.getComponent().invoke(target.getComponent().getAction("set"), BBoolean.make(value));
               }
            }
         }
      }
   }
}
