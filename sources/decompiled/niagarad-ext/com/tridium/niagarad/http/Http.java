package com.tridium.niagarad.http;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.util.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.websocket.api.UpgradeRequest;

public final class Http {
   public static final String HTTP_VERSION = "HTTP/1.1";
   public static final String PROTOCOL_HTTP = "http";
   public static final int DEFAULT_HTTP_PORT = 80;
   public static final String PROTOCOL_HTTPS = "https";
   public static final int DEFAULT_HTTPS_PORT = 443;
   public static final int CR = 13;
   public static final int LF = 10;
   public static final String CRLF = "\r\n";
   public static final String METHOD_OPTIONS = "OPTIONS";
   public static final String METHOD_GET = "GET";
   public static final String METHOD_HEAD = "HEAD";
   public static final String METHOD_POST = "POST";
   public static final String METHOD_PUT = "PUT";
   public static final String METHOD_DELETE = "DELETE";
   public static final String METHOD_TRACE = "TRACE";
   public static final String TRANSFER_CHUNKED = "chunked";
   public static final String SESSION_ID = "sessionId";
   public static final String USER_AGENT = "user-agent";
   public static final String MAX_POST_CONTENT_LENGTH = "maxPostContentLength";
   public static final String NIAGARAD_SESSION_COOKIE = "NIAGARA_DAEMON_SESSION_ID";
   public static final String FORM_CONTENT_TYPE = "application/x-www-form-encoded";
   public static final int SC_CONTINUE = 100;
   public static final int SC_SWITCHING_PROTOCOLS = 101;
   public static final int SC_OK = 200;
   public static final int SC_CREATED = 201;
   public static final int SC_ACCEPTED = 202;
   public static final int SC_NON_AUTHORITATIVE = 203;
   public static final int SC_NO_CONTENT = 204;
   public static final int SC_RESET_CONTENT = 205;
   public static final int SC_PARTIAL_CONTENT = 206;
   public static final int SC_MULTIPLE_CHOICES = 300;
   public static final int SC_MOVED_PERMANENTLY = 301;
   public static final int SC_MOVED_TEMPORARILY = 302;
   public static final int SC_SEE_OTHER = 303;
   public static final int SC_NOT_MODIFIED = 304;
   public static final int SC_USE_PROXY = 305;
   public static final int SC_BAD_REQUEST = 400;
   public static final int SC_UNAUTHORIZED = 401;
   public static final int SC_PAYMENT_REQUIRED = 402;
   public static final int SC_FORBIDDEN = 403;
   public static final int SC_NOT_FOUND = 404;
   public static final int SC_METHOD_NOT_ALLOWED = 405;
   public static final int SC_NOT_ACCEPTABLE = 406;
   public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
   public static final int SC_REQUEST_TIME_OUT = 408;
   public static final int SC_CONFLICT = 409;
   public static final int SC_GONE = 410;
   public static final int SC_LENGTH_REQUIRED = 411;
   public static final int SC_PRECONDITION_FAILED = 412;
   public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
   public static final int SC_REQUEST_URI_TOO_LARGE = 414;
   public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
   public static final int SC_TOO_MANY_REQUESTS = 429;
   public static final int SC_REQUEST_HEADER_FIELDS_TOO_LARGE = 431;
   public static final int SC_INTERNAL_SERVER_ERROR = 500;
   public static final int SC_NOT_IMPLEMENTED = 501;
   public static final int SC_BAD_GATEWAY = 502;
   public static final int SC_SERVICE_UNAVAILABLE = 503;
   public static final int SC_GATEWAY_TIME_OUT = 504;
   public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
   private static Properties mimeTable = null;
   private static String defMimeType = null;
   private static HashMap<Integer, String> reasonTable = buildReasonTable();

   private Http() {
   }

   public static String getReasonPhrase(int statusCode) {
      String reason = reasonTable.get(statusCode);
      if (reason == null) {
         reason = "Error";
      }

      return reason;
   }

   public static String getMimeType(String ext) {
      String result = null;
      if (ext != null && mimeTable != null) {
         result = mimeTable.getProperty(ext, "");
         if (result.isEmpty() && getDefaultMimeType() != null) {
            result = getDefaultMimeType();
         }
      } else if (getDefaultMimeType() != null) {
         result = getDefaultMimeType();
      }

      return result;
   }

