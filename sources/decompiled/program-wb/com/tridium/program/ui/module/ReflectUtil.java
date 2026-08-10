package com.tridium.program.ui.module;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;

public class ReflectUtil {
   public static final int OK = 0;
   public static final int WARN = 1;
   public static final int ERR = 2;
   Map<String, Set<Method>> methodMap = new HashMap<>();

   protected ReflectUtil() {
      Method[] methods = BComponent.class.getMethods();

      for (Method method : methods) {
         String name = method.getName();
         if (this.isAccessible(method)) {
            if (this.methodMap.get(name) == null) {
               this.methodMap.put(name, new HashSet<>());
            }

            Set<Method> set = this.methodMap.get(name);
            set.add(method);
         }
      }
   }

   public static ReflectUtil getInstance() {
      return ReflectUtil.ReflectUtilHolder.INSTANCE;
   }

   public int getOverrideStatus(Method m) {
      return !this.isAccessible(m) ? 0 : this.getOverrideStatus(m.getName(), m.getParameterTypes());
   }

   public int getPropertyStatus(Property prop) {
      ComponentWriter.CompProperty cp = new ComponentWriter.CompProperty(prop);
      if (this.getOverrideStatus(cp.getter(), new Class[0]) != 0) {
         return 2;
      } else {
         return this.getOverrideStatus(cp.setter(), cp.setParams) != 0 ? 2 : 0;
      }
   }

   public int getOverrideStatus(String mName, Class<?>[] mParams) {
      Set<Method> set = this.methodMap.get(mName);
      if (set == null) {
         return 0;
      } else {
         Method[] compMethods = set.toArray(new Method[0]);

         for (Method compMethod : compMethods) {
            if (this.isSameParameters(mParams, compMethod.getParameterTypes())) {
               return Modifier.isFinal(compMethod.getModifiers()) ? 2 : 1;
            }
         }

         return 0;
      }
   }

   public Class<?> getDeclaringClass(Method m) {
      Class<?>[] params = m.getParameterTypes();
      Set<Method> set = this.methodMap.get(m.getName());
      Method[] methods = set.toArray(new Method[0]);

      for (Method method : methods) {
         if (this.isSameParameters(params, method.getParameterTypes())) {
            return method.getDeclaringClass();
         }
      }

      return null;
   }

   private boolean isSameParameters(Class<?>[] a, Class<?>[] b) {
      if (a.length != b.length) {
         return false;
      } else {
         for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(b[i])) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isAccessible(Method m) {
      int mod = m.getModifiers();
      return !Modifier.isStatic(mod) && (Modifier.isPublic(mod) || Modifier.isProtected(mod));
   }

   private static class ReflectUtilHolder {
      private static final ReflectUtil INSTANCE = new ReflectUtil();
   }
}
