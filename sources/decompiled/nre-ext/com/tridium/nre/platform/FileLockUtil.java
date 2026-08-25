package com.tridium.nre.platform;

import com.tridium.nre.util.FileLockException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class FileLockUtil {
   private static final Map<Integer, FileLockUtil.FileLockObject> fileLocksById = new HashMap<>();
   private static final Set<String> lockedPaths = new HashSet<>();
   private static final Logger log = Logger.getLogger("sys.file.lock");

   static synchronized int lockFile(String filePath) {
      if (filePath == null) {
         return -1;
      }

      if (filePath.length() > 4096) {
         return -1;
      }

      if (log.isLoggable(Level.FINEST)) {
         log.finest("obtain file lock for '" + filePath + "'...");
      }

      File file = new File(filePath);
      if (file.isDirectory()) {
         log.severe("failed to obtain file lock for '" + filePath + "', file is directory");
         return -1;
      }

      File parentFile = file.getAbsoluteFile().getParentFile();
      if (!parentFile.exists()) {
         log.severe("failed to obtain file lock for '" + filePath + "', parent directory does not exist");
         return -1;
      }

      if (lockedPaths.contains(filePath)) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("failed to obtain file lock for '" + filePath + "', this process already has file lock");
         }

         return -1;
      } else {
         FileLockUtil.FileLockObject fileLockObject = null;

         int lockId;
         try {
            fileLockObject = new FileLockUtil.FileLockObject(filePath);
            lockId = fileLockObject.lock();
         } catch (Exception e) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("failed to obtain file lock for '" + filePath + "', " + e.getLocalizedMessage());
               if (log.isLoggable(Level.FINEST)) {
                  log.log(Level.FINEST, "file lock stack trace: ", e);
               }
            }

            try {
               if (fileLockObject != null) {
                  fileLockObject.close();
               }
            } catch (Exception var7) {
            }

            return -1;
         }

         fileLocksById.put(lockId, fileLockObject);
         lockedPaths.add(filePath);
         if (log.isLoggable(Level.FINEST)) {
            log.finest("obtained file lock for '" + filePath + "' with lock identifier '" + lockId + "'");
         }

         return lockId;
      }
   }

   static synchronized int unlockFile(String filePath, int lockId) {
      if (filePath == null) {
         return -1;
      }

      if (filePath.length() > 4096) {
         return -1;
      }

      if (log.isLoggable(Level.FINEST)) {
         log.finest("release file lock for '" + filePath + "' with lock identifier '" + lockId + "'...");
      }

      File file = new File(filePath);
      if (file.isDirectory()) {
         log.severe("failed to release file lock for '" + filePath + "', file is directory");
         return -1;
      }

      File parentFile = file.getAbsoluteFile().getParentFile();
      if (!parentFile.exists()) {
         log.severe("failed to release file lock for '" + filePath + "', parent directory does not exist");
         return -1;
      }

      if (!lockedPaths.contains(filePath)) {
         log.severe("failed to release file lock '" + filePath + "' with lock identifier '" + lockId + "', file is not locked by this process");
         return -1;
      }

      FileLockUtil.FileLockObject fileLockObject = fileLocksById.get(lockId);
      if (fileLockObject == null) {
         log.log(
            Level.SEVERE,
            "failed to release file lock for '" + filePath + "' with lock identifier '" + lockId + "', no file associated with this lock identifier"
         );
         return -1;
      }

      try {
         fileLockObject.unlock();
      } catch (Exception e) {
         log.log(Level.SEVERE, "failed to release file lock for '" + filePath + "' with lock identifier '" + lockId + "'", e);
         return -1;
      }

      try {
         fileLockObject.close();
      } catch (Exception var6) {
      }

      lockedPaths.remove(filePath);
      fileLocksById.remove(lockId);
      if (log.isLoggable(Level.FINEST)) {
         log.finest("released file lock for '" + filePath + "' with lock identifier '" + lockId + "'");
      }

      return 0;
   }

   private static class FileLockObject {
      private static final boolean USE_FILELOCK_SUFFIX = AccessController.doPrivileged(
         () -> Boolean.valueOf(System.getProperty("niagara.use.filelock.suffix", OperatingSystemEnum.isOS(OperatingSystemEnum.windows) ? "true" : "false"))
      );
      private File file;
      private RandomAccessFile randomAccessFile;
      private FileChannel fileChannel;
      private FileLock fileLock;

      FileLockObject(String filePath) throws Exception {
         if (USE_FILELOCK_SUFFIX) {
            filePath = filePath + ".lock";
         }

         this.file = new File(filePath);
         this.randomAccessFile = new RandomAccessFile(this.file, "rw");
         this.fileChannel = this.randomAccessFile.getChannel();
      }

      int lock() throws Exception {
         this.fileLock = this.fileChannel.tryLock();
         if (this.fileLock == null) {
            throw new FileLockException("Failed to obtain lock on requested file '" + this.file + "'");
         } else {
            return this.hashCode();
         }
      }

      void unlock() throws Exception {
         this.fileLock.release();
      }

      void close() throws Exception {
         if (this.fileChannel != null) {
            this.fileChannel.close();
            this.fileChannel = null;
         }

         if (this.randomAccessFile != null) {
            this.randomAccessFile.close();
            this.randomAccessFile = null;
         }

         if (USE_FILELOCK_SUFFIX && this.file != null && this.file.exists() && !this.file.delete()) {
            throw new Exception("Failed to delete suffix lock file '" + this.file + "'");
         }

         if (this.file != null) {
            this.file = null;
         }
      }

      @Override
      public int hashCode() {
         return this.fileLock.hashCode();
      }

      @Override
      public boolean equals(Object other) {
         return !(other instanceof FileLockUtil.FileLockObject) ? false : this.fileLock.equals(other);
      }
   }
}
