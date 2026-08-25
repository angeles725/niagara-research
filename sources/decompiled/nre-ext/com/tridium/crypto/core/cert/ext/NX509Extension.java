package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.cert.CertificateParseException;
import com.tridium.crypto.core.util.BouncyCastleHelper;
import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.IX509Extension;
import javax.baja.nre.util.ByteArrayUtil;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

public class NX509Extension implements IX509Extension {
   protected static final Logger logger = Logger.getLogger("crypto.cert");
   protected Extension extension;

   public static IX509Extension make(ASN1ObjectIdentifier oid, boolean isCritical, byte[] octets) throws IOException {
      byte[] t = JcaX509ExtensionUtils.parseExtensionValue(octets).toASN1Primitive().getEncoded("DER");
      Extension extension = new Extension(oid, isCritical, new DEROctetString(t));
      return make(extension);
   }

   public static IX509Extension make(Extension extension) throws IOException {
      try {
         ASN1ObjectIdentifier oid = extension.getExtnId();
         if (oid.equals(Extension.basicConstraints)) {
            return new NBasicConstraints(extension);
         }

         if (oid.equals(Extension.authorityKeyIdentifier)) {
            return new NAuthorityKeyIdentifier(extension);
         }

         if (oid.equals(Extension.extendedKeyUsage)) {
            return new NExtendedKeyUsage(extension);
         }

         if (oid.equals(Extension.issuerAlternativeName)) {
            return new NIssuerAlternativeName(extension);
         }

         if (oid.equals(Extension.keyUsage)) {
            return new NKeyUsage(extension);
         }

         if (oid.equals(Extension.subjectAlternativeName)) {
            return new NSubjectAlternativeName(extension);
         }

         if (oid.equals(Extension.subjectKeyIdentifier)) {
            return new NSubjectKeyIdentifier(extension);
         }

         if (oid.equals(Extension.inhibitAnyPolicy)) {
            return new NInhibitAnyPolicy(extension);
         }

         if (oid.equals(Extension.policyConstraints)) {
            return new NPolicyConstraints(extension);
         }

         if (oid.equals(Extension.authorityInfoAccess)) {
            return new NAuthorityInfoAccess(extension);
         }

         if (oid.equals(Extension.certificatePolicies)) {
            return new NCertificatePolicies(extension);
         }

         if (oid.equals(Extension.policyMappings)) {
            return new NPolicyMappings(extension);
         }

         if (oid.equals(Extension.privateKeyUsagePeriod)) {
            return new NPrivateKeyUsagePeriod(extension);
         }

         if (oid.equals(Extension.subjectInfoAccess)) {
            return new NSubjectInfoAccess(extension);
         }

         if (oid.equals(Extension.nameConstraints)) {
            return new NNameConstraints(extension);
         }

         if (oid.equals(Extension.cRLDistributionPoints)) {
            return new NCRLDistributionPoints(extension);
         }

         if (oid.equals(Extension.freshestCRL)) {
            return new NFreshestCRL(extension);
         }

         if (oid.equals(Extension.subjectDirectoryAttributes)) {
            return new NSubjectDirectoryAttributes(extension);
         }
      } catch (Exception e) {
         logger.log(Level.WARNING, "error parsing extension", e);
         if (e instanceof IOException) {
            throw e;
         }

         throw new IOException("error parsing extension", e);
      }

      return new NX509Extension(extension);
   }

   public static IX509Extension decodeFromString(String val) throws IOException, CertificateParseException {
      try {
         JSONObject obj = new JSONObject(val);
         ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(obj.getString("oid"));
         if (oid.equals(Extension.basicConstraints)) {
            return NBasicConstraints.doDecodeFromString(val);
         }

         if (oid.equals(Extension.authorityKeyIdentifier)) {
            return NAuthorityKeyIdentifier.doDecodeFromString(val);
         }

         if (oid.equals(Extension.extendedKeyUsage)) {
            return NExtendedKeyUsage.doDecodeFromString(val);
         }

         if (oid.equals(Extension.issuerAlternativeName)) {
            return NIssuerAlternativeName.doDecodeFromString(val);
         }

         if (oid.equals(Extension.keyUsage)) {
            return NKeyUsage.doDecodeFromString(val);
         }

         if (oid.equals(Extension.subjectAlternativeName)) {
            return NSubjectAlternativeName.doDecodeFromString(val);
         }

         if (oid.equals(Extension.subjectKeyIdentifier)) {
            return NSubjectKeyIdentifier.doDecodeFromString(val);
         }

         if (oid.equals(Extension.cRLDistributionPoints)) {
            return NCRLDistributionPoints.doDecodeFromString(val);
         }

         boolean isCritical = false;
         if (obj.has("isCritical")) {
            isCritical = obj.getBoolean("isCritical");
         }

         JSONObject valObj = obj.getJSONObject("value");
         byte[] encoded = ByteArrayUtil.hexStringToBytes(valObj.getString("extension"));
         Extension ext = new Extension(oid, isCritical, encoded);
         return make(ext);
      } catch (Exception e) {
         logger.log(Level.WARNING, "error decoding extension from string", e);
         if (!(e instanceof IOException) && !(e instanceof CertificateParseException)) {
            throw new IOException("error decoding extension from string", e);
         } else {
            throw e;
         }
      }
   }

   protected NX509Extension(Extension extension) {
      this.extension = extension;
   }

