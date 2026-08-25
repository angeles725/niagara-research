package com.tridium.crypto.core.cert;

import com.tridium.json.JSONObject;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public abstract class NKeyPairGenerator {
   protected static final String GENERATOR_TYPE_KEY = "generatorType";
   protected static final String PARAMETERS_KEY = "parameters";

   public abstract KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

   public abstract String encodeToString();

   public static NKeyPairGenerator decodeFromString(String encoded) throws IOException {
      JSONObject obj = new JSONObject(encoded);
      String type = obj.getString("generatorType");
      if (type.equals("EC")) {
         return NEcdsaKeyPairGenerator.doDecodeFromString(encoded);
      } else if (type.equals("RSA")) {
         return NRsaKeyPairGenerator.doDecodeFromString(encoded);
      } else {
         throw new IOException("unrecognized generator type : " + type);
      }
   }
}
