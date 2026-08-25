package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.cert.CertificateParseException;
import com.tridium.crypto.core.cert.NGeneralName;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;

public final class NSubjectAlternativeName extends NX509Extension {
   private static final NGeneralName[] EMPTY_NAMES_LIST = new NGeneralName[0];
   private final Set<NGeneralName> nNames;

   public static NSubjectAlternativeName make(boolean isCritical, Collection<NGeneralName> names) throws IOException {
      return make(isCritical, names.toArray(EMPTY_NAMES_LIST));
   }

   public static NSubjectAlternativeName make(boolean isCritical, NGeneralName... names) throws IOException {
      GeneralNamesBuilder builder = new GeneralNamesBuilder();

      for (NGeneralName name : names) {
         builder.addName(name.getName());
      }

      Extension ianExt = new Extension(Extension.subjectAlternativeName, isCritical, builder.build().toASN1Primitive().getEncoded("DER"));
      return new NSubjectAlternativeName(ianExt);
   }

   NSubjectAlternativeName(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.subjectAlternativeName)) {
         throw new IllegalArgumentException("extension is not a SubjectAlternativeName extension");
      }

      GeneralName[] names = GeneralNames.fromExtensions(new Extensions(extension), Extension.subjectAlternativeName).getNames();
      if (names == null) {
         throw new IllegalArgumentException("extension isn't a SubjectAlternativeName extension");
      }

      Set<NGeneralName> tnames = new HashSet<>();

      for (GeneralName name : names) {
         tnames.add(NGeneralName.make(name));
      }

      this.nNames = Collections.unmodifiableSet(tnames);
   }

   public Set<NGeneralName> getNames() {
      return this.nNames;
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      JSONArray arr = new JSONArray();

      for (NGeneralName name : this.nNames) {
         arr.put(name.getJSON());
      }

      obj.put("names", arr);
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("oid", this.getOid().getId());
      obj.put("isCritical", this.isCritical());
      JSONObject valObj = new JSONObject();

      for (NGeneralName name : this.nNames) {
         valObj.append("names", new JSONObject(name.encodeToString()));
      }

      obj.put("value", valObj);
      return obj.toString();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      NSubjectAlternativeName ian = (NSubjectAlternativeName)obj;
      return this.nNames.equals(ian.nNames) && this.isCritical() == ian.isCritical();
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), this.isCritical(), this.nNames);
   }

   static NSubjectAlternativeName doDecodeFromString(String val) throws IOException, CertificateParseException {
      try {
         boolean isCritical = false;
         JSONObject obj = new JSONObject(val);
         if (obj.has("oid") && new ASN1ObjectIdentifier(obj.getString("oid")).equals(Extension.subjectAlternativeName)) {
            if (obj.has("isCritical")) {
               isCritical = obj.getBoolean("isCritical");
            }

            JSONObject valObj = obj.getJSONObject("value");
            Set<NGeneralName> names = new HashSet<>();
            JSONArray namesArr = valObj.getJSONArray("names");

            for (int i = 0; i < namesArr.length(); i++) {
               try {
                  NGeneralName name = NGeneralName.decodeFromString(JSONUtil.getString(namesArr, i));
                  names.add(name);
               } catch (Exception e) {
                  throw new CertificateParseException("subjectAlternativeName", new JSONObject(JSONUtil.getString(namesArr, i)).getString("value"));
               }
            }

            return make(isCritical, names);
         }
      } catch (Exception e) {
         if (!(e instanceof IOException) && !(e instanceof CertificateParseException)) {
            throw new IOException("error decoding NSubjectAlternativeName from string", e);
         }

         throw e;
      }

      throw new IOException("error decoding NSubjectAlternativeName from string");
   }

   public NSubjectAlternativeName merge(NSubjectAlternativeName other) throws IOException {
      Set<NGeneralName> mergedNames = new HashSet<>(this.getNames());
      mergedNames.addAll(other.getNames());
      return make(this.isCritical() || other.isCritical(), mergedNames);
   }
}
