package com.tridium.nre.auth;

import com.tridium.nre.util.Normalizer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import javax.baja.nre.util.TextUtil;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public abstract class Scram {
   protected static final boolean DEBUG = false;
   protected String userName = null;
   protected byte[] salt = null;
   protected int iterationCount = 0;
   protected byte[] saltedPassword = null;
   protected String clientNonce = null;
   protected String serverNonce = null;
   protected String clientFirstMessageBare = null;
   protected String serverFirstMessage = null;
   protected String clientFinalMessageWithoutProof = null;
   protected String authMessage = null;
   protected ScramAlgorithmBundle algorithmBundle = null;

   protected Scram(ScramAlgorithmBundle algorithmBundle) {
      this.algorithmBundle = algorithmBundle;
   }

   protected final String _createClientFirstMessageBare(String userName, String clientNonce) {
      return "n=" + userName + ",r=" + clientNonce;
   }

   protected final String _createClientFinalMessageWithoutProof(String clientNonce, String serverNonce) {
      return "c=biws,r=" + clientNonce + serverNonce;
   }

   protected final String _createAuthMessage(String clientFirstMessageBare, String serverFirstMessage, String clientFinalMessageWithoutProof) {
      return clientFirstMessageBare + "," + serverFirstMessage + "," + clientFinalMessageWithoutProof;
   }

   protected final byte[] hmac(byte[] keyBytes, byte[] dataBytes) {
      try {
         Mac mac = Mac.getInstance(this.algorithmBundle.getMacAlgorithmName());
         SecretKey macKey = null;
         if (keyBytes.length > 0) {
            macKey = new SecretKeySpec(keyBytes, this.algorithmBundle.getSecretKeySpecAlgorithmName());
         }

         mac.init(macKey);
         return mac.doFinal(dataBytes);
      } catch (Exception e) {
         throw new SecurityException("Could not get hmac: " + e);
      }
   }

   protected final byte[] _createClientKey(byte[] saltedPassword) {
      return this.hmac(saltedPassword, TextUtil.stringToBytes("Client Key"));
   }

   protected final byte[] _createServerSignature(byte[] saltedPassword, String authMessage) {
      byte[] serverKey = this.hmac(saltedPassword, TextUtil.stringToBytes("Server Key"));
      return this.hmac(serverKey, TextUtil.stringToBytes(authMessage));
   }

   public final String getUserName() {
      return this.userName;
   }

   protected void clearData() {
      this.userName = null;
      this.salt = null;
      this.iterationCount = 0;
      this.saltedPassword = null;
      this.clientNonce = null;
      this.serverNonce = null;
      this.clientFirstMessageBare = null;
      this.serverFirstMessage = null;
      this.clientFinalMessageWithoutProof = null;
      this.authMessage = null;
   }

   protected static void debug(String msg) {
   }

   public static String passwordPrep(String input) throws Exception {
      return Normalizer.normalize(input, 5);
   }

   public static String usernamePrep(String input) throws Exception {
      String value = Normalizer.normalize(input, 5);
      value = TextUtil.replace(value, "=", "=3D");
      return TextUtil.replace(value, ",", "=2C");
   }

   protected final byte[] h(byte[] src) throws NoSuchAlgorithmException {
      MessageDigest messageDigest = MessageDigest.getInstance(this.algorithmBundle.getMessageDigestAlgorithmName());
      messageDigest.update(src);
      return messageDigest.digest();
   }

   public final byte[] xor(byte[] left, byte[] right) throws NullPointerException, IllegalArgumentException {
      if (left != null && right != null) {
         if (left.length != right.length) {
            throw new IllegalArgumentException();
         }

         byte[] result = new byte[left.length];

         for (int i = 0; i < left.length; i++) {
            result[i] = (byte)(left[i] ^ right[i]);
         }

         return result;
      } else {
         throw new NullPointerException();
      }
   }

   public static Properties parseMessage(String message) {
      Properties props = new Properties();
      String[] components = TextUtil.split(message, ',');

      for (String component : components) {
         if (component.length() > 0 && component.indexOf("=") > 0) {
            int eq = component.indexOf("=");
            String key = component.substring(0, eq);
            String value = component.substring(eq + 1);
            props.put(key, value);
         }
      }

      return props;
   }
}
