package com.prosysopc.ua.stack.transport.tcp.io;

import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.ServiceRequest;
import com.prosysopc.ua.stack.builtintypes.ServiceResponse;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.common.ServiceFaultException;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.ChannelSecurityToken;
import com.prosysopc.ua.stack.core.CloseSecureChannelRequest;
import com.prosysopc.ua.stack.core.EndpointConfiguration;
import com.prosysopc.ua.stack.core.EndpointDescription;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.core.OpenSecureChannelRequest;
import com.prosysopc.ua.stack.core.OpenSecureChannelResponse;
import com.prosysopc.ua.stack.core.ResponseHeader;
import com.prosysopc.ua.stack.core.SecurityTokenRequestType;
import com.prosysopc.ua.stack.core.ServiceFault;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.IEncodeable;
import com.prosysopc.ua.stack.transport.AsyncResult;
import com.prosysopc.ua.stack.transport.IConnectionListener;
import com.prosysopc.ua.stack.transport.SecureChannel;
import com.prosysopc.ua.stack.transport.TransportChannelSettings;
import com.prosysopc.ua.stack.transport.UriUtil;
import com.prosysopc.ua.stack.transport.impl.AsyncResultImpl;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.transport.tcp.impl.InternalClientSideDecodingServiceFault;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import com.prosysopc.ua.stack.utils.StackUtils;
import com.prosysopc.ua.stack.utils.TimerUtil;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureChannelTcp implements IConnectionListener, SecureChannel, IConnection.IMessageListener, ITransportChannel {
   static Logger logger = LoggerFactory.getLogger(SecureChannelTcp.class);
   private static final int[] yr = new int[]{0, 1, 2, 4, 8, 16, 32, 64, 120, 120, 120};
   private static final double oJ = 0.1;
   @Deprecated
   public static boolean disableReconnectLogic = false;
   private EncoderContext ti;
   Executor executor = StackUtils.getBlockingWorkExecutor();
   int uX = -1;
   long ys;
   long yt;
   TransportChannelSettings yu;
   InetSocketAddress addr;
   AtomicInteger vb = new AtomicInteger(0);
   AtomicReference<IConnection> yv = new AtomicReference<>(null);
   Map<Integer, SecureChannelTcp.a> vk = new ConcurrentHashMap<>();
   int yw = 0;
   TimerTask yx;
   boolean yy = false;
   Object yz = new Object();
   TimerTask yA;
   AtomicReference<TimerTask> vm = new AtomicReference<>(null);
   Timer vl;
   Runnable vq = new Runnable() {
      @Override
      public void run() {
         SecureChannelTcp.this.etn();
         long var1 = System.currentTimeMillis();

         for (SecureChannelTcp.a var4 : SecureChannelTcp.this.vk.values()) {
            if (var1 >= var4.vu) {
               SecureChannelTcp.logger
                  .warn(
                     "Request id={} timeouted {}ms elapsed. timeout at {}ms",
                     new Object[]{var4.requestId, System.currentTimeMillis() - var4.vt, var4.vu - var4.vt}
                  );
               var4.vv.setError(new ServiceResultException(StatusCodes.Bad_Timeout));
               SecureChannelTcp.this.vk.remove(var4.requestId);
            }
         }

         SecureChannelTcp.this.eto();
      }
   };
   private Runnable yB = new Runnable() {
      @Override
      public void run() {
         synchronized (SecureChannelTcp.this.yB) {
            try {
               SecureChannelTcp.this.etv();
            } catch (ServiceResultException var4) {
               SecureChannelTcp.logger.trace("failure while sendPendingRequestMessages", var4);
            }
         }
      }
   };
   Runnable yC = new Runnable() {
      @Override
      public void run() {
         synchronized (SecureChannelTcp.this.yz) {
            if (!SecureChannelTcp.this.yy) {
               return;
            }
         }

         if (!SecureChannelTcp.this.isOpen()) {
            SecureChannelTcp.this.d(false);
            SecureChannelTcp.logger.info("{}: Error recovery failed, security token has expired", SecureChannelTcp.this.uX);
            SecureChannelTcp.this.close();
         } else {
            try {
               SecureChannelTcp.logger.debug("{}: Error recovery reconnect", SecureChannelTcp.this.uX);
               if (SecureChannelTcp.this.etw() == null) {
                  throw new ServiceResultException(StatusCodes.Bad_SecureChannelClosed);
               }

               SecureChannelTcp.this.etw().open();
               SecureChannelTcp.this.c(true);
               SecureChannelTcp.this.d(false);
               SecureChannelTcp.this.executor.execute(SecureChannelTcp.this.yB);
            } catch (ServiceResultException var11) {
               if (var11.getStatusCode().isStatusCode(StatusCodes.Bad_TcpSecureChannelUnknown)
                  || var11.getStatusCode().isStatusCode(StatusCodes.Bad_SecureChannelTokenUnknown)
                  || var11.getStatusCode().isStatusCode(StatusCodes.Bad_NotFound)) {
                  SecureChannelTcp.logger.info(SecureChannelTcp.this.uX + ": The secure channel has been closed by the server", var11);
                  SecureChannelTcp.this.close();
                  return;
               }

               synchronized (SecureChannelTcp.this.yz) {
                  SecureChannelTcp.this.yw++;
                  long var3 = System.currentTimeMillis();
                  long var5 = SecureChannelTcp.this.yw >= SecureChannelTcp.yr.length
                     ? SecureChannelTcp.yr[SecureChannelTcp.yr.length - 1] * 1000
                     : SecureChannelTcp.yr[SecureChannelTcp.this.yw] * 1000;
                  long var7 = (long)(SecureChannelTcp.this.yt * 1.25) + SecureChannelTcp.this.ys;
                  if (var3 + var5 > var7) {
                     SecureChannelTcp.logger.info("{}: Error recovery failed, security token has expired", SecureChannelTcp.this.uX);
                     SecureChannelTcp.this.close();
                     return;
                  }

                  if (!SecureChannelTcp.disableReconnectLogic) {
                     SecureChannelTcp.this.yx = TimerUtil.schedule(
                        SecureChannelTcp.this.vl, SecureChannelTcp.this.yC, SecureChannelTcp.this.executor, var3 + var5
                     );
                  }
               }
            }
         }
      }
   };
   private Runnable yD = new Runnable() {
      @Override
      public void run() {
         try {
            SecureChannelTcp.logger.debug("{} Renewing security token", SecureChannelTcp.this.uX);
            SecureChannelTcp.this.c(true);
         } catch (ServiceResultException var2) {
            SecureChannelTcp.logger.error(SecureChannelTcp.this.uX + " Failed to renew security token. ", var2);
         }
      }
   };

   public void close() {
      this.d(false);
      TimerTask var1 = this.yA;
      this.yA = null;
      if (var1 != null) {
         var1.cancel();
      }

      IConnection var7 = this.etw();
      if (var7 != null) {
         CloseSecureChannelRequest var2 = new CloseSecureChannelRequest();

         try {
            this.serviceRequest(var2);
         } catch (ServiceResultException var6) {
         }

         if (this.uX != -1) {
            logger.info("{} Closed", this.uX);
         }

         this.uX = -1;
         var7.close();
         var7.removeMessageListener(this);
         var7.removeConnectionListener(this);
         var7.dispose();
         this.setTransportChannel(null);
      }

      this.etn();
      ArrayList var8 = new ArrayList<>(this.vk.values());
      logger.debug("requests.clear()");
      this.vk.clear();
      if (!var8.isEmpty()) {
         ServiceResultException var3 = new ServiceResultException(StatusCodes.Bad_SecureChannelClosed);

         for (SecureChannelTcp.a var5 : var8) {
            var5.vv.setError(var3);
         }
      }
   }

   public AsyncResult<SecureChannel> closeAsync() {
      final AsyncResultImpl var1 = new AsyncResultImpl();
      this.executor.execute(new Runnable() {
         @Override
         public void run() {
            try {
               SecureChannelTcp.this.close();
            } finally {
               var1.setResult(SecureChannelTcp.this);
            }
         }
      });
      return var1;
   }

   @Override
   public void dispose() {
      this.close();
      this.yv = null;
      this.yu = null;
      this.addr = null;
      this.vk = null;
      this.vl = null;
   }

   public String getConnectURL() {
      return this.getEndpointDescription().getEndpointUrl();
   }

   @Override
   public EndpointConfiguration getEndpointConfiguration() {
      return this.yu == null ? null : this.yu.getConfiguration();
   }

   @Override
   public EndpointDescription getEndpointDescription() {
      return this.yu == null ? null : this.yu.getDescription();
   }

   @Override
   public EncoderContext getMessageContext() {
      return this.ti;
   }

   public MessageSecurityMode getMessageSecurityMode() {
      return this.getEndpointDescription().getSecurityMode();
   }

   @Override
   public int getOperationTimeout() {
      Integer var1 = this.yu.getConfiguration().getOperationTimeout();
      return var1 == null ? 0 : var1;
   }

   public int getSecureChannelId() {
      return this.uX;
   }

   public SecurityPolicy getSecurityPolicy() {
      try {
         return SecurityPolicy.getSecurityPolicy(this.getEndpointDescription().getSecurityPolicyUri());
      } catch (ServiceResultException var2) {
         return null;
      }
   }

   @Override
   public EnumSet<ITransportChannel.TransportChannelFeature> getSupportedFeatures() {
      return EnumSet.of(
         ITransportChannel.TransportChannelFeature.open,
         ITransportChannel.TransportChannelFeature.openAsync,
         ITransportChannel.TransportChannelFeature.close,
         ITransportChannel.TransportChannelFeature.closeAync,
         ITransportChannel.TransportChannelFeature.sendRequest,
         ITransportChannel.TransportChannelFeature.sendRequestAsync
      );
   }

   public void initialize(InetSocketAddress var1, TransportChannelSettings var2, EncoderContext var3) throws ServiceResultException {
      if (this.uX != -1) {
         throw new ServiceResultException(StatusCodes.Bad_InternalError, "Cannot reconfigure already opened secure channel");
      } else {
         this.yu = var2.clone();
         this.addr = var1;
         this.ti = var3;
         this.yx = null;
         this.yw = 0;
         this.vl = TimerUtil.getTimer();
         String var4 = var2.getDescription().getEndpointUrl();
         if (var4 != null && !var4.isEmpty() && !"opc.tcp".equals(UriUtil.getTransportProtocol(var4))) {
            throw new ServiceResultException(StatusCodes.Bad_ServerUriInvalid, "The protocol is not supported by the this SecureChannelTcp");
         } else {
            this.setTransportChannel(new TcpConnection());
            this.etw().initialize(var1, var2, var3);
            this.etw().addConnectionListener(this);
            this.etw().addMessageListener(this);
         }
      }
   }

   @Override
   public void initialize(String var1, TransportChannelSettings var2, EncoderContext var3) throws ServiceResultException {
      logger.debug("initialize: url={}, settings: {}", var1, var2);
      InetSocketAddress var4 = UriUtil.getSocketAddress(var1);
      this.initialize(var4, var2, var3);
   }

   public void initialize(TransportChannelSettings var1, EncoderContext var2) throws ServiceResultException {
      this.initialize(var1.getDescription().getEndpointUrl(), var1, var2);
   }

   public boolean isOpen() {
      if (this.uX == -1) {
         return false;
      } else {
         long var1 = (long)(this.yt * 1.25) + this.ys;
         long var3 = System.currentTimeMillis();
         return var1 > var3;
      }
   }

   public void onClosed(ServiceResultException var1) {
      if (this.uX != -1) {
         StatusCode var2 = var1 == null ? null : var1.getStatusCode();
         if (var2 != null && var2.isStatusCode(StatusCodes.Bad_ConnectionClosed)) {
            this.d(true);
         } else {
            if (var1 == null) {
               var1 = new ServiceResultException(StatusCodes.Bad_UnexpectedError);
            }

            while (!this.vk.isEmpty()) {
               ArrayList var3 = new ArrayList<>(this.vk.values());

               for (SecureChannelTcp.a var5 : var3) {
                  var5.vv.setError(var1);
               }

               this.vk.values().removeAll(var3);
            }

            this.d(false);
         }

         if (var2 != null && var2.isStatusCode(StatusCodes.Bad_ConnectionClosed)) {
            this.d(true);
         }

         boolean var6 = var1.getStatusCode().isStatusCode(StatusCodes.Bad_ConnectionClosed);
         this.d(var6);
      }
   }

   @Override
   public void onMessage(int var1, int var2, IEncodeable var3) {
      if (var2 == this.uX) {
         SecureChannelTcp.a var4 = this.vk.remove(var1);
         if (var4 != null) {
            if (var3 instanceof ServiceFault) {
               if (var3 instanceof InternalClientSideDecodingServiceFault) {
                  InternalClientSideDecodingServiceFault var7 = (InternalClientSideDecodingServiceFault)var3;
                  var4.vv
                     .setError(
                        new ServiceResultException(
                           StatusCodes.Bad_InternalError,
                           var7.getDecodingException(),
                           "Client-side DecodingException happened while decoding the server reponse"
                        )
                     );
               } else {
                  var4.vv.setError(new ServiceFaultException((ServiceFault)var3));
               }
            } else {
               try {
                  var4.vv.setResult(var3);
               } catch (ClassCastException var6) {
                  logger.error("onMessage: Cannot set result", var6);
               }
            }
         } else {
            if (!(var3 instanceof OpenSecureChannelResponse)) {
               ServiceFault var5 = var3 instanceof ServiceFault ? (ServiceFault)var3 : null;
               if (var5 != null && var5.getResponseHeader().getServiceResult().equals(StatusCode.valueOf(StatusCodes.Bad_TooManyPublishRequests))) {
                  logger.info("ServiceFault={}", var5);
               } else {
                  logger.warn("{} Unidentified message, RequestId={}, type={}!", new Object[]{var2, var1, var3.getClass().getSimpleName()});
                  if (var5 != null) {
                     logger.warn("ServiceFault={}", var5);
                  }
               }
            }
         }
      }
   }

   public void onOpen() {
   }

   public void open() throws ServiceResultException {
      logger.debug("open");
      if (this.uX == -1) {
         try {
            this.etw().open();
         } catch (ServiceResultException var2) {
            logger.warn("Connection failed: {}", var2.getMessage());
            if (!var2.getStatusCode().getValue().equals(StatusCodes.Bad_CommunicationError)) {
               throw var2;
            }

            logger.warn("Bad_CommunicationError: Retrying");
            this.etw().open();
         }

         this.c(false);
      }
   }

   public AsyncResult<SecureChannel> openAsync() {
      final AsyncResultImpl var1 = new AsyncResultImpl();
      if (this.uX != -1) {
         var1.setResult(this);
         return var1;
      } else {
         this.executor.execute(new Runnable() {
            @Override
            public void run() {
               try {
                  SecureChannelTcp.this.open();
                  var1.setResult(SecureChannelTcp.this);
               } catch (ServiceResultException var2) {
                  var1.setError(var2);
               }
            }
         });
         return var1;
      }
   }

   @Override
   public <T extends ServiceResponse> T serviceRequest(ServiceRequest<T> var1) throws ServiceFaultException, ServiceResultException {
      long var2 = this.getRequestTimeout(var1);
      return this.serviceRequest(var1, var2);
   }

   @Override
   public <T extends ServiceResponse> T serviceRequest(ServiceRequest<T> var1, long var2) throws ServiceFaultException, ServiceResultException {
      int var4 = 0;

      while (!this.isOpen()) {
         try {
            if (var4++ > 100) {
               throw new ServiceResultException(StatusCodes.Bad_SecureChannelClosed);
            }

            Thread.sleep(10L);
         } catch (InterruptedException var14) {
         }
      }

      SecureChannelTcp.a var5 = this.a(var1, var2);
      var5.vv = new AsyncResultImpl();
      this.vk.put(var5.requestId, var5);
      logger.debug("serviceRequest: requests.size={}", this.vk.size());

      ServiceResponse var17;
      try {
         try {
            IConnection var6 = this.etw();
            if (var6 != null) {
               var6.sendRequest(var1, this.uX, var5.requestId);
            }

            logger.debug("serviceRequest: Message sent, requestId={} secureChannelId={}", var5.requestId, this.uX);
            logger.trace("serviceRequest: message={}", var1);
         } catch (ServiceResultException var15) {
            if (logger.isDebugEnabled()) {
               logger.debug("serviceRequest: While sending requestId=" + var5.requestId + ", secureChannelId=" + this.uX + ", message=" + var1, var15);
            }

            if (!var15.getStatusCode().isStatusCode(StatusCodes.Bad_CommunicationError)) {
               throw var15;
            }

            var5.yJ = var1;
            this.executor.execute(this.yB);
         }

         if (!(var1 instanceof CloseSecureChannelRequest)) {
            if (var2 == 0L) {
               var17 = (ServiceResponse)var5.vv.waitForResult();
            } else {
               long var7 = var5.vu - System.currentTimeMillis();
               var17 = (ServiceResponse)var5.vv.waitForResult(var7, TimeUnit.MILLISECONDS);
            }

            if (logger.isTraceEnabled()) {
               logger.trace("Response: {}", var17);
            } else {
               logger.debug("Response: {}", var17.getClass().getSimpleName());
            }

            ResponseHeader var19 = var17.getResponseHeader();
            StatusCode var8 = var19.getServiceResult();
            if (var8.isBad()) {
               logger.debug("BAD response: {}", var8);
               throw new ServiceFaultException(new ServiceFault(var19));
            }

            return (T)var17;
         }

         var17 = null;
      } finally {
         this.vk.remove(var5.requestId);
      }

      return (T)var17;
   }

   @Override
   public <T extends ServiceResponse> AsyncResult<T> serviceRequestAsync(ServiceRequest<T> var1) {
      long var2 = this.getRequestTimeout(var1);
      return this.serviceRequestAsync(var1, var2);
   }

   @Override
   public <T extends ServiceResponse> AsyncResult<T> serviceRequestAsync(ServiceRequest<T> var1, long var2) {
      AsyncResultImpl var4 = new AsyncResultImpl();
      if (!this.isOpen()) {
         var4.setError(new ServiceResultException(StatusCodes.Bad_SecureChannelClosed));
         return var4;
      } else {
         SecureChannelTcp.a var5 = this.a(var1, var2);
         var5.vv = var4;
         var5.yJ = var1;
         this.vk.put(var5.requestId, var5);
         logger.debug("serviceRequestAsync: requests.size={}", this.vk.size());
         if (var2 != 0L) {
            this.eto();
         }

         this.executor.execute(this.yB);
         return var4;
      }
   }

   @Override
   public void setOperationTimeout(int var1) {
      this.yu.getConfiguration().setOperationTimeout(var1);
   }

   @Override
   public String toString() {
      return "SecureChannel " + this.uX + " " + (this.isOpen() ? "open" : "closed");
   }

   private void etn() {
      TimerTask var1 = this.vm.getAndSet(null);
      if (var1 != null) {
         var1.cancel();
      }
   }

   private void c(boolean var1) throws ServiceResultException {
      IConnection var2 = this.etw();
      logger.debug("createSecureChannel: renew={} channel={}", var1, var2);
      if (var2 == null) {
         throw new ServiceResultException(StatusCodes.Bad_SecureChannelClosed);
      } else {
         long var3 = System.currentTimeMillis();
         final int var5 = this.vb.incrementAndGet();
         logger.debug("createSecureChannel: requestId={}", var5);
         OpenSecureChannelRequest var6 = new OpenSecureChannelRequest();
         SecurityPolicy var7 = SecurityPolicy.getSecurityPolicy(this.yu.getDescription().getSecurityPolicyUri());
         ByteString var8 = CryptoUtil.createNonce(var7.getSecureChannelNonceLength());
         Integer var9 = this.yu.getConfiguration().getSecurityTokenLifetime();
         if (var9 == null) {
            var9 = 3600000;
         }

         logger.debug("tokenLifetime: {}", var9);
         var6.setClientNonce(var8);
         var6.setClientProtocolVersion(UnsignedInteger.valueOf(0L));
         var6.setRequestedLifetime(UnsignedInteger.valueOf(var9.intValue()));
         var6.setRequestType(var1 ? SecurityTokenRequestType.Renew : SecurityTokenRequestType.Issue);
         var6.setSecurityMode(this.yu.getDescription().getSecurityMode());
         int var10 = var1 ? this.uX : 0;
         final Semaphore var11 = new Semaphore(0);
         final ServiceResultException[] var12 = new ServiceResultException[1];
         final IEncodeable[] var13 = new IEncodeable[1];
         final int[] var14 = new int[1];
         IConnection.IMessageListener var16 = new IConnection.IMessageListener() {
            @Override
            public void onMessage(int var1, int var2x, IEncodeable var3x) {
               if (var1 == var5) {
                  var13[0] = var3x;
                  var14[0] = var2x;
                  var11.release(10);
               }
            }
         };
         IConnectionListener var17 = new IConnectionListener() {
            public void onClosed(ServiceResultException var1) {
               if (var1 == null) {
                  var1 = new ServiceResultException(StatusCodes.Bad_CommunicationError, "Connection Closed");
               }

               var12[0] = var1;
               var11.release(10);
            }

            public void onOpen() {
            }
         };
         var2.addConnectionListener(var17);
         var2.addMessageListener(var16);

         try {
            var2.sendRequest(var6, var10, var5);

            try {
               long var18 = this.getOperationTimeout();
               if (var18 > 0L) {
                  long var20 = (System.currentTimeMillis() - var3) / 1000L;
                  long var22 = var18 - var20;
                  var11.tryAcquire(1, var22, TimeUnit.MILLISECONDS);
               } else {
                  var11.acquire();
               }
            } catch (InterruptedException var29) {
            }

            if (var12[0] != null) {
               throw var12[0];
            }

            IEncodeable var32 = var13[0];
            if (var32 == null) {
               throw new ServiceResultException(StatusCodes.Bad_Timeout);
            }

            if (var32 instanceof ServiceFault) {
               ServiceFaultException var33 = new ServiceFaultException((ServiceFault)var32);
               logger.error(var14 + ": CreateSecureChannel Fault", var33);
               throw var33;
            }

            if (!(var32 instanceof OpenSecureChannelResponse)) {
               throw new ServiceResultException(
                  StatusCodes.Bad_UnexpectedError, "Unexpected result " + var32.getClass().getName() + " OpenSecureChannelResponse expected"
               );
            }

            OpenSecureChannelResponse var19 = (OpenSecureChannelResponse)var32;
            ChannelSecurityToken var34 = var19.getSecurityToken();
            this.uX = var34.getChannelId().intValue();
            if (var1) {
               logger.debug("{} Secure channel renewed, SecureChannelId={}, TokenId={}", new Object[]{this.uX, this.uX, var34.getTokenId().longValue()});
            } else {
               logger.debug("{} Secure channel opened, SecureChannelId={}, TokenId={}", new Object[]{this.uX, this.uX, var34.getTokenId().longValue()});
            }

            if (var1) {
               this.uX = var10;
            }

            long var21 = System.currentTimeMillis();
            this.ys = var3 / 2L + var21 / 2L;
            this.yt = var34.getRevisedLifetime().longValue();
            TimerTask var23 = this.yA;
            this.yA = null;
            if (var23 != null) {
               var23.cancel();
            }

            long var35 = var34.getRevisedLifetime().longValue();
            logger.debug("RevisedLifetime: {}", var35);
            this.yA = TimerUtil.schedule(this.vl, this.yD, this.executor, var21 + (long)(var35 * 0.75));
         } catch (ServiceResultException var30) {
            throw var30;
         } finally {
            var2.removeConnectionListener(var17);
            var2.removeMessageListener(var16);
         }
      }
   }

   private SecureChannelTcp.a ett() {
      long var1 = Long.MAX_VALUE;
      SecureChannelTcp.a var3 = null;
      logger.debug("getNextTimeoutingPendingRequest: requests.size={}", this.vk.size());

      for (SecureChannelTcp.a var5 : this.vk.values()) {
         if (var1 > var5.vu) {
            var1 = var5.vu;
            var3 = var5;
         }
      }

      return var3;
   }

   private SecureChannelTcp.a etu() {
      for (SecureChannelTcp.a var2 : this.vk.values()) {
         if (var2.yJ != null) {
            return var2;
         }
      }

      return null;
   }

   private <T extends ServiceResponse> SecureChannelTcp.a<T> a(ServiceRequest<T> var1, long var2) {
      SecureChannelTcp.a var4 = new SecureChannelTcp.a();
      var4.requestId = this.vb.incrementAndGet();
      var4.vt = System.currentTimeMillis();
      var4.vu = var2 == 0L ? Long.MAX_VALUE : (long)(var4.vt + var2 * 1.1);
      return var4;
   }

   private void eto() {
      SecureChannelTcp.a var1 = this.ett();
      if (var1 == null) {
         this.etn();
      } else {
         TimerTask var2 = this.vm.get();
         if (var2 == null || var2.scheduledExecutionTime() > var1.vu) {
            this.etn();
            var2 = TimerUtil.schedule(this.vl, this.vq, this.executor, var1.vu);
            if (!this.vm.compareAndSet(null, var2)) {
               var2.cancel();
            }
         }
      }
   }

   private void etv() throws ServiceResultException {
      if (this.isOpen()) {
         SecureChannelTcp.a var2 = null;

         while (true) {
            var2 = this.etu();
            if (var2 == null) {
               return;
            }

            IEncodeable var1 = var2.yJ;
            var2.yJ = null;
            long var3 = System.currentTimeMillis();
            long var5 = var3 - var2.vt;
            if (var3 > var2.vu) {
               logger.debug("Request id={} timeouted {}ms elapsed. timeout at {} ms", new Object[]{var2.requestId, var5, var2.vu - var2.vt});
               this.vk.remove(var2.requestId);
               var2.vv.setError(new ServiceResultException(StatusCodes.Bad_Timeout));
            } else if (var1 != null) {
               try {
                  logger.debug("sendPendingRequestMessages: requestId={}", var2.requestId);
                  IConnection var7 = this.etw();
                  if (var7 != null) {
                     var7.sendRequest((ServiceRequest)var1, this.uX, var2.requestId);
                  }
               } catch (EncodingException var9) {
                  this.vk.remove(var2.requestId);
                  var2.vv.setError(var9);
               } catch (ServiceResultException var10) {
                  StatusCode var8 = var10.getStatusCode();
                  if (var8.isStatusCode(StatusCodes.Bad_CommunicationError)) {
                     var2.yJ = var1;
                  } else {
                     var2.vv.setError(var10);
                  }
               }
            }
         }
      }
   }

   private void d(boolean var1) {
      synchronized (this.yz) {
         if (this.yy != var1) {
            if (var1) {
               logger.info("{}: Error recovery = true", this.uX);
               this.yy = true;
               this.yw = 0;
               if (!disableReconnectLogic) {
                  long var3 = System.currentTimeMillis();
                  this.yx = TimerUtil.schedule(this.vl, this.yC, this.executor, var3 + yr[0]);
               }
            } else {
               logger.info("{}: Error recovery = false", this.uX);
               this.yy = false;
               this.yw = 0;
               this.yx.cancel();
               this.yx = null;
            }
         }
      }
   }

   protected long getRequestTimeout(ServiceRequest var1) {
      UnsignedInteger var2 = var1.getRequestHeader() != null ? var1.getRequestHeader().getTimeoutHint() : null;
      return var2 != null ? var2.longValue() : this.getOperationTimeout();
   }

   protected void setTransportChannel(IConnection var1) {
      this.yv.set(var1);
   }

   IConnection etw() {
      return this.yv.get();
   }

   static class a<T extends ServiceResponse> {
      long vt = System.currentTimeMillis();
      long vu;
      int requestId;
      AsyncResultImpl<T> vv;
      IEncodeable yJ;
   }
}
