package com.tridium.crypto.core.io;

import com.tridium.crypto.core.async.AsyncCertGeneration;
import com.tridium.crypto.core.async.CertGenerationEntry;
import com.tridium.crypto.core.async.IAsyncCertRequestEntry;
import com.tridium.crypto.core.async.ResetUserKeyStoreEntry;
import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.crypto.core.cert.CertificateChainValidator;
import com.tridium.crypto.core.cert.NCertificateParameters;
import com.tridium.crypto.core.cert.NKeyPairGenerator;
import com.tridium.crypto.core.cert.NPKCS10CertificationRequest;
import com.tridium.crypto.core.cert.NRsaKeyPairGenerator;
import com.tridium.crypto.core.cert.NX509CertificateBuilder;
import com.tridium.crypto.core.cert.NX509CertificateEntry;
import com.tridium.crypto.core.cert.ValidationException;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.KeyStorePermission;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityConstants;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.util.NamedThreadFactory;
import com.tridium.nre.util.Version;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PrivilegedActionException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;
import java.security.cert.CRLException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertificateException;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.ClientTlsParameters;
import javax.baja.nre.security.ServerTlsParameters;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.FileUtil;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory.Client;
import org.eclipse.jetty.util.ssl.SslContextFactory.Server;

public final class CoreCryptoManager implements ICoreCryptoManager, IRecurringCertManager {
   private boolean checkPaused = false;
   private static final Object PAUSE_STATE_MONITOR = new Object();
   private boolean savePaused = false;
   private static final Object SAVE_STATE_MONITOR = new Object();
   private static final Logger logger = Logger.getLogger("crypto");
   private static final String KEY_STORE = "keystore";
   private static final String SYSTEM_TRUST_STORE = "lib/security/cacerts";
   private static final String USER_TRUST_STORE = "cacerts";
   private static final String USER_UNTRUSTED_STORE = "untrusted";
   private static final String EXEMPTION_STORE_PATH = "exemptions.tes";
   private static final String CORE_KEY_STORE_KR = "com.tridium.crypto.io.CoreKeyStore";
   private static final String CORE_SYSTEM_TRUST_STORE_KR = "com.tridium.crypto.io.CoreKeyStore[SystemTrustStore]";
   private static final String CORE_USER_TRUST_STORE_KR = "com.tridium.crypto.io.CoreKeyStore[UserTrustStore]";
   private static final String CORE_USER_UNTRUSTED_STORE_KR = "com.tridium.crypto.io.CoreKeyStore[UserUntrustedStore]";
   private static final String CORE_EXEMPTION_STORE_KR = "com.tridium.crypto.io.CoreExemptionStore";
   private static final String USER_KEY_STORE_NAME = CryptoStoreId.USER_KEY_STORE.getValue();
   private static final String USER_TRUST_STORE_NAME = CryptoStoreId.USER_TRUST_STORE.getValue();
   private static final String USER_UNTRUSTED_STORE_NAME = CryptoStoreId.USER_UNTRUSTED_STORE.getValue();
   private static final String SYSTEM_TRUST_STORE_NAME = CryptoStoreId.SYSTEM_TRUST_STORE.getValue();
   private static final String USER_EXEMPTION_STORE_NAME = CryptoStoreId.USER_EXEMPTION_STORE.getValue();
   private static final String CACERTS_KEY = "jiUz!rHw2d8&i3kI";
   private static final String LEGACY_CACERTS_KEY = "changeit";
   private static final String KEY_VERIFICATION_HASH_ALGORITHM = "SHA1";
   private static final ConcurrentHashMap<File, CoreCryptoManager> INSTANCES = new ConcurrentHashMap<>();
   private static final ThreadFactory FILE_MONITOR_THREAD_FACTORY = new NamedThreadFactory("CoreCryptoManager.fileMonitor", true);
   private static final ThreadFactory RECURRING_SAVE_THREAD_FACTORY = new NamedThreadFactory("CoreCryptoManager.recurringCertsSave", true);
   private CoreTrustStore userTrustStore = null;
   private CoreTrustStore userUntrustedStore = null;
   private CoreTrustStore systemTrustStore = null;
   private CoreKeyStore keyStore = null;
   private CoreExemptionStore exemptionStore = null;
   private CertificateChainValidator certValidator = null;
   private AtomicBoolean userTrustStoreChanged = new AtomicBoolean(true);
   private CoreCryptoManager.RecurringCertManagerImpl recurringCertManagerImpl = null;
   private final AtomicReference<Optional<ScheduledExecutorService>> recurringCertsSaveExecutor = new AtomicReference<>(Optional.empty());
   private final ISecurityInfoProvider secInfo;
   private final AtomicReference<Optional<ICoreProviderInfo>> providerInfo = new AtomicReference<>(Optional.empty());
   private final AtomicReference<Optional<ScheduledExecutorService>> fileMonitor = new AtomicReference<>(Optional.empty());
   private final AsyncCertGeneration certGen = new AsyncCertGeneration();
   private final Map<String, TrustAnchorProvider> trustAnchorProviders = new HashMap<>();
   private final Map<String, TrustAnchorConsumer> trustAnchorConsumers = new HashMap<>();
   private final Map<String, CRLProvider> crlProviders = new HashMap<>();
   private SecureRandom secureRandom = new SecureRandom();
   private Version nreVersion;

