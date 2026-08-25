package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Http;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import javax.baja.nre.util.FileUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PlatformLoginFileServlet extends Servlet {
   private static final String RC_PREFIX = "/loginfile";
   private static final String RC_PATH = "/com/tridium/niagarad/http/rc";
   private static final String CORE_RC_PREFIX = "/core";
   private static final String CORE_RC_PATH = "/com/tridium/nre/jetty/rc";
   private static final Set<String> acceptedFiles = new HashSet<>();
   private static final Set<String> acceptedCoreFiles = new HashSet<>();

   public PlatformLoginFileServlet() {
      super("loginfile");
   }

   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      if (!DebugServlet.debugEnabled) {
         Http.sendError(req, resp, 403);
      } else {
         InputStream in = null;
         OutputStream out = null;

         try {
            try {
               String pathInfo = req.getPathInfo();
               if (pathInfo == null || !pathInfo.startsWith("/loginfile")) {
                  Http.sendError(req, resp, 404);
                  return;
               }

               String requestedFile = pathInfo.substring("/loginfile".length());
               if (requestedFile.startsWith("/core")) {
                  requestedFile = requestedFile.substring("/core".length());
                  if (acceptedCoreFiles.contains(requestedFile)) {
                     in = this.getClass().getResourceAsStream("/com/tridium/nre/jetty/rc" + requestedFile);
                  }
               } else if (acceptedFiles.contains(requestedFile)) {
                  in = this.getClass().getResourceAsStream("/com/tridium/niagarad/http/rc" + requestedFile);
               }

               if (in != null) {
                  OutputStream var19 = resp.getOutputStream();
                  FileUtil.pipe(in, var19);
                  return;
               }

               Http.sendError(req, resp, 403);
            } finally {
               try {
                  if (in != null) {
                     in.close();
                  }
               } catch (Exception var16) {
               }
            }
         } catch (IOException var18) {
         }
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean requiresAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      return true;
   }

   static {
      acceptedFiles.add("/plat-login.js");
      acceptedFiles.add("/plat-login.css");
      acceptedCoreFiles.add("/auth.min.js");
   }
}
