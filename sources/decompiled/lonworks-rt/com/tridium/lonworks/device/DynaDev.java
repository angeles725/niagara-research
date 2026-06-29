package com.tridium.lonworks.device;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.util.NameUtil;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.selfdoc.NodeSelfDoc;
import com.tridium.lonworks.xml.XConfigProperty;
import com.tridium.lonworks.xml.XCpTypeDef;
import com.tridium.lonworks.xml.XDeviceData;
import com.tridium.lonworks.xml.XDeviceFacets;
import com.tridium.lonworks.xml.XLonDataUtil;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import com.tridium.lonworks.xml.XMessageTag;
import com.tridium.lonworks.xml.XNetworkConfig;
import com.tridium.lonworks.xml.XNetworkVariable;
import com.tridium.lonworks.xml.XUtil;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.control.BControlPoint;
import javax.baja.lonworks.BConfigParameter;
import javax.baja.lonworks.BDynamicDevice;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonObject;
import javax.baja.lonworks.BMessageTag;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BConfigProps;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BExtDeviceData;
import javax.baja.lonworks.datatypes.BImportParameters;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.lonworks.proxy.BLonProxyExt;
import javax.baja.lonworks.util.ScptUtil;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.SortUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.sync.Transaction;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Type;

public class DynaDev {
   private DynaDev.Prop[] props;
   private DynaDev.Obj[] objects = null;
   private static final int CURRENT = 0;
   private static final int NEW = 1;
   private static final int OBSOLETE = 2;
   private static final BFacets DEVICE_SPEC = BFacets.make("deviceSpecific", true);
   private Logger log = null;
   BDynamicDevice dev;
   int[] nvObjNdx = new int[4095];
   boolean useLonObjects = false;
   boolean syncNvConfig = false;
   boolean useZeroBasedArrays = false;

   public static void importXLon(BDynamicDevice dev, XLonInterfaceFile root, BImportParameters param) {
      DynaDev dd = new DynaDev(dev);
      if (param != null) {
         dd.useLonObjects = param.getUseLonObjects();
         dd.syncNvConfig = param.getSyncNvConfig();
      }

      dd.useZeroBasedArrays = root.getLonDevice().useZeroBasedArrays;
      dd.doImportXLon(root);
   }

   public static void importXLon(BDynamicDevice dev, XLonDevice xdev, boolean use) throws Exception {
      DynaDev dd = new DynaDev(dev);
      dd.useLonObjects = use;
      dd.useZeroBasedArrays = xdev.useZeroBasedArrays;
      dd.dynamicUpdate(xdev, null);
   }

   private DynaDev(BDynamicDevice dev) {
      this.dev = dev;
   }

   private void doImportXLon(XLonInterfaceFile root) {
      XLonInterfaceFile st = XUtil.getStandard();
      if (st != null) {
         root.addAttribute("standard", st);
      }

      if (this.log().isLoggable(Level.FINE)) {
         this.log().fine("importXlon " + this.dev.getDisplayName(null));
      }

      try {
         XLonDevice xdev = root.getLonDevice();
         this.dynamicUpdate(xdev, root);
         if (this.dev.isRunning() && this.syncNvConfig) {
            this.uploadNvConfig();
         }
      } catch (Throwable var4) {
         this.log().log(Level.SEVERE, "error in doImportXml", var4);
      }
   }

