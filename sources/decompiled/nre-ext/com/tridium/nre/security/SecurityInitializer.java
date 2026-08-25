package com.tridium.nre.security;

import com.tridium.nre.bootstrap.Bootstrap;
import com.tridium.nre.di.NreInstantiationException;
import com.tridium.nre.di.RequiresConfiguration;
import com.tridium.nre.security.provider.XMLDSigRI;
import com.tridium.nre.util.NiagaraFiles;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.Security;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.crypto.Cipher;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

@RequiresConfiguration(method = "configure", argument = ISecurityInitializerConfig.class)
public final class SecurityInitializer implements ISecurityInitializer {
   private static final String SUNJGSS_PROVIDER = "SunJGSS";
   private static final String SUNJCE_PROVIDER = "SunJCE";
   private static HashSet<Object> sunJCEWhitelist = new HashSet<>();
   private static final String SUNRSA_PROVIDER = "SunRsaSign";
   private static HashSet<Object> sunRSAWhitelist = new HashSet<>();
   private static final String SUN_PROVIDER = "SUN";
   private static HashSet<Object> sunWhitelist = new HashSet<>();
   private static final String BCFIPS_PROVIDER = "BCFIPS";
   private static final String BCFIPSKS_PROVIDER = "BCFIPS-WRAP-BCFKS";
   private static final String BCFIPS_JSSE_PROVIDER = "BCJSSE";
   private static final String XMLDSIG_PROVIDER = "XMLDSig";
   private static final int FIPS_VERSION = 2;
   private static final int FIPS_LEVEL = 1;
   private static final Date FIPS_REVISION_DATE;
   private static final int NIAGARA_FIPS_VERSION = 1;
   private static final String DEFAULT_KR_NAME = ".kr";
   private static Logger log = Logger.getLogger("security.initializer");
   private ISecurityInitializerConfig siConfig = null;
   private ISecurityInfoProvider secInfProvider = null;
   private FipsInformation fipsInformation = null;
   private CryptoProvider cryptoProvider = null;

   public static ISecurityInitializer getInstance() throws NreInstantiationException {
      return AccessController.doPrivileged(() -> Bootstrap.getInstantiator().instance(ISecurityInitializer.class));
   }

   SecurityInitializer() {
      this.initializeSecurityProviders();
      this.checkUnrestrictedPolicyFiles();
      this.initFipsInformation();
   }

   private void configure(ISecurityInitializerConfig config) {
      synchronized (this) {
         if (this.siConfig != null) {
            throw new IllegalStateException("security initializer configuration has already been set");
         }

         this.siConfig = config;
         this.initSecurityInfo(false);
      }
   }

   @Override
   public ISecurityInfoProvider getSecurityInfoProvider() {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         NiagaraBasicPermission securityInfoPermission = new NiagaraBasicPermission("GET_SECURITY_INFO_PROVIDER");
         sm.checkPermission(securityInfoPermission);
      }

