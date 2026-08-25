package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Authenticator;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.http.WebServer;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.SimpleErrorHandler;
import com.tridium.niagarad.util.KeyedList;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Level;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BatchServlet extends Servlet {
   public static final int MAX_POST_FORM_TRANSFER = 2097152;

   public BatchServlet() {
      super("batch");
   }

   @Override
   public void doPost(HttpServletRequest req, HttpServletResponse resp) {
      int contentLength = req.getIntHeader("Content-Length");
      if (contentLength == -1) {
         MessageBundle msg = new MessageBundle("requested chunked input stream not supported, rejecting request");
         this.getServer().getFilter().severe("requested chunked input stream not supported, rejecting request");
         req.setAttribute("maxPostContentLength", 2097152);
         Http.sendErrorXML(req, resp, 415, msg);
      } else if (contentLength <= 2097152 && Http.validateHeapAvailable(contentLength)) {
         resp.setStatus(200);
         resp.setHeader("Content-Type", "text/xml");
         BatchServlet.Item items = null;

         String line;
         try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null && line.length() != 0 && line.charAt(0) != '\n') {
               if (items == null) {
                  items = new BatchServlet.Item(line, this.getServer());
               } else {
                  items.last().next = new BatchServlet.Item(line, this.getServer());
               }
            }
         } catch (Exception e) {
            this.getServer().getFilter().log(Level.SEVERE, "batch: failed to write response (" + e + ")");
            if (this.getServer().getFilter().isLoggable(Level.FINE)) {
               this.getServer().getFilter().log(Level.SEVERE, "Stack trace: ", e);
            }

            Http.sendError(req, resp, 400);
            return;
         }

         ByteBuffer buffer = new ByteBuffer();
         XWriter content = new XWriter();
         content.setOutputStream(buffer.getOutputStream());
         content.prolog();
         content.w("<batch>\n");

         try {
            for (BatchServlet.Item item = items; item != null; item = item.next) {
               if (item.servlet == null) {
                  MessageBundle msg = new MessageBundle("BatchServlet: Not Found");
                  content.w("<error ").attr("code", String.valueOf(404)).w(">\n");
                  msg.appendXML(content);
                  content.w("</error>\n");
               } else {
                  SimpleErrorHandler handler = new SimpleErrorHandler();
                  ByteBuffer sbuffer = new ByteBuffer();
                  XWriter scontent = new XWriter();
                  scontent.setOutputStream(sbuffer.getOutputStream());
                  int rc;
                  if (item.servlet.useDefaultAuthentication()
                     || !item.servlet.requiresAuthentication()
                     || item.servlet.requiresAuthentication() && item.servlet.authenticate(req, resp)) {
                     try {
                        if (InetAddress.getByName(req.getLocalAddr()).isLoopbackAddress()) {
                           Authenticator.addRunningStationExemption(req);
                        }
                     } catch (Exception var32) {
                     }

                     if (this.getServer().getFilter().isLoggable(Level.FINE)) {
                        this.getServer().getFilter().fine("batch: servlet handle GET " + item.rawUri);
                     }

                     rc = item.servlet.doGet(req, handler, item.query, scontent);
                  } else {
                     rc = 401;
                  }

                  scontent.flush();
                  scontent.close();
                  if (rc > 299 || handler.getLastError() != null) {
                     content.w("<error ").attr("code", String.valueOf(rc)).w(">\n");
                     MessageBundle error = handler.getLastError() != null ? handler.getLastError() : new MessageBundle(Http.getReasonPhrase(rc));
                     error.appendXML(content);
                     content.w("</error>\n");
                  } else if (sbuffer.toByteArray().length == 0) {
                     content.w("<success ").attr("statusCode", String.valueOf(rc)).w("/>\n");
                  } else {
                     content.w(new String(sbuffer.toByteArray()));
                  }
               }
            }

            if (items != null) {
               items.clearNext();
            }

            content.w("</batch>\n");
            content.flush();
            content.close();
            byte[] xmlBytes = buffer.toByteArray();
            resp.setIntHeader("Content-Length", xmlBytes.length);

            try {
               resp.getOutputStream().write(xmlBytes);
            } catch (IOException ioe) {
               if (this.getServer() != null && this.getServer().getState() == 1) {
                  this.getServer().getFilter().log(Level.SEVERE, "batch: failed to write response (" + ioe + ")", ioe);
                  Http.sendError(req, resp, 500);
               }
            }
         } finally {
            Authenticator.removeRunningStationExemption(req);
         }
      } else {
         MessageBundle msg = new MessageBundle("content length " + contentLength + " exceeds maximum allowed transfer size " + 2097152 + ", rejecting request");
         this.getServer().getFilter().severe("content length " + contentLength + " exceeds maximum allowed transfer size " + 2097152 + ", rejecting request");
         req.setAttribute("maxPostContentLength", 2097152);
         Http.sendErrorXML(req, resp, 413, msg);
      }
   }

   private static class Item {
      public DaemonServlet servlet;
      public KeyedList query;
      public BatchServlet.Item next = null;
      public String rawUri;

      public Item(String rawUri, WebServer server) {
         this.servlet = null;
         this.query = null;
         String encquery = "";
         int questionMarkIndex = rawUri.indexOf(63);
         String encuri;
         if (questionMarkIndex == -1) {
            encuri = rawUri;
         } else {
            encuri = rawUri.substring(0, questionMarkIndex);
            if (questionMarkIndex != rawUri.length()) {
               encquery = rawUri.substring(questionMarkIndex + 1);
            }
         }

         String servletName = Http.getServletName(encuri);
         if (servletName.length() == 0) {
            this.servlet = null;
         } else {
            Servlet tempServlet = server.getServlet(servletName);
            if (tempServlet.isDaemonServlet()) {
               this.servlet = (DaemonServlet)tempServlet;
            } else {
               this.servlet = null;
            }

            if (this.servlet != null && encquery.length() != 0) {
               this.query = Http.getGetForm(encquery);
            }

            this.rawUri = rawUri;
         }
      }

      BatchServlet.Item last() {
         return this.next == null ? this : this.next.last();
      }

      void clearNext() {
         if (this.next != null) {
            this.next.clearNext();
            this.next = null;
         }
      }
   }
}
