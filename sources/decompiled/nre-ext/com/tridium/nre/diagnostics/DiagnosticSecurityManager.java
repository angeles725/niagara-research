package com.tridium.nre.diagnostics;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

public class DiagnosticSecurityManager extends SecurityManager {
   @Override
   public void checkAccept(String host, int port) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkAccept(host, port);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkAccept", host + port);
      }
   }

   @Override
   public void checkAccess(Thread t) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkAccess(t);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkAccess", t);
      }
   }

   @Override
   public void checkAccess(ThreadGroup g) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkAccess(g);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkAccess", g);
      }
   }

   @Override
   public void checkConnect(String host, int port) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkConnect(host, port);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkConnect", host + port);
      }
   }

   @Override
   public void checkConnect(String host, int port, Object cx) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkConnect(host, port, cx);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkConnect", host + port);
      }
   }

   @Override
   public void checkCreateClassLoader() {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkPropertyAccess("none");
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkCreateClassLoader", null);
      }
   }

   @Override
   public void checkDelete(String file) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkDelete(file);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkDelete", file);
      }
   }

   @Override
   public void checkExec(String cmd) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkExec(cmd);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkExec", cmd);
      }
   }

   @Override
   public void checkExit(int status) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkExit(status);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkExit", status);
      }
   }

   @Override
   public void checkLink(String lib) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkLink(lib);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkLink", lib);
      }
   }

   @Override
   public void checkListen(int port) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkListen(port);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkListen", port);
      }
   }

   @Deprecated
   public void checkMemberAccess(Class<?> clazz, int which) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkMemberAccess(clazz, which);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkMemberAccess*", "" + clazz + which);
      }
   }

   @Override
   public void checkMulticast(InetAddress maddr) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkMulticast(maddr);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkMulticast", maddr);
      }
   }

   @Deprecated
   @Override
   public void checkMulticast(InetAddress maddr, byte ttl) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkMulticast(maddr, ttl);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkMulticast*", "" + maddr + ttl);
      }
   }

   @Override
   public void checkPackageDefinition(String pkg) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkPackageDefinition(pkg);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkPackageDefinition", pkg);
      }
   }

   @Override
   public void checkPermission(Permission perm) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkPermission(perm);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkPermission", perm);
      }
   }

   @Override
   public void checkPermission(Permission perm, Object context) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkPermission(perm, context);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkPermission", perm);
      }
   }

   @Override
   public void checkPrintJobAccess() {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkPrintJobAccess();
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkPrintJobAccess", "");
      }
   }

   @Override
   public void checkPropertiesAccess() {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkPropertiesAccess();
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkProperties", "");
      }
   }

   @Override
   public void checkPropertyAccess(String key) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkPropertyAccess(key);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkPropertyAccess", key);
      }
   }

   @Override
   public void checkRead(FileDescriptor fd) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkRead(fd);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkRead", fd);
      }
   }

   @Override
   public void checkRead(String file) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkRead(file);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkRead", file);
      }
   }

   @Override
   public void checkRead(String file, Object context) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkRead(file, context);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkRead", file);
      }
   }

   @Override
   public void checkSecurityAccess(String target) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkSecurityAccess(target);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkSecurityAccess", target);
      }
   }

   @Override
   public void checkSetFactory() {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkSetFactory();
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkSetFactory", "");
      }
   }

   @Deprecated
   public void checkSystemClipboardAccess() {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkSystemClipboardAccess();
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkSystemClipboardAccess*", "");
      }
   }

   @Deprecated
   public boolean checkTopLevelWindow(Object window) {
      long start = DiagnosticUtil.nanoTime();

      try {
         return super.checkTopLevelWindow(window);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkTopLevelWindow*", window);
      }
   }

   @Override
   public void checkWrite(FileDescriptor fd) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkWrite(fd);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkWrite", fd);
      }
   }

   @Override
   public void checkWrite(String file) {
      long start = DiagnosticUtil.nanoTime();

      try {
         super.checkWrite(file);
      } finally {
         DiagnosticUtil.complete(start, "SecurityManger.checkWrite", file);
      }
   }
}
