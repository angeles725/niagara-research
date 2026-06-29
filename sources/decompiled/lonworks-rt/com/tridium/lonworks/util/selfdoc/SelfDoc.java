package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.util.LonByteArrayUtil;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import java.util.Vector;
import java.util.logging.Logger;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.io.LonInputStream;

public class SelfDoc implements NetMessages {
   private LonComm lonComm;
   public static final int MAX_DATA_SIZE = 11;
   private SnvtStruct hdr = null;
   public int snvtPtr;
   private LonAddress sendAddr;
   private boolean auth;
   private boolean countsOnly;
   public byte[] doc;
   public String nodeDocString = null;
   public NodeSelfDoc nodeDoc = null;
   public NvDoc[] nvs = null;
   public int hdrLength;
   public int docLength;
   private boolean bindingII = false;
   private boolean queryStats = false;
   private int aliasCnt = 0;
   private boolean extended = false;

   public SelfDoc(BLonDevice lonDevice) throws LonException {
      this(lonDevice.lonComm(), NmUtil.getSendAddress(lonDevice), lonDevice.authenticate(), false, lonDevice.isExtended());
   }

   public SelfDoc(LonComm lonComm, LonAddress s, boolean a, boolean ex) throws LonException {
      this(lonComm, s, a, false, ex);
   }

   public SelfDoc(LonComm lonComm, LonAddress s, boolean a, boolean co, boolean ex) throws LonException {
      this.lonComm = lonComm;
      this.sendAddr = s;
      this.auth = a;
      this.countsOnly = co;
      this.extended = ex;
      this.hdr = new SnvtStruct(lonComm, this.sendAddr, this.auth);
      if (this.hdr.hasSelfDoc) {
         this.snvtPtr = this.hdr.snvtPtr;
         switch (this.hdr.version) {
            case 0:
            case 1:
               this.processSelfDocV01();
               break;
            case 2:
               this.processSelfDocV2();
         }
      }
   }

   private void processSelfDocV2() throws LonException {
      this.bindingII = this.hdr.bindingII;
      this.queryStats = this.hdr.queryStats;
      this.aliasCnt = this.hdr.aliasCount;
      if (!this.countsOnly) {
         this.nodeDocString = InstallUtil.getNodeInfo(this.lonComm, this.sendAddr, this.auth);
         this.log().fine("\nnode self documentation string =>" + this.nodeDocString);

         try {
            this.nodeDoc = new NodeSelfDoc(this.nodeDocString);
         } catch (Throwable var8) {
            this.nodeDoc = null;
         }

         int numNvs = this.hdr.currentNvCount;
         Vector<NvDoc> v = new Vector<>(numNvs);
         int nvIndex = 0;
         int maxNdx = this.hdr.maxNvInUse;

         while (nvIndex <= maxNdx) {
            try {
               NvDoc doc = NvDoc.makeNvDocVersion2(this.lonComm, this.sendAddr, nvIndex, this.auth, this.extended);
               nvIndex += doc.getArraySize();
               v.addElement(doc);
            } catch (FailedResponseException var6) {
               nvIndex++;
            } catch (LonException var7) {
               var7.printStackTrace();
               throw var7;
            }
         }

         this.nvs = new NvDoc[v.size()];
         v.copyInto(this.nvs);
      }
   }

   private void processSelfDocV01() throws LonException {
      this.hdrLength = this.hdr.hdrLength;
      this.docLength = this.hdr.docLength;
      int numNvDocs = this.hdr.numNvsDocs;
      if (this.docLength > this.hdrLength) {
         this.doc = this.getData(this.lonComm, this.sendAddr, this.hdrLength, this.docLength - this.hdrLength, this.auth);
         this.log().fine("self documentation length = " + this.doc.length + " " + LonByteArrayUtil.toString(this.doc));
         LonInputStream in = new LonInputStream(this.doc);
         LonInputStream selfDoc = new LonInputStream(this.doc);
         selfDoc.skip(numNvDocs * 2);
         this.nodeDocString = selfDoc.readString();
         this.log().fine("node self documentation string =>" + this.nodeDocString);
         this.nodeDoc = new NodeSelfDoc(this.nodeDocString);
         this.nvs = new NvDoc[numNvDocs];
         int nvIndex = 0;

         for (int i = 0; i < numNvDocs; i++) {
            BNvConfigData configData = NmUtil.queryNvConfigData(this.lonComm, this.sendAddr, nvIndex, this.auth, this.extended);
            this.nvs[i] = NvDoc.makeNvDocVersion01(in, selfDoc, configData, nvIndex, this.extended);
            if (!this.countsOnly && this.nvs[i].getSnvtType() == 0) {
               this.nvs[i].setNvLength(NmUtil.getNvLength(this.lonComm, this.sendAddr, nvIndex, this.auth));
            }

            nvIndex += this.nvs[i].getArraySize();
         }

         if (selfDoc.available() > 0) {
            int val = selfDoc.read();
            if (val >= 0) {
               this.bindingII = (val & 128) > 0;
               this.queryStats = (val & 64) > 0;
               this.aliasCnt = val & 63;
               if (this.aliasCnt == 63) {
                  if (selfDoc.available() >= 2) {
                     this.aliasCnt = selfDoc.readUnsigned16();
                  } else {
                     this.aliasCnt = 0;
                  }
               }
            }
         }
      }
   }

   private byte[] getData(LonComm lonComm, LonAddress sendAddr, int offset, int len, boolean auth) throws LonException {
      byte[] data;
      if (this.snvtPtr == 65535) {
         data = Neuron.querySnvt(lonComm, sendAddr, offset, len, auth, 11);
      } else {
         data = Neuron.readMemory(lonComm, 0, sendAddr, this.snvtPtr + offset, len, auth, false, 11);
      }

      return data;
   }

   public NvDoc[] getNvDocs() {
      return this.nvs;
   }

   public NodeSelfDoc getNodeDoc() {
      return this.nodeDoc;
   }

   public int getMaxNvIndex() {
      if (this.nvs != null && this.nvs.length != 0) {
         NvDoc lastDoc = this.nvs[this.nvs.length - 1];
         return lastDoc.getNvIndex() + lastDoc.getArraySize() - 1;
      } else {
         return 0;
      }
   }

   public int getAliasOffset() {
      return this.getVersion() < 2 ? this.getMaxNvIndex() + 1 : this.hdr.maxMumNvs;
   }

   public boolean hasNvs() {
      return this.nvs != null && this.nvs.length > 0;
   }

   public int getVersion() {
      return this.hdr.version;
   }

   public int getMsgtagCount() {
      return this.hdr.mtagCount;
   }

   public boolean getBindingII() {
      return this.bindingII;
   }

   public boolean getQueryStats() {
      return this.queryStats;
   }

   public int getAliasCnt() {
      return this.aliasCnt;
   }

   public SnvtStruct getSnvtStruct() {
      return this.hdr;
   }

   private Logger log() {
      return this.lonComm.lonNetwork().log();
   }
}
