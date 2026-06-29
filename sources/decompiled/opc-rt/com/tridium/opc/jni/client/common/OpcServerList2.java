package com.tridium.opc.jni.client.common;

import com.tridium.opc.OpcException;

public class OpcServerList2 {
   private long peer = 0L;

   public Object[] discoverServersInNetwork(String host, String catid) throws OpcException {
      return this.discoverServers(host, catid);
   }

   private native Object[] discoverServers(String var1, String var2);
}
