package com.tridium.lonworks.local;

import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.xml.LonXMLReader;
import com.tridium.lonworks.xml.XLonDataUtil;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import com.tridium.lonworks.xml.XNetworkConfig;
import com.tridium.lonworks.xml.XNetworkVariable;
import com.tridium.lonworks.xml.XTypeDef;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.baja.lonworks.BDynamicDevice;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BLocalImportXmlParameter;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.SortUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.sync.Transaction;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;

public class LocalDev {
   public static void importXLon(BLocalLonDevice ld, BLocalImportXmlParameter p) {
      boolean leave = false;
      boolean remove = false;

      try {
         BOrd ord = p.getFile();
         XLonInterfaceFile root = LonXMLReader.decode(ord);
         BDynamicDevice dynDev = new BDynamicDevice();
         DynaDev.importXLon(dynDev, root, null);
         Context tx = Transaction.start(ld, BLocalLonDevice.noInfoChange);
         XLonDevice dev = root.getLonDevice();
         ld.setSelfDoc(dev.deviceData.nodeSelfID);
         BINetworkVariable[] curNvs = ld.getNetworkVariables();
         BINetworkVariable[] newNvs = dynDev.getNetworkVariables();
         XNetworkVariable[] xnvs = dev.getNetworkVariables();
         XNetworkConfig[] xncs = dev.getNetworkConfigs();

         for (int i = 0; i < curNvs.length; i++) {
            if (curNvs[i] != null) {
               if (i >= newNvs.length || newNvs[i] == null || !isEquivalent(newNvs[i], curNvs[i])) {
                  ld.remove(((BComponent)curNvs[i]).getPropertyInParent(), tx);
                  remove = true;
               } else if (i < newNvs.length) {
                  newNvs[i] = null;
                  leave = true;
               }
            }
         }

         for (int ix = 0; ix < newNvs.length; ix++) {
            if (newNvs[ix] != null) {
               if (newNvs[ix].isNetworkVariable()) {
                  BNetworkVariable nv = (BNetworkVariable)newNvs[ix];
                  BLocalNv lnv = new BLocalNv();
                  lnv.getNvProps().copyFrom(nv.getNvProps());
                  lnv.getNvConfigData().copyFrom(nv.getNvConfigData());
                  lnv.getNvConfigData().setUnbound(lnv.getNvProps().getNvIndex());
                  lnv.setSelfDoc(getSelfDoc(getNv(xnvs, ix)));
                  setData(nv, lnv);
                  ld.add(nv.getName(), lnv, tx);
               } else if (newNvs[ix].isNetworkConfig()) {
                  BNetworkConfig nc = (BNetworkConfig)newNvs[ix];
                  BLocalNci lnc = new BLocalNci();
                  lnc.getNcProps().copyFrom(nc.getNcProps());
                  lnc.getNvConfigData().copyFrom(nc.getNvConfigData());
                  lnc.setSelfDoc(getSelfDoc(getNci(xncs, ix), root));
                  setData(nc, lnc);
                  ld.add(nc.getName(), lnc, tx);
               }
            }
         }

         Transaction.end(ld, tx);
         if (remove && leave) {
            reorder(ld);
         }
      } catch (Throwable var16) {
         ld.lonNetwork().log().log(Level.SEVERE, "Error importing xml.", var16);
      }
   }

