package com.tridium.nre.jetty;

import java.security.AccessController;
import org.eclipse.jetty.server.HttpConfiguration;

public final class JettyUtil {
   private JettyUtil() {
   }

   public static HttpConfiguration makeBasicHttpConfiguration() {
      HttpConfiguration httpConfig = AccessController.doPrivileged(HttpConfiguration::new);
      httpConfig.setSendXPoweredBy(false);
      httpConfig.setSendDateHeader(false);
      httpConfig.setSendServerVersion(false);
      return httpConfig;
   }
}
