package com.tridium.opc.client.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "vtEmpty",
      ordinal = 0
   ), @Range(
      value = "vtNull",
      ordinal = 1
   ), @Range(
      value = "vtInt2",
      ordinal = 2
   ), @Range(
      value = "vtInt4",
      ordinal = 3
   ), @Range(
      value = "vtReal4",
      ordinal = 4
   ), @Range(
      value = "vtReal8",
      ordinal = 5
   ), @Range(
      value = "vtCurrency",
      ordinal = 6
   ), @Range(
      value = "vtDate",
      ordinal = 7
   ), @Range(
      value = "vtString",
      ordinal = 8
   ), @Range(
      value = "vtDispatch",
      ordinal = 9
   ), @Range(
      value = "vtError",
      ordinal = 10
   ), @Range(
      value = "vtBoolean",
      ordinal = 11
   ), @Range(
      value = "vtVariant",
      ordinal = 12
   ), @Range(
      value = "vtUnknown",
      ordinal = 13
   ), @Range(
      value = "vtDecimal",
      ordinal = 14
   ), @Range(
      value = "vtSignedByte",
      ordinal = 16
   ), @Range(
      value = "vtUnsignedByte",
      ordinal = 17
   ), @Range(
      value = "vtUint2",
      ordinal = 18
   ), @Range(
      value = "vtUint4",
      ordinal = 19
   ), @Range(
      value = "vtInt",
      ordinal = 22
   ), @Range(
      value = "vtUint",
      ordinal = 23
   ), @Range(
      value = "vtArrayOfShort",
      ordinal = 8194
   ), @Range(
      value = "vtArrayOfLong",
      ordinal = 8195
   ), @Range(
      value = "vtArrayOfReal4",
      ordinal = 8196
   ), @Range(
      value = "vtArrayOfReal8",
      ordinal = 8197
   ), @Range(
      value = "vtArrayOfString",
      ordinal = 8200
   ), @Range(
      value = "vtArrayOfBool",
      ordinal = 8203
   ), @Range(
      value = "vtArrayOfChar",
      ordinal = 8208
   ), @Range(
      value = "vtArrayOfByte",
      ordinal = 8209
   ), @Range(
      value = "vtArrayOfUShort",
      ordinal = 8210
   ), @Range(
      value = "vtArrayOfULong",
      ordinal = 8211
   )}
)
public final class BOpcDataType extends BFrozenEnum {
   public static final int VT_EMPTY = 0;
   public static final int VT_NULL = 1;
   public static final int VT_INT_2 = 2;
   public static final int VT_INT_4 = 3;
   public static final int VT_REAL_4 = 4;
   public static final int VT_REAL_8 = 5;
   public static final int VT_CURRENCY = 6;
   public static final int VT_DATE = 7;
   public static final int VT_STRING = 8;
   public static final int VT_DISPATCH = 9;
   public static final int VT_ERROR = 10;
   public static final int VT_BOOLEAN = 11;
   public static final int VT_VARIANT = 12;
   public static final int VT_UNKNOWN = 13;
   public static final int VT_DECIMAL = 14;
   public static final int VT_SIGNED_BYTE = 16;
   public static final int VT_UNSIGNED_BYTE = 17;
   public static final int VT_UINT_2 = 18;
   public static final int VT_UINT_4 = 19;
   public static final int VT_INT = 22;
   public static final int VT_UINT = 23;
   public static final int VT_ARRAY_OF_SHORT = 8194;
   public static final int VT_ARRAY_OF_LONG = 8195;
   public static final int VT_ARRAY_OF_REAL_4 = 8196;
   public static final int VT_ARRAY_OF_REAL_8 = 8197;
   public static final int VT_ARRAY_OF_STRING = 8200;
   public static final int VT_ARRAY_OF_BOOL = 8203;
   public static final int VT_ARRAY_OF_CHAR = 8208;
   public static final int VT_ARRAY_OF_BYTE = 8209;
   public static final int VT_ARRAY_OF_USHORT = 8210;
   public static final int VT_ARRAY_OF_ULONG = 8211;
   public static final BOpcDataType vtEmpty = new BOpcDataType(0);
   public static final BOpcDataType vtNull = new BOpcDataType(1);
   public static final BOpcDataType vtInt2 = new BOpcDataType(2);
   public static final BOpcDataType vtInt4 = new BOpcDataType(3);
   public static final BOpcDataType vtReal4 = new BOpcDataType(4);
   public static final BOpcDataType vtReal8 = new BOpcDataType(5);
   public static final BOpcDataType vtCurrency = new BOpcDataType(6);
   public static final BOpcDataType vtDate = new BOpcDataType(7);
   public static final BOpcDataType vtString = new BOpcDataType(8);
   public static final BOpcDataType vtDispatch = new BOpcDataType(9);
   public static final BOpcDataType vtError = new BOpcDataType(10);
   public static final BOpcDataType vtBoolean = new BOpcDataType(11);
   public static final BOpcDataType vtVariant = new BOpcDataType(12);
   public static final BOpcDataType vtUnknown = new BOpcDataType(13);
   public static final BOpcDataType vtDecimal = new BOpcDataType(14);
   public static final BOpcDataType vtSignedByte = new BOpcDataType(16);
   public static final BOpcDataType vtUnsignedByte = new BOpcDataType(17);
   public static final BOpcDataType vtUint2 = new BOpcDataType(18);
   public static final BOpcDataType vtUint4 = new BOpcDataType(19);
   public static final BOpcDataType vtInt = new BOpcDataType(22);
   public static final BOpcDataType vtUint = new BOpcDataType(23);
   public static final BOpcDataType vtArrayOfShort = new BOpcDataType(8194);
   public static final BOpcDataType vtArrayOfLong = new BOpcDataType(8195);
   public static final BOpcDataType vtArrayOfReal4 = new BOpcDataType(8196);
   public static final BOpcDataType vtArrayOfReal8 = new BOpcDataType(8197);
   public static final BOpcDataType vtArrayOfString = new BOpcDataType(8200);
   public static final BOpcDataType vtArrayOfBool = new BOpcDataType(8203);
   public static final BOpcDataType vtArrayOfChar = new BOpcDataType(8208);
   public static final BOpcDataType vtArrayOfByte = new BOpcDataType(8209);
   public static final BOpcDataType vtArrayOfUShort = new BOpcDataType(8210);
   public static final BOpcDataType vtArrayOfULong = new BOpcDataType(8211);
   public static final BOpcDataType DEFAULT = vtEmpty;
   public static final Type TYPE = Sys.loadType(BOpcDataType.class);

