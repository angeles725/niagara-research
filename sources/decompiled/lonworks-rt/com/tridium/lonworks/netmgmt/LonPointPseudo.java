package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonLinkStatus;
import com.tridium.lonworks.local.BPseudoNV;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.sys.BComponent;

public class LonPointPseudo extends LonPointLocal {
   BComponent pseudoComp = null;
   BPseudoNV pseudoNv = null;

   public LonPointPseudo(BINvContainer nvCntr, BNetworkVariable nv, BLonLinkStatus status, BLonDevice localDevice, BComponent pseudoComp, BPseudoNV pseudoNv) {
      super(nvCntr, nv, status, localDevice);
      this.pseudoComp = pseudoComp;
      this.pseudoNv = pseudoNv;
   }

   private LonPointPseudo() {
   }

   @Override
   public Object cloneMe() {
      LonPointPseudo lp = new LonPointPseudo();
      lp.nvCntr = this.nvCntr;
      lp.origNv = this.origNv;
      lp.status = this.status;
      lp.addressIndex = this.addressIndex;
      lp.overlappCnt = this.overlappCnt;
      lp.localDevice = this.localDevice;
      lp.pseudoComp = this.pseudoComp;
      lp.pseudoNv = this.pseudoNv;
      return lp;
   }

   @Override
   public BNvProps getNvProps() {
      return this.pseudoNv.getNvProps();
   }

   @Override
   public BNvConfigData getNvConfigData() {
      return this.pseudoNv.getNvConfigData();
   }

   @Override
   public BNetworkVariable getNetworkVariable() {
      return this.pseudoNv;
   }

   @Override
   public BLonNvDirection getDirection() {
      return this.getNvConfigData().getDirection();
   }

   @Override
   public boolean isPseudo() {
      return true;
   }

   @Override
   public boolean isLocal() {
      return false;
   }

   @Override
   public String getDeviceName() {
      return this.pseudoComp.getDisplayName(null);
   }

   @Override
   public String getSummaryString() {
      return "pseudo." + this.pseudoComp.getDisplayName(null) + ":" + (this.pseudoNv != null ? this.pseudoNv.getDisplayName(null) : "??");
   }

   @Override
   public String getNvName() {
      return this.pseudoNv != null ? this.pseudoNv.getDisplayName(null) : "??";
   }

   @Override
   public int hashCode() {
      return 1073741824 | this.pseudoNv.hashCode();
   }

   @Override
   public String toString() {
      return "LonPointPseudo " + super.toString();
   }

   @Override
   protected BIAddressEntry getAddressEntry(int ndx) {
      return this.localDevice.getDeviceData().getAddressEntry(ndx);
   }
}
