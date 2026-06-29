package com.tridium.opc.jni.client.common;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.ComObjectClient;

public abstract class OpcServer extends ComObjectClient {
   private OpcShutdown shutdown = null;
   private OpcServer.ShutdownListener shutdownListener = null;

   public synchronized OpcCommon getCommon() throws OpcException {
      return (OpcCommon)this.query(OpcCommon.IID);
   }

   public OpcServer.ShutdownListener getShutdownListener() {
      return this.shutdownListener;
   }

   @Override
   public void release() {
      if (this.shutdownListener != null) {
         this.setShutdownListener(null);
      }

      super.release();
   }

   public void setShutdownListener(OpcServer.ShutdownListener l) {
      if (l == null) {
         if (this.shutdown != null) {
            this.shutdown.unregisterCallback();
            this.shutdown = null;
         }
      } else {
         if (this.shutdown == null) {
            this.shutdown = new OpcShutdown(this, this.getPeer());
            this.shutdown.registerCallback();
         }

         this.shutdownListener = l;
      }
   }

   private void shutdownDeleted() {
      this.shutdown = null;
      if (this.shutdownListener != null) {
         this.shutdownListener.shutdownDeleted();
      }
   }

   private void shutdownRequest(String reason) {
      if (this.shutdownListener != null) {
         this.shutdownListener.shutdownRequest(reason);
      }
   }

   public interface ShutdownListener {
      void shutdownDeleted();

      void shutdownRequest(String var1);
   }
}
