package com.tridium.crypto.core.io;

import com.tridium.nre.util.Version;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

public final class CryptoSupport {
   public static final String TLS_V1_TAG = "tlsv1";
   public static final String TLS_V1_1_TAG = "tlsv1_1";
   public static final String TLS_V1_2_TAG = "tlsv1_2";
   public static final String TLS_V1_3_TAG = "tlsv1_3";
   public static final String TLS = "TLS";
   public static final String TLS_V1 = "tlsv1";
   public static final String TLS_V1_1 = "tlsv1.1";
   public static final String TLS_V1_2 = "tlsv1.2";
   public static final String TLS_V1_3 = "tlsv1.3";
   public static final String DEFAULT = "tlsv1";
   public static final Set<String> VALID_TLS_TAGS = new HashSet<String>() {
      {
         this.add("tlsv1");
         this.add("tlsv1_1");
         this.add("tlsv1_2");
         this.add("tlsv1_3");
      }
   };
   public static final HashMap<String, String> TYPES = new HashMap<>();
   public static final HashMap<String, String[]> TYPE_LISTS = new HashMap<>();
   public static final HashMap<String, String[]> TYPE_EXCLUDE_LISTS = new HashMap<>();
   public static final HashMap<String, String> TYPE_TAG_TO_VERSION = new HashMap<>();
   private static final String[] SUPPORTED_CIPHER_SUITES;
   private static final String[] RECOMMENDED_CIPHER_SUITES;
   private static final Set<String> PATTERN_EXCLUDED_CIPHER_SUITES = new HashSet<>();
   private static final Set<String> CIPHER_SUITE_EXCLUDE_PATTERNS = getCipherSuiteExcludePatterns();
   public static final Version MIN_TLS_1_3_NIAGARA_VERSION = new Version("4.11.0.56");
   public static final Version MIN_CIPHER_SUITE_GROUP_NIAGARA_VERSION = new Version("4.4.73.25");
   public static final Version MIN_MASTER_SECRET_NIAGARA_VERSION = new Version("4.6.96.6");

   private CryptoSupport() {
   }

   public static int getEncryptionBits(String cipher) {
      if (cipher.contains("AES_128")) {
         return 128;
      } else if (cipher.contains("AES_256")) {
         return 256;
      } else {
         return cipher.contains("CHACHA20") ? 256 : -1;
      }
   }

   public static String getMessageAuthCipher(String cipher) {
      if (cipher.endsWith("_SHA")) {
         return "SHA1";
      } else if (cipher.endsWith("_SHA256")) {
         return "SHA256";
      } else if (cipher.endsWith("_SHA384")) {
         return "SHA384";
      } else {
         return !cipher.endsWith("_CCM") && !cipher.endsWith("_CCM_8") ? "<???>" : "SHA256";
      }
   }

   public static String getKeyExchangeCipher(String cipher) {
      if (cipher.startsWith("TLS_ECDH_RSA")) {
         return "ECDH_RSA";
      } else if (cipher.startsWith("TLS_ECDHE_RSA")) {
         return "ECDHE_RSA";
      } else if (cipher.startsWith("TLS_ECDH_ECDSA")) {
         return "ECDH_ECDSA";
      } else if (cipher.startsWith("TLS_ECDHE_ECDSA")) {
         return "ECDHE_ECDSA";
      } else if (cipher.startsWith("TLS_RSA")) {
         return "RSA";
      } else if (cipher.startsWith("TLS_AES") || cipher.startsWith("TLS_CHACHA20")) {
         return "NULL";
      } else {
         return cipher.startsWith("TLS_DHE_RSA") ? "DHE_RSA" : "<???>";
      }
   }

