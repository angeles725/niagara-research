package com.tridium.opc.jni.client.da;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;

public class OpcSyncIo extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{39c13a52-011e-11d0-9675-0020afd8adb3}", OpcSyncIo.class);
   OpcGroup group = null;

   public void read(int[] serverHandles, boolean cache) throws OpcException {
      this.read(this.getPeer(), serverHandles, cache, this.group);
   }

   public int writeArray(int Count, int[] serverHandles, int[] dataTypes, String[] values) {
      return this.writeArray(this.getPeer(), Count, serverHandles, dataTypes, values, this.group);
   }

   public int writeBoolean(int serverHandle, boolean val) throws OpcException {
      return this.writeBoolean(this.getPeer(), serverHandle, val);
   }

   public int writeNumeric(int serverHandle, int dataType, double val) throws OpcException {
      return this.writeNumeric(this.getPeer(), serverHandle, dataType, val);
   }

   public int writeString(int serverHandle, String val, int opcDataType) throws OpcException {
      return this.writeString(this.getPeer(), serverHandle, val, opcDataType);
   }

   private native void read(long var1, int[] var3, boolean var4, OpcGroup var5);

   private native int writeArray(long var1, int var3, int[] var4, int[] var5, String[] var6, OpcGroup var7);

   private native int writeBoolean(long var1, int var3, boolean var4);

   private native int writeNumeric(long var1, int var3, int var4, double var5);

   private native int writeString(long var1, int var3, String var4, int var5);
}
