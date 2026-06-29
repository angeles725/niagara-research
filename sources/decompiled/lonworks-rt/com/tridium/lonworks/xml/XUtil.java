package com.tridium.lonworks.xml;

import com.tridium.lonworks.util.LonStringUtil;
import com.tridium.lonworks.util.selfdoc.SelfDocUtil;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BUnionQualifier;
import javax.baja.lonworks.datatypes.BUnionQualifiers;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.londata.BLonBigInteger;
import javax.baja.lonworks.londata.BLonBoolean;
import javax.baja.lonworks.londata.BLonByteArray;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonDataUnion;
import javax.baja.lonworks.londata.BLonDouble;
import javax.baja.lonworks.londata.BLonElementQualifiers;
import javax.baja.lonworks.londata.BLonFloat;
import javax.baja.lonworks.londata.BLonLong;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.lonworks.londata.BLonString;
import javax.baja.lonworks.londata.LonFacetsUtil;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.units.UnitDatabase;

public class XUtil {
   private static BUnit CELSIUS = UnitDatabase.getUnit("celsius");
   private static XLonInterfaceFile st = null;

   public static BLonData getLonDataNv(XNetVariable xnv, XLonInterfaceFile root) {
      BLonData data = getLonDataClass(xnv, root);
      if (data != null) {
         return data;
      } else {
         int snvt = XLonDataUtil.snvtTypeFromString(xnv.snvtType);
         if (snvt > 0) {
            return SnvtUtil.getLonData(snvt, XLonDataUtil.isDiffQualifier(xnv.snvtType));
         } else {
            XElementQualifier[] eqs = xnv.getElementQualifiers(root);
            return makeLonData(eqs, xnv.getUnion(), root);
         }
      }
   }

   public static BLonData getLonDataNc(XNetworkConfig xnc, XLonInterfaceFile root) {
      BLonData ld = getLonDataNv(xnc, root);
      if (ld == null) {
         return null;
      } else {
         byte[] init = xnc.getInitBytes(root);
         if (init != null) {
            ld.fromNetBytes(init);
         }

         addMinMax(xnc, ld);
         return ld;
      }
   }

   public static BLonData getLonDataCp(
      XConfigProperty xcp, XLonInterfaceFile root, XLonDevice xdev, BLonConfigScope scope, String select, BLonDevice dev, int ndx
   ) {
      BLonData ld = getLonDataClass(xcp, root);

      try {
         if (ld == null) {
            ld = getCpLonData(xcp, scope, select, root, xdev);
         }
      } catch (Throwable var10) {
         dev.log().warning(dev.getName() + "." + xcp.getName() + ": " + var10.getMessage());
      }

      if (ld == null) {
         BLonElementQualifiers eq = BLonElementQualifiers.make(BLonElementType.na, xcp.length);
         ld = new BLonData(BLonByteArray.make(xcp.length), eq, null);
      }

      byte[] init = xcp.getInitBytes(root);
      if (init != null) {
         byte[] dat = init;
         if (ndx > 0 && init.length >= xcp.length * (ndx + 1)) {
            dat = new byte[xcp.length];
            System.arraycopy(init, xcp.length * ndx, dat, 0, xcp.length);
         }

         ld.fromNetBytes(dat);
      }

      addMinMax(xcp, ld);
      return ld;
   }

   private static BLonData getLonDataClass(XLonTyped x, XLonInterfaceFile root) {
      String typeSpec = x.getTypeSpec(root);
      if (typeSpec != null && typeSpec.length() != 0) {
         try {
            return (BLonData)Sys.getType(typeSpec).getInstance();
         } catch (Throwable var4) {
            throw new BajaRuntimeException("Unable to create class " + typeSpec, var4);
         }
      } else {
         return null;
      }
   }

   public static BLonData makeLonData(XElementQualifier[] eqs, XLonInterfaceFile root) {
      return makeLonData(eqs, null, root);
   }

