package com.prosysopc.ua.stack.transport.tcp.io;

import com.prosysopc.ua.stack.builtintypes.ServiceRequest;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.IEncodeable;
import com.prosysopc.ua.stack.transport.IConnectionListener;
import com.prosysopc.ua.stack.transport.TransportChannelSettings;
import java.net.InetSocketAddress;

public interface IConnection {
   void addConnectionListener(IConnectionListener var1);

   void addMessageListener(IConnection.IMessageListener var1);

   void close();

   void dispose();

   void initialize(InetSocketAddress var1, TransportChannelSettings var2, EncoderContext var3) throws ServiceResultException;

   void open() throws ServiceResultException;

   void reconnect() throws ServiceResultException;

   void removeConnectionListener(IConnectionListener var1);

   void removeMessageListener(IConnection.IMessageListener var1);

   void sendRequest(ServiceRequest var1, int var2, int var3) throws ServiceResultException;

   public interface IMessageListener {
      void onMessage(int var1, int var2, IEncodeable var3);
   }
}
