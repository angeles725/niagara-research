package com.tridium.fox.session;

import com.tridium.fox.sys.UnreachableStationException;
import java.util.logging.Level;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.sys.LocalizableRuntimeException;

public class SessionDispatcher implements Runnable {
   private FoxSession session;
   private String name;
   private boolean isAlive;
   private Thread thread;
   private FrameQueue queue;

   public SessionDispatcher(FoxSession session) {
      this.name = "Fox:Dispatcher:" + SecurityUtil.calculateSessionIdHash(session.getId());
      this.session = session;
      this.queue = new FrameQueue();
      this.isAlive = true;
   }

   public void enqueue(FoxFrame frame) throws InterruptedException {
      if (this.session.isClosed()) {
         throw new InterruptedException("FoxSession is closed");
      } else {
         this.queue.enqueue(frame);
      }
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

   @Override
   public void run() {
      while (this.isAlive && !this.session.isClosed()) {
         try {
            FoxFrame request = this.queue.dequeue(-1);
            this.dispatch(request);
         } catch (InterruptedException var2) {
         } catch (Throwable var3) {
            var3.printStackTrace();
         }
      }
   }

   private void dispatch(FoxFrame frame) throws InterruptedException {
      try {
         FoxResponse resp;
         if (frame.channel == "fox") {
            resp = this.session.processFoxChannelRequest((FoxRequest)frame.message);
         } else {
            resp = this.session.conn().process((FoxRequest)frame.message);
         }

         this.session.sendReply(frame, resp);
      } catch (Throwable var3) {
         if (this.isAlive) {
            if (!(var3 instanceof IncompatibleVersionException)
               && !(var3 instanceof UnreachableStationException)
               && !(var3 instanceof InvalidChannelException)
               && (
                  !(var3 instanceof LocalizableRuntimeException)
                     || !"fox.channel.unsupportedRemoteVersion".equals(((LocalizableRuntimeException)var3).getLexiconKey())
                        && !"fox.channel.unsupportedRemoteVersionAlongRoute".equals(((LocalizableRuntimeException)var3).getLexiconKey())
               )) {
               var3.printStackTrace();
            } else {
               FoxServer.log.log(Level.FINE, var3.getMessage(), var3);
            }

            this.session.sendError(frame, var3);
         }
      }
   }

   @Override
   public String toString() {
      return this.name + " {" + this.queue + "}";
   }
}