   public static BLonData makeLonData(XElementQualifier[] eqs, XUnion union, XLonInterfaceFile root) {
      if (eqs.length == 0) {
         return null;
      } else {
         boolean useUnion = union != null && isValid(union);
         BLonData data = (BLonData)(useUnion ? new BLonDataUnion() : new BLonData());

         for (int i = 0; i < eqs.length; i++) {
            XElementQualifier eq = eqs[i];
            BFacets f = LonFacetsUtil.makeFacets(getQuals(eq), getUnits(eq.getEngUnit()));
            data.add(eq.getName(), getLonPrimitive(eq, root), 0, f, null);
         }

         if (useUnion) {
            BUnionQualifiers uqs = ((BLonDataUnion)data).getUnionQuals();
            uqs.setConditionProp(union.branchElem);
            XUnionBranch[] a = union.getUnionBranches();

            for (int i = 0; i < a.length; i++) {
               XUnionBranch b = a[i];
               BUnionQualifier uq = new BUnionQualifier();
               uq.setBranch(b.branchName);
               uq.setBranchProps(getBranchPropsString(b.branchName, eqs));
               uq.setConditions(b.condition);
               uqs.add(b.branchName, uq);
            }
         }

         return data;
      }
   }

   private static boolean isValid(XUnion union) {
      if (union.branchElem.indexOf(63) >= 0) {
         return false;
      } else {
         Vector<XUnionBranch> v = union.branch;
         int i = 0;

         while (i < v.size()) {
            XUnionBranch b = v.elementAt(i);
            if (b.branchName.length() != 0 && b.branchName.indexOf(63) < 0) {
               if (b.condition.length() != 0 && b.condition.indexOf(63) < 0) {
                  i++;
                  continue;
               }

               return false;
            }

            return false;
         }

         return true;
      }
   }

   private static String getBranchPropsString(String name, XElementQualifier[] eqs) {
      Array<String> a = new Array(String.class);

      for (int i = 0; i < eqs.length; i++) {
         String n = eqs[i].getUnionBranch();
         if (n != null && n.equals(name)) {
            a.add(eqs[i].getName());
         }
      }

      return LonStringUtil.toString((String[])a.trim());
   }

   private static BLonElementQualifiers getQuals(XElementQualifier eq) {
      try {
         return BLonElementQualifiers.make(
            BLonElementType.make(eq.getElementType()),
            eq.hasMinimum(),
            eq.getMinimum(),
            eq.hasMaximum(),
            eq.getMaximum(),
            eq.getResolution(),
            eq.getOffset(),
            eq.hasByteOffset(),
            eq.getByteOffset(),
            eq.getBitOffset(),
            eq.hasInvalid(),
            eq.getInvalid(),
            eq.getLength()
         );
      } catch (Throwable var2) {
         throw new RuntimeException("Unable to create element qualifier " + eq.name + "\nencodes to:" + eq.encodeToString());
      }
   }

   private static BUnit getUnits(String units) {
      if (units != null && units.length() != 0) {
         try {
            return BUnit.getUnit(units);
         } catch (RuntimeException var3) {
            String s = "Could not find units " + units + "  " + var3;
            System.out.println(s);
            return null;
         }
      } else {
         return null;
      }
   }

   private static BLonPrimitive getLonPrimitive(XElementQualifier eq, XLonInterfaceFile root) {
      BLonPrimitive prim;
      switch (XElementQualifier.qualTypeFromString(eq.getElementType())) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 8:
         case 11:
         case 12:
            prim = BLonFloat.DEFAULT;
            break;
         case 5:
         case 17:
         case 18:
            if (eq.getResolution() < 1.0F) {
               prim = BLonDouble.DEFAULT;
            } else {
               prim = BLonLong.DEFAULT;
            }
            break;
         case 6:
         case 10:
            prim = BLonBoolean.FALSE;
            break;
         case 7:
         case 9:
            prim = getEnum(eq.getEnumDef(), root);
            break;
         case 13:
            prim = BLonString.DEFAULT;
            break;
         case 14:
         case 15:
         default:
            prim = BLonByteArray.make(eq.getLength());
            break;
         case 16:
            prim = BLonDouble.DEFAULT;
            break;
         case 19:
            if (eq.getResolution() < 1.0F) {
               prim = BLonDouble.DEFAULT;
            } else {
               prim = BLonBigInteger.DEFAULT;
            }
      }