   public static String getEncryptionCipher(String cipher) {
      if (cipher.contains("AES_128_GCM")) {
         return "AES_128_GCM";
      }

      if (cipher.contains("AES_256_GCM")) {
         return "AES_256_GCM";
      }

      if (cipher.contains("AES_128_CBC")) {
         return "AES_128_CBC";
      }

      if (cipher.contains("AES_256_CBC")) {
         return "AES_256_CBC";
      }

      if (cipher.contains("CHACHA20_POLY1305")) {
         return "CHACHA20_POLY1305";
      }

      if (cipher.contains("CCM")) {
         if (cipher.contains("AES_128")) {
            return "AES_128_CCM";
         }

         if (cipher.contains("AES_256")) {
            return "AES_256_CCM";
         }
      }

      return '<' + cipher + '>';
   }

   public static String[] getSupportedCipherSuites() {
      return Arrays.copyOf(SUPPORTED_CIPHER_SUITES, SUPPORTED_CIPHER_SUITES.length);
   }

   public static String[] getRecommendedCipherSuites() {
      return Arrays.copyOf(RECOMMENDED_CIPHER_SUITES, RECOMMENDED_CIPHER_SUITES.length);
   }

   public static Set<String> getExcludedCipherSuites() {
      return new HashSet<>(PATTERN_EXCLUDED_CIPHER_SUITES);
   }

   private static boolean excludeCipherSuite(String cipherSuite) {
      for (String excludePattern : CIPHER_SUITE_EXCLUDE_PATTERNS) {
         if (!excludePattern.isEmpty() && cipherSuite.contains(excludePattern)) {
            PATTERN_EXCLUDED_CIPHER_SUITES.add(cipherSuite);
            return true;
         }
      }

      return false;
   }

   private static Set<String> getCipherSuiteExcludePatterns() {
      String cipherSuiteExcludePatternsProp = AccessController.doPrivileged(() -> System.getProperty("cipherSuite.exclude.patterns", ""));
      String[] propertyPatterns = cipherSuiteExcludePatternsProp.split(",");
      Set<String> cipherSuiteExcludePatterns = new HashSet<>();

      for (String propertyPattern : propertyPatterns) {
         String pattern = propertyPattern.trim();
         if (!pattern.isEmpty()) {
            cipherSuiteExcludePatterns.add(pattern);
         }
      }

      return cipherSuiteExcludePatterns;
   }

