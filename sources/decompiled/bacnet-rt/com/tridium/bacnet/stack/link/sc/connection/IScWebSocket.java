package com.tridium.bacnet.stack.link.sc.connection;

public interface IScWebSocket {
   void sendBytes(byte[] var1) throws Exception;

   void close(int var1, String var2);

   void setIdleTimeout(long var1);

   String getRemoteHost();

   int getRemotePort();
}
