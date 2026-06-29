package com.tridium.opc.jni;

import com.tridium.opc.OpcEnv;
import com.tridium.opc.OpcException;

public class ComObjectClient {
   protected long peer = 0L;

   public ComObjectClient query(OpcInterface ifc) throws OpcException {
      ComObjectClient o = null;

      try {
         o = ifc.newInstance();
      } catch (Exception var4) {
         var4.printStackTrace();
         throw new OpcException(var4.toString());
      }

      o.peer = this.query(this.peer, ifc.iid);
      if (o.peer == 0L) {
         throw new OpcException("Query interface failed: " + ifc);
      } else {
         return o;
      }
   }

   public void release() throws OpcException {
      if (this.peer != 0L) {
         this.release(this.peer);
      }

      this.peer = 0L;
   }

   protected void doFinalize() throws Throwable {
   }

   @Override
   protected final void finalize() throws Throwable {
      Throwable t = null;

      try {
         OpcEnv.initializeThread();
         this.release();
      } catch (Exception var3) {
         t = var3;
         System.out.println("Failure releasing peer: " + this.peer);
         var3.printStackTrace();
      }

      this.doFinalize();
      if (t != null) {
         throw t;
      }
   }

   protected long getPeer() {
      return this.peer;
   }

   private native void release(long var1);

   private native long query(long var1, String var3);
}
