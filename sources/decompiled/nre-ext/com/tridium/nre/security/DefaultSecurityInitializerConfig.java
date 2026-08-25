package com.tridium.nre.security;

import java.io.File;

public final class DefaultSecurityInitializerConfig implements ISecurityInitializerConfig {
   private File secDir = null;
   private String kmName = null;

   public DefaultSecurityInitializerConfig() {
      this.secDir = new File(System.getProperty("niagara.user.home"), "security");
      this.kmName = ".km";
   }

   @Override
   public File getSecDir() {
      return this.secDir;
   }

   @Override
   public String getKmName() {
      return this.kmName;
   }
}
