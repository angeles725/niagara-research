package com.tridium.opc.client.util;

import com.tridium.opc.OpcEnv;
import javax.baja.log.Log;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BThreadPoolWorker;
import javax.baja.util.CoalesceQueue;
import javax.baja.util.ThreadPoolWorker;
import javax.baja.util.Worker;

@NiagaraType
public class BOpcThreadPool extends BThreadPoolWorker {
   public static final Type TYPE = Sys.loadType(BOpcThreadPool.class);
   private ThreadPoolWorker worker;
   private CoalesceQueue queue;
   Log opcWriteLog = Log.getLog("OpcDaWriteLog");
   Log opcLog = Log.getLog("OpcDaLog");

   public Type getType() {
      return TYPE;
   }

   public Worker getWorker() {
      if (this.worker == null) {
         this.queue = new CoalesceQueue();
         this.worker = new BOpcThreadPool.MyWorker();
      }

      return this.worker;
   }

   public boolean post(Runnable request) {
      if (this.queue == null) {
         this.opcWriteLog.trace("BOpcThreadPool::post Queue is null so returning without queuing the request: " + request.toString());
         throw new IllegalStateException("Null queue, please report this error.");
      } else if (!this.isRunning()) {
         return false;
      } else {
         boolean retVal = this.queue.enqueue(request);
         if (!retVal) {
            System.out.println("BOpcThreadPool::post - Enqueue operation returned false. This means it was coalesced.");
         }

         return retVal;
      }
   }

   public void started() throws Exception {
      super.started();
      this.getWorker().start("Opc Worker");
   }

   public void stopped() throws Exception {
      super.stopped();
      if (this.worker != null) {
         this.worker.stop();
      }
   }

   protected String getWorkerThreadName() {
      return "Opc Worker";
   }

   protected void threadStarted() {
      OpcEnv.initializeThread();
   }

   private class MyWorker extends ThreadPoolWorker {
      MyWorker() {
         super(BOpcThreadPool.this.queue);
      }

      protected void threadStarted() {
         BOpcThreadPool.this.threadStarted();
      }

      protected void threadStopped() {
      }
   }
}
