package com.tridium.bacnet.stack.link.ip;

import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "bacnetIPAddress",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "timeToLive",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "purgeTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   )})
public class BFdtEntry extends BStruct {
   public static final Property bacnetIPAddress = newProperty(0, "", null);
   public static final Property timeToLive = newProperty(0, -1, null);
   public static final Property purgeTime = newProperty(3, BAbsTime.NULL, null);
   public static final Type TYPE = Sys.loadType(BFdtEntry.class);
   private static final Context TIME_CX = BFacets.make("showSeconds", BBoolean.TRUE);

   public String getBacnetIPAddress() {
      return this.getString(bacnetIPAddress);
   }

   public void setBacnetIPAddress(String v) {
      this.setString(bacnetIPAddress, v, null);
   }

   public int getTimeToLive() {
      return this.getInt(timeToLive);
   }

   public void setTimeToLive(int v) {
      this.setInt(timeToLive, v, null);
   }

   public BAbsTime getPurgeTime() {
      return (BAbsTime)this.get(purgeTime);
   }

   public void setPurgeTime(BAbsTime v) {
      this.set(purgeTime, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BFdtEntry() {
   }

   public BFdtEntry(String bacnetIPAddress, int timeToLive, BAbsTime purgeTime) {
      this.setBacnetIPAddress(bacnetIPAddress);
      this.setTimeToLive(timeToLive);
      this.setPurgeTime(purgeTime);
   }

   public byte[] getBIpAddr() {
      return BBacnetAddress.stringToBytes(2, 5, this.getBacnetIPAddress());
   }

   public void setBIpAddr(byte[] bipAddr) {
      this.setBacnetIPAddress(BBacnetAddress.bytesToString(2, bipAddr));
   }

   public boolean isAddressValid() {
      try {
         this.getBIpAddr();
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("B/IP addr:" + this.getBacnetIPAddress());
      sb.append("  TTL:" + this.getTimeToLive());
      sb.append("  PrgTm:" + this.getPurgeTime().toString(TIME_CX));
      return sb.toString();
   }
}
