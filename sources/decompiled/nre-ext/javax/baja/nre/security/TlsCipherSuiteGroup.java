package javax.baja.nre.security;

import com.tridium.crypto.core.io.CryptoSupport;
import java.util.logging.Logger;

public enum TlsCipherSuiteGroup {
   recommended,
   supported;

   private static final Logger LOG = Logger.getLogger("nre");

   public String[] getEnabledCipherSuites() {
      switch (this) {
         case recommended:
            return CryptoSupport.getRecommendedCipherSuites();
         case supported:
            return CryptoSupport.getSupportedCipherSuites();
         default:
            return null;
      }
   }

   public static TlsCipherSuiteGroup getEnum(String tag) {
      TlsCipherSuiteGroup result;
      try {
         result = valueOf(tag);
      } catch (Exception e) {
         LOG.warning("error resolving tls cipher suite group tag '" + tag + "', returning 'recommended'");
         result = recommended;
      }

      return result;
   }
}
