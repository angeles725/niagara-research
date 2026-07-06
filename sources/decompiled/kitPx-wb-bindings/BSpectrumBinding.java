package com.tridium.kitpx;

import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;

@NiagaraType(
   agent = {@AgentOn(
      types = {"bajaui:Widget"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "widgetProperty",
      type = "String",
      defaultValue = "fill"
   ), @NiagaraProperty(
      name = "lowColor",
      type = "BColor",
      defaultValue = "BColor.blue"
   ), @NiagaraProperty(
      name = "midColor",
      type = "BColor",
      defaultValue = "BColor.white"
   ), @NiagaraProperty(
      name = "highColor",
      type = "BColor",
      defaultValue = "BColor.red"
   ), @NiagaraProperty(
      name = "setpoint",
      type = "double",
      defaultValue = "50"
   ), @NiagaraProperty(
      name = "extent",
      type = "double",
      defaultValue = "100"
   )})
public class BSpectrumBinding extends BBinding {
   public static final Property widgetProperty = newProperty(0, "fill", null);
   public static final Property lowColor = newProperty(0, BColor.blue, null);
   public static final Property midColor = newProperty(0, BColor.white, null);
   public static final Property highColor = newProperty(0, BColor.red, null);
   public static final Property setpoint = newProperty(0, 50, null);
   public static final Property extent = newProperty(0, 100, null);
   public static final Type TYPE = Sys.loadType(BSpectrumBinding.class);

   public String getWidgetProperty() {
      return this.getString(widgetProperty);
   }

   public void setWidgetProperty(String v) {
      this.setString(widgetProperty, v, null);
   }

   public BColor getLowColor() {
      return (BColor)this.get(lowColor);
   }

   public void setLowColor(BColor v) {
      this.set(lowColor, v, null);
   }

   public BColor getMidColor() {
      return (BColor)this.get(midColor);
   }

   public void setMidColor(BColor v) {
      this.set(midColor, v, null);
   }

   public BColor getHighColor() {
      return (BColor)this.get(highColor);
   }

   public void setHighColor(BColor v) {
      this.set(highColor, v, null);
   }

   public double getSetpoint() {
      return this.getDouble(setpoint);
   }

   public void setSetpoint(double v) {
      this.setDouble(setpoint, v, null);
   }

   public double getExtent() {
      return this.getDouble(extent);
   }

   public void setExtent(double v) {
      this.setDouble(extent, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BValue getOnWidget(Property prop) {
      if (prop.getName().equals(this.getWidgetProperty()) && this.isBound()) {
         BObject target = this.get();
         if (target instanceof BINumeric) {
            double value = ((BINumeric)target).getNumeric();
            BColor color = this.solveColor(value);
            if (prop.getType().is(BBrush.TYPE)) {
               return color.toBrush();
            }

            if (prop.getType().is(BColor.TYPE)) {
               return color;
            }
         }
      }

      return super.getOnWidget(prop);
   }

   BColor solveColor(double value) {
      double mid = this.getSetpoint();
      double delta = this.getExtent() / 2.0;
      BColor lowColor = this.getLowColor();
      BColor midColor = this.getMidColor();
      BColor highColor = this.getHighColor();
      int red;
      int blue;
      int green;
      int alpha;
      if (value < mid) {
         if (value < mid - delta) {
            return lowColor;
         }

         double mRed = (midColor.getRed() - lowColor.getRed()) / delta;
         double bRed = midColor.getRed() - mRed * mid;
         red = (int)(mRed * value + bRed);
         double mGreen = (midColor.getGreen() - lowColor.getGreen()) / delta;
         double bGreen = midColor.getGreen() - mGreen * mid;
         green = (int)(mGreen * value + bGreen);
         double mBlue = (midColor.getBlue() - lowColor.getBlue()) / delta;
         double bBlue = midColor.getBlue() - mBlue * mid;
         blue = (int)(mBlue * value + bBlue);
         double mAlpha = (midColor.getAlpha() - lowColor.getAlpha()) / delta;
         double bAlpha = midColor.getAlpha() - mAlpha * mid;
         alpha = (int)(mAlpha * value + bAlpha);
      } else {
         if (value > mid + delta) {
            return highColor;
         }

         double mRed = (highColor.getRed() - midColor.getRed()) / delta;
         double bRed = midColor.getRed() - mRed * mid;
         red = (int)(mRed * value + bRed);
         double mGreen = (highColor.getGreen() - midColor.getGreen()) / delta;
         double bGreen = midColor.getGreen() - mGreen * mid;
         green = (int)(mGreen * value + bGreen);
         double mBlue = (highColor.getBlue() - midColor.getBlue()) / delta;
         double bBlue = midColor.getBlue() - mBlue * mid;
         blue = (int)(mBlue * value + bBlue);
         double mAlpha = (highColor.getAlpha() - midColor.getAlpha()) / delta;
         double bAlpha = midColor.getAlpha() - mAlpha * mid;
         alpha = (int)(mAlpha * value + bAlpha);
      }

      return BColor.make(red, green, blue, alpha);
   }
}
