package com.tridium.fox.kerberos;

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

public class KerberosConfig extends Configuration {
   AppConfigurationEntry entry = null;
   String principal = null;
   boolean useTicketCache = false;
   boolean doNotPrompt = false;
   boolean isInitiator = true;
   boolean storeKey = false;

   public KerberosConfig(String principal) {
      this.principal = principal;
      this.init();
   }

   public KerberosConfig(boolean useTicketCache, boolean doNotPrompt) {
      this.useTicketCache = useTicketCache;
      this.doNotPrompt = doNotPrompt;
      this.init();
   }

   public void setUseTicketCache(boolean useTicketCache) {
      this.useTicketCache = useTicketCache;
      this.init();
   }

   public void setDoNotPrompt(boolean doNotPrompt) {
      this.doNotPrompt = doNotPrompt;
      this.init();
   }

   public void setIsInitiator(boolean isInitiator) {
      this.isInitiator = isInitiator;
      if (!isInitiator) {
         this.storeKey = true;
      }

      this.init();
   }

   private void init() {
      Map<String, String> options = new HashMap<>();
      if (this.principal != null) {
         options.put("principal", this.principal);
      }

      options.put("useTicketCache", Boolean.toString(this.useTicketCache));
      options.put("doNotPrompt", Boolean.toString(this.doNotPrompt));
      options.put("isInitiator", Boolean.toString(this.isInitiator));
      options.put("storeKey", Boolean.toString(this.storeKey));
      options.put("refreshKrb5Config", "true");
      this.entry = new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule", LoginModuleControlFlag.REQUIRED, options);
   }

   @Override
   public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
      return new AppConfigurationEntry[]{this.entry};
   }
}
