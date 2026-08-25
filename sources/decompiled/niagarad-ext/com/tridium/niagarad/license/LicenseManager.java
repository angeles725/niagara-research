package com.tridium.niagarad.license;

import com.tridium.nre.subscription.SubscriptionLicenseUtil;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.NiagaraFiles;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LicenseManager {
   private static LicenseManager INSTANCE = null;
   public long tridiumGeneratedDate;
   private ArrayList<Feature> features = null;
   private ArrayList<LicenseFile> licenses = null;
   static final ArrayList<String> VENDOR_WHITELIST = new ArrayList<>();
   static final ArrayList<String> FEATURE_WHITELIST = new ArrayList<>();

   public static LicenseManager getInstance(Logger log) {
      if (INSTANCE == null) {
         INSTANCE = new LicenseManager(log);
      }

      return INSTANCE;
   }

   private LicenseManager(Logger log) {
      this.tridiumGeneratedDate = -1L;
      this.load(log);
   }

   private void load(Logger log) {
      if (this.features != null) {
         this.features.clear();
         this.features = null;
      }

      if (this.licenses != null) {
         this.licenses.clear();
         this.licenses = null;
      }

      this.licenses = new ArrayList<>();
      this.features = new ArrayList<>();
      File licenseDirFile;
      if (SubscriptionLicenseUtil.getLicenseMode().equals(LicenseMode.SUBSCRIPTION)) {
         licenseDirFile = NiagaraFiles.getSubscriptionLicensePath();
      } else {
         licenseDirFile = NiagaraFiles.getPerpetualLicensePath();
      }

      boolean createDirectory = false;
      if (!licenseDirFile.exists()) {
         createDirectory = true;
      } else if (!licenseDirFile.isDirectory()) {
         log.warning("license directory \"" + licenseDirFile.getPath() + "\" is not a directory, attempting to correct");
         if (!licenseDirFile.delete()) {
            log.severe("failed to delete license \"" + licenseDirFile.getPath() + "\", user must manually delete file");
            return;
         }

         createDirectory = true;
      }

      if (createDirectory && !licenseDirFile.mkdirs()) {
         log.severe("error loading licenses, could not create directory");
      } else {
         File[] kids = licenseDirFile.listFiles();
         if (kids == null) {
            log.severe("error loading licenses, \"" + licenseDirFile.getPath() + "\" list files returned null");
         } else {
            if (log.isLoggable(Level.FINE)) {
               log.fine("loading licenses from \"" + licenseDirFile.getPath() + "\"...");
            }

            for (File kid : kids) {
               if (kid.getName().endsWith(".license")) {
                  LicenseFile license = new LicenseFile(kid.getPath());

                  try {
                     license.load(this, null);
                  } catch (Exception e) {
                     license.error = e.toString();
                  }

                  StringBuilder buffer = new StringBuilder();
                  if (license.error != null) {
                     buffer.append("error loading license \"").append(kid.getPath()).append("\" (").append(license.error).append(")");
                     log.warning(buffer.toString());
                  } else {
                     this.licenses.add(license);
                     if (log.isLoggable(Level.FINE)) {
                        buffer.append("loaded license \"").append(kid.getPath()).append("\"");
                        log.fine(buffer.toString());
                     }
                  }
               } else if (kid.getName().endsWith(".lar")) {
                  try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(kid))) {
                     for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                        if (entry.getName().endsWith(".license")) {
                           LicenseFile license = new LicenseFile(kid.getPath() + "$" + entry.getName());

                           try {
                              license.load(this, zipIn);
                           } catch (Exception e) {
                              license.error = e.toString();
                           }

                           StringBuilder buffer = new StringBuilder();
                           if (license.error != null) {
                              buffer.append("error loading license \"").append(kid.getPath()).append("\" (").append(license.error).append(")");
                              log.warning(buffer.toString());
                           } else {
                              this.licenses.add(license);
                              if (log.isLoggable(Level.FINE)) {
                                 buffer.append("loaded license \"").append(kid.getPath()).append("\"");
                                 log.fine(buffer.toString());
                              }
                           }
                        }

                        zipIn.closeEntry();
                     }
                  } catch (IOException e) {
                     log.warning("error loading license \"" + kid.getPath() + "\" (" + e + ")");
                  }
               }
            }
         }
      }
   }

   public void reload(Logger log) {
      Brand.unload();
      this.load(log);
   }

   public static void unload() {
      Brand.unload();
   }

   public Feature getFeature(String vendor, String feature) {
      if (VENDOR_WHITELIST.contains(vendor.toLowerCase(Locale.ENGLISH)) && FEATURE_WHITELIST.contains(feature.toLowerCase(Locale.ENGLISH))) {
         String key = LicenseUtil.toKey(vendor, feature);
         if (this.features != null) {
            for (Feature currentFeature : this.features) {
               if (currentFeature.key.equalsIgnoreCase(key)) {
                  return currentFeature;
               }
            }
         }

         return null;
      } else {
         throw new RuntimeException("Requested feature '" + vendor + ":" + feature + "' not found in whitelist, would be ignored during load!");
      }
   }

   public Feature checkFeature(String vendor, String feature) {
      Feature result = this.getFeature(vendor, feature);
      if (result == null) {
         return null;
      } else {
         return result.check() ? result : null;
      }
   }

   public void addFeature(Feature feature) {
      if (VENDOR_WHITELIST.contains(feature.getVendorName().toLowerCase(Locale.ENGLISH))
         && FEATURE_WHITELIST.contains(feature.getFeatureName().toLowerCase(Locale.ENGLISH))) {
         Feature orig = this.getFeature(feature.getVendorName(), feature.getFeatureName());
         if (orig != null) {
            orig.merge(feature);
         } else if (this.features == null) {
            this.features = new ArrayList<>();
         } else {
            this.features.add(feature);
         }
      }
   }

   public LicenseFile getLicenseFile(String hostId, String vendor) {
      if (hostId != null && vendor != null && this.licenses != null && !this.licenses.isEmpty()) {
         for (LicenseFile licenseFile : this.licenses) {
            if (hostId.equals(licenseFile.hostId) && vendor.equals(licenseFile.vendor)) {
               return licenseFile;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static {
      VENDOR_WHITELIST.add("tridium".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("jre8qnx".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("jre8Qnx7Zulu".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("jre8J8000Azul".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("qnx7".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("globalCapacity".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("fips140-2".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("station".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("stationAzul".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("brand".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("smDeveloperMode".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("ieee8021x".toLowerCase(Locale.ENGLISH));
      FEATURE_WHITELIST.add("syslog".toLowerCase(Locale.ENGLISH));
   }
}
