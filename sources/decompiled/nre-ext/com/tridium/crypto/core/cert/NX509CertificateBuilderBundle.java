package com.tridium.crypto.core.cert;

import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import com.tridium.nre.security.SecretChars;
import java.io.IOException;

public class NX509CertificateBuilderBundle {
   private static final String BUILDER_KEY = "builder";
   private static final String KEY_GENERATOR_KEY = "keyGenerator";
   private static final String PASSWORD_KEY = "password";
   private final NX509CertificateBuilder builder;
   private final NKeyPairGenerator generator;
   private final SecretChars password;

   public NX509CertificateBuilderBundle(NX509CertificateBuilder builder, NKeyPairGenerator generator, SecretChars password) {
      this.builder = builder;
      this.generator = generator;
      this.password = password;
   }

   public NX509CertificateBuilder getBuilder() {
      return this.builder;
   }

   public NKeyPairGenerator getGenerator() {
      return this.generator;
   }

   public SecretChars getPassword() {
      return this.password;
   }

   public static NX509CertificateBuilderBundle decodeFromString(String encoded) throws IOException, CertificateParseException {
      try {
         JSONObject object = new JSONObject(encoded);
         NX509CertificateBuilder builder = NX509CertificateBuilder.decodeFromString(JSONUtil.getString(object, "builder"));
         NKeyPairGenerator generator = object.has("keyGenerator") ? NKeyPairGenerator.decodeFromString(JSONUtil.getString(object, "keyGenerator")) : null;
         SecretChars password = object.has("password") ? SecretChars.fromString(object.getString("password")) : null;
         return new NX509CertificateBuilderBundle(builder, generator, password);
      } catch (CertificateParseException e) {
         throw e;
      } catch (Exception e) {
         if (e instanceof IOException) {
            throw e;
         } else {
            throw new IOException(e);
         }
      }
   }

   public String encodeToString() throws IOException {
      JSONObject object = new JSONObject();
      object.put("builder", new JSONObject(this.builder.encodeToString()));
      if (this.generator != null) {
         object.put("keyGenerator", new JSONObject(this.generator.encodeToString()));
      }

      if (this.password != null) {
         object.put("password", this.password.asString(false));
      }

      return object.toString();
   }
}
