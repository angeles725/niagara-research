package com.tridium.niagarad.log;

import com.tridium.niagarad.NiagaraDaemon;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletResponse;

public class HttpLogger extends Logger {
   private HttpServletResponse respDest;
   private boolean errorInWrite = false;
   private boolean inited = false;
   private int bytesWritten = 0;

   protected HttpLogger(String name, String resourceBundleName) {
      super(name, resourceBundleName);
   }

   public HttpLogger(String name, HttpServletResponse pResp) {
      this(null, (String)null);
      this.setLevel(Level.ALL);
      this.respDest = pResp;
      this.inited = false;
      this.bytesWritten = 0;
   }

   @Override
   public void log(LogRecord record) {
      String logName = "message";
      Level level = record.getLevel();
      if (level == Level.ALL || level == Level.FINE || level == Level.FINER || level == Level.FINEST) {
         logName = "trace";
      } else if (level == Level.INFO || level == Level.CONFIG) {
         logName = "message";
      } else if (level == Level.WARNING) {
         logName = "warning";
      } else if (level == Level.SEVERE) {
         logName = "error";
      }

      this.write(new MessageBundle(record.getMessage()), logName);
   }

   private void write(MessageBundle message, String logLevelName) {
      if (!this.errorInWrite) {
         this.init();
         ByteBuffer writer = new ByteBuffer();
         XWriter xml = new XWriter();
         xml.setOutputStream(writer.getOutputStream());
         xml.w("<" + logLevelName + ">\n");
         message.appendXML(xml);
         xml.w("</" + logLevelName + ">\n");
         xml.flush();
         xml.close();
         byte[] logBytes = writer.toByteArray();
         this.bytesWritten += logBytes.length;

         try {
            this.respDest.getOutputStream().write(logBytes);
         } catch (IOException ioe) {
            this.respDest.setStatus(500);
            NiagaraDaemon.getFilter().log(Level.SEVERE, "failed to write http logger response (" + ioe + ")", ioe);
            this.errorInWrite = true;
         }
      }
   }

   private void init() {
      if (this.respDest != null && !this.inited) {
         this.inited = true;
         ByteBuffer writer = new ByteBuffer();
         XWriter xml = new XWriter();
         xml.setOutputStream(writer.getOutputStream());
         xml.prolog();
         xml.w("<log>\n");
         xml.flush();
         xml.close();
         byte[] logBytes = writer.toByteArray();
         this.respDest.setHeader("Content-Type", "text/xml");
         this.bytesWritten += logBytes.length;

         try {
            this.respDest.getOutputStream().write(logBytes);
         } catch (IOException ioe) {
            this.respDest.setStatus(500);
            NiagaraDaemon.getFilter().log(Level.SEVERE, "failed to init http logger response (" + ioe + ")", ioe);
            this.errorInWrite = true;
         }
      }
   }

   public void finish() {
      if (!this.errorInWrite) {
         ByteBuffer writer = new ByteBuffer();
         XWriter xml = new XWriter();
         xml.setOutputStream(writer.getOutputStream());
         xml.w("</log>\n");
         xml.flush();
         xml.close();
         byte[] logBytes = writer.toByteArray();
         this.respDest.setStatus(200);
         this.bytesWritten += logBytes.length;
         this.respDest.setIntHeader("Content-Length", this.bytesWritten);

         try {
            this.respDest.getOutputStream().write(logBytes);
         } catch (IOException ioe) {
            if (NiagaraDaemon.getInstance().webServer != null && NiagaraDaemon.getInstance().webServer.getState() == 1) {
               this.respDest.setStatus(500);
               NiagaraDaemon.getFilter().log(Level.SEVERE, "failed to finish http logger response (" + ioe + ")", ioe);
            }
         }
      }
   }
}
