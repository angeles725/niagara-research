package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.io.LonInputStream;

public class NvDoc implements NetMessages {
   public int origin;
   private boolean hasExtension;
   private boolean sychronous;
   private boolean polled;
   private boolean updateOffline;
   private boolean servTypeConfig;
   private boolean priorityConfig;
   private boolean authConfigurable;
   private boolean configNv;
   private int snvtType;
   private int ver01Desc;
   private BLonNvDirection direction;
   private BLonServiceType serviceType;
   private boolean authenticated;
   private boolean priority;
   private String name = null;
   public NvSelfDoc selfDoc = null;
   private int arraySize = 0;
   private int nvIndex = 0;
   private int nvLength = 0;
   public boolean extended;

   public static NvDoc makeNvDocVersion01(LonInputStream descIn, LonInputStream selfIn, BNvConfigData nvConfig, int nvIndex, boolean extended) {
      NvDoc nvDoc = new NvDoc();
      int desc = descIn.readUnsigned16();
      nvDoc.hasExtension = (desc & 32768) != 0;
      nvDoc.sychronous = (desc & 16384) != 0;
      nvDoc.polled = (desc & 8192) != 0;
      nvDoc.updateOffline = (desc & 4096) != 0;
      nvDoc.servTypeConfig = (desc & 2048) != 0;
      nvDoc.priorityConfig = (desc & 1024) != 0;
      nvDoc.authConfigurable = (desc & 512) != 0;
      nvDoc.configNv = (desc & 256) != 0;
      nvDoc.snvtType = desc & 0xFF;
      nvDoc.ver01Desc = desc;
      nvDoc.extended = extended;
      if (nvDoc.hasExtension) {
         int mask = selfIn.readUnsigned8();
         if ((mask & 128) != 0) {
            selfIn.readUnsigned8();
         }

         if ((mask & 64) != 0) {
            selfIn.readUnsigned8();
         }

         if ((mask & 32) != 0) {
            nvDoc.name = selfIn.readString();
         }

         if ((mask & 16) != 0) {
            nvDoc.selfDoc = new NvSelfDoc(selfIn.readString());
         }

         if ((mask & 8) != 0) {
            nvDoc.arraySize = selfIn.readUnsigned16();
         }
      }

      nvDoc.direction = nvConfig.getDirection();
      nvDoc.serviceType = nvConfig.getServiceType();
      nvDoc.authenticated = nvConfig.getAuthenticated();
      nvDoc.priority = nvConfig.getPriority();
      nvDoc.nvIndex = nvIndex;
      return nvDoc;
   }

   public static NvDoc makeNvDocVersion2(LonComm lonComm, LonAddress sndAdr, int nvIndex, boolean auth, boolean extended) throws LonException {
      NvDoc nvDoc = new NvDoc();
      byte[] desc = InstallUtil.getNvInfo(lonComm, sndAdr, auth, nvIndex, 0);
      nvDoc.nvLength = desc[0] >> 3 & 31;
      nvDoc.origin = desc[0] & 3;
      nvDoc.direction = (desc[1] & 16) != 0 ? BLonNvDirection.output : BLonNvDirection.input;
      nvDoc.serviceType = BLonServiceType.make(desc[1] & 3);
      nvDoc.sychronous = (desc[2] & 64) != 0;
      nvDoc.polled = (desc[2] & 32) != 0;
      nvDoc.updateOffline = (desc[2] & 16) != 0;
      nvDoc.servTypeConfig = (desc[2] & 8) != 0;
      nvDoc.priorityConfig = (desc[2] & 4) != 0;
      nvDoc.authConfigurable = (desc[2] & 2) != 0;
      nvDoc.configNv = (desc[2] & 1) != 0;
      nvDoc.snvtType = desc[3] & 255;
      if ((desc[4] & 8) != 0 && (desc[4] & 32) != 0) {
         StringBuilder sb = new StringBuilder();

         for (int i = 9; i < desc.length && desc[i] != 0; i++) {
            sb.append((char)desc[i]);
         }

         nvDoc.name = sb.toString();
      } else if ((desc[4] & 32) != 0) {
         StringBuilder sb = new StringBuilder();
         byte[] nam = InstallUtil.getNvInfo(lonComm, sndAdr, auth, nvIndex, 2);

         for (int i = 0; i < nam.length && nam[i] != 0; i++) {
            sb.append((char)nam[i]);
         }

         nvDoc.name = sb.toString();
      }

      if ((desc[4] & 16) != 0) {
         nvDoc.selfDoc = new NvSelfDoc(InstallUtil.getNvSdInfo(lonComm, sndAdr, auth, nvIndex));
      }

      nvDoc.arraySize = ((desc[5] & 255) << 8) + (desc[6] & 255);
      nvDoc.nvIndex = nvIndex;
      BNvConfigData nvConfig = NmUtil.queryNvConfigData(lonComm, sndAdr, nvIndex, auth, extended);
      nvDoc.direction = nvConfig.getDirection();
      nvDoc.serviceType = nvConfig.getServiceType();
      nvDoc.authenticated = nvConfig.getAuthenticated();
      nvDoc.priority = nvConfig.getPriority();
      return nvDoc;
   }

