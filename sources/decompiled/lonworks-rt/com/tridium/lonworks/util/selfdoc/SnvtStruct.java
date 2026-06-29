package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.io.LonInputStream;

public class SnvtStruct implements NetMessages {
   private LonComm lonComm;
   private LonAddress sendAddr;
   private boolean auth;
   public boolean hasSelfDoc = false;
   public byte[] data = null;
   public int hdrLength;
   public int snvtPtr;
   public int numNvsDocs = 0;
   public int version;
   public int docLength;
   public int mtagCount = 0;
   public int maxMumNvs;
   public int staticNvCount = 0;
   public int currentNvCount = 0;
   public int maxNvInUse = 65535;
   public int aliasCount = 0;
   public int nodeSelfDocLen = 0;
   public boolean bindingII = false;
   public boolean queryStats = false;

   public SnvtStruct(BLonDevice dev) throws LonException {
      this(dev.lonComm(), NmUtil.getSendAddress(dev), dev.getDeviceData().getAuthenticate());
   }

   public SnvtStruct(LonComm lonComm, LonAddress destAddr, boolean authenticate) throws LonException {
      this.lonComm = lonComm;
      this.sendAddr = destAddr;
      this.auth = authenticate;
      this.snvtPtr = Neuron.getSnvtPtr(lonComm, this.sendAddr, this.auth);
      if (this.snvtPtr != 0) {
         try {
            this.data = this.getData(17, this.auth);
         } catch (LonException var6) {
            return;
         }

         this.hasSelfDoc = true;
         LonInputStream in = new LonInputStream(this.data);
         this.docLength = in.readUnsigned16();
         this.numNvsDocs = in.readUnsigned8();
         this.version = in.readUnsigned8();
         switch (this.version) {
            case 0:
               this.hdrLength = 5;
               break;
            case 1:
               this.hdrLength = 6;
               this.numNvsDocs = (in.readUnsigned8() << 8) + this.numNvsDocs;
               break;
            case 2:
               this.hdrLength = 17;
               this.maxMumNvs = this.numNvsDocs = (in.readUnsigned8() << 8) + this.numNvsDocs;
               break;
            default:
               throw new LonException("Unknown selfdoc header version {" + this.version + "}.");
         }

         this.mtagCount = in.readUnsigned8();
         if (this.log().isLoggable(Level.FINE)) {
            System.out.println("numNvsDocs = " + this.numNvsDocs);
            System.out.println("mtagCount = " + this.mtagCount);
         }

         if (this.version == 2) {
            this.staticNvCount = in.readUnsigned16();
            this.currentNvCount = in.readUnsigned16();
            this.maxNvInUse = in.readUnsigned16();
            this.aliasCount = in.readUnsigned16();
            this.nodeSelfDocLen = in.readUnsigned16();
            int val = in.readUnsigned8();
            this.bindingII = (val & 128) > 0;
            this.queryStats = (val & 64) > 0;
            if (this.log().isLoggable(Level.FINE)) {
               System.out.println("staticNvCount = " + this.staticNvCount);
               System.out.println("currentNvCount = " + this.currentNvCount);
               System.out.println("maxNvInUse = " + this.maxNvInUse);
               System.out.println("aliasCount = " + this.aliasCount);
               System.out.println("nodeSelfDocLen = " + this.nodeSelfDocLen);
               System.out.println("bindingII = " + this.bindingII);
               System.out.println("queryStats = " + this.queryStats);
            }
         }
      }
   }

   private byte[] getData(int length, boolean auth) throws LonException {
      byte[] data;
      if (this.snvtPtr == 65535) {
         data = Neuron.querySnvt(this.lonComm, this.sendAddr, 0, length, auth, 11);
      } else {
         data = Neuron.readMemory(this.lonComm, 0, this.sendAddr, this.snvtPtr, length, auth, false, 11);
      }

      return data;
   }

   private Logger log() {
      return this.lonComm.lonNetwork().log();
   }
}
