package com.tridium.nre.platform;

import java.security.AccessController;

public final class OperatingSystemEnum {
   public static final int UNKNOWN = -1;
   public static final int UNSUPPORTED = 0;
   public static final int WINDOWS = 1;
   public static final int QNX = 2;
   public static final int LINUX = 3;
   public static final int MAC = 4;
   public static final OperatingSystemEnum unknown = new OperatingSystemEnum(-1);
   public static final OperatingSystemEnum windows = new OperatingSystemEnum(1);
   public static final OperatingSystemEnum qnx = new OperatingSystemEnum(2);
   public static final OperatingSystemEnum linux = new OperatingSystemEnum(3);
   public static final OperatingSystemEnum unsupported = new OperatingSystemEnum(0);
   public static final OperatingSystemEnum mac = new OperatingSystemEnum(4);
   private int ordinalValue = -1;
   private static OperatingSystemEnum os = null;

   private OperatingSystemEnum(int ordinalValue) {
      this.ordinalValue = ordinalValue;
   }

   public static OperatingSystemEnum getOS() {
      if (os == null) {
         checkOS();
      }

      return os;
   }

   public static boolean isOS(OperatingSystemEnum suspectOS) {
      if (os == null) {
         checkOS();
      }

      return suspectOS == os;
   }

   private static void checkOS() {
      if (os == null) {
         String operatingSystemString = AccessController.doPrivileged(() -> System.getProperty("os.name"));
         if (operatingSystemString.toLowerCase().contains("nix")
            || operatingSystemString.toLowerCase().contains("nux")
            || operatingSystemString.toLowerCase().contains("aix")) {
            os = linux;
         } else if (operatingSystemString.toLowerCase().startsWith("win")) {
            os = windows;
         } else if (operatingSystemString.toLowerCase().equals("qnx")) {
            os = qnx;
         } else if (operatingSystemString.toLowerCase().contains("mac")) {
            os = mac;
         } else {
            os = unsupported;
         }
      }
   }

   public int getOrdinal() {
      return this.ordinalValue;
   }
}
