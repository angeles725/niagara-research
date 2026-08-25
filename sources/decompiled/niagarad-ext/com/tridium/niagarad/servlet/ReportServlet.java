package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.app.AppRegistry;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class ReportServlet extends DaemonServlet {
   public ReportServlet() {
      super("report");
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null) {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "query", "ReportServlet: missing query");
         handler.error(msg);
         return 400;
      }

      if (!DebugServlet.debugEnabled && query.size() > 1 && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         return 403;
      }

      int result = 400;
      String appType = null;
      AppRegistry registry = null;
      if (query.containsKey("station")) {
         appType = "station";
         registry = NiagaraDaemon.getInstance().getStationRegistry();
      }

      if (appType != null) {
         if (registry == null) {
            MessageBundle msg = new MessageBundle("platform", "Servlet.missingRegistry", appType, "ReportServlet: missing station registry");
            handler.error(msg);
            return 400;
         }

         KeyedList reportData = query.duplicateDeep();
         String name = reportData.get(appType, null);
         if (name != null) {
            reportData.removeAll(appType);
            registry.captureApp(name, reportData);
         }

         result = 200;
      } else {
         MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", "appType", "ReportServlet: missing appType parameter");
         handler.error(msg);
      }

      return result;
   }
}