      return this.secInfProvider;
   }

   private void initializeSecurityProviders() {
      try {
         if (!this.isFipsLicensed() || !Bootstrap.isStation() && Bootstrap.nonFipsOverride()) {
            this.initializeSecurityProviders(false);
         } else {
            System.setProperty("org.bouncycastle.fips.approved_only", "true");
            this.initializeSecurityProviders(true);
            log.fine("FIPS mode successfully loaded.");
         }
      } catch (Exception e) {
         e.printStackTrace();
         throw new SecurityException("Could not initializeSecurityProviders FIPS mode: " + e);
      }
   }

   private void initializeSecurityProviders(boolean isFips) {
      AccessController.doPrivileged(new SecurityInitializer.InitializePrivilegedAction(isFips));
   }

   private void removeSecurityProviders() {
      Provider[] providers = Security.getProviders();

      for (Provider provider : providers) {
         if (!provider.getName().equals("SunJCE")
            && !provider.getName().equals("SunRsaSign")
            && !provider.getName().equals("SUN")
            && !provider.getName().equals("SunJGSS")
            && !provider.getName().equals("BCFIPS-WRAP-BCFKS")
            && !provider.getName().equals("BCJSSE")
            && !provider.getName().equals("BCFIPS")
            && !provider.getName().equals("XMLDSig")) {
            Security.removeProvider(provider.getName());
         }

         Provider xmlDSigProvider = Security.getProvider("XMLDSig");
         if (xmlDSigProvider != null) {
            Provider tridiumDomProvider = new XMLDSigRI(xmlDSigProvider);
            Security.removeProvider("XMLDSig");
            Security.addProvider(tridiumDomProvider);
         }
      }

      this.stripSunServices();
   }

   private void stripSunServices() {
      Provider sunJCE = Security.getProvider("SunJCE");
      Set<Object> nonEssentialAlgorithms = new HashSet<>(sunJCE.keySet());
      nonEssentialAlgorithms.removeAll(sunJCEWhitelist);

      for (Object obj : nonEssentialAlgorithms) {
         sunJCE.remove(obj);
      }

      Security.removeProvider("SunJCE");
      Security.addProvider(sunJCE);
      Provider sunRSA = Security.getProvider("SunRsaSign");
      nonEssentialAlgorithms = new HashSet<>(sunRSA.keySet());
      nonEssentialAlgorithms.removeAll(sunRSAWhitelist);

      for (Object obj : nonEssentialAlgorithms) {
         sunRSA.remove(obj);
      }

      Security.removeProvider("SunRsaSign");
      Security.addProvider(sunRSA);
      Provider sun = Security.getProvider("SUN");
      nonEssentialAlgorithms = new HashSet<>(sun.keySet());
      nonEssentialAlgorithms.removeAll(sunWhitelist);

      for (Object obj : nonEssentialAlgorithms) {
         sun.remove(obj);
      }

      Security.removeProvider("SUN");
      Security.addProvider(sun);
   }

   private boolean isFipsLicensed() {
      File dir = NiagaraFiles.getPerpetualLicensePath();
      File[] files = dir.listFiles();
      if (files == null) {
         return false;
      }

      for (File f : files) {
         try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            if (this.isFipsLicensed(in)) {
               return true;
            }
         } catch (Exception e) {
            log.log(Level.FINEST, "unable to read tridium.license", e);
         }
      }

      return false;
   }

   private boolean isFipsLicensed(InputStream in) {
      try {
         XElem[] elems = XParser.make(in).parse().elems("feature");

         for (XElem e : elems) {
            if (e.get("name").equals("fips140-2")) {
               long now = System.currentTimeMillis();
               long expiration = this.parseDate(e.get("expiration"));
               if (now < expiration) {
                  return true;
               }
            }
         }
      } catch (Exception e) {
         log.log(Level.FINEST, "error parsing license xml", e);
      }

      return false;
   }

   private long parseDate(String x) throws Exception {
      if (x.equalsIgnoreCase("never")) {
         return Long.MAX_VALUE;
      }

      try {
         StringTokenizer st = new StringTokenizer(x, "- ");
         int year = Integer.parseInt(st.nextToken()) - 1900;
         int month = Integer.parseInt(st.nextToken()) - 1;
         int day = Integer.parseInt(st.nextToken());
         Date d = new GregorianCalendar(year + 1900, month, day, 23, 59).getTime();
         return d.getTime();
      } catch (Exception e) {
         throw new Exception("Invalid date format yyyy-MM-dd: " + x);
      }
   }

   private void checkUnrestrictedPolicyFiles() throws SecurityException {
      boolean unrestrictedPolicyFilesPresent = false;

      try {
         if (Cipher.getMaxAllowedKeyLength("AES") > 128) {
            unrestrictedPolicyFilesPresent = true;
         }
      } catch (NoSuchAlgorithmException var3) {
      }

      if (!unrestrictedPolicyFilesPresent) {
         throw new SecurityException("Unrestricted cryptography policy files not found, can not start");
      }
   }

   @Override
   public void initSecurityInfo(boolean resetKeyRing) {
      if (resetKeyRing) {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            NiagaraBasicPermission securityInfoPermission = new NiagaraBasicPermission("RESET_KEYRING");
            sm.checkPermission(securityInfoPermission);
         }
      }

      try {
         if (this.secInfProvider == null || resetKeyRing) {
            if (resetKeyRing) {
               for (File f : this.siConfig.getSecDir().listFiles()) {
                  f.delete();
               }
            }

            KeyRing kr = KeyRingFactory.getInstance(this.siConfig.getSecDir(), ".kr", this.siConfig.getKmName()).getKeyRing(resetKeyRing);
            this.secInfProvider = new SecurityInitializer.SecurityInfoProvider(kr, this.siConfig, ".kr");
         }
      } catch (Exception e) {
         throw new RuntimeException("Unable to initializeSecurityProviders key ring", e);
      }
   }

   @Override
   public boolean isFips() {
      return this.cryptoProvider.isFips();
   }

   @Override
   public FipsInformation getFipsInformation() {
      return this.fipsInformation;
   }

   private void initFipsInformation() {
      if (this.isFips()) {
         Provider bcProvider = Security.getProvider("BCFIPS");
         double BC_PROVIDER_VERSION = bcProvider.getVersion();
         this.fipsInformation = new FipsInformation(2, 1, FIPS_REVISION_DATE, "BCFIPS", BC_PROVIDER_VERSION, 1);
      }
   }

   @Override
   public CryptoProvider getCryptoProvider() {
      return this.cryptoProvider;
   }

   static {
      sunJCEWhitelist.add("Cipher.Blowfish");
      sunJCEWhitelist.add("AlgorithmParameters.PBE");
      sunJCEWhitelist.add("KeyGenerator.SunTlsRsaPremasterSecret");
      sunJCEWhitelist.add("Alg.Alias.KeyGenerator.SunTls12RsaPremasterSecret");
      sunJCEWhitelist.add("KeyGenerator.SunTlsMasterSecret");
      sunJCEWhitelist.add("Alg.Alias.KeyGenerator.SunTls12MasterSecret");
      sunJCEWhitelist.add("KeyGenerator.SunTlsKeyMaterial");
      sunJCEWhitelist.add("Alg.Alias.KeyGenerator.SunTls12KeyMaterial");
      sunJCEWhitelist.add("KeyGenerator.SunTlsPrf");
      sunJCEWhitelist.add("KeyGenerator.SunTls12Prf");
      sunRSAWhitelist.add("Provider.id className");
      sunRSAWhitelist.add("Provider.id info");
      sunRSAWhitelist.add("Provider.id name");
      sunRSAWhitelist.add("Provider.id version");
      sunRSAWhitelist.add("Signature.SHA1withRSA");
      sunRSAWhitelist.add("Signature.SHA1withRSA SupportedKeyClasses");
      sunRSAWhitelist.add("Signature.SHA256withRSA");
      sunRSAWhitelist.add("Signature.MD5withRSA");
      sunRSAWhitelist.add("Alg.Alias.Signature.1.2.840.113549.1.1.4");
      sunWhitelist.add("Provider.id className");
      sunWhitelist.add("Provider.id info");
      sunWhitelist.add("Provider.id name");
      sunWhitelist.add("Provider.id version");
      sunWhitelist.add("CertificateFactory.X.509");
      sunWhitelist.add("CertificateFactory.X.509 ImplementedIn");
      sunWhitelist.add("Alg.Alias.CertificateFactory.X509");
      sunWhitelist.add("KeyStore.JKS");
      sunWhitelist.add("MessageDigest.MD5");
      sunWhitelist.add("MessageDigest.SHA");
      sunWhitelist.add("Alg.Alias.MessageDigest.SHA-1");
      sunWhitelist.add("MessageDigest.SHA-256");
      sunWhitelist.add("Signature.SHA1withDSA");
      sunWhitelist.add("Signature.SHA256withDSA");
      Calendar calendar = Calendar.getInstance();
      calendar.set(2016, 3, 6, 0, 0);
      FIPS_REVISION_DATE = calendar.getTime();
   }

   private class InitializePrivilegedAction implements PrivilegedAction<Object> {
      private boolean isFips;

      InitializePrivilegedAction(boolean isFips) {
         this.isFips = isFips;
      }

      @Override
      public Object run() {
         if (this.isFips) {
            SecurityInitializer.this.cryptoProvider = new BouncyCastleFipsCryptoProvider();
            Provider tlsProvider = new BouncyCastleJsseProvider("fips:BCFIPS");
            Security.addProvider(tlsProvider);
            SecurityInitializer.this.removeSecurityProviders();
         } else {
            SecurityInitializer.this.cryptoProvider = new BouncyCastleCryptoProvider();
            Provider tlsProvider = new BouncyCastleJsseProvider();
            Security.addProvider(tlsProvider);
            Security.removeProvider("BCFIPS");
            Security.removeProvider("BCFIPS-WRAP-BCFKS");
            Security.insertProviderAt(SecurityInitializer.this.cryptoProvider.getProvider(), 1);
            HsmManagerImpl mgr = HsmManagerImpl.make(this.getClass().getClassLoader());
            mgr.registerProvider();
         }

         Security.setProperty("keystore.type", SecurityInitializer.this.cryptoProvider.getDefaultKeyStoreType());
         return null;
      }
   }

   private class SecurityInfoProvider implements ISecurityInfoProvider {
      private final KeyRing keyRing;
      private final File securityDir;
      private final String keyMaterialName;
      private final String keyRingName;

      private SecurityInfoProvider(KeyRing keyRing, ISecurityInitializerConfig securityInitializerConfig, String keyRingName) {
         this.keyRing = keyRing;
         this.securityDir = securityInitializerConfig.getSecDir();
         this.keyMaterialName = securityInitializerConfig.getKmName();
         this.keyRingName = keyRingName;
      }

      @Override
      public KeyRing getKeyRing() {
         return this.keyRing;
      }

      @Override
      public File getSecurityDir() {
         return this.securityDir;
      }

      @Override
      public String getKeyMaterialName() {
         return this.keyMaterialName;
      }

      @Override
      public String getKeyRingName() {
         return this.keyRingName;
      }

      @Override
      public int getDefaultMinimumPasswordLength() {
         return SecurityInitializer.this.isFips() ? 14 : 10;
      }
   }
}
