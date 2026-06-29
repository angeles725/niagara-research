package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.transport.security.SecurityConfiguration;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.crypto.Mac;

public class SecurityToken {
   private SecurityConfiguration securityConfiguration;
   private int ya;
   private int uX;
   private long yb;
   private long yc;
   private ByteString yd;
   private ByteString ye;
   private byte[] yf;
   private byte[] yg;
   private byte[] yh;
   private byte[] yi;
   private byte[] yj;
   private byte[] yk;

   public SecurityToken(SecurityConfiguration var1, int var2, int var3, long var4, long var6, ByteString var8, ByteString var9) throws ServiceResultException {
      if (var1 == null) {
         throw new IllegalArgumentException("null arg");
      } else {
         this.uX = var2;
         this.securityConfiguration = var1;
         this.ya = var3;
         this.yc = var6;
         this.yb = var4;
         this.yd = var8;
         this.ye = var9;
         boolean var10 = var1.getMessageSecurityMode() == MessageSecurityMode.None;
         int var11 = this.getSecurityPolicy().getSignatureKeySize();
         int var12 = this.getSecurityPolicy().getEncryptionKeySize();
         int var13 = this.getSecurityPolicy().getEncryptionBlockSize();
         this.yf = var10 ? null : this.a(this.getRemoteNonce(), null, this.getLocalNonce(), 0, var11);
         this.yg = var10 ? null : this.a(this.getRemoteNonce(), null, this.getLocalNonce(), var11, var12);
         this.yh = var10 ? null : this.a(this.getRemoteNonce(), null, this.getLocalNonce(), var11 + var12, var13);
         this.yi = var10 ? null : this.a(this.getLocalNonce(), null, this.getRemoteNonce(), 0, var11);
         this.yj = var10 ? null : this.a(this.getLocalNonce(), null, this.getRemoteNonce(), var11, var12);
         this.yk = var10 ? null : this.a(this.getLocalNonce(), null, this.getRemoteNonce(), var11 + var12, var13);
      }
   }

   public Mac createLocalHmac() throws ServiceResultException {
      return this.createHmac(this.getLocalSigningKey());
   }

   public Mac createRemoteHmac() throws ServiceResultException {
      return this.createHmac(this.getRemoteSigningKey());
   }

   public long getCreationTime() {
      return this.yb;
   }

   public long getLifeTime() {
      return this.yc;
   }

   public byte[] getLocalEncryptingKey() {
      return this.yg;
   }

   public byte[] getLocalInitializationVector() {
      return this.yh;
   }

   public ByteString getLocalNonce() {
      return this.yd;
   }

   public byte[] getLocalSigningKey() {
      return this.yf;
   }

   public MessageSecurityMode getMessageSecurityMode() {
      return this.securityConfiguration.getMessageSecurityMode();
   }

   public byte[] getRemoteEncryptingKey() {
      return this.yj;
   }

   public byte[] getRemoteInitializationVector() {
      return this.yk;
   }

   public ByteString getRemoteNonce() {
      return this.ye;
   }

   public byte[] getRemoteSigningKey() {
      return this.yi;
   }

   public long getRenewTime() {
      return this.yb + this.yc * 3L / 4L;
   }

   public int getSecureChannelId() {
      return this.uX;
   }

   public SecurityConfiguration getSecurityConfiguration() {
      return this.securityConfiguration;
   }

   public SecurityPolicy getSecurityPolicy() {
      return this.securityConfiguration.getSecurityPolicy();
   }

   public int getTokenId() {
      return this.ya;
   }

   public boolean isExpired() {
      return System.currentTimeMillis() >= this.yb + this.yc;
   }

   public boolean isTimeToRenew() {
      return this.yb + this.yc * 3L / 4L < System.currentTimeMillis();
   }

   public boolean isValid() {
      return System.currentTimeMillis() < this.yb + this.yc + this.yc / 4L;
   }

   public void setLocalEncryptingKey(byte[] var1) {
      this.yg = var1;
   }

   public void setLocalInitializationVector(byte[] var1) {
      this.yh = var1;
   }

   public void setLocalSigningKey(byte[] var1) {
      this.yf = var1;
   }

   public void setRemoteEncryptingKey(byte[] var1) {
      this.yj = var1;
   }

   public void setRemoteInitializationVector(byte[] var1) {
      this.yk = var1;
   }

   public void setRemoteSigningKey(byte[] var1) {
      this.yi = var1;
   }

   @Override
   public String toString() {
      return "SecurityToken(Id=" + this.ya + ", secureChannelId=" + this.uX + ", creationTime=" + Instant.ofEpochMilli(this.yb) + ", lifetime=" + this.yc + ")";
   }

   private byte[] a(ByteString var1, String var2, ByteString var3, int var4, int var5) throws ServiceResultException {
      if (var1 == null) {
         throw new IllegalArgumentException("ArgumentNullException: secret");
      } else if (var4 < 0) {
         throw new IllegalArgumentException("ArgumentOutOfRangeException: offset");
      } else if (var5 < 0) {
         throw new IllegalArgumentException("ArgumentOutOfRangeException: offset");
      } else {
         byte[] var6 = var2 != null && !var2.isEmpty() ? var2.getBytes(StandardCharsets.UTF_8) : null;
         if (var3 != null && var3.getLength() > 0) {
            if (var6 != null) {
               ByteBuffer var7 = ByteBuffer.allocate(var6.length + var3.getLength());
               var7.put(var6);
               var7.put(var3.getValue());
               ((Buffer)var7).rewind();
               var6 = var7.array();
            } else {
               var6 = var3.getValue();
            }
         }

         if (var6 == null) {
            throw new ServiceResultException(StatusCodes.Bad_UnexpectedError, "The PSHA algorithm requires a non-null seed.");
         } else {
            SecurityPolicy var15 = this.securityConfiguration.getSecurityPolicy();
            Mac var8 = CryptoUtil.createMac(var15.getKeyDerivationAlgorithm(), var1.getValue());
            var8.update(var6);
            byte[] var9 = var8.doFinal();
            byte[] var10 = new byte[var8.getMacLength() + var6.length];
            System.arraycopy(var9, 0, var10, 0, var9.length);
            System.arraycopy(var6, 0, var10, var9.length, var6.length);
            byte[] var11 = new byte[var5];
            int var12 = 0;

            do {
               var8.update(var10);
               byte[] var13 = var8.doFinal();
               if (var4 < var13.length) {
                  for (int var14 = var4; var12 < var5 && var14 < var13.length; var14++) {
                     var11[var12++] = var13[var14];
                  }
               }

               if (var4 > var13.length) {
                  var4 -= var13.length;
               } else {
                  var4 = 0;
               }

               var8.update(var9);
               var9 = var8.doFinal();
               System.arraycopy(var9, 0, var10, 0, var9.length);
            } while (var12 < var5);

            return var11;
         }
      }
   }

   protected Mac createHmac(byte[] var1) throws ServiceResultException {
      SecurityPolicy var2 = this.securityConfiguration.getSecurityPolicy();
      return CryptoUtil.createMac(var2.getSymmetricSignatureAlgorithm(), var1);
   }
}