   private void dynamicUpdate(XLonDevice xdev, XLonInterfaceFile root) throws Exception {
      this.initPropertyList();
      this.dev.initImport();
      Context tx = Transaction.start(this.dev, new BasicContext());
      this.addObjects(xdev, tx);
      int maxNvIndex = 0;
      XNetworkVariable[] nvs = xdev.getNetworkVariables();

      for (int i = 0; i < nvs.length; i++) {
         this.addNetworkVariable(nvs[i], root, tx);
         int maxNdx = nvs[i].getMaxIndex();
         if (maxNdx > maxNvIndex) {
            maxNvIndex = maxNdx;
         }
      }

      XNetworkConfig[] ncs = xdev.getNetworkConfigs();

      for (int ix = 0; ix < ncs.length; ix++) {
         this.addNetworkConfigs(ncs[ix], root, tx);
         int maxNdx = ncs[ix].getMaxIndex();
         if (maxNdx > maxNvIndex) {
            maxNvIndex = maxNdx;
         }
      }

      XConfigProperty[] cfgs = xdev.getConfigProperties();
      int offset = 0;
      int rdOnlyOffset = 0;

      for (int ixx = 0; ixx < cfgs.length; ixx++) {
         if (xdev.hasReadOnlyFile && XLonDataUtil.isReadOnly(cfgs[ixx].modifyFlag)) {
            rdOnlyOffset += this.addConfigProperty(cfgs[ixx], root, xdev, rdOnlyOffset, tx);
         } else {
            offset += this.addConfigProperty(cfgs[ixx], root, xdev, offset, tx);
         }
      }

      XMessageTag[] mtags = xdev.getMessageTags();

      for (int ixxx = 0; ixxx < mtags.length; ixxx++) {
         this.addMessageTag(mtags[ixxx], tx);
      }

      this.addDeviceData(xdev.deviceData, xdev.deviceFacets, maxNvIndex, mtags.length, tx);
      this.processLonObject(tx);
      this.removeRemainingProps(tx);
      Transaction.end(this.dev, tx);
      this.reorder();
      this.dev.closeImport();
      this.dev.fireDynamicOpComplete(BString.make("hi"));
   }

   private void addDeviceData(XDeviceData xdd, XDeviceFacets xdf, int maxNvIndex, int mtagCount, Context c) {
      BDeviceData dd = this.dev.getDeviceData();
      boolean ecs = xdd.maxNetMgmtVer > 0;
      int addrCnt = Math.max(xdd.addressTableEntries, xdd.numEcsAddressEntries);
      boolean makeExtended = (ecs || addrCnt > 15) && !(dd instanceof BExtDeviceData);
      if (makeExtended) {
         dd = BExtDeviceData.make(dd);
      } else {
         dd = (BDeviceData)dd.newCopy(true);
      }

      if (dd instanceof BExtDeviceData) {
         ((BExtDeviceData)dd).setExtended(ecs);
      }

      dd.setProgramId(BProgramId.make(xdd.programID));
      dd.setAliasTable(new BAliasTable(xdd.aliasCount));
      dd.getAliasTable().setAliasOffset(Math.max(xdd.maxNumNvSupported, maxNvIndex + 1));
      dd.setBindingII(xdd.bindingII);
      dd.setHosted(XLonDataUtil.isHostedApplication(xdd.applicationType));
      dd.setTwoDomains(xdd.domains == 2);
      dd.setAddressCount(addrCnt);
      dd.setMsgTagCount(Math.max(mtagCount, xdd.numEcsMessageTags));
      dd.setSelfDoc(xdd.nodeSelfID);
      dd.setFreezeChannelPriorities(xdd.freezeChannelPriorities);

      NodeSelfDoc sd;
      try {
         sd = new NodeSelfDoc(xdd.nodeSelfID);
      } catch (Exception var12) {
         sd = null;
      }

      dd.setHasNodeObject(sd != null && sd.hasNodeObject());
      if (sd != null) {
         int ndx = sd.getNodeObjectIndex();
         if (ndx > 0) {
            if (xdf == null) {
               xdf = new XDeviceFacets();
            }

            xdf.nodeObjectIndex = ndx;
         }
      }

      if (xdf != null) {
         BFacets f = DeviceFacets.makeDeviceFacets(xdf);
         if (f != null) {
            dd.setFacets(f);
         }
      }

      if (makeExtended) {
         this.dev.set(BLonDevice.deviceData, dd, c);
      } else {
         this.dev.getDeviceData().copyFrom(dd, BDeviceData.importChanges);
      }

      if (dd.getProgramId().hasChangeableNvs()) {
         this.dev.deviceDataChanged(BDeviceData.programId, null);
      }
   }

