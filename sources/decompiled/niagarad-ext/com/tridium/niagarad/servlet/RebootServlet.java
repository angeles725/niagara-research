package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import java.util.logging.Logger;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RebootServlet extends DaemonServlet {
   private Logger filter;

   public RebootServlet() {
      super("reboot");
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("reboot");
      return true;
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         this.filter.severe("invalid CSRF token in request");
         return 403;
      }

      String username = this.getServer().getAuthenticator().getRequestUserName(request);
      boolean force = Boolean.valueOf(query.get("force", "false"));
      if (force) {
         NiagaraDaemon.getInstance().lockClientPermanent();
      } else {
         if (!NiagaraDaemon.getInstance().lockClientPermanent()) {
            MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.multipleClient", "multiple client access conflict (reboot)");
            this.filter.severe("multiple client access conflict (reboot)");
            handler.error(msg);
            return 409;
         }

         NiagaraDaemon.getInstance().stopApps();
      }

      this.filter.warning((force ? "forced " : "") + "reboot request received from user \"" + username + "\"");
      NiagaraDaemon.getInstance().queueReboot(force);
      return 200;
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      return DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp);
   }
}
