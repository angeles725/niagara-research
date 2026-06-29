package com.tridium.modbusCore;

import java.lang.reflect.Field;
import javax.baja.sys.BajaRuntimeException;

public class ModbusException extends BajaRuntimeException {
   public ModbusException(int code) {
      super(errorCodeToString(code));
   }

   public static String errorCodeToString(int code) {
      Field[] fa = ModbusErrorCodes.class.getDeclaredFields();

      for (int i = 0; i < fa.length; i++) {
         try {
            if (fa[i].getInt(null) == code) {
               return fa[i].getName();
            }
         } catch (Exception var4) {
         }
      }

      return Integer.toString(code);
   }
}
