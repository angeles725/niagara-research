package com.tridium.nre.subscription;

import com.tridium.json.JSONException;
import com.tridium.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

public class SubscriptionMetadataUtil {
   private static final Properties LICENSE_PROPERTIES = new Properties();
   private static final String REGISTRATION_METADATA_KEY_PREFIX = "registration.metadata.";
   private static final int PREFIX_LENGTH = 22;

   private SubscriptionMetadataUtil() {
   }

   public static synchronized void addMetadata(String key, String value) throws IOException {
      load();
      LICENSE_PROPERTIES.put(key, value);
      save();
   }

   public static synchronized void addRegistrationMetadata(String key, String value) throws IOException {
      load();
      LICENSE_PROPERTIES.put("registration.metadata." + key, value);
      save();
   }

   public static synchronized void addRegistrationMetadata(Map<String, String> metadataMap) throws IOException {
      load();
      metadataMap.forEach((metadataKey, metadataValue) -> LICENSE_PROPERTIES.put("registration.metadata." + metadataKey, metadataValue));
      save();
   }

   public static synchronized void removeRegistrationMetadata(String propertyKey) throws IOException {
      load();
      LICENSE_PROPERTIES.remove("registration.metadata." + propertyKey);
      save();
   }

   public static synchronized void removeRegistrationMetadata(String[] propertyKeys) throws IOException {
      load();

      for (String propertyKey : propertyKeys) {
         LICENSE_PROPERTIES.remove("registration.metadata." + propertyKey);
      }

      save();
   }

   public static JSONObject getRegistrationMetadataJson() throws IOException {
      load();
      JSONObject metadataJson = new JSONObject();
      LICENSE_PROPERTIES.forEach((propertyKey, propertyValue) -> {
         if (propertyKey.toString().startsWith("registration.metadata.")) {
            metadataJson.put(propertyKey.toString().substring(22), propertyValue);
         }
      });
      return metadataJson;
   }

   public static synchronized JSONObject getRegistrationMetadataJson(Map<String, String> metadataMap) throws IOException, JSONException {
      JSONObject metadataJson = getRegistrationMetadataJson();
      metadataMap.forEach(metadataJson::put);
      return metadataJson;
   }

   private static synchronized void load() throws IOException {
      File workingLicensePropertiesFile = new File(SubscriptionLicenseUtil.LICENSE_PROPERTIES_FILE + ".working");
      if (workingLicensePropertiesFile.exists() && !SubscriptionLicenseUtil.LICENSE_PROPERTIES_FILE.exists()) {
         try {
            Files.move(workingLicensePropertiesFile.toPath(), SubscriptionLicenseUtil.LICENSE_PROPERTIES_FILE.toPath(), StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException ioe) {
            throw new IOException("Failure during save. Unable to rename working properties file to original.");
         }
      }

      LICENSE_PROPERTIES.clear();
      File LICENSE_PROPERTIES_FILE = SubscriptionLicenseUtil.getLicensePropertiesFile();

      try (FileInputStream in = new FileInputStream(LICENSE_PROPERTIES_FILE)) {
         LICENSE_PROPERTIES.load(in);
      } catch (IOException e) {
         throw new IOException("Unable to load properties from file.", e);
      }
   }

   private static synchronized void save() throws IOException {
      File LICENSE_PROPERTIES_FILE = SubscriptionLicenseUtil.getLicensePropertiesFile();
      File workingLicensePropertiesFile = new File(LICENSE_PROPERTIES_FILE + ".working");

      try (FileOutputStream out = new FileOutputStream(workingLicensePropertiesFile)) {
         LICENSE_PROPERTIES.store(out, "Auto-generated file, do not modify.");
         out.getFD().sync();
      } catch (IOException e) {
         throw new IOException("Unable to save properties to working file.", e);
      }

      try {
         Files.move(workingLicensePropertiesFile.toPath(), LICENSE_PROPERTIES_FILE.toPath(), StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
         throw new IOException("Failure to save. Unable to rename working properties file to original.", e);
      }
   }

   public static String getRegistrationMetadata(String key) throws IOException {
      load();
      return LICENSE_PROPERTIES.getProperty("registration.metadata." + key);
   }

   public static boolean containsRegistrationMetadata(String key) throws IOException {
      load();
      return LICENSE_PROPERTIES.containsKey("registration.metadata." + key);
   }
}
