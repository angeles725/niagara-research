package com.tridium.nre.auth;

import com.tridium.nre.security.SecretBytes;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;

public final class ScramClient extends Scram {
   private String password;

   public ScramClient(ScramAlgorithmBundle algorithmBundle, String userName, String password) {
      super(algorithmBundle);

      try {
         this.userName = usernamePrep(userName);
         this.password = passwordPrep(password);
      } catch (Exception e) {
         this.clearData();
         throw new SecurityException();
      }
   }

   public SecretBytes getSaltedPassword() {
      return this.saltedPassword != null ? new SecretBytes(this.saltedPassword, false) : new SecretBytes(0);
   }

   private byte[] _createSaltedPassword(String password, byte[] salt, int iterationCount) throws Exception {
      PasswordHashAlgorithm passwordHashAlgorithm = this.algorithmBundle.getKeyDerivationAlgorithmType();
      switch (passwordHashAlgorithm) {
         case pbkdf2:
            this.saltedPassword = Pbkdf2.deriveKey(salt, iterationCount, password, this.algorithmBundle);
            break;
         case glibc_sha256: {
            String saltedHashPasswordResult = Sha256Crypt.Sha256_crypt(password, new String(salt), iterationCount);
            this.saltedPassword = saltedHashPasswordResult.substring(saltedHashPasswordResult.lastIndexOf("$") + 1).getBytes();
            break;
         }
         case glibc_sha512: {
            String saltedHashPasswordResult = Sha512Crypt.Sha512_crypt(password, new String(salt), iterationCount);
            this.saltedPassword = saltedHashPasswordResult.substring(saltedHashPasswordResult.lastIndexOf("$") + 1).getBytes();
            break;
         }
         case bcrypt: {
            String saltedHashPasswordResult = BCrypt.hashpw(password, "$2$" + iterationCount + "$" + new String(salt));
            this.saltedPassword = saltedHashPasswordResult.substring(saltedHashPasswordResult.lastIndexOf(36) + 23).getBytes();
            break;
         }
         default:
            throw new UnsupportedOperationException("Unknown password hash algorithm '" + passwordHashAlgorithm + "'");
      }

      return this.saltedPassword;
   }

   private byte[] _createClientProof(byte[] saltedPassword, String authMessage) throws NoSuchAlgorithmException {
      byte[] clientKey = this._createClientKey(saltedPassword);
      byte[] storedKey = this.h(clientKey);
      byte[] clientSignature = this.hmac(storedKey, TextUtil.stringToBytes(authMessage));
      return this.xor(clientKey, clientSignature);
   }

   public String createClientFirstMessage() {
      try {
         byte[] nonceVal = new byte[16];
         new SecureRandom().nextBytes(nonceVal);
         this.clientNonce = Base64.getEncoder().encodeToString(nonceVal);
         this.clientFirstMessageBare = this._createClientFirstMessageBare(this.userName, this.clientNonce);
         String clientFirstMessage = "n,," + this.clientFirstMessageBare;
         return "n,," + this.clientFirstMessageBare;
      } catch (Exception var3) {
         this.clearData();
         throw new SecurityException();
      }
   }

   public String createClientFinalMessage(String serverFirstMessage) {
      try {
         Properties values = parseMessage(serverFirstMessage);
         this.iterationCount = Integer.parseInt(values.getProperty("i"));
         this.salt = Base64.getDecoder().decode(values.getProperty("s"));
         String clientNonce = values.getProperty("r").substring(0, this.clientNonce.length());
         if (!SecurityUtil.equals(this.clientNonce, clientNonce)) {
            throw new SecurityException();
         }

         this.serverNonce = values.getProperty("r").substring(this.clientNonce.length());
         this.saltedPassword = this._createSaltedPassword(this.password, this.salt, this.iterationCount);
         this.clientFinalMessageWithoutProof = this._createClientFinalMessageWithoutProof(this.clientNonce, this.serverNonce);
         this.authMessage = this._createAuthMessage(this.clientFirstMessageBare, serverFirstMessage, this.clientFinalMessageWithoutProof);
         byte[] clientProof = this._createClientProof(this.saltedPassword, this.authMessage);
         this.password = null;
         return this.clientFinalMessageWithoutProof + ",p=" + Base64.getEncoder().encodeToString(clientProof);
      } catch (Exception var6) {
         this.clearData();
         throw new SecurityException();
      }
   }

   public void processServerFinalMessage(String serverFinalMessage) {
      try {
         Properties values = parseMessage(serverFinalMessage);
         byte[] serverSignature = this._createServerSignature(this.saltedPassword, this.authMessage);
         byte[] remoteServerSignature = Base64.getDecoder().decode(values.getProperty("v"));
         if (!SecurityUtil.equals(serverSignature, remoteServerSignature)) {
            debug("rejected remoteServerSignature");
            throw new SecurityException();
         }

         debug("accepted remoteServerSignature");
      } catch (Exception var8) {
         throw new SecurityException();
      } finally {
         this.clearData();
      }
   }

   @Override
   protected void clearData() {
      this.password = null;
      super.clearData();
   }

   protected static void debug(String msg) {
   }
}
