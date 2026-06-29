package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BSetServiceTypeParameter;
import com.tridium.lonworks.util.NmUtil;
import java.util.StringTokenizer;
import javax.baja.lonworks.BLonLink;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.proxy.BLonProxyExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonSetServiceTypeJob extends BLonNetmgmtJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonSetServiceTypeJob.class);
   private ConnectionTable connTable;
   private BSetServiceTypeParameter param;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonSetServiceTypeJob() {
   }

   public BLonSetServiceTypeJob(BLonNetmgmt netMgmt, BSetServiceTypeParameter param) {
      super(netMgmt);
      this.connTable = new ConnectionTable(netMgmt.lonNetwork());
      this.param = param;
   }

   @Override
   public void run() {
      String s = this.param.getSelector();
      BLonLinkType linkType = this.param.getLinkType();
      boolean priority = this.param.getPriority();
      this.log().start("SetServiceType for selector " + s + " " + linkType + (priority ? "/priority" : ""));

      try {
         StringTokenizer st = new StringTokenizer(s, ",. ");

         while (st.hasMoreTokens()) {
            int sel = Integer.decode(st.nextToken());

            for (Connection c = this.connTable.getConnection(sel); c != null; c = c.getSecondary()) {
               if ((c.getLinkType() != linkType || c.getPriority() != priority) && this.isChangeLegal(c.getHub(), linkType, priority)) {
                  this.changeLinkType(c, linkType, priority);
               }
            }
         }
      } catch (Throwable var7) {
         this.fatal("Unexpected exception in BLonSetServiceTypeJob.", var7);
      }

      this.netMgmt.doRefreshLinkTable();
      this.end();
   }

   private boolean isChangeLegal(LonPoint pnt, BLonLinkType linkType, boolean priority) {
      if (linkType == BLonLinkType.pollOnly) {
         return true;
      } else {
         BNvProps nvProps = pnt.getNvProps();
         BNvConfigData cfgDat = pnt.getNvConfigData();
         if (!nvProps.getServiceConf() && cfgDat.getServiceType() != NmUtil.linkTypeToServiceType(linkType)) {
            this.fatal("Service type not configurable in " + pnt.getSummaryString());
            return false;
         } else if (!nvProps.getAuthConf() && cfgDat.getAuthenticated() != (linkType == BLonLinkType.authenticated)) {
            this.fatal("Authenticate not configurable in " + pnt.getSummaryString());
            return false;
         } else if (!nvProps.getPriorityConf() && cfgDat.getPriority() != priority) {
            this.fatal("Priority not configurable in " + pnt.getSummaryString());
            return false;
         } else {
            return true;
         }
      }
   }

   private void changeLinkType(Connection c, BLonLinkType linkType, boolean priority) {
      try {
         LonPoint hub = c.getHub();
         if (hub == null) {
            return;
         }

         if (hub.isProxy()) {
            BLonProxyExt ext = ((LonPointProxy)hub).getProxyExtension();
            ext.setLinkType(linkType);
            ext.setPriority(priority);
         } else {
            BNetworkVariable nv = hub.getNetworkVariable();
            Knob[] kbs = ((BComponent)nv.getParent()).getKnobs(nv.getPropertyInParent());

            for (int i = 0; i < kbs.length; i++) {
               BLonLink lnk = (BLonLink)kbs[i].getLink();
               this.updateLink(lnk, linkType, priority);
            }

            LonPoint[] a = c.getTargets();

            for (int i = 0; i < a.length; i++) {
               if (a[i].isProxy()) {
                  for (LonPointProxy pxy = (LonPointProxy)a[i]; pxy != null; pxy = pxy.sec) {
                     BLonProxyExt ext = pxy.getProxyExtension();
                     ext.setLinkType(linkType);
                     ext.setPriority(priority);
                  }
               }
            }
         }
      } catch (Throwable var11) {
         this.fatal("Unable to change link type for selector " + c.getSelector());
      }
   }

   private void updateLink(BLonLink lnk, BLonLinkType linkType, boolean priority) {
      BNetworkVariable saveNv = null;
      BComponent nvCntr = null;
      Property nvProp = null;
      boolean frozen = false;
      if (!lnk.getMessageTag()) {
         BNetworkVariable nv = lnk.getDestinationNv();
         if (nv.getPropertyInParent().isFrozen()) {
            saveNv = nv;
            nvCntr = (BComponent)nv.getParent();
            nvProp = nv.getPropertyInParent();
            frozen = true;
         }
      }

      lnk.setLinkType(linkType);
      lnk.setPriority(priority);
      if (frozen) {
         BNetworkVariable nv = (BNetworkVariable)nvCntr.get(nvProp);
         nv.setNvProps((BNvProps)saveNv.getNvProps().newCopy(true));
         nv.setNvConfigData((BNvConfigData)saveNv.getNvConfigData().newCopy(true));
      }
   }
}
