package com.tridium.nre.jetty;

import java.util.Arrays;
import java.util.Objects;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public final class JettyThreadUtil {
   private static final int MINIMUM_SELECTORS_REQUIRED = 4;

   private JettyThreadUtil() {
   }

   public static int calcAcceptorThreads(int acceptorThreads, int cores) {
      return acceptorThreads < 0 ? defaultAcceptorCount(cores) : acceptorThreads;
   }

   public static int calcSelectorThreads(int selectorThreads, int cores) {
      int selectorThreadsNeeded = selectorThreads > 0 ? selectorThreads : defaultSelectorCount(cores);
      return Math.max(4, selectorThreadsNeeded);
   }

   public static int calcThreads(int acceptorThreads, int selectorThreads, int cores) {
      return calcAcceptorThreads(acceptorThreads, cores) + calcSelectorThreads(selectorThreads, cores);
   }

   public static int getBaseThreadCount(int cores) {
      return Math.min(8, cores);
   }

   public static int getThreadCount(Server jetty) {
      Objects.requireNonNull(jetty);
      return Arrays.stream(jetty.getConnectors()).mapToInt(c -> {
         int needed = 0;
         if (c instanceof AbstractConnector) {
            needed += ((AbstractConnector)c).getAcceptors();
         }

         if (c instanceof ServerConnector) {
            needed += ((ServerConnector)c).getSelectorManager().getSelectorCount();
         }

         return needed;
      }).sum();
   }

   private static int defaultAcceptorCount(int cores) {
      return Math.max(1, Math.min(4, cores / 8));
   }

   private static int defaultSelectorCount(int cores) {
      int maxThreads = 200;
      return Math.max(1, Math.min(cores / 2, maxThreads / 16));
   }
}
