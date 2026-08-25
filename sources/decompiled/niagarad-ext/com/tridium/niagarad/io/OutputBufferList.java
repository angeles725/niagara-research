package com.tridium.niagarad.io;

import java.util.LinkedList;

public class OutputBufferList {
   private LinkedList<OutputBuffer> bufferList = null;
   private OutputBufferList.OutputBufferListThread bufferListThread = null;
   private final Object listMonitor = new Object();

   private OutputBufferList() {
      this.bufferList = new LinkedList<>();
      this.bufferListThread = null;
   }

   public static OutputBufferList getInstance() {
      return OutputBufferList.InstanceHolder.INSTANCE;
   }

   public void addBuffer(OutputBuffer buffer) {
      synchronized (this.listMonitor) {
         this.bufferList.add(buffer);
      }
   }

   public void removeBuffer(OutputBuffer buffer) {
      synchronized (this.listMonitor) {
         this.bufferList.remove(buffer);
      }
   }

   public void checkBuffers() {
      synchronized (this.listMonitor) {
         this.bufferList.forEach(OutputBuffer::check);
      }
   }

   public void start() {
      if (this.bufferListThread == null) {
         this.bufferListThread = new OutputBufferList.OutputBufferListThread();
         this.bufferListThread.start();
      }
   }

   public void stop() {
      if (this.bufferListThread != null) {
         this.bufferListThread.stopRequested = true;
         this.bufferListThread.interrupt();

         try {
            if (this.bufferListThread != null) {
               this.bufferListThread.join();
            }
         } catch (InterruptedException var2) {
         }

         if (this.bufferListThread != null) {
            this.bufferListThread.stopRequested = false;
            this.bufferListThread = null;
         }
      }
   }

   public void run() {
      this.bufferListThread.stopRequested = false;

      while (true) {
         try {
            Thread.sleep(10000L);
         } catch (InterruptedException ie) {
            if (this.bufferListThread.stopRequested) {
               this.bufferListThread.stopRequested = false;
               break;
            }
         }

         this.checkBuffers();
         if (this.bufferListThread.stopRequested) {
            this.bufferListThread.stopRequested = false;
            break;
         }
      }
   }

   private static final class InstanceHolder {
      private static final OutputBufferList INSTANCE = new OutputBufferList();
   }

   private class OutputBufferListThread extends Thread {
      boolean stopRequested = false;

      public OutputBufferListThread() {
         super("Niagarad:OutputBufferList");
      }

      @Override
      public void run() {
         OutputBufferList.this.run();
      }
   }
}
