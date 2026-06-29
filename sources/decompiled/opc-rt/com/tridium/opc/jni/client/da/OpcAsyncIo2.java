package com.tridium.opc.jni.client.da;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;

public class OpcAsyncIo2 extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{39c13a71-011e-11d0-9675-0020afd8adb3}", OpcAsyncIo2.class);
   OpcGroup group = null;

   public void refresh(boolean cache) {
      this.refresh(this.getPeer(), cache);
   }

   public synchronized long registerCallback(OpcGroup group) throws OpcException {
      return this.advise(this.getPeer(), group);
   }

   public synchronized void unregisterCallback(long cookie) throws OpcException {
      this.unadvise(this.getPeer(), cookie);
   }

   public int writeAsync(int Count, int TransactionID, int[] serverHandles, int[] dataTypes, String[] values) throws OpcException {
      return this.writeAsync(this.getPeer(), Count, TransactionID, serverHandles, dataTypes, values, this.group);
   }

   public void readAsync(int Count, int TransactionID, int[] serverHandles) throws OpcException {
      this.readAsync(this.getPeer(), Count, TransactionID, serverHandles, this.group);
   }

   public int cancel2(int[] readCancelIds, int[] writeCancelIds) throws OpcException {
      return this.cancel2(this.getPeer(), readCancelIds, writeCancelIds);
   }

   private native long advise(long var1, OpcGroup var3);

   private native void refresh(long var1, boolean var3);

   private native void unadvise(long var1, long var3);

   private native int writeAsync(long var1, int var3, int var4, int[] var5, int[] var6, String[] var7, OpcGroup var8);

   private native void readAsync(long var1, int var3, int var4, int[] var5, OpcGroup var6);

   private native int cancel2(long var1, int[] var3, int[] var4);
}
