package com.tridium.niagarad.platform;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.license.Feature;
import com.tridium.niagarad.license.LicenseManager;
import com.tridium.niagarad.log.NullLogger;
import com.tridium.nre.bootstrap.Bootstrap;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.FipsInformation;
import com.tridium.nre.security.HsmManagerImpl;
import com.tridium.nre.security.ISecurityInitializer;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class PlatformInfo {
   private static final String NIAGARA_JRE_HOME = AccessController.doPrivileged(() -> System.getenv("NIAGARA_JRE_HOME"));
   private List<XElem> mPartElems = null;
   private long mMaxStations;
   private long mMaxRunningStations;
   private String mConfigDistFileName = null;
   private final IPlatformProvider platformProvider = PlatformUtil.getPlatformProvider();
   private ArrayList<String> mWarningDescriptions;
   private static final Set<String> UNKNOWN_VERSION_STRINGS = new HashSet<>(Arrays.asList(new Version("0.0").toString()));

   private PlatformInfo() {
      this.init();
   }

   private static PlatformInfo loadPlatformInfo() {
      return new PlatformInfo();
   }

   public static PlatformInfo getInstance() {
      return PlatformInfo.PlatformInfoHolder.PLATFORM_INFO_INSTANCE;
   }

   public void reset() {
      this.mPartElems.clear();
      this.mPartElems = null;
      this.init();
   }

   protected void init() {
      this.mWarningDescriptions = new ArrayList<>();
      this.mPartElems = new ArrayList<>();
      XElem elem = new XElem("os");
      String vendor = this.getVendor();
      if (vendor.equalsIgnoreCase("tridium")) {
         elem.setAttr("name", this.platformProvider.getOsName());
      } else {
         elem.setAttr("name", vendor + "-" + this.platformProvider.getOsName());
      }

      elem.setAttr("desc", this.platformProvider.getOsDescription());
      String osVersion = this.platformProvider.getOsVersion();
      elem.setAttr("version", osVersion);
      if (this.platformProvider.isOsInstallable()) {
         elem.setAttr("installable", "true");
      }

      if (UNKNOWN_VERSION_STRINGS.contains(osVersion)) {
         String warningMessage = "Unknown OS version '" + osVersion + "'";
         NiagaraDaemon.getFilter().warning(warningMessage);
         this.mWarningDescriptions.add(warningMessage);
      }

      this.mPartElems.add(elem);
      String jreVersionPath = this.getJreVersionPath();
      elem = null;
      File file;
      if ((file = new File(jreVersionPath)).exists()) {
         try {
            XParser parser = XParser.make(file);
            elem = parser.parse(true);
         } catch (Exception e) {
            String warningMessage = "JRE version file '" + jreVersionPath + "' failed to parse as an xml element, reverting to fallback values";
            NiagaraDaemon.getFilter().severe(warningMessage);
            this.mWarningDescriptions.add(warningMessage);
         }
      } else {
         String warningMessage = "JRE version file '" + jreVersionPath + "' does not exist, reverting to fallback values";
         NiagaraDaemon.getFilter().warning(warningMessage);
         this.mWarningDescriptions.add(warningMessage);
      }

      if (elem == null) {
         elem = new XElem("vm");
         elem.setAttr("name", "Undetermined VM Name");
         elem.setAttr("desc", "Undetermined VM Description");
         elem.setAttr("version", "Undetermined VM Version");
         elem.setAttr("vendor", "Undetermined Vendor");
      }

      this.mPartElems.add(elem);
      String nreVersionPath = NiagaraDaemon.NIAGARA_HOME + File.separator + "bin" + File.separator + "nreVersion.xml";
      elem = null;
      if ((file = new File(nreVersionPath)).exists()) {
         try {
            XParser parser = XParser.make(file);
            elem = parser.parse(true);
         } catch (Exception e) {
            String warningMessage = "NRE version file '" + nreVersionPath + "' failed to parse as an xml element, reverting to fallback values";
            NiagaraDaemon.getFilter().severe(warningMessage);
            this.mWarningDescriptions.add(warningMessage);
         }
      } else {
         String warningMessage = "NRE version file '" + nreVersionPath + "' does not exist, reverting to fallback values";
         NiagaraDaemon.getFilter().warning(warningMessage);
         this.mWarningDescriptions.add(warningMessage);
      }

      if (elem == null) {
         String osName = TextUtil.toLowerCase(this.platformProvider.getOsName());
         elem = new XElem("nre");
         elem.setAttr("name", "nre-core-" + osName + "-???");
         elem.setAttr("desc", "Niagara Core for " + osName + "???");
         elem.setAttr("version", "4.0.???");
      }

      this.mPartElems.add(elem);
      elem = new XElem("model");
      elem.setAttr("name", this.platformProvider.getHostModel());
      elem.setAttr("desc", this.platformProvider.getHostProduct());
      elem.setAttr("version", this.platformProvider.getHostModelVersion());
      this.mPartElems.add(elem);

      try {
         if (SecurityInitializer.getInstance().isFips()) {
            FipsInformation fipsInfo = SecurityInitializer.getInstance().getFipsInformation();
            String fipsName = "FIPS_140-" + fipsInfo.getFipsVersion();
            String fipsDescription = fipsName + " mode for Niagara";
            elem = new XElem("part");
            elem.setAttr("name", fipsName);
            elem.setAttr("version", Integer.toString(fipsInfo.getNiagaraVersion()));
            elem.setAttr("description", fipsDescription);
            elem.setAttr("revision", fipsInfo.getFipsRevisionDate().toString());
            this.mPartElems.add(elem);
         }
      } catch (Throwable t) {
         String warningMessage = "Failed to instantiate FipsInformation, skipping FIPS part creation";
         NiagaraDaemon.getFilter().log(Level.SEVERE, warningMessage, t);
         this.mWarningDescriptions.add(warningMessage);
      }

      try {
         if (!((ISecurityInitializer)Bootstrap.getInstantiator().instance(ISecurityInitializer.class)).isFips()) {
            HsmManagerImpl instance = HsmManagerImpl.make(ClassLoader.getSystemClassLoader());
            if (instance.hasHsm()) {
               Map<String, String> hsmProperties = instance.getProperties();
               elem = new XElem("part");
               if (hsmProperties != null && instance.hasHsmEngine()) {
                  elem.setAttr("name", hsmProperties.getOrDefault("hsm_partname", "hsm-???"));
                  elem.setAttr("vendor", hsmProperties.getOrDefault("hsm_vendor", ""));
                  elem.setAttr("version", hsmProperties.getOrDefault("hsm_version", "???"));
                  elem.setAttr("description", hsmProperties.getOrDefault("hsm_desc", ""));
               } else {
                  elem.setAttr("name", "hsm-???");
                  elem.setAttr("vendor", "");
                  elem.setAttr("version", "???");
                  elem.setAttr("description", "");
               }

               this.mPartElems.add(elem);
            }
         }
      } catch (Throwable t) {
         String warningMessage = "Failed to instantiate HsmManager, skipping HSM part creation";
         NiagaraDaemon.getFilter().log(Level.SEVERE, warningMessage, t);
         this.mWarningDescriptions.add(warningMessage);
      }

      try {
         String extraHostParts = this.platformProvider.getHostParts();
         if (!extraHostParts.isEmpty()) {
            XElem extraHostPartsElem = XParser.make(extraHostParts).parse();
            XElem[] extraPartElems = extraHostPartsElem.elems("part");
            if (extraPartElems.length != 0) {
               for (XElem extraPartElem : extraPartElems) {
                  if (NiagaraDaemon.getFilter().isLoggable(Level.FINE)) {
                     NiagaraDaemon.getFilter()
                        .fine("platform info adding extra part \"" + extraPartElem.get("name") + " (" + extraPartElem.get("version") + ")\"");
                  }

                  this.mPartElems.add(extraPartElem);
               }
            }
         }
      } catch (Throwable t) {
         String warningMessage = "Failed to create extra host parts element, skipping parts";
         NiagaraDaemon.getFilter().log(Level.SEVERE, warningMessage, t);
         this.mWarningDescriptions.add(warningMessage);
      }

      this.refreshMaxRunningAppCounts();
   }

   public String hostId() {
      return this.platformProvider.getHostId();
   }

   public String hostId(LicenseMode licenseMode) {
      return this.platformProvider.getHostId(licenseMode);
   }

   public String serialNumber() {
      return this.platformProvider.getHostSerialNumber();
   }

   public String archName() {
      return this.platformProvider.getOsArchitecture();
   }

   public boolean allowStationRestartDefault() {
      return this.platformProvider.getAllowStationRestartDefault();
   }

   public boolean allowBrandChangeDefault() {
      return this.platformProvider.getAllowBrandChangeDefault();
   }

   public boolean isLicenseReadonly() {
      return this.platformProvider.isLicenseReadonly();
   }

   public boolean isSoftwareReadonly() {
      return this.platformProvider.isSoftwareReadonly();
   }

   public boolean isNiagaraHomeReadonly() {
      return this.platformProvider.isNiagaraHomeReadonly();
   }

   public boolean requireSubscription() {
      return this.platformProvider.requireSubscription();
   }

   public boolean isSystemTimeReadonly() {
      return this.platformProvider.isSystemTimeReadonly();
   }

   public void refreshMaxRunningAppCounts() {
      if (PlatformUtil.isTridiumPlatform()) {
         if (this.platformProvider.isEmbedded()) {
            this.mMaxStations = 1L;
            this.mMaxRunningStations = 1L;
         } else {
            this.mMaxStations = 65535L;
            Feature station = LicenseManager.getInstance(NullLogger.getInstance()).getFeature("tridium", "station");
            if (station == null) {
               this.mMaxRunningStations = 1L;
            } else if (!station.check()) {
               this.mMaxRunningStations = 1L;
            } else {
               try {
                  this.mMaxRunningStations = station.geti("station.limit", 32);
               } catch (NumberFormatException nfe) {
                  String stationLimit = station.get("station.limit");
                  if (stationLimit.equalsIgnoreCase("none")) {
                     this.mMaxRunningStations = 2147483647L;
                  } else {
                     NiagaraDaemon.getFilter().severe("invalid station.limit value \"" + stationLimit + "\", defaulting to 32");
                  }
               }
            }
         }
      } else {
         this.mMaxStations = 1L;
         this.mMaxRunningStations = 1L;
      }
   }

   public String configDistFileName() {
      if (this.mConfigDistFileName == null) {
         String vendor = this.getVendor();
         if (vendor.equalsIgnoreCase("tridium")) {
            this.mConfigDistFileName = "nre-config-*.dist";
         } else {
            this.mConfigDistFileName = "nre-config-" + this.getVendor() + "-*.dist";
         }

         String brandPropsPath = NiagaraDaemon.NIAGARA_HOME + File.separator + "etc" + File.separator + "brand.properties";
         File brandPropsFile = new File(brandPropsPath);

         try (FileInputStream fin = new FileInputStream(brandPropsFile)) {
            Properties brandProps = new Properties();
            brandProps.load(fin);
            this.mConfigDistFileName = brandProps.getProperty("install.config", this.mConfigDistFileName);
         } catch (IOException var17) {
         }
      }

      return this.mConfigDistFileName;
   }

   public String getVendor() {
      return this.platformProvider.getHostVendor();
   }

   public long maxStations() {
      return this.mMaxStations;
   }

   public long maxRunningStations() {
      return this.mMaxRunningStations;
   }

   public String getJreVersionPath() {
      return NIAGARA_JRE_HOME == null
         ? NiagaraDaemon.NIAGARA_HOME + File.separator + "jre" + File.separator + "jreVersion.xml"
         : NIAGARA_JRE_HOME + File.separator + "jreVersion.xml";
   }

   public List<XElem> partElems() {
      return this.mPartElems;
   }

   public ArrayList<String> warningDescriptions() {
      return this.mWarningDescriptions;
   }

   private static class PlatformInfoHolder {
      public static final PlatformInfo PLATFORM_INFO_INSTANCE = PlatformInfo.loadPlatformInfo();
   }
}