   public static CoreCryptoManager get(ISecurityInfoProvider secInfo) {
      return INSTANCES.computeIfAbsent(secInfo.getSecurityDir(), dir -> {
         try {
            return new CoreCryptoManager(secInfo);
         } catch (RuntimeException rethrow) {
            throw rethrow;
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      });
   }

   public static CoreCryptoManager get() {
      return get(AccessController.doPrivileged(() -> SecurityInitializer.getInstance().getSecurityInfoProvider()));
   }

   private CoreCryptoManager(ISecurityInfoProvider secInfo) throws Exception {
      this.secInfo = secInfo;
      this.init();
   }

   @Override
   public synchronized ICoreKeyStore getKeyStore() {
      return this.keyStore;
   }

   @Override
   public synchronized ICoreTrustStore getUserTrustStore() {
      return this.userTrustStore;
   }

   @Override
   public synchronized ICoreTrustStore getUserUntrustedStore() {
      return this.userUntrustedStore;
   }

   @Override
   public synchronized ICoreTrustStore getSystemTrustStore() {
      return this.systemTrustStore;
   }

   @Override
   public synchronized ICoreExemptionStore getExemptionStore() {
      return this.exemptionStore;
   }

   @Override
   public int generateSelfSignedCert(NCertificateParameters certParams) throws Exception {
      NX509CertificateBuilder builder = CertUtils.createCertBuilderFromParameters(certParams);
      NKeyPairGenerator generator = new NRsaKeyPairGenerator(certParams.getKeySize());

      try (SecretChars password = certParams.getPassword() != null ? new SecretChars(certParams.getPassword().toCharArray(), true) : null) {
         return this.generateSelfSignedCert(builder, generator, password);
      }
   }

   @Override
   public int generateSelfSignedCert(NX509CertificateBuilder builder, NKeyPairGenerator generator, SecretChars password) throws Exception {
      CertGenerationEntry entry = new CertGenerationEntry(this.keyStore, builder, password, generator);
      return this.certGen.enqueue(entry);
   }

   @Override
   public int resetUserKeyStore() throws Exception {
      IAsyncCertRequestEntry entry = new ResetUserKeyStoreEntry(this.keyStore);
      return this.certGen.enqueue(entry);
   }

   @Override
   public int getCertGenerationStatus(int requestId) {
      return this.certGen.status(requestId);
   }

   @Override
   public NPKCS10CertificationRequest generateCSR(String alias, String passwd) throws Exception {
      char[] cpass = null;
      if (passwd != null) {
         cpass = passwd.toCharArray();
      }

      X509Certificate cert = this.keyStore.getCertificate(alias);
      PrivateKey key = (PrivateKey)this.keyStore.getKey(alias, cpass);
      NX509CertificateEntry entry = NX509CertificateEntry.make(null, new X509Certificate[]{cert}, key);
      return CertUtils.generateCSR(entry);
   }

   @Override
   public boolean canGenerateCertificate() {
      return true;
   }

   @Override
   public synchronized Version getCryptoManagerVersion() throws IOException {
      if (this.nreVersion == null) {
         try {
            this.nreVersion = AccessController.doPrivileged(
               () -> {
                  File nreJarPath = new File(
                     AccessController.doPrivileged(() -> System.getProperty("niagara.home"))
                        + File.separator
                        + "bin"
                        + File.separator
                        + "ext"
                        + File.separator
                        + "nre.jar"
                  );

                  try (JarFile nreJar = new JarFile(nreJarPath)) {
                     JarEntry moduleXmlEntry = nreJar.getJarEntry("META-INF/module.xml");

                     try (BufferedInputStream bufferedInputStream = new BufferedInputStream(nreJar.getInputStream(moduleXmlEntry))) {
                        XElem moduleElement = XParser.make(bufferedInputStream).parse();
                        return new Version(moduleElement.get("vendorVersion"));
                     }
                  }
               }
            );
         } catch (PrivilegedActionException e) {
            throw new IOException("Could not determine Niagara version", e.getException());
         }
      }

      return this.nreVersion;
   }

   public synchronized ServerCertificateHealth checkServerCertificateStatus(String alias, SecretChars passphrase, Logger logger) throws GeneralSecurityException {
      try {
         this.verifyServerCertificate(alias, passphrase, logger);
         return new ServerCertificateHealth(alias, alias, CertificateStatusEnum.OK);
      } catch (Exception e) {
         CertificateStatusEnum statusEnum;
         if (e instanceof UnrecoverableKeyException) {
            statusEnum = CertificateStatusEnum.BAD_PASSWORD;
         } else if (this.keyStore.containsAlias(alias)) {
            statusEnum = CertificateStatusEnum.BAD_KEY;
         } else {
            statusEnum = CertificateStatusEnum.MISSING_KEY;
         }

         if (!"default".equals(alias)) {
            if (logger.isLoggable(Level.FINEST)) {
               logger.log(Level.WARNING, "unable to obtain certificate '" + alias + "' (" + e.getLocalizedMessage() + "), trying '" + "default" + '\'', e);
            } else {
               logger.warning("unable to obtain certificate '" + alias + "' (" + e.getLocalizedMessage() + "), trying '" + "default" + '\'');
            }
         } else {
            logger.warning("unable to load factory certificate default with a password");
         }

         try {
            this.verifyServerCertificate("default", null, logger);
            return new ServerCertificateHealth(alias, "default", statusEnum);
         } catch (Exception f) {
            if (logger.isLoggable(Level.FINEST)) {
               logger.log(Level.WARNING, "unable to obtain certificate 'default' (" + f.getLocalizedMessage() + "), regenerating", f);
            } else {
               logger.warning("unable to obtain certificate 'default' (" + f.getLocalizedMessage() + "), regenerating");
            }

            try {
               statusEnum = CertificateStatusEnum.BAD_DEFAULT;
               this.keyStore.generateDefaultEntry(true);
            } catch (Exception g) {
               logger.log(Level.SEVERE, "unable to create default certificate", g);
               throw new GeneralSecurityException(g);
            }

            return new ServerCertificateHealth(alias, "default", statusEnum);
         }
      }
   }

   private void verifyServerCertificate(String alias, SecretChars passphrase, Logger logger) throws GeneralSecurityException {
      char[] keyChars = null;

      X509Certificate[] chain;
      PrivateKey privateKey;
      try {
         if (passphrase == null) {
            byte[] keyBytes = null;

            try {
               keyBytes = AccessController.doPrivileged(((CoreKeyStore)this.getKeyStore())::getKeyRingKey);
               keyChars = SecurityUtil.toHexChars(keyBytes);

               try (SecretChars keyRingSecretChars = new SecretChars(keyChars, true)) {
                  privateKey = (PrivateKey)this.getKeyStore().getKey(alias, keyRingSecretChars.get());
               }
            } finally {
               SecurityUtil.zeroByteArray(keyBytes);
            }
         } else {
            try (SecretChars passwordSecretChars = passphrase.newCopy()) {
               privateKey = (PrivateKey)this.getKeyStore().getKey(alias, passwordSecretChars.get());
            }
         }

         if (privateKey == null) {
            throw new GeneralSecurityException("missing private key");
         }

         chain = this.getKeyStore().getCertificateChain(alias);
         if (chain == null || chain.length <= 0) {
            throw new GeneralSecurityException("missing cert chain");
         }
      } catch (GeneralSecurityException gse) {
         throw gse;
      } catch (Exception e) {
         throw new GeneralSecurityException(e);
      } finally {
         SecurityUtil.zeroCharArray(keyChars);
      }

      if (!CertUtils.isServerCertificate(chain[0])) {
         throw new GeneralSecurityException("specified certificate '" + alias + "' is not a server certificate");
      }

      PublicKey publicKey = chain[0].getPublicKey();
      this.verifyCertChainSignatures(chain);
      this.verifyKeyPair(publicKey, privateKey);
   }

   public CertificateStatusEnum checkCACertificateStatus(String alias, SecretChars passphrase) throws GeneralSecurityException {
      try {
         this.verifyCACertificate(alias, passphrase);
         return CertificateStatusEnum.OK;
      } catch (Exception e) {
         CertificateStatusEnum statusEnum;
         if (e instanceof UnrecoverableKeyException) {
            statusEnum = CertificateStatusEnum.BAD_PASSWORD;
         } else if (this.keyStore.containsAlias(alias)) {
            statusEnum = CertificateStatusEnum.BAD_KEY;
         } else {
            statusEnum = CertificateStatusEnum.MISSING_KEY;
         }

         return statusEnum;
      }
   }

   private void verifyCACertificate(String alias, SecretChars passphrase) throws GeneralSecurityException {
      char[] keyChars = null;

      X509Certificate[] chain;
      PrivateKey privateKey;
      try {
         if (passphrase == null) {
            byte[] keyBytes = null;

            try {
               keyBytes = AccessController.doPrivileged(((CoreKeyStore)this.getKeyStore())::getKeyRingKey);
               keyChars = SecurityUtil.toHexChars(keyBytes);

               try (SecretChars keyRingSecretChars = new SecretChars(keyChars, true)) {
                  privateKey = (PrivateKey)this.getKeyStore().getKey(alias, keyRingSecretChars.get());
               }
            } finally {
               SecurityUtil.zeroByteArray(keyBytes);
            }
         } else {
            try (SecretChars passwordSecretChars = passphrase.newCopy()) {
               privateKey = (PrivateKey)this.getKeyStore().getKey(alias, passwordSecretChars.get());
            }
         }

         if (privateKey == null) {
            throw new GeneralSecurityException("missing private key");
         }

         chain = this.getKeyStore().getCertificateChain(alias);
         if (chain == null || chain.length <= 0) {
            throw new GeneralSecurityException("missing cert chain");
         }
      } catch (GeneralSecurityException gse) {
         throw gse;
      } catch (Exception e) {
         throw new GeneralSecurityException(e);
      } finally {
         SecurityUtil.zeroCharArray(keyChars);
      }

      if (!CertUtils.isCACertificate(chain[0])) {
         throw new GeneralSecurityException("specified certificate '" + alias + "' is not a CA certificate");
      }

      PublicKey publicKey = chain[0].getPublicKey();
      this.verifyCertChainSignatures(chain);
      this.verifyKeyPair(publicKey, privateKey);
   }

   private void verifyKeyPair(PublicKey publicKey, PrivateKey privateKey) throws GeneralSecurityException {
      byte[] bytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
      String sigAlg = null;
      String keyAlg = publicKey.getAlgorithm();
      if (keyAlg.equalsIgnoreCase("DSA")) {
         sigAlg = "SHA1withDSA";
      } else if (keyAlg.equalsIgnoreCase("RSA")) {
         sigAlg = "SHA1withRSA";
      } else if (keyAlg.equalsIgnoreCase("EC")) {
         sigAlg = "SHA1withECDSA";
      }

      Signature sig = Signature.getInstance(sigAlg);
      sig.initSign(privateKey);
      sig.update(bytes);
      byte[] sigBytes = sig.sign();
      sig = Signature.getInstance(sigAlg);
      sig.initVerify(publicKey);
      sig.update(bytes);
      if (!sig.verify(sigBytes)) {
         throw new GeneralSecurityException("mismatched public and private key, failed test signature verification");
      }
   }

   private void verifyCertChainSignatures(X509Certificate[] chain) throws GeneralSecurityException {
      chain[chain.length - 1].verify(chain[chain.length - 1].getPublicKey());
      if (chain.length > 1) {
         for (int i = chain.length - 2; i >= 0; i--) {
            chain[i].verify(chain[i + 1].getPublicKey());
         }
      }
   }

   public Server getSslContextFactory(ServerTlsParameters params) throws Exception {
      return this.getSslContextFactory(params, null);
   }

   public Server getSslContextFactory(ServerTlsParameters params, Map<String, SecretChars> sniAliases) throws Exception {
      NiagaraSslContextFactory sslContextFactory = new NiagaraSslContextFactory();
      sslContextFactory.setProtocol(CryptoSupport.TYPES.get(params.getMinTlsProtocol().toLowerCase()));
      sslContextFactory.setIncludeProtocols(CryptoSupport.TYPE_LISTS.get(params.getMinTlsProtocol().toLowerCase()));
      sslContextFactory.setExcludeProtocols(CryptoSupport.TYPE_EXCLUDE_LISTS.get(params.getMinTlsProtocol().toLowerCase()));
      synchronized (this.secureRandom) {
         sslContextFactory.setSecureRandomAlgorithm(this.secureRandom.getAlgorithm());
      }

      sslContextFactory.setIncludeCipherSuites(params.getTlsCipherSuiteGroup().getEnabledCipherSuites());
      sslContextFactory.setExcludeCipherSuites(new String[]{""});
      String alias = params.getCertAlias();
      if (alias != null) {
         boolean isSniConfigured = this.verifyCertificatesForSni(sslContextFactory, alias, sniAliases);
         if (isSniConfigured) {
            logger.log(Level.FINE, "Using SNI for web server TLS configuration.");
            sslContextFactory.setSniRequired(false);
            sslContextFactory.setSNISelector(new JettySniSelector(sslContextFactory, this.getPublicKeyType(alias), alias));
            if (params.getWantClientCertAuth()) {
               sslContextFactory.setTrustStore(get(this.secInfo).getTrustStore());
               sslContextFactory.setWantClientAuth(true);
            }

            this.configureCRLsForSni(sslContextFactory);
            sslContextFactory.setTrustManagerFactoryAlgorithm("PKIX");
         } else {
            logger.log(Level.FINE, "Using aliased key manager for web server TLS configuration.");
            this.configureAliasedKeyManager(sslContextFactory, params);
            sslContextFactory.setCertAlias(alias);
         }
      }

      return sslContextFactory;
   }

   public Client getSslContextFactory(ClientTlsParameters params, TrustManager[] trustManagers, boolean useCRLs) throws Exception {
      Client sslContextFactory = new Client();
      sslContextFactory.setProtocol(CryptoSupport.TYPES.get(params.getMinTlsProtocol().toLowerCase()));
      sslContextFactory.setIncludeProtocols(CryptoSupport.TYPE_LISTS.get(params.getMinTlsProtocol().toLowerCase()));
      sslContextFactory.setExcludeProtocols(CryptoSupport.TYPE_EXCLUDE_LISTS.get(params.getMinTlsProtocol().toLowerCase()));
      synchronized (this.secureRandom) {
         sslContextFactory.setSecureRandomAlgorithm(this.secureRandom.getAlgorithm());
      }

      sslContextFactory.setIncludeCipherSuites(params.getTlsCipherSuiteGroup().getEnabledCipherSuites());
      sslContextFactory.setExcludeCipherSuites(new String[]{""});
      if (useCRLs) {
         this.configureContextFactoryForCRLs(sslContextFactory);
      }

      String alias = params.getCertAlias();
      if (alias != null) {
         SSLContext sslContext = SSLContext.getInstance(CryptoSupport.TYPES.get("tlsv1"));
         sslContext.init(
            new AliasedKeyManagerBuilder(this.secInfo, alias, params.getKeyPassphrase() == null ? null : new SecretChars(params.getKeyPassphrase(), true))
               .getKeyManagers(),
            trustManagers,
            null
         );
         sslContextFactory.setSslContext(sslContext);
         sslContextFactory.setCertAlias(params.getCertAlias());
      }

      return sslContextFactory;
   }

   public Client getSslContextFactory(ClientTlsParameters params) throws Exception {
      Client sslContextFactory = new Client();
      sslContextFactory.setProtocol(CryptoSupport.TYPES.get(params.getMinTlsProtocol().toLowerCase()));
      sslContextFactory.setIncludeProtocols(CryptoSupport.TYPE_LISTS.get(params.getMinTlsProtocol().toLowerCase()));
      sslContextFactory.setExcludeProtocols(CryptoSupport.TYPE_EXCLUDE_LISTS.get(params.getMinTlsProtocol().toLowerCase()));
      synchronized (this.secureRandom) {
         sslContextFactory.setSecureRandomAlgorithm(this.secureRandom.getAlgorithm());
      }

      sslContextFactory.setIncludeCipherSuites(params.getTlsCipherSuiteGroup().getEnabledCipherSuites());
      sslContextFactory.setExcludeCipherSuites(new String[]{""});
      String alias = params.getCertAlias();
      if (alias != null) {
         SSLContext sslContext = SSLContext.getInstance(CryptoSupport.TYPES.get("tlsv1"));
         sslContext.init(
            new AliasedKeyManagerBuilder(this.secInfo, alias, params.getKeyPassphrase() == null ? null : new SecretChars(params.getKeyPassphrase(), true))
               .getKeyManagers(),
            new TrustManager[0],
            null
         );
         sslContextFactory.setSslContext(sslContext);
         sslContextFactory.setCertAlias(params.getCertAlias());
      }

      return sslContextFactory;
   }

   private boolean verifyCertificatesForSni(Server sslContextFactory, String alias, Map<String, SecretChars> sniAliases) throws Exception {
      if (!alias.isEmpty() && sniAliases != null && sniAliases.size() >= 2) {
         byte[] keyBytes = null;
         char[] keyChars = null;

         try {
            CoreKeyStore coreKeyStore = (CoreKeyStore)this.getKeyStore();
            KeyStorePermission.checkRead(coreKeyStore.storeName);
            KeyStore keyStore = AccessController.doPrivileged(coreKeyStore::getKeyStore);
            List<String> keyStoreAliases = Collections.list(keyStore.aliases());
            keyBytes = AccessController.doPrivileged(coreKeyStore::getKeyRingKey);
            keyChars = SecurityUtil.toHexChars(keyBytes);
            ProtectionParameter ksParam = new PasswordProtection(keyChars);
            KeyStore tempKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            tempKeyStore.load(null, null);

            for (Entry<String, SecretChars> sniAliasEntry : sniAliases.entrySet()) {
               String sniAlias = sniAliasEntry.getKey();
               if (keyStoreAliases.contains(sniAlias)) {
                  try {
                     ProtectionParameter tempKsParam = ksParam;
                     if (sniAliasEntry.getValue() != null) {
                        SecretChars aliasPass = sniAliasEntry.getValue();

                        try (SecretChars aliasPassCopy = aliasPass.newCopy()) {
                           this.verifyServerCertificate(sniAlias, aliasPassCopy, logger);
                        }

                        tempKsParam = new PasswordProtection(aliasPass.get());
                     } else {
                        this.verifyServerCertificate(sniAlias, null, logger);
                     }

                     java.security.KeyStore.Entry ksEntry = keyStore.getEntry(sniAlias, tempKsParam);
                     tempKeyStore.setEntry(sniAlias, ksEntry, ksParam);
                  } catch (Exception e) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.log(
                           Level.WARNING,
                           "Unable to obtain certificate '" + sniAlias + "' (" + e.getLocalizedMessage() + "), it will not be used for SNI configuration.",
                           e
                        );
                     } else {
                        logger.warning(
                           "Unable to obtain certificate '" + sniAlias + "' (" + e.getLocalizedMessage() + "), it will not be used for SNI configuration."
                        );
                     }
                  }
               } else {
                  logger.warning("An alias identified for participation in web server SNI configuration was not found in key store: " + sniAlias);
               }
            }

            if (tempKeyStore.size() < 2) {
               logger.log(Level.WARNING, "Could not validate a minimum of two certificates for SNI configuration.");
               return false;
            }

            sslContextFactory.setKeyStore(tempKeyStore);
            sslContextFactory.setKeyStorePassword(new String(keyChars));
         } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to configure SNI for web server (" + e.getLocalizedMessage() + "), standard configuration will be attempted.", e);
            return false;
         } finally {
            SecurityUtil.zeroByteArray(keyBytes);
            SecurityUtil.zeroCharArray(keyChars);
         }

