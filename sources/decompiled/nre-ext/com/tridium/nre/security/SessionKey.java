package com.tridium.nre.security;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.baja.nre.security.SharedSecretKey;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

public final class SessionKey implements HttpSessionBindingListener {
   private SecretBytes sessionKey;
   private EncryptionAlgorithmBundle encryptionAlgorithmBundle;

   public SessionKey(SecretBytes sessionKey, EncryptionAlgorithmBundle encryptionAlgorithmBundle) {
      this(sessionKey, encryptionAlgorithmBundle, true);
   }

   public SessionKey(SecretBytes sessionKey, EncryptionAlgorithmBundle encryptionAlgorithmBundle, boolean clearSessionKeyParameter) {
      this.sessionKey = sessionKey.newCopy();
      this.encryptionAlgorithmBundle = encryptionAlgorithmBundle;
      if (clearSessionKeyParameter) {
         sessionKey.close();
      }
   }

   public static SessionKey make(byte[] password, byte[] salt) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         digest.update(salt, 0, salt.length);
         digest.update(password, 0, password.length);
         byte[] keyBytes = new byte[digest.getDigestLength()];
         keyBytes = digest.digest(keyBytes);
         return new SessionKey(new SecretBytes(keyBytes, true), null, true);
      } catch (NoSuchAlgorithmException e) {
         throw new SecurityException("Required MessageDigest algorithm \"SHA-256\" not implemented by supported Security Providers.", e);
      }
   }

   public SharedSecretKey generateSharedSecret(String name) {
      if (this.encryptionAlgorithmBundle == null) {
         throw new IllegalStateException("EncryptionAlgorithmBundle has not been initialized.");
      } else {
         return new SharedSecretKey(name, this.sessionKey.get(), this.encryptionAlgorithmBundle.getKeySize(), this.getAesTransformation());
      }
   }

   public SharedSecretKey generateSharedSecret(String name, byte[] salt, byte[] iv) {
      if (this.encryptionAlgorithmBundle == null) {
         throw new IllegalStateException("EncryptionAlgorithmBundle has not been initialized.");
      } else {
         return new SharedSecretKey(name, this.sessionKey.get(), salt, iv, this.encryptionAlgorithmBundle.getKeySize(), this.getAesTransformation());
      }
   }

   private String getAesTransformation() {
      return this.encryptionAlgorithmBundle instanceof AesAlgorithmBundle
         ? ((AesAlgorithmBundle)this.encryptionAlgorithmBundle).getAesTransformation()
         : "AES/GCM/NoPadding";
   }

   public void valueBound(HttpSessionBindingEvent event) {
      try {
         NiagaraBasicPermission setSessionKeyPermission = new NiagaraBasicPermission("SET_HTTP_SESSION_KEY");
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(setSessionKeyPermission);
         }
      } catch (AccessControlException e) {
         AccessController.doPrivileged(() -> {
            event.getSession().setAttribute(event.getName(), event.getValue());
            return null;
         });
      }
   }

   public void valueUnbound(HttpSessionBindingEvent event) {
      try {
         NiagaraBasicPermission setSessionKeyPermission = new NiagaraBasicPermission("SET_HTTP_SESSION_KEY");
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(setSessionKeyPermission);
         }
      } catch (AccessControlException e) {
         AccessController.doPrivileged(() -> {
            event.getSession().setAttribute(event.getName(), event.getValue());
            return null;
         });
      }
   }

   public EncryptionAlgorithmBundle getEncryptionAlgorithmBundle() {
      return this.encryptionAlgorithmBundle;
   }

   public void setEncryptionAlgorithmBundle(EncryptionAlgorithmBundle encryptionAlgorithmBundle) {
      this.encryptionAlgorithmBundle = encryptionAlgorithmBundle;
   }

   public void clearData() {
      this.sessionKey.close();
   }
}
