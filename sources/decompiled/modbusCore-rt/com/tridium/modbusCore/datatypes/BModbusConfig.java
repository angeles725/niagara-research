package com.tridium.modbusCore.datatypes;

import com.tridium.modbusCore.enums.BDataByteOrder64BitEnum;
import com.tridium.modbusCore.enums.BDataByteOrderEnum;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "overrideNetwork",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "floatByteOrder",
      type = "BDataByteOrderEnum",
      defaultValue = "BDataByteOrderEnum.order3210"
   ), @NiagaraProperty(
      name = "longByteOrder",
      type = "BDataByteOrderEnum",
      defaultValue = "BDataByteOrderEnum.order3210"
   ), @NiagaraProperty(
      name = "double64BitByteOrder",
      type = "BDataByteOrder64BitEnum",
      defaultValue = "BDataByteOrder64BitEnum.order76543210"
   ), @NiagaraProperty(
      name = "long64BitByteOrder",
      type = "BDataByteOrder64BitEnum",
      defaultValue = "BDataByteOrder64BitEnum.order76543210"
   )})
public class BModbusConfig extends BStruct {
   public static final Property overrideNetwork = newProperty(0, false, null);
   public static final Property floatByteOrder = newProperty(0, BDataByteOrderEnum.order3210, null);
   public static final Property longByteOrder = newProperty(0, BDataByteOrderEnum.order3210, null);
   public static final Property double64BitByteOrder = newProperty(0, BDataByteOrder64BitEnum.order76543210, null);
   public static final Property long64BitByteOrder = newProperty(0, BDataByteOrder64BitEnum.order76543210, null);
   public static final Type TYPE = Sys.loadType(BModbusConfig.class);

   public boolean getOverrideNetwork() {
      return this.getBoolean(overrideNetwork);
   }

   public void setOverrideNetwork(boolean v) {
      this.setBoolean(overrideNetwork, v, null);
   }

   public BDataByteOrderEnum getFloatByteOrder() {
      return (BDataByteOrderEnum)this.get(floatByteOrder);
   }

   public void setFloatByteOrder(BDataByteOrderEnum v) {
      this.set(floatByteOrder, v, null);
   }

   public BDataByteOrderEnum getLongByteOrder() {
      return (BDataByteOrderEnum)this.get(longByteOrder);
   }

   public void setLongByteOrder(BDataByteOrderEnum v) {
      this.set(longByteOrder, v, null);
   }

   public BDataByteOrder64BitEnum getDouble64BitByteOrder() {
      return (BDataByteOrder64BitEnum)this.get(double64BitByteOrder);
   }

   public void setDouble64BitByteOrder(BDataByteOrder64BitEnum v) {
      this.set(double64BitByteOrder, v, null);
   }

   public BDataByteOrder64BitEnum getLong64BitByteOrder() {
      return (BDataByteOrder64BitEnum)this.get(long64BitByteOrder);
   }

   public void setLong64BitByteOrder(BDataByteOrder64BitEnum v) {
      this.set(long64BitByteOrder, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BModbusConfig() {
   }

   public BModbusConfig(BModbusConfig src) {
      this.copyFrom(src);
   }

   public String toString(Context context) {
      return this.getOverrideNetwork() + ":" + this.getFloatByteOrder().getTag();
   }

   public String toDebugString() {
      return this.getOverrideNetwork() + ":" + this.getFloatByteOrder().getTag();
   }
}
