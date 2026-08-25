package com.tridium.nre.util;

import java.security.AccessControlContext;
import java.security.AccessController;

public class PrivilegedRunnable implements Runnable {
   private final Runnable r;
   private final AccessControlContext context;

   public PrivilegedRunnable(Runnable r, AccessControlContext context) {
      this.r = r;
      this.context = context;
   }

   @Override
   public void run() {
      AccessController.doPrivileged(() -> {
         this.r.run();
         return null;
      }, this.context);
   }
}
