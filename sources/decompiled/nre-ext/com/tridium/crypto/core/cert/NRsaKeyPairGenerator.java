package com.tridium.crypto.core.cert;

import com.tridium.json.JSONObject;
import com.tridium.nre.security.SecurityInitializer;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public final class NRsaKeyPairGenerator extends NKeyPairGenerator {
   public static final String KEY_ALGORITHM_NAME = "RSA";
   private static final String KEY_SIZE_KEY = "keySize";
   public static final int DEFAULT_KEYSIZE = 2048;
   public static final SortedSet<Integer> RSA_KEYSIZE_SET;
   private final int keySize;

   public NRsaKeyPairGenerator(int keySize) {
      this.keySize = keySize;
   }

   public int getKeySize() {
      return this.keySize;
   }

   @Override
   public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
      KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
      kpGen.initialize(this.keySize);
      return kpGen.generateKeyPair();
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("generatorType", "RSA");
      JSONObject params = new JSONObject();
      params.put("keySize", this.keySize);
      obj.put("parameters", params);
      return obj.toString();
   }

   static NKeyPairGenerator doDecodeFromString(String encoded) throws IOException {
      JSONObject obj = new JSONObject(encoded);
      if (!obj.getString("generatorType").equals("RSA")) {
         throw new IOException("invalid type");
      } else {
         JSONObject parameters = obj.getJSONObject("parameters");
         if (parameters == null) {
            throw new IOException("missing parameters");
         } else {
            return new NRsaKeyPairGenerator(parameters.getInt("keySize"));
         }
      }
   }

   @Override
   public String toString() {
      return this.getClass().getName() + ':' + this.keySize;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else {
         return !(obj instanceof NRsaKeyPairGenerator) ? false : ((NRsaKeyPairGenerator)obj).keySize == this.keySize;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.keySize);
   }

   static {
      SortedSet<Integer> specs = new TreeSet<>();
      if (!SecurityInitializer.getInstance().isFips()) {
         specs.add(1024);
      }

      specs.add(2048);
      specs.add(3072);
      specs.add(4096);
      RSA_KEYSIZE_SET = Collections.unmodifiableSortedSet(specs);
   }
}