   public static void setDefaultMimeType(String newDef) {
      if (defMimeType != null) {
         defMimeType = null;
      }

      if (newDef != null) {
         defMimeType = newDef;
      }
   }

   public static String getDefaultMimeType() {
      if (defMimeType != null) {
         return defMimeType;
      }

      if (mimeTable != null) {
         defMimeType = mimeTable.getProperty("*", "");
         if (defMimeType.isEmpty()) {
            defMimeType = null;
         }

         return defMimeType;
      } else {
         return null;
      }
   }

   public static void loadMimeTable(Logger log, String fileName) {
      mimeTable = new Properties();
      File mimeProperties = new File(fileName);
      FileInputStream inputStream = null;

      try {
         inputStream = new FileInputStream(mimeProperties);
         mimeTable.load(inputStream);
      } catch (FileNotFoundException var15) {
      } catch (IOException ioe) {
         log.log(Level.SEVERE, "failed to load webserver mime table (" + ioe + ")", ioe);
      } finally {
         if (inputStream != null) {
            try {
               inputStream.close();
            } catch (IOException var14) {
            }
         }
      }
   }

   public static void setMimeTable(Properties newTable) {
      mimeTable = newTable;
   }

   public static synchronized void addMimeType(String ext, String mimeType) {
      if (mimeTable == null) {
         mimeTable = new Properties();
      }

      mimeTable.setProperty(ext, mimeType);
   }

   public static int fromHex(char ch) {
      if (ch >= '0' && ch <= '9') {
         return ch - 48;
      } else {
         return ch >= 65 && ch <= 70 ? ch - 65 + 10 : -1;
      }
   }

   public static boolean isNiagaraClient(HttpServletRequest req) {
      return isNiagaraClient(req.getHeader("user-agent"));
   }

   public static boolean isNiagaraClient(UpgradeRequest req) {
      return isNiagaraClient(req.getHeader("user-agent"));
   }

   private static boolean isNiagaraClient(String userAgent) {
      return userAgent != null && userAgent.startsWith("Niagara/");
   }

   public static Version getNiagaraClientVersion(HttpServletRequest req) {
      if (!isNiagaraClient(req)) {
         return null;
      }

      Version clientVersion = null;

      try {
         String niagaraVersion = req.getHeader("user-agent");
         String version = niagaraVersion.substring(8);
         clientVersion = new Version(version);
      } catch (Exception var4) {
      }

      return clientVersion;
   }

   public static String getServletName(String uri) {
      if (uri != null && uri.length() != 0) {
         if (uri.charAt(0) != '/') {
            return null;
         }

         if (uri.length() == 1) {
            return "";
         }

         String uriWithoutSlash = uri.substring(1);
         int indexOfSlash = uriWithoutSlash.indexOf(47);
         return indexOfSlash != -1 ? uri.substring(1, indexOfSlash + 1) : uri.substring(1);
      } else {
         return null;
      }
   }

   public static KeyedList getGetForm(String queryString) {
      KeyedList table = new KeyedList();
      if (queryString == null) {
         return table;
      }

      int len = queryString.length();
      char[] characters = queryString.toCharArray();
      int start = 0;
      int end = 0;

      while (end < len) {
         while (end < len && characters[end] != '=') {
            end++;
         }

         String key = decodeUri(queryString.substring(start, end));
         start = ++end;
         String val;
         if (end >= len) {
            val = "";
         } else {
            while (end < len && characters[end] != '&') {
               end++;
            }

            val = decodeUri(queryString.substring(start, end));
            if (end != len) {
               start = ++end;
            }
         }

         table.add(key, val);
      }

      return table;
   }

   public static String decodeUri(String s) {
      try {
         return URLDecoder.decode(s, "UTF-8");
      } catch (Exception e) {
         NiagaraDaemon.getFilter().log(Level.WARNING, "URLDecoder failed, using fallback ASCII URI decoder", e);
         String decoded1 = s.replace('+', ' ');
         StringBuilder decoded2 = new StringBuilder();

         for (int i = 0; i < decoded1.length(); i++) {
            if (s.charAt(i) == '%') {
               if (i + 2 >= decoded1.length()) {
                  break;
               }

               int intVal = 0;
               intVal += fromHex(s.charAt(i + 1)) * 16;
               intVal += fromHex(s.charAt(i + 2));
               decoded2.append((char)intVal);
               i += 2;
            } else {
               decoded2.append(decoded1.charAt(i));
            }
         }

         return decoded2.toString();
      }
   }