   private static String getSelfDoc(XNetworkVariable xnv) {
      if (xnv != null && xnv.memberIndex >= 0) {
         StringBuilder sb = new StringBuilder();
         sb.append("@").append(xnv.objectIndex);
         sb.append(xnv.mfgMember ? "#" : "|");
         sb.append(Integer.toString(xnv.memberIndex));
         if (xnv.memberArraySize > 1) {
            sb.append("[").append(Integer.toString(xnv.memberArraySize)).append("]");
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   private static String getSelfDoc(XNetworkConfig xnc, XLonInterfaceFile root) {
      if (xnc == null) {
         return "";
      } else {
         int scope;
         int typNdx;
         if (xnc.scptType.length() > 0) {
            scope = 0;
            typNdx = XLonDataUtil.scptTypeFromString(xnc.scptType);
         } else {
            if (xnc.typeDef.length() <= 0) {
               return "";
            }

            XTypeDef xTypDef = root.resolveTypeDef(xnc.typeDef);
            if (xTypDef == null) {
               return "";
            }

            String typScp = xTypDef.typeScope;
            if (typScp.length() == 0) {
               return "";
            }

            scope = Integer.parseInt(typScp.substring(0, typScp.indexOf(44)));
            typNdx = Integer.parseInt(typScp.substring(typScp.indexOf(44) + 1));
         }

         StringBuilder sb = new StringBuilder();
         sb.append("&").append(XLonDataUtil.scopeFromString(xnc.scope)).append(",");
         sb.append(xnc.select).append(",");
         sb.append(Integer.toString(scope)).append("\\x");
         sb.append(Integer.toString(XLonDataUtil.flagFromString(xnc.modifyFlag), 16)).append(",");
         sb.append(Integer.toString(typNdx)).append(";");
         return sb.toString();
      }
   }

   private static XNetworkVariable getNv(XNetworkVariable[] nvs, int ndx) {
      for (int i = 0; i < nvs.length; i++) {
         if (nvs[i] != null && nvs[i].index == ndx) {
            return nvs[i];
         }
      }

      return null;
   }

   private static XNetworkConfig getNci(XNetworkConfig[] ncs, int ndx) {
      for (int i = 0; i < ncs.length; i++) {
         if (ncs[i] != null && ncs[i].index <= ndx && ndx <= ncs[i].getMaxIndex()) {
            return ncs[i];
         }
      }

      return null;
   }

   private static boolean isEquivalent(BINetworkVariable inv1, BINetworkVariable inv2) {
      if (!inv1.getName().equals(inv2.getName())) {
         return false;
      } else if (!inv1.getNvConfigData().getDirection().equals(inv2.getNvConfigData().getDirection())) {
         return false;
      } else {
         if (inv1.isNetworkVariable() && inv2.isLocalNv()) {
            if (!((BNetworkVariable)inv1).getNvProps().equivalent(((BLocalNv)inv2).getNvProps())) {
               return false;
            }
         } else {
            if (!inv1.isNetworkConfig() || !inv2.isLocalNci()) {
               return false;
            }

            if (!((BNetworkConfig)inv1).getNcProps().equivalent(((BLocalNci)inv2).getNcProps())) {
               return false;
            }
         }

         return inv1.getData().hasEquivalentElements(inv2.getData());
      }
   }

   private static void setData(BLonData src, BLonComponent dest) {
      Property[] psrc = src.getPropertiesArray();

      for (int i = 0; i < psrc.length; i++) {
         Property p = psrc[i];
         BValue v = src.get(p);
         if (v instanceof BLonPrimitive || v instanceof BLonData) {
            dest.add(p.getName(), v, p.getDefaultFlags(), p.getFacets(), null);
         }
      }
   }

   private static void reorder(BLocalLonDevice ld) {
      ArrayList<Property> v = new ArrayList<>();
      SlotCursor<Property> c = ld.getProperties();

      while (c.next()) {
         Property p = c.property();
         if (!p.isFrozen()) {
            v.add(p);
         }
      }

      Property[] a = v.toArray(new Property[0]);
      String[] keys = new String[a.length];

      for (int i = 0; i < keys.length; i++) {
         BObject bo = ld.get(a[i]);
         if (bo.getType().is(BINetworkVariable.TYPE)) {
            keys[i] = getKey("b", ((BINetworkVariable)bo).getNvIndex());
         } else {
            keys[i] = getKey("z", i);
         }
      }

      SortUtil.sort(keys, a, true);
      ld.reorder(a);
   }

   private static String getKey(String prefix, int index) {
      String i = Integer.toString(index);
      return prefix + TextUtil.getSpaces(5 - i.length()) + i;
   }
}
