package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.common.RuntimeServiceResultException;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.transport.security.SecurityConfiguration;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import com.prosysopc.ua.stack.utils.bytebuffer.ByteBufferFactory;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkFactory extends ByteBufferFactory {
   static Logger logger = LoggerFactory.getLogger(ChunkFactory.class);
   public int maxChunkSize;
   public int maxPlaintextSize;
   public int messageHeaderSize;
   public int securityHeader;
   public int sequenceHeader;
   public int cipherBlockSize;
   public int signatureSize;
   public MessageSecurityMode securityMode;
   private boolean xQ;

   public ChunkFactory(int var1, int var2, int var3, int var4, int var5, int var6, MessageSecurityMode var7, int var8) {
      logger.trace("ChunkFactory: class={}", this.getClass());
      this.maxChunkSize = var1;
      this.messageHeaderSize = var2;
      this.securityHeader = var3;
      this.sequenceHeader = var4;
      this.cipherBlockSize = var6;
      this.signatureSize = var5;
      this.securityMode = var7;
      this.xQ = var8 > 2048;
      if (var7 == MessageSecurityMode.None) {
         this.maxPlaintextSize = var1 - var2 - var3 - this.sequenceHeader;
      } else if (var7 == MessageSecurityMode.Sign) {
         this.maxPlaintextSize = var1 - var2 - var3 - this.sequenceHeader - var5;
      }

      if (var7 == MessageSecurityMode.SignAndEncrypt) {
         int var9 = this.getMinimumPadding();
         int var10 = var1 - var2 - var3 - var9;
         var10 -= var10 % var6;
         this.maxPlaintextSize = var10 - var4 - var5 - var9;
      }
   }

   public ByteBuffer allocate(int var1) {
      var1 = Math.min(var1, this.maxPlaintextSize);
      int var2 = 0;
      if (this.securityMode == MessageSecurityMode.SignAndEncrypt) {
         int var3 = var1 + this.sequenceHeader + this.signatureSize;
         var2 = this.getMinimumPadding();
         int var4 = (var2 + var3) % this.cipherBlockSize;
         if (var4 != 0) {
            var2 += this.cipherBlockSize - var4;
         }

         logger.trace("allocate: padding={}", var2);
      }

      int var6 = var1 + this.messageHeaderSize + this.securityHeader + this.sequenceHeader + this.signatureSize + var2;
      logger.trace("allocate: chunkSize={}", var6);
      ByteBuffer var7 = ByteBuffer.allocate(var6);
      var7.order(ByteOrder.LITTLE_ENDIAN);
      ((Buffer)var7).position(4);
      var7.putInt(var6);
      if (this.securityMode == MessageSecurityMode.SignAndEncrypt) {
         this.writePadding(this.messageHeaderSize + this.securityHeader + this.sequenceHeader + var1, var2, var7);
      }

      ((Buffer)var7).position(this.messageHeaderSize + this.securityHeader + this.sequenceHeader);
      var7 = var7.slice();
      var7.order(ByteOrder.LITTLE_ENDIAN);
      ((Buffer)var7).limit(var1);
      return var7;
   }

   public void encryptChunk(ByteBuffer var1) {
   }

   public ByteBuffer expandToCompleteChunk(ByteBuffer var1) {
      return ByteBuffer.wrap(var1.array()).order(ByteOrder.LITTLE_ENDIAN);
   }

   public ByteBuffer[] expandToCompleteChunk(ByteBuffer[] var1) {
      ByteBuffer[] var2 = new ByteBuffer[var1.length];

      for (int var3 = 0; var3 < var2.length; var3++) {
         var2[var3] = this.expandToCompleteChunk(var1[var3]);
      }

      return var2;
   }

   public void signChunk(ByteBuffer var1) {
   }

   protected int getMinimumPadding() {
      return this.xQ ? 2 : 1;
   }

   protected void writePadding(int var1, int var2, ByteBuffer var3) {
      int var4 = this.getMinimumPadding();
      logger.trace("writePadding: result.position={}", var3.position());
      logger.trace("writePadding: minimumPadding={}", var4);
      logger.trace("writePadding: padding={}", var2);
      if (var4 == 1) {
         ((Buffer)var3).position(var1);
         byte var5 = (byte)(var2 - 1 & 0xFF);

         for (int var6 = 0; var6 < var2; var6++) {
            var3.put(var5);
         }
      }

      if (var4 == 2) {
         ((Buffer)var3).position(var1);
         byte var7 = (byte)(var2 - 2 & 0xFF);

         for (int var8 = 0; var8 < var2 - 1; var8++) {
            var3.put(var7);
         }

         var3.put((byte)(var2 - 2 >> 8));
      }

      if (logger.isTraceEnabled()) {
         logger.trace("writePadding: result={}", CryptoUtil.toHex(var3.array(), 64));
      }
   }

   protected void writePaddingSize(int var1, int var2, ByteBuffer var3) {
      int var4 = this.getMinimumPadding();
      ((Buffer)var3).position(var1);
      if (var4 == 1) {
         var3.put((byte)(var2 - 1 & 0xFF));
      }

      if (var4 == 2) {
         var3.put((byte)(var2 - 2 & 0xFF));
         var3.put((byte)(var2 - 2 >> 8));
      }

      if (logger.isTraceEnabled()) {
         logger.trace("writePadding: result={}", CryptoUtil.toHex(var3.array(), 64));
      }
   }

   public static class AcknowledgeChunkFactory extends ChunkFactory {
      public AcknowledgeChunkFactory() {
         super(8192, 8, 0, 0, 0, 1, MessageSecurityMode.None, 0);
         this.maxChunkSize = 8192;
         this.messageHeaderSize = 8;
         this.maxPlaintextSize = this.maxChunkSize - 8;
      }
   }

   public static class AsymmMsgChunkFactory extends ChunkFactory {
      SecurityConfiguration xP;

      public AsymmMsgChunkFactory(int var1, SecurityConfiguration var2) throws ServiceResultException {
         super(
            var1,
            12,
            12
               + var2.getSecurityPolicy().getEncodedPolicyUri().length
               + (var2.getEncodedLocalCertificate() != null ? var2.getEncodedLocalCertificate().length : 0)
               + (var2.getEncodedRemoteCertificateThumbprint() != null ? var2.getEncodedRemoteCertificateThumbprint().length : 0),
            8,
            var2.getMessageSecurityMode() != MessageSecurityMode.None
               ? CryptoUtil.getSignatureSize(var2.getSecurityPolicy().getAsymmetricSignatureAlgorithm(), var2.getLocalPrivateKey())
               : 0,
            var2.getMessageSecurityMode() != MessageSecurityMode.None
               ? CryptoUtil.getCipherBlockSize(var2.getSecurityPolicy().getAsymmetricEncryptionAlgorithm(), var2.getRemoteCertificate().getPublicKey())
               : 1,
            var2.getMessageSecurityMode(),
            var2.getLocalCertificate() == null ? 0 : ((RSAPublicKey)var2.getRemoteCertificate().getPublicKey()).getModulus().bitLength()
         );
         this.xP = var2;
      }

      @Override
      public ByteBuffer allocate(int var1) {
         MessageSecurityMode var2 = this.securityMode;
         if (var2 == MessageSecurityMode.Sign) {
            var2 = MessageSecurityMode.SignAndEncrypt;
         }

         var1 = Math.min(var1, this.maxPlaintextSize);
         int var3 = -1;
         int var4 = -1;
         int var5 = var1 + this.sequenceHeader;
         int var6 = 0;
         int var7 = this.getMinimumPadding();
         int var8 = 0;
         int var9 = -1;
         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            int var10 = 1;

            try {
               PublicKey var11 = this.xP.getReceiverCertificate().getPublicKey();
               var10 = CryptoUtil.getPlainTextBlockSize(this.xP.getSecurityPolicy().getAsymmetricEncryptionAlgorithm(), var11);
            } catch (ServiceResultException var12) {
               throw new RuntimeServiceResultException(var12);
            }

            var5 += this.signatureSize;
            var5 += var7;
            if (var5 % var10 != 0) {
               var8 = var10 - var5 % var10;
               var5 += var8;
            }

            var6 = var7 + var8;
            var3 = var5 / var10;
            var4 = var3 * this.cipherBlockSize;
            var9 = this.messageHeaderSize + this.securityHeader + var4;
         } else if (var2 == MessageSecurityMode.Sign) {
            var9 = this.messageHeaderSize + this.securityHeader + var5 + this.signatureSize;
         } else if (var2 == MessageSecurityMode.None) {
            var9 = this.messageHeaderSize + this.securityHeader + var5;
         }

         logger.trace("AsymmMSGChunkFactory.allocate: chunkSize={}", var9);
         ByteBuffer var19 = ByteBuffer.allocate(var9);
         var19.order(ByteOrder.LITTLE_ENDIAN);
         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            this.writePadding(this.messageHeaderSize + this.securityHeader + this.sequenceHeader + var1, var6, var19);
            this.writePaddingSize(var9 - var7, var6, var19);
         }

         ((Buffer)var19).position(4);
         var19.putInt(var9);
         ((Buffer)var19).position(this.messageHeaderSize + this.securityHeader + this.sequenceHeader);
         var19 = var19.slice();
         var19.order(ByteOrder.LITTLE_ENDIAN);
         ((Buffer)var19).limit(var1);
         return var19;
      }
   }

   public static class ErrorMessageChunkFactory extends ChunkFactory {
      public ErrorMessageChunkFactory() {
         super(4100, 8, 0, 0, 0, 1, MessageSecurityMode.None, 0);
         this.maxChunkSize = 4108;
         this.messageHeaderSize = 8;
         this.maxPlaintextSize = 4092;
      }
   }

   public static class HelloChunkFactory extends ChunkFactory {
      public HelloChunkFactory() {
         super(8192, 8, 0, 0, 0, 0, MessageSecurityMode.None, 0);
         this.maxChunkSize = 8192;
         this.messageHeaderSize = 8;
         this.maxPlaintextSize = this.maxChunkSize - 8;
      }
   }
}
