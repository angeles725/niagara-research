package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.encoding.IEncodeable;

public final class ReverseHello extends AbstractUaTcpCommMessage implements IEncodeable {
   private String xZ;
   private String endpointUrl;

   public ReverseHello() {
   }

   public ReverseHello(String var1, String var2) {
      this.xZ = var1;
      this.endpointUrl = var2;
   }

   public String getEndpointUrl() {
      return this.endpointUrl;
   }

   public String getServerUri() {
      return this.xZ;
   }

   public void setEndpointUrl(String var1) {
      this.endpointUrl = var1;
   }

   public void setServerUri(String var1) {
      this.xZ = var1;
   }

   @Override
   public String toString() {
      return "ReverseHello [ServerUri=" + this.xZ + ", EndpointUrl=" + this.endpointUrl + "]";
   }
}
