package com.tridium.modbusCore.point;

import com.tridium.basicdriver.point.BBasicProxyExt;
import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.BModbusNetwork;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import javax.baja.driver.util.BIPollable;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal"
   ), @NiagaraProperty(
      name = "dataAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   )})
@NiagaraActions({@NiagaraAction(
      name = "forceRead",
      flags = 16
   ), @NiagaraAction(
      name = "forceWrite",
      flags = 16
   )})
public abstract class BModbusProxyExt extends BBasicProxyExt implements ModbusMessageConst, BIPollable {
   public static final Property pollFrequency = newProperty(0, BPollFrequency.normal, null);
   public static final Property dataAddress = newProperty(0, new BFlexAddress(), null);
   public static final Action forceRead = newAction(16, null);
   public static final Action forceWrite = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BModbusProxyExt.class);
   protected static Context noAddressCheck = new BasicContext() {
      public boolean equals(Object o) {
         return this == o;
      }

      public int hashCode() {
         return System.identityHashCode(this);
      }

      public String toString() {
         return "Modbus:noAddressCheck";
      }
   };

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public BFlexAddress getDataAddress() {
      return (BFlexAddress)this.get(dataAddress);
   }

   public void setDataAddress(BFlexAddress v) {
      this.set(dataAddress, v, null);
   }

   public void forceRead() {
      this.invoke(forceRead, null, null);
   }

   public void forceWrite() {
      this.invoke(forceWrite, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   protected synchronized BModbusNetwork modbusNet() {
      return this.getDevice() == null ? null : ((BModbusDevice)this.getDevice()).modbusNet();
   }

   protected int getDevAddr() {
      return ((BModbusDevice)this.getDevice()).getDeviceAddress();
   }

   public void poll() {
      if (!this.isUnoperational()) {
         this.read();
      }
   }

   public void read() {
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      return !action.equals(forceRead) && !action.equals(forceWrite)
         ? super.post(action, arg, cx)
         : this.modbusNet().postAsync(new Invocation(this, action, arg, cx));
   }

   public void doForceRead() {
      this.poll();
   }

   public void doForceWrite() {
      try {
         this.write(null);
      } catch (Exception var2) {
         this.modbusNet().getModbusLog().error("Exception during forceWrite for " + this.getParentPoint().getName(), var2);
      }
   }

   public void writablePointActionInvoked() {
      super.writablePointActionInvoked();
      this.getParentPoint().execute();
   }
}
