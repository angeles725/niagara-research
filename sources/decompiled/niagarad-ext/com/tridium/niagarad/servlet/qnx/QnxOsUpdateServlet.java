package com.tridium.niagarad.servlet.qnx;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.servlet.DaemonServlet;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import java.io.IOException;
import java.util.logging.Logger;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QnxOsUpdateServlet extends DaemonServlet {
   private Logger filter;
   public static final String SERVLET_NAME = "osupdate";

   public QnxOsUpdateServlet() {
      super("osupdate");
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("qnxosupdate");
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

      if (!NiagaraDaemon.getInstance().lockClient()) {
         MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.multipleClient", "multiple client access conflict (osupdate)");
         this.filter.severe("multiple client access conflict (osupdate)");
         handler.error(msg);
         return 409;
      }

      int result = 200;

      try {
         long millisStart = System.currentTimeMillis();
         this.filter.info("update initiated");
         String command = "/proc/boot/ksh /sys/bin/osupdate";
         String[] commandString = TextUtil.split(command, ' ');
         ProcessBuilder builder = new ProcessBuilder(commandString);

         Process osupdate;
         try {
            osupdate = builder.start();
         } catch (IOException e) {
            int resultx = 500;
            this.filter.severe("process exec failed");
            handler.error("process exec failed");
            return resultx;
         }

         while (osupdate.isAlive()) {
            try {
               osupdate.waitFor();
            } catch (InterruptedException var17) {
            }
         }

         if (osupdate.exitValue() != 0) {
            result = 500;
            this.filter.severe("osupdate failed");
            handler.error("osupdate failed");
         } else {
            this.filter.info("update complete (" + (System.currentTimeMillis() - millisStart) + "ms)");
         }
      } finally {
         NiagaraDaemon.getInstance().unlockClient();
      }

      return result;
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
