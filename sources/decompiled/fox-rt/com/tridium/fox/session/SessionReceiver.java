package com.tridium.fox.session;

import javax.baja.nre.util.SecurityUtil;

public class SessionReceiver implements Runnable {
   FoxSession session;
   String name;
   boolean isAlive;
   Thread thread;

   public SessionReceiver(FoxSession session) {
      this.session = session;
      this.name = "Fox:Receiver:" + SecurityUtil.calculateSessionIdHash(session.getId());
      this.isAlive = true;
   }

   public void start() {
      this.thread = this.session.conn.makeThread(Fox.threadGroup, this, this.name);
      this.thread.start();
   }

   public void kill() {
      this.isAlive = false;
      if (this.thread != null) {
         this.thread.interrupt();
         this.thread = null;
      }
   }

   @Override
   public void run() {
      while (this.isAlive && !this.session.isClosed()) {
         try {
            FoxFrame frame = this.session.readFrame();
            if (!this.session.isClosed()) {
               if (frame.frameType != 115 && frame.frameType != 97 && frame.frameType != 99) {
                  this.session.replyReceived(frame);
                  continue;
               }

               this.session.requestReceived(frame);
               continue;
            }
            break;
         } catch (Throwable var2) {
            if (this.isAlive) {
               this.session.close(var2);
            }

            return;
         }
      }
   }

   @Override
   public String toString() {
      return this.name;
   }
}
