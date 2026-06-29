package com.tridium.modbusCore.util;

import com.tridium.modbusCore.enums.BDataByteOrder64BitEnum;
import com.tridium.modbusCore.enums.BDataByteOrderEnum;

public class ByteConverterUtil {
   private static final float MAX_2_BYTE_INT_SIGNED = 32767.0F;
   private static final float MIN_2_BYTE_INT_SIGNED = -32768.0F;
   private static final float MAX_2_BYTE_INT_UNSIGNED = 65535.0F;
   private static final float MIN_2_BYTE_INT_UNSIGNED = 0.0F;
   private static final double MAX_4_BYTE_LONG_SIGNED = 2.147483647E9;
   private static final double MIN_4_BYTE_LONG_SIGNED = -2.1474836E9F;
   private static final double MAX_4_BYTE_LONG_UNSIGNED = 4.294967295E9;
   private static final double MIN_4_BYTE_LONG_UNSIGNED = 0.0;
   private static final double MAX_CONTIGUOUS_DOUBLE = 9.007199E15F;
   private static final double MIN_8_BYTE_LONG_SIGNED = -9.007199E15F;
   private static final double MAX_8_BYTE_LONG_SIGNED = 9.007199E15F;
   private static final double MIN_8_BYTE_LONG_UNSIGNED = 0.0;
   private static final double MAX_8_BYTE_LONG_UNSIGNED = 9.007199E15F;

   public static byte[] to4ByteLongArray(double value, BDataByteOrderEnum byteOrder, boolean isSigned) {
      byte[] ba = new byte[4];
      if (value >= 0.0) {
         value += 0.5;
      } else {
         value -= 0.5;
      }

      double minValue = isSigned ? -2.1474836E9F : 0.0;
      double maxValue = isSigned ? 2.147483647E9 : 4.294967295E9;
      if (value > maxValue) {
         value = maxValue;
      } else if (value < minValue) {
         value = minValue;
      }

      long longValue = (long)value;
      if (byteOrder == BDataByteOrderEnum.order3210) {
         ba[0] = (byte)((longValue & -16777216L) >> 24);
         ba[1] = (byte)((longValue & 16711680L) >> 16);
         ba[2] = (byte)((longValue & 65280L) >> 8);
         ba[3] = (byte)(longValue & 255L);
      } else if (byteOrder == BDataByteOrderEnum.order0123) {
         ba[0] = (byte)(longValue & 255L);
         ba[1] = (byte)((longValue & 65280L) >> 8);
         ba[2] = (byte)((longValue & 16711680L) >> 16);
         ba[3] = (byte)((longValue & -16777216L) >> 24);
      } else {
         ba[0] = (byte)((longValue & 65280L) >> 8);
         ba[1] = (byte)(longValue & 255L);
         ba[2] = (byte)((longValue & -16777216L) >> 24);
         ba[3] = (byte)((longValue & 16711680L) >> 16);
      }

      return ba;
   }

   public static byte[] to4ByteFloatArray(float value, BDataByteOrderEnum byteOrder) {
      byte[] ba = new byte[4];
      int floatBits = Float.floatToIntBits(value);
      if (byteOrder == BDataByteOrderEnum.order3210) {
         ba[0] = (byte)((floatBits & 0xFF000000) >> 24);
         ba[1] = (byte)((floatBits & 0xFF0000) >> 16);
         ba[2] = (byte)((floatBits & 0xFF00) >> 8);
         ba[3] = (byte)(floatBits & 0xFF);
      } else if (byteOrder == BDataByteOrderEnum.order0123) {
         ba[0] = (byte)(floatBits & 0xFF);
         ba[1] = (byte)((floatBits & 0xFF00) >> 8);
         ba[2] = (byte)((floatBits & 0xFF0000) >> 16);
         ba[3] = (byte)((floatBits & 0xFF000000) >> 24);
      } else {
         ba[0] = (byte)((floatBits & 0xFF00) >> 8);
         ba[1] = (byte)(floatBits & 0xFF);
         ba[2] = (byte)((floatBits & 0xFF000000) >> 24);
         ba[3] = (byte)((floatBits & 0xFF0000) >> 16);
      }

      return ba;
   }

   public static byte[] to2ByteIntArray(double value, boolean isSigned) {
      byte[] ba = new byte[2];
      if (value >= 0.0) {
         value += 0.5;
      } else {
         value -= 0.5;
      }

      double minValue = isSigned ? -32768.0 : 0.0;
      double maxValue = isSigned ? 32767.0 : 65535.0;
      if (value > maxValue) {
         value = maxValue;
      } else if (value < minValue) {
         value = minValue;
      }

      int intValue = (int)value;
      ba[0] = (byte)((intValue & 0xFF00) >> 8);
      ba[1] = (byte)(intValue & 0xFF);
      return ba;
   }

   public static byte[] to8ByteLongArray(double value, BDataByteOrder64BitEnum byteOrder, boolean isSigned) {
      double minValue = isSigned ? -9.007199E15F : 0.0;
      double maxValue = isSigned ? 9.007199E15F : 9.007199E15F;
      if (value >= 0.0) {
         value += 0.5;
      } else {
         value -= 0.5;
      }

      if (value > maxValue) {
         value = maxValue;
      } else if (value < minValue) {
         value = minValue;
      }

      long longValue = (long)value;
      return to8ByteArrangedArray(longValue, byteOrder);
   }