   static {
      TYPES.put("tlsv1", "TLSv1");
      TYPES.put("tlsv1.1", "TLSv1.1");
      TYPES.put("tlsv1.2", "TLSv1.2");
      TYPES.put("tlsv1.3", "TLSv1.3");
      TYPES.put("tlsv1_1", "TLSv1.1");
      TYPES.put("tlsv1_2", "TLSv1.2");
      TYPES.put("tlsv1_3", "TLSv1.3");
      TYPE_LISTS.put("tlsv1", new String[]{"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"});
      TYPE_LISTS.put("tlsv1.1", new String[]{"TLSv1.1", "TLSv1.2", "TLSv1.3"});
      TYPE_LISTS.put("tlsv1.2", new String[]{"TLSv1.2", "TLSv1.3"});
      TYPE_LISTS.put("tlsv1.3", new String[]{"TLSv1.3"});
      TYPE_LISTS.put("tlsv1_1", new String[]{"TLSv1.1", "TLSv1.2", "TLSv1.3"});
      TYPE_LISTS.put("tlsv1_2", new String[]{"TLSv1.2", "TLSv1.3"});
      TYPE_LISTS.put("tlsv1_3", new String[]{"TLSv1.3"});
      TYPE_EXCLUDE_LISTS.put("tlsv1", new String[]{"SSL", "SSLv2", "SSLv3"});
      TYPE_EXCLUDE_LISTS.put("tlsv1.1", new String[]{"SSL", "SSLv2", "SSLv3", "TLSv1"});
      TYPE_EXCLUDE_LISTS.put("tlsv1.2", new String[]{"SSL", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1"});
      TYPE_EXCLUDE_LISTS.put("tlsv1.3", new String[]{"SSL", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
      TYPE_EXCLUDE_LISTS.put("tlsv1_1", new String[]{"SSL", "SSLv2", "SSLv3", "TLSv1"});
      TYPE_EXCLUDE_LISTS.put("tlsv1_2", new String[]{"SSL", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1"});
      TYPE_EXCLUDE_LISTS.put("tlsv1_3", new String[]{"SSL", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
      TYPE_TAG_TO_VERSION.put("TLSv1", "tlsv1");
      TYPE_TAG_TO_VERSION.put("TLSv1.1", "tlsv1_1");
      TYPE_TAG_TO_VERSION.put("TLSv1.2", "tlsv1_2");
      TYPE_TAG_TO_VERSION.put("TLSv1.3", "tlsv1_3");
      String[] supportedCipherSuites = null;
      String[] recommendedCipherSuites = null;

      try {
         SSLContext context = SSLContext.getInstance("TLS");
         AccessController.doPrivileged(() -> {
            context.init(null, new TrustManager[0], null);
            return null;
         });
         SSLServerSocketFactory ssf = context.getServerSocketFactory();
         String[] supportedCiphers = ssf.getSupportedCipherSuites();
         List<String> supportedCipherSuitesList = new LinkedList<>(Arrays.asList(supportedCiphers));
         supportedCipherSuitesList.removeIf(
            entry -> entry.contains("DES")
               || entry.contains("NULL")
               || entry.contains("EXPORT")
               || entry.contains("RC4")
               || entry.contains("MD5")
               || entry.contains("SSL")
               || entry.contains("anon")
               || entry.contains("CAMELLIA")
               || entry.contains("ARIA")
               || entry.contains("_ECDH_")
               || entry.contains("DSS")
               || excludeCipherSuite(entry)
         );
         supportedCipherSuitesList.sort(new CryptoSupport.CipherSuiteComparator());
         supportedCipherSuites = supportedCipherSuitesList.toArray(new String[0]);
         List<String> recommendedCipherSuitesList = new LinkedList<>(supportedCipherSuitesList);
         recommendedCipherSuitesList.removeIf(
            entry -> !entry.startsWith("TLS_ECDHE")
                  && !entry.startsWith("TLS_AES")
                  && !entry.startsWith("TLS_DHE")
                  && !entry.startsWith("TLS_CHACHA20")
                  && !"TLS_EMPTY_RENEGOTIATION_INFO_SCSV".equals(entry)
               || entry.endsWith("SHA")
               || entry.contains("_CBC_")
         );
         recommendedCipherSuites = recommendedCipherSuitesList.toArray(new String[0]);
      } catch (Exception e) {
         System.err.println("SEVERE [" + new Date() + "][crypto] Failed to initialize crypto support values (" + e + ")");
         e.printStackTrace();
      }

      SUPPORTED_CIPHER_SUITES = supportedCipherSuites;
      RECOMMENDED_CIPHER_SUITES = recommendedCipherSuites;
   }

   private static final class CipherSuiteComparator implements Comparator<String> {
      private CipherSuiteComparator() {
      }

      public int compare(String suite1, String suite2) {
         return score(suite1) > score(suite2) ? -1 : 1;
      }

      public static int score(String suite) {
         int score = 0;
         if (!suite.endsWith("SCSV")) {
            if (suite.startsWith("TLS_AES_")) {
               score += 256;
            }

            if (suite.startsWith("TLS_CHACHA20_")) {
               score += 256;
            }

            if (suite.contains("_ECDHE_")) {
               score += 128;
            }

            if (suite.contains("_ECDSA_")) {
               score += 64;
            }

            if (suite.contains("_ECDH_")) {
               score += 32;
            }

            if (suite.contains("_GCM_")) {
               score += 16;
            }

            if (suite.endsWith("_SHA384")) {
               score += 8;
            }

            if (suite.endsWith("_SHA256")) {
               score += 4;
            }

            if (suite.contains("_AES_256_")) {
               score += 2;
            }

            score++;
         }

         return score;
      }
   }
}
