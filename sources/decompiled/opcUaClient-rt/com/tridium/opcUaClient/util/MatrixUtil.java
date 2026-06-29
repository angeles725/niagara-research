package com.tridium.opcUaClient.util;

import com.prosysopc.ua.stack.utils.NumericRange;
import java.util.logging.Logger;

public class MatrixUtil {
   public static final Logger logger = Logger.getLogger("opcUaClient.util");

   public static String indexesToString(long[] indexes) {
      if (indexes.length == 1) {
         return Long.toString(indexes[0]);
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append('[');

         for (int i = 0; i < indexes.length; i++) {
            sb.append(indexes[i]);
            if (i < indexes.length - 1) {
               sb.append(',');
            }
         }

         sb.append(']');
         return sb.toString();
      }
   }

   public static int[] stringToIndexes(String s) throws NumberFormatException {
      if (s.isEmpty()) {
         return new int[0];
      } else {
         int il = s.indexOf(91);
         int ir = s.indexOf(93);
         if (il >= 0 && ir > il) {
            s = s.substring(il + 1, ir);
         }

         String[] split = s.split(",");
         int[] dimensions = new int[split.length];

         for (int i = 0; i < split.length; i++) {
            try {
               dimensions[dimensions.length - 1 - i] = Integer.parseInt(split[i].trim());
            } catch (Exception var7) {
               throw new NumberFormatException(var7.getMessage());
            }
         }

         return dimensions;
      }
   }

   public static String makeAddName(String baseName, long[] indexes) {
      String s = indexesToString(indexes);
      String rtnVal = baseName + s;
      rtnVal = rtnVal.replace('[', '_');
      rtnVal = rtnVal.replace(']', '_');
      return rtnVal.replace(',', '_');
   }

   public static boolean indexToNext(long[] curIndex, long[] dimensions) {
      for (int i = curIndex.length - 1; i >= 0; i--) {
         if (++curIndex[i] < dimensions[i]) {
            return false;
         }

         curIndex[i] = 0L;
      }

      return true;
   }

   public static NumericRange indexToNumericRange(String curIndex) {
      int[] selIndex = stringToIndexes(curIndex);
      switch (selIndex.length) {
         case 1:
            return new NumericRange(selIndex[0]);
         case 2:
            return new NumericRange(new int[][]{{selIndex[0]}, {selIndex[1]}});
         case 3:
            return new NumericRange(new int[][]{{selIndex[0]}, {selIndex[1]}, {selIndex[2]}});
         case 4:
            return new NumericRange(new int[][]{{selIndex[0]}, {selIndex[1]}, {selIndex[2]}, {selIndex[3]}});
         case 5:
            return new NumericRange(new int[][]{{selIndex[0]}, {selIndex[1]}, {selIndex[2]}, {selIndex[3]}, {selIndex[4]}});
         default:
            logger.severe("Array dimension size not supported: " + selIndex.length);
            return new NumericRange(new int[][]{selIndex});
      }
   }
}
