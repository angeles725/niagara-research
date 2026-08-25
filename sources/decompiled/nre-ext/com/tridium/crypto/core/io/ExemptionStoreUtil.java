package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.NHostExemption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ExemptionStoreUtil {
   private final ICoreExemptionStore exemptionStore;
   private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^([^:]+|\\[.+\\]):([0-9]+)$");
   private static final Logger LOGGER = Logger.getLogger("crypto");

   public ExemptionStoreUtil(ICoreExemptionStore exemptionStore) {
      if (exemptionStore == null) {
         throw new NullPointerException("exemptionStore must not be null");
      }

      this.exemptionStore = exemptionStore;
   }

   public final Collection<NHostExemption> findExemptionsForHostname(String hostname, boolean transientOnly) {
      Collection<NHostExemption> matchingExemptions = new ArrayList<>();

      try {
         if (hostname != null) {
            Enumeration<NHostExemption> exemptions = this.exemptionStore.exemptions();

            while (exemptions.hasMoreElements()) {
               NHostExemption exemption = exemptions.nextElement();
               if (!transientOnly || exemption.isTransient()) {
                  int portNumStart = exemption.getHost().lastIndexOf(58);
                  if (portNumStart == -1) {
                     LOGGER.warning(() -> String.format("Unrecognized host format for %s", exemption.getHost()));
                  } else if (hostname.equals(exemption.getHost().substring(0, portNumStart))) {
                     matchingExemptions.add(exemption);
                     LOGGER.fine(() -> String.format("Found matching transient exemption for host %s", exemption.getHost()));
                  }
               }
            }
         }
      } catch (Exception e) {
         LOGGER.log(Level.SEVERE, e, () -> String.format("Error finding exemptions for hostname %s", hostname));
      }

      return matchingExemptions;
   }

   public final void deleteExemptionsForHostname(String hostname, boolean transientOnly) {
      Collection<NHostExemption> exemptions = this.findExemptionsForHostname(hostname, transientOnly);
      if (exemptions != null) {
         for (NHostExemption exemption : exemptions) {
            try {
               LOGGER.info(() -> String.format("Deleting transient exemption for host %s", exemption.getHost()));
               this.exemptionStore.deleteExemption(exemption.getHost());
            } catch (Exception e) {
               LOGGER.log(Level.SEVERE, e, () -> String.format("Error deleting exemption for host %s", exemption.getHost()));
            }
         }
      }
   }
}