   public boolean hasExtension() {
      return this.hasExtension;
   }

   public boolean isSychronous() {
      return this.sychronous;
   }

   public boolean isPolled() {
      return this.polled;
   }

   public boolean isUpdateOffline() {
      return this.updateOffline;
   }

   public boolean isServTypeConfig() {
      return this.servTypeConfig;
   }

   public boolean isPriorityConfig() {
      return this.priorityConfig;
   }

   public boolean isAuthConfigurable() {
      return this.authConfigurable;
   }

   public boolean isConfigNv() {
      return this.configNv;
   }

   public int getSnvtType() {
      return this.snvtType;
   }

   public boolean getAuthenticated() {
      return this.authenticated;
   }

   public boolean getPriority() {
      return this.priority;
   }

   public BLonServiceType getServiceType() {
      return this.serviceType;
   }

   public BLonNvDirection getDirection() {
      return this.direction;
   }

   public boolean isArray() {
      return this.arraySize > 0;
   }

   public int getVer01Desc() {
      return this.ver01Desc;
   }

   public int getNvIndex() {
      return this.nvIndex;
   }

   public int getNvLength() {
      return this.nvLength;
   }

   public int getArraySize() {
      return this.arraySize > 0 ? this.arraySize : 1;
   }

   public int getObjectIndex(int ndx) {
      return this.selfDoc != null ? this.selfDoc.getObjectIndex(ndx) : -1;
   }

   public String getObjectIndexString() {
      return this.selfDoc != null ? this.selfDoc.getObjectIndexString() : "";
   }

   public int getMemberIndex(int ndx) {
      return this.selfDoc != null ? this.selfDoc.getMemberIndex(ndx) : -1;
   }

   public boolean isMfrMember() {
      return this.selfDoc != null ? this.selfDoc.isMfrMember() : false;
   }

   public int getConfigIndex() {
      return this.selfDoc != null ? this.selfDoc.getConfigIndex() : -1;
   }

   public boolean isChangeableType() {
      return this.selfDoc != null ? this.selfDoc.isChangeableType() : false;
   }

   public BModifyFlags getModifyFlag() {
      return this.selfDoc != null ? this.selfDoc.getModifyFlag() : BModifyFlags.DEFAULT;
   }

   public BLonConfigScope getScope() {
      return this.selfDoc != null ? this.selfDoc.getScope() : BLonConfigScope.node;
   }

   public String getSelect() {
      return this.selfDoc != null ? this.selfDoc.getSelect() : "";
   }

   public String getName() {
      return this.name == null ? "var_" + this.nvIndex : this.name;
   }

   public void setNvIndex(int nvIndex) {
      this.nvIndex = nvIndex;
   }

   public void setNvLength(int nvLength) {
      this.nvLength = nvLength;
   }

   @Override
   public String toString() {
      return "\nname = "
         + this.getName()
         + "\nDirection = "
         + this.getDirection()
         + "\nnvIndex = "
         + this.getNvIndex()
         + "\nconfig nv = "
         + this.isConfigNv()
         + "\narraySize = "
         + this.arraySize
         + "\nselfDoc = "
         + this.selfDoc
         + "\n";
   }
}
