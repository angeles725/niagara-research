package com.tridium.opc.jni.client.da;

import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;

public class OpcNTSecurity extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{7AA83A01-6C77-11d3-84F9-00008630A38B}", OpcNTSecurity.class);

   public boolean isAvailableNT() {
      return this.isAvailableNT(this.getPeer());
   }

   public long queryImpersonationLevel() {
      return this.queryImpersonationLevel(this.getPeer());
   }

   public long changeUser(String username, String password) {
      return this.changeUser(this.getPeer(), username, password);
   }

   private native boolean isAvailableNT(long var1);

   private native long queryImpersonationLevel(long var1);

   private native long changeUser(long var1, String var3, String var4);
}
