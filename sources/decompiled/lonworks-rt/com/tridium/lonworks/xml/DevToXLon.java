package com.tridium.lonworks.xml;

import java.util.Vector;
import javax.baja.lonworks.BConfigParameter;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BMessageTag;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BConfigProps;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonElementQualifiers;
import javax.baja.lonworks.londata.BLonEnum;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.lonworks.londata.LonFacetsUtil;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

public class DevToXLon {
   public static XLonInterfaceFile devToXLon(BLonDevice dev) {
      XLonInterfaceFile xfile = new XLonInterfaceFile();
      XLonDevice xdev = new XLonDevice();
      xdev.setName(dev.getName());
      SlotCursor<Property> sc = dev.getProperties();

      while (sc.nextObject()) {
         BObject o = sc.get();
         if (o.getType().is(BNetworkVariable.TYPE)) {
            xdev.addAttribute(sc.property().getName(), getXNetworkVariable(xfile, (BNetworkVariable)o));
         } else if (o.getType().is(BNetworkConfig.TYPE)) {
            xdev.addAttribute(sc.property().getName(), getXNetworkConfig(xfile, (BNetworkConfig)o));
         } else if (o.getType().is(BConfigParameter.TYPE)) {
            xdev.addAttribute(sc.property().getName(), getXConfigProperty(xfile, (BConfigParameter)o));
         } else if (o.getType().is(BMessageTag.TYPE) && ((BMessageTag)o).isOutput()) {
            xdev.addAttribute(sc.property().getName(), getXMessageTag((BMessageTag)o));
         }
      }

      xdev.deviceData = getXDeviceData(dev);
      xfile.addAttribute(dev.getName(), xdev);
      return xfile;
   }

   private static XDeviceData getXDeviceData(BLonDevice dev) {
      XDeviceData xdd = new XDeviceData();
      xdd.setName("deviceData");
      BDeviceData dd = dev.getDeviceData();
      xdd.programID = dd.getProgramId().getByteArray();
      xdd.aliasCount = dd.getAliasTable().getAliasArray().length;
      xdd.bindingII = dd.getBindingII();
      xdd.applicationType = dd.getHosted() ? "hostSelect" : "unknown";
      xdd.domains = dd.getTwoDomains() ? 2 : 1;
      xdd.addressTableEntries = dd.getAddressCount();
      xdd.nodeSelfID = dd.getSelfDoc();
      xdd.freezeChannelPriorities = dd.getFreezeChannelPriorities();
      return xdd;
   }

   private static XNetworkVariable getXNetworkVariable(XLonInterfaceFile xfile, BNetworkVariable nv) {
      BNvProps nvProps = nv.getNvProps();
      XNetworkVariable xnv = new XNetworkVariable();
      xnv.setName(nv.getName());
      int snvt = nvProps.getSnvtType();
      xnv.snvtType = XLonDataUtil.getSnvtTypeName(snvt, "");
      xnv.index = nvProps.getNvIndex();
      xnv.objectIndex = getObjectIndex(nvProps.getObjectIndex());
      xnv.memberIndex = nvProps.getMemberIndex();
      xnv.polled = nvProps.getPolled();
      xnv.authenticatedConfigurable = nvProps.getAuthConf();
      xnv.serviceTypeConfigurable = nvProps.getServiceConf();
      xnv.priorityConfigurable = nvProps.getPriorityConf();
      xnv.offline = nvProps.getModifyOffline();
      xnv.sync = nvProps.getSync();
      xnv.changeType = nvProps.getChangeableType();
      BNvConfigData nvConfig = nv.getNvConfigData();
      xnv.priority = nvConfig.getPriority();
      xnv.direction = nvConfig.getDirection().getTag();
      xnv.serviceType = nvConfig.getServiceType().getTag();
      xnv.authenticated = nvConfig.getAuthenticated();
      if (snvt <= 0) {
         addData(xfile, xnv, nv.getData());
      }

      return xnv;
   }

   private static String getObjectIndex(int ndx) {
      return ndx >= 0 ? Integer.toString(ndx) : "";
   }