   private void addNetworkVariable(XNetworkVariable xnv, XLonInterfaceFile root, Context c) {
      int arraySize = xnv.arraySize;
      if (arraySize <= 0) {
         arraySize = 1;
      }

      int memberArraySize = xnv.memberArraySize;
      if (memberArraySize <= 0) {
         memberArraySize = 1;
      }

      int[] objNdx = this.getObjectIndexArray(arraySize, memberArraySize, xnv.objectIndex);
      int[] memNdx = this.getMemberIndexArray(arraySize, memberArraySize, xnv.memberIndex);
      int snvt = XLonDataUtil.snvtTypeFromString(xnv.snvtType);
      BLonData data = XUtil.getLonDataNv(xnv, root);
      if (data != null && data.getByteLength() > Lon.maxNvLength()) {
         this.log().severe(xnv.getName() + " ignored because length " + data.getByteLength() + " > " + Lon.maxNvLength() + " bytes");
      } else {
         if (data != null) {
            setNonCritical(data);
         }

         for (int i = 0; i < arraySize; i++) {
            BNetworkVariable nv = new BNetworkVariable();
            BNvProps nvProps = nv.getNvProps();
            nvProps.setNvIndex(xnv.index + i);
            nvProps.setSnvtType(snvt);
            nvProps.setObjectIndex(objNdx[i]);
            nvProps.setMemberIndex(memNdx[i]);
            nvProps.setPolled(xnv.polled);
            nvProps.setAuthConf(xnv.authenticatedConfigurable);
            nvProps.setServiceConf(xnv.serviceTypeConfigurable);
            nvProps.setPriorityConf(xnv.priorityConfigurable);
            nvProps.setModifyOffline(xnv.offline);
            nvProps.setSync(xnv.sync);
            nvProps.setChangeableType(xnv.changeType);
            BNvConfigData nvConfig = nv.getNvConfigData();
            nvConfig.setPriority(xnv.priority);
            nvConfig.setDirection((BLonNvDirection)nvConfig.getDirection().getRange().get(xnv.direction));
            nvConfig.setSelector(16383 - xnv.index - i);
            BLonServiceType st = (BLonServiceType)nvConfig.getServiceType().getRange().get(xnv.serviceType);
            nvConfig.setServiceType(st.getWriteServiceType());
            nvConfig.setAuthenticated(xnv.authenticated);
            this.nvObjNdx[nvProps.getNvIndex()] = nvProps.getObjectIndex();
            String name = xnv.getName();
            String fixName = NameUtil.toJavaName(name, false);
            if (arraySize > 1) {
               name = this.arrayedName(name, i);
               fixName = this.arrayedName(fixName, i);
            }

            if (data == null) {
               this.log().warning("No lonData for " + this.dev.getDisplayName(null) + "." + name);
               return;
            }

            if (i > 0) {
               data = (BLonData)data.newCopy();
            }

            if (nvConfig.isOutput()) {
               this.setReadonly(data);
            }

            nv.setData(data);
            int flags = nvConfig.isInput() ? 1024 : 0;
            if (this.log().isLoggable(Level.FINE)) {
               this.log().fine("add nv " + nvProps.getNvIndex() + " " + name);
            }

            this.addLonComponent(name, fixName, nv, flags, c, BFacets.NULL, objNdx[i]);
         }
      }
   }

   private void setReadonly(BLonData data) {
      Flags.setAllReadonly(data, true, null);
      SlotCursor<Property> c = data.getProperties();

      while (c.next(BLonData.class)) {
         this.setReadonly((BLonData)c.get());
      }
   }

   public static void setNonCritical(BLonData data) {
      SlotCursor<Property> c = data.getProperties();

      while (c.next()) {
         BObject o = c.get();
         if (o instanceof BLonPrimitive) {
            Property p = c.property();
            data.setFlags(p, data.getFlags(p) | 65536);
         } else if (o instanceof BLonData) {
            setNonCritical((BLonData)o);
         }
      }
   }

