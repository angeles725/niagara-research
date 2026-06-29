package com.tridium.bacnet.util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.driver.point.BProxyExt;
import javax.baja.status.BStatusValue;

@Deprecated
public class BacnetProxyExtUtil {
   @Deprecated
   public static BStatusValue getLastWorking(BBacnetProxyExt proxyExt) throws PrivilegedActionException, IllegalArgumentException, IllegalAccessException {
      Object o = lastWorking().get(proxyExt);
      return o instanceof BStatusValue ? (BStatusValue)o : null;
   }

   @Deprecated
   public static void setLastWorking(BBacnetProxyExt proxyExt, BStatusValue lastWorking) throws PrivilegedActionException, IllegalArgumentException, IllegalAccessException {
      lastWorking().set(proxyExt, lastWorking);
   }

   private static Field lastWorking() throws PrivilegedActionException {
      return AccessController.doPrivileged((PrivilegedExceptionAction<Field>)(() -> {
         Field f = BProxyExt.class.getDeclaredField("lastWorking");
         f.setAccessible(true);
         return f;
      }));
   }
}
