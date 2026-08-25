package com.tridium.niagarad.file;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.security.PBEEncodingInfo;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.SecretChars;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileStore {
   protected String id = null;
   protected FileStoreElement head = null;
   protected FileStoreElement tail = null;
   protected Map<String, Long> transactionDestinationSizeDeltaByFS = null;
   protected long transactionCacheSize = 0L;
   private final boolean isAutoCommit;
   private PBEEncodingKey pbeKey = null;
   private static String tempDirPath = null;
   private static boolean tempUsesSharedMemory = false;
   public static FileStore currentInstance = null;
   private static IPlatformProvider platformProvider = null;
   protected static List<String> cancelledTransactionIds = new ArrayList<>();

   public static FileStore getInstance(String id) {
      if (currentInstance == null) {
         return null;
      } else {
         return currentInstance.getId().equals(id) ? currentInstance : null;
      }
   }

   public static FileStore make() {
      return make(false);
   }

   public static FileStore make(boolean isAutoCommit) {
      currentInstance = new FileStore(isAutoCommit);
      return currentInstance;
   }

   public static FileStore make(boolean isAutoCommit, String encodedValidator, String encodingSaltHex, int encodingIterationCount, SecretChars passPhrase) throws Exception {
      currentInstance = new FileStore(isAutoCommit, encodedValidator, encodingSaltHex, encodingIterationCount, passPhrase);
      return currentInstance;
   }

   public static FileStore make(boolean isAutoCommit, PBEEncodingKey keyInfo) throws Exception {
      currentInstance = new FileStore(isAutoCommit, keyInfo);
      return currentInstance;
   }

   public static boolean commitInstance(Logger log) {
      if (currentInstance == null) {
         log.fine("FileStore::commitInstance no current instance");
         return false;
      }

      CoreCryptoManager coreCryptoManager = null;

      try {
         coreCryptoManager = CoreCryptoManager.get(NiagaraDaemon.getSecurityInfoProvider());
         coreCryptoManager.pauseFileMonitor();
         coreCryptoManager.pauseSaveRecurring();
         boolean result = currentInstance.commit(log);
         log.fine("FileStore::commitInstance commit complete");
         currentInstance = null;
         return result;
      } finally {
         if (coreCryptoManager != null) {
            coreCryptoManager.resumeFileMonitor();
            coreCryptoManager.resumeSaveRecurring();
         }
      }
   }

   public static boolean abortInstance(Logger log) {
      if (currentInstance == null) {
         log.fine("FileStore::abortInstance no current instance");
         return false;
      } else {
         boolean result = currentInstance.abort(log);
         log.fine("FileStore::abortInstance abort complete");
         currentInstance = null;
         return result;
      }
   }

   public static String getTempDirPath() {
      if (tempDirPath == null) {
         if (NiagaraDaemon.FILESTORE_SHARED_MEMORY_PATH != null) {
            tempDirPath = NiagaraDaemon.FILESTORE_SHARED_MEMORY_PATH;
            tempUsesSharedMemory = true;
         } else {
            tempDirPath = platformProvider.getTempDirPath();
            tempUsesSharedMemory = false;
         }

         if (NiagaraDaemon.getFilter().isLoggable(Level.FINE)) {
            NiagaraDaemon.getFilter().fine("file store using '" + tempDirPath + "' for temporary file storage (shared memory = " + tempUsesSharedMemory + ")");
         }

         long freeSpace = getFreeSpace(tempDirPath);
         if (freeSpace <= 0L) {
            NiagaraDaemon.getFilter()
               .warning("file store temporary storage at '" + tempDirPath + "' free space = " + freeSpace + " bytes, file caching unavailable");
         }
      }

      return tempDirPath;
   }

   public static void initializeProvider(IPlatformProvider newPlatformProvider) {
      if (platformProvider == null) {
         platformProvider = newPlatformProvider;
      }
   }

   public FileStoreElement newElement(String destPath, int size, ErrorHandler handler, Logger log) {
      StringBuilder buffer = new StringBuilder();
      File destFile = new File(destPath);
      if (destFile.exists() && !destFile.canWrite()) {
         buffer.append("write permission denied for ").append(destPath);
         handler.error(buffer.toString());
         return InvalidFileStoreElement.PERMISSION_DENIED;
      }

      if (!destFile.exists() && destFile.getParentFile() != null && destFile.getParentFile().exists() && !destFile.getParentFile().canWrite()) {
         buffer.append("write permission denied for parent directory of ").append(destPath);
         handler.error(buffer.toString());
         return InvalidFileStoreElement.PERMISSION_DENIED;
      }

      if (size >= 0) {
         this.transactionCacheSize += size;
      }

      FileStoreElement element = FileStoreElement.make(destPath, size, this.isAutoCommit, this, log);
      if (element == null) {
         buffer.append("insufficient cache storage for ").append(destPath).append(" ").append(size);
         handler.error(buffer.toString());
         return InvalidFileStoreElement.TOO_LARGE;
      }

      String destinationFileSystem = platformProvider.getFileSystemName(destPath);
      if (destinationFileSystem == null) {
         buffer.append("can not determine parent filesystem for requested file ").append(destPath).append(", can not create new element");
         handler.error(buffer.toString());
         return InvalidFileStoreElement.UNKNOWN_FILESYSTEM;
      }

      if (element instanceof InvalidFileStoreElement) {
         buffer.append("error occurred while validating file destination ")
            .append(destPath)
            .append(" (")
            .append(element.getErrorCode())
            .append(") can not create new element");
         handler.error(buffer.toString());
         return element;
      }

      long availableStorageOnDestination = getFreeSpace(destPath);
      if (log.isLoggable(Level.FINEST)) {
         log.finest(
            "FileStore::newElement free space at destination '" + destPath + "' is " + availableStorageOnDestination + " bytes (" + size + " bytes required)"
         );
      }

      Long currentDestinationDelta;
      if (element instanceof FileCachedFileStoreElement) {
         if (destinationFileSystem.toLowerCase().startsWith(getTempDirPath().toLowerCase())) {
            ((FileCachedFileStoreElement)element).sameFileSystem = true;
            if (Long.MAX_VALUE - availableStorageOnDestination < this.transactionCacheSize) {
               availableStorageOnDestination = Long.MAX_VALUE;
            } else {
               availableStorageOnDestination += this.transactionCacheSize;
            }
         }

         currentDestinationDelta = this.transactionDestinationSizeDeltaByFS.get(destinationFileSystem);
         if (currentDestinationDelta == null) {
            currentDestinationDelta = 0L;
         }

         if (element.getSizeDelta() >= 0L && currentDestinationDelta >= 0L && Long.MAX_VALUE - currentDestinationDelta <= element.getSizeDelta()) {
            currentDestinationDelta = Long.MAX_VALUE;
         } else {
            currentDestinationDelta = currentDestinationDelta + element.getSizeDelta();
         }

         this.transactionDestinationSizeDeltaByFS.put(destinationFileSystem, currentDestinationDelta);
      } else {
         if (!(element instanceof UncachedFileStoreElement)) {
            throw new IllegalStateException("Unexpected element type in FileStore");
         }

         currentDestinationDelta = element.getSizeDelta();
      }

      if (currentDestinationDelta > availableStorageOnDestination) {
         buffer.append("entity too large ").append(destPath).append(" ").append(currentDestinationDelta).append(" > ").append(availableStorageOnDestination);
         handler.error(buffer.toString());
         return InvalidFileStoreElement.TOO_LARGE;
      } else {
         return this.addElement(element);
      }
   }

   public FileStoreElement addElement(FileStoreElement element) {
      Objects.requireNonNull(element);
      if (this.head == null) {
         this.head = this.tail = element;
      } else {
         this.tail.next = element;
         this.tail = element;
      }

      return element;
   }

   public FileStoreElement newDeleteElement(String destPath, ErrorHandler handler, Logger log) {
      StringBuilder buffer = new StringBuilder();
      File destFile = new File(destPath);
      if (destFile.exists() && !destFile.canWrite()) {
         buffer.append("write permission denied for ").append(destPath);
         handler.error(buffer.toString());
         return InvalidFileStoreElement.PERMISSION_DENIED;
      }

      DeleteFileStoreElement result = DeleteFileStoreElement.make(destPath, this);
      if (result != null) {
         String destinationFileSystem = platformProvider.getFileSystemName(destPath);
         if (destinationFileSystem == null) {
            buffer.append("can not determine parent filesystem for requested file ").append(destPath).append(", can not create delete element");
            handler.error(buffer.toString());
            return InvalidFileStoreElement.UNKNOWN_FILESYSTEM;
         }

         Long currentDestinationDelta = this.transactionDestinationSizeDeltaByFS.get(destinationFileSystem);
         if (currentDestinationDelta == null) {
            currentDestinationDelta = 0L;
         }

         currentDestinationDelta = currentDestinationDelta + result.getSizeDelta();
         this.transactionDestinationSizeDeltaByFS.put(destinationFileSystem, currentDestinationDelta);
         if (this.head == null) {
            this.head = this.tail = result;
         } else {
            this.tail.next = result;
            this.tail = result;
         }
      }

      return result;
   }

   public FileStoreElement newMkDirElement(String destPath) {
      MkDirFileStoreElement result = MkDirFileStoreElement.make(destPath, this);
      if (this.head == null) {
         this.head = this.tail = result;
      } else {
         this.tail.next = result;
         this.tail = result;
      }

      return result;
   }

   public FileStoreElement newRenameElement(String srcPath, String destPath) {
      RenameFileStoreElement result = RenameFileStoreElement.make(srcPath, destPath, this);
      if (this.head == null) {
         this.head = this.tail = result;
      } else {
         this.tail.next = result;
         this.tail = result;
      }

      return result;
   }

   public String getId() {
      return this.id;
   }

   public static long getFreeSpace(String destPath) {
      return destPath.startsWith(tempDirPath) && tempUsesSharedMemory
         ? platformProvider.getFreePhysicalMemoryBytes() - 1048576L
         : platformProvider.getFreeBytes(destPath);
   }

   public static void unload() {
      currentInstance = null;
   }

   protected FileStore(boolean isAutoCommit, String encodedValidator, String encodingSaltHex, int encodingIterationCount, SecretChars passPhrase) throws Exception {
      this(isAutoCommit);
      PBEEncodingInfo validator = new PBEEncodingInfo(encodedValidator, encodingSaltHex, encodingIterationCount);
      if (passPhrase == null) {
         SecretChars secretChars = platformProvider.getSystemPassword();
         Throwable var8 = null;

         try {
            this.pbeKey = validator.makePBEKey(secretChars);
         } catch (Throwable var17) {
            var8 = var17;
            throw var17;
         } finally {
            if (secretChars != null) {
               if (var8 != null) {
                  try {
                     secretChars.close();
                  } catch (Throwable var16) {
                     var8.addSuppressed(var16);
                  }
               } else {
                  secretChars.close();
               }
            }
         }
      } else {
         this.pbeKey = validator.makePBEKey(passPhrase);
      }
   }

   protected FileStore(boolean isAutoCommit, PBEEncodingKey pbeKey) throws Exception {
      this(isAutoCommit);
      this.pbeKey = pbeKey;
   }

   protected FileStore(boolean isAutoCommit) {
      this.head = null;
      this.tail = null;
      this.isAutoCommit = isAutoCommit;
      this.id = new String(Base64.getEncoder().encode(String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));
      this.id = this.id.replace('+', 'A');
      this.transactionDestinationSizeDeltaByFS = new HashMap<>();
      this.transactionCacheSize = 0L;
   }

   protected boolean commit(Logger log) {
      log.fine("FileStore::commit");
      boolean result = true;

      for (FileStoreElement temp = this.head; temp != null; temp = temp.next) {
         result = temp.commit(log) && result;
      }

      return result;
   }

   protected boolean abort(Logger log) {
      log.fine("FileStore::abort");
      FileStoreElement temp = this.head;
      boolean result = true;

      while (temp != null) {
         result = temp.abort(log) && result;
         temp = temp.next;
      }

      cancelledTransactionIds.add(this.getId());
      return result;
   }

   public boolean hasPBEKey() {
      return this.pbeKey != null;
   }

   public PBEEncodingKey getPBEKey() {
      if (this.pbeKey == null) {
         SecretChars secretChars = platformProvider.getSystemPassword();
         Throwable var2 = null;

         try {
            this.pbeKey = new PBEEncodingKey(secretChars);
         } catch (Throwable var11) {
            var2 = var11;
            throw var11;
         } finally {
            if (secretChars != null) {
               if (var2 != null) {
                  try {
                     secretChars.close();
                  } catch (Throwable var10) {
                     var2.addSuppressed(var10);
                  }
               } else {
                  secretChars.close();
               }
            }
         }
      }

      return this.pbeKey;
   }

   public static boolean isTransactionCancelled(String transactionId) {
      for (String cancelledTransactionId : cancelledTransactionIds) {
         if (cancelledTransactionId.equals(transactionId)) {
            return true;
         }
      }

      return false;
   }
}
