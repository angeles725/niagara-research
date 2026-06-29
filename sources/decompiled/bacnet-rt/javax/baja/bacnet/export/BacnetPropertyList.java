package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;

public final class BacnetPropertyList {
   private static final int[] requiredProps = new int[]{77, 79, 75};

   private BacnetPropertyList() {
   }

   public static int[] makePropertyList(int[]... propertyLists) {
      int totalSize = 0;

      for (int[] propList : propertyLists) {
         totalSize += propList.length;
      }

      int i = 0;
      int[] propertyList = new int[totalSize];

      for (int[] propList : propertyLists) {
         for (int prop : propList) {
            propertyList[i++] = prop;
         }
      }

      return propertyList;
   }

   public static byte[] readAll(int[] props) {
      AsnOutputStream out = new AsnOutputStream();

      for (int propId : props) {
         if (shouldInclude(propId)) {
            out.writeEnumerated(propId);
         }
      }

      return out.toByteArray();
   }

   public static int size(int[] props) {
      int i = 0;

      for (int propId : props) {
         if (shouldInclude(propId)) {
            i++;
         }
      }

      return i;
   }

   public static int read(int ndx, int[] props) {
      int[] cleanProps = new int[props.length - requiredProps.length];
      int i = 0;

      for (int propId : props) {
         if (shouldInclude(propId)) {
            cleanProps[i++] = propId;
         }
      }

      return ndx >= 1 && ndx <= cleanProps.length + 1 ? cleanProps[ndx - 1] : -1;
   }

   public static NReadPropertyResult getInvalidIdx(int propId, int ndx) {
      return new NReadPropertyResult(propId, ndx, new NErrorType(2, 42));
   }

   public static boolean shouldInclude(int propId) {
      for (int i = 0; i < requiredProps.length; i++) {
         if (propId == requiredProps[i]) {
            return false;
         }
      }

      return true;
   }
}
