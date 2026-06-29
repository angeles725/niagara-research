package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.encoding.IEncodeable;

public final class Acknowledge extends AbstractUaTcpCommMessage implements IEncodeable {
   private UnsignedInteger xF;
   private UnsignedInteger xG;
   private UnsignedInteger xH;
   private UnsignedInteger xI;
   private UnsignedInteger xJ;

   public Acknowledge() {
   }

   public Acknowledge(UnsignedInteger var1, UnsignedInteger var2, UnsignedInteger var3, UnsignedInteger var4, UnsignedInteger var5) {
      this.xJ = var5;
      this.xI = var4;
      this.xF = var1;
      this.xG = var2;
      this.xH = var3;
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
      return "Acknowledge [protocolVersion="
         + this.xF
         + ", receiveBufferSize="
         + this.xG
         + ", sendBufferSize="
         + this.xH
         + ", maxMessageSize="
         + this.xI
         + ", maxChunkCount="
         + this.xJ
         + "]";
   }
}
