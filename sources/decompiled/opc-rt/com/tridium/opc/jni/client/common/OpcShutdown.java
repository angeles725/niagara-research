package com.tridium.opc.jni.client.common;

import com.tridium.opc.OpcException;

class OpcShutdown {
   private OpcServer callback;
   private long cookie;
   private long server;

   public OpcShutdown(OpcServer callback, long server) {
      this.callback = callback;
      this.server = server;
   }

   public synchronized void registerCallback() throws OpcException {
      this.cookie = this.advise(this.server, this.callback);
   }

   public synchronized void unregisterCallback() throws OpcException {
      this.unadvise(this.server, this.cookie);
   }

   private native long advise(long var1, OpcServer var3);

   private native void unadvise(long var1, long var3);
}
