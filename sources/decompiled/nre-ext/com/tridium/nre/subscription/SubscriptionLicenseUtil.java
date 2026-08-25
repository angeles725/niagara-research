package com.tridium.nre.subscription;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.KeyRingFactory;
import com.tridium.nre.util.FileLock;
import com.tridium.nre.util.FileLockException;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.NiagaraFiles;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class SubscriptionLicenseUtil {
   public static final String HOST_ID_OK = "ok";
   public static final String HOST_ID_UNREGISTERED = "unregistered";
   public static final String HOST_ID_CLONED = "cloned";
   public static final String HOST_ID_PERPETUAL = "perpetual";
   public static final String LICENSE_UPDATE_FOR_VENDOR = "Subscription license updated for vendor";
   public static final String SUBSCRIPTION_KEY_REGEX = "[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}";
   protected static final File LICENSE_PROPERTIES_FILE = NiagaraFiles.getLicensePropertiesPath();
   private static String hostIdStatus = null;
   private static String nreId;
   private static boolean licenseModeInitialized = false;
   private static final Logger logger = Logger.getLogger("sys.license");
   private static final Properties licenseProperties = new Properties();
   private static final String KEY_RING_NAME = ".kr";
   private static final String KEY_MATERIAL_NAME = ".km";
   private static final String NRE_ID_FILENAME = "nreId";
   private static final String RESTORE_ID_FILENAME = ".restoreId";
   private static final File SUBSCRIPTION_DIRECTORY = NiagaraFiles.getSubscriptionPath();
   private static final File LICENSE_DIRECTORY = new File(SUBSCRIPTION_DIRECTORY, "licenses");
   private static final File SUBSCRIPTION_DB_DIRECTORY = new File(SUBSCRIPTION_DIRECTORY, "db");
   private static final File CERTIFICATE_DIRECTORY = new File(SUBSCRIPTION_DIRECTORY, "certificates");
   private static final File REGISTRATION_FILE = new File(SUBSCRIPTION_DIRECTORY, ".registered");
   private static final File CLONED_FILE = new File(SUBSCRIPTION_DIRECTORY, ".cloned");
   public static final SecureRandom SECURE_RANDOM = new SecureRandom();
   private static final int NRE_ID_FILE_LOCK_TIMEOUT = 5000;

   private SubscriptionLicenseUtil() {
   }

   public static SubscriptionLicenseUtil getInstance() {
      return SubscriptionLicenseUtil.SubscriptionLicenseUtilHolder._SUBSCRIPTION_LICENSE_UTIL_INSTANCE;
   }

   public static String getHostIdStatus() {
      if (getLicenseMode() == LicenseMode.PERPETUAL) {
         hostIdStatus = "perpetual";
         return hostIdStatus;
      } else if (!isRegistered() && !isCloned()) {
         hostIdStatus = "unregistered";
         return hostIdStatus;
      } else if (isCloned()) {
         hostIdStatus = "cloned";
         return hostIdStatus;
      } else {
         hostIdStatus = "ok";
         return hostIdStatus;
      }
   }

   public static LicenseMode getLicenseMode() {
      loadLicenseProperties();
      boolean platformRequiresSubscription = SubscriptionLicenseUtil.LocalHostMetaDataHolder.REQUIRE_SUBSCRIPTION;
      String subscriptionLicenseModeProperty = licenseProperties.getProperty("license.subscriptionMode", platformRequiresSubscription ? "true" : "false");
      if (Boolean.parseBoolean(subscriptionLicenseModeProperty)) {
         return LicenseMode.SUBSCRIPTION;
      } else if (platformRequiresSubscription) {
         logger.log(Level.INFO, "The platform is forcing subscription licensing, overriding property settings.");
         return LicenseMode.SUBSCRIPTION;
      } else {
         return LicenseMode.PERPETUAL;
      }
   }

   public static void reinitializeLicenseMode() {
      licenseModeInitialized = false;
      loadLicenseProperties();
   }

   public static void createSubscriptionLicCertDirectory() {
      if (getLicenseMode() == LicenseMode.SUBSCRIPTION) {
         getSubscriptionDirectory();
         getSubscriptionCertificateDirectory();
         getSubscriptionLicenseDirectory();
      } else {
         nreId = null;
      }
   }

   public static File getSubscriptionDirectory() {
      if (!SUBSCRIPTION_DIRECTORY.exists()) {
         try {
            Files.createDirectory(SUBSCRIPTION_DIRECTORY.toPath());
         } catch (IOException ioe) {
            throw new RuntimeException("Unable to create the subscription folder " + SUBSCRIPTION_DIRECTORY, ioe);
         }
      }

      return SUBSCRIPTION_DIRECTORY;
   }

   public static File getSubscriptionDbDirectory(String licenseKey) {
      if (!SUBSCRIPTION_DB_DIRECTORY.exists()) {
         try {
            Files.createDirectory(SUBSCRIPTION_DB_DIRECTORY.toPath());
         } catch (IOException ioe) {
            throw new RuntimeException("Unable to create the remote db folder " + SUBSCRIPTION_DB_DIRECTORY, ioe);
         }
      }

      File DEVICE_DIRECTORY = new File(SUBSCRIPTION_DB_DIRECTORY, licenseKey);
      if (!DEVICE_DIRECTORY.exists()) {
         try {
            Files.createDirectory(DEVICE_DIRECTORY.toPath());
         } catch (IOException ioe) {
            throw new RuntimeException("Unable to create the remote db folder " + DEVICE_DIRECTORY, ioe);
         }
      }

      return DEVICE_DIRECTORY;
   }

   public static File getSubscriptionCertificateDirectory() {
      if (!CERTIFICATE_DIRECTORY.exists()) {
         try {
            Files.createDirectory(CERTIFICATE_DIRECTORY.toPath());
         } catch (IOException ioe) {
            throw new RuntimeException("Unable to create the subscription certificates folder " + CERTIFICATE_DIRECTORY, ioe);
         }
      }

      return CERTIFICATE_DIRECTORY;
   }

   public static File getSubscriptionLicenseDirectory() {
      if (!LICENSE_DIRECTORY.exists()) {
         try {
            Files.createDirectory(LICENSE_DIRECTORY.toPath());
         } catch (IOException ioe) {
            throw new RuntimeException("Unable to create the subscription licenses folder " + LICENSE_DIRECTORY, ioe);
         }
      }

      return LICENSE_DIRECTORY;
   }

   public static String getNreId() {
      if (nreId == null || nreId.isEmpty()) {
         nreId = establishNreId();
      }

      return nreId;
   }

   public static String regenerateNreId() throws IOException {
      if (SUBSCRIPTION_DIRECTORY.exists()) {
         deleteDirectoryContents(SUBSCRIPTION_DIRECTORY);
      }

      nreId = null;
      getNreId();
      return nreId;
   }

   public static void deleteSubscriptionDbDirectory(String licenseKey) throws IOException {
      if (SUBSCRIPTION_DB_DIRECTORY.exists()) {
         File DEVICE_DIRECTORY = new File(SUBSCRIPTION_DB_DIRECTORY, licenseKey);
         if (DEVICE_DIRECTORY.exists()) {
            deleteDirectoryContents(DEVICE_DIRECTORY);
         }
      }
   }

   private static void deleteDirectoryContents(File directory) throws IOException {
      File[] files = directory.listFiles();
      if (files != null) {
         for (File file : files) {
            if (file.isDirectory()) {
               deleteDirectoryContents(file);
            } else if (!file.delete()) {
               throw new IOException("Unable to delete file: " + file.getName());
            }
         }
      }

      if (!directory.delete()) {
         throw new IOException("Unable to delete directory: " + directory.getName());
      }
   }

   public static Properties getLicenseProperties() {
      loadLicenseProperties();
      return licenseProperties;
   }

   protected static File getLicensePropertiesFile() {
      if (!LICENSE_PROPERTIES_FILE.exists()) {
         try {
            Files.createFile(LICENSE_PROPERTIES_FILE.toPath());
         } catch (IOException ioe) {
            throw new RuntimeException("Unable to create license.properties file " + LICENSE_PROPERTIES_FILE, ioe);
         }
      }

      return LICENSE_PROPERTIES_FILE;
   }

   public static synchronized void removeRestoreParameters() throws IOException {
      if (SUBSCRIPTION_DIRECTORY.exists()) {
         File restoreIdFile = new File(SUBSCRIPTION_DIRECTORY, ".restoreId");
         if (restoreIdFile.exists()) {
            boolean fileDeleteStatus = restoreIdFile.delete();
            if (!fileDeleteStatus) {
               throw new IOException("Failed to delete Restore ID File");
            }
         }

         RefreshIncrement.getInstance().reset();
         if (REGISTRATION_FILE.exists()) {
            boolean fileDeleteStatus = REGISTRATION_FILE.delete();
            if (!fileDeleteStatus) {
               throw new IOException("Failed to delete Registered File");
            }
         }

         if (LICENSE_DIRECTORY.exists()) {
            deleteDirectoryContents(LICENSE_DIRECTORY);
         }
      }
   }

   public static synchronized boolean isRegistered() {
      return REGISTRATION_FILE.exists() ? readRegistrationTime().isAfter(Instant.EPOCH) : false;
   }

   private static synchronized Instant readRegistrationTime() {
      try (DataInputStream in = new DataInputStream(new FileInputStream(REGISTRATION_FILE))) {
         return Instant.parse(in.readUTF());
      } catch (Exception e) {
         System.err.println("WARNING [" + new Date() + "][nre] Registration file was corrupted. " + e.getLocalizedMessage());
         if (REGISTRATION_FILE.exists() && !REGISTRATION_FILE.delete()) {
            System.err.println("WARNING [" + new Date() + "][nre] Failed to delete corrupted registration file: " + REGISTRATION_FILE);
         }

         return Instant.EPOCH;
      }
   }

   public static synchronized boolean isCloned() {
      return CLONED_FILE.exists() ? readCloningTime().isAfter(Instant.EPOCH) : false;
   }

   private static synchronized Instant readCloningTime() {
      try (DataInputStream in = new DataInputStream(new FileInputStream(CLONED_FILE))) {
         return Instant.parse(in.readUTF());
      } catch (Exception e) {
         System.err.println("WARNING [" + new Date() + "][nre] Cloned file was corrupted. " + e.getLocalizedMessage());
         if (CLONED_FILE.exists() && !CLONED_FILE.delete()) {
            System.err.println("WARNING [" + new Date() + "][nre] Failed to delete corrupted cloned file: " + CLONED_FILE);
         }

         return Instant.EPOCH;
      }
   }

   private static synchronized void loadLicenseProperties() {
      if (!licenseModeInitialized) {
         licenseProperties.clear();
         if (LICENSE_PROPERTIES_FILE.exists()) {
            try (FileInputStream in = new FileInputStream(LICENSE_PROPERTIES_FILE)) {
               licenseProperties.load(in);
            } catch (Throwable e) {
               throw new RuntimeException("Unable to open " + NiagaraFiles.getLicensePropertiesFileName(), e);
            }
         }

         licenseModeInitialized = true;
      }
   }

   private static String establishNreId() {
      return establishNreId(SUBSCRIPTION_DIRECTORY);
   }

   private static String establishNreId(File nreIdDir) {
      if (!nreIdDir.exists()) {
         try {
            if (!nreIdDir.mkdir()) {
               throw new RuntimeException("Unable to create the subscription license folder");
            }
         } catch (Exception e) {
            throw new RuntimeException("Failed to create the subscription license folder", e);
         }
      }

      File nreIdFile = new File(nreIdDir, "nreId");
      FileLock nreIdFileLock = null;
      if (nreIdFile.exists()) {
         try {
            nreIdFileLock = FileLock.lock(nreIdFile, 5000);

            try (DataInputStream in = new DataInputStream(new FileInputStream(nreIdFile))) {
               return in.readUTF();
            } catch (Exception e) {
               System.err
                  .println("WARNING [" + new Date() + "][nre] Exception occurred opening the existing NRE ID, generating a new one. " + e.getLocalizedMessage());
               if (nreIdFile.exists() && !nreIdFile.delete()) {
                  System.err
                     .println("WARNING [" + new Date() + "][nre] The existing NRE ID file was not deleted, so there could be a problem generating a new one.");
               }
            }
         } catch (FileLockException e) {
            throw new RuntimeException("Failed to obtain a lock on the NRE ID file", e);
         } catch (IOException e) {
            throw new RuntimeException("Failed to resolve the existing NRE ID file", e);
         } finally {
            if (nreIdFileLock != null) {
               nreIdFileLock.unlock();
            }
         }
      }

      try {
         if (nreIdFile.createNewFile()) {
            try {
               nreIdFileLock = FileLock.lock(nreIdFile, 5000);
               String newUUID = UUID.randomUUID().toString();
               StringBuilder newNreIdBuilder = new StringBuilder();
               String newNreId = newNreIdBuilder.append(newUUID, 0, 4)
                  .append('-')
                  .append(newUUID, 4, 28)
                  .append('-')
                  .append(newUUID, 28, 32)
                  .append('-')
                  .append(newUUID.substring(32))
                  .toString()
                  .toUpperCase(Locale.ENGLISH);
               newNreId = "Nre-" + newNreId;

               try (DataOutputStream out = new DataOutputStream(new FileOutputStream(nreIdFile))) {
                  out.writeUTF(newNreId);
               }

               return newNreId;
            } finally {
               if (nreIdFileLock != null) {
                  nreIdFileLock.unlock();
               }
            }
         } else {
            throw new RuntimeException("Failed to create NRE ID file - File already exists but couldn't be loaded.");
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed to resolve the existing NRE ID file", e);
      }
   }

   public static String generateNreIdForRemoteDevice(String licenseKey) {
      try {
         File subscriptionDb = new File(SUBSCRIPTION_DB_DIRECTORY, licenseKey);
         if (subscriptionDb.exists()) {
            deleteDirectoryContents(subscriptionDb);
         }
      } catch (IOException e) {
         logger.log(Level.SEVERE, "failed to generate NRE ID for remote device", e);
      }

      return establishNreId(getSubscriptionDbDirectory(licenseKey));
   }

   KeyRing getKeyRing(File securityDirectory) throws Exception {
      return KeyRingFactory.getInstance(securityDirectory, ".kr", ".km").getKeyRing();
   }

   KeyRing getKeyRing() throws Exception {
      return this.getKeyRing(SUBSCRIPTION_DIRECTORY);
   }

   protected synchronized boolean writeCertificate(XElem certificateElem) {
      return AccessController.doPrivileged(() -> {
         try {
            File certificateFile = new File(getSubscriptionCertificateDirectory(), this.getCertificateFilePrefix(certificateElem) + ".certificate");
            if (!certificateFile.exists()) {
               certificateElem.write(certificateFile);
               return true;
            }

            try (FileInputStream in = new FileInputStream(certificateFile)) {
               XElem existingCertificateElem = XParser.make(in).parse();
               long existingGenerated = this.parseDate(existingCertificateElem.get("generated"), true);
               long certificateGenerated = this.parseDate(certificateElem.get("generated"), true);
               if (existingGenerated < certificateGenerated) {
                  certificateElem.write(certificateFile);
                  return true;
               }
            }

            return false;
         } catch (Exception e) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.WARNING, "Write certificate file failed", e);
            } else {
               logger.log(Level.WARNING, "Write certificate file failed: " + e.getMessage());
            }

            return false;
         }
      });
   }

   private String getCertificateFilePrefix(XElem certificateElem) {
      return TextUtil.capitalize(certificateElem.get("vendor"));
   }

   public EntitlementApi.EntitlementStatus getLicenseUpdate() {
      createSubscriptionLicCertDirectory();
      if (getLicenseMode() != LicenseMode.SUBSCRIPTION) {
         String message = "This Host Id " + getNreId() + " is not configured for subscription licensing.";
         logger.info(message);
         return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.FAILURE, 200, message);
      } else {
         LicenseRefreshToken lrt = new LicenseRefreshToken();
         lrt.setNreId(getNreId());
         lrt.setProductId("station");
         lrt.updateRefreshIncrement();
         RetrieveEntitlements re = new RetrieveEntitlements(lrt);
         return this.getLicenseUpdate(re);
      }
   }

   public EntitlementApi.EntitlementStatus getLicenseUpdate(RetrieveEntitlements re) {
      EntitlementApi.EntitlementStatus status = re.entitlementsApi();
      if (status.isSuccess()) {
         XElem[] licenses = status.getLicenses().elems("licenses");
         if (licenses == null) {
            String message = "License request for Host Id " + getNreId() + " returned no licenses.";
            logger.info(message);
            return new EntitlementApi.EntitlementStatus(EntitlementApi.EntitlementState.FAILURE, 200, message);
         }

         HashSet<String> vendors = new HashSet<>();

         for (XElem license : licenses) {
            String vendor = license.get("vendor");
            if (vendor != null) {
               File certificateFile = new File(getSubscriptionCertificateDirectory(), vendor + ".certificate");
               if (!certificateFile.exists()) {
                  vendors.add(vendor);
               }
            }
         }

         if (!vendors.isEmpty()) {
            RequestCertificates rc = new RequestCertificates();
            EntitlementApi.EntitlementStatus certStatus = rc.getCertificatesApi(getNreId(), vendors.toArray(new String[0]), rc.getCertificateVersion(), false);
            if (certStatus.isSuccess()) {
               for (XElem certificate : certStatus.getCertificates().elems("certificates")) {
                  status.addCertificate(certificate);
               }
            }
         }
      }

      return status;
   }

   public synchronized boolean writeLicense(XElem licenseElem) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(EntitlementUtil.ENTITLEMENT_WORKFLOW_PERMISSION);
      }

      return AccessController.doPrivileged(() -> {
         try {
            File licenseFile = new File(getSubscriptionLicenseDirectory(), this.getLicenseFilePrefix(licenseElem) + ".license");
            if (!licenseFile.exists()) {
               licenseElem.write(licenseFile);
               if (logger.isLoggable(Level.FINE)) {
                  logger.log(Level.FINE, "Write license file success for new license " + licenseFile.getName());
               }

               return true;
            } else {
               try (FileInputStream in = new FileInputStream(licenseFile)) {
                  XElem existingLicenseElem = XParser.make(in).parse();
                  if (!this.getLicenseSignature(existingLicenseElem).equals(this.getLicenseSignature(licenseElem))) {
                     licenseElem.write(licenseFile);
                     if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Write license file success for updated license " + licenseFile.getName());
                     }

                     return true;
                  }
               }

               return false;
            }
         } catch (Exception e) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.WARNING, "Write license file failed", e);
            } else {
               logger.log(Level.WARNING, "Write license file failed: " + e.getMessage());
            }

            return false;
         }
      });
   }

   private String getLicenseFilePrefix(XElem licenseElem) {
      for (XElem feature : licenseElem.elems("feature")) {
         if ("brand".equals(feature.get("name", null))) {
            String result = feature.get("brandId", null);
            if (result != null) {
               return TextUtil.capitalize(result);
            }
         }
      }

      return TextUtil.capitalize(licenseElem.get("vendor"));
   }

   private String getLicenseSignature(XElem licenseElem) {
      XElem sigElem = licenseElem.elem("signature");
      return sigElem != null ? sigElem.string() : "";
   }

   private long parseDate(String date, boolean startOfDay) {
      if (date.equalsIgnoreCase("never")) {
         return Long.MAX_VALUE;
      }

      try {
         StringTokenizer st = new StringTokenizer(date, "- ");
         int year = Integer.parseInt(st.nextToken()) - 1900;
         int month = Integer.parseInt(st.nextToken()) - 1;
         int dayOfMonth = Integer.parseInt(st.nextToken());
         int hourOfDay = startOfDay ? 0 : 23;
         int minute = startOfDay ? 0 : 59;
         int second = startOfDay ? 0 : 59;
         Date d = new GregorianCalendar(year + 1900, month, dayOfMonth, hourOfDay, minute, second).getTime();
         return d.getTime();
      } catch (Exception e) {
         throw new RuntimeException("Invalid license date format yyyy-MM-dd: " + date);
      }
   }

   public static boolean isKeyValid(String key) {
      return getSubscriptionKeyPattern().matcher(key).matches();
   }

   public static boolean isSubscriptionLicensingSupported() {
      return SubscriptionLicenseUtil.SubscriptionLicenseUtilHolder.IS_SUBSCRIPTION_LICENSING_SUPPORTED;
   }

   public static Pattern getSubscriptionKeyPattern() {
      return SubscriptionLicenseUtil.SubscriptionLicenseUtilHolder.SUBSCRIPTION_KEY_PATTERN;
   }

   public static boolean loadIsSubscriptionLicensingSupported() {
      return AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.license.subscriptionLicenseAllowed"));
   }

   static final class LocalHostMetaDataHolder {
      static final boolean REQUIRE_SUBSCRIPTION = SubscriptionLicenseUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.requireSubscription();
      static final String HOST_MODEL = SubscriptionLicenseUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getHostModel();
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }

   private static class SubscriptionLicenseUtilHolder {
      public static final SubscriptionLicenseUtil _SUBSCRIPTION_LICENSE_UTIL_INSTANCE = new SubscriptionLicenseUtil();
      public static final boolean IS_SUBSCRIPTION_LICENSING_SUPPORTED = SubscriptionLicenseUtil.loadIsSubscriptionLicensingSupported();
      public static final Pattern SUBSCRIPTION_KEY_PATTERN = Pattern.compile("[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}");
   }
}
