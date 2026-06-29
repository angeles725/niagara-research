package com.prosysopc.ua.stack.transport.tcp.io;

import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.ServiceRequest;
import com.prosysopc.ua.stack.builtintypes.ServiceResponse;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.common.RuntimeServiceResultException;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.ChannelSecurityToken;
import com.prosysopc.ua.stack.core.CloseSecureChannelRequest;
import com.prosysopc.ua.stack.core.EndpointConfiguration;
import com.prosysopc.ua.stack.core.EndpointDescription;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.core.OpenSecureChannelRequest;
import com.prosysopc.ua.stack.core.OpenSecureChannelResponse;
import com.prosysopc.ua.stack.core.ResponseHeader;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.encoding.DecodingException;
import com.prosysopc.ua.stack.encoding.EncodeType;
import com.prosysopc.ua.stack.encoding.EncoderContext;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.IEncodeable;
import com.prosysopc.ua.stack.encoding.binary.BinaryDecoder;
import com.prosysopc.ua.stack.encoding.binary.BinaryEncoder;
import com.prosysopc.ua.stack.transport.IConnectionListener;
import com.prosysopc.ua.stack.transport.ReverseConnectionListener;
import com.prosysopc.ua.stack.transport.ReverseTransportChannelSettings;
import com.prosysopc.ua.stack.transport.TransportChannelSettings;
import com.prosysopc.ua.stack.transport.UriUtil;
import com.prosysopc.ua.stack.transport.security.Cert;
import com.prosysopc.ua.stack.transport.security.CertificateValidator;
import com.prosysopc.ua.stack.transport.security.KeyPair;
import com.prosysopc.ua.stack.transport.security.PrivKey;
import com.prosysopc.ua.stack.transport.security.SecurityAlgorithm;
import com.prosysopc.ua.stack.transport.security.SecurityConfiguration;
import com.prosysopc.ua.stack.transport.security.SecurityMode;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.transport.tcp.impl.Acknowledge;
import com.prosysopc.ua.stack.transport.tcp.impl.ChunkAsymmDecryptVerifier;
import com.prosysopc.ua.stack.transport.tcp.impl.ChunkAsymmEncryptSigner;
import com.prosysopc.ua.stack.transport.tcp.impl.ChunkFactory;
import com.prosysopc.ua.stack.transport.tcp.impl.ChunkSymmDecryptVerifier;
import com.prosysopc.ua.stack.transport.tcp.impl.ChunkSymmEncryptSigner;
import com.prosysopc.ua.stack.transport.tcp.impl.ChunkUtils;
import com.prosysopc.ua.stack.transport.tcp.impl.ErrorMessage;
import com.prosysopc.ua.stack.transport.tcp.impl.Hello;
import com.prosysopc.ua.stack.transport.tcp.impl.InternalBinaryEncodingsHelper;
import com.prosysopc.ua.stack.transport.tcp.impl.InternalClientSideDecodingServiceFault;
import com.prosysopc.ua.stack.transport.tcp.impl.ReverseHello;
import com.prosysopc.ua.stack.transport.tcp.impl.SecurityToken;
import com.prosysopc.ua.stack.utils.CertificateUtils;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import com.prosysopc.ua.stack.utils.SizeCalculationOutputStream;
import com.prosysopc.ua.stack.utils.StackUtils;
import com.prosysopc.ua.stack.utils.TimerUtil;
import com.prosysopc.ua.stack.utils.bytebuffer.ByteBufferArrayReadable;
import com.prosysopc.ua.stack.utils.bytebuffer.ByteBufferArrayWriteable2;
import com.prosysopc.ua.stack.utils.bytebuffer.InputStreamReadable;
import com.prosysopc.ua.stack.utils.bytebuffer.OutputStreamWriteable;
import com.prosysopc.ua.stack.utils.bytebuffer.ByteBufferArrayWriteable2.ChunkListener;
import com.prosysopc.ua.types.opcua.Ids;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpConnection implements IConnection {
   static Logger logger = LoggerFactory.getLogger(TcpConnection.class);
   private static int yK = 0;
   private static int yL = 60000;
   private static int yM = 0;
   private static int yN = 0;
   private static TcpConnection.SocketFactory yO = new TcpConnection.DefaultSocketFactory();
   private static TcpConnection.ReverseConnectionProvider yP = new TcpConnection.DefaultReverseConnectionProvider();
   private static TcpConnection.ExecutorProvider yQ = new TcpConnection.DefaultExecutorProvider();
   EncodeType encodeType;
   PrivKey rF;
   Cert rE;
   Cert rD;
   EndpointConfiguration fZ;
   EndpointDescription yR;
   CertificateValidator cr;
   InetSocketAddress addr;
   TcpConnectionLimits yS;
   TcpQuotas yT = TcpQuotas.DEFAULT_CLIENT_QUOTA;
   EnumSet<OpcTcpSettings.Flag> yn = EnumSet.noneOf(OpcTcpSettings.Flag.class);
   int connectTimeout = yL;
   int yo = yL;
   SecurityConfiguration securityConfiguration;
   final List<SecurityToken> yU = new CopyOnWriteArrayList<>();
   final Map<Integer, SecurityToken> yV = new ConcurrentHashMap<>();
   final Map<Integer, ByteString> yW = new ConcurrentHashMap<>();
   final Map<Integer, SequenceNumber> yX = new ConcurrentHashMap<>();
   private TcpConnection.SocketWrapper yY = null;
   int yZ;
   OutputStreamWriteable za;
   ReentrantLock lock = new ReentrantLock();
   TcpConnection.b zb;
   EncoderContext ti;
   List<IConnection.IMessageListener> listeners = new CopyOnWriteArrayList<>();
   List<IConnectionListener> zc = new CopyOnWriteArrayList<>();
   ReverseConnectionListener gL = null;
   boolean zd = false;
   int yp = yM;

   public static int getDefaultHandshakeTimeout() {
      return yL;
   }

   public static int getDefaultReverseHelloAcceptTimeout() {
      return yM;
   }

   public static TcpConnection.ExecutorProvider getExecutorProvider() {
      return yQ;
   }

   public static int getReceiveBufferSize() {
      return yK;
   }

   public static TcpConnection.ReverseConnectionProvider getReverseConnectionProvider() {
      return yP;
   }

   public static int getSendBufferSize() {
      return yN;
   }

   public static TcpConnection.SocketFactory getSocketFactory() {
      return yO;
   }

   public static void setDefaultHandshakeTimeout(int var0) {
      yL = var0;
   }

   public static void setDefaultReverseHelloAcceptTimeout(int var0) {
      yM = var0;
   }

   public static void setExecutorProvider(TcpConnection.ExecutorProvider var0) {
      yQ = var0;
   }

   public static void setReceiveBufferSize(int var0) {
      yK = var0;
   }

   public static void setReverseConnectionProvider(TcpConnection.ReverseConnectionProvider var0) {
      yP = var0;
   }

   public static void setSendBufferSize(int var0) {
      yN = var0;
   }

   public static void setSocketFactory(TcpConnection.SocketFactory var0) {
      yO = var0;
   }

   @Override
   public void addConnectionListener(IConnectionListener var1) {
      this.zc.add(var1);
   }

   @Override
   public void addMessageListener(IConnection.IMessageListener var1) {
      this.listeners.add(var1);
   }

   @Override
   public void close() {
      TcpConnection.b var1 = this.zb;
      if (var1 != null) {
         var1.zk = true;
      }

      this.a(new ServiceResultException(StatusCodes.Bad_CommunicationError, "Socket closed by the user"));
   }

   @Override
   public void dispose() {
      this.lock.lock();

      try {
         this.close();
         this.rF = null;
         this.rE = null;
         this.rD = null;
         this.fZ = null;
         this.yR = null;
         this.cr = null;
         this.setSocket(null);
         this.ti = null;
         this.za = null;
         this.yT = null;
         this.yS = null;
      } finally {
         this.lock.unlock();
      }
   }

   public EndpointConfiguration getEndpointConfiguration() {
      return this.fZ;
   }

   public EndpointDescription getEndpointDescription() {
      return this.yR;
   }

   public int getHandshakeTimeout() {
      return this.yo;
   }

   public EncoderContext getMessageContext() {
      return this.ti;
   }

   public int getProtocolVersion() {
      return this.yZ;
   }

   public int getReverseHelloAcceptTimeout() {
      return this.yp;
   }

   public SocketAddress getSocketAddress() {
      return this.addr;
   }

   @Override
   public void initialize(InetSocketAddress var1, TransportChannelSettings var2, EncoderContext var3) throws ServiceResultException {
      this.lock.lock();

      try {
         if (var2.getOpctcpSettings().getConnectTimeout() >= 0) {
            this.connectTimeout = var2.getOpctcpSettings().getConnectTimeout();
         }

         if (var2.getOpctcpSettings().getHandshakeTimeout() >= 0) {
            this.yo = var2.getOpctcpSettings().getHandshakeTimeout();
         }

         if (var2.getOpctcpSettings().getReverseHelloAcceptTimeout() >= 0) {
            this.yp = var2.getOpctcpSettings().getReverseHelloAcceptTimeout();
         }

         this.addr = var1;
         if (var2 instanceof ReverseTransportChannelSettings) {
            this.gL = ((ReverseTransportChannelSettings)var2).getReverseConnectionListener();
            this.zd = true;
         } else {
            this.gL = null;
            this.zd = false;
         }

         this.fZ = var2.getConfiguration().clone();
         this.yR = var2.getDescription().clone();
         this.cr = var2.getOpctcpSettings().getCertificateValidator();
         this.ti = var3;
         this.rE = var2.getOpctcpSettings().getClientCertificate();
         this.rD = var2.getServerCertificate();
         this.rF = var2.getOpctcpSettings().getPrivKey();
         this.encodeType = EncodeType.Binary;
         if (this.fZ.getUseBinaryEncoding() != null && !this.fZ.getUseBinaryEncoding()) {
            this.encodeType = EncodeType.Xml;
         }

         this.yn = var2.getOpctcpSettings().getFlags();
         KeyPair var4 = this.rE == null ? null : new KeyPair(this.rE, this.rF);
         SecurityPolicy var5 = SecurityPolicy.getSecurityPolicy(this.yR.getSecurityPolicyUri());
         SecurityMode var6 = new SecurityMode(var5, this.yR.getSecurityMode());
         this.securityConfiguration = new SecurityConfiguration(var6, var4, this.rD);
      } finally {
         this.lock.unlock();
      }
   }

   public void initialize(String var1, TransportChannelSettings var2, EncoderContext var3) throws ServiceResultException {
      try {
         InetSocketAddress var4 = UriUtil.getSocketAddress(var1);
         this.initialize(var4, var2, var3);
      } catch (IllegalArgumentException var5) {
         logger.error("Error while TcpConnection.initialize", var5);
         throw new ServiceResultException(StatusCodes.Bad_ServerUriInvalid);
      }
   }

   public void initialize(TransportChannelSettings var1, EncoderContext var2) throws ServiceResultException {
      this.initialize(var1.getDescription().getEndpointUrl(), var1, var2);
   }

   @Override
   public void open() throws ServiceResultException {
      this.lock.lock();

      try {
         TcpConnection.SocketWrapper var1 = this.getSocket();
         if (var1 != null && var1.isConnected()) {
            return;
         }

         if (this.zd) {
            try {
               try {
                  var1 = yP.provideOpenReverseConnectionSocket(this);
               } catch (SocketException var29) {
                  logger.info("ServerSocket.accept {} failed (or the socket was closed while waiting)", this.addr, var29);
                  throw new ServiceResultException(
                     StatusCodes.Bad_UnexpectedError, var29, "ServerSocket.accept failed (or was closed, possibly due to a timeout)"
                  );
               }

               var1.setTcpNoDelay(true);
               if (yK > 0) {
                  var1.setReceiveBufferSize(yK);
               }

               if (yN > 0) {
                  var1.setSendBufferSize(yN);
               }

               if (this.yo > 0) {
                  var1.setSoTimeout(this.yo);
               }

               this.setSocket(var1);
               logger.debug("{} Socket connected", var1.getRemoteSocketAddress());
            } catch (IOException var30) {
               logger.info(this.addr + " Connect failed", var30);
               throw new ServiceResultException(StatusCodes.Bad_ConnectionRejected, var30);
            }
         } else {
            try {
               logger.info("{} Connecting", this.addr);
               var1 = yO.createSocket(this);
               var1.setTcpNoDelay(true);
               if (yK > 0) {
                  var1.setReceiveBufferSize(yK);
               }

               if (yN > 0) {
                  var1.setSendBufferSize(yN);
               }

               this.setSocket(var1);
               if (this.yo > 0) {
                  var1.setSoTimeout(this.yo);
               }

               if (this.connectTimeout == 0) {
                  var1.connect(this.addr);
               } else {
                  var1.connect(this.addr, this.connectTimeout);
               }
            } catch (ConnectException var31) {
               logger.info(this.addr + " Connect failed", var31);
               throw new ServiceResultException(StatusCodes.Bad_ConnectionRejected, var31);
            } catch (IOException var32) {
               logger.info(this.addr + " Connect failed", var32);
               throw new ServiceResultException(StatusCodes.Bad_ConnectionRejected, var32);
            } catch (IllegalArgumentException var33) {
               throw new ServiceResultException(StatusCodes.Bad_ServerUriInvalid);
            }

            logger.debug("{} Socket connected", this.addr);
         }

         try {
            this.etx();
            OutputStreamWriteable var2 = new OutputStreamWriteable(new BufferedOutputStream(var1.getOutputStream()));
            var2.order(ByteOrder.LITTLE_ENDIAN);
            InputStreamReadable var3 = new InputStreamReadable(new BufferedInputStream(var1.getInputStream()), Long.MAX_VALUE);
            var3.order(ByteOrder.LITTLE_ENDIAN);
            BinaryDecoder var4 = new BinaryDecoder(var3);
            var4.setEncoderContext(this.ti);
            BinaryEncoder var5 = new BinaryEncoder(var2);
            var5.setEncoderContext(this.ti);
            if (this.zd) {
               ReverseHello var6 = this.a(var3, var4);
               logger.debug("Got ReverseHello: {}", var6);
               if (var6.getServerUri() == null || var6.getServerUri().length() > 4096) {
                  logger.error("ReverseHello did not contain ServerUri, or is too long, got:{}", var6.getServerUri());
                  throw new ServiceResultException(StatusCodes.Bad_TcpEndpointUrlInvalid);
               }

               if (var6.getEndpointUrl() == null || var6.getEndpointUrl().length() > 4096) {
                  logger.error("ReverseHello did not contain correct EndpointUrl, or is too long, got:{}", var6.getEndpointUrl());
                  throw new ServiceResultException(StatusCodes.Bad_TcpEndpointUrlInvalid);
               }

               if (this.gL != null && !this.gL.onConnect(var6.getServerUri(), var6.getEndpointUrl(), var1.getRemoteSocketAddress())) {
                  throw new ServiceResultException(StatusCodes.Bad_ConnectionClosed, "Reverse Connection rejected by the ReverseConnectionListener");
               }

               if (this.yR.getEndpointUrl() == null) {
                  this.yR.setEndpointUrl(var6.getEndpointUrl());
               }
            }

            Hello var38 = new Hello();
            var38.setEndpointUrl(this.yR.getEndpointUrl());
            var38.setMaxChunkCount(UnsignedInteger.valueOf(this.fZ.getMaxBufferSize() == null ? 65535L : this.fZ.getMaxBufferSize().intValue()));
            var38.setMaxMessageSize(UnsignedInteger.valueOf(this.ti.getMaxMessageSize()));
            var38.setReceiveBufferSize(UnsignedInteger.valueOf(this.yT.maxBufferSize));
            var38.setSendBufferSize(UnsignedInteger.valueOf(this.yT.maxBufferSize));
            var38.setProtocolVersion(UnsignedInteger.valueOf(0L));
            if (this.yS != null) {
               var38.setProtocolVersion(UnsignedInteger.valueOf(this.yZ));
               var38.setMaxChunkCount(UnsignedInteger.valueOf(this.yS.maxRecvChunkCount));
               var38.setMaxMessageSize(UnsignedInteger.valueOf(this.yS.maxRecvMessageSize));
               var38.setSendBufferSize(UnsignedInteger.valueOf(this.yS.maxSendBufferSize));
               var38.setReceiveBufferSize(UnsignedInteger.valueOf(this.yS.maxRecvBufferSize));
            }

            logger.debug("Writing Hello: {}", var38);
            SizeCalculationOutputStream var7 = new SizeCalculationOutputStream();
            BinaryEncoder var8 = new BinaryEncoder(var7);
            var8.setEncoderContext(this.ti);
            var2.putInt(1179403592);
            InternalBinaryEncodingsHelper.putUaTcpCommMessage(var8, var38);
            int var9 = var7.getLength() + 8;
            var2.putInt(var9);
            InternalBinaryEncodingsHelper.putUaTcpCommMessage(var5, var38);
            var2.flush();
            int var10 = -1;

            while (var10 == -1) {
               try {
                  var10 = var3.getInt();
               } catch (EOFException var28) {
                  var10 = var3.getInt();
               }
            }

            var9 = var3.getInt();
            if (var9 < 8 || var9 > 4096) {
               throw new ServiceResultException(StatusCodes.Bad_TcpMessageTooLarge);
            }

            if (var10 == 1179800133) {
               var4.getEncoderContext().setMaxStringLength(4096);
               ErrorMessage var40 = InternalBinaryEncodingsHelper.getUaTcpCommMessage(var4, ErrorMessage.class);
               throw new ServiceResultException(StatusCode.valueOf(var40.getError()), var40.getReason());
            }

            if (var10 != 1179337537) {
               throw new ServiceResultException(StatusCodes.Bad_TcpMessageTypeInvalid, "Message type was " + var10 + ", expected " + 1179337537);
            }

            Acknowledge var11 = InternalBinaryEncodingsHelper.getUaTcpCommMessage(var4, Acknowledge.class);
            logger.debug("Received Acknowledge: {}", var11);
            if (var11.getProtocolVersion().intValue() < var38.getProtocolVersion().intValue()) {
               throw new ServiceResultException(
                  StatusCodes.Bad_ProtocolVersionUnsupported,
                  "Version " + var38.getProtocolVersion().intValue() + " requested, got " + var11.getProtocolVersion()
               );
            }

            this.yZ = Math.min(var38.getProtocolVersion().intValue(), var11.getProtocolVersion().intValue());
            if (var11.getMaxMessageSize().equals(UnsignedInteger.valueOf(0L))) {
               var11.setMaxMessageSize(UnsignedInteger.valueOf(2147483647L));
            }

            if (var11.getMaxChunkCount().equals(UnsignedInteger.valueOf(0L))) {
               var11.setMaxChunkCount(UnsignedInteger.valueOf(2147483647L));
            }

            if (var11.getReceiveBufferSize().longValue() > var38.getReceiveBufferSize().longValue()) {
               throw new ServiceResultException(StatusCodes.Bad_TcpInternalError, "Acknowledge.ReceiveBufferSize > Hello.ReceiveBufferSize");
            }

            if (var11.getReceiveBufferSize().longValue() < 8192L) {
               throw new ServiceResultException(StatusCodes.Bad_TcpInternalError, "Server recv buffer size < 8192");
            }

            if (var11.getSendBufferSize().longValue() > var38.getSendBufferSize().longValue()) {
               throw new ServiceResultException(StatusCodes.Bad_TcpInternalError, "Acknowledge.SendBufferSize > Hello.SendBufferSize");
            }

            if (var11.getSendBufferSize().longValue() < 8192L) {
               throw new ServiceResultException(StatusCodes.Bad_TcpInternalError, "Server send buffer size < 8192");
            }

            this.yS = new TcpConnectionLimits();
            this.yS.maxSendBufferSize = (int)Math.min(var11.getSendBufferSize().longValue(), Long.valueOf(2147483647L));
            this.yS.maxRecvBufferSize = (int)Math.min(var11.getReceiveBufferSize().longValue(), Long.valueOf(2147483647L));
            this.yS.maxSendChunkCount = (int)Math.min(var11.getMaxChunkCount().longValue(), Long.valueOf(2147483647L));
            this.yS.maxRecvChunkCount = (int)Math.min(var38.getMaxChunkCount().longValue(), Long.valueOf(2147483647L));
            this.yS.maxSendMessageSize = (int)Math.min(var11.getMaxMessageSize().longValue(), Long.valueOf(2147483647L));
            this.yS.maxRecvMessageSize = (int)Math.min(var38.getMaxMessageSize().longValue(), Long.valueOf(2147483647L));
            var1.setSoTimeout(0);
            var1.setKeepAlive(true);
            if (this.zd) {
               logger.info("Connected (reverse), handshake completed, local={}, remote={}", var1.getLocalSocketAddress(), var1.getRemoteSocketAddress());
            } else {
               logger.info("Connected (non-reverse), handshake completed, local={}, remote={}", var1.getLocalSocketAddress(), var1.getRemoteSocketAddress());
            }

            for (IConnectionListener var13 : this.zc) {
               var13.onOpen();
            }

            logger.debug("Creating ReadThread");
            this.zb = new TcpConnection.b(var1, var4.getEncoderContext());
            this.zb.start();
            this.ti = var5.getEncoderContext();
            this.za = var2;
         } catch (IOException var34) {
            try {
               var1.close();
            } catch (IOException var27) {
            }

            this.setSocket(null);
            logger.info(this.addr + " Connect failed", var34);
            throw new ServiceResultException(StatusCodes.Bad_CommunicationError, var34);
         } catch (ServiceResultException var35) {
            try {
               var1.close();
            } catch (IOException var26) {
            }

            this.setSocket(null);
            logger.info(this.addr + " Connect failed", var35);
            throw var35;
         }
      } finally {
         this.lock.unlock();
      }
   }

   @Override
   public void reconnect() throws ServiceResultException {
      this.lock.lock();

      try {
         TcpConnection.SocketWrapper var1 = this.getSocket();
         if (var1 != null && var1.isConnected() && !var1.isClosed()) {
            this.close();
         }

         this.open();
      } finally {
         this.lock.unlock();
      }
   }

   @Override
   public void removeConnectionListener(IConnectionListener var1) {
      this.zc.remove(var1);
   }

   @Override
   public void removeMessageListener(IConnection.IMessageListener var1) {
      this.listeners.remove(var1);
   }

   @Override
   public void sendRequest(ServiceRequest var1, int var2, int var3) throws ServiceResultException {
      if (var1 == null) {
         logger.warn("sendRequest: request=null");
      }

      boolean var4 = var1 instanceof OpenSecureChannelRequest;
      TcpConnection.SocketWrapper var5 = this.getSocket();
      logger.debug("sendRequest: socket={}", var5);

      try {
         if (var5 != null && var5.isConnected() && !var5.isClosed()) {
            logger.debug("sendRequest: {} Sending Request rid:{}", var2, var3);
            logger.trace("sendrequest: request={}", var1);
            SecurityToken var6 = null;
            SizeCalculationOutputStream var7 = new SizeCalculationOutputStream();
            BinaryEncoder var8 = new BinaryEncoder(var7);
            var8.setEncoderContext(this.ti);
            InternalBinaryEncodingsHelper.putServiceRequest(var8, var1);
            int var9 = var7.getLength();
            if (var2 != 0) {
               var6 = this.v(var2);
            }

            logger.debug("sendRequest: token={}", var6);
            SecurityMode var10 = this.a(var4, var1, var6);
            int var11 = var6 != null ? var6.getSecurityPolicy().getEncryptionKeySize() : 0;
            logger.debug("sendRequest: keySize={}", var11);
            ChunkFactory var12 = this.a(var4, var10, var11);
            if (var12 != null) {
               TcpConnection.a var13 = this.a(var12, var9, var1);
               if (var13 != null) {
                  ByteBuffer[] var14 = var13.etz();
                  ByteBuffer[] var15 = var13.etA();
                  if (var14 != null & var15 != null) {
                     try {
                        this.lock.lock();

                        try {
                           if (var4) {
                              ByteString var29 = ((OpenSecureChannelRequest)var1).getClientNonce();
                              this.yW.put(var3, var29);

                              for (int var30 = 0; var30 < var14.length; var30++) {
                                 boolean var31 = var30 == var14.length - 1;
                                 this.a(var2, var3, var10, var14[var30], var15[var30], var31);
                                 var15[var30] = null;
                                 var14[var30] = null;
                              }
                           } else {
                              this.yV.put(var2, var6);
                              SequenceNumber var16 = this.yX.get(var2);

                              for (int var17 = 0; var17 < var14.length; var17++) {
                                 ByteBuffer var18 = var14[var17];
                                 ByteBuffer var19 = var15[var17];
                                 boolean var20 = var18 == var14[var14.length - 1];
                                 int var21 = 1128747853;
                                 if (var20) {
                                    var21 = 1179079501;
                                 }

                                 if (var1 instanceof CloseSecureChannelRequest) {
                                    var21 = 1179601987;
                                 }

                                 this.a(var3, var6, var16, var18, var19, var21);
                                 var15[var17] = null;
                                 var14[var17] = null;
                              }
                           }

                           this.za.flush();
                        } catch (IOException var26) {
                           this.yW.remove(var3);
                           logger.info(this.addr + " Connect failed", var26);
                           this.close();
                           throw new ServiceResultException(StatusCodes.Bad_CommunicationError, var26);
                        }
                     } finally {
                        this.lock.unlock();
                     }
                  }
               }
            }
         } else {
            throw new ServiceResultException(StatusCodes.Bad_ServerNotConnected);
         }
      } catch (RuntimeException var28) {
         logger.warn(String.format(Locale.ROOT, "sendRequest %s failed: socket=%s, asymm=%s", var1.getClass().getName(), var5, var4), var28);
         throw var28;
      }
   }

   public void setHandshakeTimeout(int var1) {
      this.yo = var1;
   }

   public void setReverseHelloAcceptTimeout(int var1) {
      this.yp = var1;
   }

   private void a(ServiceResultException var1) {
      this.lock.lock();

      try {
         TcpConnection.SocketWrapper var2 = this.getSocket();
         if (var2 == null || !var2.isConnected() || var2.isClosed()) {
            return;
         }

         try {
            var2.close();
         } catch (IOException var7) {
            logger.warn(this.addr + " Close error", var7);
         }

         this.setSocket(null);
         this.yW.clear();
         logger.info(this.addr + " Closed");
      } finally {
         this.lock.unlock();
      }

      for (IConnectionListener var3 : this.zc) {
         var3.onClosed(var1);
      }
   }

   private TcpConnection.a a(ChunkFactory var1, int var2, ServiceRequest<?> var3) throws ServiceResultException {
      if (this.ti.getMaxMessageSize() != 0 && var2 > this.ti.getMaxMessageSize()) {
         EncodingException var14 = new EncodingException(StatusCodes.Bad_EncodingLimitsExceeded, "MaxMessageSize " + this.ti.getMaxMessageSize() + " < " + var2);
         logger.warn("encodeMessage: failed", var14);
         throw var14;
      } else {
         int var4 = (var2 + var1.maxPlaintextSize - 1) / var1.maxPlaintextSize;
         this.lock.lock();

         int var5;
         try {
            if (this.yS == null) {
               return null;
            }

            var5 = this.yS.maxSendChunkCount;
         } finally {
            this.lock.unlock();
         }

         if (var5 != 0 && var4 > var5) {
            throw new ServiceResultException(StatusCodes.Bad_TcpMessageTooLarge);
         } else {
            int var6 = var2;
            ByteBuffer[] var7 = new ByteBuffer[var4];
            ByteBuffer[] var8 = new ByteBuffer[var4];

            for (int var9 = 0; var9 < var4; var9++) {
               var7[var9] = var1.allocate(var6);
               var8[var9] = var1.expandToCompleteChunk(var7[var9]);
               var6 -= var7[var9].remaining();
            }

            ChunkListener var16 = new ChunkListener() {
               public void onChunkComplete(ByteBuffer[] var1, int var2x) {
               }
            };
            ByteBufferArrayWriteable2 var10 = new ByteBufferArrayWriteable2(var7, var16);
            var10.order(ByteOrder.LITTLE_ENDIAN);
            BinaryEncoder var11 = new BinaryEncoder(var10);
            var11.setEncoderContext(this.ti);
            InternalBinaryEncodingsHelper.putServiceRequest(var11, var3);
            return new TcpConnection.a(var8, var7);
         }
      }
   }

   private ChunkFactory a(boolean var1, SecurityMode var2, int var3) throws ServiceResultException {
      MessageSecurityMode var4 = var2.getMessageSecurityMode();
      this.lock.lock();

      int var5;
      try {
         if (this.yS == null) {
            return null;
         }

         var5 = this.yS.maxSendBufferSize;
      } finally {
         this.lock.unlock();
      }

      if (var1) {
         return new ChunkFactory.AsymmMsgChunkFactory(var5, this.securityConfiguration);
      } else {
         SecurityPolicy var6 = var2.getSecurityPolicy();
         SecurityAlgorithm var7 = var6.getSymmetricEncryptionAlgorithm();
         SecurityAlgorithm var8 = var6.getSymmetricSignatureAlgorithm();
         int var9 = CryptoUtil.getCipherBlockSize(var7, null);
         int var10 = CryptoUtil.getSignatureSize(var8, null);
         return new ChunkFactory(var5, 8, 8, 8, var10, var9, var4, var3);
      }
   }

   private SecurityMode a(boolean var1, ServiceRequest<?> var2, SecurityToken var3) {
      SecurityPolicy var4;
      MessageSecurityMode var5;
      if (var1) {
         var5 = ((OpenSecureChannelRequest)var2).getSecurityMode();
         var4 = this.securityConfiguration.getSecurityMode().getSecurityPolicy();
      } else {
         var5 = var3.getMessageSecurityMode();
         var4 = var3.getSecurityPolicy();
      }

      return new SecurityMode(var4, var5);
   }

   private SecurityToken v(int var1) throws ServiceResultException {
      this.etl();
      SecurityToken var2 = null;
      logger.debug("tokens={}", this.yU);

      for (SecurityToken var4 : this.yU) {
         if (var4.getSecureChannelId() == var1 && (var2 == null || var2.getCreationTime() < var4.getCreationTime())) {
            var2 = var4;
         }
      }

      logger.debug("getSecurityTokenToUse={}", var2);
      if (var2 == null) {
         throw new ServiceResultException(StatusCodes.Bad_CommunicationError, "All security tokens have expired");
      } else {
         return var2;
      }
   }

   private void etx() {
      int var1 = Math.min(this.fZ.getMaxMessageSize() != null ? this.fZ.getMaxMessageSize() : Integer.MAX_VALUE, this.yT.maxMessageSize);
      this.ti.setMaxMessageSize(var1);
      this.ti.setMaxArrayLength(this.fZ.getMaxArrayLength() != null ? this.fZ.getMaxArrayLength() : 0);
      this.ti.setMaxStringLength(this.fZ.getMaxStringLength() != null ? this.fZ.getMaxStringLength() : 0);
      this.ti.setMaxByteStringLength(this.fZ.getMaxByteStringLength() != null ? this.fZ.getMaxByteStringLength() : 0);
   }

   private void etl() {
      logger.debug("pruneInvalidTokens: tokens({})={}", this.yU.size(), this.yU);

      for (SecurityToken var2 : this.yU) {
         if (!var2.isValid()) {
            this.yU.remove(var2);
         }
      }
   }

   private ReverseHello a(InputStreamReadable var1, BinaryDecoder var2) throws IOException, ServiceResultException, DecodingException {
      int var3 = -1;

      while (var3 == -1) {
         try {
            var3 = var1.getInt();
         } catch (EOFException var6) {
            var3 = var1.getInt();
         }
      }

      int var4 = var1.getInt();
      if (var4 < 8 || var4 > 4096) {
         throw new ServiceResultException(StatusCodes.Bad_TcpMessageTooLarge);
      } else if (var3 != 1178945618) {
         logger.error("Did not receive correct message type, expecting: {}, got: {}", 1178945618, var3);
         throw new ServiceResultException(StatusCodes.Bad_TcpMessageTypeInvalid, "Message type was " + var3 + ", expected " + 1178945618);
      } else {
         return InternalBinaryEncodingsHelper.getUaTcpCommMessage(var2, ReverseHello.class);
      }
   }

   private void a(int var1, int var2, SecurityMode var3, ByteBuffer var4, ByteBuffer var5, boolean var6) throws ServiceResultException, IOException {
      ((Buffer)var4).rewind();
      var4.putInt(var6 ? 1179537487 : 1129205839);
      ((Buffer)var4).position(8);
      var4.putInt(var1);
      byte[] var7 = var3.getSecurityPolicy().getEncodedPolicyUri();
      var4.putInt(var7.length);
      var4.put(var7);
      var7 = this.securityConfiguration.getEncodedLocalCertificate();
      var4.putInt(var7 == null ? -1 : var7.length);
      if (var7 != null) {
         var4.put(var7);
      }

      var7 = this.securityConfiguration.getEncodedRemoteCertificateThumbprint();
      var4.putInt(var7 == null ? -1 : var7.length);
      if (var7 != null) {
         var4.put(var7);
      }

      SequenceNumber var8 = this.yX.get(var1);
      int var9 = var8 == null ? 1 : var8.getNextSendSequencenumber();
      var4.putInt(var9);
      var4.putInt(var2);
      logger.debug("SecureChannelId={} SequenceNumber={}, RequestId={}", new Object[]{var1, var9, var2});

      try {
         new ChunkAsymmEncryptSigner(var4, var5, this.securityConfiguration).run();
      } catch (RuntimeServiceResultException var11) {
         throw var11.getCause();
      }

      ((Buffer)var4).rewind();
      this.za.put(var4);
   }

   private void a(int var1, SecurityToken var2, SequenceNumber var3, ByteBuffer var4, ByteBuffer var5, int var6) throws ServiceResultException, IOException {
      ((Buffer)var4).rewind();
      var4.putInt(var6);
      ((Buffer)var4).position(8);
      var4.putInt(var2.getSecureChannelId());
      var4.putInt(var2.getTokenId());
      int var7 = var3.getNextSendSequencenumber();
      var4.putInt(var7);
      var4.putInt(var1);

      try {
         new ChunkSymmEncryptSigner(var4, var5, var2).run();
      } catch (RuntimeServiceResultException var9) {
         throw var9.getCause();
      }

      ((Buffer)var4).rewind();
      this.za.put(var4);
   }

   protected TcpConnection.SocketWrapper getSocket() {
      this.lock.lock();

      TcpConnection.SocketWrapper var1;
      try {
         var1 = this.yY;
      } finally {
         this.lock.unlock();
      }

      return var1;
   }

   protected void setSocket(TcpConnection.SocketWrapper var1) {
      this.yY = var1;
   }

   public static class DefaultExecutorProvider implements TcpConnection.ExecutorProvider {
      @Override
      public Executor get(TcpConnection var1) {
         return StackUtils.getBlockingWorkExecutor();
      }
   }

   public static class DefaultReverseConnectionProvider implements TcpConnection.ReverseConnectionProvider {
      @Override
      public TcpConnection.SocketWrapper provideOpenReverseConnectionSocket(TcpConnection var1) throws IOException {
         final ServerSocket var2 = new ServerSocket();
         var2.bind(var1.addr);
         TcpConnection.logger.info("Opened ServerSocket at:{}, waiting ReverseHello connection", var1.addr);
         if (var1.yp > 0) {
            TimerUtil.getTimer().schedule(new TimerTask() {
               @Override
               public void run() {
                  try {
                     var2.close();
                  } catch (IOException var2x) {
                     TcpConnection.logger.error("Could not close ServerSocket in timeout", var2x);
                  }
               }
            }, var1.yp);
         }

         TcpConnection.DelegatingSocketWrapper var3 = new TcpConnection.DelegatingSocketWrapper(var2.accept());

         try {
            var2.close();
         } catch (IOException var7) {
            try {
               var3.delegate.close();
            } catch (Exception var6) {
               TcpConnection.logger.warn("Closing ReverseHello related ServerSocket failed and also closing the accepted connection failed", var6);
            }

            throw var7;
         }

         TcpConnection.logger.debug("ReverseHello ServerSocket {} closed.", var1.addr);
         return var3;
      }
   }

   public static class DefaultSocketFactory implements TcpConnection.SocketFactory {
      @Override
      public TcpConnection.SocketWrapper createSocket(TcpConnection var1) {
         return new TcpConnection.DelegatingSocketWrapper(new Socket());
      }
   }

   public static class DelegatingSocketWrapper implements TcpConnection.SocketWrapper {
      protected final Socket delegate;

      public DelegatingSocketWrapper(Socket var1) {
         this.delegate = var1;
      }

      @Override
      public void close() throws IOException {
         this.delegate.close();
      }

      @Override
      public void connect(InetSocketAddress var1) throws IOException {
         this.delegate.connect(var1);
      }

      @Override
      public void connect(InetSocketAddress var1, int var2) throws IOException {
         this.delegate.connect(var1, var2);
      }

      @Override
      public InputStream getInputStream() throws IOException {
         return this.delegate.getInputStream();
      }

      @Override
      public SocketAddress getLocalSocketAddress() {
         return this.delegate.getLocalSocketAddress();
      }

      @Override
      public OutputStream getOutputStream() throws IOException {
         return this.delegate.getOutputStream();
      }

      @Override
      public SocketAddress getRemoteSocketAddress() {
         return this.delegate.getRemoteSocketAddress();
      }

      @Override
      public boolean isClosed() {
         return this.delegate.isClosed();
      }

      @Override
      public boolean isConnected() {
         return this.delegate.isConnected();
      }

      @Override
      public void setKeepAlive(boolean var1) throws IOException {
         this.delegate.setKeepAlive(var1);
      }

      @Override
      public void setReceiveBufferSize(int var1) throws IOException {
         this.delegate.setReceiveBufferSize(var1);
      }

      @Override
      public void setSendBufferSize(int var1) throws IOException {
         this.delegate.setSendBufferSize(var1);
      }

      @Override
      public void setSoTimeout(int var1) throws IOException {
         this.delegate.setSoTimeout(var1);
      }

      @Override
      public void setTcpNoDelay(boolean var1) throws IOException {
         this.delegate.setTcpNoDelay(var1);
      }
   }

   public interface ExecutorProvider {
      Executor get(TcpConnection var1);
   }

   public interface ReverseConnectionProvider {
      TcpConnection.SocketWrapper provideOpenReverseConnectionSocket(TcpConnection var1) throws IOException;
   }

   public interface SocketFactory {
      TcpConnection.SocketWrapper createSocket(TcpConnection var1) throws IOException;
   }

   public interface SocketWrapper {
      void close() throws IOException;

      void connect(InetSocketAddress var1) throws IOException;

      void connect(InetSocketAddress var1, int var2) throws IOException;

      InputStream getInputStream() throws IOException;

      SocketAddress getLocalSocketAddress();

      OutputStream getOutputStream() throws IOException;

      SocketAddress getRemoteSocketAddress();

      boolean isClosed();

      boolean isConnected();

      void setKeepAlive(boolean var1) throws IOException;

      void setReceiveBufferSize(int var1) throws IOException;

      void setSendBufferSize(int var1) throws IOException;

      void setSoTimeout(int var1) throws IOException;

      void setTcpNoDelay(boolean var1) throws IOException;
   }

   private class a {
      private ByteBuffer[] zh;
      private ByteBuffer[] zi;

      public a(ByteBuffer[] var2, ByteBuffer[] var3) {
         this.zh = var2;
         this.zi = var3;
      }

      public ByteBuffer[] etz() {
         return this.zh;
      }

      public ByteBuffer[] etA() {
         return this.zi;
      }
   }

   class b extends Thread {
      TcpConnection.SocketWrapper zj;
      EncoderContext ti;
      ServiceResultException lp = null;
      boolean zk = false;

      b(TcpConnection.SocketWrapper var2, EncoderContext var3) {
         super("TcpConnection/Read");
         this.setDaemon(true);
         this.zj = var2;
         this.ti = var3;
      }

      @Override
      public void run() {
         try {
            InputStreamReadable var1 = new InputStreamReadable(new BufferedInputStream(this.zj.getInputStream()), Long.MAX_VALUE);
            var1.order(ByteOrder.LITTLE_ENDIAN);
            ArrayList var30 = new ArrayList(256);

            label191:
            while (this.zj == TcpConnection.this.getSocket()) {
               var30.clear();
               int var3 = 0;
               int var4 = 0;
               int var5 = 0;
               final int var6 = 0;
               final int var7 = 0;

               while (var4 <= TcpConnection.this.yS.maxRecvChunkCount) {
                  int var8 = var1.getInt();
                  int var9 = var8 & 16777215;
                  var5 = var8 & 0xFF000000;
                  if (var4 == 0) {
                     var3 = var9;
                  } else if (var9 != var3) {
                     this.lp = new ServiceResultException("Error, message type changed between chunks");
                     TcpConnection.logger.warn("{} Error, message type changed between chunks", TcpConnection.this.addr);
                     break label191;
                  }

                  if (var9 != 5132367 && var9 != 4674381 && var8 != 1179800133) {
                     this.lp = new ServiceResultException("Error, unknown message type " + String.format(Locale.ROOT, "0x%08x", var8));
                     TcpConnection.logger.warn("{} Error, unknown message type {}", TcpConnection.this.addr, String.format(Locale.ROOT, "0x%08x", var8));
                     break label191;
                  }

                  int var10 = var1.getInt();
                  if (var10 > TcpConnection.this.yS.maxRecvBufferSize) {
                     this.lp = new ServiceResultException("Error, chunk too large (max = " + TcpConnection.this.yS.maxRecvBufferSize + ")");
                     TcpConnection.logger.warn("{} Error, chunk too large (max = {})", TcpConnection.this.addr, TcpConnection.this.yS.maxRecvBufferSize);
                     break label191;
                  }

                  ByteBuffer var11 = ByteBuffer.allocate(var10);
                  var11.order(ByteOrder.LITTLE_ENDIAN);
                  var11.putInt(var8);
                  var11.putInt(var10);
                  var1.get(var11, var10 - 8);
                  if (var8 == 1179800133) {
                     ((Buffer)var11).position(8);
                     BinaryDecoder var38 = new BinaryDecoder(var11);
                     var38.setEncoderContext(this.ti);
                     ErrorMessage var42 = InternalBinaryEncodingsHelper.getUaTcpCommMessage(var38, ErrorMessage.class);
                     ServiceResultException var46 = new ServiceResultException(var42.getError(), var42.getReason());
                     this.lp = var46;
                     TcpConnection.logger.warn(this.zj.getRemoteSocketAddress() + " Error", var46);
                     break label191;
                  }

                  int var12 = ChunkUtils.getSecureChannelId(var11);
                  if (var4 == 0) {
                     var7 = var12;
                  } else if (var7 != var12) {
                     this.lp = new ServiceResultException("Error, SecureChannelId mismatch");
                     TcpConnection.logger.warn("{} Error, SecureChannelId mismatch", TcpConnection.this.addr);
                     break label191;
                  }

                  if (var3 == 5132367) {
                     try {
                        String var13 = ChunkUtils.getSecurityPolicyUri(var11);
                        SecurityPolicy var14 = SecurityPolicy.getSecurityPolicy(var13);
                        byte[] var15 = ChunkUtils.getByteString(var11);
                        byte[] var16 = ChunkUtils.getByteString(var11);
                        if (var14 != TcpConnection.this.securityConfiguration.getSecurityPolicy()) {
                           this.lp = new ServiceResultException("Error, unexpected security policy in OpenSecureChannelResponse");
                           TcpConnection.logger.warn("{} Error, unexpected security policy in OpenSecureChannelResponse", TcpConnection.this.addr);
                           break label191;
                        }

                        if (TcpConnection.this.securityConfiguration.getSecurityPolicy() != SecurityPolicy.NONE
                           && !Arrays.equals(var16, TcpConnection.this.securityConfiguration.getEncodedLocalCertificateThumbprint())) {
                           this.lp = new ServiceResultException("Error, certificate thumbprint mismatch");
                           TcpConnection.logger.warn("{} Error, certificate thumbprint mismatch", TcpConnection.this.addr);
                           break label191;
                        }

                        Cert var17 = null;
                        if (var15 != null && var15.length > 0) {
                           try {
                              var17 = new Cert(CertificateUtils.decodeX509Certificate(var15));
                           } catch (CertificateException var22) {
                              this.lp = new ServiceResultException(StatusCodes.Bad_CertificateInvalid, "Error, Invalid Remote Certificate");
                              TcpConnection.logger.warn(TcpConnection.this.addr + " Error, Invalid Remote Certificate", var22);
                              break label191;
                           }
                        }

                        if (TcpConnection.this.cr != null) {
                           StatusCode var18 = TcpConnection.this.cr.validateCertificate(var17);
                           if (var18 != null && !var18.isGood()) {
                              this.lp = new ServiceResultException(var18, "Remote certificate not accepted");
                              TcpConnection.logger.info("{} Remote certificate not accepted: {}", TcpConnection.this.addr, var18);
                              break label191;
                           }
                        }

                        TcpConnection.this.securityConfiguration = new SecurityConfiguration(
                           TcpConnection.this.securityConfiguration.getSecurityMode(), TcpConnection.this.securityConfiguration.getLocalCertificate2(), var17
                        );
                        ChunkAsymmDecryptVerifier var54 = new ChunkAsymmDecryptVerifier(var11, TcpConnection.this.securityConfiguration);
                        var54.run();
                     } catch (ServiceResultException var23) {
                        this.lp = var23;
                        TcpConnection.logger.warn(TcpConnection.this.addr + "", var23);
                        break label191;
                     }
                  }

                  if (var3 == 4674381) {
                     int var39 = ChunkUtils.getTokenId(var11);
                     SecurityToken var43 = null;
                     TcpConnection.logger.debug("tokens({})={}", TcpConnection.this.yU.size(), TcpConnection.this.yU);

                     for (SecurityToken var51 : TcpConnection.this.yU) {
                        if (var51.getTokenId() == var39 && var51.getSecureChannelId() == var12) {
                           var43 = var51;
                        }
                     }

                     TcpConnection.logger.debug("token={}", var43);
                     if (var43 == null) {
                        this.lp = new ServiceResultException("Unexpected securityTokenId = " + var39);
                        TcpConnection.logger.warn("{} Unexpected securityTokenId = {}", TcpConnection.this.addr, var39);
                        break label191;
                     }

                     if (!var43.isValid()) {
                        this.lp = new ServiceResultException("SecurityToken " + var39 + " has timeouted");
                        TcpConnection.logger.warn("{} SecurityToken {} has timeouted", TcpConnection.this.addr, var43);
                        break label191;
                     }

                     TcpConnection.this.yV.put(var12, var43);
                     ChunkSymmDecryptVerifier var48 = new ChunkSymmDecryptVerifier(var11, var43);
                     var48.run();
                     ((Buffer)var11).position(24);
                  }

                  ((Buffer)var11).position(var11.position() - 8);
                  int var40 = var11.getInt();
                  SequenceNumber var44 = TcpConnection.this.yX.get(var7);
                  if ((var3 == 4674381 || var44 != null) && !var44.testAndSetRecvSequencenumber(var40)) {
                     this.lp = new ServiceResultException("Sequence number mismatch");
                     TcpConnection.logger
                        .warn("{} Sequence number mismatch: {} vs. {}", new Object[]{TcpConnection.this.addr, var44.getRecvSequenceNumber(), var40});
                     break label191;
                  }

                  int var49 = var11.getInt();
                  if (var4 == 0) {
                     var6 = var49;
                  } else if (var49 != var6) {
                     this.lp = new ServiceResultException("Request id mismatch");
                     TcpConnection.logger.warn("{} Request id mismatch", TcpConnection.this.addr);
                     break label191;
                  }

                  var30.add(var11);
                  var4++;
                  if (var5 != 1124073472) {
                     if (var5 != 1090519040) {
                        ByteBufferArrayReadable var32 = new ByteBufferArrayReadable(var30.toArray(new ByteBuffer[var30.size()]));
                        var32.order(ByteOrder.LITTLE_ENDIAN);
                        var9 = (int)var32.getByteQueue().remaining();
                        final byte[] var34 = new byte[var9];
                        var32.getByteQueue().get(var34);
                        BinaryDecoder var35 = new BinaryDecoder(var34);
                        var35.setEncoderContext(this.ti);
                        NodeId var37 = var35.getNodeId(null);
                        NodeId var41 = new NodeId(0, (UnsignedInteger)Ids.OpenSecureChannelResponse_DefaultBinary.getValue());
                        if (var41.equals(var37)) {
                           BinaryDecoder var36 = new BinaryDecoder(var34);
                           var36.setEncoderContext(this.ti);
                           ServiceResponse var45 = InternalBinaryEncodingsHelper.getServiceResponse(var36);
                           OpenSecureChannelResponse var50 = (OpenSecureChannelResponse)var45;
                           ChannelSecurityToken var52 = var50.getSecurityToken();
                           ByteString var53 = TcpConnection.this.yW.get(var6);
                           ByteString var55 = var50.getServerNonce();
                           int var19 = var7;
                           int var20 = var52.getChannelId().intValue();
                           if (var20 != var7) {
                              TcpConnection.logger
                                 .warn(
                                    "{} OpenSecureChannel, server sent two secureChannelIds {} and {} using {}",
                                    new Object[]{TcpConnection.this.addr, var7, var20, var7}
                                 );
                           }

                           try {
                              SecurityToken var21 = new SecurityToken(
                                 TcpConnection.this.securityConfiguration,
                                 var19,
                                 var52.getTokenId().intValue(),
                                 System.currentTimeMillis(),
                                 var52.getRevisedLifetime().longValue(),
                                 var53,
                                 var55
                              );
                              TcpConnection.logger.debug("new token={}", var21);
                              TcpConnection.this.yU.add(var21);
                              if (!TcpConnection.this.yX.containsKey(var19)) {
                                 TcpConnection.this.yX.put(var19, new SequenceNumber());
                              }
                           } catch (ServiceResultException var24) {
                              this.lp = var24;
                              TcpConnection.logger.warn(TcpConnection.this.addr + " SecurityTokenError ", var24);
                              break label191;
                           }
                        }

                        TcpConnection.this.yW.remove(var6);
                        TcpConnection.yQ.get(TcpConnection.this).execute(new Runnable() {
                           @Override
                           public void run() {
                              BinaryDecoder var1 = new BinaryDecoder(var34);
                              var1.setEncoderContext(b.this.ti);
                              AtomicReference var2 = new AtomicReference();
                              var1.setStructureFieldDecodeListener((var1x, var2x) -> {
                                 if (UaIds.ResponseHeader.equals(var1x.getDataTypeId()) && "ResponseHeader".equals(var1x.getName())) {
                                    var2.compareAndSet(null, (ResponseHeader)var2x);
                                 }
                              });

                              Object var3;
                              try {
                                 var3 = InternalBinaryEncodingsHelper.getMessage(var1);
                              } catch (DecodingException var6x) {
                                 ResponseHeader var5 = (ResponseHeader)var2.get();
                                 if (var5 == null) {
                                    TcpConnection.logger.error("Decoding error for Message", var6x);
                                    return;
                                 }

                                 var3 = new InternalClientSideDecodingServiceFault(var5, var6x);
                              }

                              for (IConnection.IMessageListener var7x : TcpConnection.this.listeners) {
                                 var7x.onMessage(var6, var7, (IEncodeable)var3);
                              }
                           }
                        });
                     }
                     continue label191;
                  }
               }

               this.lp = new ServiceResultException("Recv chunk count exceeded (max = " + var4 + ")");
               TcpConnection.logger.warn("{} Recv chunk count exceeded (max = {})", TcpConnection.this.addr, var4);
               break;
            }
         } catch (IOException var25) {
            if (var25 instanceof SocketException) {
               if (!this.zk) {
                  TcpConnection.logger.info("{} Closed (unexpected)", TcpConnection.this.addr);
                  this.lp = new ServiceResultException(StatusCodes.Bad_ConnectionClosed, var25, "Connection closed (unexpected)");
               } else {
                  TcpConnection.logger.info("{} Closed (expected)", TcpConnection.this.addr);
                  this.lp = new ServiceResultException(StatusCodes.Bad_ConnectionClosed, var25, "Connection closed (expected)");
               }
            } else if (var25 instanceof EOFException) {
               this.lp = new ServiceResultException(StatusCodes.Bad_ConnectionClosed, var25, "Connection closed (graceful)");
               TcpConnection.logger.info("{} Closed (graceful)", TcpConnection.this.addr);
            } else {
               this.lp = StackUtils.toServiceResultException(var25);
               TcpConnection.logger.warn(TcpConnection.this.addr + " Error", var25);
            }
         } catch (DecodingException var26) {
            if (var26.getCause() != null && var26.getCause() instanceof EOFException) {
               TcpConnection.logger.info("{} Closed", TcpConnection.this.addr);
            } else {
               TcpConnection.logger.warn(TcpConnection.this.addr + " Error", var26);
            }

            this.lp = var26;
         } catch (RuntimeServiceResultException var27) {
            ServiceResultException var2 = var27.getCause();
            TcpConnection.logger.warn(TcpConnection.this.addr + " Error", var2);
            this.lp = var2;
         } catch (Exception var28) {
            this.lp = new ServiceResultException(StatusCodes.Bad_InternalError, var28);
            TcpConnection.logger.error("Error in ReadThread", this.lp);
         } catch (StackOverflowError var29) {
            this.lp = new ServiceResultException(StatusCodes.Bad_DecodingError, var29);
            TcpConnection.logger.error("Error in ReadThread", this.lp);
         }

         TcpConnection.this.a(this.lp);
      }
   }
}
