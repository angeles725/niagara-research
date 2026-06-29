package com.prosysopc.ua.stack.transport.tcp.io;

import com.prosysopc.ua.stack.builtintypes.ServiceRequest;
import com.prosysopc.ua.stack.builtintypes.ServiceResponse;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.EndpointConfiguration;
import com.prosysopc.ua.stack.core.EndpointDescription;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.transport.AsyncResult;
import com.prosysopc.ua.stack.transport.TransportChannelSettings;
import java.util.EnumSet;

public interface ITransportChannel {
   void dispose();

   EndpointConfiguration getEndpointConfiguration();

   EndpointDescription getEndpointDescription();

   EncoderContext getMessageContext();

   int getOperationTimeout();

   EnumSet<ITransportChannel.TransportChannelFeature> getSupportedFeatures();

   void initialize(String var1, TransportChannelSettings var2, EncoderContext var3) throws ServiceResultException;

   <T extends ServiceResponse> T serviceRequest(ServiceRequest<T> var1) throws ServiceResultException;

   <T extends ServiceResponse> T serviceRequest(ServiceRequest<T> var1, long var2) throws ServiceResultException;

   <T extends ServiceResponse> AsyncResult<T> serviceRequestAsync(ServiceRequest<T> var1);

   <T extends ServiceResponse> AsyncResult<T> serviceRequestAsync(ServiceRequest<T> var1, long var2);

   void setOperationTimeout(int var1);

   public static enum TransportChannelFeature {
      open,
      openAsync,
      reconnect,
      reconnectAsync,
      sendRequest,
      sendRequestAsync,
      close,
      closeAync;
   }
}
