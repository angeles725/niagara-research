package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DiagnosticServlet extends Servlet {
   private static final String HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"https://www.w3.org/TR/html4/strict.dtd\">\n<html>\n<body>\n<pre>\n";
   private static final String FOOTER = "</pre>\n</body>\n</html>\n";
   private static final String[][] VALID_LINKS = buildValidLinks();
   private final IPlatformProvider platformProvider;

   public DiagnosticServlet(IPlatformProvider platformProvider) {
      super("diagnostic");
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
      String uri = this.getUriWithoutName(req.getRequestURI());
      KeyedList query = Http.getGetForm(req.getQueryString());
      Logger logger = NiagaraDaemon.getFilter();
      if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
         logger.severe("invalid CSRF token in request to " + this.getName());
         Http.sendError(req, resp, 403);
      } else {
         boolean valid = false;
         int uriIndex = 0;
         if (uri.length() > 1) {
            uri = uri.substring(1);
            if (!uri.equals("jmxinfo") && !uri.equals("threads")) {
               try {
                  uriIndex = Integer.parseInt(uri);
                  if (uriIndex >= 0 && uriIndex < VALID_LINKS.length - 1) {
                     valid = true;
                  }
               } catch (NumberFormatException var17) {
               }
            } else {
               valid = true;
            }
         }

         resp.setStatus(200);
         if (valid) {
            resp.setHeader("Content-Type", "text/plain");
         } else {
            resp.setHeader("Content-Type", "text/html");
         }

         StringWriter out = new StringWriter();
         if (!valid) {
            out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"https://www.w3.org/TR/html4/strict.dtd\">\n<html>\n<body>\n<pre>\n");
            out.write("Diagnostics Servlet\n");
            out.write("\n");
            out.write("<table>\n");

            for (String[] commandTuple : VALID_LINKS) {
               String commandDescription = commandTuple[0];
               String commandCommand = commandTuple[1];
               String commandUri = commandTuple[2];
               String subLink = "<tr><td><a href=\"/diagnostic/"
                  + commandUri
                  + "\">"
                  + commandDescription
                  + "</a></td><td style=\"padding-left: 20px;\">"
                  + commandCommand
                  + "</td></tr>\n";
               out.write(subLink);
            }

            out.write("</table>\n");
            out.write("</pre>\n</body>\n</html>\n");
         } else if (uri.equals("jmxinfo")) {
            dumpJmxInfo(out);
         } else if (uri.equals("threads")) {
            dumpThreads(out);
         } else {
            String commandOutput = this.platformProvider.executeNativeDiagnosticsCommand(uriIndex);
            if (commandOutput == null) {
               commandOutput = "(Error launching command)";
            } else if (commandOutput.trim().length() == 0) {
               commandOutput = "(Command output empty)";
            }

            out.write(commandOutput);
         }

         byte[] htmlBytes = out.toString().getBytes(StandardCharsets.UTF_8);
         resp.setStatus(200);
         resp.setIntHeader("Content-Length", htmlBytes.length);

         try {
            resp.getOutputStream().write(htmlBytes);
         } catch (IOException ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               this.getServer().getFilter().log(Level.SEVERE, this.getName() + ": failed to write diagnostics menu (" + ioe + ")", ioe);
               Http.sendError(req, resp, 500);
            }
         }
      }
   }

   private static void dumpThreads(StringWriter out) {
      out.write(new Date() + "\n");
      out.write("Full thread dump\n\n");

      for (Thread t : Thread.getAllStackTraces().keySet()) {
         out.write("\"" + t.getName() + "\"" + (t.isDaemon() ? " daemon" : "") + " prio=" + t.getPriority() + " tid=" + t.getId() + "\n");
         out.write("   java.lang.Thread.State: " + t.getState() + "\n");

         for (StackTraceElement ste : t.getStackTrace()) {
            out.write("\t" + ste.toString() + "\n");
         }

         out.write("\n");
      }
   }

   private static void dumpJmxInfo(StringWriter out) {
      out.write("\nJMX Thread Info\n");
      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      out.write("\tpeak thread count    " + threadBean.getPeakThreadCount() + "\n");
      out.write("\tcurrent thread count " + threadBean.getThreadCount() + "\n");
      out.write("\ttotal started count  " + threadBean.getTotalStartedThreadCount() + "\n");
      out.write("\nJMX Class Loading Info\n");
      ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();
      out.write("\tloaded class  " + classLoadingBean.getLoadedClassCount() + "\n");
      out.write("\ttotal loaded  " + classLoadingBean.getTotalLoadedClassCount() + "\n");
      out.write("\tunloaded      " + classLoadingBean.getUnloadedClassCount() + "\n");
      out.write("\nJMX Memory Pool Info\n\n");

      for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
         MemoryUsage currentUsage = pool.getUsage();
         MemoryUsage peakUsage = pool.getPeakUsage();
         out.write(pool.getName() + "\n");
         out.write("\tcurrent " + currentUsage + "\n");
         out.write("\tpeak    " + peakUsage + "\n\n");
      }

      Runtime runtime = Runtime.getRuntime();
      long total = runtime.totalMemory() / 1024L;
      long max = runtime.maxMemory() / 1024L;
      long free = runtime.freeMemory() / 1024L;
      long used = total - free;
      out.write("\nHeap Information\n\n");
      out.write("\ttotal memory " + total + "K\n");
      out.write("\tmax memory   " + max + "K\n");
      out.write("\tused memory  " + used + "K\n");
      out.write("\tfree memory  " + free + "K\n");
   }

   private static String[][] buildValidLinks() {
      ArrayList<String[]> validLinksList = new ArrayList<>();
      validLinksList.add(new String[]{"Niagara Daemon JMX Info", "jmxinfo", "jmxinfo"});
      validLinksList.add(new String[]{"Niagara Daemon Thread Info", "threads", "threads"});
      String[][] diagnosticCommands = PlatformUtil.getPlatformProvider().getNativeDiagnosticsCommands();
      if (diagnosticCommands != null && diagnosticCommands.length != 0) {
         for (int i = 0; i < diagnosticCommands.length; i++) {
            String[] commandTuple = diagnosticCommands[i];
            String description = commandTuple[0];
            String command = commandTuple[1];
            validLinksList.add(new String[]{description, command, String.valueOf(i)});
         }
      }

      diagnosticCommands = new String[validLinksList.size()][];

      for (int i = 0; i < validLinksList.size(); i++) {
         String[] row = validLinksList.get(i);
         diagnosticCommands[i] = row;
      }

      return diagnosticCommands;
   }
}
