package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SshdServlet extends Servlet {
   private final IPlatformProvider platformProvider;

   public SshdServlet(IPlatformProvider platformProvider) {
      super("sshd");
      this.platformProvider = platformProvider;
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
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      String queryString = req.getQueryString();
      KeyedList query = Http.getGetForm(queryString);
      Logger logger = NiagaraDaemon.getFilter();
      if (query.containsKey("update")) {
         if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
            logger.severe("invalid CSRF token in request");
            Http.sendError(req, resp, 403);
            return;
         }

         this.doUpdate(req, resp, query, logger);
      } else {
         StringBuilder content = new StringBuilder();
         content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
         int sshdPort = this.platformProvider.getSSHPort();
         content.append("<sshd port=\"").append(sshdPort).append("\"/>\n");
         byte[] htmlResponse = content.toString().getBytes(StandardCharsets.UTF_8);
         resp.setIntHeader("Content-Length", htmlResponse.length);
         resp.setHeader("Content-Type", "text/xml");

         try {
            resp.getOutputStream().write(htmlResponse);
         } catch (IOException ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               MessageBundle msg = new MessageBundle("sshd: failed to write sshd response (" + ioe + ")");
               logger.log(Level.SEVERE, msg.getNonLocalizedMessage(), ioe);
               Http.sendErrorXML(req, resp, 500, msg);
            }
         }
      }
   }

   private void doUpdate(HttpServletRequest req, HttpServletResponse resp, KeyedList query, Logger logger) {
      int sshdPort;
      try {
         sshdPort = Integer.valueOf(query.get("port", "-1"));
      } catch (NumberFormatException nfe) {
         logger.severe("invalid port value specified");
         Http.sendError(req, resp, 400);
         return;
      }

      if (DaemonAuthUtil.defaultCredentialsExist(this.platformProvider) && sshdPort != -1) {
         MessageBundle msg = new MessageBundle("can not enable SSH when default credentials are present");
         logger.severe(msg.getNonLocalizedMessage());
         Http.sendErrorXML(req, resp, 400, msg);
      } else if (!this.platformProvider.setSSHPort(sshdPort)) {
         MessageBundle msg = new MessageBundle("platform", "Servlet.sshd.updateFailed", "failed to update the SSHD settings");
         logger.severe(msg.getNonLocalizedMessage());
         Http.sendErrorXML(req, resp, 500, msg);
      } else {
         resp.setStatus(200);
         resp.setIntHeader("Content-Length", 0);
      }
   }
}
