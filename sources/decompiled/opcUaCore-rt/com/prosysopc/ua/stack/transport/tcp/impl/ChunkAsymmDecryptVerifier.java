package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.common.RuntimeServiceResultException;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.transport.security.SecurityAlgorithm;
import com.prosysopc.ua.stack.transport.security.SecurityConfiguration;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkAsymmDecryptVerifier implements Runnable {
   static Logger logger = LoggerFactory.getLogger(ChunkAsymmDecryptVerifier.class);
   ByteBuffer xK;
   SecurityConfiguration xL;
   String vj;
   byte[] xM;
   byte[] xN;

   public ChunkAsymmDecryptVerifier(ByteBuffer var1, SecurityConfiguration var2) {
      this.xK = var1;
      this.xL = var2;
   }

   public byte[] getReceiverCertificateThumbprint() {
      return this.xN;
   }

   public String getSecurityPolicyUri() {
      return this.vj;
   }

   public byte[] getSenderCertificate() {
      return this.xM;
   }

   @Override
   public void run() throws RuntimeServiceResultException {
      try {
         SecurityPolicy var1 = this.xL.getSecurityPolicy();
         MessageSecurityMode var2 = this.xL.getMessageSecurityMode();
         if (var2 == MessageSecurityMode.Sign) {
            var2 = MessageSecurityMode.SignAndEncrypt;
         }

         ((Buffer)this.xK).position(12);
         this.vj = ChunkUtils.getString(this.xK);
         logger.debug("SecurityPolicy in use: {}", this.vj);
         logger.debug("SecurityMode in use: {}", this.xL.getMessageSecurityMode());
         if (logger.isTraceEnabled()) {
            logger.trace("Chunk: {}", CryptoUtil.toHex(this.xK.array(), 64));
         }

         this.xM = ChunkUtils.getByteString(this.xK);
         this.xN = ChunkUtils.getByteString(this.xK);
         int var3 = this.xK.position();
         int var4 = this.xK.position() + 8;
         int var5 = this.xK.limit();
         int var6 = var5 - var3;
         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            byte[] var7 = new byte[var6];
            ((Buffer)this.xK).position(var3);
            this.xK.get(var7, 0, var7.length);
            var6 = this.a(var7, this.xL.getLocalPrivateKey(), this.xK.array(), var3 + this.xK.arrayOffset());
            if (logger.isTraceEnabled()) {
               logger.trace("Chunk decrypted: {}", CryptoUtil.toHex(this.xK.array(), 64));
            }
         }

         int var18 = 0;
         if (MessageSecurityMode.Sign == var2 || MessageSecurityMode.SignAndEncrypt == var2) {
            SecurityAlgorithm var8 = var1.getAsymmetricSignatureAlgorithm();
            logger.debug("signatureAlgorithm={}", var8);
            PublicKey var9 = this.xL.getRemoteCertificate().getPublicKey();
            var18 = CryptoUtil.getSignatureSize(var8, var9);
            logger.debug("signatureSize={}", var18);
            byte[] var10 = new byte[var3 + var6 - var18];
            ((Buffer)this.xK).position(0);
            this.xK.get(var10, 0, var10.length);
            ((Buffer)this.xK).position(var3 + var6 - var18);
            byte[] var11 = new byte[var18];
            this.xK.get(var11, 0, var18);
            Certificate var12 = this.xL.getRemoteCertificate();
            if (!this.a(var10, var12, var11)) {
               logger.error("Signature verification fails.");
               throw new ServiceResultException(StatusCodes.Bad_SecurityChecksFailed, "Signature could not be VERIFIED");
            }
         }

         int var19 = 0;
         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            int var21 = this.xL.getLocalCertificate2().getCertificate().getKeySize();
            int var22 = var3 + var6 - var18 - 1;
            boolean var23 = var21 > 2048;
            byte var20;
            if (var23) {
               var20 = this.xK.get(var22 - 1);
               byte var13 = this.xK.get(var22);
               var19 = (var20 & 255 | (var13 & 255) << 8) + 2;
            } else {
               var20 = this.xK.get(var22);
               var19 = (var20 & 255) + 1;
            }

            logger.debug("paddingEnd={} paddingSize={}", var22, var19);
            int var24 = var23 ? var19 - 1 : var19;
            int var14 = var23 ? var22 - var24 : var22 - var24 + 1;

            for (int var15 = 0; var15 < var24; var15++) {
               byte var16 = this.xK.get(var14 + var15);
               if (var16 != var20) {
                  logger.error(String.format(Locale.ROOT, "Padding does not match: %x <> %x", Integer.valueOf(var16), Integer.valueOf(var20)));
                  throw new ServiceResultException(StatusCodes.Bad_SecurityChecksFailed, "Could not verify the padding in the message");
               }
            }
         }

         ((Buffer)this.xK).position(var4);
         ((Buffer)this.xK).limit(this.xK.position() + var6 - 8 - var19 - var18);
      } catch (ServiceResultException var17) {
         throw new RuntimeServiceResultException(var17);
      }
   }

   private int a(byte[] var1, PrivateKey var2, byte[] var3, int var4) throws ServiceResultException {
      int var5 = CryptoUtil.getCryptoProvider().decryptAsymm(var2, this.xL.getSecurityPolicy().getAsymmetricEncryptionAlgorithm(), var1, var3, var4);
      if (logger.isTraceEnabled()) {
         logger.trace("decrypt: dataToDecrypt={}", CryptoUtil.toHex(var1, 64));
         logger.trace("decrypt: output={}", CryptoUtil.toHex(var3, 64));
         logger.trace("decrypt: bytesDecrypted={}", var5);
      }

      return var5;
   }

   private boolean a(byte[] var1, Certificate var2, byte[] var3) throws ServiceResultException {
      SecurityPolicy var4 = this.xL.getSecurityPolicy();
      logger.debug("verify: policy={}", var4);
      if (logger.isTraceEnabled()) {
         logger.trace("verify: {}", var2);
         logger.trace("verify: dataToVerify={}", CryptoUtil.toHex(var1, 64));
         logger.trace("verify: signature={}", CryptoUtil.toHex(var3, 64));
      }

      return CryptoUtil.getCryptoProvider().verifyAsymm(var2.getPublicKey(), this.xL.getSecurityPolicy().getAsymmetricSignatureAlgorithm(), var1, var3);
   }
}
