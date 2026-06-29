package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.common.RuntimeServiceResultException;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkSymmDecryptVerifier implements Runnable {
   static Logger logger = LoggerFactory.getLogger(ChunkSymmDecryptVerifier.class);
   static final int xR = 8;
   static final int messageHeaderSize = 8;
   static final int xS = 8;
   static final int xT = 16;
   ByteBuffer xK;
   SecurityToken xU;

   public ChunkSymmDecryptVerifier(ByteBuffer var1, SecurityToken var2) {
      this.xK = var1;
      this.xU = var2;
   }

   @Override
   public void run() throws RuntimeServiceResultException {
      SecurityPolicy var1 = this.xU.getSecurityPolicy();
      MessageSecurityMode var2 = this.xU.getMessageSecurityMode();
      int var3 = this.xK.limit();

      try {
         int var4 = ChunkUtils.getTokenId(this.xK);
         if (var4 != this.xU.getTokenId()) {
            throw new ServiceResultException(StatusCodes.Bad_UnexpectedError);
         } else {
            ServiceResultException var6 = null;
            int var5;
            if (var2 == MessageSecurityMode.SignAndEncrypt) {
               ((Buffer)this.xK).position(16);
               byte[] var7 = new byte[this.xK.limit() - 16];
               this.xK.get(var7, 0, var7.length);

               try {
                  var5 = this.a(this.xU, var7, 0, var7.length, this.xK.array(), 16 + this.xK.arrayOffset());
               } catch (ServiceResultException var14) {
                  var6 = var14;
                  var5 = var7.length;
               }
            } else {
               var5 = this.xK.limit() - 16;
            }

            int var18 = 0;
            int var8 = var1.getSymmetricSignatureSize();
            ServiceResultException var9 = null;
            if (var2 == MessageSecurityMode.Sign || var2 == MessageSecurityMode.SignAndEncrypt) {
               byte[] var10 = new byte[var8];
               ((Buffer)this.xK).position(var3 - var8);
               this.xK.get(var10);
               ((Buffer)this.xK).position(0);
               byte[] var11 = new byte[16 + var5 - var8];
               this.xK.get(var11, 0, var11.length);

               try {
                  this.a(this.xU, var11, var10);
               } catch (ServiceResultException var13) {
                  var9 = var13;
               }
            }

            ServiceResultException var19 = null;
            if (var2 == MessageSecurityMode.SignAndEncrypt) {
               int var20 = 0;
               var20 = 16 + var5 - var8 - 1;
               var18 = this.xK.get(var20);

               for (int var12 = var20 - var18; var12 < var20; var12++) {
                  if (this.xK.get(var12) != var18) {
                     logger.error("Padding does not match");
                     var19 = new ServiceResultException(StatusCodes.Bad_SecurityChecksFailed, "Could not verify the padding in the message");
                  }
               }

               var18++;
            }

            ((Buffer)this.xK).position(24);
            ((Buffer)this.xK).limit(this.xK.position() + var5 - 8 - var18 - var8);
            int var22 = this.xK.limit() - 8 - 8 - 8 - var8 - var18;
            if (var22 < 0) {
            }

            if (var6 != null) {
               throw var6;
            } else if (var9 != null) {
               throw var9;
            } else if (var19 != null) {
               throw var19;
            }
         }
      } catch (ServiceResultException var15) {
         this.u(100);
         throw new RuntimeServiceResultException(var15);
      } catch (Exception var16) {
         this.u(100);
         throw new RuntimeServiceResultException(new ServiceResultException(StatusCodes.Bad_SecurityChecksFailed, var16));
      }
   }

   private int a(SecurityToken var1, byte[] var2, int var3, int var4, byte[] var5, int var6) throws ServiceResultException {
      logger.debug(
         "decrypt: dataToDecrypt.length={} inputOffset={} inputLength={} output.length={} outputOffset={}",
         new Object[]{var2.length, var3, var4, var5.length, var6}
      );
      return CryptoUtil.getCryptoProvider()
         .decryptSymm(var1.getSecurityPolicy(), var1.getRemoteEncryptingKey(), var1.getRemoteInitializationVector(), var2, var3, var4, var5, var6);
   }

   private void u(int var1) {
      int var2 = 1 + CryptoUtil.getRandom().nextInt(var1);

      try {
         Thread.sleep(var2);
      } catch (InterruptedException var4) {
         Thread.currentThread().interrupt();
      }
   }

   private void a(SecurityToken var1, byte[] var2, byte[] var3) throws ServiceResultException {
      CryptoUtil.getCryptoProvider().verifySymm(var1.getSecurityPolicy(), var1.getRemoteSigningKey(), var2, 0, var2.length, var3);
   }
}