   private static XNetworkConfig getXNetworkConfig(XLonInterfaceFile xfile, BNetworkConfig nc) {
      XNetworkConfig xnc = new XNetworkConfig();
      xnc.setName(nc.getName());
      BNcProps ncProps = nc.getNcProps();
      xnc.index = ncProps.getNvIndex();
      int snvt = ncProps.getSnvtType();
      xnc.snvtType = XLonDataUtil.getSnvtTypeName(snvt, "");
      int scpt = ncProps.getConfigIndex();
      xnc.scptType = XLonDataUtil.getScptTypeName(scpt, "");
      xnc.modifyFlag = getModifyFlags(ncProps.getModifyFlag());
      xnc.scope = ncProps.getScope().getTag();
      xnc.select = ncProps.getSelect();
      BNvConfigData nvConfig = nc.getNvConfigData();
      xnc.priority = nvConfig.getPriority();
      xnc.direction = nvConfig.getDirection().getTag();
      xnc.serviceType = nvConfig.getServiceType().getTag();
      xnc.authenticated = nvConfig.getAuthenticated();
      xnc.config = true;
      if (snvt <= 0 && scpt <= 0) {
         addData(xfile, xnc, nc.getData());
      }

      return xnc;
   }

   private static XConfigProperty getXConfigProperty(XLonInterfaceFile xfile, BConfigParameter cp) {
      XConfigProperty xcp = new XConfigProperty();
      xcp.setName(cp.getName());
      BConfigProps cfgProps = cp.getConfigProps();
      int scpt = cfgProps.getConfigIndex();
      xcp.scptType = XLonDataUtil.getScptTypeName(scpt, "");
      xcp.length = cfgProps.getLength();
      xcp.modifyFlag = getModifyFlags(cfgProps.getModifyFlag());
      xcp.scope = cfgProps.getScope().getTag();
      xcp.select = cfgProps.getSelect();
      if (scpt <= 0) {
         addData(xfile, xcp, cp.getData());
      }

      return xcp;
   }

   private static void addData(XLonInterfaceFile xfile, XLonTyped xd, BLonData ld) {
      SlotCursor<Property> sc = ld.getProperties();

      while (sc.nextObject()) {
         BObject obj = sc.get();
         Type typ = obj.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            Property p = sc.property();
            BFacets f = p.getFacets();
            BLonElementQualifiers e = LonFacetsUtil.getQualifiers(f);
            XElementQualifier eq = new XElementQualifier(p.getName(), e);
            if (typ.is(BLonEnum.TYPE)) {
               eq.setEnumDef(addEnum(xfile, ((BLonEnum)obj).getEnum()));
            }

            BUnit u = (BUnit)f.get("units");
            if (u != null) {
               eq.setEngUnit(u.getUnitName());
            }

            xd.addAttribute(p.getName(), eq);
         } else if (typ.is(BLonData.TYPE)) {
            addData(xfile, xd, (BLonData)obj);
         }
      }
   }

   private static String addEnum(XLonInterfaceFile xfile, BEnum en) {
      XEnumDef xe = new XEnumDef();
      BEnumRange er = en.getRange();
      int[] ords = er.getOrdinals();

      for (int i = 0; i < ords.length; i++) {
         xe.addEnum(er.get(ords[i]).getTag(), Integer.toString(ords[i]));
      }

      Vector<XEnumDef> v = xfile.enums;

      for (int i = 0; i < v.size(); i++) {
         XEnumDef vxd = v.elementAt(i);
         if (xe.equals(vxd)) {
            return vxd.getName();
         }
      }

      String name = "enum" + v.size();
      xe.setName(name);
      v.addElement(xe);
      return name;
   }

   private static String getModifyFlags(BModifyFlags flgs) {
      StringBuilder sb = new StringBuilder();
      if (flgs.isDisabled()) {
         sb.append("objDisable ");
      }

      if (flgs.isOffline()) {
         sb.append("offline ");
      }

      if (flgs.isConst()) {
         sb.append("constant ");
      }

      if (flgs.isReset()) {
         sb.append("reset ");
      }

      if (flgs.isMfgOnly()) {
         sb.append("mfgOnly ");
      }

      int len = sb.length();
      return len > 0 ? sb.toString().substring(0, len - 1) : "anytime";
   }

   private static XMessageTag getXMessageTag(BMessageTag mtag) {
      XMessageTag xmtag = new XMessageTag();
      xmtag.setName(mtag.getName());
      return xmtag;
   }
}
