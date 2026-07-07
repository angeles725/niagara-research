package com.honeywell.easybinding.converter;

import com.tridium.nv.comps.BNiagaraVirtualBooleanPoint;
import com.tridium.nv.comps.BNiagaraVirtualEnumPoint;
import com.tridium.nv.comps.BNiagaraVirtualNumericPoint;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BEnumWritable;
import javax.baja.control.BNumericPoint;
import javax.baja.converters.BEnumToSimpleMap;
import javax.baja.converters.BNumericToSimpleMap;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BConverter;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "numMap",
      type = "baja:Simple",
      defaultValue = "BNumericToSimpleMap.NULL"
   ), @NiagaraProperty(
      name = "enumMap",
      type = "baja:Simple",
      defaultValue = "BEnumToSimpleMap.NULL"
   ), @NiagaraProperty(
      name = "trueValue",
      type = "baja:Simple",
      defaultValue = "BBoolean.TRUE"
   ), @NiagaraProperty(
      name = "falseValue",
      type = "baja:Simple",
      defaultValue = "BBoolean.FALSE"
   )})
public class BIEbConverter extends BConverter {
   public static final Property numMap = newProperty(0, BNumericToSimpleMap.NULL, null);
   public static final Property enumMap = newProperty(0, BEnumToSimpleMap.NULL, null);
   public static final Property trueValue = newProperty(0, BBoolean.TRUE, null);
   public static final Property falseValue = newProperty(0, BBoolean.FALSE, null);
   public static final Type TYPE = Sys.loadType(BIEbConverter.class);

   public BSimple getNumMap() {
      return (BSimple)this.get(numMap);
   }

   public void setNumMap(BSimple var1) {
      this.set(numMap, var1, null);
   }

   public BSimple getEnumMap() {
      return (BSimple)this.get(enumMap);
   }

   public void setEnumMap(BSimple var1) {
      this.set(enumMap, var1, null);
   }

   public BSimple getTrueValue() {
      return (BSimple)this.get(trueValue);
   }

   public void setTrueValue(BSimple var1) {
      this.set(trueValue, var1, null);
   }

   public BSimple getFalseValue() {
      return (BSimple)this.get(falseValue);
   }

   public void setFalseValue(BSimple var1) {
      this.set(falseValue, var1, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BObject convert(BObject var1, BObject var2, Context var3) {
      if (var1 instanceof BNumericPoint || var1 instanceof BNiagaraVirtualNumericPoint) {
         double var8 = ((BINumeric)var1).getNumeric();
         BSimple var6 = ((BNumericToSimpleMap)this.getNumMap()).get(var8);
         return (BObject)(var6 != null ? var6 : var2);
      } else if (var1 instanceof BEnumPoint || var1 instanceof BNiagaraVirtualEnumPoint) {
         BEnum var7 = ((BIEnum)var1).getEnum();
         BSimple var9 = ((BEnumToSimpleMap)this.getEnumMap()).get(var7.getOrdinal());
         return (BObject)(var9 != null ? var9 : var2);
      } else if (!(var1 instanceof BBooleanPoint) && !(var1 instanceof BNiagaraVirtualBooleanPoint)) {
         return null;
      } else {
         boolean var4 = ((BIBoolean)var1).getBoolean();
         BSimple var5;
         if (var4) {
            var5 = this.getTrueValue();
         } else {
            var5 = this.getFalseValue();
         }

         return (BObject)(var5.getType() == var2.getType() ? var5 : var2);
      }
   }

   public void init(BObject var1, BObject var2) {
      if (var1 instanceof BNumericPoint || var1 instanceof BNiagaraVirtualNumericPoint) {
         this.setNumMap(BNumericToSimpleMap.make((BSimple)var2));
      } else if (var1 instanceof BBooleanPoint || var1 instanceof BNiagaraVirtualBooleanPoint) {
         this.setTrueValue((BSimple)var2);
         this.setFalseValue((BSimple)var2);
      } else if (var1 instanceof BEnumWritable || var1 instanceof BNiagaraVirtualEnumPoint) {
         this.setEnumMap(BEnumToSimpleMap.make((BSimple)var2));
      }
   }
}
