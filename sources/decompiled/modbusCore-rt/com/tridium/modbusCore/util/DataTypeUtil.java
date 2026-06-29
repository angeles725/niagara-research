package com.tridium.modbusCore.util;

import com.tridium.modbusCore.enums.BDataTypeEnum;

public class DataTypeUtil {
   public static int getRegisterCount(BDataTypeEnum dataType) {
      if (dataType.equals(BDataTypeEnum.doubleType)
         || dataType.equals(BDataTypeEnum.signed64BitLongType)
         || dataType.equals(BDataTypeEnum.unsigned64BitLongType)) {
         return 4;
      } else {
         return !is16BitInteger(dataType) ? 2 : 1;
      }
   }

   public static boolean is16BitInteger(BDataTypeEnum dataType) {
      return dataType.equals(BDataTypeEnum.integerType) || dataType.equals(BDataTypeEnum.signedInteger);
   }

   public static boolean isDouble(BDataTypeEnum dataType) {
      return dataType.equals(BDataTypeEnum.doubleType);
   }

   public static boolean is64BitLong(BDataTypeEnum dataType) {
      return dataType.equals(BDataTypeEnum.signed64BitLongType) || dataType.equals(BDataTypeEnum.unsigned64BitLongType);
   }

   public static boolean is32BitLong(BDataTypeEnum dataType) {
      return dataType.equals(BDataTypeEnum.longType) || dataType.equals(BDataTypeEnum.unsignedLong);
   }

   public static boolean isFloat(BDataTypeEnum dataType) {
      return dataType.equals(BDataTypeEnum.floatType);
   }

   public static boolean isSigned(BDataTypeEnum dataType) {
      return dataType.equals(BDataTypeEnum.signed64BitLongType) || dataType.equals(BDataTypeEnum.signedInteger) || dataType.equals(BDataTypeEnum.longType);
   }
}
