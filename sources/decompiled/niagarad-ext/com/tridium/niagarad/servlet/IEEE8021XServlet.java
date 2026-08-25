package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.license.LicenseManager;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.PlatformUtil;
import java.io.File;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class IEEE8021XServlet extends DaemonServlet {
   private static final String EN = "en";
   private static final String DM = "dm";
   protected static Logger filter;
   private static String IEEE8021X_SUPPORTED;
   private static String IEEE8021X_READONLY;
   private static String IEEE8021X_CONF_FILE_PATTERN;
   private static String IEEE8021X_STATUS_FILE_PATTERN;
   private static String IEEE8021X_SUPPORTED_ADAPTERS;
   private static String IEEE8021X_PKI_CERTIFICATES_DIRECTORY;
   public static final String IEEE8021X_SUPPORTED_PROPERTY = "niagara.ieee8021x.supported";
   private static final String IEEE8021X_READONLY_PROPERTY = "niagara.ieee8021x.readonly";
   private static final String IEEE8021X_SUPPORTED_ADAPTERS_PROPERTY = "niagara.ieee8021x.adaptersSupported";
   private static final String IEEE8021X_CONF_FILE_PATTERN_PROPERTY = "niagara.ieee8021x.configurationFilePattern";
   private static final String IEEE8021X_STATUS_FILE_PATTERN_PROPERTY = "niagara.ieee8021x.statusFilePattern";
   private static final String IEEE8021X_PKI_CERTIFICATES_DIRECTORY_PROPERTY = "niagara.ieee8021x.pkiCertificatesDirectory";
   public static final String SERVLET_NAME = "ieee8021x";
   public static final String FEATURE_VENDOR = "tridium";
   public static final String FEATURE_NAME = "ieee8021x";
   public static final String SERVLET_ADAPTERS_ELEM = "adapters";
   public static final String SERVLET_ADAPTER_ELEM = "adapter";
   public static final String SERVLET_NAME_ATTR = "name";
   public static final String SERVLET_ID_ATTR = "id";
   public static final String SERVLET_SUPPORTED_ATTR = "supported";
   public static final String SERVLET_LICENSED_ATTR = "licensed";
   public static final String SERVLET_READONLY_ATTR = "readonly";
   public static final String SERVLET_ENABLED_ATTR = "enabled";
   public static final String SERVLET_CONF_FILE_ATTR = "configuration_file";
   public static final String SERVLET_STATUS_FILE_ATTR = "status_file";
   public static final String SERVLET_PKI_DIR_ATTR = "pki_certificates_directory";

   public IEEE8021XServlet() {
      super("ieee8021x");
   }

   @Override
   public boolean doInit() {
      filter = Logger.getLogger("ieee8021x");
      IEEE8021X_SUPPORTED = readPropertyValue("niagara.ieee8021x.supported", filter);
      IEEE8021X_READONLY = readPropertyValue("niagara.ieee8021x.readonly", filter);
      IEEE8021X_SUPPORTED_ADAPTERS = readPropertyValue("niagara.ieee8021x.adaptersSupported", filter);
      IEEE8021X_CONF_FILE_PATTERN = readPropertyValue("niagara.ieee8021x.configurationFilePattern", filter);
      IEEE8021X_STATUS_FILE_PATTERN = readPropertyValue("niagara.ieee8021x.statusFilePattern", filter);
      IEEE8021X_PKI_CERTIFICATES_DIRECTORY = readPropertyValue("niagara.ieee8021x.pkiCertificatesDirectory", filter);
      if (IEEE8021X_SUPPORTED != null
         && IEEE8021X_READONLY != null
         && IEEE8021X_SUPPORTED_ADAPTERS != null
         && IEEE8021X_CONF_FILE_PATTERN != null
         && IEEE8021X_STATUS_FILE_PATTERN != null
         && IEEE8021X_PKI_CERTIFICATES_DIRECTORY != null) {
         if (filter.isLoggable(Level.FINEST)) {
            filter.finest("using ieee8021x configuration properties:");
            filter.finest("niagara.ieee8021x.supported = " + IEEE8021X_SUPPORTED);
            filter.finest("niagara.ieee8021x.readonly = " + IEEE8021X_READONLY);
            filter.finest("niagara.ieee8021x.adaptersSupported = " + IEEE8021X_SUPPORTED_ADAPTERS);
            filter.finest("niagara.ieee8021x.configurationFilePattern = " + IEEE8021X_CONF_FILE_PATTERN);
            filter.finest("niagara.ieee8021x.statusFilePattern = " + IEEE8021X_STATUS_FILE_PATTERN);
            filter.finest("niagara.ieee8021x.pkiCertificatesDirectory = " + IEEE8021X_PKI_CERTIFICATES_DIRECTORY);
         }

         if (!IEEE8021X_CONF_FILE_PATTERN.contains("%s") || IEEE8021X_CONF_FILE_PATTERN.indexOf("%s") != IEEE8021X_CONF_FILE_PATTERN.lastIndexOf("%s")) {
            filter.severe("ieee8021x servlet configuration failed, configuration file pattern is invalid");
            return false;
         }

         if (!IEEE8021X_STATUS_FILE_PATTERN.contains("%s") || IEEE8021X_STATUS_FILE_PATTERN.indexOf("%s") != IEEE8021X_STATUS_FILE_PATTERN.lastIndexOf("%s")) {
            filter.severe("ieee8021x servlet configuration failed, status file pattern is invalid");
            return false;
         }

         if (IEEE8021X_PKI_CERTIFICATES_DIRECTORY.contains("%s")) {
            filter.severe("ieee8021x servlet configuration failed, pki root directory should not be a file pattern");
            return false;
         }

         String relativeConfigurationFilePattern = makeRelativeFilePath(IEEE8021X_CONF_FILE_PATTERN, filter);
         if (relativeConfigurationFilePattern.equals(IEEE8021X_CONF_FILE_PATTERN)) {
            filter.severe("ieee8021x servlet configuration failed, configuration file pattern is not relative to any niagara path");
            return false;
         }

         String relativeStatusFilePattern = makeRelativeFilePath(IEEE8021X_STATUS_FILE_PATTERN, filter);
         if (relativeStatusFilePattern.equals(IEEE8021X_STATUS_FILE_PATTERN)) {
            filter.severe("ieee8021x servlet configuration failed, status file pattern is not relative to any niagara path");
            return false;
         }

         String relativePkiDirectory = makeRelativeFilePath(IEEE8021X_PKI_CERTIFICATES_DIRECTORY, filter);
         if (relativePkiDirectory.equals(IEEE8021X_PKI_CERTIFICATES_DIRECTORY)) {
            filter.severe("ieee8021x servlet configuration failed, pki root directory is not relative to any niagara path");
            return false;
         }

         File configurationFilePatternParent = new File(IEEE8021X_CONF_FILE_PATTERN).getParentFile();
         if (!Boolean.parseBoolean(IEEE8021X_READONLY) && !configurationFilePatternParent.exists() && !configurationFilePatternParent.mkdirs()) {
            filter.severe("ieee8021x servlet configuration failed, configuration file pattern parent directory did not exist and servlet failed to create it");
         }

         File statusFilePatternParent = new File(IEEE8021X_STATUS_FILE_PATTERN).getParentFile();
         if (!Boolean.parseBoolean(IEEE8021X_READONLY) && !statusFilePatternParent.exists() && !statusFilePatternParent.mkdirs()) {
            filter.severe("ieee8021x servlet configuration failed, status file pattern parent directory did not exist and servlet failed to create it");
         }

         File certificatesDirectoryFile = new File(IEEE8021X_PKI_CERTIFICATES_DIRECTORY);
         if (!Boolean.parseBoolean(IEEE8021X_READONLY) && !certificatesDirectoryFile.exists() && !certificatesDirectoryFile.mkdirs()) {
            filter.severe("ieee8021x servlet configuration failed, pki directory did not exist and servlet failed to create it");
         }

         if (PlatformUtil.isNpsdkPlatform()) {
            Set<String> uniqueFilePaths = new HashSet<>();
            uniqueFilePaths.add(configurationFilePatternParent.getAbsolutePath());
            uniqueFilePaths.add(statusFilePatternParent.getAbsolutePath());
            if (!uniqueFilePaths.contains(certificatesDirectoryFile.getParentFile().getAbsolutePath())) {
               uniqueFilePaths.add(certificatesDirectoryFile.getAbsolutePath());
            }

            for (String uniqueFilePath : uniqueFilePaths) {
               if (filter.isLoggable(Level.FINEST)) {
                  filter.finest("passphrase protecting path '" + uniqueFilePath + "'");
               }

               FileServlet.addPassphraseEncryptedPath(uniqueFilePath);
            }
         }

         return true;
      } else {
         filter.severe("ieee8021x servlet configuration failed, missing servlet configuration values");
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
      XElem ieee8021xSettingsElem = new XElem("ieee8021x");
      ieee8021xSettingsElem.addAttr("readonly", IEEE8021X_READONLY);
      XElem supportedAdaptersElem = new XElem("adapters");
      if (IEEE8021X_SUPPORTED_ADAPTERS != null) {
         String[] supportedAdaptersArray = TextUtil.splitAndTrim(IEEE8021X_SUPPORTED_ADAPTERS, ',');

         for (String supportedAdapterOsName : supportedAdaptersArray) {
            XElem adapterElem = new XElem("adapter");
            boolean licensed = LicenseManager.getInstance(filter).checkFeature("tridium", "ieee8021x") != null;
            String configurationFilePath = String.format(IEEE8021X_CONF_FILE_PATTERN, supportedAdapterOsName);
            boolean enabled = new File(configurationFilePath).exists();
            String statusFilePath = String.format(IEEE8021X_STATUS_FILE_PATTERN, supportedAdapterOsName);
            String pkiCertificatesPath = IEEE8021X_PKI_CERTIFICATES_DIRECTORY;
            adapterElem.setAttr("name", supportedAdapterOsName);
            adapterElem.setAttr("id", getAdapterIdFromOsName(supportedAdapterOsName));
            adapterElem.setAttr("supported", IEEE8021X_SUPPORTED);
            adapterElem.setAttr("licensed", String.valueOf(licensed));
            adapterElem.setAttr("readonly", IEEE8021X_READONLY);
            adapterElem.setAttr("enabled", String.valueOf(enabled));
            adapterElem.setAttr("configuration_file", makeRelativeFilePath(configurationFilePath, filter));
            adapterElem.setAttr("status_file", makeRelativeFilePath(statusFilePath, filter));
            adapterElem.setAttr("pki_certificates_directory", makeRelativeFilePath(pkiCertificatesPath, filter));
            supportedAdaptersElem.addContent(adapterElem);
         }
      }

      ieee8021xSettingsElem.addContent(supportedAdaptersElem);
      ieee8021xSettingsElem.write(content);
      return 200;
   }

   public static String getAdapterIdFromOsName(String supportedAdapterOsName) {
      if (!PlatformUtil.isTridiumPlatform()) {
         return supportedAdapterOsName;
      }

      String adapterId = supportedAdapterOsName;
      StringBuilder genAdapterId = new StringBuilder();
      if (supportedAdapterOsName.startsWith("dm")) {
         genAdapterId.append("en").append(supportedAdapterOsName.substring("dm".length()));
         adapterId = genAdapterId.toString().trim();
      }

      return adapterId;
   }

   private static String readPropertyValue(String key, Logger log) {
      String property = AccessController.doPrivileged(() -> System.getProperty(key, null));
      if (property == null) {
         log.severe("failed to find IEEE 802.1X configuration property '" + key + "'");
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