      return prim;
   }

   private static BLonPrimitive getEnum(String enumType, XLonInterfaceFile root) {
      XEnumDef ed = root.resolveEnumDef(enumType);
      if (ed == null) {
         throw new RuntimeException("No enumType " + enumType);
      } else {
         return ed.getEnum();
      }
   }

   private static BLonData getCpLonData(XConfigProperty xcp, BLonConfigScope scope, String sel, XLonInterfaceFile root, XLonDevice xdev) {
      XCpTypeDef cpType = null;
      if (xcp.scptType.length() > 0) {
         cpType = findStandardCpType(xcp.scptType);
      } else {
         if (xcp.typeDef.length() <= 0) {
            return null;
         }

         XTypeDef tdef = root.resolveTypeDef(xcp.typeDef);
         if (tdef == null) {
            System.out.println("WARNING:typeDef not found " + xcp.getName() + ":" + xcp.typeDef);
            return null;
         }

         if (!(tdef instanceof XCpTypeDef)) {
            return makeLonData(tdef.getElementQualifiers(root), tdef.union, root);
         }

         cpType = (XCpTypeDef)tdef;
      }

      if (cpType == null) {
         throw new RuntimeException("Could not find type {" + xcp.getName() + "}");
      } else {
         xcp.setXTypeDef(cpType);
         if (!cpType.inherited) {
            XLonInterfaceFile ifile = xcp.scptType.length() > 0 ? getStandard() : root;
            return cpType.getLonData(ifile);
         } else {
            int nvIndex = -1;
            XNetworkVariable[] nvs = xdev.getNetworkVariables();
            if (scope.equals(BLonConfigScope.nv)) {
               nvIndex = SelfDocUtil.getFirstIndex(sel);
            } else {
               if (!scope.equals(BLonConfigScope.object)) {
                  throw new RuntimeException("Scpt inherits data type from nv but scope is " + scope + " {" + xcp.getName() + "}");
               }

               nvIndex = getInheritanceSourceNv(xcp, xdev, sel);
            }

            XNetworkVariable xnv = null;

            for (int i = 0; i < nvs.length; i++) {
               if (nvs[i].index <= nvIndex && nvs[i].index + nvs[i].arraySize > nvIndex) {
                  xnv = nvs[i];
                  break;
               }
            }

            if (xnv == null) {
               throw new RuntimeException("Scpt inherits data type from nv " + nvIndex + ".  Unable to find nv. {" + xcp.getName() + "}");
            } else {
               BLonData ld = getLonDataNv(xnv, root);
               if (xcp.scptType.equals("CpSndDelta") || xcp.scptType.equals("CpOffset")) {
                  tempToDeltaTemp(ld);
               }

               return ld;
            }
         }
      }
   }

   public static int getInheritanceSourceNv(XConfigProperty xcp, XLonDevice xdev, String sel) {
      int nvIndex = -1;
      XNetworkVariable[] nvs = xdev.getNetworkVariables();
      if (xcp.principalNv.length() <= 0) {
         throw new RuntimeException("Inherited type with object scope but no principalNv {" + xcp.getName() + "}");
      } else {
         int obj = SelfDocUtil.getFirstIndex(sel);
         int mem = Integer.parseInt(xcp.principalNv.substring(1));
         boolean mfgMem = xcp.principalNv.startsWith("#");

         for (int i = 0; i < nvs.length; i++) {
            XNetworkVariable nv = nvs[i];
            if (nv != null && nv.objectIndex.length() > 0) {
               int firstObj = SelfDocUtil.getFirstIndex(nv.objectIndex);
               int lastObj = SelfDocUtil.getLastIndex(nv.objectIndex);
               if (obj >= firstObj && obj <= lastObj && mem >= nv.memberIndex && mem < nv.memberIndex + nv.memberArraySize && nv.mfgMember == mfgMem) {
                  nvIndex = nv.index;
                  break;
               }
            }
         }

         if (nvIndex == -1) {
            throw new RuntimeException("Can't find object " + obj + " with member " + xcp.principalNv + " {" + xcp.getName() + "}");
         } else {
            return nvIndex;
         }
      }
   }

   private static void tempToDeltaTemp(BLonData ld) {
      SlotCursor<Property> c = ld.getProperties();

      while (c.nextObject()) {
         BObject obj = c.get();
         Type typ = obj.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            BFacets f = c.property().getFacets();
            BUnit u;
            if (f != null && (u = (BUnit)f.get("units")) != null && u.getDimension().equals(CELSIUS.getDimension())) {
               u = BUnit.getUnit(u.getUnitName() + " degrees");
               f = BFacets.make(f, "units", u);
               ld.setFacets(c.property(), f);
            }
         } else if (typ.is(BLonData.TYPE)) {
            tempToDeltaTemp((BLonData)obj);
         }
      }
   }

   private static XCpTypeDef findStandardCpType(String type) {
      XLonInterfaceFile standard = getStandard();
      Vector<XTypeDef> types = standard.types;

      for (int i = 0; i < types.size(); i++) {
         XTypeDef t = types.elementAt(i);
         if (t.isCpType() && type.equals(t.getName())) {
            return (XCpTypeDef)t;
         }
      }

      return null;
   }

   public static XCpTypeDef findCpType(String type) {
      XLonInterfaceFile standard = getStandard();
      Vector<XTypeDef> types = standard.types;

      for (int i = 0; i < types.size(); i++) {
         XTypeDef t = types.elementAt(i);
         if (t.isCpType() && type.equals(t.getName())) {
            return (XCpTypeDef)t;
         }
      }

      return null;
   }

   public static XCpTypeDef findCpType(int scope, int index) {
      XLonInterfaceFile standard = getStandard();
      Vector<XTypeDef> types = standard.types;
      String typeScope = Integer.toString(scope) + "," + Integer.toString(index);

      for (int i = 0; i < types.size(); i++) {
         XTypeDef t = types.elementAt(i);
         if (t.isCpType() && t.getTypeScope().equals(typeScope)) {
            return (XCpTypeDef)t;
         }
      }

      return null;
   }

   public static void addMinMax(XIConfig xc, BLonData dat) {
      XTypeDef xtd = xc.getXTypeDef();
      if (xtd != null && xtd.isCpType()) {
         XCpTypeDef xctd = (XCpTypeDef)xtd;
         if (xctd.getMax() == null && xctd.getMin() != null) {
         }
      }

      float[] fmin = null;
      float[] fmax = null;
      if (xc.getMin().length() > 0) {
         fmin = getFloats(xc.getMin());
      }

      if (xc.getMax().length() > 0) {
         fmax = getFloats(xc.getMax());
      }

      if (fmin != null || fmax != null) {
         setFacets(dat, fmin, fmax, 0);
      }
   }

   private static int setFacets(BLonData dat, float[] fmin, float[] fmax, int cnt) {
      SlotCursor<Property> c = dat.getProperties();

      while (c.nextObject()) {
         BObject obj = c.get();
         Type typ = obj.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            if (typ.is(BLonFloat.TYPE)) {
               Property p = c.property();
               BFacets f = p.getFacets();
               if (fmin != null) {
                  f = BFacets.make(f, "min", BFloat.make(fmin[cnt]));
               }

               if (fmax != null) {
                  f = BFacets.make(f, "max", BFloat.make(fmax[cnt]));
               }

               dat.setFacets(p, f);
            }

            cnt++;
         } else if (typ.is(BLonData.TYPE)) {
            cnt = setFacets(dat, fmin, fmax, cnt);
         }
      }

      return cnt;
   }

   private static float[] getFloats(String f) {
      StringTokenizer st = new StringTokenizer(f, " ,|");
      int cnt = st.countTokens();
      float[] fa = new float[cnt];

      for (int i = 0; i < cnt; i++) {
         fa[i] = Float.valueOf(st.nextToken());
      }

      return fa;
   }

   public static XLonInterfaceFile getStandard() {
      if (st == null) {
         try {
            BOrd ref = BOrd.make("module://lonworks/javax/baja/lonworks/standard.lnml");
            st = LonXMLReader.decode(ref);
         } catch (Throwable var1) {
            var1.printStackTrace();
            throw new RuntimeException("Cannot access standard.lnml " + var1);
         }
      }

      return st;
   }
}
