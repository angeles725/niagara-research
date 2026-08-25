package com.tridium.crypto.core.cert;

import com.tridium.json.JSONObject;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.ECGenParameterSpec;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class NEcdsaKeyPairGenerator extends NKeyPairGenerator {
   public static final String KEY_ALGORITHM_NAME = "EC";
   private static final String SPEC_KEY = "spec";
   public static final String DEFAULT_KEYSPEC = "P-256";
   public static final Set<String> ECDSA_KEYSPEC_SET;
   private final String spec;

   public NEcdsaKeyPairGenerator(String spec) {
      if (spec != null && !spec.trim().isEmpty()) {
         this.spec = spec;
      } else {
         throw new IllegalArgumentException("spec cannot be blank");
      }
   }

   public String getSpec() {
      return this.spec;
   }

   @Override
   public KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
      KeyPairGenerator kpGen = KeyPairGenerator.getInstance("EC");
      kpGen.initialize(new ECGenParameterSpec(this.spec));
      return kpGen.generateKeyPair();
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("generatorType", "EC");
      JSONObject parameters = new JSONObject();
      parameters.put("spec", this.spec);
      obj.put("parameters", parameters);
      return obj.toString();
   }

   static NKeyPairGenerator doDecodeFromString(String encoded) throws IOException {
      JSONObject obj = new JSONObject(encoded);
      if (!obj.getString("generatorType").equals("EC")) {
         throw new IOException("invalid type");
      } else {
         JSONObject parameters = obj.getJSONObject("parameters");
         if (parameters == null) {
            throw new IOException("missing parameters");
         } else {
            return new NEcdsaKeyPairGenerator(parameters.getString("spec"));
         }
      }
   }

   @Override
   public String toString() {
      return this.getClass().getName() + ':' + this.spec;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else {
         return !(obj instanceof NEcdsaKeyPairGenerator) ? false : this.spec.equals(((NEcdsaKeyPairGenerator)obj).spec);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.spec);
   }

   static {
      Set<String> specs = new HashSet<>();
      specs.add("P-256");
      specs.add("P-384");
      specs.add("P-521");
      specs.add("brainpoolP256r1");
      specs.add("brainpoolP320r1");
      specs.add("brainpoolP384r1");
      specs.add("brainpoolP512r1");
      ECDSA_KEYSPEC_SET = Collections.unmodifiableSet(specs);
   }
}
