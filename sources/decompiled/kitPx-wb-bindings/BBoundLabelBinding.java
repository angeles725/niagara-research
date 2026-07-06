package com.tridium.kitpx;

import com.tridium.kitpx.enums.BStatusEffect;
import javax.baja.converters.BFixedSimple;
import javax.baja.converters.BINumericToSimple;
import javax.baja.converters.BNumericToSimpleMap;
import javax.baja.converters.BPassThrough;
import javax.baja.gx.BColor;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BValueBinding;

@NiagaraType(
   agent = {@AgentOn(
      types = {"kitPx:BoundLabel"}
   )}
)
@NiagaraProperty(
   name = "statusEffect",
   type = "BStatusEffect",
   defaultValue = "BStatusEffect.colorAndBlink"
)
public class BBoundLabelBinding extends BValueBinding {
   public static final Property statusEffect = newProperty(0, BStatusEffect.colorAndBlink, null);
   public static final Type TYPE = Sys.loadType(BBoundLabelBinding.class);

   public BStatusEffect getStatusEffect() {
      return (BStatusEffect)this.get(statusEffect);
   }

   public void setStatusEffect(BStatusEffect v) {
      this.set(statusEffect, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BValue getOnWidget(Property prop) {
      BStatusEffect effect = this.getStatusEffect();
      if (this.isBound() && this.get() instanceof BIStatus && effect != BStatusEffect.none) {
         BStatus status = ((BIStatus)this.get()).getStatus();
         String name = prop.getName();
         if (name.equals("blink")) {
            if (status.isUnackedAlarm() && effect == BStatusEffect.colorAndBlink) {
               return BBoolean.TRUE;
            }
         } else if (name.equals("foreground")) {
            BColor fg = (BColor)status.getForegroundColor(null);
            if (fg != null) {
               return fg.toBrush();
            }
         } else if (name.equals("background")) {
            BColor bg = (BColor)status.getBackgroundColor(null);
            if (bg != null) {
               return bg.toBrush();
            }
         } else if (name.equals("border") && this.get() instanceof BStatusBoolean) {
            BValue override = this.get(prop.getName());
            if (override instanceof BFixedSimple) {
               BFixedSimple fs = (BFixedSimple)override;
               return fs.getValue();
            }

            if (override instanceof BPassThrough) {
               BPassThrough pt = (BPassThrough)override;
               BObject from = this.get();
               BObject to = prop.getDefaultValue().newCopy();
               return (BValue)pt.convert(from, to);
            }

            if (override instanceof BINumericToSimple) {
               BStatusBoolean boolStat = (BStatusBoolean)this.get();
               BINumericToSimple converter = (BINumericToSimple)override;
               BNumericToSimpleMap map = converter.getMap();
               double[] maximums = map.getMaximums();
               double[] minimums = map.getMinimums();
               double numeric = 0.0;
               if (boolStat.getValue()) {
                  numeric = this.getHighest(maximums);
               } else {
                  numeric = this.getLowest(minimums);
               }

               return map.get(numeric);
            }
         }
      }

      return super.getOnWidget(prop);
   }

   private double getLowest(double[] minimums) {
      if (minimums.length < 1) {
         return 0.0;
      } else {
         double lowest = minimums[0];

         for (int i = 1; i < minimums.length; i++) {
            if (minimums[i] < lowest) {
               lowest = minimums[i];
            }
         }

         return lowest;
      }
   }

   private double getHighest(double[] maximums) {
      if (maximums.length < 1) {
         return 0.0;
      } else {
         double biggest = maximums[0];

         for (int i = 1; i < maximums.length; i++) {
            if (maximums[i] > biggest) {
               biggest = maximums[i];
            }
         }

         return biggest;
      }
   }
}
