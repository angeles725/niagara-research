package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.SimpleErrorHandler;
import com.tridium.niagarad.util.KeyedList;
import java.io.IOException;
import java.util.logging.Level;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class DaemonServlet extends Servlet {
   protected volatile boolean responseSent;
   protected volatile boolean errorOnSend;
   protected HttpServletResponse response;
   private final ByteBuffer responseBuffer = new ByteBuffer();
   private XWriter xmlWriter;

   protected DaemonServlet(String servletName) {
      super(servletName);

      try {
         this.xmlWriter = new XWriter(this.responseBuffer.getOutputStream());
      } catch (IOException var3) {
      }
   }

   @Override
   public final synchronized void doGet(HttpServletRequest req, HttpServletResponse resp) {
      this.responseSent = false;
      this.errorOnSend = false;
      String queryString = req.getQueryString();
      ErrorHandler handler = new SimpleErrorHandler();
      KeyedList queryList = queryString != null ? Http.getGetForm(queryString) : new KeyedList();
      this.response = resp;
      resp.setStatus(this.doGet(req, handler, queryList, this.xmlWriter));
      if (!this.errorOnSend) {
         this.sendResponse(req, resp, handler, this.responseBuffer, this.xmlWriter, this.responseSent);
      }
   }

   protected final void sendResponse(
      HttpServletRequest req, HttpServletResponse resp, ErrorHandler handler, ByteBuffer buffer, XWriter content, boolean partialResponse
   ) {
      if (buffer == null) {
         buffer = this.responseBuffer;
      }

      if (this.errorOnSend) {
         if (this.getServer().getFilter().isLoggable(Level.FINE)) {
            this.getServer().getFilter().fine(this.getName() + ": ignoring sendResponse for request \"" + req.getRequestURI() + "\", errorOnSend == true");
         }

         buffer.reset();
      } else if (this.responseSent || resp.getStatus() <= 299 && handler.getLastError() == null) {
         content.flush();
         if (!this.responseSent && buffer.getLength() == 0) {
            content.prolog();
            content.w("<success ").attr("statusCode", String.valueOf(resp.getStatus())).w("/>").nl();
            content.flush();
         }

         if (!this.responseSent && resp.getHeader("Content-Type") == null) {
            resp.setHeader("Content-Type", "text/xml");
         }

         byte[] contentBuffer = buffer.toByteArray();
         if (!partialResponse) {
            resp.setIntHeader("Content-Length", contentBuffer.length);
         }

         try {
            resp.getOutputStream().write(contentBuffer);
            Http.flushResponseBuffer(this.getServer().getFilter(), req, resp);
         } catch (IOException e) {
            String logMessage = "error occurred on thread \""
               + Thread.currentThread().getName()
               + "\" when sending response to \""
               + req.getRequestURI()
               + (req.getQueryString() == null ? "" : "?" + req.getQueryString())
               + "\": "
               + e;
            if (this.getServer().getState() == 1) {
               this.getServer().getFilter().log(Level.SEVERE, this.getName() + ": " + logMessage, e);
            }

            handler.error(e.toString());
            this.errorOnSend = true;
         }

         buffer.reset();
         this.responseSent = true;
      } else {
         MessageBundle errorMessage = handler.getLastError() != null ? handler.getLastError() : new MessageBundle(Http.getReasonPhrase(resp.getStatus()));
         Http.sendErrorXML(req, resp, resp.getStatus(), errorMessage);
         buffer.reset();
      }
   }

   protected int checkMandatoryParams(ErrorHandler handler, KeyedList query, String... params) {
      for (String param : params) {
         if ("".equals(query.get(param, ""))) {
            MessageBundle msg = new MessageBundle("platform", "Servlet.missingParam", param, this.getName() + "Servlet: Missing " + param + " argument");
            handler.error(msg);
            return 400;
         }
      }

      return 200;
   }

   public abstract int doGet(HttpServletRequest var1, ErrorHandler var2, KeyedList var3, XWriter var4);

   @Override
   public boolean isDaemonServlet() {
      return true;
   }
}