   private static HashMap<Integer, String> buildReasonTable() {
      reasonTable = new HashMap<>();
      reasonTable.put(100, "Continue");
      reasonTable.put(101, "Switching Protocols");
      reasonTable.put(200, "OK");
      reasonTable.put(201, "Created");
      reasonTable.put(202, "Accepted");
      reasonTable.put(203, "Non-Authoritative Information");
      reasonTable.put(204, "No Content");
      reasonTable.put(205, "Reset Content");
      reasonTable.put(206, "Partial Content");
      reasonTable.put(300, "Multiple Choices");
      reasonTable.put(301, "Moved Permanently");
      reasonTable.put(302, "Moved Temporarily");
      reasonTable.put(303, "See Other");
      reasonTable.put(304, "Not Modified");
      reasonTable.put(305, "Use Proxy");
      reasonTable.put(400, "Bad Request");
      reasonTable.put(401, "Unauthorized");
      reasonTable.put(402, "Payment Required");
      reasonTable.put(403, "Forbidden");
      reasonTable.put(404, "Not Found");
      reasonTable.put(405, "Method Not Allowed");
      reasonTable.put(406, "Not Acceptable");
      reasonTable.put(407, "Proxy Authentication Required");
      reasonTable.put(408, "Request Timeout");
      reasonTable.put(409, "Conflict");
      reasonTable.put(410, "Gone");
      reasonTable.put(411, "Length Required");
      reasonTable.put(412, "Precondition Failed");
      reasonTable.put(413, "Request Entity Too Large");
      reasonTable.put(414, "Request URI to large");
      reasonTable.put(415, "Unsupported Media Type");
      reasonTable.put(429, "Too Many Requests");
      reasonTable.put(431, "Request Header Fields Too Large");
      reasonTable.put(500, "Internal Server Error");
      reasonTable.put(501, "Not Implemented");
      reasonTable.put(502, "Bad Gateway");
      reasonTable.put(503, "Service Unavailable");
      reasonTable.put(504, "Gateway Timeout");
      reasonTable.put(505, "HTTP Version Not Supported");
      return reasonTable;
   }

   public static void sendError(HttpServletRequest request, HttpServletResponse response, int statusCode) {
      readFully(request, statusCode);
      response.setStatus(statusCode);
      if (statusCode / 100 != 1 && statusCode != 204 && statusCode != 304) {
         if (statusCode == 403 || statusCode == 500 || statusCode == 404) {
            response.setHeader("Connection", "close");
         }

         if (!DebugServlet.debugEnabled) {
            response.setIntHeader("Content-Length", 0);
         } else {
            String reason = getReasonPhrase(statusCode);
            ByteBuffer writer = new ByteBuffer();

            XWriter xml;
            try {
               xml = new XWriter(writer.getOutputStream());
            } catch (IOException ioe) {
               WebServer server = NiagaraDaemon.getInstance().webServer;
               if (server != null && server.getState() == 1) {
                  response.setStatus(500);
                  NiagaraDaemon.getFilter().log(Level.SEVERE, "failed to send HTTP error for msg '" + reason + "'", ioe);
               }

               return;
            }

            xml.w("<html>\n");
            xml.w("<body>\n");
            xml.w("<h1>");
            xml.w(statusCode + ": " + reason);
            xml.w("</h1>\n");
            xml.w("</body>\n");
            xml.w("</html>");
            xml.flush();
            xml.close();
            byte[] xmlBytes = writer.toByteArray();
            response.setIntHeader("Content-Length", xmlBytes.length);
            response.setHeader("Content-Type", "text/html");
            if (!request.getMethod().equalsIgnoreCase("HEAD")) {
               try {
                  response.getOutputStream().write(xmlBytes);
               } catch (IOException ioe) {
                  WebServer server = NiagaraDaemon.getInstance().webServer;
                  if (server != null && server.getState() == 1) {
                     response.setStatus(500);
                     NiagaraDaemon.getFilter().log(Level.SEVERE, "failed to send HTTP error for msg '" + reason + "'", ioe);
                  }
               }
            }
         }
      }
   }

