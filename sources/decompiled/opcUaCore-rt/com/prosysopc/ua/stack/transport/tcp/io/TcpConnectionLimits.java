package com.prosysopc.ua.stack.transport.tcp.io;

public class TcpConnectionLimits implements Cloneable {
   public int maxSendMessageSize;
   public int maxSendBufferSize;
   public int maxSendChunkCount;
   public int maxRecvMessageSize;
   public int maxRecvBufferSize;
   public int maxRecvChunkCount;

   public TcpConnectionLimits clone() {
      try {
         return (TcpConnectionLimits)super.clone();
      } catch (CloneNotSupportedException var2) {
         throw new RuntimeException(var2);
      }
   }
}
