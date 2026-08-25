package com.tridium.niagarad.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;

public class SessionList {
   private boolean enabled;
   public static final int _SESSION_KEEP_ALIVE_MS = 10000;
   public static final int _UNFOLLOWED_SESSION_EXPIRATION_MS = 5000;
   private volatile boolean ignoreMessages;
   private final LinkedList<Session> sessionList;
   private final Object listMonitor = new Object();
   private final Logger centralLog;
   public static final ByteBuffer PING_BUFFER = ByteBuffer.wrap("ping!".getBytes(StandardCharsets.UTF_8));
   public static final ByteBuffer PONG_BUFFER = ByteBuffer.wrap("pong!".getBytes(StandardCharsets.UTF_8));

   protected SessionList(Logger pCentralLog) {
      this.enabled = true;
      this.sessionList = new LinkedList<>();
      this.centralLog = pCentralLog;
   }

   protected void addSession(Session out) {
      synchronized (this.listMonitor) {
         this.sessionList.add(out);
      }
   }

   protected Session removeSession(Session item, boolean closeOnRemove, int closeStatus, String closeMessage) {
      synchronized (this.listMonitor) {
         int index = this.sessionList.indexOf(item);
         if (index != -1) {
            if (this.sessionList.remove(item) && closeOnRemove && item.isOpen()) {
               item.close();
            }

            Session var10000;
            try {
               var10000 = this.sessionList.get(index);
            } catch (IndexOutOfBoundsException ioobe) {
               return null;
            }

            return var10000;
         } else {
            return null;
         }
      }
   }

   protected void sendAll(String message, boolean async) {
      if (this.enabled) {
         if (!this.ignoreMessages) {
            synchronized (this.listMonitor) {
               try {
                  this.ignoreMessages = true;
                  int index = 0;

                  while (index < this.sessionList.size()) {
                     Session current = this.sessionList.get(index);

                     try {
                        if (message.length() == 0) {
                           current.getRemote().sendPing(PING_BUFFER);
                        } else if (async) {
                           current.getRemote().sendStringByFuture(message);
                        } else {
                           current.getRemote().sendString(message);
                        }
                     } catch (IOException | WebSocketException ex) {
                        int closeStatus = 1001;
                        String closeStatusMessage = "Disconnected";
                        if (message.length() != 0) {
                           closeStatus = 1011;
                           closeStatusMessage = "Server failed to send message to client";
                           if (this.centralLog.isLoggable(Level.FINE)) {
                              this.centralLog.fine("SessionList: unable to send message of length " + message.length() + " (" + ex + "), removing from list");
                           }
                        }

                        this.removeSession(current, true, closeStatus, closeStatusMessage);
                        continue;
                     }

                     index++;
                  }
               } finally {
                  this.ignoreMessages = false;
               }
            }
         }
      }
   }

   protected void check() {
      this.sendAll("", false);
   }

   protected void setEnabled(boolean value) {
      this.enabled = value;
   }
}