   public static void sendErrorXML(HttpServletRequest request, HttpServletResponse response, int statusCode, MessageBundle msg) {
      readFully(request, statusCode);
      response.setStatus(statusCode);
      if (statusCode / 100 != 1 && statusCode != 204 && statusCode != 304) {
         if (statusCode == 403 || statusCode == 500 || statusCode == 404) {
            response.setHeader("Connection", "close");
         }

         ByteBuffer writer = new ByteBuffer();

         XWriter xml;
         try {
            xml = new XWriter(writer.getOutputStream());
         } catch (IOException ioe) {
            WebServer server = NiagaraDaemon.getInstance().webServer;
            if (server != null && server.getState() == 1) {
               response.setStatus(500);
               NiagaraDaemon.getFilter().log(Level.SEVERE, "failed to send HTTP error XML for msg '" + msg.getNonLocalizedMessage() + "'", ioe);
            }

            return;
         }

         xml.prolog();
         xml.w("<error ").attr("code", String.valueOf(statusCode)).w(">\n");
         msg.appendXML(xml);
         xml.w("</error>\n");
         xml.flush();
         xml.close();
         byte[] xmlBytes = writer.toByteArray();
         response.setIntHeader("Content-Length", xmlBytes.length);
         response.setHeader("Content-Type", "text/xml");
         if (!request.getMethod().equalsIgnoreCase("HEAD")) {
            try {
               response.getOutputStream().write(xmlBytes);
            } catch (IOException ioe) {
               WebServer server = NiagaraDaemon.getInstance().webServer;
               if (server != null && server.getState() == 1) {
                  response.setStatus(500);
                  NiagaraDaemon.getFilter().log(Level.SEVERE, "failed to send HTTP error XML for msg '" + msg.getNonLocalizedMessage() + "'", ioe);
               }
            }
         }
      }
   }

   public static boolean validateHeapAvailable(long contentLength) {
      return Runtime.getRuntime().freeMemory() - 262144L > contentLength;
   }

   private static void readFully(HttpServletRequest request, int statusCode) {
      if (statusCode != 403) {
         int length = request.getIntHeader("Content-Length");
         if (length >= 0) {
            Integer maximumPostInputLength = (Integer)request.getAttribute("maxPostContentLength");
            if (maximumPostInputLength != null && length > maximumPostInputLength) {
               NiagaraDaemon.getFilter()
                  .warning(
                     "request "
                        + request.getMethod()
                        + ": "
                        + request.getRequestURI()
                        + " specified unsupported input stream length "
                        + length
                        + ", rejecting client input stream"
                  );
            } else {
               try {
                  while (request.getInputStream().read() > 0) {
                  }
               } catch (Exception var5) {
               }
            }
         }
      }
   }

   public static void flushResponseBuffer(Logger log, HttpServletRequest request, HttpServletResponse response) {
      try {
         if (response.getOutputStream() instanceof HttpOutput) {
            HttpOutput responseOutput = (HttpOutput)response.getOutputStream();
            responseOutput.flush();
         } else if (log != null) {
            log.log(
               Level.WARNING,
               "unrecognized HttpServletResponse output stream in use for request "
                  + request.getMethod()
                  + " '"
                  + TextUtil.truncate(request.getRequestURI(), 25)
                  + "...', skipping explicit flush request"
            );
         }
      } catch (IOException ioe) {
         WebServer webServer = NiagaraDaemon.getInstance().webServer;
         if (webServer != null && webServer.getState() == 1) {
            String logMessage = "error occurred writing "
               + response.getStatus()
               + " response to client for request "
               + request.getMethod()
               + " '"
               + TextUtil.truncate(request.getRequestURI(), 25)
               + "...' ("
               + ioe
               + ")";
            if (log != null) {
               log.log(Level.SEVERE, logMessage, ioe);
            } else {
               System.err.println("SEVERE [" + new Date() + "][webserver] " + logMessage);
               ioe.printStackTrace();
            }
         }
      }
   }
}