   private void addNetworkConfigs(XNetworkConfig xnc, XLonInterfaceFile root, Context c) {
      int arraySize = xnc.arraySize;
      if (arraySize <= 0) {
         arraySize = 1;
      }

      String[] selects = this.getSelects(arraySize, xnc.select);
      BLonConfigScope scope = this.getScope(xnc.scope);
      int snvt = XLonDataUtil.snvtTypeFromString(xnc.snvtType);
      BLonData data = XUtil.getLonDataNc(xnc, root);
      if (data != null && data.getByteLength() > Lon.maxNvLength()) {
         this.log().severe(xnc.getName() + " ignored because length " + data.getByteLength() + " > " + Lon.maxNvLength() + " bytes");
      } else {
         for (int i = 0; i < arraySize; i++) {
            BNetworkConfig nci = new BNetworkConfig();
            BNcProps ncProps = nci.getNcProps();
            ncProps.setNvIndex(xnc.index + i);
            ncProps.setConfigIndex(XLonDataUtil.scptTypeFromString(xnc.scptType));
            ncProps.setSnvtType(snvt);
            ncProps.setModifyFlag(this.getModifyFlags(xnc.modifyFlag));
            ncProps.setScope(scope);
            ncProps.setSelect(selects[i]);
            BNvConfigData nvConfig = nci.getNvConfigData();
            nvConfig.setPriority(xnc.priority);
            nvConfig.setDirection((BLonNvDirection)nvConfig.getDirection().getRange().get(xnc.direction));
            nvConfig.setSelector(16383 - xnc.index - i);
            BLonServiceType st = (BLonServiceType)nvConfig.getServiceType().getRange().get(xnc.serviceType);
            nvConfig.setServiceType(st.getWriteServiceType());
            nvConfig.setAuthenticated(xnc.authenticated);
            String name = xnc.getName();
            String fixName = NameUtil.toJavaName(name, false);
            if (arraySize > 1) {
               name = this.arrayedName(name, i);
               fixName = this.arrayedName(fixName, i);
            }

            if (data == null) {
               this.log().warning("No lonData for " + this.dev.getDisplayName(null) + "." + xnc.getName());
               return;
            }

            if (i > 0) {
               data = (BLonData)data.newCopy();
            }

            if (!ncProps.getModifyFlag().isReadWrite()) {
               this.setReadonly(data);
            }

            BFacets f = this.isDeviceSpecific(xnc.modifyFlag) ? DEVICE_SPEC : BFacets.NULL;
            nci.setData(data);
            if (this.log().isLoggable(Level.FINE)) {
               this.log().fine("add nci " + ncProps.getNvIndex() + " " + name);
            }

            this.addLonComponent(name, fixName, nci, 0, c, f, this.getObjectNdx(scope, selects[i]));
         }
      }
   }

   private int addConfigProperty(XConfigProperty xcp, XLonInterfaceFile root, XLonDevice xdev, int offset, Context c) {
      int arraySize = xcp.dimension;
      if (arraySize <= 0) {
         arraySize = 1;
      }

      String[] selects = this.getSelects(arraySize, xcp.select);
      BLonConfigScope scope = this.getScope(xcp.scope);

      for (int i = 0; i < arraySize; i++) {
         BConfigParameter cp = new BConfigParameter();
         BConfigProps cfgProps = cp.getConfigProps();
         cfgProps.setConfigIndex(XLonDataUtil.scptTypeFromString(xcp.scptType));
         cfgProps.setOffset(offset + i * xcp.length);
         cfgProps.setLength(xcp.length);
         cfgProps.setModifyFlag(this.getModifyFlags(xcp.modifyFlag));
         cfgProps.setScope(scope);
         cfgProps.setSelect(selects[i]);
         String name = xcp.getName();
         String fixName = NameUtil.toJavaName(name, false);
         if (arraySize > 1) {
            name = this.arrayedName(name, i);
            fixName = this.arrayedName(fixName, i);
         }

         BLonData data = XUtil.getLonDataCp(xcp, root, xdev, scope, selects[i], this.dev, i);
         if (!cfgProps.getModifyFlag().isReadWrite()) {
            this.setReadonly(data);
         }

         cp.setData(data);
         XCpTypeDef xtype = (XCpTypeDef)xcp.getXTypeDef();
         BFacets f = BFacets.NULL;
         if (xtype != null && xtype.inherited) {
            if (xcp.scope.equals("object")) {
               try {
                  int sourceNv = XUtil.getInheritanceSourceNv(xcp, xdev, cfgProps.getSelect());
                  f = BFacets.make("inherited", BBoolean.TRUE, "sourceNv", BInteger.make(sourceNv));
               } catch (Throwable var18) {
               }
            } else {
               f = BFacets.make("inherited", true);
            }
         }

         if (this.isDeviceSpecific(xcp.modifyFlag)) {
            f = BFacets.make(f, DEVICE_SPEC);
         }

         if (this.log().isLoggable(Level.FINE)) {
            this.log().fine("add cp " + cfgProps.getOffset() + ":" + cfgProps.getLength() + " " + name);
         }

         this.addLonComponent(name, fixName, cp, 0, c, f, this.getObjectNdx(scope, selects[i]));
      }

      return xcp.length * arraySize;
   }

