package com.tridium.nre.security;

import com.tridium.nre.platform.OperatingSystemEnum;
import com.tridium.nre.security.km.BadKeyMaterialException;
import com.tridium.nre.security.km.KeyMaterial;
import com.tridium.nre.security.km.KeyMaterialFactory;
import java.io.File;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;

public abstract class KeyRing extends RecoverableKeyData {
   protected Hashtable<String, KeyRing.KeyRingEntry> keys = new Hashtable<>();
   protected final KeyMaterial keyMaterial;
   protected static final Logger LOG = Logger.getLogger("security.keyRing");
   public static final long KEY_MATERIAL_ROLL_INTERVAL = AccessController.doPrivileged(
      () -> Long.getLong("niagara.keyMaterialRollInterval", TimeUnit.MILLISECONDS.convert(365L, TimeUnit.DAYS))
   );

   protected KeyRing(KeyMaterial keyMaterial) {
      this.keyMaterial = keyMaterial;
   }

   public synchronized byte[] getKey(String alias) throws Exception {
      checkKeyRingPermission(alias);
      this.checkReload();
      KeyRing.KeyRingEntry entry = this.keys.get(alias);
      if (entry == null) {
         return null;
      }

      try {
         return entry.getKey();
      } catch (BadKeyMaterialException e) {
         KeyRing.KeyRingLock lock = this.lock();

         try {
            AccessController.doPrivileged(this.keyMaterial::refresh);
         } catch (PrivilegedActionException pae) {
            throw pae.getException();
         } finally {
            if (lock != null) {
               lock.unlock();
            }
         }

         return entry.getKey();
      }
   }

   public synchronized void removeKey(String alias) throws Exception {
      checkKeyRingPermission(alias);
      this.preChange();
      this.keys.remove(alias);
      this.postChange();
   }

   public synchronized byte[] createKey(String alias, boolean isExportable) throws Exception {
      checkKeyRingPermission(alias);
      this.preChange();
      KeyRing.KeyRingEntry entry = this.createKeyRingEntry(null, isExportable);
      this.keys.put(alias, entry);
      this.postChange();
      return this.getKey(alias);
   }

   public synchronized void setKey(String alias, byte[] keyData, boolean isExportable) throws Exception {
      checkKeyRingPermission(alias);
      this.preChange();
      KeyRing.KeyRingEntry entry = this.createKeyRingEntry(keyData, isExportable);
      this.keys.put(alias, entry);
      this.postChange();
   }

   private static void checkKeyRingPermission(String alias) {
      SecurityManager manager = System.getSecurityManager();
      if (manager != null) {
         manager.checkPermission(new KeyRingPermission(alias));
      }
   }

   protected abstract KeyRing.KeyRingEntry createKeyRingEntry(byte[] var1, boolean var2) throws Exception;

   protected abstract void preChangeImpl(KeyRing.KeyRingLock var1) throws Exception;

   private void preChange() throws Exception {
      this.preChange(null);
   }

   private void preChange(KeyRing.KeyRingLock lock) throws Exception {
      try {
         this.preChangeImpl(lock);
      } catch (BadKeyMaterialException e) {
         try {
            AccessController.doPrivileged(this.keyMaterial::refresh);
         } catch (PrivilegedActionException pae) {
            throw pae.getException();
         }

         this.preChangeImpl(lock);
      }
   }

   protected abstract void postChangeImpl(KeyRing.KeyRingLock var1) throws Exception;

   private void postChange() throws Exception {
      this.postChange(null);
   }

   private void postChange(KeyRing.KeyRingLock lock) throws Exception {
      this.postChangeImpl(lock);
   }

   protected abstract void checkReload();

   public abstract byte[] exportKeyData(ISecretBytesSupplier var1) throws Exception;

   public abstract void importKeyData(InputStream var1, int var2, ISecretBytesSupplier var3) throws Exception;

   protected abstract KeyRing.KeyRingLock lock() throws Exception;

   public boolean checkSupportsKeyRecovery() {
      return this.supportsKeyRecovery() && this.keyMaterial.supportsKeyRecovery();
   }

   public final synchronized boolean checkRollKeyMaterial(long maxAge) {
      try {
         if (!this.checkSupportsKeyRecovery()) {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.fine("Recovery unsupported, skipping key material roll check.");
            }

            return false;
         }

         long lastModified = this.keyMaterial.getLastModified();
         if (lastModified <= 0L) {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.fine("Skipping key material roll check for invalid lastModified value.");
            }

            return false;
         }

         long keyMaterialAge = System.currentTimeMillis() - lastModified;
         if (maxAge > 0L && keyMaterialAge >= maxAge) {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.fine(String.format("Key material age of %d exceeds max age of %d. Rolling key material.", keyMaterialAge, maxAge));
            }

