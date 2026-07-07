package com.tridium.template.api.impl;

public final class NiagaraTemplateUtils {
   public static String replaceNull(String s) {
      return s == null ? "" : s;
   }

   public static <E extends Enum<E>> E replaceNull(Class<E> enumClass, E value) {
      return value == null ? enumClass.getEnumConstants()[0] : value;
   }

   private NiagaraTemplateUtils() {
   }
}
