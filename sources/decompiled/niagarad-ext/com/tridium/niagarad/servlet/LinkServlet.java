package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import java.io.File;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LinkServlet extends DaemonServlet {
   protected Logger filter;
   private static String LINK_SUPPORTED;
   private static String LINK_READONLY;
   private static String LINK_MODES_SUPPORTED;
   private static String LINK_FILE_PATH;
   public static final String LINK_SUPPORTED_PROPERTY = "niagara.link.supported";
   private static final String LINK_READONLY_PROPERTY = "niagara.link.readonly";
   private static final String LINK_MODES_SUPPORTED_PROPERTY = "niagara.link.modesSupported";
   private static final String LINK_FILE_PATH_PROPERTY = "niagara.link.configurationFile";
   public static final String SERVLET_NAME = "linkcfg";
   public static final String SERVLET_LINK_SETTINGS_ELEM = "linkSettings";
   public static final String SERVLET_LINK_MODES_ELEM = "linkmodes";
   public static final String SERVLET_LINK_MODE_ELEM = "linkmode";
   public static final String SERVLET_LINK_READONLY_ATTR = "readonly";
   public static final String SERVLET_LINK_PATH_ATTR = "link_file_path";
   public static final String SERVLET_NAME_ATTR = "name";
   public static final String SERVLET_LINK_MODE_STANDARD = "standard";
   public static final String SERVLET_LINK_MODE_DAISYCHAIN = "daisychain";
   public static final char SERVLET_LINK_MODE_DELIMITER = ',';

   public LinkServlet() {
      super("linkcfg");
   }

   @Override
   public boolean doInit() {
      this.filter = Logger.getLogger("linkcfg");
      LINK_SUPPORTED = readPropertyValue("niagara.link.supported", this.filter);
      LINK_READONLY = readPropertyValue("niagara.link.readonly", this.filter);
      LINK_MODES_SUPPORTED = readPropertyValue("niagara.link.modesSupported", this.filter);
      LINK_FILE_PATH = readPropertyValue("niagara.link.configurationFile", this.filter);
      if (LINK_SUPPORTED != null && LINK_READONLY != null && LINK_MODES_SUPPORTED != null && LINK_FILE_PATH != null) {
         if (this.filter.isLoggable(Level.FINEST)) {
            this.filter.finest("using link configuration properties:");
            this.filter.finest(LINK_SUPPORTED + " = " + LINK_SUPPORTED);
            this.filter.finest("niagara.link.readonly = " + LINK_READONLY);
            this.filter.finest("niagara.link.modesSupported = " + LINK_MODES_SUPPORTED);
            this.filter.finest("niagara.link.configurationFile = " + LINK_FILE_PATH);
         }

         String[] linkModes = TextUtil.split(LINK_MODES_SUPPORTED, ',');
         if (linkModes.length == 0) {
            this.filter.severe("link servlet configuration failed, link property 'niagara.link.modesSupported' has invalid content");
            return false;
         }

         for (String linkMode : linkModes) {
            if (!"daisychain".equals(linkMode) && !"standard".equals(linkMode)) {
               this.filter.severe("link servlet configuration failed, unrecognized supported link mode '" + linkMode + "'");
               return false;
            }
         }

         if (makeRelativeFilePath(LINK_FILE_PATH, this.filter).equals(LINK_FILE_PATH)) {
            this.filter.severe("link servlet configuration failed, link file path is not relative to any niagara path");
            return false;
         }

         File linkFileDirectory = new File(LINK_FILE_PATH).getParentFile();
         if (!Boolean.parseBoolean(LINK_READONLY) && !linkFileDirectory.exists() && !linkFileDirectory.mkdirs()) {
            this.filter.severe("link servlet configuration failed, link file path parent directory did not exist and servlet failed to create it");
         }

         return true;
      } else {
         this.filter.severe("link servlet configuration failed, missing servlet configuration values");
         return false;
      }
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      return this.sendSettings(content);
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

   private int sendSettings(XWriter content) {
      XElem settingsElem = new XElem("linkSettings");
      settingsElem.addAttr("readonly", LINK_READONLY);
      settingsElem.addAttr("link_file_path", makeRelativeFilePath(LINK_FILE_PATH, this.filter));
      XElem linkModesElem = new XElem("linkmodes");
      if (LINK_MODES_SUPPORTED != null) {
         String[] supportedArray = TextUtil.splitAndTrim(LINK_MODES_SUPPORTED, ',');

         for (String eachLinkmode : supportedArray) {
            XElem linkModelElem = new XElem("linkmode");
            linkModelElem.addAttr("name", eachLinkmode);
            linkModesElem.addContent(linkModelElem);
         }
      }

      settingsElem.addContent(linkModesElem);
      settingsElem.write(content);
      return 200;
   }

   private static String readPropertyValue(String key, Logger log) {
      String property = AccessController.doPrivileged(() -> System.getProperty(key, null));
      if (property == null) {
         log.severe("failed to find link configuration property '" + key + "'");
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
