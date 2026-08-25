package com.tridium.nre.util;

import com.tridium.nre.platform.PlatformUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedActionException;

public class MqUtil {
   private static final String _MQUEUE_RECEIVE_MESSAGE_PATH = "/proc/boot/mqueue-receive";
   private static final String _MQUEUE_CREATE_QUEUE_PATH = "/proc/boot/mqueue-create";
   private static final String _MQUEUE_SEND_MESSAGE_PATH = "/proc/boot/mqueue-send";
   private static final String _MQUEUE_GETATTR_PATH = "/proc/boot/mqueue-getattr";

   private static int execute(String[] arguments, StringBuilder standardOutputBuffer) {
      int rc;
      try {
         rc = AccessController.doPrivileged(() -> {
            Process process = Runtime.getRuntime().exec(arguments);
            if (standardOutputBuffer != null) {
               try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                  char[] buf = new char[1024];
                  int size = reader.read(buf);
                  if (size >= 0) {
                     standardOutputBuffer.append(new String(buf, 0, size));
                  }
               } catch (IOException var17) {
               }
            }

            return process.waitFor();
         });
      } catch (PrivilegedActionException pae) {
         pae.printStackTrace();
         rc = -1;
      }

      return rc;
   }

   public static boolean createMessageQueue(String name) {
      return PlatformUtil.isNativePlatform() ? createMessageQueue0(name) : execute(new String[]{"/proc/boot/mqueue-create", name}, null) == 0;
   }

   public static boolean queueExists(String name) {
      return PlatformUtil.isNativePlatform() ? queueExists0(name) : execute(new String[]{"/proc/boot/mqueue-getattr", name}, null) == 0;
   }

   public static boolean sendMessage(String name, String message) {
      return PlatformUtil.isNativePlatform() ? sendMessage0(name, message) : execute(new String[]{"/proc/boot/mqueue-send", name, message}, null) == 0;
   }

   public static boolean receiveMessage(String name, StringBuilder messageBuffer) {
      if (PlatformUtil.isNativePlatform()) {
         byte[] buffer = new byte[64];
         int size = receiveMessage0(name, buffer);
         if (size > 0) {
            messageBuffer.append(new String(buffer, 0, size));
         }

         return size > 0;
      } else {
         return execute(new String[]{"/proc/boot/mqueue-receive", name}, messageBuffer) == 0;
      }
   }

   private static native boolean createMessageQueue0(String var0);

   private static native boolean queueExists0(String var0);

   private static native boolean sendMessage0(String var0, String var1);

   private static native int receiveMessage0(String var0, byte[] var1);
}
