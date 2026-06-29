package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.encoding.IEncodeable;

public final class Hello extends AbstractUaTcpCommMessage implements IEncodeable {
   private UnsignedInteger xF;
   private UnsignedInteger xG;
   private UnsignedInteger xH;
   private UnsignedInteger xI;
   private UnsignedInteger xJ;
   private String endpointUrl;

   public Hello() {
   }

   public Hello(UnsignedInteger var1, UnsignedInteger var2, UnsignedInteger var3, UnsignedInteger var4, UnsignedInteger var5, String var6) {
      this.endpointUrl = var6;
      this.xJ = var5;
      this.xI = var4;
      this.xF = var1;
      this.xG = var2;
      this.xH = var3;
   }

   public String getEndpointUrl() {
      return this.endpointUrl;
   }

   public UnsignedInteger getMaxChunkCount() {
      return this.xJ;
   }

   public UnsignedInteger getMaxMessageSize() {
      return this.xI;
   }

   public UnsignedInteger getProtocolVersion() {
      return this.xF;
   }

   public UnsignedInteger getReceiveBufferSize() {
      return this.xG;
   }

   public UnsignedInteger getSendBufferSize() {
      return this.xH;
   }

   public void setEndpointUrl(String var1) {
      this.endpointUrl = var1;
   }

   public void setMaxChunkCount(UnsignedInteger var1) {
      this.xJ = var1;
   }

   public void setMaxMessageSize(UnsignedInteger var1) {
      this.xI = var1;
   }

   public void setProtocolVersion(UnsignedInteger var1) {
      this.xF = var1;
   }

   public void setReceiveBufferSize(UnsignedInteger var1) {
      this.xG = var1;
   }

   public void setSendBufferSize(UnsignedInteger var1) {
      this.xH = var1;
   }

   @Override
   public String toString() {
      return "Hello [protocolVersion="
         + this.xF
         + ", receiveBufferSize="
         + this.xG
         + ", sendBufferSize="
         + this.xH
         + ", maxMessageSize="
         + this.xI
         + ", maxChunkCount="
         + this.xJ
         + ", endpointUrl="
         + this.endpointUrl
         + "]";
   }
}
