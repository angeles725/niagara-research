package com.tridium.nre.security;

import com.tridium.nre.security.km.KeyMaterial;
import com.tridium.nre.security.km.KeyMaterialFactory;
import java.io.File;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;

public class KeyRingFactory {
   private File securityDir = null;
   private KeyRing kr;
   private String keyRingName;
   private String keyMaterialName;
   private static HashMap<KeyRingFactory.KeyRingParameters, KeyRingFactory> factories = new HashMap<>();

   public static KeyRingFactory getInstance(File securityDir, String keyRingName, String keyMaterialName) {
      KeyRingFactory.KeyRingParameters parameters = new KeyRingFactory.KeyRingParameters(securityDir, keyRingName, keyMaterialName);
      KeyRingFactory factory = factories.get(parameters);
      if (factory == null) {
         factory = new KeyRingFactory(securityDir, keyRingName, keyMaterialName);
         factories.put(parameters, factory);
      }

      return factory;
   }

   private KeyRingFactory(File securityDir, String keyRingName, String keyMaterialName) {
      this.securityDir = securityDir;
      this.keyRingName = keyRingName;
      this.keyMaterialName = keyMaterialName;
   }

   public final KeyRing getKeyRing() throws Exception {
      return this.getKeyRing(false);
   }

   public final KeyRing getKeyRing(boolean reload) throws Exception {
      try {
         Permission keyRingPermission = new NiagaraBasicPermission("GET_KEY_RING");
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(keyRingPermission);
         }

         return AccessController.doPrivileged(new KeyRingFactory.GetKeyRingPrivilegedAction(reload));
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   private class GetKeyRingPrivilegedAction implements PrivilegedExceptionAction<KeyRing> {
      private final boolean reload;

      private GetKeyRingPrivilegedAction(boolean reload) {
         this.reload = reload;
      }

      public KeyRing run() throws Exception {
         if (KeyRingFactory.this.kr == null || this.reload) {
            KeyRingFactory.this.securityDir.mkdirs();
            KeyMaterial km = KeyMaterialFactory.getInstance(KeyRingFactory.this.securityDir, KeyRingFactory.this.keyMaterialName).getKeyMaterial();

            try {
               KeyRingFactory.this.kr = new SimpleKeyRing(KeyRingFactory.this.securityDir, KeyRingFactory.this.keyRingName, km);
            } catch (Exception e) {
               if (KeyRingFactory.this.kr == null) {
                  KeyRingFactory.this.kr = new SimpleKeyRing(KeyRingFactory.this.securityDir, KeyRingFactory.this.keyRingName, km, false);
               }

               if (!km.supportsKeyRecovery()
                  || !KeyRingFactory.this.kr.supportsKeyRecovery()
                  || !km.recoveryKeyExists()
                  || !KeyRingFactory.this.kr.recoveryKeyExists()) {
                  KeyRing.LOG.severe("Key ring is corrupt and unrecoverable.");
                  throw new SecurityException("Key ring is corrupt and unrecoverable.");
               }

               km.rollBackRecoveryKey();
               KeyRingFactory.this.kr.rollBackRecoveryKey();
               KeyRingFactory.this.kr.deleteRecoveryKey();
               km.deleteRecoveryKey();
               KeyRingFactory.this.kr.rollKeyMaterial();
            }

            if (km.supportsKeyRecovery() && KeyRingFactory.this.kr.supportsKeyRecovery()) {
               if (km.recoveryKeyExists() && KeyRingFactory.this.kr.recoveryKeyExists()) {
                  km.rollBackRecoveryKey();
                  KeyRingFactory.this.kr.rollBackRecoveryKey();
                  KeyRingFactory.this.kr.deleteRecoveryKey();
                  km.deleteRecoveryKey();
                  KeyRingFactory.this.kr.rollKeyMaterial();
               } else if (km.recoveryKeyExists()) {
                  KeyRingFactory.this.kr.deleteRecoveryKey();
                  km.deleteRecoveryKey();
                  KeyRingFactory.this.kr.rollKeyMaterial();
               }
            }
         }

         return KeyRingFactory.this.kr;
      }
   }

   private static class KeyRingParameters {
      File securityDir;
      String keyRingName;
      String keyMaterialName;

      private KeyRingParameters(File securityDir, String keyRingName, String keyMaterialName) {
         this.securityDir = securityDir;
         this.keyRingName = keyRingName;
         this.keyMaterialName = keyMaterialName;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }

         if (o != null && this.getClass() == o.getClass()) {
            KeyRingFactory.KeyRingParameters that = (KeyRingFactory.KeyRingParameters)o;
            if (this.keyMaterialName != null ? this.keyMaterialName.equals(that.keyMaterialName) : that.keyMaterialName == null) {
               if (this.keyRingName != null ? this.keyRingName.equals(that.keyRingName) : that.keyRingName == null) {
                  return this.securityDir != null ? this.securityDir.equals(that.securityDir) : that.securityDir == null;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.securityDir != null ? this.securityDir.hashCode() : 0;
         result = 31 * result + (this.keyRingName != null ? this.keyRingName.hashCode() : 0);
         return 31 * result + (this.keyMaterialName != null ? this.keyMaterialName.hashCode() : 0);
      }
   }
}
