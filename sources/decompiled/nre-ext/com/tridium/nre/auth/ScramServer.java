package com.tridium.nre.auth;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;

public final class ScramServer extends Scram {
   private ScramServer.IUserKeyFactory userKeyFactory = null;
   private boolean authenticated = false;

   public ScramServer(ScramAlgorithmBundle algorithmBundle, ScramServer.IUserKeyFactory userKeyFactory) {
      super(algorithmBundle);
      if (userKeyFactory == null) {
         throw new SecurityException();
      }

      this.userKeyFactory = userKeyFactory;
   }

   private String _createServerFirstMessage(String clientNonce, String serverNonce, byte[] salt, int iterationCount) {
      return "r=" + clientNonce + serverNonce + ",s=" + Base64.getEncoder().encodeToString(salt) + ",i=" + iterationCount;
   }

   private byte[] _createClientSignature(byte[] saltedPassword, String authMessage) throws NoSuchAlgorithmException {
      byte[] clientKey = this._createClientKey(saltedPassword);
      byte[] storedKey = this.h(clientKey);
      return this.hmac(storedKey, TextUtil.stringToBytes(authMessage));
   }

   private static String _validateUserName(String userName) throws Exception {
      if (userName.contains(",")) {
         throw new IllegalArgumentException("invalid character ',' in username");
      }

      for (int index = userName.indexOf("="); index >= 0; index = userName.indexOf("=", index + 1)) {
         String nextChars = userName.substring(index + 1, index + 3).toLowerCase();
         if (!nextChars.equals("2c") && !nextChars.equals("3d")) {
            throw new IllegalArgumentException("invalid character '=' in username");
         }
      }

      userName = TextUtil.replace(userName, "=2c", ",");
      userName = TextUtil.replace(userName, "=2C", ",");
      userName = TextUtil.replace(userName, "=3d", "=");
      return TextUtil.replace(userName, "=3D", "=");
   }

   public String createServerFirstMessage(String clientFirstMessage) {
      try {
         if (!clientFirstMessage.startsWith("n,,")) {
            throw new IllegalArgumentException("The server only supports the GS2 'n' flag");
         }

         Properties clientProps = parseMessage(clientFirstMessage.substring(3));
         this.userName = clientProps.getProperty("n");
         this.clientNonce = clientProps.getProperty("r");
         this.userName = _validateUserName(this.userName);
         byte[] nonceVal = new byte[16];
         new SecureRandom().nextBytes(nonceVal);
         this.serverNonce = Base64.getEncoder().encodeToString(nonceVal);
         String userKey = this.userKeyFactory.getUserKey(this.userName);
         if (userKey == null) {
            throw new SecurityException();
         }

         String[] userKeyValues = TextUtil.split(userKey, ':');
         this.salt = ByteArrayUtil.hexStringToBytes(userKeyValues[0]);
         this.iterationCount = Integer.parseInt(userKeyValues[1]);
         this.saltedPassword = ByteArrayUtil.hexStringToBytes(userKeyValues[2]);
         this.serverFirstMessage = this._createServerFirstMessage(this.clientNonce, this.serverNonce, this.salt, this.iterationCount);
         return this.serverFirstMessage;
      } catch (Exception var6) {
         this.clearData();
         throw new SecurityException();
      }
   }

   public String createServerFinalMessage(String clientFinalMessage) {
      try {
         Properties clientProps = parseMessage(clientFinalMessage);
         if (!SecurityUtil.equals(clientProps.getProperty("r"), this.clientNonce + this.serverNonce)) {
            throw new SecurityException();
         }

         this.clientFirstMessageBare = this._createClientFirstMessageBare(usernamePrep(this.userName), this.clientNonce);
         this.clientFinalMessageWithoutProof = this._createClientFinalMessageWithoutProof(this.clientNonce, this.serverNonce);
         this.authMessage = this._createAuthMessage(this.clientFirstMessageBare, this.serverFirstMessage, this.clientFinalMessageWithoutProof);
         byte[] clientSignature = this._createClientSignature(this.saltedPassword, this.authMessage);
         byte[] clientProof = Base64.getDecoder().decode(clientProps.getProperty("p"));
         byte[] calculatedClientKey = this.xor(clientProof, clientSignature);
         byte[] clientKey = this._createClientKey(this.saltedPassword);
         if (SecurityUtil.equals(clientKey, calculatedClientKey)) {
            byte[] serverSignature = this._createServerSignature(this.saltedPassword, this.authMessage);
            String serverFinalMessage = "v=" + Base64.getEncoder().encodeToString(serverSignature);
            this.authenticated = true;
            return serverFinalMessage;
         }
      } catch (Exception var13) {
      } finally {
         this.clearData();
      }

      throw new SecurityException();
   }

   public static String extractUsername(String clientMessage) {
      int index = 0;
      if (!clientMessage.startsWith("n,,")) {
         index = 3;
      }

      Properties clientProps = parseMessage(clientMessage.substring(index));

      try {
         return _validateUserName(clientProps.getProperty("n"));
      } catch (Exception e) {
         return clientProps.getProperty("n");
      }
   }

   @Override
   protected void clearData() {
      this.userKeyFactory = null;
      super.clearData();
   }

   protected static void debug(String msg) {
   }

   public boolean isAuthenticated() {
      return this.authenticated;
   }

   public interface IUserKeyFactory {
      String getUserKey(String var1);
   }
}
