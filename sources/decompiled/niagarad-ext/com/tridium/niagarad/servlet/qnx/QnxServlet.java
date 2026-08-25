package com.tridium.niagarad.servlet.qnx;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.servlet.Servlet;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class QnxServlet extends Servlet {
   private static final String HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"https://www.w3.org/TR/html4/strict.dtd\">\n<html>\n<body>\n<pre>\n";
   private static final String FOOTER = "</pre>\n</body>\n</html>\n";
   private static final String[] VALID_COMMANDS = buildValidCommands();
   private static final String[] VALID_SCRIPTS = buildValidScripts();
   private static final String[][] VALID_LINKS = buildValidLinks();

   public QnxServlet() {
      super("qnx");
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
         ArrayList<String> commandLine = new ArrayList<>();
         if (uri.length() > 1) {
            uri = uri.substring(1);
            char delimiters = '!';
            String[] args = TextUtil.splitAndTrim(uri, delimiters);
            if (args[0].equals("jmxinfo") || args[0].equals("threads")) {
               commandLine.add(args[0]);
               valid = true;
            } else if (args[0].equals("viewlog")) {
               if (args.length == 1) {
                  commandLine.add("slog2info");
                  valid = true;
               } else if (args.length == 2) {
                  if (args[1].equals("log1")) {
                     String logArgument = this.deglobArgument("/var/slog/log1_*");
                     if (logArgument != null) {
                        commandLine.add("cat");
                        commandLine.add(logArgument);
                        valid = true;
                     }
                  } else if (args[1].equals("log2")) {
                     String logArgument = this.deglobArgument("/var/slog/log2_*");
                     if (logArgument != null) {
                        commandLine.add("cat");
                        commandLine.add(logArgument);
                        valid = true;
                     }
                  }
               }
            } else if (args[0].equals("viewnet")) {
               commandLine.add("ifconfig");
               commandLine.add("-a");
               valid = true;
            } else if (args[0].equals("viewlfss")) {
               if (getQNXSysInfoString("/sys/info/model", "").equalsIgnoreCase("titan")) {
                  commandLine.add("lfss");
                  commandLine.add("-s");
                  valid = true;
               }
            } else {
               for (String valid_cmd : VALID_COMMANDS) {
                  if (args[0].equals(valid_cmd)) {
                     commandLine.addAll(Arrays.asList(args));
                     valid = true;
                     break;
                  }
               }

               if (!valid) {
                  for (String valid_script : VALID_SCRIPTS) {
                     if (args[0].equals(valid_script)) {
                        commandLine.add("ksh");
                        commandLine.addAll(Arrays.asList(args));
                        valid = true;
                        break;
                     }
                  }
               }
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

            for (String[] commandTuple : VALID_LINKS) {
               String commandDescription = commandTuple[0];
               String commandUri = commandTuple[1];
               String subLink = "<a href=\"/qnx/" + commandUri + "\">" + commandDescription + "</a>\n";
               out.write(subLink);
            }

            out.write("</pre>\n</body>\n</html>\n");
         } else if (commandLine.get(0).equals("jmxinfo")) {
            dumpJmxInfo(out);
         } else if (commandLine.get(0).equals("threads")) {
            dumpThreads(out);
         } else {
            spawnCommand(commandLine.toArray(new String[0]), out);
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

   private static int spawnCommand(String[] args, StringWriter output) {
      args[0] = "/proc/boot/" + args[0];
      ArrayList<String> stage = new ArrayList<>();

      for (String arg : args) {
         if (arg == null) {
            break;
         }

         stage.add(arg);
      }

      args = stage.toArray(new String[0]);
      ProcessBuilder builder = new ProcessBuilder(args);
      builder.redirectErrorStream(true);
      Process proc = null;

      int rc;
      try {
         proc = builder.start();
         proc.getOutputStream().close();

         try (InputStream in = proc.getInputStream()) {
            int len = 4096;
            byte[] buf = new byte[len];

            while (true) {
               int n = in.read(buf, 0, len);
               if (n < 0) {
                  break;
               }

               output.write(new String(buf, StandardCharsets.UTF_8).toCharArray(), 0, n);
            }
         }

         rc = proc.waitFor();
      } catch (Exception e) {
         if (proc != null) {
            proc.destroy();
         }

         output.write(e.getMessage() == null ? "Error launching command" : e.getMessage());
         rc = -1;
      }

      return rc;
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

   private String deglobArgument(String argument) {
      FileSystem defaultFileSystem = null;

      try {
         defaultFileSystem = FileSystems.getDefault();
         PathMatcher globMatcher = defaultFileSystem.getPathMatcher("glob:" + argument);
         File globFileParent = new File(argument).getParentFile();
         if (globFileParent != null && globFileParent.exists()) {
            File[] childrenFiles = globFileParent.listFiles();
            if (childrenFiles != null) {
               for (File childFile : childrenFiles) {
                  if (globMatcher.matches(childFile.toPath())) {
                     return childFile.getPath();
                  }
               }
            }
         }
      } finally {
         if (defaultFileSystem != null) {
            try {
               defaultFileSystem.close();
            } catch (Throwable var18) {
            }
         }
      }

      return null;
   }

   private static String[] buildValidCommands() {
      ArrayList<String> validCommandsList = new ArrayList<>();
      validCommandsList.add("df");
      validCommandsList.add("du");
      validCommandsList.add("fdcount");
      validCommandsList.add("nicinfo");
      validCommandsList.add("netstat");
      validCommandsList.add("pidin");
      validCommandsList.add("qspy");
      Collections.sort(validCommandsList);
      return validCommandsList.toArray(new String[0]);
   }

   private static String[] buildValidScripts() {
      ArrayList<String> validCommandsList = new ArrayList<>();
      validCommandsList.add("/sys/bin/si");
      Collections.sort(validCommandsList);
      return validCommandsList.toArray(new String[0]);
   }

   private static String[][] buildValidLinks() {
      ArrayList<String[]> validLinksList = new ArrayList<>();
      validLinksList.add(new String[]{"Niagara Daemon JMX Info", "jmxinfo"});
      validLinksList.add(new String[]{"Niagara Daemon Thread Info", "threads"});
      validLinksList.add(new String[]{"System information", "/sys/bin/si"});
      validLinksList.add(new String[]{"View network interface parameters", "viewnet"});
      validLinksList.add(new String[]{"View network statistics", "netstat!-A"});
      validLinksList.add(new String[]{"View network interface controllers", "nicinfo"});
      validLinksList.add(new String[]{"pidin", "pidin"});
      validLinksList.add(new String[]{"pidin arg", "pidin!arg"});
      validLinksList.add(new String[]{"pidin env", "pidin!env"});
      validLinksList.add(new String[]{"pidin family", "pidin!family"});
      validLinksList.add(new String[]{"pidin fds", "pidin!fds"});
      validLinksList.add(new String[]{"pidin in", "pidin!in"});
      validLinksList.add(new String[]{"pidin mem", "pidin!mem"});
      validLinksList.add(new String[]{"pidin pmem", "pidin!pmem"});
      validLinksList.add(new String[]{"pidin times", "pidin!times"});
      validLinksList.add(new String[]{"pidin ttimes", "pidin!ttimes"});
      validLinksList.add(new String[]{"Current CPU usage", "qspy"});
      validLinksList.add(new String[]{"Reset CPU usage", "qspy!-r"});
      validLinksList.add(new String[]{"Report free disk space", "df!-Pk"});
      validLinksList.add(new String[]{"Report disk usage", "du!-k!/"});
      validLinksList.add(new String[]{"Report file descriptor usage", "fdcount!-a"});
      validLinksList.add(new String[]{"View current system log", "viewlog"});
      validLinksList.add(new String[]{"View historical system log 1", "viewlog!log1"});
      validLinksList.add(new String[]{"View historical system log 2", "viewlog!log2"});
      if (getQNXSysInfoString("/sys/info/model", "").equalsIgnoreCase("titan")) {
         validLinksList.add(new String[]{"View linear flash strings", "viewlfss"});
      }

      String[][] validLinksArray = new String[validLinksList.size()][];

      for (int i = 0; i < validLinksList.size(); i++) {
         String[] row = validLinksList.get(i);
         validLinksArray[i] = row;
      }

      return validLinksArray;
   }

   private static String getQNXSysInfoString(String path, String defaultValue) {
      File file = new File(path);
      String returnValue = defaultValue;
      if (file.exists()) {
         try (BufferedReader fin = new BufferedReader(new FileReader(file))) {
            returnValue = fin.readLine();
            if (returnValue == null) {
               returnValue = defaultValue;
            }
         } catch (IOException ioe) {
            returnValue = defaultValue;
         }
      }

      return returnValue;
   }
}
