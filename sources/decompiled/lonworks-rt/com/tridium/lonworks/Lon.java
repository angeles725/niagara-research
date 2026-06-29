package com.tridium.lonworks;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class Lon {
   public static boolean disableBindingCom;
   public static boolean disableDeviceCom;
   private static boolean disableResetOnStart_ = Boolean.getBoolean("lonworks.disableResetOnStart");
   public static boolean lcInNavTree;
   public static int maxNvLength;

   public static boolean d() {
      return !disableDeviceCom;
   }

   public static boolean n() {
      return !disableBindingCom;
   }

   public static boolean disableResetOnStart() {
      return disableResetOnStart_;
   }

   public static boolean lcInNavTree() {
      return lcInNavTree;
   }

   public static int maxNvLength() {
      return maxNvLength;
   }

   static {
      AccessController.doPrivileged((PrivilegedAction)(() -> {
         disableBindingCom = Boolean.getBoolean("lonworks.disableBindingCom");
         disableDeviceCom = Boolean.getBoolean("lonworks.disableDeviceCom");
         return null;
      }));
      AccessController.doPrivileged((PrivilegedAction)(() -> {
         lcInNavTree = Boolean.getBoolean("lonworks.lonComponentsInNavTree");
         return null;
      }));
      AccessController.doPrivileged((PrivilegedAction)(() -> {
         maxNvLength = Integer.getInteger("lonworks.maxNvLength", 31);
         return null;
      }));
   }
}
