package com.tridium.nre.security;

import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.baja.nre.util.ByteArrayUtil;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Aes256PasswordManager {
   public static final String DEFAULT_AES_KEY_ALIAS = "javax.baja.security.BAes256PasswordEncoder.key";
   private KeyRing kr = null;
   private String keyAlias = null;

   public static Aes256PasswordManager getManager(KeyRing kr) {
      return new Aes256PasswordManager(kr, "javax.baja.security.BAes256PasswordEncoder.key");
   }

   public static Aes256PasswordManager getManager(KeyRing kr, String keyAlias) {
      return new Aes256PasswordManager(kr, keyAlias);
   }

   private Aes256PasswordManager(KeyRing kr, String keyAlias) {
      this.kr = kr;
      this.keyAlias = keyAlias;
   }

   public static byte[] encrypt(byte[] passwordBytes, byte[] iv, byte[] key) throws Exception {
      return encrypt(passwordBytes, iv, key, "AES/GCM/NoPadding");
   }

   public static byte[] encrypt(byte[] passwordBytes, byte[] iv, byte[] key, String aesTransformation) throws Exception {
      AlgorithmParameterSpec spec = makeAlgorithmParameterSpec(aesTransformation, iv);
      Cipher aesCipher = Cipher.getInstance(aesTransformation);
      SecretKey aesKey = new SecretKeySpec(key, "AES");
      SecureRandom random = new SecureRandom();
      aesCipher.init(1, aesKey, spec, random);
      byte[] cipher = aesCipher.doFinal(passwordBytes);
      Cipher var9 = null;
      return cipher;
   }

   public byte[] encrypt(String password, String hexIv) throws Exception {
      return this.encrypt(password, hexIv, "AES/GCM/NoPadding");
   }

   public byte[] encrypt(String password, String hexIv, String aesTransformation) throws Exception {
      byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
      byte[] iv = ByteArrayUtil.hexStringToBytes(hexIv);

      try (SecretBytes keyBytes = this.getKey(this.keyAlias)) {
         return encrypt(passwordBytes, iv, keyBytes.get(), aesTransformation);
      }
   }

   public byte[] encrypt(byte[] passwordBytes, byte[] iv) throws Exception {
      return this.encrypt(this.keyAlias, passwordBytes, iv);
   }

   public byte[] encrypt(byte[] passwordBytes, byte[] iv, String aesTransformation) throws Exception {
      return this.encrypt(this.keyAlias, passwordBytes, iv, aesTransformation);
   }

   public byte[] encrypt(String keyName, byte[] passwordBytes, byte[] iv) throws Exception {
      try (SecretBytes keyBytes = this.getKey(keyName)) {
         return encrypt(passwordBytes, iv, keyBytes.get());
      }
   }

   public byte[] encrypt(String keyName, byte[] passwordBytes, byte[] iv, String aesTransformation) throws Exception {
      try (SecretBytes keyBytes = this.getKey(keyName)) {
         return encrypt(passwordBytes, iv, keyBytes.get(), aesTransformation);
      }
   }

   public String decrypt(String hexCipher, String hexIv) throws Exception {
      return this.decrypt(hexCipher, hexIv, "AES/GCM/NoPadding");
   }

   public String decrypt(String hexCipher, String hexIv, String aesTransformation) throws Exception {
      byte[] cipher = ByteArrayUtil.hexStringToBytes(hexCipher);
      byte[] iv = ByteArrayUtil.hexStringToBytes(hexIv);

      try (SecretBytes key = this.getKey(this.keyAlias)) {
         return new String(decrypt(key.get(), cipher, iv, aesTransformation), "UTF-8");
      }
   }

   public SecretBytes decryptSecret(String hexCipher, String hexIv) throws Exception {
      return this.decryptSecret(hexCipher, hexIv, "AES/GCM/NoPadding");
   }

   public SecretBytes decryptSecret(String hexCipher, String hexIv, String aesTransformation) throws Exception {
      byte[] cipher = ByteArrayUtil.hexStringToBytes(hexCipher);
      byte[] iv = ByteArrayUtil.hexStringToBytes(hexIv);

      try (SecretBytes key = this.getKey(this.keyAlias)) {
         return new SecretBytes(decrypt(key.get(), cipher, iv, aesTransformation), true);
      }
   }

   public SecretBytes decryptSecret(byte[] cipher, byte[] iv) throws Exception {
      return this.decryptSecret(cipher, iv, "AES/GCM/NoPadding");
   }

   public SecretBytes decryptSecret(byte[] cipher, byte[] iv, String aesTransformation) throws Exception {
      return this.decryptSecret(this.keyAlias, cipher, iv, aesTransformation);
   }

   public SecretBytes decryptSecret(String keyName, byte[] cipher, byte[] iv) throws Exception {
      return this.decryptSecret(keyName, cipher, iv, "AES/GCM/NoPadding");
   }

   public SecretBytes decryptSecret(String keyName, byte[] cipher, byte[] iv, String aesTransformation) throws Exception {
      try (SecretBytes key = this.getKey(keyName)) {
         return new SecretBytes(decrypt(key.get(), cipher, iv, aesTransformation), true);
      }
   }

   public static SecretBytes decryptSecret(byte[] key, byte[] cipher, byte[] iv) throws Exception {
      return decryptSecret(key, cipher, iv, "AES/GCM/NoPadding");
   }

   public static SecretBytes decryptSecret(byte[] key, byte[] cipher, byte[] iv, String aesTransformation) throws Exception {
      return new SecretBytes(decrypt(key, cipher, iv, aesTransformation), true);
   }

   public static byte[] decrypt(byte[] key, byte[] cipher, byte[] iv) throws Exception {
      return decrypt(key, cipher, iv, "AES/GCM/NoPadding");
   }

   public static byte[] decrypt(byte[] key, byte[] cipher, byte[] iv, String aesTransformation) throws Exception {
      AlgorithmParameterSpec spec = makeAlgorithmParameterSpec(aesTransformation, iv);
      Cipher aesCipher = Cipher.getInstance(aesTransformation);
      SecretKey aesKey = new SecretKeySpec(key, "AES");
      SecureRandom random = new SecureRandom();
      aesCipher.init(2, aesKey, spec, random);
      byte[] passwordBytes = aesCipher.doFinal(cipher);
      Cipher var9 = null;
      return passwordBytes;
   }

   public static byte[] transcode(
      byte[] cipher, byte[] iv, byte[] sourceKey, byte[] destinationKey, String sourceAesTransformation, String destAesTransformation
   ) {
      byte[] newCipher = null;

      try (SecretBytes secret = decryptSecret(sourceKey, cipher, iv, sourceAesTransformation)) {
         newCipher = encrypt(secret.get(), iv, destinationKey, destAesTransformation);
      } catch (Exception var20) {
      }

      return newCipher;
   }

   public byte[] transcodeFromKeyring(String keyName, byte[] cipher, byte[] iv, byte[] targetKey, String sourceAesTransformation, String destAesTransformation) {
      byte[] newCipher = null;

      try (SecretBytes secret = this.decryptSecret(keyName, cipher, iv, sourceAesTransformation)) {
         newCipher = encrypt(secret.get(), iv, targetKey, destAesTransformation);
      } catch (AccessControlException e) {
         throw e;
      } catch (Exception var23) {
      }

      return newCipher;
   }

   public byte[] transcodeFromKeyring(byte[] cipher, byte[] iv, byte[] targetKey, String sourceAesTransformation, String destAesTransformation) {
      return this.transcodeFromKeyring(this.keyAlias, cipher, iv, targetKey, sourceAesTransformation, destAesTransformation);
   }

   public byte[] transcodeToKeyring(String keyName, byte[] cipher, byte[] iv, byte[] sourceKey, String sourceAesTransformation, String destAesTransformation) {
      byte[] newCipher = null;

      try {
         newCipher = this.encrypt(keyName, decrypt(sourceKey, cipher, iv, sourceAesTransformation), iv, destAesTransformation);
      } catch (Exception var9) {
      }

      return newCipher;
   }

   public byte[] transcodeToKeyring(byte[] cipher, byte[] iv, byte[] sourceKey, String sourceAesTransformation, String destAesTransformation) {
      return this.transcodeToKeyring(this.keyAlias, cipher, iv, sourceKey, sourceAesTransformation, destAesTransformation);
   }

   private SecretBytes getKey(String keyName) throws SecurityException {
      try {
         byte[] key = this.kr.getKey(keyName);
         if (key == null) {
            key = this.kr.createKey(keyName, false);
         }

         return new SecretBytes(key, true);
      } catch (AccessControlException ace) {
         throw ace;
      } catch (Exception e) {
         throw new SecurityException("Cannot find key to use for AES encryption");
      }
   }

   private static AlgorithmParameterSpec makeAlgorithmParameterSpec(String aesTransformation, byte[] iv) {
      switch (aesTransformation) {
         case "AES/GCM/NoPadding":
            return new GCMParameterSpec(128, iv);
         case "AES/CBC/PKCS5Padding":
            return new IvParameterSpec(iv);
         default:
            throw new InvalidParameterException("Invalid AES transformation: " + aesTransformation);
      }
   }
}