            this.rollKeyMaterial();
            return true;
         }
      } catch (Exception e) {
         LOG.log(Level.WARNING, "Failed to check key material roll", e);
      }

      return false;
   }

   public final synchronized void rollKeyMaterial() {
      if (this.supportsKeyRecovery() && this.keyMaterial.supportsKeyRecovery()) {
         NiagaraBasicPermission keyRollPermission = new NiagaraBasicPermission("ROLL_KEY_MATERIAL");
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(keyRollPermission);
         }

         KeyRing.KeyRingLock krLock = null;

         try {
            krLock = this.lock();
            int failStep = getKeyRollFailStep();
            this.preChange(krLock);
            HashMap<String, KeyRing.PlainKeyRingEntry> plainKeys = new HashMap<>();

            for (String alias : this.keys.keySet()) {
               KeyRing.KeyRingEntry entry = this.keys.get(alias);
               plainKeys.put(alias, new KeyRing.PlainKeyRingEntry(entry.getKey(), entry.isKeyExportable()));
            }

            this.keyMaterial.createRecoveryKey();
            if (failStep == 0) {
               testKeyRollFail("Fail after recovery key material created.");
            }

            this.createRecoveryKey();
            if (failStep == 1) {
               testKeyRollFail("Fail after recovery key material and key ring created.");
            }

            this.keyMaterial.setKeyMaterial(KeyMaterialFactory.generateNewKeyMaterial());
            if (failStep == 2) {
               testKeyRollFail("Fail after generating a new key material.");
            }

            this.keys.clear();

            for (String alias : plainKeys.keySet()) {
               KeyRing.PlainKeyRingEntry plainEntry = plainKeys.get(alias);
               KeyRing.KeyRingEntry entry = this.createKeyRingEntry(plainEntry.getKey(), plainEntry.isKeyExportable());
               plainEntry.clearKey();
               this.keys.put(alias, entry);
            }

            plainKeys.clear();
            this.postChange(krLock);
            if (failStep == 3) {
               testKeyRollFail("Fail after re-encrypting the key ring.");
            }

            this.deleteRecoveryKey();
            if (failStep == 4) {
               testKeyRollFail("Fail after recovery key ring deleted.");
            }

            this.keyMaterial.deleteRecoveryKey();
            if (failStep == 5) {
               testKeyRollFail("Fail after recovery key material and key ring deleted.");
            }
         } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not roll key material. Cause is: " + e.getMessage(), e);
         } finally {
            if (krLock != null) {
               krLock.unlock();
            }
         }
      } else {
         throw new IllegalStateException("Cannot roll key material on this platform");
      }
   }

   private static int getKeyRollFailStep() {
      if (SecurityConstants.canCheckTpk()) {
         LOG.fine("Skipping key roll failure test check in release build");
      }

      Integer failStepProp = AccessController.doPrivileged(() -> Integer.getInteger("niagara.keyRollFailureTest"));
      if (failStepProp != null) {
         LOG.warning("Property found to test key material rolling failure cases. Process will exit when failure step is reached");
         return failStepProp;
      }

      if (!OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
         return -1;
      }

      File file = new File("/etc/key-roll-failure-test");
      int failStep = -1;
      if (file.exists()) {
         LOG.warning("Cookie found to test key material rolling failure cases. Process will exit when failure step is reached");

         try (Scanner scanner = new Scanner(file)) {
            failStep = scanner.nextInt();
            if (!file.delete()) {
               LOG.warning("Failed to delete key roll failure cookie.");
            }
         } catch (Exception e) {
            LOG.warning("Could not read cookie. Process will run normally.");
         }
      }

      return failStep;
   }

   private static void testKeyRollFail(String failStepMessage) {
      LOG.severe("Testing failure for step: " + failStepMessage);
      System.exit(-1);
   }

   protected abstract class KeyRingEntry {
      public abstract boolean isKeyExportable();

      public abstract byte[] getKey() throws Exception;
   }

   protected abstract class KeyRingLock {
      protected abstract void unlock();
   }

   private class PlainKeyRingEntry extends KeyRing.KeyRingEntry {
      private final byte[] key;
      private final boolean isKeyExportable;

      private PlainKeyRingEntry(byte[] key, boolean isKeyExportable) {
         this.key = key;
         this.isKeyExportable = isKeyExportable;
      }

      @Override
      public boolean isKeyExportable() {
         return this.isKeyExportable;
      }

      @Override
      public byte[] getKey() throws Exception {
         return this.key;
      }

      protected void clearKey() {
         SecurityUtil.zeroByteArray(this.key);
      }
   }
}
