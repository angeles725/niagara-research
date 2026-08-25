package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import java.util.Properties;
import javax.baja.nre.util.SortUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DebugServlet extends DaemonServlet {
   public static volatile boolean debugEnabled = false;

   public DebugServlet() {
      super("debug");
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
   public synchronized int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null || !query.containsKey("update")) {
         content.w("<debug").w(' ').attr("daemonDebug", String.valueOf(debugEnabled)).w(">").nl();
         if (debugEnabled) {
            XElem timesElem = new XElem("times");
            timesElem.addAttr("millisAtBootstrap", String.valueOf(NiagaraDaemon.getMillisAtBootstrap()));
            timesElem.addAttr("millisAtServiceStart", String.valueOf(NiagaraDaemon.getMillisAtStart()));
            timesElem.addAttr("jarSignatureBuildTime", String.valueOf(NiagaraDaemon.getJarSignatureBuildMillis()));
            timesElem.addAttr("bootDuration", String.valueOf(NiagaraDaemon.getMillisAtStart() - NiagaraDaemon.getMillisAtBootstrap()));
            timesElem.addAttr("uptime", String.valueOf(System.currentTimeMillis() - NiagaraDaemon.getMillisAtBootstrap()));
            timesElem.write(content, 2);
            content.w("  <properties>").nl();
            Properties props = System.getProperties();
            String[] propertyNames = props.stringPropertyNames().toArray(new String[0]);
            SortUtil.sort(propertyNames);

            for (String propName : propertyNames) {
               XElem property = new XElem("property");
               property.addAttr("name", propName);
               property.addAttr("value", props.getProperty(propName));
               property.write(content, 2);
               content.nl();
            }

            content.w("  </properties>").nl();
         }

         content.w("</debug>").nl();
         return 200;
      } else {
         if (!debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
            MessageBundle msg = new MessageBundle("invalid CSRF token in request");
            handler.error(msg);
            return 403;
         }

         if (query.containsKey("daemonDebug")) {
            if (!NiagaraDaemon.getDebugSupported()) {
               MessageBundle msg = new MessageBundle("Daemon debug not supported");
               handler.error(msg);
               return 400;
            }

            String debugValue = query.get("daemonDebug", "false");
            debugEnabled = Boolean.parseBoolean(debugValue);
            this.getServer().updateSessionCookieHttpOnlyConfig();
         }

         return 200;
      }
   }
}