         return true;
      } else {
         logger.fine("SNI cannot be configured due to unspecified web service https certificate or additional https certs.");
         return false;
      }
   }

   private String getPublicKeyType(String alias) throws KeyStoreException {
      String keyType = "";
      PublicKey publicKey = this.keyStore.getCertificateChain(alias)[0].getPublicKey();
      if (publicKey instanceof DSAPublicKey) {
         keyType = "DSA";
      } else if (publicKey instanceof ECPublicKey) {
         keyType = "EC";
      } else if (publicKey instanceof RSAPublicKey) {
         keyType = "RSA";
      } else {
         logger.warning("Unrecognized key type for certificate alias '" + alias + "': " + publicKey.getClass().getName());
      }

      return keyType;
   }

   private void configureAliasedKeyManager(Server sslContextFactory, ServerTlsParameters params) throws Exception {
      Set<X509CRL> crls = this.getCRLs();
      if (crls != null && !crls.isEmpty()) {
         this.configureContextFactoryForCRLs(sslContextFactory);
      }

      String alias = params.getCertAlias();
      if (alias != null) {
         TrustManager[] trustManagers = new TrustManager[0];
         if (params.getWantClientCertAuth()) {
            trustManagers = TrustManagerBuilder.getTrustManagers(this.getTrustAnchors(), crls);
            sslContextFactory.setWantClientAuth(true);
         }

         SSLContext sslContext = SSLContext.getInstance(CryptoSupport.TYPES.get("tlsv1"));
         sslContext.init(
            new AliasedKeyManagerBuilder(this.secInfo, alias, params.getKeyPassphrase() == null ? null : new SecretChars(params.getKeyPassphrase(), true))
               .getKeyManagers(),
            trustManagers,
            null
         );
         sslContextFactory.setSslContext(sslContext);
      }
   }

   private void configureCRLsForSni(NiagaraSslContextFactory sslContextFactory) throws Exception {
      Set<X509CRL> crls = this.getCRLs();
      if (crls != null && !crls.isEmpty()) {
         this.configureContextFactoryForCRLs(sslContextFactory);
         sslContextFactory.addCRLs(crls);
         sslContextFactory.setValidatePeerCerts(true);
      }
   }

   private void configureContextFactoryForCRLs(SslContextFactory sslContextFactory) throws Exception {
      CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
      PKIXRevocationChecker rc = (PKIXRevocationChecker)cpb.getRevocationChecker();
      rc.setOptions(EnumSet.of(Option.PREFER_CRLS, Option.ONLY_END_ENTITY, Option.NO_FALLBACK));
      sslContextFactory.setEnableCRLDP(false);
      sslContextFactory.setPkixCertPathChecker(rc);
   }

   @Override
   public ICoreProviderInfo getProviderInfo() {
      return this.providerInfo.updateAndGet(optionalProvider -> optionalProvider.isPresent() ? optionalProvider : Optional.of(new CoreProviderInfo())).get();
   }

   @Override
   public boolean isSecure() {
      return true;
   }

   private synchronized void load() throws Exception {
      try {
         AccessController.doPrivileged(() -> {
            this.loadPrivileged();
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   public synchronized void loadPrivileged() throws Exception {
   }

   private synchronized void save() throws Exception {
      try {
         AccessController.doPrivileged(() -> {
            this.savePrivileged();
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   public synchronized void savePrivileged() throws Exception {
   }

   private synchronized void init() throws Exception {
      try {
         AccessController.doPrivileged(() -> {
            this.initPrivileged();
            return null;
         });
      } catch (PrivilegedActionException e) {
         System.err.println("SEVERE [" + new Date() + "][crypto] Failed to initialize CoreCryptoManager (" + e.getException() + ")");
         e.getException().printStackTrace();
         throw e.getException();
      } catch (Exception e) {
         System.err.println("SEVERE [" + new Date() + "][crypto] Failed to initialize CoreCryptoManager (" + e + ")");
         e.printStackTrace();
         throw e;
      }
   }

   private synchronized void initPrivileged() throws Exception {
      boolean rebuild = false;
      File securityDir = this.secInfo.getSecurityDir();
      if (!securityDir.exists()) {
         securityDir.mkdirs();
      }

      try {
         this.load();
      } catch (Exception e) {
         System.err.println("SEVERE [" + new Date() + "][crypto] Failed to initialize privileged CoreCryptoManager (" + e + ")");
         e.printStackTrace();
         rebuild = true;
         this.save();
      }

      boolean isFips = SecurityInitializer.getInstance().isFips();
      String storeType = isFips ? "FIPS" : "JKS";
      String systemTrustStorePath = "lib/security/cacerts" + (isFips ? ".bcfks" : "");
      File systemTrustStoreFile = new File(AccessController.doPrivileged(() -> System.getProperty("java.home")), systemTrustStorePath);
      if (systemTrustStoreFile.exists()) {
         char[] cacertsKey = PlatformUtil.isNpsdkPlatform() ? "changeit".toCharArray() : "jiUz!rHw2d8&i3kI".toCharArray();
         verifyCacertsSignature(systemTrustStoreFile);
         KeyStore cacerts = KeyStore.getInstance(storeType);

         try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(systemTrustStoreFile))) {
            cacerts.load(bin, cacertsKey);
         } catch (Exception e) {
            try (BufferedInputStream bin = new BufferedInputStream(new FileInputStream(systemTrustStoreFile))) {
               cacerts.load(bin, "changeit".toCharArray());
            }
         }

         this.systemTrustStore = new CoreTrustStore(this, this.secInfo, cacerts, SYSTEM_TRUST_STORE_NAME);
      }

      File userTrustStoreFile = getStoreFile(securityDir, "cacerts");
      storeType = this.getStoreType(userTrustStoreFile);
      if (userTrustStoreFile.exists() && !rebuild) {
         this.userTrustStore = new CoreTrustStore(
            this, userTrustStoreFile, this.secInfo, "com.tridium.crypto.io.CoreKeyStore[UserTrustStore]", storeType, USER_TRUST_STORE_NAME
         );
         this.userTrustStore.setReadOnly(false);
      } else {
         if (userTrustStoreFile.exists()) {
            userTrustStoreFile.delete();
         }

         this.userTrustStore = new CoreTrustStore(
            this, userTrustStoreFile, this.secInfo, "com.tridium.crypto.io.CoreKeyStore[UserTrustStore]", storeType, USER_TRUST_STORE_NAME
         );
         this.userTrustStore.setReadOnly(false);
         this.userTrustStore.save();
      }

      File userUntrustedStoreFile = getStoreFile(securityDir, "untrusted");
      storeType = this.getStoreType(userUntrustedStoreFile);
      if (userUntrustedStoreFile.exists() && !rebuild) {
         this.userUntrustedStore = new CoreTrustStore(
            this, userUntrustedStoreFile, this.secInfo, "com.tridium.crypto.io.CoreKeyStore[UserUntrustedStore]", storeType, USER_UNTRUSTED_STORE_NAME
         );
         this.userUntrustedStore.setReadOnly(false);
      } else {
         if (userUntrustedStoreFile.exists()) {
            userUntrustedStoreFile.delete();
         }

         this.userUntrustedStore = new CoreTrustStore(
            this, userUntrustedStoreFile, this.secInfo, "com.tridium.crypto.io.CoreKeyStore[UserUntrustedStore]", storeType, USER_UNTRUSTED_STORE_NAME
         );
         this.userUntrustedStore.setReadOnly(false);
         this.userUntrustedStore.save();
      }

      this.userTrustStore.registerCertAddedListener(cert -> {
         try {
            String alias = this.userUntrustedStore.getCertificateAlias(cert);
            if (alias != null) {
               this.userUntrustedStore.deleteEntry(alias);
               this.userUntrustedStore.save();
            }
         } catch (Exception var3x) {
         }
      });
      File keyStoreFile = getStoreFile(this.secInfo.getSecurityDir(), "keystore");
      storeType = this.getStoreType(keyStoreFile);
      if (keyStoreFile.exists() && !rebuild) {
         this.keyStore = new CoreKeyStore(this, keyStoreFile, this.secInfo, "com.tridium.crypto.io.CoreKeyStore", storeType, USER_KEY_STORE_NAME);
         this.keyStore.setReadOnly(false);
      } else {
         if (keyStoreFile.exists()) {
            keyStoreFile.delete();
         }

         this.keyStore = new CoreKeyStore(this, keyStoreFile, this.secInfo, "com.tridium.crypto.io.CoreKeyStore", storeType, USER_KEY_STORE_NAME);
         this.keyStore.setReadOnly(false);
      }

      if (this.keyStore.containsAlias("default")) {
         X509Certificate cert = this.keyStore.getCertificate("default");
         if (!CertUtils.isServerCertificate(cert)) {
            File backupFile = new File(keyStoreFile.getAbsolutePath() + ".bu");
            FileUtil.copyFile(keyStoreFile, backupFile);
            this.keyStore.generateDefaultEntry(true);
            System.err
               .println(
                  "**************************************************************************************\nSEVERE ["
                     + new Date()
                     + "][crypto] A certificate called 'default' that\nisn't a server cert was found in the keystore. 'default' is required to be a server\ncert in order for Niagara to operate. It has been replaced and a backup has been\ncreated at:\n\n"
                     + backupFile.getAbsolutePath()
                     + "\n**************************************************************************************"
               );
         }
      } else {
         this.keyStore.generateDefaultEntry(false);
      }

      File exemptionStoreFile = new File(this.secInfo.getSecurityDir(), "exemptions.tes");
      if (exemptionStoreFile.exists() && !rebuild) {
         this.exemptionStore = new CoreExemptionStore(
            this, exemptionStoreFile, this.secInfo, "com.tridium.crypto.io.CoreExemptionStore", USER_EXEMPTION_STORE_NAME
         );
         this.exemptionStore.setReadOnly(false);
      } else {
         if (exemptionStoreFile.exists()) {
            exemptionStoreFile.delete();
         }

         this.exemptionStore = new CoreExemptionStore(
            this, exemptionStoreFile, this.secInfo, "com.tridium.crypto.io.CoreExemptionStore", USER_EXEMPTION_STORE_NAME
         );
         this.exemptionStore.setReadOnly(false);
         this.exemptionStore.save();
      }

      this.enableFileMonitor();
      this.recurringCertManagerImpl = new CoreCryptoManager.RecurringCertManagerImpl();

      try {
         this.loadRecurringCerts();
      } catch (Exception var38) {
      }

      this.initRecurringCertsSaveExecutor();
      this.certValidator = new CertificateChainValidator();
   }

   private static void verifyCacertsSignature(File cacerts) {
      if (SecurityConstants.canCheckTpk() && !PlatformUtil.isNpsdkPlatform()) {
         try {
            File sigFile = new File(cacerts.getAbsolutePath() + ".sig");
            byte[] cacertsBytes = Files.readAllBytes(cacerts.toPath());
            byte[] sigBytes = Files.readAllBytes(sigFile.toPath());
            Signature signature = Signature.getInstance("SHA256withRSA");
            PublicKey tpk = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(SecurityConstants.getTpk()));
            signature.initVerify(tpk);
            signature.update(cacertsBytes);
            if (!signature.verify(sigBytes)) {
               throw new SecurityException("Signature is invalid");
            }
         } catch (Exception e) {
            throw new SecurityException("Failed cacerts signature verification", e);
         }
      }
   }

   public void enableFileMonitor() {
      Objects.requireNonNull(this.exemptionStore);
      Objects.requireNonNull(this.keyStore);
      Objects.requireNonNull(this.userTrustStore);
      Objects.requireNonNull(this.userUntrustedStore);
      this.fileMonitor.getAndUpdate(optionalExecutor -> {
         if (optionalExecutor.isPresent()) {
            return optionalExecutor;
         }

         ScheduledExecutorService result = Executors.newSingleThreadScheduledExecutor(FILE_MONITOR_THREAD_FACTORY);
         result.scheduleAtFixedRate(this::checkFileMonitor, 10L, 10L, TimeUnit.SECONDS);
         return Optional.of(result);
      });
   }

   public void disableFileMonitor() {
      this.fileMonitor.getAndUpdate(optionalExecutor -> {
         if (optionalExecutor.isPresent()) {
            ((ScheduledExecutorService)optionalExecutor.get()).shutdownNow();

            try {
               ((ScheduledExecutorService)optionalExecutor.get()).awaitTermination(15L, TimeUnit.SECONDS);
            } catch (InterruptedException var2) {
            }

            return Optional.empty();
         } else {
            return optionalExecutor;
         }
      });
   }

   private void checkFileMonitor() {
      synchronized (PAUSE_STATE_MONITOR) {
         if (!this.checkPaused) {
            this.keyStore.checkLastModified();
            this.userTrustStore.checkLastModified();
            this.userUntrustedStore.checkLastModified();
            this.exemptionStore.checkLastModified();
         }
      }
   }

   public void pauseFileMonitor() {
      synchronized (PAUSE_STATE_MONITOR) {
         this.checkPaused = true;
      }
   }

   public void resumeFileMonitor() {
      synchronized (PAUSE_STATE_MONITOR) {
         this.checkPaused = false;
      }
   }

   @Override
   public void usedRecurringCert(X509Certificate cert) {
      this.recurringCertManagerImpl.usedRecurringCert(cert);
   }

   @Override
   public void removeRecurringCert(X509Certificate cert) {
      this.recurringCertManagerImpl.removeRecurringCert(cert);
   }

   @Override
   public void saveRecurringCerts() throws Exception {
      this.recurringCertManagerImpl.saveRecurringCerts();
   }

   private void checkSaveRecurringCertsSchedule() {
      try {
         synchronized (SAVE_STATE_MONITOR) {
            if (!this.savePaused) {
               this.recurringCertManagerImpl.saveRecurringCerts();
            }
         }
      } catch (Exception var4) {
      }
   }

   @Override
   public void loadRecurringCerts() throws Exception {
      this.recurringCertManagerImpl.loadRecurringCerts();
   }

   @Override
   public void clearRecurringCerts() {
      this.recurringCertManagerImpl.clearRecurringCerts();
   }

   private void initRecurringCertsSaveExecutor() {
      this.recurringCertsSaveExecutor.getAndUpdate(optionalExecutor -> {
         if (optionalExecutor.isPresent()) {
            return optionalExecutor;
         }

         ScheduledExecutorService result = Executors.newSingleThreadScheduledExecutor(RECURRING_SAVE_THREAD_FACTORY);
         result.scheduleAtFixedRate(this::checkSaveRecurringCertsSchedule, 10L, 10L, TimeUnit.MINUTES);
         return Optional.of(result);
      });
   }

   public void disableRecurringSave() {
      this.recurringCertsSaveExecutor.getAndUpdate(optionalExecutor -> {
         if (optionalExecutor.isPresent()) {
            ((ScheduledExecutorService)optionalExecutor.get()).shutdownNow();

            try {
               ((ScheduledExecutorService)optionalExecutor.get()).awaitTermination(15L, TimeUnit.SECONDS);
            } catch (InterruptedException var2) {
            }

            return Optional.empty();
         } else {
            return optionalExecutor;
         }
      });
   }

   public void pauseSaveRecurring() {
      synchronized (SAVE_STATE_MONITOR) {
         this.savePaused = true;
      }
   }

   public void resumeSaveRecurring() {
      synchronized (SAVE_STATE_MONITOR) {
         this.savePaused = false;
      }
   }

   public void validateCertChain(JarEntry entry, boolean checkTpk) throws ValidationException {
      this.validateCertChain(entry.getCodeSigners(), checkTpk);
   }

   public void validateCertChain(Class<?> cls, boolean checkTpk) throws ValidationException {
      try {
         AccessController.doPrivileged(() -> {
            CodeSigner[] signers = cls.getProtectionDomain().getCodeSource().getCodeSigners();
            this.validateCertChain(signers, checkTpk);
            return null;
         });
      } catch (PrivilegedActionException e) {
         throw (ValidationException)e.getCause();
      }
   }

   private void validateCertChain(CodeSigner[] codeSigners, boolean checkTpk) throws ValidationException {
      if (codeSigners != null && codeSigners.length != 0) {
         boolean foundValid = false;
         ValidationException exception = null;

         for (CodeSigner codeSigner : codeSigners) {
            try {
               this.validateCertChain(codeSigner, checkTpk);
               foundValid = true;
            } catch (ValidationException e) {
               exception = e;
            }
         }

         if (!foundValid && SecurityConstants.canCheckTpk()) {
            throw new ValidationException("Could not validate certificate chain", exception);
         }
      } else {
         throw new ValidationException("Error validating certPath chain: no code signers found");
      }
   }

   public void validateCertChain(CodeSigner codeSigner, boolean checkTpk) throws ValidationException {
      if (this.userTrustStoreChanged.getAndSet(false)) {
         this.certValidator.rebuildTrustAnchors(this);
      }

      this.certValidator.validateCertChain(codeSigner, checkTpk);
   }

   void trustStoreModified(CoreTrustStore store) {
      if (store == this.userTrustStore) {
         this.userTrustStoreChanged.set(true);
      }
   }

   public synchronized String registerTrustAnchorProvider(TrustAnchorProvider trustAnchorProvider) {
      NiagaraBasicPermission trustAnchorPermission = new NiagaraBasicPermission("MANAGE_SERVER_TRUST_ANCHORS");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(trustAnchorPermission);
      }

      byte[] idBytes = new byte[16];

      String id;
      do {
         synchronized (this.secureRandom) {
            this.secureRandom.nextBytes(idBytes);
         }

         id = ByteArrayUtil.toHexString(idBytes);
      } while (this.trustAnchorProviders.containsKey(id));

      this.trustAnchorProviders.put(id, trustAnchorProvider);
      this.trustAnchorsUpdated(id);
      return id;
   }

   public synchronized void unregisterTrustAnchorProvider(String id) {
      NiagaraBasicPermission trustAnchorPermission = new NiagaraBasicPermission("MANAGE_SERVER_TRUST_ANCHORS");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(trustAnchorPermission);
      }

      this.trustAnchorProviders.remove(id);
      this.trustAnchorsUpdated();
   }

   public synchronized void trustAnchorsUpdated(String id) {
      if (this.trustAnchorProviders.containsKey(id)) {
         this.trustAnchorsUpdated();
      }
   }

   private void trustAnchorsUpdated() {
      NiagaraBasicPermission trustAnchorPermission = new NiagaraBasicPermission("MANAGE_SERVER_TRUST_ANCHORS");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(trustAnchorPermission);
      }

      for (TrustAnchorConsumer trustAnchorConsumer : this.trustAnchorConsumers.values()) {
         try {
            trustAnchorConsumer.updateTrustAnchors();
         } catch (Exception e) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, "Exception while updating trust anchor " + trustAnchorConsumer, e);
            }
         }
      }
   }

   public synchronized String registerTrustAnchorConsumer(TrustAnchorConsumer trustAnchorConsumer) {
      byte[] idBytes = new byte[16];

      String id;
      do {
         synchronized (this.secureRandom) {
            this.secureRandom.nextBytes(idBytes);
         }

         id = ByteArrayUtil.toHexString(idBytes);
      } while (this.trustAnchorConsumers.containsKey(id));

      this.trustAnchorConsumers.put(id, trustAnchorConsumer);
      return id;
   }

   public synchronized void unregisterTrustAnchorConsumer(String id) {
      this.trustAnchorConsumers.remove(id);
   }

   public synchronized String registerCRLProvider(CRLProvider crlProvider) {
      byte[] idBytes = new byte[16];

      String id;
      do {
         synchronized (this.secureRandom) {
            this.secureRandom.nextBytes(idBytes);
         }

         id = ByteArrayUtil.toHexString(idBytes);
      } while (this.crlProviders.containsKey(id));

      this.crlProviders.put(id, crlProvider);
      return id;
   }

   public synchronized void unregisterCRLProvider(String id) {
      this.crlProviders.remove(id);
   }

   public Set<TrustAnchor> getTrustAnchors() {
      Set<TrustAnchor> trustAnchors = new HashSet<>();

      for (TrustAnchorProvider trustAnchorProvider : this.trustAnchorProviders.values()) {
         trustAnchors.addAll(trustAnchorProvider.getTrustAnchors());
      }

      return trustAnchors;
   }

   private KeyStore getTrustStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
      KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
      trustStore.load(null, null);
      int trustAnchorAliasIndex = 1;

      for (TrustAnchorProvider trustAnchorProvider : this.trustAnchorProviders.values()) {
         for (TrustAnchor trustAnchor : trustAnchorProvider.getTrustAnchors()) {
            trustStore.setCertificateEntry("anchor" + trustAnchorAliasIndex++, trustAnchor.getTrustedCert());
         }
      }

      return trustStore;
   }

   private Set<X509CRL> getCRLs() throws Exception {
      Set<X509CRL> crls = new HashSet<>();

      for (CRLProvider crlProvider : this.crlProviders.values()) {
         try {
            crls.addAll(crlProvider.getCRLs());
         } catch (CRLException e) {
            logger.warning("Could not get CRLs from CRL provider: " + crlProvider.getClass().toString());
            throw e;
         }
      }

      return crls;
   }

   private String getStoreType(File file) {
      String name = file.getName();
      String fileType = name.substring(name.lastIndexOf(46) + 1).toUpperCase();
      return fileType.equalsIgnoreCase("bcfks") ? "FIPS" : fileType;
   }

   private File getStoreFile(String directory, String store) {
      return new File(directory, store + "." + (SecurityInitializer.getInstance().isFips() ? "bcfks" : "jceks"));
   }

   public static File getStoreFile(File directory, String store) {
      return new File(directory, store + "." + (SecurityInitializer.getInstance().isFips() ? "bcfks" : "jceks"));
   }

   public class RecurringCertManagerImpl extends HashMap<String, Map<String, RecurringCertEntry>> implements IRecurringCertManager {
      private boolean recurringCertsDirty = false;
      private final int numToSave = 5;
      private final String RECURRING_CERTS_PATH = "recurringcerts.json";
      private final String SYSTEM_TRUST_STORE = "SystemTrustStore";
      private final String USER_TRUST_STORE = "UserTrustStore";
      private final String USER_UNTRUSTED_STORE = "UserUntrustedStore";

      public RecurringCertManagerImpl() {
         this.put("SystemTrustStore", new HashMap<>());
         this.put("UserTrustStore", new HashMap<>());
         this.put("UserUntrustedStore", new HashMap<>());
      }

      @Override
      public void usedRecurringCert(X509Certificate cert) {
         Objects.requireNonNull(cert);

         for (String storeName : this.keySet()) {
            Optional<KeyStore> oKeyStore = this.getStore(storeName);
            if (oKeyStore.isPresent()) {
               String alias = null;

               try {
                  alias = oKeyStore.get().getCertificateAlias(cert);
               } catch (KeyStoreException var7) {
               }

               if (alias != null) {
                  this.recurringCertsDirty = true;
                  this.incrementEntry(alias, storeName);
               }
            }
         }
      }

      @Override
      public void removeRecurringCert(X509Certificate cert) {
         Objects.requireNonNull(cert);

         for (String storeName : this.keySet()) {
            Optional<KeyStore> oKeyStore = this.getStore(storeName);
            if (oKeyStore.isPresent()) {
               String alias = null;

               try {
                  alias = oKeyStore.get().getCertificateAlias(cert);
               } catch (KeyStoreException var7) {
               }

               if (alias != null) {
                  this.recurringCertsDirty = true;
                  this.get(storeName).remove(alias);
               }
            }
         }
      }

      @Override
      public void saveRecurringCerts() throws IOException {
         if (this.recurringCertsDirty) {
            Map<String, List<RecurringCertEntry>> toSave = this.getTopCertLists(5);
            JSONObject jsonObject = new JSONObject();

            for (String storeName : toSave.keySet()) {
               toSave.get(storeName).forEach(entry -> jsonObject.accumulate(storeName, entry.toJson()));
            }

            Path path = Paths.get(CoreCryptoManager.this.secInfo.getSecurityDir().toString(), "recurringcerts.json");
            Files.write(path, jsonObject.toString().getBytes(Charset.forName("UTF-8")));
            this.recurringCertsDirty = false;
         }
      }

      @Override
      public void loadRecurringCerts() throws IOException {
         Path path = Paths.get(CoreCryptoManager.this.secInfo.getSecurityDir().toString(), "recurringcerts.json");
         StringBuilder sb = new StringBuilder();
         if (path.toFile().exists()) {
            Files.readAllLines(path, Charset.forName("UTF-8")).forEach(sb::append);
            JSONObject jsonObject = new JSONObject(sb.toString());
            Map<String, String[]> aliases = this.parseAliases(jsonObject);

            for (String key : aliases.keySet()) {
               Optional<KeyStore> oKeyStore = this.getStore(key);
               if (oKeyStore.isPresent()) {
                  for (String alias : aliases.get(key)) {
                     try {
                        oKeyStore.get().getCertificate(alias).getPublicKey();
                     } catch (Exception var13) {
                     }
                  }
               }
            }
         }
      }

      @Override
      public void clearRecurringCerts() {
         this.forEach((store, map) -> map.clear());
      }

      private Optional<KeyStore> getStore(String storeName) {
         try {
            switch (storeName) {
               case "UserTrustStore":
                  return Optional.ofNullable(CoreCryptoManager.this.getUserTrustStore().getKeyStore());
               case "UserUntrustedStore":
                  return Optional.ofNullable(CoreCryptoManager.this.getUserUntrustedStore().getKeyStore());
               case "SystemTrustStore":
                  return Optional.ofNullable(CoreCryptoManager.this.getSystemTrustStore().getKeyStore());
               default:
                  return Optional.empty();
            }
         } catch (Exception e) {
            return Optional.empty();
         }
      }

      private Map<String, List<RecurringCertEntry>> getTopCertLists(int numOfCerts) {
         Map<String, List<RecurringCertEntry>> rankedRecurringLists = this.getRankedRecurringCertLists();
         Map<String, List<RecurringCertEntry>> toSave = new HashMap<>();
         this.keySet().forEach(storeNamex -> {
            List var10000 = toSave.put(storeNamex, new ArrayList<>());
         });

         for (int i = 0; i < numOfCerts; i++) {
            RecurringCertEntry best = null;
            String bestStoreName = null;

            for (String storeName : rankedRecurringLists.keySet()) {
               List<RecurringCertEntry> testList = rankedRecurringLists.get(storeName);
               if (testList.size() > 0 && testList.get(0).getCount() > 0 && (best == null || testList.get(0).getCount() > best.getCount())) {
                  best = testList.get(0);
                  bestStoreName = storeName;
               }
            }

            if (best != null) {
               toSave.get(bestStoreName).add(best);
               rankedRecurringLists.get(bestStoreName).remove(best);
            }
         }

         return toSave;
      }

      private Map<String, List<RecurringCertEntry>> getRankedRecurringCertLists() {
         Map<String, List<RecurringCertEntry>> rankedRecurringLists = new HashMap<>();

         for (String storeName : this.keySet()) {
            List<RecurringCertEntry> entries = new ArrayList<>(this.get(storeName).values());
            entries.sort((a, b) -> b.getCount().compareTo(a.getCount()));
            rankedRecurringLists.put(storeName, entries);
         }

         return rankedRecurringLists;
      }

      private void incrementEntry(String alias, String storeName) {
         RecurringCertEntry entry = null;
         if (alias != null && this.get(storeName) != null) {
            entry = this.get(storeName).get(alias);
         }

         if (entry != null) {
            entry.increment();
         } else if (alias != null) {
            RecurringCertEntry newEntry = new RecurringCertEntry(alias);
            newEntry.increment();
            this.get(storeName).put(alias, newEntry);
         }
      }

      private Map<String, String[]> parseAliases(JSONObject jsonObject) {
         int numToLoad = 5;
         Map<String, String[]> storeAliasMap = new HashMap<>();
         Iterator<?> keyIterator = jsonObject.keys();

         while (keyIterator.hasNext()) {
            List<String> aliases = new ArrayList<>();
            Object key = keyIterator.next();
            if (key instanceof String) {
               Object result = jsonObject.get((String)key);
               if (result instanceof JSONArray) {
                  JSONArray entries = (JSONArray)result;

                  for (int i = 0; i < entries.length() && i < numToLoad; i++) {
                     Object alias = entries.getJSONObject(i).get("alias");
                     if (alias instanceof String) {
                        aliases.add((String)alias);
                     }
                  }
               }

               storeAliasMap.put((String)key, aliases.toArray(new String[0]));
            }
         }

         return storeAliasMap;
      }
   }
}
