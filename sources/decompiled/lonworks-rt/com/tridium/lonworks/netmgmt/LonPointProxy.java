package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonLinkStatus;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.control.BControlPoint;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.proxy.BLonProxyExt;

public class LonPointProxy extends LonPointLocal {
   private BControlPoint cp;
   private BLonProxyExt ext = null;
   LonPointProxy sec = null;

   public LonPointProxy(BINvContainer nvCntr, BNetworkVariable nv, BLonLinkStatus status, BControlPoint cp, BLonDevice localDevice, BLonProxyExt ext) {
      super(nvCntr, nv, status, localDevice);
      this.cp = cp;
      this.ext = ext;
   }

   private LonPointProxy() {
   }

   @Override
   public Object cloneMe() {
      LonPointProxy lp = new LonPointProxy();
      lp.nvCntr = this.nvCntr;
      lp.origNv = this.origNv;
      lp.status = this.status;
      lp.addressIndex = this.addressIndex;
      lp.overlappCnt = this.overlappCnt;
      lp.localDevice = this.localDevice;
      lp.cp = this.cp;
      lp.ext = this.ext;
      return lp;
   }

   @Override
   public boolean isProxy() {
      return true;
   }

   @Override
   public boolean requiresAddressEntry() {
      return false;
   }

   @Override
   public String getDeviceName() {
      return "LocalDev";
   }

   public BLonProxyExt getProxyExtension() {
      return this.ext;
   }

   @Override
   public String getSummaryString() {
      return "proxy." + super.getSummaryString();
   }

   @Override
   public String getNvName() {
      return this.cp.getDisplayName(null);
   }

   @Override
   public String toString() {
      return "LonPointProxy " + super.toString();
   }

   @Override
   public boolean validatePoint(BLonLinkType linkType, boolean priority, boolean debug, boolean isProxyTgt) {
      if (!this.isActive()) {
         return true;
      } else {
         BNvConfigData cnf = this.getNvConfigData();
         BLonServiceType svcType = NmUtil.linkTypeToServiceType(linkType);
         if (cnf.getServiceType() != svcType) {
            if (debug) {
               System.out.println("Must match nvConfig service type");
            }

            this.status = BLonLinkStatus.newLink;
            return false;
         } else if (cnf.getPriority() != priority) {
            if (debug) {
               System.out.println("Must match nvConfig service type priority");
            }

            this.status = BLonLinkStatus.newLink;
            return false;
         } else {
            return true;
         }
      }
   }
}
