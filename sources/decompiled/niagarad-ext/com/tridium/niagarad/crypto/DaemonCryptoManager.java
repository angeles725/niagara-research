package com.tridium.niagarad.crypto;

import com.tridium.nre.platform.PlatformUtil;
import java.security.AccessController;
import java.security.KeyStore;
import javax.baja.nre.security.ServerTlsParameters;
import javax.baja.nre.security.TlsCipherSuiteGroup;
import javax.net.ServerSocketFactory;

public abstract class DaemonCryptoManager {
   public static final String SSL_PORT = "sslPort";
   public static final String DEFAULT_SSL_PORT = "5011";
   public static final String SSL_ENABLED = "sslEnabled";
   public static final String DEFAULT_SSL_ENABLED = "true";
   public static final boolean REQUIRE_SECURE_PLATFORM = AccessController.doPrivileged(() -> PlatformUtil.getPlatformProvider().requireSecurePlatform());
   public static final String SSL_ONLY = "sslOnly";
   public static final String DEFAULT_SSL_ONLY = REQUIRE_SECURE_PLATFORM ? "true" : "false";
   public static final String SSL_ENABLED_READONLY = "sslEnabledStateReadonly";
   public static final String SSL_ENABLED_READONLY_VALUE = REQUIRE_SECURE_PLATFORM ? "true" : "false";
   public static final String SSL_KEY_ALIAS = "keyAlias";
   public static final String SSL_KEY_PASSPHRASE = "keyPassphrase";
   public static final String DEFAULT_SSL_KEY_ALIAS = "default";
   public static final String LEGACY_SSL_KEY_ALIAS = "tridium";
   public static final String SSL_ALG_TYPE = "sslAlgType";
   public static final String DEFAULT_SSL_ALG_TYPE = "tlsv1_3";
   public static final String FIPS_MODE = "fipsMode";
   public static final String DEFAULT_FIPS_SSL_ALG_TYPE = "tlsv1_3";
   public static final String USE_EXTENDED_MASTER_SECRET = "tlsUseExtendedMasterSecret";
   public static final String REQUIRE_STRONG_CIPHER_SUITES = "requireStrongCipherSuites";
   public static final String TLS_CIPHER_SUITE_GROUP = "tlsCipherSuiteGroup";
   public static final String DEFAULT_TLS_CIPHER_SUITE_GROUP = TlsCipherSuiteGroup.recommended.name();
   private static DaemonCryptoManager instance = null;
   private static final Object mutex = new Object();

   public static DaemonCryptoManager getInstance() throws Exception {
      synchronized (mutex) {
         if (instance == null) {
            instance = new NDaemonCryptoManager();
         }

         return instance;
      }
   }

   public static boolean isTlsAlgFipsApproved(String algType) {
      return "tlsv1_2".equalsIgnoreCase(algType) || "tlsv1_3".equalsIgnoreCase(algType);
   }

   public abstract KeyStore getKeyStore() throws Exception;

   public abstract ServerSocketFactory getServerSocketFactory(String var1, ServerTlsParameters var2) throws Exception;
}
