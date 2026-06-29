package javax.baja.lonworks.util;

import com.tridium.lonworks.xml.XEnumDef;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import com.tridium.lonworks.xml.XNetVariable;
import com.tridium.lonworks.xml.XTypeDef;
import com.tridium.lonworks.xml.XUtil;
import java.util.Vector;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.londata.BLonByteArray;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonElementQualifiers;
import javax.baja.lonworks.londata.BLonEnum;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.units.UnitDatabase;
import javax.baja.units.UnitDatabase.Quantity;

public class SnvtUtil {
   public static BLonData getLonData(int snvtType, int size) {
      BLonData data = getLonData(snvtType);
      return data != null ? data : new BLonData(BLonByteArray.make(size), BLonElementQualifiers.make(BLonElementType.na, size), null);
   }

   public static BLonData getLonData(int snvtType) {
      return getLonData(snvtType, false);
   }

   public static BLonData getLonData(int snvtType, boolean diff) {
      XLonInterfaceFile standard = XUtil.getStandard();
      Vector<XTypeDef> types = standard.types;
      String typeScope = "0," + Integer.toString(snvtType);

      for (int i = 0; i < types.size(); i++) {
         XTypeDef t = types.elementAt(i);
         if (!t.isCpType() && t.getTypeScope().equals(typeScope)) {
            BLonData ldata = t.getLonData(standard);
            if (diff) {
               makeDiff(ldata);
            }

            return ldata;
         }
      }

      return null;
   }

   public static Object getStandard() {
      return XUtil.getStandard();
   }

   public static BLonData getLonData(Object xnv, Object root) {
      return XUtil.getLonDataNv((XNetVariable)xnv, (XLonInterfaceFile)root);
   }

   public static BLonEnum getStandardEnum(String enumType) {
      XLonInterfaceFile sxfile = XUtil.getStandard();
      XEnumDef ed = sxfile.resolveEnumDef(enumType);
      if (ed == null) {
         throw new RuntimeException("No enumType " + enumType);
      } else {
         return ed.getEnum();
      }
   }

   private static void makeDiff(BLonData ld) {
      SlotCursor<Property> c = ld.getProperties();

      while (c.nextObject()) {
         Property p = c.property();
         Type typ = p.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            BFacets f = p.getFacets();
            BUnit u = (BUnit)f.get("units");
            if (isTemp(u)) {
               ld.setFacets(p, makeNewUnits(f, makeTempDiff(u)));
            }
         } else if (typ.is(BLonData.TYPE)) {
            makeDiff((BLonData)c.get());
         }
      }
   }

   private static final BUnit makeTempDiff(BUnit u) {
      return UnitDatabase.getUnit(u.getUnitName() + " degrees");
   }

   private static final boolean isTemp(BUnit u) {
      Quantity q = UnitDatabase.getDefault().getQuantity(u);
      return q.getName().equals("temperature");
   }

   private static final boolean isTempDiff(BUnit u) {
      Quantity q = UnitDatabase.getDefault().getQuantity(u);
      return q.getName().equals("temperature differential");
   }

   private static BFacets makeNewUnits(BFacets f, BUnit u) {
      BFacets newFacets = BFacets.makeRemove(f, "units");
      return BFacets.make(newFacets, "units", u);
   }
}
