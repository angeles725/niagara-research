package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SystemLogServlet extends Servlet {
   private final IPlatformProvider platformProvider;

   public SystemLogServlet(IPlatformProvider platformProvider) {
      super("systemlog");
      this.platformProvider = platformProvider;
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      return DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp);
   }

   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      String queryString = req.getQueryString();
      KeyedList query = Http.getGetForm(queryString);
      String logName = "";
      if (query.containsKey("log")) {
         logName = query.get("log", "");
      }

      String systemLog = this.platformProvider.readSystemLog(logName);
      if (systemLog == null) {
         systemLog = "";
      }

      byte[] systemLogBytes = systemLog.getBytes(StandardCharsets.UTF_8);
      resp.setStatus(200);
      resp.setHeader("Content-Type", "text/plain");
      resp.setIntHeader("Content-Length", systemLogBytes.length);

      try {
         resp.getOutputStream().write(systemLogBytes);
      } catch (IOException ioe) {
         if (this.getServer() != null && this.getServer().getState() == 1) {
            NiagaraDaemon.getFilter().log(Level.SEVERE, this.getName() + ": failed to write system log message (" + ioe + ")", ioe);
            Http.sendError(req, resp, 500);
         }
      }
   }
}