   private int getObjectNdx(BLonConfigScope scope, String selects) {
      if (selects == null) {
         return -1;
      } else {
         int[] nselects = ScptUtil.decomposeSelect(selects);
         if (nselects.length == 0) {
            return -1;
         } else if (scope == BLonConfigScope.object) {
            return nselects.length > 1 ? -1 : nselects[0];
         } else if (scope != BLonConfigScope.nv) {
            return -1;
         } else {
            int ndx = this.nvObjNdx[nselects[0]];

            for (int i = 1; i < nselects.length; i++) {
               int oNdx = this.nvObjNdx[nselects[i]];
               if (oNdx == -1 || oNdx != ndx) {
                  return -1;
               }
            }

            return ndx;
         }
      }
   }

   private void addLonComponent(String name, String fixName, BLonComponent lc, int flags, Context c, BFacets f, int objNdx) {
      BComponent parent = this.dev;
      boolean newp = false;
      if (this.objects != null && objNdx >= 0 && objNdx < this.objects.length && !this.objects[objNdx].isObsolete()) {
         parent = this.objects[objNdx].lonObj;
         newp = this.objects[objNdx].isNew();
      }

      DynaDev.Prop prop = this.findAndRemoveProperty(name, lc, parent, c);
      if (prop == null) {
         prop = this.findAndRemoveProperty(fixName, lc, parent, c);
      }

      if (prop != null) {
         ((BComplex)parent.get(prop.p)).copyFrom(lc);
         parent.setFacets(prop.p, f, c);
         parent.setFlags(prop.p, flags, newp ? null : c);
      } else {
         if (!SlotPath.isValidName(fixName)) {
            this.log().warning("could not add lonComponent - invalid name " + fixName);
            return;
         }

         parent.add(fixName + "?", lc, flags, f, newp ? null : c);
      }
   }

   private void addObjects(XLonDevice xdev, Context tx) {
      Array<DynaDev.Obj> loa = new Array(DynaDev.Obj.class);
      BLonObject[] blos = this.dev.getLonObjects();

      for (int i = 0; i < blos.length; i++) {
         BLonObject lo = blos[i];
         DynaDev.Obj obj = new DynaDev.Obj(lo, 2, lo.getName());
         loa.add(lo.getObjectId(), obj);
      }

      String nodeSelfID = xdev.deviceData.nodeSelfID;
      NodeSelfDoc sd = new NodeSelfDoc(nodeSelfID);
      NodeSelfDoc.LonMarkObj[] lmos = sd.getObjects();
      if (this.useLonObjects && lmos != null && lmos.length != 0) {
         for (int i = 0; i < lmos.length; i++) {
            String typ = Integer.toString(lmos[i].type);
            String name = lmos[i].name;
            if (name == null) {
               name = "Type" + typ;
            }

            name = SlotPath.escape(name);
            DynaDev.Obj obj = loa.size() > i ? (DynaDev.Obj)loa.get(i) : null;
            if (obj != null) {
               BLonObject lo = obj.lonObj;
               if (!typ.equals(lo.getObjectType())) {
                  lo.setString(BLonObject.objectType, typ, tx);
               }

               if (!obj.name.equals(name)) {
                  ((BComponent)lo.getParent()).rename(lo.getPropertyInParent(), name, tx);
               }

               obj.state = 0;
            } else {
               BLonObject lox = new BLonObject();
               lox.setObjectId(i);
               lox.setObjectType(typ);
               obj = new DynaDev.Obj(lox, 1, name);
               loa.add(obj);
            }
         }
      }

      this.objects = (DynaDev.Obj[])loa.trim();
   }

