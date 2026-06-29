package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.common.RuntimeServiceResultException;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChunkSymmEncryptSigner implements Runnable {
   static Logger logger = LoggerFactory.getLogger(ChunkSymmEncryptSigner.class);
   ByteBuffer xK;
   ByteBuffer xV;
   SecurityToken xU;

   public ChunkSymmEncryptSigner(ByteBuffer var1, ByteBuffer var2, SecurityToken var3) {
      this.xK = var1;
      this.xV = var2;
      this.xU = var3;
   }

   @Override
   public void run() throws RuntimeServiceResultException {
      SecurityPolicy var1 = this.xU.getSecurityPolicy();
      MessageSecurityMode var2 = this.xU.getMessageSecurityMode();

      try {
         int var3 = this.xK.limit();
         int var4 = this.xV.limit();
         byte var5 = 8;
         byte var6 = 8;
         byte var7 = 8;
         int var8 = var1.getSymmetricSignatureSize();
         if (var2 == MessageSecurityMode.Sign || var2 == MessageSecurityMode.SignAndEncrypt) {
            int var9 = var3 - var8;
            byte[] var10 = new byte[var8];
            this.a(this.xU, this.xK.array(), var9, var10);
            ((Buffer)this.xK).position(var3 - var8);
            this.xK.put(var10, 0, var8);
            if (logger.isTraceEnabled()) {
               logger.trace("signature={}", CryptoUtil.toHex(var10));
            }
         }

         int var12 = 0;
         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            ((Buffer)this.xK).position(var3 - var8 - 1);
            var12 = this.xK.get() + 1;
         }

         ((Buffer)this.xK).position(4);
         this.xK.putInt(var3);
         if (var2 == MessageSecurityMode.SignAndEncrypt) {
            byte[] var13 = new byte[var5 + var4 + var12 + var8];
            ((Buffer)this.xK).position(var6 + var7);
            this.xK.get(var13);
            this.b(this.xU, var13, 0, var13.length, this.xK.array(), var6 + var7);
         }
      } catch (ServiceResultException var11) {
         throw new RuntimeServiceResultException(var11);
      }
   }

   private int b(SecurityToken var1, byte[] var2, int var3, int var4, byte[] var5, int var6) throws ServiceResultException {
      return CryptoUtil.getCryptoProvider()
         .encryptSymm(var1.getSecurityPolicy(), var1.getLocalEncryptingKey(), var1.getLocalInitializationVector(), var2, var3, var4, var5, var6);
   }

   private void a(SecurityToken var1, byte[] var2, int var3, byte[] var4) throws ServiceResultException {
      CryptoUtil.getCryptoProvider().signSymm(var1.getSecurityPolicy(), var1.getLocalSigningKey(), var2, 0, var3, var4, 0);
   }
}
