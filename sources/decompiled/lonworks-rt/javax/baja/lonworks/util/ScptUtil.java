package javax.baja.lonworks.util;

import com.tridium.lonworks.xml.XCpTypeDef;
import com.tridium.lonworks.xml.XUtil;
import java.util.StringTokenizer;
import javax.baja.lonworks.BConfigParameter;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.datatypes.BConfigProps;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.nre.util.Array;
import javax.baja.sys.BFacets;

public class ScptUtil {
   private static int[] inhTypes = new int[]{7, 9, 10, 11, 12, 13, 14, 18, 19, 20, 23, 26, 27, 33, 213, 257, 258, 276};

   public static BLonData getLonData(int configIndex) {
      XCpTypeDef typDef = XUtil.findCpType(0, configIndex);
      if (typDef == null) {
         throw new RuntimeException("No data definition for configIndex " + configIndex);
      } else {
         return typDef.getLonData(XUtil.getStandard());
      }
   }

   public static boolean doesSelectContain(int ndx, String sel) {
      try {
         StringTokenizer st = new StringTokenizer(sel, ",-~.");
         if (sel.indexOf(45) > 0) {
            int min = Integer.parseInt(st.nextToken());
            int max = Integer.parseInt(st.nextToken());
            return ndx >= min && ndx <= max;
         }

         while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (Integer.parseInt(s) == ndx) {
               return true;
            }
         }
      } catch (Throwable var5) {
         var5.printStackTrace();
      }

      return false;
   }

   public static int[] decomposeSelect(String sel) {
      try {
         StringTokenizer st = new StringTokenizer(sel, ",-~.");
         if (sel.indexOf(45) > 0) {
            int min = Integer.parseInt(st.nextToken());
            int max = Integer.parseInt(st.nextToken());
            int[] a = new int[max - min + 1];

            for (int i = 0; i < a.length; i++) {
               a[i] = min + i;
            }

            return a;
         } else {
            int[] a = new int[st.countTokens()];
            int i = 0;

            while (st.hasMoreTokens()) {
               String s = st.nextToken();
               a[i] = Integer.parseInt(s);
            }

            return a;
         }
      } catch (Throwable var6) {
         var6.printStackTrace();
         return new int[0];
      }
   }

   public static boolean isInheritedType(int scptIndex) {
      for (int i = 0; i < inhTypes.length; i++) {
         if (inhTypes[i] == scptIndex) {
            return true;
         }
      }

      return false;
   }

   public static BLonComponent findScptForNv(BLonDevice dev, int nvSelect, int scptIndex) {
      BNetworkConfig[] ncis = dev.getNetworkConfigs();

      for (int i = 0; i < ncis.length; i++) {
         BNetworkConfig nci = ncis[i];
         BNcProps ncProps = nci.getNcProps();
         if (ncProps.getScope().equals(BLonConfigScope.nv) && doesSelectContain(nvSelect, ncProps.getSelect()) && ncProps.getConfigIndex() == scptIndex) {
            return nci;
         }
      }

      BConfigParameter[] cps = dev.getConfigParameters();

      for (int ix = 0; ix < cps.length; ix++) {
         BConfigParameter cp = cps[ix];
         BConfigProps ncProps = cp.getConfigProps();
         if (ncProps.getScope().equals(BLonConfigScope.nv) && doesSelectContain(nvSelect, ncProps.getSelect()) && ncProps.getConfigIndex() == scptIndex) {
            return cp;
         }
      }

      return null;
   }

   public static BLonComponent[] findInheritedConfigsForNv(BLonDevice dev, int nvIndex) {
      Array<BLonComponent> a = new Array(BLonComponent.class);
      BNetworkConfig[] ncis = dev.getNetworkConfigs();

      for (int i = 0; i < ncis.length; i++) {
         BNetworkConfig nci = ncis[i];
         BNcProps ncProps = nci.getNcProps();
         if (ncProps.getScope().equals(BLonConfigScope.nv)
            && doesSelectContain(nvIndex, ncProps.getSelect())
            && (isInheritedType(ncProps.getConfigIndex()) || nci.getPropertyInParent().getFacets().getb("inherited", false))) {
            a.add(nci);
         }
      }

      int objectIndex = dev.getNetworkVariable(nvIndex).getNvProps().getObjectIndex();
      BConfigParameter[] cps = dev.getConfigParameters();

      for (int ix = 0; ix < cps.length; ix++) {
         BConfigParameter cp = cps[ix];
         BConfigProps ncProps = cp.getConfigProps();
         if (ncProps.getScope().equals(BLonConfigScope.nv)) {
            if (doesSelectContain(nvIndex, ncProps.getSelect())
               && (isInheritedType(ncProps.getConfigIndex()) || cp.getPropertyInParent().getFacets().getb("inherited", false))) {
               a.add(cp);
            }
         } else if (ncProps.getScope().equals(BLonConfigScope.object)) {
            BFacets f = cp.getPropertyInParent().getFacets();
            if (doesSelectContain(objectIndex, ncProps.getSelect()) && f.getb("inherited", false) && f.geti("sourceNv", -1) == nvIndex) {
               a.add(cp);
            }
         }
      }

      return (BLonComponent[])a.trim();
   }
}
