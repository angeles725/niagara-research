package com.prosysopc.ua.stack.transport.tcp.io;

public class TcpQuotas {
   public static final TcpQuotas DEFAULT_CLIENT_QUOTA = new TcpQuotas(Integer.MAX_VALUE, 147456, 60000, 3600000);
   public static final TcpQuotas DEFAULT_SERVER_QUOTA = new TcpQuotas(Integer.MAX_VALUE, 147456, 60000, 3600000);
   public final int maxMessageSize;
   public final int maxBufferSize;
   public final int channelLifetime;
   public final int securityTokenLifetime;

   public TcpQuotas(int var1, int var2, int var3, int var4) {
      if (var2 < 8192) {
         throw new IllegalArgumentException();
      } else {
         this.maxMessageSize = var1;
         this.maxBufferSize = var2;
         this.channelLifetime = var3;
         this.securityTokenLifetime = var4;
      }
   }
}