   private void processLonObject(Context c) {
      for (int i = 0; i < this.objects.length; i++) {
         if (this.objects[i].isNew()) {
            this.dev.add(this.objects[i].name + "?", this.objects[i].lonObj, c);
         } else if (this.objects[i].isObsolete()) {
            this.dev.remove(this.objects[i].lonObj.getPropertyInParent(), c);
         }
      }
   }

   private void addMessageTag(String name, BMessageTag mt, Context c) {
      DynaDev.Prop prop = this.findAndRemoveProperty(name, mt, this.dev, c);
      if (prop != null) {
         ((BComplex)prop.c.get(prop.p)).copyFrom(mt);
      } else {
         this.dev.add(name + "?", mt, 0, c);
      }
   }

   private void addMessageTag(XMessageTag xmt, Context c) {
      BMessageTag mt = new BMessageTag();
      mt.setIndex(xmt.index);
      mt.setDirection(BLonNvDirection.output);
      if (this.log().isLoggable(Level.FINE)) {
         this.log().fine("add mt " + mt.getIndex() + " " + xmt.getName());
      }

      this.addMessageTag(xmt.getName(), mt, c);
   }

   private String arrayedName(String name, int ndx) {
      if (!this.useZeroBasedArrays) {
         ndx++;
      }

      return name + "_" + ndx;
   }

   private void initPropertyList() {
      Array<DynaDev.Prop> a = new Array(DynaDev.Prop.class);
      this.initPropertyList(a, this.dev);
      this.props = (DynaDev.Prop[])a.trim();
   }

   private void initPropertyList(Array<DynaDev.Prop> a, BComponent c) {
      Property[] p = c.getPropertiesArray();

      for (int i = 0; i < p.length; i++) {
         Type typ = p[i].getType();
         if (typ.is(BINvContainer.TYPE)) {
            this.initPropertyList(a, (BComponent)c.get(p[i]));
         } else if (typ.is(BLonComponent.TYPE) || typ.is(BMessageTag.TYPE)) {
            a.add(new DynaDev.Prop(c, p[i]));
         }
      }
   }

   private DynaDev.Prop findAndRemoveProperty(String name, BComplex lc, BComponent prt, Context c) {
      for (int i = 0; i < this.props.length; i++) {
         DynaDev.Prop pr = this.props[i];
         if (pr != null) {
            Property p = pr.p;
            if (p != null && p.getName().equals(name) && !p.isFrozen()) {
               if (p.getType() != lc.getType()) {
                  this.log().fine("remove property because of type change " + p.getDefaultDisplayName(null));
                  this.removeLonProperty(pr, c);
                  pr = null;
               } else if (p.getType().is(BLonComponent.TYPE) && !((BLonComponent)pr.c.get(p)).getData().hasEquivalentElements(((BLonComponent)lc).getData())) {
                  this.log().fine("remove because data elements don't match " + p.getDefaultDisplayName(null));
                  this.removeLonProperty(pr, c);
                  pr = null;
               } else if (prt != pr.c) {
                  this.log().fine("move to a new container " + p.getDefaultDisplayName(null));
                  this.removeLonProperty(pr, c);
                  pr = null;
               } else if (p.getType().is(BINetworkVariable.TYPE)) {
                  BINetworkVariable origNv = (BINetworkVariable)pr.c.get(p);
                  BINetworkVariable newNv = (BINetworkVariable)lc;
                  if ((origNv.getSnvtType() > 0 || newNv.getSnvtType() > 0) && origNv.getSnvtType() != newNv.getSnvtType()) {
                     this.log().fine("remove because snvt types don't match " + p.getDefaultDisplayName(null));
                     this.removeLonProperty(pr, c);
                     pr = null;
                  }
               }

               this.props[i] = null;
               return pr;
            }
         }
      }

      return null;
   }

