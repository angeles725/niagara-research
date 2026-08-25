package com.tridium.nre.security.policy;

import java.util.Objects;

public final class DefaultCodeSource implements ICodeSourceInfo {
   private final String url;
   private final String name;
   private final boolean signed;
   public static final ICodeSourceInfo ALL_CODE_SOURCE = new DefaultCodeSource("*", "*", false);

   public DefaultCodeSource(String url, String name, boolean signed) {
      this.url = url;
      this.name = name;
      this.signed = signed;
   }

   @Override
   public String getUrl() {
      return this.url;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public boolean isSigned() {
      return this.signed;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         DefaultCodeSource that = (DefaultCodeSource)o;
         return this.signed == that.signed && Objects.equals(this.url, that.url) && Objects.equals(this.name, that.name);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.url, this.name, this.signed);
   }
}
