package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "serviceType",
      type = "BLonServiceType",
      defaultValue = "BLonServiceType.unacked",
      flags = 1
   ), @NiagaraProperty(
      name = "repeatTimer",
      type = "BLonRepeatTimer",
      defaultValue = "BLonRepeatTimer.milliSec16"
   ), @NiagaraProperty(
      name = "retries",
      type = "int",
      defaultValue = "3"
   ), @NiagaraProperty(
      name = "receiveTimer",
      type = "BLonReceiveTimer",
      defaultValue = "BLonReceiveTimer.milliSec128"
   ), @NiagaraProperty(
      name = "transmitTimer",
      type = "BLonRepeatTimer",
      defaultValue = "BLonRepeatTimer.milliSec128"
   )})
public class BLinkDescriptor extends BStruct {
   public static final Property serviceType = newProperty(1, BLonServiceType.unacked, null);
   public static final Property repeatTimer = newProperty(0, BLonRepeatTimer.milliSec16, null);
   public static final Property retries = newProperty(0, 3, null);
   public static final Property receiveTimer = newProperty(0, BLonReceiveTimer.milliSec128, null);
   public static final Property transmitTimer = newProperty(0, BLonRepeatTimer.milliSec128, null);
   public static final Type TYPE = Sys.loadType(BLinkDescriptor.class);

   public BLinkDescriptor() {
   }

   public BLonServiceType getServiceType() {
      return (BLonServiceType)this.get(serviceType);
   }

   public void setServiceType(BLonServiceType v) {
      this.set(serviceType, v, null);
   }

   public BLonRepeatTimer getRepeatTimer() {
      return (BLonRepeatTimer)this.get(repeatTimer);
   }

   public void setRepeatTimer(BLonRepeatTimer v) {
      this.set(repeatTimer, v, null);
   }

   public int getRetries() {
      return this.getInt(retries);
   }

   public void setRetries(int v) {
      this.setInt(retries, v, null);
   }

   public BLonReceiveTimer getReceiveTimer() {
      return (BLonReceiveTimer)this.get(receiveTimer);
   }

   public void setReceiveTimer(BLonReceiveTimer v) {
      this.set(receiveTimer, v, null);
   }

   public BLonRepeatTimer getTransmitTimer() {
      return (BLonRepeatTimer)this.get(transmitTimer);
   }

   public void setTransmitTimer(BLonRepeatTimer v) {
      this.set(transmitTimer, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLinkDescriptor(BLonServiceType svc, BLonRepeatTimer rpt, int retries, BLonReceiveTimer rcv, BLonRepeatTimer tx) {
      this.setServiceType(svc);
      this.setRepeatTimer(rpt);
      this.setRetries(retries);
      this.setReceiveTimer(rcv);
      this.setTransmitTimer(tx);
   }

   public boolean entryMatches(BIAddressEntry entry) {
      return entry.getRepeatTimer() == this.getRepeatTimer()
         && entry.getRetries() == this.getRetries()
         && entry.getReceiveTimer() == this.getReceiveTimer()
         && entry.getTransmitTimer() == this.getTransmitTimer();
   }
}
