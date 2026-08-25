package com.tridium.niagarad.servlet.qnx;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.servlet.Servlet;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QnxUsbBackupServlet extends Servlet {
   private static final String NO_USB_SCRIPT_PATH = "/sys/bin/no-fp-usb.sh";
   private static final String USB_COOKIE_PATH = "/home/niagara/daemon/fp-usb";

   public QnxUsbBackupServlet() {
      super("usbbackup");
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
         boolean usbEnabled = new File("/home/niagara/daemon/fp-usb").exists();
         content.append("<usbbackup enabled=\"").append(usbEnabled).append("\"/>\n");
         byte[] htmlBytes = content.toString().getBytes(StandardCharsets.UTF_8);
         resp.setIntHeader("Content-Length", htmlBytes.length);
         resp.setHeader("Content-Type", "text/xml");

         try {
            resp.getOutputStream().write(htmlBytes);
         } catch (IOException ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               logger.log(Level.SEVERE, "usbbackup: failed to write usbbackup response (" + ioe + ")", ioe);
               Http.sendError(req, resp, 500);
            }
         }
      }
   }

   private void doUpdate(HttpServletRequest req, HttpServletResponse resp, KeyedList query, Logger logger) {
      boolean currentlyEnabled = new File("/home/niagara/daemon/fp-usb").exists();
      boolean enableUsb = Boolean.valueOf(query.get("enable", "false"));
      if ((enableUsb || currentlyEnabled) && (!enableUsb || !currentlyEnabled)) {
         if (!enableUsb) {
            String command = "/proc/boot/ksh /sys/bin/no-fp-usb.sh";
            String[] commandString = TextUtil.split(command, ' ');
            ProcessBuilder builder = new ProcessBuilder(commandString);

            Process noUsbProcess;
            try {
               noUsbProcess = builder.start();
            } catch (IOException e) {
               logger.log(Level.SEVERE, "usbbackup: failed to launch no-fp-usb.sh (" + e + ")", e);
               Http.sendError(req, resp, 500);
               return;
            }

            while (noUsbProcess.isAlive()) {
               try {
                  noUsbProcess.waitFor();
               } catch (InterruptedException var12) {
               }
            }

            if (noUsbProcess.exitValue() != 0) {
               logger.log(Level.SEVERE, "usbbackup: failed to launch no-fp-usb.sh");
               Http.sendError(req, resp, 500);
               return;
            }

            resp.setStatus(200);
            resp.setIntHeader("Content-Length", 0);
         } else {
            try {
               if (!new File("/home/niagara/daemon/fp-usb").createNewFile()) {
                  throw new IOException("failed to create /home/niagara/daemon/fp-usb");
               }
            } catch (IOException e) {
               logger.log(Level.SEVERE, "usbbackup: failed to create /home/niagara/daemon/fp-usb (" + e + ")", e);
               Http.sendError(req, resp, 500);
               return;
            }

            resp.setStatus(200);
            resp.setIntHeader("Content-Length", 0);
         }
      } else {
         resp.setStatus(200);
         resp.setIntHeader("Content-Length", 0);
      }
   }
}
