package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.crypto.DaemonCryptoManager;
import com.tridium.niagarad.license.Brand;
import com.tridium.niagarad.license.Feature;
import com.tridium.niagarad.license.LicenseManager;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.NullLogger;
import com.tridium.niagarad.platform.PlatformInfo;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.subscription.SubscriptionLicenseUtil;
import com.tridium.nre.syslog.SyslogManager;
import com.tridium.nre.util.LicenseMode;
import com.tridium.nre.util.SupportLevel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class PlatformInfoServlet extends DaemonServlet {
   private final IPlatformProvider platformProvider;

   public PlatformInfoServlet(IPlatformProvider platformProvider) {
      super("platformInfo");
      this.platformProvider = platformProvider;
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      PlatformInfo platformInstance = PlatformInfo.getInstance();
      NiagaraDaemon niagaradInstance = NiagaraDaemon.getInstance();
      if (query != null && Boolean.parseBoolean(query.get("reset", "false"))) {
         LicenseManager.getInstance(NullLogger.getInstance()).reload(NullLogger.getInstance());
         platformInstance.reset();
         if (niagaradInstance.getStationRegistry() != null) {
            niagaradInstance.getStationRegistry().refreshMaxRunningAppCount();
         }
      }

      String daemonVersion = niagaradInstance.daemonVersion;
      String brandId = Brand.getBrandId(NullLogger.getInstance());

      long limit;
      try {
         limit = Long.parseLong(NiagaraDaemon.props.getProperty("failureRebootLimit", "3"));
      } catch (NumberFormatException nfe) {
         limit = 3L;
      }

      long limitPeriod;
      try {
         limitPeriod = Long.parseLong(NiagaraDaemon.props.getProperty("failureRebootLimitPeriod", "600000"));
      } catch (NumberFormatException nfe) {
         limitPeriod = 600000L;
      }

      boolean allowStationRestart = Boolean.parseBoolean(
         NiagaraDaemon.props.getProperty("allowStationRestart", String.valueOf(platformInstance.allowStationRestartDefault()))
      );
      boolean allowBrandChange = Boolean.parseBoolean(
         NiagaraDaemon.props.getProperty("allowBrandChange", String.valueOf(platformInstance.allowBrandChangeDefault()))
      );
      boolean isLicenseReadonly = platformInstance.isLicenseReadonly();
      boolean isSoftwareReadonly = platformInstance.isSoftwareReadonly();
      boolean isSystemTimeReadonly = platformInstance.isSystemTimeReadonly();
      boolean isNiagaraHomeReadonly = platformInstance.isNiagaraHomeReadonly();
      boolean requireSubscription = platformInstance.requireSubscription();
      content.w("<platformInfo")
         .w(' ')
         .attr("hostId", platformInstance.hostId())
         .w(' ')
         .attr("brandId", brandId == null ? "" : brandId)
         .w(' ')
         .attr("serialNumber", platformInstance.serialNumber())
         .w(' ')
         .attr("arch", platformInstance.archName())
         .w(' ')
         .attr("daemonVersion", daemonVersion)
         .w(' ')
         .attr("httpPort", String.valueOf(this.getServer().getHttpPort()))
         .w(' ')
         .attr("allowStationRestart", allowStationRestart ? "true" : "false")
         .w(' ')
         .attr("allowBrandChange", allowBrandChange ? "true" : "false")
         .w(' ')
         .attr("isLicenseReadonly", isLicenseReadonly ? "true" : "false")
         .w(' ')
         .attr("isSoftwareReadonly", isSoftwareReadonly ? "true" : "false")
         .w(' ')
         .attr("isSystemTimeReadonly", isSystemTimeReadonly ? "true" : "false")
         .w(' ')
         .attr("isNiagaraHomeReadonly", isNiagaraHomeReadonly ? "true" : "false")
         .w(' ')
         .attr("requireSubscription", requireSubscription ? "true" : "false")
         .w(' ')
         .attr("failureRebootLimit", String.valueOf(limit))
         .w(' ')
         .attr("failureRebootLimitPeriod", String.valueOf(limitPeriod))
         .w(' ')
         .attr("configDistFileName", platformInstance.configDistFileName())
         .w(' ')
         .attr("maxStations", String.valueOf(platformInstance.maxStations()))
         .w(' ')
         .attr("maxRunningStations", String.valueOf(platformInstance.maxRunningStations()))
         .w(' ')
         .attr("npsdk", String.valueOf(PlatformUtil.isNpsdkPlatform()))
         .w(">\n");

      for (XElem elem : platformInstance.partElems()) {
         elem.write(content, 2);
      }

      content.w("  <runtimeProfileSupport>");
      Set<String> requiredProfiles = new HashSet<>();
      Collections.addAll(requiredProfiles, this.platformProvider.getRequiredRuntimeProfiles().split(","));
      Set<String> supportedProfiles = new HashSet<>();
      Collections.addAll(supportedProfiles, this.platformProvider.getSupportedRuntimeProfiles().split(","));
      Set<String> enabledProfiles = new HashSet<>();
      Collections.addAll(
         enabledProfiles, NiagaraDaemon.props.getProperty("runtimeProfilesEnabled", this.platformProvider.getRequiredRuntimeProfiles()).split(",")
      );
      RuntimeProfile[] knownProfiles = RuntimeProfile.values();

      for (RuntimeProfile currentProfile : knownProfiles) {
         XElem currentProfileElem = new XElem("profile");
         currentProfileElem.addAttr("name", currentProfile.name());
         if (requiredProfiles.contains(currentProfile.name())) {
            currentProfileElem.addAttr("supportLevel", SupportLevel.required.name());
         } else if (supportedProfiles.contains(currentProfile.name())) {
            currentProfileElem.addAttr("supportLevel", SupportLevel.optional.name());
            currentProfileElem.addAttr("enabled", String.valueOf(enabledProfiles.contains(currentProfile.name())));
         } else {
            currentProfileElem.addAttr("supportLevel", SupportLevel.unsupported.name());
         }

         currentProfileElem.write(content, 2);
      }

      content.w("  </runtimeProfileSupport>").nl();
      content.w("  <cpu")
         .w(' ')
         .attr("nCpus", String.valueOf(Runtime.getRuntime().availableProcessors()))
         .w(' ')
         .attr("currentUtilization", String.valueOf(this.platformProvider.getCurrentCPUUtilization()))
         .w(' ')
         .attr("overallUtilization", String.valueOf(this.platformProvider.getOverallCPUUtilization()))
         .w("/>")
         .nl();
      content.w("  <physicalMemory")
         .w(' ')
         .attr("freeBytes", String.valueOf(this.platformProvider.getFreePhysicalMemoryBytes()))
         .w(' ')
         .attr("totalBytes", String.valueOf(this.platformProvider.getTotalPhysicalMemoryBytes()))
         .w("/>")
         .nl();
      String[] fsNames = this.platformProvider.getAllFileSystemNames();
      content.w("  <filesystems>").nl();

      for (String fsName : fsNames) {
         String fsDisplayName = this.platformProvider.getFileSystemDisplayName(fsName);
         content.w("    <filesystem")
            .w(' ')
            .attr("name", fsName)
            .w(' ')
            .attr("displayName", fsDisplayName != null ? fsDisplayName : "")
            .w(' ')
            .attr("totalBytes", String.valueOf(this.platformProvider.getTotalBytes(fsName)))
            .w(' ')
            .attr("freeBytes", String.valueOf(this.platformProvider.getFreeBytes(fsName)))
            .w(' ')
            .attr("maxFileCount", String.valueOf(this.platformProvider.getMaxFileCount(fsName)))
            .w(' ')
            .attr("currentFileCount", String.valueOf(this.platformProvider.getCurrentFileCount(fsName)))
            .w("/>")
            .nl();
      }

      content.w("  </filesystems>").nl();
      content.w("  <tzSupport").w(' ').attr("dayMode", "full").w("/>").nl();
      content.w("<sslSupported ");
      addSSLAttr(content, "sslEnabled", "true").w(' ');
      addSSLAttr(content, "sslOnly", DaemonCryptoManager.DEFAULT_SSL_ONLY).w(' ');
      addSSLAttr(content, "sslEnabledStateReadonly", DaemonCryptoManager.SSL_ENABLED_READONLY_VALUE).w(' ');
      addSSLAttr(content, "sslPort", "5011").w(' ');
      addSSLAttr(content, "keyAlias", "default").w(' ');
      addSSLAttr(content, "tlsCipherSuiteGroup", DaemonCryptoManager.DEFAULT_TLS_CIPHER_SUITE_GROUP).w(' ');
      addSSLAttr(content, "tlsUseExtendedMasterSecret", NiagaraDaemon.props.getProperty("tlsUseExtendedMasterSecret")).w(' ');
      if (SecurityInitializer.getInstance().isFips()) {
         addSSLAttr(content, "fipsMode", "true").w(' ');
         addSSLAttr(content, "sslAlgType", "tlsv1_3").w("/>");
      } else {
         addSSLAttr(content, "sslAlgType", "tlsv1_3").w("/>");
      }

      content.w("  <hostIdSupport")
         .w(' ')
         .attr("licenseMode", String.valueOf(SubscriptionLicenseUtil.getLicenseMode()))
         .w(' ')
         .attr("hostIdStatus", SubscriptionLicenseUtil.getHostIdStatus())
         .w(' ')
         .attr("perpetualHostId", platformInstance.hostId(LicenseMode.PERPETUAL))
         .w("/>")
         .nl();
      Feature syslogFeature = LicenseManager.getInstance(NullLogger.getInstance()).getFeature("tridium", "syslog");
      SyslogManager manager = SyslogManager.getInstance();
      if (syslogFeature != null && syslogFeature.check()) {
         content.w("  <syslogSupported")
            .w(' ')
            .attr("enabled", String.valueOf(manager.getEnabled()))
            .w(' ')
            .attr("isReadonly", String.valueOf(manager.getIsReadonly()))
            .w(' ')
            .attr("serverHost", manager.getServerHost())
            .w(' ')
            .attr("serverPort", String.valueOf(manager.getServerPort()))
            .w(' ')
            .attr("messageType", manager.getMessageType().name().toLowerCase(Locale.ENGLISH))
            .w(' ')
            .attr("transportProtocol", manager.getTransportProtocol().name().toLowerCase(Locale.ENGLISH))
            .w(' ')
            .attr("clientAlias", manager.getClientAlias())
            .w(' ')
            .attr("platformLogEnabled", String.valueOf(manager.getPlatformLogEnabled()))
            .w(' ')
            .attr("stationLogEnabled", String.valueOf(manager.getStationLogEnabled()))
            .w(' ')
            .attr("logLevelFilter", manager.getLogLevelFilter().getName().toLowerCase(Locale.ENGLISH))
            .w(' ')
            .attr("stationAuditEnabled", String.valueOf(manager.getStationAuditEnabled()))
            .w(' ')
            .attr("securityAuditEnabled", String.valueOf(manager.getSecurityAuditEnabled()))
            .w(' ')
            .attr("facility", manager.getFacility().name().toLowerCase(Locale.ENGLISH))
            .w(' ')
            .attr("queueSize", String.valueOf(manager.getQueueSize()))
            .w("/>")
            .nl();
      } else if (manager.getEnabled()) {
         try {
            manager.writeSyslogConfig(
               "false",
               manager.getServerHost(),
               String.valueOf(manager.getServerPort()),
               manager.getMessageType().name().toLowerCase(Locale.ENGLISH),
               manager.getTransportProtocol().name().toLowerCase(Locale.ENGLISH),
               manager.getClientAlias(),
               manager.getClientPassword(),
               String.valueOf(manager.getPlatformLogEnabled()),
               String.valueOf(manager.getStationLogEnabled()),
               manager.getLogLevelFilter().getName(),
               String.valueOf(manager.getStationAuditEnabled()),
               String.valueOf(manager.getSecurityAuditEnabled()),
               manager.getFacility().name().toLowerCase(Locale.ENGLISH),
               String.valueOf(manager.getQueueSize())
            );
            NiagaraDaemon.getFilter().warning("syslog is not licensed, this feature has been disabled.");
         } catch (Exception e) {
            NiagaraDaemon.getFilter().log(Level.SEVERE, "syslog is not licensed, failed to disable this feature.", e);
         }
      }

      content.w("</platformInfo>").nl();
      return 200;
   }

   private static XWriter addSSLAttr(XWriter content, String name, String defaultValue) {
      content.attr(name, NiagaraDaemon.props.getProperty(name, defaultValue));
      return content;
   }
}
