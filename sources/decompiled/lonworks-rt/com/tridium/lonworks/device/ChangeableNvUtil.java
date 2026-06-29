package com.tridium.lonworks.device;

import javax.baja.lonworks.BConfigParameter;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BConfigProps;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.lonworks.util.ScptUtil;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.sys.BComponent;

public class ChangeableNvUtil {
   public static void uploadChangeables(BINvContainer nvCntr) {
      BLonDevice dev = nvCntr.getLonDevice();
      BComponent c = nvCntr.asComponent();
      BNetworkConfig[] ncis = (BNetworkConfig[])c.getChildren(BNetworkConfig.class);

      for (int i = 0; i < ncis.length; i++) {
         BNetworkConfig nci = ncis[i];
         BNcProps ncProps = nci.getNcProps();
         if (ncProps.getConfigIndex() == 254) {
            nci.doForceRead();
            int[] nvs = ScptUtil.decomposeSelect(ncProps.getSelect());

            for (int n = 0; n < nvs.length; n++) {
               updateNvType(dev, nvs[n], nci);
            }
         }
      }

      BConfigParameter[] cps = (BConfigParameter[])c.getChildren(BConfigParameter.class);

      for (int ix = 0; ix < cps.length; ix++) {
         BConfigParameter cp = cps[ix];
         BConfigProps cfgProps = cp.getConfigProps();
         if (cfgProps.getConfigIndex() == 254) {
            cp.doForceRead();
            int[] nvs = ScptUtil.decomposeSelect(cfgProps.getSelect());

            for (int n = 0; n < nvs.length; n++) {
               updateNvType(dev, nvs[n], cp);
            }
         }
      }
   }

   private static void updateNvType(BLonDevice dev, int nvIndex, BLonData nvType) {
      int typeScope = (int)((BLonPrimitive)nvType.get("typeScope")).getDataAsDouble();
      int typeIndex = (int)((BLonPrimitive)nvType.get("typeIndex")).getDataAsDouble();
      if (typeIndex == 0) {
         System.out.println("no type specified for " + dev.getDisplayName(null) + ":" + dev.getNetworkVariable(nvIndex).getDisplayName(null));
      } else {
         BLonData dat = SnvtUtil.getLonData(typeIndex);
         if (dat == null) {
            System.out.println("type not found for " + dev.getDisplayName(null) + ":" + dev.getNetworkVariable(nvIndex).getDisplayName(null));
         } else {
            BNetworkVariable nv = dev.getNetworkVariable(nvIndex);
            if (nv != null) {
               updateLonData(nv, dat);
               DynaDev.setNonCritical(nv);
               int snvtType = typeScope == 0 ? typeIndex : 0;
               nv.getNvProps().setSnvtType(snvtType);
               BLonComponent[] lcs = ScptUtil.findInheritedConfigsForNv(dev, nvIndex);

               for (int i = 0; i < lcs.length; i++) {
                  updateLonData(lcs[i], dat);
               }
            }
         }
      }
   }

   private static void updateLonData(BLonComponent lc, BLonData data) {
      lc.setData(data);
   }
}
