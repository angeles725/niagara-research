package com.tridium.webChart;

import com.tridium.json.JSONObject;
import com.tridium.json.JSONTokener;
import com.tridium.nre.util.Version;
import com.tridium.web.RestUtil;
import com.tridium.web.WebUtil;
import com.tridium.web.RestUtil.Accept;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.regex.Pattern;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.sys.Context;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class WebChartFileServlet extends HttpServlet {
   private static final boolean indentChartContent = AccessController.doPrivileged(
      (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.webChart.indentChartFile"))
   );
   private static final Pattern chartNameFilter = Pattern.compile("[|]|([.][.])");
   private static final Version version = new Version("1");

   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      Context cx = (Context)req.getAttribute("niagara.context");
      if (!BFileSystem.INSTANCE.getPermissions(cx).hasOperatorWrite()) {
         resp.sendError(403);
      } else {
         RestUtil util = new RestUtil(req, resp);
         String chartPath = this.getChartPath("save", util, resp);
         if (chartPath.length() != 0) {
            chartPath = "^" + chartPath;
            String content = null;
            if (chartPath.endsWith(".chart")) {
               try (BufferedReader reader = req.getReader()) {
                  JSONObject chartJson = new JSONObject(new JSONTokener(reader));
                  content = indentChartContent ? chartJson.toString(2) : chartJson.toString();
               } catch (Throwable var23) {
                  resp.sendError(403);
                  return;
               }
            } else if (!chartPath.endsWith(".csv")) {
               resp.sendError(406);
               return;
            }

            String finalChartPath = chartPath;
            String finalContent = content;

            try {
               AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
                  FilePath path = new FilePath(finalChartPath);
                  boolean replace = false;
                  BIFile file = BFileSystem.INSTANCE.findFile(path);
                  if (file != null) {
                     if (!BFileSystem.INSTANCE.getPermissionsFor(path, cx).hasOperatorWrite()) {
                        resp.sendError(403);
                        return null;
                     }

                     replace = true;
                  }

                  OutputStream outputStream = null;
                  if (replace) {
                     outputStream = BFileSystem.INSTANCE.findFile(path).getOutputStream();
                  } else {
                     BIFile newFile = BFileSystem.INSTANCE.makeFile(path, cx);
                     outputStream = newFile.getOutputStream();
                  }

                  if (finalContent != null) {
                     try {
                        outputStream.write(finalContent.getBytes("UTF-8"));
                        outputStream.flush();
                     } catch (IOException var21) {
                        resp.sendError(400);
                     } finally {
                        WebUtil.closeOutputStream(outputStream);
                     }
                  } else {
                     InputStream inputStream = req.getInputStream();

                     Object var11;
                     try {
                        BajaFileUtil.pipe(inputStream, outputStream);
                        return null;
                     } catch (Throwable var23x) {
                        resp.sendError(403);
                        var11 = null;
                     } finally {
                        WebUtil.closeOutputStream(outputStream);
                        if (inputStream != null) {
                           inputStream.close();
                        }
                     }

                     return (Void)var11;
                  }

                  return null;
               }));
            } catch (PrivilegedActionException var20) {
               WebUtil.handleServletPrivilegedException(var20);
            }
         }
      }
   }

   private String getChartPath(String cmd, RestUtil util, HttpServletResponse resp) throws ServletException, IOException {
      if (!util.acceptJson()) {
         resp.sendError(406);
         return "";
      } else {
         Accept accept = util.getJsonAccept();
         if (!accept.isCustomMediaType() || accept.getCustomProtocol().equals("webChart") && accept.getCustomVersion().equals(version)) {
            if (util.has(0, cmd) && util.has(1)) {
               String chartPath = util.getPathInfo().substring(cmd.length() + 2);
               if (chartNameFilter.matcher(chartPath).find()) {
                  resp.sendError(403);
                  chartPath = "";
               }

               return chartPath;
            } else {
               resp.sendError(404);
               return "";
            }
         } else {
            resp.sendError(406);
            return "";
         }
      }
   }
}