   private void removeRemainingProps(Context c) {
      for (int i = 0; i < this.props.length; i++) {
         DynaDev.Prop pr = this.props[i];
         if (pr != null) {
            Property p = pr.p;
            if (!p.isFrozen()) {
               Type t = p.getType();
               if (t.is(BLonComponent.TYPE) || t.is(BMessageTag.TYPE)) {
                  this.removeLonProperty(pr, c);
               }
            }
         }
      }
   }

   private void removeLonProperty(DynaDev.Prop pr, Context c) {
      Property p = pr.p;
      if (p.getType().is(BLonComponent.TYPE)) {
         String lcName = p.getName();
         BControlPoint[] pnts = this.dev.getLonProxies();

         for (int i = 0; i < pnts.length; i++) {
            BControlPoint cp = pnts[i];
            BLonProxyExt pext = (BLonProxyExt)cp.getProxyExt();
            String targetLc = pext.getTargetComp();
            if (targetLc.lastIndexOf(47) >= 0) {
               targetLc = targetLc.substring(targetLc.lastIndexOf(47) + 1);
            }

            if (targetLc.equals(lcName)) {
               ((BComponent)cp.getParent()).remove(cp.getPropertyInParent(), c);
            }
         }
      }

      pr.c.remove(p, c);
   }

   private BModifyFlags getModifyFlags(String flgs) {
      boolean readWrite = true;
      boolean mfgOnly = false;
      boolean reset = false;
      boolean constant = false;
      boolean offline = false;
      boolean disabled = false;
      StringTokenizer st = new StringTokenizer(flgs, ", ");

      while (st.hasMoreTokens()) {
         String flag = st.nextToken();
         if (flag.equals("mfgOnly")) {
            mfgOnly = true;
            readWrite = false;
         } else if (flag.equals("reset")) {
            reset = true;
         } else if (flag.equals("constant")) {
            constant = true;
            readWrite = false;
         } else if (flag.equals("offline")) {
            offline = true;
         } else if (flag.equals("objDisable")) {
            disabled = true;
         } else if (flag.equals("deviceSpecific")) {
            constant = true;
            readWrite = false;
         }
      }

      return BModifyFlags.make(readWrite, mfgOnly, reset, constant, offline, disabled);
   }

   private boolean isDeviceSpecific(String flgs) {
      return flgs.indexOf("deviceSpecific") >= 0;
   }

   private BLonConfigScope getScope(String scope) {
      if (scope.equals("node")) {
         return BLonConfigScope.node;
      } else if (scope.equals("object")) {
         return BLonConfigScope.object;
      } else {
         return scope.equals("nv") ? BLonConfigScope.nv : BLonConfigScope.node;
      }
   }

   private int[] getObjectIndexArray(int arraySize, int memberArraySize, String index) {
      int[] objIdx = new int[arraySize];
      if (index != null && index.length() > 0) {
         try {
            StringTokenizer st = new StringTokenizer(index, "-");
            int firstNdx = Integer.decode(st.nextToken());
            int ndx = firstNdx;
            int i = 0;

            while (i < arraySize) {
               for (int j = 0; j < memberArraySize; j++) {
                  objIdx[i + j] = ndx;
               }

               ndx++;
               i += memberArraySize;
            }

            return objIdx;
         } catch (Throwable var10) {
         }
      }

      for (int i = 0; i < arraySize; i++) {
         objIdx[i] = -1;
      }

      return objIdx;
   }

   private int[] getMemberIndexArray(int arraySize, int memberArraySize, int memberIndex) {
      int[] memIdx = new int[arraySize];
      int i = 0;

      while (i < arraySize) {
         for (int j = 0; j < memberArraySize; j++) {
            memIdx[i + j] = memberIndex + j;
         }

         i += memberArraySize;
      }

      return memIdx;
   }

