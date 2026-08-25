package com.tridium.nre.syslog;

import com.tridium.crypto.core.io.CryptoCoreClientSocketFactory;
import com.tridium.nre.security.Aes256PasswordEncoderUtil;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.baja.nre.security.ClientTlsParameters;

public class TcpSender extends SyslogSender {
   private final String serverHost;
   private final int serverPort;
   private final boolean useTLS;
   private final String clientAlias;
   private final SecretChars clientPassword;
   private Socket socket;
   private volatile OutputStream os;
   private boolean shutdown;

   protected TcpSender(SyslogManager syslogManager, BlockingQueue<Message> blockingQueue) {
      super(syslogManager, blockingQueue, "Syslog:TcpSender");
      this.serverHost = syslogManager.getServerHost();
      this.serverPort = syslogManager.getServerPort();
      this.useTLS = syslogManager.getUseTLS();
      this.clientAlias = syslogManager.getClientAlias();
      if (syslogManager.getClientPassword() == null) {
         this.clientPassword = null;
      } else {
         KeyRing keyRing = SecurityInitializer.getInstance().getSecurityInfoProvider().getKeyRing();
         SecretChars tempClientPassword = null;

         try {
            tempClientPassword = Aes256PasswordEncoderUtil.decodePassword(keyRing, "com.tridium.syslog.clientPassword", syslogManager.getClientPassword());
         } catch (Exception e) {
            SyslogManager.LOG.log(Level.WARNING, "Error decoding client password", e);
         }

         this.clientPassword = tempClientPassword;
      }
   }

   @Override
   public void run() {
      String msg = "";

      while (!this.shutdown) {
         try {
            if (this.os == null) {
               if (SyslogManager.LOG.isLoggable(Level.FINE)) {
                  SyslogManager.LOG.fine("Creating TCP socket to " + this.serverHost + ':' + this.serverPort);
               }

               if (this.useTLS) {
                  ClientTlsParameters params = new ClientTlsParameters("tlsv1", this.clientAlias);
                  if (this.clientPassword != null) {
                     try (SecretChars clientPasswordCopy = this.clientPassword.newCopy()) {
                        params.setKeyPassphrase(clientPasswordCopy.get());
                     }
                  }

                  CryptoCoreClientSocketFactory sf = new CryptoCoreClientSocketFactory(params);
                  this.socket = sf.createSocket(this.serverHost, this.serverPort);
               } else {
                  this.socket = new Socket(this.serverHost, this.serverPort);
               }

               msg = "TCP Server connected.";
               this.os = this.socket.getOutputStream();
               this.syslogManager.updateServerConnectionStatus(true, msg);
            }

            Message message = this.blockingQueue.peek();
            if (message != null) {
               SyslogManager.LOG.fine("Processing syslog message");
               this.os.write(message.getBytes(), 0, message.getLength());
               this.os.write(10);
               SyslogManager.LOG.fine("Syslog message sent");
               this.blockingQueue.poll(100L, TimeUnit.MILLISECONDS);
               this.syslogManager.updateServerConnectionStatus(true, msg);
            } else {
               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var19) {
               }
            }
         } catch (InterruptedException e) {
            msg = "TCP Server disconnected: " + e.getMessage();
            this.syslogManager.updateServerConnectionStatus(false, msg);
            this.releaseResources();
            return;
         } catch (Throwable e) {
            SyslogManager.LOG.log(Level.WARNING, "Unable to write log messages to TCP socket output stream: " + e.getMessage(), e);
            msg = "TCP Server disconnected: " + e.getMessage();
            this.syslogManager.updateServerConnectionStatus(false, msg);
            this.releaseResources();

            try {
               Thread.sleep(5000L);
            } catch (InterruptedException ie) {
               SyslogManager.LOG.warning("TCP sender thread interrupted, shutting down.");
               return;
            }
         }
      }

      SyslogManager.LOG.fine("Log sending complete");
      this.releaseResources();
   }

   private void releaseResources() {
      try {
         if (this.os != null) {
            this.os.flush();
         }
      } catch (Throwable var4) {
      }

      try {
         if (this.os != null) {
            this.os.close();
         }
      } catch (Throwable var3) {
      }

      this.os = null;

      try {
         if (this.socket != null) {
            this.socket.close();
         }
      } catch (Throwable var2) {
      }

      this.socket = null;
   }

   @Override
   public void shutdown() {
      SyslogManager.LOG.fine("Shutting down...");
      this.shutdown = true;
      this.interrupt();
   }
}
