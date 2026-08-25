package com.tridium.niagarad.io;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Logger;

public class PipedOutputBuffer extends OutputBuffer {
   private PipedInputStream hRead = null;
   private PipedOutputStream hWrite = null;
   private final String appName;
   private PipedOutputBuffer.PipedOutputBufferThread hThread = null;

   public PipedOutputBuffer(int bufferSize, Logger centralLog, String appName) {
      super(bufferSize, centralLog);
      this.appName = appName;
   }

   public void start() {
      if (this.hThread == null) {
         this.hRead = new PipedInputStream(8192);

         try {
            this.hWrite = new PipedOutputStream(this.hRead);
         } catch (IOException e) {
            return;
         }

         this.hThread = new PipedOutputBuffer.PipedOutputBufferThread();
         this.hThread.start();
      }
   }

   public void stop() {
      OutputBufferList.getInstance().removeBuffer(this);
      if (this.hThread != null) {
         if (this.hThread.isAlive()) {
            this.hThread.stopRequested = true;
            this.hThread.interrupt();

            try {
               this.hThread.join();
            } catch (InterruptedException var2) {
            }

            this.hThread.stopRequested = false;
         }
      }
   }

   public int doRun() {
      byte[] array = new byte[8192];

      while (!this.hThread.stopRequested && this.hRead != null) {
         try {
            int bytesRead;
            if ((bytesRead = this.hRead.read(array, 0, 8192)) != -1) {
               this.writeBuffer(array, bytesRead);
            } else {
               try {
                  this.hWrite.close();
               } catch (Exception var7) {
               }

               try {
                  this.hRead.close();
               } catch (Exception var6) {
               }

               this.hRead = new PipedInputStream(8192);

               try {
                  this.hWrite = new PipedOutputStream(this.hRead);
               } catch (IOException e) {
                  break;
               }
            }
         } catch (IOException e) {
            break;
         }
      }

      if (this.hWrite != null) {
         try {
            this.hWrite.close();
         } catch (Exception var5) {
         }
      }

      if (this.hRead != null) {
         try {
            this.hRead.close();
         } catch (Exception var4) {
         }
      }

      return 0;
   }

   public PipedOutputStream getWriteHandle() {
      return this.hWrite;
   }

   private class PipedOutputBufferThread extends Thread {
      boolean stopRequested = false;

      public PipedOutputBufferThread() {
         super("Niagarad:PipedOutputBuffer-" + PipedOutputBuffer.this.appName);
         this.setDaemon(true);
      }

      @Override
      public void run() {
         PipedOutputBuffer.this.doRun();
      }
   }
}
