package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.utils.StackUtils;

public class TcpConnectionParameters implements Cloneable {
   public static int defaultMaxSendMessageSize = 0;
   public static int defaultMaxSendChunkSize = StackUtils.cores() > 1 ? 8196 : 65536;
   public static int defaultMaxSendChunkCount = 0;
   public static int defaultMaxRecvMessageSize = 0;
   public static int defaultMaxRecvChunkSize = StackUtils.cores() > 1 ? 8196 : 65536;
   public static int defaultMaxRecvChunkCount = 0;
   public int maxSendMessageSize = defaultMaxSendMessageSize;
   public int maxSendChunkSize = defaultMaxSendChunkSize;
   public int maxSendChunkCount = defaultMaxSendChunkCount;
   public int maxRecvMessageSize = defaultMaxRecvMessageSize;
   public int maxRecvChunkSize = defaultMaxRecvChunkSize;
   public int maxRecvChunkCount = defaultMaxRecvChunkCount;
   public String endpointUrl;

   public TcpConnectionParameters clone() {
      try {
         return (TcpConnectionParameters)super.clone();
      } catch (CloneNotSupportedException var2) {
         throw new RuntimeException(var2);
      }
   }
}
