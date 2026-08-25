package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.security.Aes256PasswordEncoderUtil;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.syslog.SyslogManager;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SyslogServlet extends DaemonServlet {
   private Logger filter;
   public static final String SERVLET_NAME = "syslog";

   public SyslogServlet() {
      super("syslog");
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("syslog");
      return true;
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

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query != null) {
         if (query.containsKey("update")) {
            if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
               MessageBundle msg = new MessageBundle("invalid CSRF token in request");
               handler.error(msg);
               return 403;
            }

            return this.doUpdate(request, handler, query);
         }

         if (query.containsKey("status")) {
            return this.sendStatus(content);
         }
      }

      return 400;
   }

   private int sendStatus(XWriter content) {
      XElem syslogMessageQueueElem = new XElem("syslogMessageQueue");
      SyslogManager instance = SyslogManager.getInstance();
      boolean syslogDisabled = instance.isPlatformSyslogDisabled();
      boolean syslogServerConnected = instance.isSyslogServerConnected();
      int queueFullPercentage = instance.getQueueFullPercentage();
      syslogMessageQueueElem.addAttr("queueFullPercentage", String.valueOf(queueFullPercentage));
      syslogMessageQueueElem.addAttr("platformSyslogDisabled", String.valueOf(syslogDisabled));
      syslogMessageQueueElem.addAttr("platformSyslogServerConnected", String.valueOf(syslogServerConnected));
      ByteBuffer buffer = new ByteBuffer();
      XWriter out = new XWriter();
      out.setOutputStream(buffer.getOutputStream());
      out.prolog();
      syslogMessageQueueElem.write(out);
      out.flush();
      out.close();
      String syslogStatusXml = new String(buffer.getBytes(), 0, buffer.getLength(), StandardCharsets.UTF_8);
      content.write(syslogStatusXml);
      return 200;
   }

   private String urlDecodeWithNullCheck(KeyedList query, String key) throws Exception {
      String value = query.get(key, null);
      if (value != null) {
         value = URLDecoder.decode(value, "UTF-8");
      }

      return value;
   }

   private int doUpdate(HttpServletRequest request, ErrorHandler handler, KeyedList query) {
      try {
         SyslogManager manager = SyslogManager.getInstance();
         String enabled = this.urlDecodeWithNullCheck(query, "enabled");
         String serverHost = this.urlDecodeWithNullCheck(query, "serverHost");
         String serverPort = this.urlDecodeWithNullCheck(query, "serverPort");
         String messageType = this.urlDecodeWithNullCheck(query, "messageType");
         String transportProtocol = this.urlDecodeWithNullCheck(query, "transportProtocol");
         String clientAlias = this.urlDecodeWithNullCheck(query, "clientAlias");
         String encodedClientPassword = null;
         if (query.containsKey("clientPassword")) {
            String sharedKeyInQuery = this.urlDecodeWithNullCheck(query, "sharedKeyName");
            if (sharedKeyInQuery == null) {
               throw new Exception("No shared key found in query");
            }

            String clientPassword = this.urlDecodeWithNullCheck(query, "clientPassword");
            String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
            SharedSecretKey sharedKey = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeName);
            clientPassword = sharedKey.decrypt(Base64.getDecoder().decode(clientPassword)).asString(true, StandardCharsets.UTF_8);
            if (!Aes256PasswordEncoderUtil.isDefault(clientPassword)) {
               KeyRing keyRing = SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing();
               encodedClientPassword = Aes256PasswordEncoderUtil.encodePassword(
                  keyRing, "com.tridium.syslog.clientPassword", new SecretChars(clientPassword.toCharArray(), true)
               );
            }
         } else {
            encodedClientPassword = manager.getClientPassword();
         }

         String platformLogEnabled = this.urlDecodeWithNullCheck(query, "platformLogEnabled");
         String stationLogEnabled = this.urlDecodeWithNullCheck(query, "stationLogEnabled");
         String logLevelFilter = this.urlDecodeWithNullCheck(query, "logLevelFilter");
         String stationAuditEnabled = this.urlDecodeWithNullCheck(query, "stationAuditEnabled");
         String securityAuditEnabled = this.urlDecodeWithNullCheck(query, "securityAuditEnabled");
         String facility = this.urlDecodeWithNullCheck(query, "facility");
         String queueSize = this.urlDecodeWithNullCheck(query, "queueSize");
         manager.writeSyslogConfig(
            enabled,
            serverHost,
            serverPort,
            messageType,
            transportProtocol,
            clientAlias,
            encodedClientPassword,
            platformLogEnabled,
            stationLogEnabled,
            logLevelFilter,
            stationAuditEnabled,
            securityAuditEnabled,
            facility,
            queueSize
         );
         return 200;
      } catch (Exception e) {
         MessageBundle msg = new MessageBundle("Failed to update syslog configuration: " + e.getMessage());
         handler.error(msg);
         this.filter.log(Level.SEVERE, "failed to update syslog configuration (" + e + ')');
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.SEVERE, "Stack trace: ", e);
         }

         return 500;
      }
   }
}
