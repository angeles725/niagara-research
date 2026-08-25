package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.NiagaraDaemonLogSettings;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FilteredLogServlet extends Servlet {
   private Logger filter;

   public FilteredLogServlet() {
      super("logfilter");
   }

   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      String queryString = req.getQueryString();
      KeyedList query = Http.getGetForm(queryString);
      String uri = this.getUriWithoutName(req.getRequestURI());
      boolean update = false;
      switch (uri.toLowerCase()) {
         case "/set":
         case "/save":
            update = true;
         default:
            if (update && !DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
               this.filter.severe("invalid CSRF token in request");
               Http.sendError(req, resp, 403);
            } else {
               if (uri.isEmpty() || uri.equalsIgnoreCase("/")) {
                  this.doGetFilterStatus(req, resp);
               } else if (uri.equalsIgnoreCase("/status")) {
                  this.doGetFilterStatus(req, resp);
               } else if (uri.equalsIgnoreCase("/get")) {
                  this.doGetFilters(req, resp);
               } else if (uri.equalsIgnoreCase("/set")) {
                  this.doSetFilters(req, resp);
               } else if (uri.equalsIgnoreCase("/save")) {
                  this.doSaveFilters(req, resp);
               } else {
                  this.filter.fine("bad request");
                  Http.sendError(req, resp, 400);
               }
            }
      }
   }

   @Override
   public boolean doInit() {
      this.filter = Logger.getLogger("filteredlog");
      return true;
   }

   @Override
   public boolean doStart() {
      return true;
   }

   private void doGetFilterStatus(HttpServletRequest req, HttpServletResponse resp) {
      this.filter.fine("get filter status");
      StringWriter out = new StringWriter();
      out.write("<html>\n");
      out.write("  <body>\n");
      out.write("    <h1>FilteredLogStatus</h1>\n");
      if (NiagaraDaemon.niagaraDaemonLogSettings.getForceLevel() != null) {
         out.write("    <font color=\"red\"><b>FORCE LEVEL SET TO " + NiagaraDaemon.niagaraDaemonLogSettings.getForceLevel().getName() + "</b></font><br/>\n");
      }

      if (NiagaraDaemon.niagaraDaemonLogSettings.getSaveTime() > 0L) {
         out.write("    Last save time = " + new Date(NiagaraDaemon.niagaraDaemonLogSettings.getSaveTime()) + " <br/>\n");
      }

      out.write("    <pre>\n");
      out.write("     <table border=\"1\">\n");
      out.write("      <tr><th>filters</th><th>level</th></tr>\n");

      for (String logName : NiagaraDaemon.niagaraDaemonLogSettings.getAllLogs()) {
         if (!logName.isEmpty() && !logName.equalsIgnoreCase("global")) {
            boolean containsIgnoredPrefix = false;

            for (String ignoredLogPrefix : NiagaraDaemonLogSettings.IGNORED_LOG_PREFIXES) {
               containsIgnoredPrefix = logName.startsWith(ignoredLogPrefix);
               if (containsIgnoredPrefix) {
                  break;
               }
            }

            if (!containsIgnoredPrefix) {
               out.write("      <tr><td>" + logName + "</td><td>" + NiagaraDaemonLogSettings.getLogLevel(logName) + "</td></tr>\n");
            }
         }
      }

      out.write("    </table></pre>\n");
      out.write("  </body>\n");
      out.write("</html>\n");
      byte[] htmlBytes = out.toString().getBytes(StandardCharsets.UTF_8);
      resp.setStatus(200);
      resp.setHeader("Content-Type", "text/html");
      resp.setIntHeader("Content-Length", htmlBytes.length);

      try {
         resp.getOutputStream().write(htmlBytes);
      } catch (IOException ioe) {
         if (this.getServer() != null && this.getServer().getState() == 1) {
            this.filter.log(Level.SEVERE, "failed to write current filter status (" + ioe + ")", ioe);
            Http.sendError(req, resp, 500);
         }
      }
   }

   private void doGetFilters(HttpServletRequest req, HttpServletResponse resp) {
      this.filter.fine("get filters");
      StringWriter out = new StringWriter();
      out.write("<filters");
      Level forceLevel = NiagaraDaemon.niagaraDaemonLogSettings.getForceLevel();
      if (forceLevel != null) {
         out.write(" forceLevel=\"" + forceLevel.getName() + "\"");
      }

      out.write(" version=\"2\"");
      out.write(">\n");

      for (String logName : NiagaraDaemon.niagaraDaemonLogSettings.getAllLogs()) {
         if (!logName.isEmpty() && !logName.equalsIgnoreCase("global")) {
            boolean containsIgnoredPrefix = false;

            for (String ignoredLogPrefix : NiagaraDaemonLogSettings.IGNORED_LOG_PREFIXES) {
               containsIgnoredPrefix = logName.startsWith(ignoredLogPrefix);
               if (containsIgnoredPrefix) {
                  break;
               }
            }

            if (!containsIgnoredPrefix) {
               out.write("  <filter name=\"" + logName + "\" value=\"" + NiagaraDaemonLogSettings.getLogLevel(logName) + "\" />\n");
            }
         }
      }

      out.write("</filters>\n");
      byte[] htmlBytes = out.toString().getBytes(StandardCharsets.UTF_8);
      resp.setStatus(200);
      resp.setHeader("Content-Type", "text/html");
      resp.setIntHeader("Content-Length", htmlBytes.length);

      try {
         resp.getOutputStream().write(htmlBytes);
      } catch (IOException ioe) {
         if (this.getServer() != null && this.getServer().getState() == 1) {
            this.filter.log(Level.SEVERE, "failed to write filters (" + ioe + ")", ioe);
            Http.sendError(req, resp, 500);
         }
      }
   }

   private void doSetFilters(HttpServletRequest req, HttpServletResponse resp) {
      boolean save = false;
      String queryString = req.getQueryString();
      KeyedList query = Http.getGetForm(queryString);

      for (int i = 0; i < query.size(); i++) {
         String key = query.getKey(i);
         String value = query.getAtIndex(i);
         if (key.equalsIgnoreCase("save")) {
            save = true;
         } else if (!key.equalsIgnoreCase("csrfToken")) {
            try {
               if (value == null) {
                  throw new IllegalArgumentException();
               }

               Level javaLogLevel = Level.parse(value);
               this.filter.info("set \"" + key + "\" log to " + javaLogLevel.getName());
               NiagaraDaemon.niagaraDaemonLogSettings.setLogLevel(key, javaLogLevel);
            } catch (IllegalArgumentException badValue) {
               MessageBundle msg = new MessageBundle("platform", "Servlet.invalidLogFilterLevel", value, "invalid log filter level " + value + " specified");
               this.filter.severe("invalid log filter level " + value + " specified for filter " + key);
               Http.sendErrorXML(req, resp, 400, msg);
               return;
            }
         }
      }

      if (save) {
         if (NiagaraDaemon.niagaraDaemonLogSettings.getForceLevel() != null) {
            this.filter.warning("can not process filter logs save request, filter logs forced to " + NiagaraDaemon.niagaraDaemonLogSettings.getForceLevel());
         } else {
            this.filter.fine("saving filter log properties");
            NiagaraDaemon.saveLogSettings();
         }
      }

      resp.setStatus(200);
      resp.setIntHeader("Content-Length", 0);
   }

   private void doSaveFilters(HttpServletRequest req, HttpServletResponse resp) {
      if (NiagaraDaemon.niagaraDaemonLogSettings.getForceLevel() != null) {
         this.filter.severe("can not process filter logs save request, filter logs forced to " + NiagaraDaemon.niagaraDaemonLogSettings.getForceLevel());
         Http.sendError(req, resp, 405);
      } else {
         this.filter.fine("saving filter log properties");
         boolean result = NiagaraDaemon.saveLogSettings();
         if (result) {
            resp.setStatus(200);
            resp.setIntHeader("Content-Length", 0);
         } else {
            Http.sendError(req, resp, 405);
         }
      }
   }
}