   public static BOpcDataType make(int ordinal) {
      return (BOpcDataType)vtEmpty.getRange().get(ordinal, false);
   }

   public static BOpcDataType make(String tag) {
      return (BOpcDataType)vtEmpty.getRange().get(tag);
   }

   private BOpcDataType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static double getMax(int dataType) {
      switch (dataType) {
         case 2:
            return 32767.0;
         case 3:
            return 2.147483647E9;
         case 4:
            return Float.MAX_VALUE;
         case 5:
            return Double.MAX_VALUE;
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 12:
         case 13:
         case 14:
         case 15:
         case 20:
         case 21:
         default:
            return 0.0;
         case 11:
            return 1.0;
         case 16:
            return 127.0;
         case 17:
            return 255.0;
         case 18:
            return 65535.0;
         case 19:
            return 4.294967295E9;
         case 22:
            return 2.147483647E9;
         case 23:
            return 4.294967295E9;
      }
   }

   public static double getMin(int dataType) {
      switch (dataType) {
         case 2:
            return -32768.0;
         case 3:
            return -2.1474836E9F;
         case 4:
            return -Float.MAX_VALUE;
         case 5:
            return -Double.MAX_VALUE;
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 12:
         case 13:
         case 14:
         case 15:
         case 20:
         case 21:
         default:
            return 0.0;
         case 11:
            return 0.0;
         case 16:
            return -128.0;
         case 17:
            return 0.0;
         case 18:
            return 0.0;
         case 19:
            return 0.0;
         case 22:
            return -2.1474836E9F;
         case 23:
            return 0.0;
      }
   }

   public boolean isBoolean() {
      return isBoolean(this.getOrdinal());
   }

   public static boolean isBoolean(int dataType) {
      return dataType == 11;
   }

   public boolean isNumeric() {
      return isNumeric(this.getOrdinal());
   }

   public static boolean isNumeric(int dataType) {
      switch (dataType) {
         case 2:
         case 3:
         case 4:
         case 5:
         case 16:
         case 17:
         case 18:
         case 19:
         case 22:
         case 23:
            return true;
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 20:
         case 21:
         default:
            return false;
      }
   }

   public boolean isString() {
      return isString(this.getOrdinal());
   }

   public static boolean isString(int dataType) {
      return dataType == 8
         || dataType == 8194
         || dataType == 8195
         || dataType == 8196
         || dataType == 8197
         || dataType == 8200
         || dataType == 8203
         || dataType == 8208
         || dataType == 8209
         || dataType == 8210
         || dataType == 8211;
   }

   public boolean isSupported() {
      return isSupported(this.getOrdinal());
   }

   public static boolean isSupported(int dataType) {
      return isBoolean(dataType) || isNumeric(dataType) || isString(dataType);
   }
}
