package com.tridium.nre.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.WeakHashMap;

public class SimpleKeyValueUtil {
   private File directory = null;
   private static WeakHashMap<String, SimpleKeyValueUtil> instances = new WeakHashMap<>();

   public static synchronized SimpleKeyValueUtil getInstance(String requestedFilePath) throws Exception {
      SimpleKeyValueUtil instance = instances.get(requestedFilePath);
      if (instance == null) {
         File candidateDirectory = new File(requestedFilePath);
         if (candidateDirectory.exists()) {
            if (!candidateDirectory.isDirectory()) {
               throw new IllegalArgumentException("Provided file '" + requestedFilePath + "' is not a directory");
            }

            if (!candidateDirectory.canWrite()) {
               throw new IOException("Process can not write to provided directory '" + requestedFilePath + "'");
            }
         } else if (!candidateDirectory.mkdirs()) {
            throw new IOException("Failed to create directory or parent directories of '" + requestedFilePath + "'");
         }

         instance = new SimpleKeyValueUtil(candidateDirectory);
         instances.put(requestedFilePath, instance);
      }

      return instance;
   }

   private SimpleKeyValueUtil(File directory) {
      this.directory = directory;
   }

   public synchronized boolean exists(String keyName) {
      try {
         return new File(this.directory, keyName).exists();
      } catch (Exception var3) {
         return false;
      }
   }

   public synchronized byte[] get(String keyName) throws Exception {
      File keyFile = new File(this.directory, keyName);
      FileLock lock = null;
      if (keyFile != null && keyFile.exists()) {
         byte[] keyValue;
         try {
            lock = FileLock.lock(keyFile, 10000);

            try (FileInputStream in = new FileInputStream(keyFile)) {
               byte[] tempKeyValue = new byte[(int)keyFile.length()];
               int bytesRead = in.read(tempKeyValue);
               if (bytesRead <= 0) {
                  throw new Exception("Failed to read the value from key file '" + keyName + "'");
               }

               keyValue = new byte[bytesRead];
               System.arraycopy(tempKeyValue, 0, keyValue, 0, bytesRead);
            }
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }

         return keyValue;
      } else {
         return null;
      }
   }

   public synchronized boolean set(String keyName, byte[] keyValue) throws Exception {
      File keyFile = new File(this.directory, keyName);
      FileLock lock = null;

      try {
         if (!keyFile.exists()) {
            if (keyValue == null || keyValue.length == 0) {
               return true;
            }

            if (!keyFile.createNewFile()) {
               throw new Exception("Failed to create the key file with name '" + keyName + "'");
            }
         }

         lock = FileLock.lock(keyFile, 10000);
         if (keyValue != null && keyValue.length != 0) {
            try (FileOutputStream out = new FileOutputStream(keyFile)) {
               out.write(keyValue);
               out.flush();
            }
         } else {
            lock.unlock();
            lock = null;
            if (!keyFile.delete()) {
               throw new Exception("Failed to delete the key file with name '" + keyName + "'");
            }
         }
      } finally {
         if (lock != null) {
            lock.unlock();
         }
      }

      return true;
   }
}
