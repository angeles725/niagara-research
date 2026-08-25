package javax.baja.nre.security;

import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class SharedSecretKey implements AutoCloseable {
   private static final String KEY_VERIFICATION_MESSAGE = "Simplify, then add lightness";
   private final byte[] salt;
   private final byte[] iv;
   private final SecretBytes key;
   private String name;
   private final String aesTransformation;

   public SharedSecretKey(String name, byte[] initializerKey, int keySize) {
      this(name, initializerKey, keySize, "AES/GCM/NoPadding");
   }

   public SharedSecretKey(String name, byte[] initializerKey, int keySize, String aesTransformation) {
      this.name = name;
      this.aesTransformation = aesTransformation;
      this.salt = new byte[16];
      new SecureRandom().nextBytes(this.salt);
      this.iv = new byte[16];
      new SecureRandom().nextBytes(this.iv);
      this.key = this.createKey(initializerKey, keySize);
   }

   public SharedSecretKey(String name, byte[] initializerKey, byte[] salt, byte[] iv, int keySize) {
      this(name, initializerKey, salt, iv, keySize, "AES/GCM/NoPadding");
   }

   public SharedSecretKey(String name, byte[] initializerKey, byte[] salt, byte[] iv, int keySize, String aesTransformation) {
      this.name = name;
      this.salt = salt;
      this.iv = iv;
      this.aesTransformation = aesTransformation;
      this.key = this.createKey(initializerKey, keySize);
   }

   private SecretBytes createKey(byte[] initializerKey, int keySize) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         digest.update(this.salt, 0, this.salt.length);
         digest.update(initializerKey, 0, initializerKey.length);
         byte[] keyBytes = new byte[digest.getDigestLength()];
         keyBytes = digest.digest(keyBytes);
         return new SecretBytes(keyBytes, 0, keySize / 8);
      } catch (NoSuchAlgorithmException e) {
         throw new SecurityException("Required MessageDigest algorithm \"SHA-256\" not implemented by supported Security Providers.", e);
      }
   }

   public byte[] getSalt() {
      return this.salt;
   }

   public byte[] getIV() {
      return this.iv;
   }

   public String getName() {
      return this.name;
   }

   public byte[] getVerificationMessage() throws Exception {
      return Aes256PasswordManager.encrypt("Simplify, then add lightness".getBytes(), this.iv, this.key.get(), this.aesTransformation);
   }

   public void validateVerificationMessage(byte[] message) throws Exception {
      byte[] decryptedMessage = Aes256PasswordManager.decrypt(this.key.get(), message, this.iv, this.aesTransformation);
      if (!"Simplify, then add lightness".equals(new String(decryptedMessage))) {
         throw new SecurityException("Shared secret key validation failed.");
      }
   }

   public byte[] encrypt(SecretBytes value) throws Exception {
      return Aes256PasswordManager.encrypt(value.get(), this.iv, this.key.get(), this.aesTransformation);
   }

   public SecretBytes decrypt(byte[] cipher) throws Exception {
      return Aes256PasswordManager.decryptSecret(this.key.get(), cipher, this.iv, this.aesTransformation);
   }

   public SecretChars decryptChars(byte[] cipher) throws Exception {
      return SecretChars.fromSecretBytes(
         Aes256PasswordManager.decryptSecret(this.key.get(), cipher, this.iv, this.aesTransformation), StandardCharsets.UTF_8, true
      );
   }

   @Override
   public void close() {
      this.key.close();
   }
}
