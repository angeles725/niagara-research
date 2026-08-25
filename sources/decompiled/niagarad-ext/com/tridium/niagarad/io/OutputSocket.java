package com.tridium.niagarad.io;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.app.App;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.util.KeyedList;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.TextUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class OutputSocket extends WebSocketAdapter {
   public static final int WEBSOCKET_ERROR_STATUS = 1011;
   public static final int WEBSOCKET_DISCONNECT_STATUS = 1001;
   public static final int WEBSOCKET_BAD_POLICY_STATUS = 1008;
   boolean isServer = false;
   OutputBuffer outputBuffer = null;
   boolean followed = false;
   BufferedOutputStream bpout = null;
   PipedInputStream pin = null;
   Logger log = null;
   AtomicBoolean closing = new AtomicBoolean(false);
   AtomicBoolean closed = new AtomicBoolean(false);

   public void setLogger(Logger log) {
      this.log = log;
   }

   public void setFollowed(boolean followed) {
      this.followed = followed;
   }

   public void onWebSocketConnect(Session session) {
      try {
         super.onWebSocketConnect(session);
         this.isServer = AccessController.doPrivileged(() -> Boolean.getBoolean("NiagaraDaemon"));
         if (this.isServer) {
            if (Http.isNiagaraClient(session.getUpgradeRequest()) && session.getUpgradeRequest().getHeader("UpgradeRequestToken") != null) {
               String target = Http.getServletName(session.getUpgradeRequest().getRequestURI().getPath());
               String queryString = session.getUpgradeRequest().getQueryString();
               KeyedList query = Http.getGetForm(queryString);
               boolean follow = true;
               boolean updatesOnly = false;
               if (queryString != null) {
                  follow = Boolean.valueOf(query.get("follow", "true"));
                  updatesOnly = Boolean.valueOf(query.get("updatesonly", "false"));
               }

               if (target.equals("getdaemonoutput")) {
                  this.outputBuffer = NiagaraDaemon.niagaraDaemonOutputBuffer;
                  this.log = this.outputBuffer.logger;
               } else {
                  if (!target.equals("station")) {
                     this.cleanup(true, 1011, "Unrecognized request");
                     return;
                  }

                  String appName = null;
                  if (queryString != null) {
                     appName = query.get(NiagaraDaemon.getInstance().getStationRegistry().getAppType(), "");
                  }

                  App app = NiagaraDaemon.getInstance().getStationRegistry().getApp(appName);
                  if (app == null) {
                     this.cleanup(true, 1011, "Requested application not found");
                     return;
                  }

                  this.outputBuffer = app.getAppOut();
                  this.log = this.outputBuffer.logger;
               }

               if (this.log != null && this.log.isLoggable(Level.FINE)) {
                  this.log
                     .fine(
                        "OutputSocket onWebSocketConnect server, local: "
                           + session.getLocalAddress()
                           + " remote: "
                           + session.getRemoteAddress()
                           + " follow: "
                           + follow
                           + " updatesOnly: "
                           + updatesOnly
                     );
                  if (this.log.isLoggable(Level.FINEST)) {
                     this.log.finest("OutputSocket session websocket server policy: " + session.getPolicy());
                  }
               }

               this.followed = follow;
               if (!this.outputBuffer.streamBufferContents(session, follow, updatesOnly)) {
                  this.cleanup(true, 1011, "Server failed to stream buffer");
               }
            } else {
               this.cleanup(true, 1008, "Unauthorized client");
            }
         } else {
            if (this.log != null && this.log.isLoggable(Level.FINE)) {
               this.log.fine("OutputSocket onWebSocketConnect client, local: " + session.getLocalAddress() + " remote: " + session.getRemoteAddress());
               if (this.log.isLoggable(Level.FINEST)) {
                  this.log.finest("OutputSocket session websocket client policy: " + session.getPolicy());
               }
            }

            try {
               PipedOutputStream pout = new PipedOutputStream();
               this.pin = new PipedInputStream(pout, 8192);
               this.bpout = new BufferedOutputStream(pout, 8192);
            } catch (IOException ioe) {
               if (this.log != null) {
                  this.log.severe("OutputSocket onWebSocketConnect, client error creating pipe: " + ioe);
               }

               this.cleanup(true, 1011, "Client failed to create streams");
            }
         }
      } catch (Throwable t) {
         if (this.log != null) {
            this.log.severe("OutputSocket onWebSocketConnect, encountered unhandled throwable: " + t);
            this.cleanup(true, 1011, "Unhandled throwable");
         }

         throw t;
      }
   }

   public void onWebSocketClose(int statusCode, String reason) {
      if (this.log != null && this.log.isLoggable(Level.FINE)) {
         Session session = this.getSession();
         if (session != null) {
            this.log
               .fine(
                  "OutputSocket onWebSocketClose, local: "
                     + session.getLocalAddress()
                     + " remote: "
                     + session.getRemoteAddress()
                     + " status code: "
                     + statusCode
                     + " reason: "
                     + reason
               );
         } else {
            this.log.fine("OutputSocket onWebSocketClose, status code: " + statusCode + ", reason: " + reason);
         }
      }

      this.cleanup(true, statusCode, reason);
      super.onWebSocketClose(statusCode, reason);
   }

   public void onWebSocketBinary(byte[] payload, int offset, int len) {
      if (!this.isServer && this.log != null && this.log.isLoggable(Level.FINEST)) {
         if (payload != null) {
            this.log.finest("OutputSocket onWebSocketBinary received payload size = " + payload.length + ", offset = " + offset + ", len = " + len);
         } else {
            this.log.finest("OutputSocket onWebSocketBinary received payload = null, offset = " + offset + ", len = " + len);
         }
      }

      super.onWebSocketBinary(payload, offset, len);
   }

   public void onWebSocketText(String message) {
      if (!this.isServer) {
         if (message != null && message.length() == 0) {
            if (this.log != null && this.log.isLoggable(Level.FINEST)) {
               this.log.finest("OutputSocket onWebSocketText keep-alive received (message length = 0)");
            }

            return;
         }

         if (this.log != null && this.log.isLoggable(Level.FINEST)) {
            if (message != null) {
               this.log
                  .finest(
                     "OutputSocket onWebSocketText received text '"
                        + TextUtil.truncate(message, 10)
                        + "...' (count = "
                        + message.length()
                        + "), streaming to internal buffer"
                  );
            } else {
               this.log.warning("OutputSocket onWebSocketText received null message, error will occur when streamed");
            }
         }

         if (this.bpout != null) {
            try {
               this.bpout.write(message.getBytes(StandardCharsets.UTF_8));
               this.bpout.flush();
            } catch (Exception e) {
               if (this.log != null) {
                  this.log.log(Level.SEVERE, "OutputSocket onWebSocketText, error streaming message", e);
               }

               this.cleanup(true, 1011, "Client failed to write message");
            }
         } else if (this.log != null) {
            if (message != null) {
               this.log
                  .warning(
                     "OutputSocket onWebSocketText received text '"
                        + TextUtil.truncate(message, 10)
                        + "...' while internal buffer was uninitialized, message ignored"
                  );
            } else {
               this.log.finest("OutputSocket onWebSocketText received null message while internal buffer was uninitialized, message ignored");
            }
         }
      }

      super.onWebSocketText(message);
   }

   public void onWebSocketError(Throwable cause) {
      if (!this.closed.get() && !this.closing.get()) {
         Level logLevel = Level.SEVERE;
         if (this.isServer && !this.followed) {
            logLevel = Level.FINE;
         }

         if (this.log != null && this.log.isLoggable(logLevel)) {
            Session session = this.getSession();
            if (session != null) {
               this.log
                  .log(
                     logLevel,
                     "OutputSocket onWebSocketError, local: " + session.getLocalAddress() + " remote: " + session.getRemoteAddress() + " (" + cause + ")"
                  );
            } else {
               this.log.log(logLevel, "OutputSocket onWebSocketError (" + cause + ")");
            }

            if (this.log.isLoggable(Level.FINEST)) {
               this.log.log(logLevel, "Stack trace: ", cause);
            }
         }

         this.cleanup(true, 1011, "Closed from onWebSocketError");
      }

      super.onWebSocketError(cause);
   }

   public InputStream getInputStream(long readTimeout, WebSocketClient client) {
      return new TimeoutInputStream(client, this.pin, readTimeout, this.log, !this.followed);
   }

   private void cleanup(boolean closeSession, int closeSessionReason, String closeSessionMessage) {
      if (!this.closed.get() && this.closing.compareAndSet(false, true)) {
         if (!this.isServer) {
            if (this.bpout != null && this.pin != null) {
               Thread cleanupThread = new Thread("OutputSocketSessionCleanup") {
                  @Override
                  public void run() {
                     if (OutputSocket.this.bpout != null) {
                        try {
                           OutputSocket.this.bpout.close();
                        } catch (Throwable var3) {
                        }
                     }

                     if (OutputSocket.this.pin != null) {
                        try {
                           OutputSocket.this.pin.close();
                        } catch (Throwable var2) {
                        }
                     }
                  }
               };
               cleanupThread.start();

               try {
                  cleanupThread.join(3000L);
               } catch (InterruptedException var7) {
               }
            }
         } else {
            Session session = this.getSession();
            if (this.followed && session != null) {
               try {
                  this.outputBuffer.teeSessions.removeSession(session, false, 0, null);
               } catch (Throwable var6) {
               }
            }
         }

         if (closeSession) {
            Session session = this.getSession();
            if (session != null && session.isOpen()) {
               session.close(closeSessionReason, closeSessionMessage);
            }
         }

         this.bpout = null;
         this.pin = null;
         this.outputBuffer = null;
         this.closed.set(true);
         this.closing.set(false);
      }
   }
}
