package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.netmessages.CapacityInfoRequest;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.io.LonInputStream;

public class ExtendedSelfDoc {
   int length;
   boolean binding2;
   boolean queryStats;
   int aliasCount;
   int nodeSdTextLength;
   int staticNvCount;
   int verStruct;
   int verNmMin;
   int verNmMax;
   int verBinding;
   int[] flags = new int[6];
   int domainCapacity;
   int addressCapacity;
   int staticMtagCapacity;

   public static ExtendedSelfDoc make(BLonDevice dev) throws LonException {
      return new ExtendedSelfDoc(dev.lonComm(), NmUtil.getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public static ExtendedSelfDoc make(LonComm lonComm, LonAddress devAdr, boolean auth) throws LonException {
      return new ExtendedSelfDoc(lonComm, devAdr, auth);
   }

   private ExtendedSelfDoc(LonComm lonComm, LonAddress devAdr, boolean auth) throws LonException {
      CapacityInfoRequest capInf = new CapacityInfoRequest(0, 25);
      capInf.setAuthenticate(auth);
      LonMessage lm = lonComm.sendRequest(devAdr, capInf);
      LonInputStream in = new LonInputStream(lm.getMessageData());
      this.binding2 = in.readBit(0, 7, 1) > 0;
      this.queryStats = in.readBit(0, 6, 1) > 0;
      this.aliasCount = in.readBit(0, 0, 6);
      if (this.aliasCount == 63) {
         this.aliasCount = in.readUnsigned16();
      }

      this.nodeSdTextLength = in.readUnsigned16();
      this.staticNvCount = in.readUnsigned16();
      in.readUnsigned16();
      this.verStruct = in.readUnsigned8();
      this.verNmMin = in.readUnsigned8();
      this.verNmMax = in.readUnsigned8();
      this.verBinding = in.readUnsigned8();

      for (int i = 0; i < this.flags.length; i++) {
         this.flags[i] = in.readUnsigned8();
      }

      this.domainCapacity = in.readUnsigned16();
      this.addressCapacity = in.readUnsigned16();
      this.staticMtagCapacity = in.readUnsigned16();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("length             = ").append(this.length).append("\n");
      sb.append("binding2           = ").append(this.binding2).append("\n");
      sb.append("queryStats         = ").append(this.queryStats).append("\n");
      sb.append("aliasCount         = ").append(this.aliasCount).append("\n");
      sb.append("nodeSdTextLength   = ").append(this.nodeSdTextLength).append("\n");
      sb.append("staticNvCount      = ").append(this.staticNvCount).append("\n");
      sb.append("verStruct          = ").append(this.verStruct).append("\n");
      sb.append("verNmMin           = ").append(this.verNmMin).append("\n");
      sb.append("verNmMax           = ").append(this.verNmMax).append("\n");
      sb.append("verBinding         = ").append(this.verBinding).append("\n");
      sb.append("flags              = ");

      for (int i = 0; i < 6; i++) {
         sb.append(this.flags[i]).append(",");
      }

      sb.append("\n");
      sb.append("domainCapacity     = ").append(this.domainCapacity).append("\n");
      sb.append("addressCapacity    = ").append(this.addressCapacity).append("\n");
      sb.append("staticMtagCapacity = ").append(this.staticMtagCapacity).append("\n");
      return sb.toString();
   }

   public boolean getBinding2() {
      return this.binding2;
   }

   public boolean getQueryStats() {
      return this.queryStats;
   }

   public int getAliasCount() {
      return this.aliasCount;
   }

   public int getNodeSdTextLength() {
      return this.nodeSdTextLength;
   }

   public int getStaticNvCount() {
      return this.staticNvCount;
   }

   public int getVerStruct() {
      return this.verStruct;
   }

   public int getVerNmMin() {
      return this.verNmMin;
   }

   public int getVerNmMax() {
      return this.verNmMax;
   }

   public int getVerBinding() {
      return this.verBinding;
   }

   public int[] getFlags() {
      return this.flags;
   }

   public int getDomainCapacity() {
      return this.domainCapacity;
   }

   public int getAddressCapacity() {
      return this.addressCapacity;
   }

   public int getStaticMtagCapacity() {
      return this.staticMtagCapacity;
   }
}
