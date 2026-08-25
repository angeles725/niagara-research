package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.PlatformUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.FileUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DhcpdServlet extends DaemonServlet {
   private static final String DHCPD_ADAPTER_WIFI_SAP = "tiw_sap0";
   private static final String SAP = "sap";
   private static final String EN = "en";
   private static final String DM = "dm";
   private static final String WIFI_MONITOR_PATH = "/var/run/wilink";
   protected static Logger filter;
   public static final String DHCPD_PROVIDER_DHCPD = "dhcpd";
   public static final String DHCPD_PROVIDER_DNSMASQ = "dnsmasq";
   private static String DHCPD_PROVIDER;
   private static String DHCPD_CONF_PATH;
   private static String DHCPD_LEASE_PATH;
   private static String DHCPD_ADAPTERS_LIST_PATH;
   private static String DHCPD_SUPPORTED_ADAPTERS;
   public static final String DHCPD_SUPPORTED_PROPERTY = "niagara.dhcpd.supported";
   private static final String DHCPD_PROVIDER_PROPERTY = "niagara.dhcpd.provider";
   private static final String DHCPD_CONF_PATH_PROPERTY = "niagara.dhcpd.configurationFile";
   private static final String DHCPD_LEASE_PATH_PROPERTY = "niagara.dhcpd.leaseFile";
   private static final String DHCPD_ADAPTERS_LIST_PATH_PROPERTY = "niagara.dhcpd.adaptersEnabledFile";
   private static final String DHCPD_SUPPORTED_ADAPTERS_PROPERTY = "niagara.dhcpd.adaptersSupported";
   public static final String SERVLET_NAME = "dhcpd";
   public static final String SERVLET_PROVIDER_ATTR = "provider";
   public static final String SERVLET_CONF_FILE_ATTR = "conf_file_path";
   public static final String SERVLET_ADAPTERS_LIST_FILE_ATTR = "adapters_list_file_path";
   public static final String SERVLET_LEASE_FILE_ATTR = "lease_file_path";
   public static final String SERVLET_ADAPTERS_ELEM = "adapters";
   public static final String SERVLET_ADAPTER_ELEM = "adapter";
   public static final String SERVLET_NAME_ATTR = "name";
   public static final String SERVLET_ID_ATTR = "id";
   public static final String SERVLET_SUPPORTED_ATTR = "supported";
   public static final String SERVLET_ENABLED_ATTR = "enabled";

   public DhcpdServlet() {
      super("dhcpd");
   }

   @Override
   public boolean doInit() {
      filter = Logger.getLogger("dhcpd");
      DHCPD_PROVIDER = readPropertyValue("niagara.dhcpd.provider", filter);
      DHCPD_CONF_PATH = readPropertyValue("niagara.dhcpd.configurationFile", filter);
      DHCPD_LEASE_PATH = readPropertyValue("niagara.dhcpd.leaseFile", filter);
      DHCPD_ADAPTERS_LIST_PATH = readPropertyValue("niagara.dhcpd.adaptersEnabledFile", filter);
      DHCPD_SUPPORTED_ADAPTERS = readPropertyValue("niagara.dhcpd.adaptersSupported", filter);
      if (DHCPD_PROVIDER != null && DHCPD_CONF_PATH != null && DHCPD_LEASE_PATH != null && DHCPD_ADAPTERS_LIST_PATH != null && DHCPD_SUPPORTED_ADAPTERS != null
         )
       {
         if (filter.isLoggable(Level.FINEST)) {
            filter.finest("using dhcp server configuration properties:");
            filter.finest("niagara.dhcpd.provider = " + DHCPD_PROVIDER);
            filter.finest("niagara.dhcpd.configurationFile = " + DHCPD_CONF_PATH);
            filter.finest("niagara.dhcpd.leaseFile = " + DHCPD_LEASE_PATH);
            filter.finest("niagara.dhcpd.adaptersEnabledFile = " + DHCPD_ADAPTERS_LIST_PATH);
            filter.finest("niagara.dhcpd.adaptersSupported = " + DHCPD_SUPPORTED_ADAPTERS);
         }

         if (!DHCPD_PROVIDER.equals("dhcpd") && !DHCPD_PROVIDER.equals("dnsmasq")) {
            filter.severe("dhcpd servlet configuration failed, unrecognized DHCPD provider '" + DHCPD_PROVIDER + "'");
            return false;
         }

         String relativeConfigurationFilePath = makeRelativeFilePath(DHCPD_CONF_PATH, filter);
         if (relativeConfigurationFilePath.equals(DHCPD_CONF_PATH)) {
            filter.severe("dhcpd servlet configuration failed, configuration file path is not relative to any niagara path");
            return false;
         }

         String relativeLeaseFilePath = makeRelativeFilePath(DHCPD_LEASE_PATH, filter);
         if (PlatformUtil.isNpsdkPlatform() && relativeLeaseFilePath.equals(DHCPD_LEASE_PATH)) {
            filter.severe("dhcpd servlet configuration failed, lease file path is not relative to any niagara path");
            return false;
         }

         String relativeAdaptersListFilePath = makeRelativeFilePath(DHCPD_ADAPTERS_LIST_PATH, filter);
         if (relativeAdaptersListFilePath.equals(DHCPD_ADAPTERS_LIST_PATH)) {
            filter.severe("dhcpd servlet configuration failed, adapters list path is not relative to any niagara path");
            return false;
         }

         File configurationFilePathParent = new File(DHCPD_CONF_PATH).getParentFile();
         if (!configurationFilePathParent.exists() && !configurationFilePathParent.mkdirs()) {
            filter.severe("dhcpd servlet configuration failed, configuration file path parent directory did not exist and servlet failed to create it");
         }

         File leasesFilePathParent = new File(DHCPD_LEASE_PATH).getParentFile();
         if (!leasesFilePathParent.exists() && !leasesFilePathParent.mkdirs()) {
            filter.severe("dhcpd servlet configuration failed, lease file path parent directory did not exist and servlet failed to create it");
         }

         File adaptersListFilePathParent = new File(DHCPD_ADAPTERS_LIST_PATH).getParentFile();
         if (!adaptersListFilePathParent.exists() && !adaptersListFilePathParent.mkdirs()) {
            filter.severe("dhcpd servlet configuration failed, adapters list file path parent directory did not exist and servlet failed to create it");
         }

         return true;
      } else {
         filter.severe("dhcpd servlet configuration failed, missing servlet configuration values");
         return false;
      }
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null || !query.containsKey("update")) {
         return sendSettings(content);
      } else if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         return 403;
      } else {
         return doUpdate(handler, query);
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      boolean requireAdmin = false;
      String queryString = req.getQueryString();
      if (queryString != null) {
         KeyedList query = Http.getGetForm(queryString);
         requireAdmin = query.containsKey("update");
      }

      return requireAdmin
         ? DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp)
         : DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), req, resp);
   }

   private static int doUpdate(ErrorHandler handler, KeyedList query) {
      return 200;
   }

   private static int sendSettings(XWriter content) {
      XElem dhcpdSettingsElem = new XElem("dhcpd");
      dhcpdSettingsElem.addAttr("provider", DHCPD_PROVIDER);
      dhcpdSettingsElem.addAttr("conf_file_path", makeRelativeFilePath(DHCPD_CONF_PATH, filter));
      dhcpdSettingsElem.addAttr("adapters_list_file_path", makeRelativeFilePath(DHCPD_ADAPTERS_LIST_PATH, filter));
      dhcpdSettingsElem.addAttr("lease_file_path", makeRelativeFilePath(DHCPD_LEASE_PATH, filter));
      XElem supportedAdaptersElem = new XElem("adapters");
      if (DHCPD_SUPPORTED_ADAPTERS != null) {
         String[] supportedAdaptersArray = TextUtil.splitAndTrim(DHCPD_SUPPORTED_ADAPTERS, ',');
         String enabledAdapterList = cat(DHCPD_ADAPTERS_LIST_PATH, filter);

         for (String supportedAdapter : supportedAdaptersArray) {
            XElem adapterElem = new XElem("adapter");
            adapterElem.setAttr("name", supportedAdapter);
            adapterElem.setAttr("supported", "true");
            if (supportedAdapter.equals("tiw_sap0")) {
               adapterElem.setAttr("id", supportedAdapter);
               String monitorString = cat("/var/run/wilink", filter);
               if (monitorString != null && monitorString.trim().startsWith("sap")) {
                  adapterElem.setAttr("enabled", "true");
               } else {
                  adapterElem.setAttr("enabled", "false");
               }
            } else if (enabledAdapterList != null && enabledAdapterList.contains(supportedAdapter)) {
               String adapterId = getAdapterIdFromOsName(supportedAdapter);
               adapterElem.setAttr("id", adapterId);
               adapterElem.setAttr("enabled", "true");
            } else {
               String adapterId = getAdapterIdFromOsName(supportedAdapter);
               adapterElem.setAttr("id", adapterId);
               adapterElem.setAttr("enabled", "false");
            }

            supportedAdaptersElem.addContent(adapterElem);
         }
      }

      dhcpdSettingsElem.addContent(supportedAdaptersElem);
      dhcpdSettingsElem.write(content);
      return 200;
   }

   public static String getAdapterIdFromOsName(String supportedAdapterOsName) {
      if (!PlatformUtil.isTridiumPlatform()) {
         return supportedAdapterOsName;
      }

      String adapterId = null;
      StringBuilder genAdapterId = new StringBuilder();
      if (supportedAdapterOsName.equals("tiw_sap0")) {
         adapterId = "tiw_sap0".trim();
      } else if (supportedAdapterOsName.startsWith("dm")) {
         genAdapterId.append("en").append(supportedAdapterOsName.substring(2));
         adapterId = genAdapterId.toString().trim();
      }

      return adapterId;
   }

   private static String cat(String filePath, Logger log) {
      File inFile = new File(filePath);
      String fileContent = null;

      try {
         fileContent = FileUtil.readString(inFile);
      } catch (FileNotFoundException var5) {
      } catch (IOException ioe) {
         log.log(Level.SEVERE, "failed to read requested file path", ioe);
      }

      return fileContent;
   }

   private static String readPropertyValue(String key, Logger log) {
      String property = AccessController.doPrivileged(() -> System.getProperty(key, null));
      if (property == null) {
         log.severe("failed to find DHCP Server configuration property '" + key + "'");
      }

      return property;
   }

   private static String makeRelativeFilePath(String filePath, Logger log) {
      String transformedFilePath;
      if (filePath.startsWith(NiagaraDaemon.NIAGARA_HOME)) {
         transformedFilePath = TextUtil.replace(filePath, NiagaraDaemon.NIAGARA_HOME, "/niagara");
      } else if (filePath.startsWith(NiagaraDaemon.NIAGARA_USER_HOME)) {
         transformedFilePath = TextUtil.replace(filePath, NiagaraDaemon.NIAGARA_USER_HOME, "/niagara_user");
      } else {
         if (!filePath.startsWith("/")) {
            throw new IllegalArgumentException("Unrecognized file path provided in URL '" + filePath + "'");
         }

         transformedFilePath = filePath;
      }

      if (transformedFilePath.contains("\\")) {
         transformedFilePath = TextUtil.replace(transformedFilePath, "\\", "/");
      }

      if (log.isLoggable(Level.FINEST)) {
         log.finest("file path '" + filePath + "' relativized to '" + transformedFilePath + "'");
      }

      return transformedFilePath;
   }
}
