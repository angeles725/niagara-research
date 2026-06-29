package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.common.RuntimeServiceResultException;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.transport.security.CryptoProvider;
import com.prosysopc.ua.stack.transport.security.SecurityAlgorithm;
import com.prosysopc.ua.stack.transport.security.SecurityConfiguration;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkAsymmEncryptSigner implements Runnable {
   static Logger logger = LoggerFactory.getLogger(ChunkAsymmEncryptSigner.class);
   ByteBuffer xK;
   ByteBuffer xO;
   SecurityConfiguration xP;
   private int signatureSize;

   public ChunkAsymmEncryptSigner(ByteBuffer var1, ByteBuffer var2, SecurityConfiguration var3) {
      this.xK = var1;
      this.xO = var2;
      this.xP = var3;
   }

   @Override
   public void run() throws RuntimeServiceResultException {
      try {
         int var1 = this.xO.limit();
         MessageSecurityMode var2 = this.xP.getMessageSecurityMode();
         if (var2 == MessageSecurityMode.Sign) {
            var2 = MessageSecurityMode.SignAndEncrypt;
         }

         SecurityPolicy var3 = this.xP.getSecurityPolicy();
         byte var4 = 8;
         SecurityAlgorithm var5 = var3.getAsymmetricSignatureAlgorithm();
         boolean var6 = MessageSecurityMode.Sign == var2 || MessageSecurityMode.SignAndEncrypt == var2;
         this.signatureSize = var6 ? CryptoUtil.getSignatureSize(var5, this.xP.getLocalPrivateKey()) : 0;
         logger.debug("SecurityMode in asymm enc: {}", var2.getValue());
         int var7 = 0;
         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            int var8 = this.xP.getRemoteCertificate2().getKeySize();
            logger.trace("keySize={}", var8);
            var7 = this.t(var8);
            logger.trace("padding={}", var7);
         }

         if (var2 == MessageSecurityMode.Sign || var2 == MessageSecurityMode.SignAndEncrypt) {
            byte[] var11 = new byte[this.xO.arrayOffset() + var1 + var7];
            ((Buffer)this.xK).rewind();
            this.xK.get(var11, 0, var11.length);
            byte[] var9 = this.a(var11, this.xP.getLocalPrivateKey());
            this.xK.put(var9);
         }

         if (logger.isTraceEnabled()) {
            logger.trace("getPaddingSize: chunk={}", CryptoUtil.toHex(this.xK.array(), 64));
         }

         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            byte[] var12 = new byte[var4 + var1 + var7 + this.signatureSize];
            ((Buffer)this.xK).position(this.xO.arrayOffset() - var4);
            this.xK.get(var12, 0, var12.length);
            this.a(var12, this.xP.getRemoteCertificate().getPublicKey(), this.xK.array(), this.xO.arrayOffset() - var4);
         }

         ((Buffer)this.xK).position(this.xO.arrayOffset());
      } catch (ServiceResultException var10) {
         throw new RuntimeServiceResultException(var10);
      }
   }

   private void a(byte[] var1, PublicKey var2, byte[] var3, int var4) throws ServiceResultException {
      SecurityPolicy var5 = this.xP.getSecurityPolicy();
      logger.debug("rsa_Encrypt: policy={}", var5);
      int var6 = 1;
      PublicKey var7 = this.xP.getRemoteCertificate().getPublicKey();
      var6 = CryptoUtil.getPlainTextBlockSize(var5.getAsymmetricEncryptionAlgorithm(), var7);
      logger.debug("encrypt: inputBlockSize={}", var6);
      if (var1.length % var6 != 0) {
         logger.error("Wrong block size in asym encryption: length={} inputBlockSize={}", var1.length, var6);
         throw new ServiceResultException(StatusCodes.Bad_InternalError, "Error in asymmetric encrypt: Input data is not an even number of encryption blocks.");
      } else {
         CryptoProvider var8 = CryptoUtil.getCryptoProvider();
         var8.encryptAsymm(var2, this.xP.getSecurityPolicy().getAsymmetricEncryptionAlgorithm(), var1, var3, var4);
         if (logger.isTraceEnabled()) {
            logger.trace("encrypt: dataToEncrypt={}", CryptoUtil.toHex(var1, 64));
            logger.trace("encrypt: output={}", CryptoUtil.toHex(var3, 64));
         }
      }
   }

   private int t(int var1) {
      int var2 = this.xK.limit() - 1;
      if (logger.isTraceEnabled()) {
         logger.trace("getPaddingSize: chunk={}", CryptoUtil.toHex(this.xK.array(), 64));
         logger.trace("getPaddingSize: plaintext={}", CryptoUtil.toHex(this.xO.array(), 64));
         logger.trace("getPaddingSize: plaintext.arrayOffset()={}", this.xO.arrayOffset());
         logger.trace("getPaddingSize: plaintext.limit()={}", this.xO.limit());
         logger.trace("getPaddingSize: lastPaddingBytePosition={}", var2);
      }

      if (var1 > 2048) {
         int var5 = this.xK.get(var2) & 255;
         int var4 = this.xK.get(var2 - 1) & 255;
         logger.trace("getPaddingSize: paddingByte={}", var4);
         logger.trace("getPaddingSize: extraPaddingByte={}", var5);
         logger.trace("getPaddingSize: padding={}", var4 | var5 << 8);
         return (var4 & 0xFF | (var5 & 0xFF) << 8) + 2;
      } else {
         int var3 = this.xK.get(var2) & 255;
         return (var3 & 0xFF) + 1;
      }
   }

   private byte[] a(byte[] var1, RSAPrivateKey var2) throws ServiceResultException {
      SecurityPolicy var3 = this.xP.getSecurityPolicy();
      if (var3 == SecurityPolicy.NONE) {
         return null;
      } else {
         byte[] var4 = CryptoUtil.getCryptoProvider().signAsymm(var2, this.xP.getSecurityPolicy().getAsymmetricSignatureAlgorithm(), var1);
         if (logger.isTraceEnabled()) {
            logger.trace("sign: dataToSign={}", CryptoUtil.toHex(var1, 64));
            logger.trace("sign: signature={}", CryptoUtil.toHex(var4, 64));
         }

         return var4;
      }
   }
}
