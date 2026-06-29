package com.tridium.bacnet.stack.link.ip;

import javax.baja.bacnet.datatypes.BBacnetAddress;
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
      name = "bacnetIPAddress",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "broadcastDistributionMask",
      type = "String",
      defaultValue = ""
   )})
public class BBdtEntry extends BStruct {
   public static final Property bacnetIPAddress = newProperty(0, "", null);
   public static final Property broadcastDistributionMask = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BBdtEntry.class);

   public String getBacnetIPAddress() {
      return this.getString(bacnetIPAddress);
   }

   public void setBacnetIPAddress(String v) {
      this.setString(bacnetIPAddress, v, null);
   }

   public String getBroadcastDistributionMask() {
      return this.getString(broadcastDistributionMask);
   }

   public void setBroadcastDistributionMask(String v) {
      this.setString(broadcastDistributionMask, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBdtEntry() {
   }

   public BBdtEntry(byte[] addr, byte[] bdMask) {
      this.setBIpAddr(addr);
      this.setBdMask(bdMask);
   }

   public BBdtEntry(String addr, String bdMask) {
      this.setBacnetIPAddress(addr);
      this.setBroadcastDistributionMask(bdMask);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("B/IP addr:" + this.getBacnetIPAddress());
      sb.append("  BDMask:" + this.getBroadcastDistributionMask());
      return sb.toString();
   }

   public byte[] getBIpAddr() {
      return BBacnetAddress.stringToBytes(2, 5, this.getBacnetIPAddress());
   }

   public byte[] getBdMask() {
      return BBacnetAddress.stringToBytes(2, 4, this.getBroadcastDistributionMask());
   }

   public void setBIpAddr(byte[] bipAddr) {
      this.setBacnetIPAddress(BBacnetAddress.bytesToString(2, bipAddr));
   }

   public void setBdMask(byte[] bdMask) {
      this.setBroadcastDistributionMask(BBacnetAddress.bytesToString(2, bdMask));
   }

   public boolean ipEquals(byte[] ip) {
      return this.getBacnetIPAddress().equals(BBacnetAddress.bytesToString(2, ip));
   }

   public boolean isAddressValid() {
      try {
         this.getBIpAddr();
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   public boolean isMaskValid() {
      try {
         this.getBdMask();
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   public boolean isDirectedBroadcast() {
      byte[] bdm = this.getBdMask();
      if (bdm == null) {
         return false;
      } else {
         for (int i = 0; i < 4; i++) {
            if ((bdm[i] & 255) != 255) {
               return true;
            }
         }

         return false;
      }
   }
}