   @Override
   public final String getIdentifier() {
      return OidMap.get(this.extension.getExtnId().getId());
   }

   @Override
   public final ASN1ObjectIdentifier getOid() {
      return this.extension.getExtnId();
   }

   @Override
   public final boolean isCritical() {
      return this.extension.isCritical();
   }

   @Override
   public final Extension getExtension() {
      return this.extension;
   }

   @Override
   public final String getJSON() {
      return this.getJSONObject().toString();
   }

   public JSONObject getJSONObject() {
      JSONObject obj = new JSONObject();
      obj.put("isCritical", this.isCritical());
      obj.put("oid", OidMap.get(this.getOid()));

      try {
         JSONObject valObj = new JSONObject();
         this.appendJSON(valObj);
         obj.put("value", valObj);
      } catch (Exception e) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.WARNING, "Error creating JSON for extension");
         } else {
            logger.log(Level.WARNING, "Error creating JSON for extension", e);
         }

         obj.append("error", "parsing error");
      }

      return obj;
   }

   protected void appendJSON(JSONObject obj) {
      this.parsePrimitive(this.extension.getExtnValue().getLoadedObject(), obj);
   }

   protected void parsePrimitive(ASN1Primitive obj, JSONObject parent) {
      if (obj instanceof ASN1BitString) {
         parent.put("bitString", Integer.toHexString(((ASN1BitString)obj).intValue()));
      } else if (obj instanceof ASN1String) {
         parent.put("string", ((ASN1String)obj).getString());
      } else if (obj instanceof ASN1UTCTime) {
         parent.put("utcTime", ((ASN1UTCTime)obj).getTime());
      } else if (obj instanceof ASN1GeneralizedTime) {
         parent.put("generalizedTime", ((ASN1GeneralizedTime)obj).getTime());
      } else if (obj instanceof ASN1ObjectIdentifier) {
         parent.put("oid", OidMap.get(((ASN1ObjectIdentifier)obj).getId()));
      } else if (obj instanceof ASN1Integer) {
         parent.put("integer", ((ASN1Integer)obj).getValue().toString());
      } else if (obj instanceof ASN1Boolean) {
         parent.put("boolean", ((ASN1Boolean)obj).isTrue());
      } else if (obj instanceof ASN1TaggedObject) {
         JSONObject taggedObj = new JSONObject();
         parent.put("tagged", taggedObj);
         this.parsePrimitive(BouncyCastleHelper.getASN1TaggedObjectPrimitive((ASN1TaggedObject)obj), taggedObj);
      } else if (obj instanceof ASN1Sequence) {
         JSONArray arr = new JSONArray();
         parent.put("sequence", arr);
         Enumeration<?> objs = ((ASN1Sequence)obj).getObjects();
         this.walkASN1Enumeration(objs, arr);
      } else if (obj instanceof ASN1Set) {
         JSONArray arr = new JSONArray();
         parent.put("set", arr);
         Enumeration<?> objs = ((ASN1Set)obj).getObjects();
         this.walkASN1Enumeration(objs, arr);
      } else if (obj instanceof ASN1OctetString) {
         try {
            ASN1InputStream aIn = new ASN1InputStream(((ASN1OctetString)obj).getOctetStream());
            Throwable var23 = null;

            try {
               JSONObject octetObj = new JSONObject();
               this.parsePrimitive(aIn.readObject(), octetObj);
               parent.put("octet", octetObj);
            } catch (Throwable var16) {
               var23 = var16;
               throw var16;
            } finally {
               if (aIn != null) {
                  if (var23 != null) {
                     try {
                        aIn.close();
                     } catch (Throwable var14) {
                        var23.addSuppressed(var14);
                     }
                  } else {
                     aIn.close();
                  }
               }
            }
         } catch (Exception e) {
            parent.put("octet", ByteArrayUtil.toHexString(((ASN1OctetString)obj).getOctets()));
         }
      } else {
         try {
            parent.put("rawValue", ByteArrayUtil.toHexString(obj.getEncoded("DER")));
         } catch (IOException ioe) {
            parent.put("err", "unable to parse ASN1 object");
         }
      }
   }

   private void walkASN1Enumeration(Enumeration<?> objs, JSONArray parentArray) {
      while (objs.hasMoreElements()) {
         Object o = objs.nextElement();
         JSONObject jsonObject = new JSONObject();
         if (o instanceof ASN1Primitive) {
            this.parsePrimitive((ASN1Primitive)o, jsonObject);
            parentArray.put(jsonObject);
         } else if (o instanceof ASN1Object) {
            this.parsePrimitive(((ASN1Object)o).toASN1Primitive(), jsonObject);
            parentArray.put(jsonObject);
         } else {
            jsonObject.put("error", "parsing error");
         }
      }
   }

   @Override
   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("oid", this.getOid().getId());
      obj.put("isCritical", this.isCritical());
      JSONObject valObj = new JSONObject();

      try {
         valObj.put("extension", ByteArrayUtil.toHexString(this.extension.getExtnValue().getOctets()));
      } catch (Exception e) {
         logger.log(Level.WARNING, "error encoding extension", e);
      }

      obj.put("value", valObj);
      return obj.toString();
   }

   @Override
   public String toString() {
      return new JSONObject(this.getJSON()).toString(2);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else {
         return this.getClass() == obj.getClass() ? Objects.equals(this.extension, ((NX509Extension)obj).extension) : false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.extension);
   }
}
