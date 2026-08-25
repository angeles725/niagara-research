package com.tridium.nre.util;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;

public class FileLock {
   File file;
   int lockId;

   private FileLock(File file) {
      this.file = file;
      this.lockId = -1;
   }

   public static void check(File f) throws FileLockException, IOException {
      lock(f).unlock();
   }

   public static FileLock lock(File f) throws FileLockException, IOException {
      if (f == null) {
         throw new FileLockException("error obtaining lock, uninitialized file");
      }

      if (f.isDirectory()) {
         throw new FileLockException("error obtaining lock, file is directory");
      }

      if (!f.getAbsoluteFile().getParentFile().exists()) {
         throw new FileLockException("error obtaining lock, file parent directory does not exist");
      }

      FileLock flock = new FileLock(f);
      flock.lockImpl();
      return flock;
   }

   public static FileLock lock(File f, int timeout) throws FileLockException, IOException {
      if (f == null) {
         throw new FileLockException("error obtaining lock, uninitialized file");
      }

      if (f.isDirectory()) {
         throw new FileLockException("error obtaining lock, file is directory");
      }

      if (!f.getAbsoluteFile().getParentFile().exists()) {
         throw new FileLockException("error obtaining lock, file parent directory does not exist");
      }

      if (timeout < 0) {
         throw new FileLockException("error obtaining lock, invalid timeout");
      }

      int interval = 200;
      FileLockException lockException = null;
      FileLock flock = new FileLock(f);
      if (timeout < interval) {
         timeout = interval;
      }

      while (timeout >= 0) {
         try {
            flock.lockImpl();
            return flock;
         } catch (FileLockException fle) {
            lockException = fle;

            try {
               Thread.sleep(interval);
            } catch (Exception var6) {
            }

            timeout -= interval;
            if (interval < 5000) {
               interval = (int)(interval * 1.2);
            }

            if (interval > 5000) {
               interval = 5000;
            }
         }
      }

      throw lockException;
   }

   private void lockImpl() throws FileLockException {
      SecurityManager security = System.getSecurityManager();
      if (security != null) {
         security.checkRead(this.file.getAbsolutePath());
      }

      this.lockId = AccessController.doPrivileged(() -> FileLock.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.lockFile(this.file.getAbsolutePath()));
      if (this.lockId < 0) {
         throw new FileLockException("error obtaining lock for " + this.file.toString());
      }
   }

   public void unlock() throws FileLockException {
      SecurityManager security = System.getSecurityManager();
      if (security != null) {
         security.checkRead(this.file.getAbsolutePath());
      }

      int rc = AccessController.doPrivileged(
         () -> FileLock.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.unlockFile(this.file.getAbsolutePath(), this.lockId)
      );
      if (rc < 0) {
         throw new FileLockException("error unlocking " + this.file.toString());
      }
   }

   @Override
   public String toString() {
      return this.file.toString();
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
