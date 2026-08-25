package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.cert.ext.NBasicConstraints;
import com.tridium.crypto.core.cert.ext.NExtendedKeyUsage;
import com.tridium.crypto.core.cert.ext.NIssuerAlternativeName;
import com.tridium.crypto.core.cert.ext.NKeyUsage;
import com.tridium.crypto.core.cert.ext.NSubjectAlternativeName;
import com.tridium.crypto.core.cert.ext.NSubjectKeyIdentifier;
import com.tridium.crypto.core.cert.ext.NX509Extension;
import com.tridium.json.JSONObject;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.SecurityInitializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.IX509Certificate;
import javax.baja.nre.security.IX509Extension;
import javax.baja.nre.util.ByteArrayUtil;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class NX509Certificate extends PemSource implements IX509Certificate {
   public static final int[] KEY_USAGE_VALUES = new int[]{128, 64, 32, 16, 8, 4, 2, 1, 32768};
   private final X509Certificate cert;
   private volatile Map<ASN1ObjectIdentifier, IX509Extension> extensions = null;
   private static final Logger logger = Logger.getLogger("crypto.cert");
   private static final IX509Extension[] EMPTY_IX509EXTENSION_ARRAY = new IX509Extension[0];

   public static NX509Certificate make(X509Certificate cert) {
      return new NX509Certificate(cert);
   }

   public static NX509Certificate make(String encoded) throws Exception {
      Object obj = PemSource.getFromPEM(encoded);
      X509Certificate cert;
      if (obj instanceof X509Certificate) {
         cert = (X509Certificate)obj;
      } else {
         if (!(obj instanceof X509CertificateHolder)) {
            throw new IOException("Unable to decode certificate");
         }

         cert = new JcaX509CertificateConverter()
            .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
            .getCertificate((X509CertificateHolder)obj);
      }

      return make(cert);
   }

   public static NX509Certificate make(KeyPair pair, NCertificateParameters params) throws Exception {
      Set<IX509Extension> extensions = new HashSet<>();
      extensions.add(NSubjectKeyIdentifier.make(false, pair.getPublic()));
      switch (params.getKeyPurpose()) {
         case CLIENT_CERT:
            extensions.add(NKeyUsage.make(true, 128));
            extensions.add(NExtendedKeyUsage.make(false, KeyPurposeId.id_kp_clientAuth));
            break;
         case SERVER_CERT:
            extensions.add(NKeyUsage.make(true, 160));
            extensions.add(NExtendedKeyUsage.make(false, KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth));
            break;
         case CA_CERT:
            extensions.add(NKeyUsage.make(true, 6));
            extensions.add(NBasicConstraints.make(true, true));
            break;
         case CODE_SIGNING_CERT:
            extensions.add(NKeyUsage.make(true, 128));
            extensions.add(NExtendedKeyUsage.make(false, KeyPurposeId.id_kp_codeSigning));
            break;
         default:
            throw new IllegalArgumentException("Invalid key purpose provided");
      }

      NGeneralName hostIdName = NGeneralName.makeHostID(NX509Certificate.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getHostId());
      extensions.add(NIssuerAlternativeName.make(false, hostIdName));
      Set<NGeneralName> sans = new HashSet<>();
      if (params.getEmail() != null) {
         sans.add(NGeneralName.makeEmailName(params.getEmail()));
      }

      if (params.getKeyPurpose() == KeyPurpose.SERVER_CERT) {
         X500Name dn = new X500Name(params.getSubjectDn());
         RDN cn = dn.getRDNs(BCStyle.CN)[0];
         String cnString = IETFUtils.valueToString(cn.getFirst().getValue());
         sans.add(NGeneralName.makeDnsName(cnString));
         if (params.getSubAltNameServer() != null && !params.getSubAltNameServer().equals(cnString)) {
            sans.add(NGeneralName.makeDnsName(params.getSubAltNameServer()));
         }
      }

      if (!sans.isEmpty()) {
         extensions.add(NSubjectAlternativeName.make(false, sans.toArray(new NGeneralName[0])));
      }

      return make(pair, params, extensions.toArray(EMPTY_IX509EXTENSION_ARRAY));
   }

   public static NX509Certificate make(KeyPair pair, NCertificateParameters params, IX509Extension... extensions) throws Exception {
      SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(pair.getPublic().getEncoded());
      X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
         new X500Name(params.getIssuerDn()),
         CertUtils.generateSerialNo(),
         params.getNotBeforeDate(),
         params.getNotAfterDate(),
         CertUtils.getAsn1FormatLocale(),
         new X500Name(params.getSubjectDn()),
         keyInfo
      );

      for (IX509Extension extension : extensions) {
         certBuilder.addExtension(extension.getExtension());
      }

      String algorithm = pair.getPrivate().getAlgorithm();
      ContentSigner sigGen;
      if (!"ecdsa".equalsIgnoreCase(algorithm) && !"ec".equalsIgnoreCase(algorithm)) {
         if (!"rsa".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException("invalid private key provided");
         }

         sigGen = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());
      } else {
         sigGen = new JcaContentSignerBuilder("SHA256withECDSA").build(pair.getPrivate());
      }

      X509CertificateHolder holder = certBuilder.build(sigGen);
      X509Certificate cert = new JcaX509CertificateConverter()
         .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
         .getCertificate(holder);
      return new NX509Certificate(cert);
   }

   private NX509Certificate(X509Certificate cert) {
      this.cert = cert;
   }

   @Override
   public X509Certificate getCertificate() {
      return this.cert;
   }

   @Override
   public int getVersion() {
      return this.cert.getVersion();
   }

   @Override
   public BigInteger getSerialNumber() {
      return this.cert.getSerialNumber();
   }

   @Override
   public X500Name getIssuerDN() {
      X500Name name = JcaX500NameUtil.getIssuer(this.cert);
      String dn = CertUtils.cleanDN(name.toString());
      return new X500Name(BCStyle.INSTANCE, dn);
   }

   @Override
   public X500Name getSubjectDN() {
      X500Name name = JcaX500NameUtil.getSubject(this.cert);
      String dn = CertUtils.cleanDN(name.toString());
      return new X500Name(BCStyle.INSTANCE, dn);
   }

   @Override
   public NSubjectAlternativeName getSubjectAlternativeNameExtension() {
      return (NSubjectAlternativeName)this.getExtension(Extension.subjectAlternativeName);
   }

   @Override
   public Date getNotBefore() {
      return this.cert.getNotBefore();
   }

   @Override
   public Date getNotAfter() {
      return this.cert.getNotAfter();
   }

   @Override
   public PublicKey getPublicKey() {
      return this.cert.getPublicKey();
   }

   @Override
   public String getKeyAlgorithm() {
      return this.cert.getPublicKey().getAlgorithm();
   }

   @Override
   public int getKeySize() {
      return getPublicKeyLength(this.cert.getPublicKey());
   }

   @Override
   public String getSignatureAlgorithm() {
      return this.cert.getSigAlgName();
   }

   @Override
   public int getSignatureSize() {
      return this.cert.getSignature().length;
   }

   @Override
   public String getBasicConstraints() {
      NBasicConstraints bc = (NBasicConstraints)this.getExtension(Extension.basicConstraints);
      if (bc == null || !bc.isCA()) {
         return "Subject Type: End Entity";
      } else {
         return bc.getPathLenConstraint() == null
            ? "Subject Type: CA, Path Length Constraint: None"
            : "Subject Type: CA, Path Length Constraint: " + bc.getPathLenConstraint().toString();
      }
   }

   @Override
   public NBasicConstraints getBasicConstraintsExtension() {
      return (NBasicConstraints)this.getExtension(Extension.basicConstraints);
   }

   @Override
   public String getKeyUsage() {
      NKeyUsage bc = (NKeyUsage)this.getExtension(Extension.keyUsage);
      return bc != null ? String.join(", ", bc.getFlags()) : "Not Provided";
   }

   @Override
   public int getKeyUsageValue() {
      NKeyUsage bc = (NKeyUsage)this.getExtension(Extension.keyUsage);
      return bc != null ? bc.getKeyUsageValue() : 0;
   }

   @Override
   public NKeyUsage getKeyUsageExtension() {
      return (NKeyUsage)this.getExtension(Extension.keyUsage);
   }

   @Override
   public String getExtendedKeyUsage() {
      NExtendedKeyUsage extendedKeyUsage = (NExtendedKeyUsage)this.getExtension(Extension.extendedKeyUsage);
      return extendedKeyUsage != null ? String.join(", ", extendedKeyUsage.getPurposeStrings()) : "Not Provided";
   }

   @Override
   public NExtendedKeyUsage getExtendedKeyUsageExtension() {
      return (NExtendedKeyUsage)this.getExtension(Extension.extendedKeyUsage);
   }

   @Override
   public String getMD5Fingerprint() {
      return this.getFingerprint("MD5");
   }

   @Override
   public String getSHA1Fingerprint() {
      return this.getFingerprint("SHA1");
   }

   public String getFingerprint(String alg) {
      byte[] fingerprint;
      try {
         MessageDigest md = MessageDigest.getInstance(alg);
         fingerprint = md.digest(this.cert.getEncoded());
      } catch (Exception e) {
         return "???";
      }

      return ByteArrayUtil.toHexString(fingerprint, ":");
   }

   @Override
   public byte[] getPublicKeyHash() {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA1");
         return md.digest(this.cert.getPublicKey().getEncoded());
      } catch (Exception e) {
         return null;
      }
   }

   @Override
   public boolean checkValidity() {
      try {
         this.cert.checkValidity();
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public String getIssuer() {
      try {
         return extractFriendlyName(JcaX500NameUtil.getIssuer(this.cert));
      } catch (Exception var2) {
         return this.getIssuerDN().toString();
      }
   }

   @Override
   public String getSubject() {
      try {
         return extractFriendlyName(JcaX500NameUtil.getSubject(this.cert));
      } catch (Exception var2) {
         return this.getSubjectDN().toString();
      }
   }

   @Override
   public boolean isSelfSigned() {
      try {
         PublicKey key = this.cert.getPublicKey();
         this.cert.verify(key);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   @Override
   public IX509Extension getExtension(ASN1ObjectIdentifier oid) {
      this.checkLoadExtensions();
      return this.extensions.get(oid);
   }

   @Override
   public IX509Extension[] getExtensions() {
      this.checkLoadExtensions();
      return this.extensions.values().toArray(EMPTY_IX509EXTENSION_ARRAY);
   }

   private void checkLoadExtensions() {
      if (this.extensions == null) {
         synchronized (this) {
            if (this.extensions == null) {
               HashMap<ASN1ObjectIdentifier, IX509Extension> extensions = new HashMap<>();
               Set<String> critOids = this.cert.getCriticalExtensionOIDs();
               if (critOids != null) {
                  for (String critOid : critOids) {
                     try {
                        ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(critOid);
                        IX509Extension ext = NX509Extension.make(oid, true, this.cert.getExtensionValue(critOid));
                        extensions.put(oid, ext);
                     } catch (Exception e) {
                        logger.log(Level.INFO, "unable to parse extension: " + critOid, e);
                     }
                  }
               }

               Set<String> nonCritOids = this.cert.getNonCriticalExtensionOIDs();
               if (nonCritOids != null) {
                  for (String nonCritOid : nonCritOids) {
                     try {
                        ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(nonCritOid);
                        IX509Extension ext = NX509Extension.make(new ASN1ObjectIdentifier(nonCritOid), false, this.cert.getExtensionValue(nonCritOid));
                        extensions.put(oid, ext);
                     } catch (Exception e) {
                        logger.log(Level.INFO, "unable to parse extension: " + nonCritOid, e);
                     }
                  }
               }

               this.extensions = Collections.unmodifiableMap(extensions);
            }
         }
      }
   }

   @Override
   public String getJSON() {
      return this.getJSONObject().toString();
   }

   public JSONObject getJSONObject() {
      JSONObject obj = new JSONObject();
      obj.put("version", this.getVersion());
      obj.put("serialNumber", ByteArrayUtil.toHexString(this.getSerialNumber().toByteArray(), " "));
      obj.put("issuerDN", this.getIssuerDN());
      obj.put("issuedBy", this.getIssuer());
      obj.put("subjectDN", this.getSubjectDN());
      obj.put("subject", this.getSubject());
      obj.put("notBefore", this.getNotBefore().getTime());
      obj.put("notAfter", this.getNotAfter().getTime());
      obj.put("keyAlgorithm", this.getKeyAlgorithm());
      obj.put("keySize", this.getKeySize());
      obj.put("spec", KeySpecUtil.getSpec(this.getPublicKey()));
      obj.put("signatureAlgorithm", this.getSignatureAlgorithm());
      obj.put("signatureSize", this.getSignatureSize());
      obj.put("basicConstraints", this.getBasicConstraints());
      obj.put("keyUsage", this.getKeyUsage());
      obj.put("extendedKeyUsage", this.getExtendedKeyUsage());
      obj.put("valid", this.checkValidity());
      obj.put("selfSigned", this.isSelfSigned());
      JSONObject md5Fingerprint = new JSONObject();
      md5Fingerprint.put("fingerprintAlgorithm", "MD5");
      md5Fingerprint.put("fingerprint", this.getMD5Fingerprint());
      obj.append("fingerprints", md5Fingerprint);
      JSONObject sha1Fingerprint = new JSONObject();
      sha1Fingerprint.put("fingerprintAlgorithm", "SHA1");
      sha1Fingerprint.put("fingerprint", this.getSHA1Fingerprint());
      obj.append("fingerprints", sha1Fingerprint);
      IX509Extension[] extensions = this.getExtensions();

      for (IX509Extension extension : extensions) {
         JSONObject ext;
         if (extension instanceof NX509Extension) {
            ext = ((NX509Extension)extension).getJSONObject();
         } else {
            ext = new JSONObject(extension.getJSON());
         }

         obj.append("extensions", ext);
      }

      try {
         obj.put("pemString", this.encodeToString());
      } catch (Exception e) {
         logger.log(Level.INFO, "unable to parse pem string: ", e);
      }

      obj.put("ASN1String", this.getASN1String());
      return obj;
   }

   @Override
   public String encodeToString() throws Exception {
      return PemSource.getPEMString(this.cert);
   }

   @Override
   public String toString() {
      return this.getJSONObject().toString(2);
   }

   @Override
   public String getASN1String() {
      try {
         ByteArrayInputStream inStream = new ByteArrayInputStream(this.cert.getEncoded());
         ASN1InputStream derInStream = new ASN1InputStream(inStream);
         ASN1Object derObject = derInStream.readObject();
         return ASN1Dump.dumpAsString(derObject, true);
      } catch (Exception e) {
         return "error generating asn1 string: " + e.getLocalizedMessage();
      }
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (object == null) {
         return false;
      } else if (this.getClass() == object.getClass()) {
         return this.cert != null && ((NX509Certificate)object).cert != null ? this.cert.equals(((NX509Certificate)object).cert) : false;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.cert == null ? 0 : this.cert.hashCode();
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