   private String[] getSelects(int arraySize, String select) {
      String[] selects = new String[arraySize];
      if (select != null && select.length() > 0) {
         try {
            if (select.indexOf(126) > 0) {
               StringTokenizer st = new StringTokenizer(select, "~");
               int firstNdx = Integer.decode(st.nextToken());
               int lastNdx = Integer.decode(st.nextToken());
               if (lastNdx - firstNdx + 1 != arraySize) {
                  this.log().warning("select " + select + " doesn't match arraySize " + arraySize);
               }

               for (int i = 0; i < arraySize; i++) {
                  selects[i] = Integer.toString(firstNdx + i);
               }
            } else if (select.indexOf(47) > 0) {
               StringTokenizer st = new StringTokenizer(select, "/");

               for (int i = 0; i < arraySize; i++) {
                  selects[i] = st.nextToken();
               }
            } else {
               for (int i = 0; i < arraySize; i++) {
                  selects[i] = select;
               }
            }

            return selects;
         } catch (Throwable var8) {
         }
      }

      for (int i = 0; i < arraySize; i++) {
         selects[i] = "";
      }

      return selects;
   }

   private void uploadNvConfig() throws LonException {
      BINetworkVariable[] nvs = this.dev.getNetworkVariables();

      for (int nvIndex = 0; nvIndex < nvs.length; nvIndex++) {
         if (nvs[nvIndex] != null) {
            BNvConfigData deviceConfig = NmUtil.queryNvConfigData(this.dev, nvIndex);
            BNetworkVariable nv = (BNetworkVariable)nvs[nvIndex];
            BNvConfigData dbConfig = nv.getNvConfigData();
            if (deviceConfig.getDirection() != dbConfig.getDirection()) {
               String err = "BINetworkVariable direction mismatch for " + nv.getDisplayName(null) + "(" + nvIndex + ")";
               this.log().warning(err);
               throw new BajaRuntimeException(err);
            }

            dbConfig.copyFrom(deviceConfig);
         }
      }
   }

   private void reorder() {
      ArrayList<Property> v = new ArrayList<>();
      SlotCursor<Property> c = this.dev.getProperties();

      while (c.next()) {
         Property p = c.property();
         if (!p.isFrozen()) {
            v.add(p);
         }
      }

      Property[] a = v.toArray(new Property[0]);
      String[] keys = new String[a.length];

      for (int i = 0; i < keys.length; i++) {
         BObject bo = this.dev.get(a[i]);
         if (bo.getType().is(BLonObject.TYPE)) {
            keys[i] = this.getKey("a", ((BLonObject)bo).getObjectId());
         } else if (bo.getType().is(BINetworkVariable.TYPE)) {
            keys[i] = this.getKey("b", ((BINetworkVariable)bo).getNvIndex());
         } else if (bo.getType().is(BConfigParameter.TYPE)) {
            String pre = ((BConfigParameter)bo).getConfigProps().getModifyFlag().isConst() ? "co" : "c";
            keys[i] = this.getKey(pre, ((BConfigParameter)bo).getConfigProps().getOffset());
         } else if (bo.getType().is(BMessageTag.TYPE)) {
            keys[i] = "m" + ((BMessageTag)bo).getName();
         } else {
            keys[i] = "z" + Integer.toString(i);
         }
      }

      SortUtil.sort(keys, a, true);
      if (a.length > 0) {
         this.dev.reorder(a);
      }
   }

   private String getKey(String prefix, int index) {
      String i = Integer.toString(index);
      return prefix + TextUtil.getSpaces(5 - i.length()) + i;
   }

   public final Logger log() {
      if (this.log == null && this.dev.getLonNetwork() != null) {
         this.log = Logger.getLogger(this.dev.getLonNetwork().getLogName() + ".DynaDev");
      }

      if (this.log == null) {
         this.log = Logger.getLogger("lon.DynaDev");
      }

      return this.log;
   }

   private class Obj {
      BLonObject lonObj;
      String name;
      int state = 0;

      Obj(BLonObject ob, int st, String nam) {
         this.lonObj = ob;
         this.state = st;
         this.name = nam;
      }

      boolean isNew() {
         return this.state == 1;
      }

      boolean isObsolete() {
         return this.state == 2;
      }
   }

   private class Prop {
      BComponent c;
      Property p;

      Prop(BComponent cp, Property pr) {
         this.c = cp;
         this.p = pr;
      }
   }
}
