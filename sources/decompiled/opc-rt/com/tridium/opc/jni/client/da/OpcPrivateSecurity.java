package com.tridium.opc.jni.client.da;

import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;

public class OpcPrivateSecurity extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{7AA83A02-6C77-11d3-84F9-00008630A38B}", OpcPrivateSecurity.class);

   public boolean isAvailablePrivate() {
      return this.isAvailablePrivate(this.getPeer());
   }

   public long logOn(String name, String pwd) {
      return this.logon(this.getPeer(), name, pwd);
   }

   public boolean logOff() {
      return this.logoff(this.getPeer());
   }

   private native boolean isAvailablePrivate(long var1);

   private native long logon(long var1, String var3, String var4);

   private native boolean logoff(long var1);
}
