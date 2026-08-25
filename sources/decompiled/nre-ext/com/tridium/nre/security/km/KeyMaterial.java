package com.tridium.nre.security.km;

import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.RecoverableKeyData;
import com.tridium.nre.security.SecurityConstants;
import java.security.AccessController;
import java.security.Permission;
import java.util.logging.Logger;

public abstract class KeyMaterial extends RecoverableKeyData {
   private static boolean SIMPLE_KM_WARNING_PRINTED = false;
   public static final String SIMPLE_KEY_MATERIAL_NAME = ".km";
   public static final String LEGACY_KEY_MATERIAL_NAME = ".km";
   public static final Logger LOG = Logger.getLogger("security.keyMaterial");

   public byte[] getKeyMaterial() throws Exception {
      checkKeyMaterialPermissions();
      return this.doGetKeyMaterial();
   }

   public boolean setKeyMaterial(byte[] km) throws Exception {
      checkKeyMaterialPermissions();
      return this.doSetKeyMaterial(km);
   }

   public boolean refresh() throws Exception {
      checkKeyMaterialPermissions();
      return this.doRefreshKeyMaterial();
   }

   final boolean keyExists() throws Exception {
      checkKeyMaterialPermissions();
      byte[] km = this.getKeyMaterial();
      return km != null && km.length > 0;
   }

   public long getLastModified() throws Exception {
      checkKeyMaterialPermissions();
      return this.doGetLastModified();
   }

   public static void checkKeyMaterialPermissions() {
      Permission keyMaterialPermission = new NiagaraBasicPermission("KEY_MATERIAL");
      SecurityManager sm = System.getSecurityManager();

      try {
         if (sm != null) {
            sm.checkPermission(keyMaterialPermission);
         }
      } catch (NullPointerException npe) {
         throw new SecurityException("Cannot find NiagaraBasicPermission.KEY_MATERIAL");
      }
   }

   public static boolean usingSimpleKeyMaterial() {
      if (KeyMaterial.LocalMetaDataHolder.NIAGARA_USE_SIMPLE_KM && SecurityConstants.canCheckTpk()) {
         printSimpleKeyMaterialWarning();
      }

      return KeyMaterial.LocalMetaDataHolder.NIAGARA_USE_SIMPLE_KM;
   }

   private static void printSimpleKeyMaterialWarning() {
      if (!SIMPLE_KM_WARNING_PRINTED) {
         synchronized (System.err) {
            System.err.println("********************************************************************");
            System.err.println("**** WARNING: USING SIMPLE KEY MATERIAL, NOT FOR PRODUCTION USE ****");
            System.err.println("********************************************************************");
         }

         SIMPLE_KM_WARNING_PRINTED = true;
      }
   }

   public static boolean usingNativeKeyMaterial() {
      return KeyMaterial.LocalMetaDataHolder.NIAGARA_USE_NATIVE_KEY_MATERIAL;
   }

   abstract byte[] doGetKeyMaterial() throws Exception;

   abstract boolean doSetKeyMaterial(byte[] var1) throws Exception;

   abstract boolean doRefreshKeyMaterial() throws Exception;

   abstract long doGetLastModified() throws Exception;

   private static final class LocalMetaDataHolder {
      private static final boolean NIAGARA_USE_NATIVE_KEY_MATERIAL = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.use.native.key.material"));
      private static final boolean NIAGARA_USE_SIMPLE_KM = AccessController.doPrivileged(() -> System.getenv("NIAGARA_USE_SIMPLE_KM")) != null;
   }
}
