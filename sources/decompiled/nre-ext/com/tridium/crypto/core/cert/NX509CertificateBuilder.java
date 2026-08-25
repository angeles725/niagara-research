package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.cert.ext.NBasicConstraints;
import com.tridium.crypto.core.cert.ext.NExtendedKeyUsage;
import com.tridium.crypto.core.cert.ext.NIssuerAlternativeName;
import com.tridium.crypto.core.cert.ext.NKeyUsage;
import com.tridium.crypto.core.cert.ext.NSubjectKeyIdentifier;
import com.tridium.crypto.core.cert.ext.NX509Extension;
import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.IX509CertificateEntry;
import javax.baja.nre.security.IX509Extension;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class NX509CertificateBuilder {
   private static final String ALIAS_KEY = "alias";
   private static final String SUBJECT_DN_KEY = "subjectDn";
   private static final String ISSUER_DN_KEY = "issuerDn";
   private static final String NOT_BEFORE_KEY = "notBefore";
   private static final String NOT_AFTER_KEY = "notAfter";
   private static final String SIGNATURE_ALGORITHM_KEY = "signatureAlgorithm";
   private static final String EXTENSIONS_KEY = "extensions";
   private static final String KEY_STORE_PASSWORD = "keyStorePassword";
   private static final Logger logger = Logger.getLogger("crypto.cert");
   private String alias = null;
   private SecretChars keyStorePassword = null;
   private X500Name issuerDN = null;
   private X500Name subjectDN = null;
   private Date notBefore = null;
   private Date notAfter = null;
   private String sigAlg = null;
   private final HashMap<ASN1ObjectIdentifier, IX509Extension> extensions = new HashMap<>();

   public static NX509CertificateBuilder getInstance() {
      return new NX509CertificateBuilder();
   }

   public static NX509CertificateBuilder getInstance(KeyPurpose purpose) throws IOException {
      return getInstance(purpose, 0);
   }

   public static NX509CertificateBuilder getInstance(KeyPurpose purpose, int keyUsage) throws IOException {
      NX509CertificateBuilder builder = new NX509CertificateBuilder();
      if (purpose != null) {
         switch (purpose) {
            case CLIENT_CERT:
               logger.fine("creating client cert extensions");
               builder.withExtension(NKeyUsage.make(true, keyUsage | 128));
               builder.withExtension(NExtendedKeyUsage.make(false, KeyPurposeId.id_kp_clientAuth));
               break;
            case SERVER_CERT:
               logger.fine("creating server cert extensions");
               builder.withExtension(NKeyUsage.make(true, keyUsage | 128 | 32));
               builder.withExtension(NExtendedKeyUsage.make(false, KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth));
               break;
            case CODE_SIGNING_CERT:
               logger.fine("creating code signing cert extensions");
               builder.withExtension(NKeyUsage.make(true, keyUsage | 128));
               builder.withExtension(NExtendedKeyUsage.make(false, KeyPurposeId.id_kp_codeSigning));
               break;
            case CA_CERT:
               logger.fine("creating ca cert extensions");
               builder.withExtension(NKeyUsage.make(true, keyUsage | 4 | 2));
               builder.withExtension(NBasicConstraints.make(true, true));
               break;
            default:
               throw new IllegalArgumentException("unrecognized key purpose : " + purpose);
         }
      }

      return builder;
   }

   private NX509CertificateBuilder() {
   }

   public String getAlias() {
      return this.alias;
   }

   public SecretChars getKeyStorePassword() {
      return this.keyStorePassword;
   }

   public NX509CertificateBuilder withAlias(String alias) {
      this.alias = alias;
      return this;
   }

   public NX509CertificateBuilder withKeyStorePassword(SecretChars keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
   }

   public NX509CertificateBuilder withIssuerDN(String issuerDN) throws IOException {
      if (issuerDN == null) {
         this.issuerDN = null;
      } else {
         this.issuerDN = new X500Name(issuerDN);
      }

      return this;
   }

   public NX509CertificateBuilder withIssuerDN(X500Name issuerDN) {
      this.issuerDN = issuerDN;
      return this;
   }

   public NX509CertificateBuilder withSubjectDN(String subjectDN) throws IOException {
      try {
         if (subjectDN == null) {
            throw new IllegalArgumentException("Null Subject DN is not allowed");
         }

         this.subjectDN = new X500Name(subjectDN);
         return this;
      } catch (IllegalArgumentException e) {
         throw e;
      } catch (Exception e) {
         throw new IOException(e);
      }
   }

   public NX509CertificateBuilder withSubjectDN(X500Name subjectDN) {
      this.subjectDN = subjectDN;
      return this;
   }

   public NX509CertificateBuilder withNotBefore(Date notBefore) {
      if (notBefore == null) {
         this.notBefore = null;
      } else {
         this.notBefore = (Date)notBefore.clone();
      }

      return this;
   }

   public NX509CertificateBuilder withNotAfter(Date notAfter) {
      if (notAfter == null) {
         this.notAfter = null;
      } else {
         this.notAfter = (Date)notAfter.clone();
      }

      return this;
   }

   public NX509CertificateBuilder withExtension(IX509Extension extension) {
      if (extension != null) {
         this.extensions.put(extension.getOid(), extension);
      }

      return this;
   }

   public NX509CertificateBuilder withExtensions(IX509Extension... suppliedExtensions) {
      for (IX509Extension extension : suppliedExtensions) {
         if (extension != null) {
            this.extensions.put(extension.getOid(), extension);
         }
      }

      return this;
   }

   public NX509CertificateBuilder withSignatureAlgorithm(String sigAlg) {
      this.sigAlg = sigAlg;
      return this;
   }

   public IX509CertificateEntry generateEntry(NKeyPairGenerator generator) throws Exception {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("generating key pair for new cert : " + generator.toString());
      }

      return this.generateEntry(generator.generateKeyPair());
   }

   public IX509CertificateEntry generateEntry(KeyPair keyPair) throws Exception {
      logger.fine("creating new cert with builder...");
      if (this.subjectDN == null) {
         throw new IOException("subjectDN missing");
      }

      if (this.issuerDN == null) {
         this.issuerDN = this.subjectDN;
      }

      if (this.notBefore == null) {
         throw new IOException("notBefore date missing");
      }

      if (this.notAfter == null) {
         throw new IOException("notAfter date missing");
      }

      SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
      X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
         this.issuerDN, CertUtils.generateSerialNo(), this.notBefore, this.notAfter, CertUtils.getAsn1FormatLocale(), this.subjectDN, keyInfo
      );
      boolean hostIdAdded = false;

      for (IX509Extension extension : this.extensions.values()) {
         if (extension.getOid().equals(Extension.issuerAlternativeName)) {
            NIssuerAlternativeName issuerAlternativeName = (NIssuerAlternativeName)extension;

            for (NGeneralName name : issuerAlternativeName.getNames()) {
               if (name.getTagNo() == 0) {
                  ASN1Sequence sequence = ASN1Sequence.getInstance(name.getName().getName());
                  ASN1ObjectIdentifier otherOid = ASN1ObjectIdentifier.getInstance(sequence.getObjectAt(0));
                  if (ASN1HostId.HOST_ID_OID.equals(otherOid)) {
                     hostIdAdded = true;
                  }
               }
            }

            if (!hostIdAdded) {
               NGeneralName hostId = NGeneralName.makeHostID();
               Set<NGeneralName> names = new HashSet<>(issuerAlternativeName.getNames());
               names.add(hostId);
               NIssuerAlternativeName newIssuerAlternativeName = NIssuerAlternativeName.make(issuerAlternativeName.isCritical(), names);
               extension = newIssuerAlternativeName;
               hostIdAdded = true;
            }
         }

         if (logger.isLoggable(Level.FINE)) {
            logger.fine("adding extension to cert: " + OidMap.get(extension.getOid()));
         }

         certBuilder.addExtension(extension.getExtension());
      }

      if (!hostIdAdded) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("adding extension to cert: " + OidMap.get(Extension.issuerAlternativeName));
         }

         certBuilder.addExtension(NIssuerAlternativeName.make(false, NGeneralName.makeHostID()).getExtension());
      }

      if (!this.extensions.containsKey(Extension.subjectKeyIdentifier)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("adding extension to cert: " + OidMap.get(Extension.subjectKeyIdentifier));
         }

         certBuilder.addExtension(NSubjectKeyIdentifier.make(false, keyPair.getPublic()).getExtension());
      }

      if (this.sigAlg == null) {
         if ("ecdsa".equalsIgnoreCase(keyPair.getPrivate().getAlgorithm())) {
            this.sigAlg = "SHA256withECDSA";
         } else if ("ec".equalsIgnoreCase(keyPair.getPrivate().getAlgorithm())) {
            this.sigAlg = "SHA256withECDSA";
         } else {
            if (!"rsa".equalsIgnoreCase(keyPair.getPrivate().getAlgorithm())) {
               throw new IllegalArgumentException("unable to set the signature algorithm for " + keyPair.getPrivate().getAlgorithm());
            }

            this.sigAlg = "SHA256withRSA";
         }
      }

      ContentSigner sigGen = new JcaContentSignerBuilder(this.sigAlg).build(keyPair.getPrivate());
      X509CertificateHolder holder = certBuilder.build(sigGen);
      X509Certificate cert = new JcaX509CertificateConverter()
         .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
         .getCertificate(holder);
      return new NX509CertificateEntry(this.alias, new NX509Certificate[]{NX509Certificate.make(cert)}, keyPair.getPrivate());
   }

   public String encodeToString() throws IOException {
      JSONObject object = new JSONObject();
      object.putOpt("alias", this.alias);
      if (this.issuerDN != null) {
         object.put("issuerDn", this.issuerDN.toString());
      }

      if (this.subjectDN != null) {
         object.put("subjectDn", this.subjectDN.toString());
      }

      if (this.notBefore != null) {
         object.put("notBefore", this.notBefore.getTime());
      }

      if (this.notAfter != null) {
         object.put("notAfter", this.notAfter.getTime());
      }

      if (this.keyStorePassword != null) {
         object.put("keyStorePassword", this.keyStorePassword.asString(false));
      }

      object.putOpt("signatureAlgorithm", this.sigAlg);

      for (IX509Extension extension : this.extensions.values()) {
         object.append("extensions", new JSONObject(extension.encodeToString()));
      }

      return object.toString();
   }

   public static NX509CertificateBuilder decodeFromString(String string) throws IOException, CertificateParseException {
      NX509CertificateBuilder builder = getInstance();
      JSONObject object = new JSONObject(string);
      if (object.has("alias")) {
         builder.withAlias(object.getString("alias"));
      }

      if (object.has("keyStorePassword")) {
         builder.withKeyStorePassword(SecretChars.fromString(object.getString("keyStorePassword")));
      }

      try {
         if (object.has("issuerDn")) {
            builder.withIssuerDN(object.getString("issuerDn"));
         }
      } catch (Exception e) {
         throw new CertificateParseException("issuerDn", object.getString("issuerDn"));
      }

      try {
         if (object.has("subjectDn")) {
            builder.withSubjectDN(object.getString("subjectDn"));
         }
      } catch (Exception e) {
         throw new CertificateParseException("subjectDn", object.getString("subjectDn"));
      }

      if (object.has("notBefore")) {
         builder.withNotBefore(new Date(object.getLong("notBefore")));
      }

      if (object.has("notAfter")) {
         builder.withNotAfter(new Date(object.getLong("notAfter")));
      }

      if (object.has("signatureAlgorithm")) {
         builder.withSignatureAlgorithm(object.getString("signatureAlgorithm"));
      }

      if (object.has("extensions")) {
         JSONArray extensions = object.getJSONArray("extensions");

         for (int i = 0; i < extensions.length(); i++) {
            IX509Extension ext = NX509Extension.decodeFromString(JSONUtil.getString(extensions, i));
            builder.withExtension(ext);
         }
      }

      return builder;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else if (!(obj instanceof NX509CertificateBuilder)) {
         return false;
      } else {
         NX509CertificateBuilder other = (NX509CertificateBuilder)obj;
         if (!Objects.equals(this.alias, other.alias)) {
            return false;
         } else if (!Objects.equals(this.issuerDN, other.issuerDN)) {
            return false;
         } else if (!Objects.equals(this.subjectDN, other.subjectDN)) {
            return false;
         } else if (!Objects.equals(this.notBefore, other.notBefore)) {
            return false;
         } else if (!Objects.equals(this.notAfter, other.notAfter)) {
            return false;
         } else {
            return !Objects.equals(this.sigAlg, other.sigAlg) ? false : this.extensions.equals(other.extensions);
         }
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.alias, this.issuerDN, this.subjectDN, this.notBefore, this.notAfter, this.sigAlg, this.extensions);
   }
}
