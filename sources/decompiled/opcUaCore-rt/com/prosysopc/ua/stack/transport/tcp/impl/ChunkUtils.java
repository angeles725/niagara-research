package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.StatusCodes;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ChunkUtils {
   public static String getAbortMessage(ByteBuffer var0) throws ServiceResultException {
      ((Buffer)var0).position(8);
      return getString(var0);
   }

   public static byte[] getByteString(ByteBuffer var0) {
      int var1 = var0.getInt();
      if (var1 == -1) {
         return null;
      } else {
         if (var1 < -1 || var1 > var0.remaining()) {
            new ServiceResultException(StatusCodes.Bad_CommunicationError, "Unexpected length");
         }

         byte[] var2 = new byte[var1];
         var0.get(var2);
         return var2;
      }
   }

   public static int getMessageType(ByteBuffer var0) {
      ((Buffer)var0).position(0);
      return var0.getInt();
   }

   public static byte[] getRecvCertificateThumbprint(ByteBuffer var0) {
      ((Buffer)var0).position(12);
      int var1 = var0.getInt();
      if (var1 > 0) {
         ((Buffer)var0).position(var0.position() + var1);
      }

      int var2 = var0.getInt();
      if (var2 > 0) {
         ((Buffer)var0).position(var0.position() + var2);
      }

      return getByteString(var0);
   }

   public static int getRequestId(ByteBuffer var0) {
      ((Buffer)var0).position(20);
      return var0.getInt();
   }

   public static int getSecureChannelId(ByteBuffer var0) {
      ((Buffer)var0).position(8);
      return var0.getInt();
   }

   public static String getSecurityPolicyUri(ByteBuffer var0) throws ServiceResultException {
      ((Buffer)var0).position(12);
      return getString(var0);
   }

   public static int getSequenceNumber(ByteBuffer var0) {
      ((Buffer)var0).position(16);
      return var0.getInt();
   }

   public static String getString(ByteBuffer var0) throws ServiceResultException {
      byte[] var1 = getByteString(var0);
      return new String(var1, StandardCharsets.UTF_8);
   }

   public static int getTokenId(ByteBuffer var0) {
      ((Buffer)var0).position(12);
      return var0.getInt();
   }
}