   public static byte[] to8ByteDoubleArray(double value, BDataByteOrder64BitEnum byteOrder) {
      long bits = Double.doubleToLongBits(value);
      return to8ByteArrangedArray(bits, byteOrder);
   }

   private static byte[] to8ByteArrangedArray(long bits, BDataByteOrder64BitEnum byteOrder) {
      byte[] ba = new byte[8];
      if (byteOrder == BDataByteOrder64BitEnum.order76543210) {
         ba[0] = (byte)((bits & -72057594037927936L) >> 56);
         ba[1] = (byte)((bits & 71776119061217280L) >> 48);
         ba[2] = (byte)((bits & 280375465082880L) >> 40);
         ba[3] = (byte)((bits & 1095216660480L) >> 32);
         ba[4] = (byte)((bits & 4278190080L) >> 24);
         ba[5] = (byte)((bits & 16711680L) >> 16);
         ba[6] = (byte)((bits & 65280L) >> 8);
         ba[7] = (byte)(bits & 255L);
      } else if (byteOrder == BDataByteOrder64BitEnum.order67452301) {
         ba[0] = (byte)((bits & 71776119061217280L) >> 48);
         ba[1] = (byte)((bits & -72057594037927936L) >> 56);
         ba[2] = (byte)((bits & 1095216660480L) >> 32);
         ba[3] = (byte)((bits & 280375465082880L) >> 40);
         ba[4] = (byte)((bits & 16711680L) >> 16);
         ba[5] = (byte)((bits & 4278190080L) >> 24);
         ba[6] = (byte)(bits & 255L);
         ba[7] = (byte)((bits & 65280L) >> 8);
      } else if (byteOrder == BDataByteOrder64BitEnum.order54761032) {
         ba[0] = (byte)((bits & 280375465082880L) >> 40);
         ba[1] = (byte)((bits & 1095216660480L) >> 32);
         ba[2] = (byte)((bits & -72057594037927936L) >> 56);
         ba[3] = (byte)((bits & 71776119061217280L) >> 48);
         ba[4] = (byte)((bits & 65280L) >> 8);
         ba[5] = (byte)(bits & 255L);
         ba[6] = (byte)((bits & 4278190080L) >> 24);
         ba[7] = (byte)((bits & 16711680L) >> 16);
      } else if (byteOrder == BDataByteOrder64BitEnum.order45670123) {
         ba[0] = (byte)((bits & 1095216660480L) >> 32);
         ba[1] = (byte)((bits & 280375465082880L) >> 40);
         ba[2] = (byte)((bits & 71776119061217280L) >> 48);
         ba[3] = (byte)((bits & -72057594037927936L) >> 56);
         ba[4] = (byte)(bits & 255L);
         ba[5] = (byte)((bits & 65280L) >> 8);
         ba[6] = (byte)((bits & 16711680L) >> 16);
         ba[7] = (byte)((bits & 4278190080L) >> 24);
      } else if (byteOrder == BDataByteOrder64BitEnum.order01234567) {
         ba[0] = (byte)(bits & 255L);
         ba[1] = (byte)((bits & 65280L) >> 8);
         ba[2] = (byte)((bits & 16711680L) >> 16);
         ba[3] = (byte)((bits & 4278190080L) >> 24);
         ba[4] = (byte)((bits & 1095216660480L) >> 32);
         ba[5] = (byte)((bits & 280375465082880L) >> 40);
         ba[6] = (byte)((bits & 71776119061217280L) >> 48);
         ba[7] = (byte)((bits & -72057594037927936L) >> 56);
      } else if (byteOrder == BDataByteOrder64BitEnum.order10325476) {
         ba[0] = (byte)((bits & 65280L) >> 8);
         ba[1] = (byte)(bits & 255L);
         ba[2] = (byte)((bits & 4278190080L) >> 24);
         ba[3] = (byte)((bits & 16711680L) >> 16);
         ba[4] = (byte)((bits & 280375465082880L) >> 40);
         ba[5] = (byte)((bits & 1095216660480L) >> 32);
         ba[6] = (byte)((bits & -72057594037927936L) >> 56);
         ba[7] = (byte)((bits & 71776119061217280L) >> 48);
      } else if (byteOrder == BDataByteOrder64BitEnum.order23016745) {
         ba[0] = (byte)((bits & 16711680L) >> 16);
         ba[1] = (byte)((bits & 4278190080L) >> 24);
         ba[2] = (byte)(bits & 255L);
         ba[3] = (byte)((bits & 65280L) >> 8);
         ba[4] = (byte)((bits & 71776119061217280L) >> 48);
         ba[5] = (byte)((bits & -72057594037927936L) >> 56);
         ba[6] = (byte)((bits & 1095216660480L) >> 32);
         ba[7] = (byte)((bits & 280375465082880L) >> 40);
      } else if (byteOrder == BDataByteOrder64BitEnum.order32107654) {
         ba[0] = (byte)((bits & 4278190080L) >> 24);
         ba[1] = (byte)((bits & 16711680L) >> 16);
         ba[2] = (byte)((bits & 65280L) >> 8);
         ba[3] = (byte)(bits & 255L);
         ba[6] = (byte)((bits & 280375465082880L) >> 40);
         ba[4] = (byte)((bits & -72057594037927936L) >> 56);
         ba[5] = (byte)((bits & 71776119061217280L) >> 48);
         ba[7] = (byte)((bits & 1095216660480L) >> 32);
      }

      return ba;
   }
}
