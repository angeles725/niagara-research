package com.tridium.fox.session;

import com.tridium.authn.LoginFailureCause;
import com.tridium.fox.message.FoxMessage;
import java.io.IOException;
import javax.baja.nre.util.SecurityUtil;

public class SessionSender implements Runnable {
   private static FoxFrame keepAlive = new FoxFrame(107, -1, -1, "fox", "keepalive", new FoxMessage());
   private FoxSession session;
   private String name;
   private boolean isAlive;
   private Thread thread;
   private FrameQueue queue;
   private volatile boolean running = false;

   public SessionSender(FoxSession session) {
      this.name = "Fox:Sender:" + SecurityUtil.calculateSessionIdHash(session.getId());
      this.session = session;
      this.queue = new FrameQueue();
      this.isAlive = true;
   }

   public void enqueue(FoxFrame frame) throws InterruptedException {
      this.queue.enqueue(frame);
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

      this.queue.kill();
   }

   public void sendClose(LoginFailureCause cause) {
      if (cause != null) {
         FoxRequest req = new FoxRequest("fox", "close");
         req.add("cause", cause.name());
         FoxFrame close = new FoxFrame(99, -1, -1, "fox", "close", req);

         try {
            if (this.session.isClosed()) {
               return;
            }

            this.session.writeFrame(close);
         } catch (IOException var5) {
         }
      }
   }

   public boolean isRunning() {
      return this.running;
   }

   @Override
   public void run() {
      try {
         this.running = true;

         while (this.isAlive && !this.session.isClosed()) {
            try {
               FoxFrame frame = this.queue.dequeue(Fox.keepAliveInterval);
               if (this.session.isClosed()) {
                  break;
               }

               if (frame == null) {
                  frame = keepAlive;
               }

               this.session.writeFrame(frame);
            } catch (InterruptedException var15) {
            } catch (IOException var16) {
               this.session.close(var16);
            } catch (Throwable var17) {
               var17.printStackTrace();
            }
         }
      } finally {
         this.running = false;
         Object sessionStateLock = this.session.getSessionStateLock();
         synchronized (sessionStateLock) {
            sessionStateLock.notifyAll();
         }
      }
   }

   @Override
   public String toString() {
      return this.name + " {" + this.queue + "}";
   }
}
