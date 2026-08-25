package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.http.WebServer;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Servlet extends HttpServlet {
   public static final int SERVLET_IDLE = 0;
   public static final int SERVLET_STARTING = 1;
   public static final int SERVLET_RUNNING = 2;
   public static final int SERVLET_STOPPING = 3;
   private final String name;
   private boolean initiated = false;
   private int state;
   private WebServer server = null;

   protected Servlet(String servletName) {
      this.state = 0;
      this.name = servletName;
   }

   public boolean init(WebServer srv) {
      if (this.initiated) {
         return true;
      }

      this.server = srv;
      this.initiated = this.doInit();
      return this.initiated;
   }

   public boolean doInit() {
      return true;
   }

   public boolean start() {
      if (this.state == 2) {
         return true;
      }

      this.state = 1;
      if (!this.doStart()) {
         this.state = 0;
      } else {
         this.state = 2;
      }

      return this.state == 2;
   }

   public boolean doStart() {
      return true;
   }

   public void stop() {
      if (this.state != 2) {
         this.state = 0;
      } else {
         this.state = 3;
         this.doStop();
         this.state = 0;
      }
   }

   public void doStop() {
   }

   public String getName() {
      return this.name;
   }

   public int getState() {
      return this.state;
   }

   public WebServer getServer() {
      return this.server;
   }

   public String getUriWithoutName(String origUri) {
      if (this.name != null && !this.name.isEmpty()) {
         if (origUri.charAt(0) != '/') {
            return origUri;
         }

         String withoutFirstSlash = origUri.substring(1);
         int secondSlashIndex = withoutFirstSlash.indexOf(47);
         return secondSlashIndex == -1 ? "" : withoutFirstSlash.substring(secondSlashIndex);
      } else {
         return origUri;
      }
   }

   public boolean useDefaultAuthentication() {
      return true;
   }

   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      return false;
   }

   public boolean requiresAuthentication() {
      return true;
   }

   public boolean isDaemonServlet() {
      return false;
   }

   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      Http.sendError(req, resp, 400);
   }

   public void doPost(HttpServletRequest req, HttpServletResponse resp) {
      Http.sendError(req, resp, 400);
   }

   public void doHead(HttpServletRequest req, HttpServletResponse resp) {
      Http.sendError(req, resp, 400);
   }

   public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
      Http.sendError(req, resp, 400);
   }
}
