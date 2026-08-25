package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.cert.ext.NBasicConstraints;
import com.tridium.crypto.core.cert.ext.NExtendedKeyUsage;
import com.tridium.crypto.core.cert.ext.NIssuerAlternativeName;
import com.tridium.crypto.core.cert.ext.NKeyUsage;
import com.tridium.crypto.core.cert.ext.NSubjectAlternativeName;
import com.tridium.crypto.core.io.ICoreKeyStore;
import com.tridium.crypto.core.io.ICoreTrustStore;
import com.tridium.crypto.core.util.BouncyCastleHelper;
import com.tridium.crypto.core.util.OidMap;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PrivilegedActionException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Locale.Category;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.Adler32;
import javax.baja.nre.security.IX509Certificate;
import javax.baja.nre.security.IX509CertificateEntry;
import javax.baja.nre.security.IX509Extension;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.CertificateUtils;
import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStrictStyle;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;

public final class CertUtils {
   public static final Locale ASN1_FORMAT_LOCAL = Locale.ENGLISH;
   public static final List<Locale> FAILING_FORMAT_LOCALES = new ArrayList<>();
   public static final String FACTORY_CERT_ALIAS = "default";
   public static final String LEGACY_CERT_ALIAS = "tridium";
   public static final String FACTORY_CERT_DN = "CN=Niagara4,O=ForRecoveryPurposes,C=US";
   public static final String FACTORY_CERT_DNS_NAME = "Niagara4";
   public static final int FACTORY_RSA_KEYSIZE = 2048;
   public static final NKeyPairGenerator FACTORY_CERT_GENERATOR = new NRsaKeyPairGenerator(2048);
   private static final String CRL_DISTRIBUTION_POINTS_OID = "2.5.29.31";
   private static final Logger LOG = Logger.getLogger("crypto.certManagement");
   private static final NGeneralName[] EMPTY_NAMES = new NGeneralName[0];

   private CertUtils() {
   }

   public static IX509CertificateEntry generateSelfSignedCert(NCertificateParameters certParams) throws Exception {
      NKeyPairGenerator generator = new NRsaKeyPairGenerator(certParams.getKeySize());
      return generateSelfSignedCert(generator.generateKeyPair(), certParams);
   }

   public static IX509CertificateEntry generateSelfSignedCert(KeyPair pair, NCertificateParameters certParams) throws Exception {
      NX509CertificateBuilder builder = createCertBuilderFromParameters(certParams);
      builder.withExtension(NIssuerAlternativeName.make(false, NGeneralName.makeHostID()));
      return builder.generateEntry(pair);
   }

   public static NPKCS10CertificationRequest generateCSR(IX509CertificateEntry entry) throws Exception {
      return generateCSR(entry.getCertificate(0).getCertificate(), entry.getPrivateKey());
   }

   public static NPKCS10CertificationRequest generateCSR(X509Certificate cert, PrivateKey privateKey) throws Exception {
      return generateCSR(cert, privateKey, null);
   }

   public static NPKCS10CertificationRequest generateCSR(X509Certificate cert, PrivateKey privateKey, X509Certificate certWithAdditionalExtsToMerge) throws Exception {
      ExtensionsGenerator extensions = new ExtensionsGenerator();
      addExtensions(extensions, cert, true, certWithAdditionalExtsToMerge, false);
      addExtensions(extensions, cert, false, certWithAdditionalExtsToMerge, false);
      if (certWithAdditionalExtsToMerge != null) {
         addExtensions(extensions, certWithAdditionalExtsToMerge, true, null, true);
         addExtensions(extensions, certWithAdditionalExtsToMerge, false, null, true);
      }

      SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(cert.getPublicKey().getEncoded());
      PKCS10CertificationRequestBuilder csrBuilder = new PKCS10CertificationRequestBuilder(new X500Name(cert.getSubjectX500Principal().getName()), keyInfo);
      csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions.generate());
      ContentSigner sigGen;
      if (privateKey instanceof RSAPrivateKey) {
         sigGen = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
      } else {
         if (!(privateKey instanceof ECPrivateKey)) {
            throw new IllegalArgumentException("invalid private key provided");
         }

         sigGen = new JcaContentSignerBuilder("SHA256withECDSA").build(privateKey);
      }

      JcaPKCS10CertificationRequest csr = new JcaPKCS10CertificationRequest(csrBuilder.build(sigGen));
      return NPKCS10CertificationRequest.make(csr);
   }

   private static void addExtensions(
      ExtensionsGenerator extensions,
      X509Certificate cert,
      boolean critical,
      X509Certificate certWithAdditionalExtsToMerge,
      boolean suppressDuplicateExceptions
   ) throws Exception {
      Set<String> extensionOIDs = critical ? cert.getCriticalExtensionOIDs() : cert.getNonCriticalExtensionOIDs();
      if (extensionOIDs != null && !extensionOIDs.isEmpty()) {
         for (String oidStr : extensionOIDs) {
            byte[] value = cert.getExtensionValue(oidStr);
            ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(oidStr);
            byte[] mergeVal;
            if (certWithAdditionalExtsToMerge != null
               && Extension.subjectAlternativeName.equals(oid)
               && (mergeVal = certWithAdditionalExtsToMerge.getExtensionValue(oidStr)) != null) {
               ASN1InputStream asn1Input = new ASN1InputStream(value);
               Throwable var71 = null;

               ASN1Primitive primitive;
               try {
                  DEROctetString octet = (DEROctetString)asn1Input.readObject();
                  primitive = ASN1Primitive.fromByteArray(octet.getOctets());
               } catch (Throwable var60) {
                  var71 = var60;
                  throw var60;
               } finally {
                  if (asn1Input != null) {
                     if (var71 != null) {
                        try {
                           asn1Input.close();
                        } catch (Throwable var58) {
                           var71.addSuppressed(var58);
                        }
                     } else {
                        asn1Input.close();
                     }
                  }
               }

               Extension ext = new Extension(Extension.subjectAlternativeName, critical, primitive.getEncoded("DER"));
               GeneralName[] names = GeneralNames.fromExtensions(new Extensions(ext), Extension.subjectAlternativeName).getNames();
               ASN1InputStream asn1Inputx = new ASN1InputStream(mergeVal);
               Throwable additionalNames = null;

               try {
                  DEROctetString octet = (DEROctetString)asn1Inputx.readObject();
                  primitive = ASN1Primitive.fromByteArray(octet.getOctets());
               } catch (Throwable var59) {
                  additionalNames = var59;
                  throw var59;
               } finally {
                  if (asn1Inputx != null) {
                     if (additionalNames != null) {
                        try {
                           asn1Inputx.close();
                        } catch (Throwable var57) {
                           additionalNames.addSuppressed(var57);
                        }
                     } else {
                        asn1Inputx.close();
                     }
                  }
               }

               Extension additionalExt = new Extension(Extension.subjectAlternativeName, critical, primitive.getEncoded("DER"));
               GeneralName[] additionalNamesx = GeneralNames.fromExtensions(new Extensions(additionalExt), Extension.subjectAlternativeName).getNames();
               Set<NGeneralName> mergedNames = new HashSet<>();
               mergedNames.addAll(Arrays.stream(names).map(NGeneralName::make).collect(Collectors.toList()));
               mergedNames.addAll(Arrays.stream(additionalNamesx).map(NGeneralName::make).collect(Collectors.toList()));
               extensions.addExtension(
                  Extension.subjectAlternativeName,
                  critical,
                  NSubjectAlternativeName.make(critical, mergedNames.toArray(EMPTY_NAMES)).getExtension().getParsedValue()
               );
            } else {
               try {
                  ASN1InputStream asn1Input = new ASN1InputStream(value);
                  Throwable ext = null;

                  try {
                     DEROctetString octet = (DEROctetString)asn1Input.readObject();
                     extensions.addExtension(oid, critical, ASN1Primitive.fromByteArray(octet.getOctets()));
                  } catch (Throwable var62) {
                     ext = var62;
                     throw var62;
                  } finally {
                     if (asn1Input != null) {
                        if (ext != null) {
                           try {
                              asn1Input.close();
                           } catch (Throwable var61) {
                              ext.addSuppressed(var61);
                           }
                        } else {
                           asn1Input.close();
                        }
                     }
                  }
               } catch (IllegalArgumentException e) {
                  if (!suppressDuplicateExceptions) {
                     throw e;
                  }
               }
            }
         }
      }
   }

   public static NX509Certificate signCertificate(NPKCS10CertificationRequest csr, IX509CertificateEntry ca, NSigningParameters params) throws Exception {
      return signCertificate(csr, ca, params, false);
   }

   public static NX509Certificate signCertificate(NPKCS10CertificationRequest csr, IX509CertificateEntry ca, NSigningParameters params, boolean preferParams) throws Exception {
      X509Certificate certReply = signCertificate(csr.getRequest(), ca.getCertificate(0).getCertificate(), ca.getPrivateKey(), params, preferParams);
      return NX509Certificate.make(certReply);
   }

   public static X509Certificate signCertificate(JcaPKCS10CertificationRequest csr, X509Certificate caCert, PrivateKey caKey, NSigningParameters params) throws Exception {
      return signCertificate(csr, caCert, caKey, params, false);
   }

   public static X509Certificate signCertificate(
      JcaPKCS10CertificationRequest csr, X509Certificate caCert, PrivateKey caKey, NSigningParameters params, boolean preferParams
   ) throws Exception {
      SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(csr.getPublicKey().getEncoded());
      X500Name subjectDn = csr.toASN1Structure().getCertificationRequestInfo().getSubject();
      if (params.getSubjectDn() != null) {
         subjectDn = new X500Name(params.getSubjectDn());
      }

      X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
         JcaX500NameUtil.getSubject(caCert), generateSerialNo(), params.getNotBefore(), params.getNotAfter(), getAsn1FormatLocale(), subjectDn, keyInfo
      );
      certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(caCert));
      certBuilder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(csr.getPublicKey()));
      IX509Extension[] extensions = params.getExtensions();
      if (extensions.length > 0) {
         for (IX509Extension ext : extensions) {
            if (!ext.getOid().equals(OidMap.SUBJECT_KEY_IDENTIFIER) && !ext.getOid().equals(OidMap.AUTHORITY_KEY_IDENTIFIER)) {
               certBuilder.addExtension(ext.getOid(), ext.isCritical(), ext.getExtension().getParsedValue());
            }
         }
      }

      addKeyPurposeExtensions(certBuilder, params.getKeyPurpose(), 0, preferParams);
      ContentSigner sigGen;
      if (caKey instanceof RSAPrivateKey) {
         sigGen = new JcaContentSignerBuilder("SHA256withRSA").build(caKey);
      } else if (caKey instanceof ECPrivateKey) {
         sigGen = new JcaContentSignerBuilder("SHA256withECDSA").build(caKey);
      } else {
         if (!(caKey instanceof DSAPrivateKey)) {
            throw new IllegalArgumentException("Unsupported key type for signing: " + caKey.getClass().getName());
         }

         sigGen = new JcaContentSignerBuilder("SHA256withDSA").build(caKey);
      }

      X509CertificateHolder holder = certBuilder.build(sigGen);
      return new JcaX509CertificateConverter().setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider()).getCertificate(holder);
   }

   public static List<? extends Certificate> sortCertChain(List<? extends Certificate> certs) {
      if (certs.size() < 2) {
         return certs;
      }

      X500Principal issuer = ((X509Certificate)certs.get(0)).getIssuerX500Principal();
      boolean okay = true;

      for (int i = 1; i != certs.size(); i++) {
         X509Certificate cert = (X509Certificate)certs.get(i);
         if (!issuer.equals(cert.getSubjectX500Principal())) {
            okay = false;
            break;
         }

         issuer = cert.getIssuerX500Principal();
      }

      if (okay) {
         return certs;
      }

      List<Certificate> retList = new ArrayList<>(certs.size());
      List<? extends Certificate> orig = certs;
      certs = new ArrayList<>(certs);

      for (int i = 0; i < certs.size(); i++) {
         X509Certificate cert = (X509Certificate)certs.get(i);
         boolean found = false;
         X500Principal subject = cert.getSubjectX500Principal();

         for (int j = 0; j != certs.size(); j++) {
            X509Certificate c = (X509Certificate)certs.get(j);
            if (c.getIssuerX500Principal().equals(subject)) {
               found = true;
               break;
            }
         }

         if (!found) {
            for (int j = 0; j != retList.size(); j++) {
               X509Certificate c = (X509Certificate)retList.get(j);
               if (c.getIssuerX500Principal().equals(subject)) {
                  found = true;
                  break;
               }
            }
         }

         if (!found) {
            retList.add(cert);
            certs.remove(i);
            i--;
         }
      }

      if (retList.size() > 1) {
         return orig;
      }

      for (int i = 0; i != retList.size(); i++) {
         issuer = ((X509Certificate)retList.get(i)).getIssuerX500Principal();

         for (int j = 0; j < certs.size(); j++) {
            X509Certificate c = (X509Certificate)certs.get(j);
            if (issuer.equals(c.getSubjectX500Principal())) {
               retList.add(c);
               certs.remove(j);
               break;
            }
         }
      }

      return !certs.isEmpty() ? orig : retList;
   }

   public static void validateCertChain(X509Certificate[] certs, ICoreTrustStore trust) throws Exception {
      validateCertChain(certs, trust, true);
   }

   public static void validateCertChain(X509Certificate[] certs, ICoreTrustStore trust, boolean validateDate) throws Exception {
      ArrayList<X509Certificate> certChain = new ArrayList<>();
      if (certs.length == 1) {
         IX509Certificate iCert = NX509Certificate.make(certs[0]);
         if (iCert.isSelfSigned()) {
            return;
         }
      }

      IX509Certificate iCert = NX509Certificate.make(certs[certs.length - 1]);
      PKIXParameters param;
      if (iCert.isSelfSigned()) {
         TrustAnchor anchor = new TrustAnchor(certs[certs.length - 1], null);
         param = new PKIXParameters(Collections.singleton(anchor));
         certChain.addAll(Arrays.asList(certs).subList(0, certs.length - 1));
      } else {
         param = new PKIXParameters(trust.getKeyStore());
         Collections.addAll(certChain, certs);
      }

      param.setRevocationEnabled(false);
      if (!validateDate) {
         param.setDate(certChain.get(0).getNotBefore());
      }

      CertificateFactory fac = CertificateFactory.getInstance("X.509");
      CertPath certPath = fac.generateCertPath(certChain);
      CertPathValidator validator = CertPathValidator.getInstance("PKIX");
      validator.validate(certPath, param);
   }

   public static boolean isValidSigningCert(String alias, KeyStore store) {
      try {
         if (!store.isKeyEntry(alias)) {
            return false;
         }

         try {
            PrivateKey privateKey = (PrivateKey)getKey(store, alias, new char[0]);
            if (privateKey != null) {
               return false;
            }
         } catch (UnrecoverableKeyException var3) {
         }

         X509Certificate cert = (X509Certificate)store.getCertificate(alias);
         return cert.getExtendedKeyUsage().contains(KeyPurposeId.id_kp_codeSigning.getId());
      } catch (Exception e) {
         return false;
      }
   }

   public static boolean isValidCACert(String alias, ICoreTrustStore store) {
      try {
         if (store.isKeyEntry(alias)) {
            X509Certificate cert = store.getCertificate(alias);
            return cert != null && (cert.getBasicConstraints() != -1 || cert.getKeyUsage() != null && cert.getKeyUsage()[5]);
         }
      } catch (Exception var3) {
      }

      return false;
   }

   public static boolean isValidServerCert(String alias, ICoreKeyStore store) {
      try {
         if (!store.isKeyEntry(alias)) {
            return false;
         }

         IX509Certificate iCert = NX509Certificate.make(store.getCertificate(alias));
         NKeyUsage keyUsage = iCert.getKeyUsageExtension();
         return keyUsage == null || !keyUsage.isCritical() || keyUsage.hasFlag(32) && keyUsage.hasFlag(128);
      } catch (Exception e) {
         return false;
      }
   }

   public static boolean checkKeyUsageExtension(NKeyUsage keyUsageExtension, int... requiredKeyUsages) {
      for (int requiredKeyUsage : requiredKeyUsages) {
         if (!keyUsageExtension.hasFlag(requiredKeyUsage)) {
            return false;
         }
      }

      return true;
   }

   public static boolean checkExtendedKeyUsageExtension(NExtendedKeyUsage extendedKeyUsageExtension, KeyPurposeId... requiredKeyPurposeIds) {
      for (KeyPurposeId requiredKeyPurposeId : requiredKeyPurposeIds) {
         if (!extendedKeyUsageExtension.hasPurpose(requiredKeyPurposeId)) {
            return false;
         }
      }

      return true;
   }

   public static boolean checkBasicConstraintsExtension(NBasicConstraints basicConstraintsExtension) {
      return basicConstraintsExtension.isCA();
   }

   public static boolean isCACertificate(X509Certificate cert) {
      return cert == null ? false : cert.getBasicConstraints() != -1 || cert.getKeyUsage() != null && cert.getKeyUsage().length > 5 && cert.getKeyUsage()[5];
   }

   public static boolean isClientCert(X509Certificate cert) {
      if (cert == null) {
         return false;
      }

      try {
         return cert.getExtendedKeyUsage() != null && cert.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.2");
      } catch (Exception e) {
         return false;
      }
   }

   public static boolean isCodeSigningCertificate(X509Certificate cert) {
      if (cert == null) {
         return false;
      }

      try {
         return cert.getExtendedKeyUsage().contains(KeyPurposeId.id_kp_codeSigning.getId());
      } catch (Exception e) {
         return false;
      }
   }

   public static boolean isServerCertificate(X509Certificate cert) {
      if (cert == null) {
         return false;
      }

      IX509Certificate iCert = NX509Certificate.make(cert);
      NKeyUsage keyUsage = iCert.getKeyUsageExtension();
      return keyUsage == null ? false : keyUsage.hasFlag(32) && keyUsage.hasFlag(128);
   }

   public static boolean isPrivateKeyGloballyEncrypted(String alias, ICoreKeyStore store) throws Exception {
      try {
         return AccessController.doPrivileged(() -> {
            try {
               PrivateKey pkey = (PrivateKey)store.getKey(alias, null);
               if (pkey != null) {
                  return Boolean.TRUE;
               } else {
                  throw new Exception("alias " + alias + " not found in keystore with private key");
               }
            } catch (UnrecoverableKeyException e) {
               return Boolean.FALSE;
            }
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   public static boolean checkHasMatchingPrivateKey(String alias, SecretChars password, ICoreKeyStore store) throws Exception {
      try {
         return AccessController.doPrivileged(() -> {
            boolean success = false;

            try {
               PrivateKey pkey;
               if (password == null) {
                  pkey = (PrivateKey)store.getKey(alias, null);
               } else {
                  pkey = (PrivateKey)store.getKey(alias, password.get());
               }

               if (pkey != null) {
                  success = true;
               }
            } catch (UnrecoverableKeyException var5) {
            }

            return success;
         });
      } catch (PrivilegedActionException e) {
         throw e.getException();
      }
   }

   public static String assembleDN(String cn, String ou, String o, String l, String st, String c) {
      X500NameBuilder builder = new X500NameBuilder(new BCStrictStyle());
      if (cn != null && !cn.isEmpty()) {
         builder.addRDN(BCStyle.CN, cn.trim());
      }

      if (ou != null && !ou.isEmpty()) {
         builder.addRDN(BCStyle.OU, ou.trim());
      }

      if (o != null && !o.isEmpty()) {
         builder.addRDN(BCStyle.O, o.trim());
      }

      if (l != null && !l.isEmpty()) {
         builder.addRDN(BCStyle.L, l.trim());
      }

      if (st != null && !st.isEmpty()) {
         builder.addRDN(BCStyle.ST, st.trim());
      }

      if (c != null && !c.isEmpty()) {
         builder.addRDN(BCStyle.C, c.trim());
      }

      X500Name dn = builder.build();
      return dn.toString();
   }

   public static String cleanDN(String originalDN) {
      StringBuilder cleanDN = new StringBuilder();
      String[] originalDNTokens = originalDN.split(",");

      for (int i = 0; i < originalDNTokens.length; i++) {
         if (originalDNTokens[i].indexOf(61) != originalDNTokens[i].length() - 1) {
            if (i > 0 && cleanDN.length() > 0) {
               cleanDN.append(',');
            }

            cleanDN.append(originalDNTokens[i]);
         }
      }

      return cleanDN.toString();
   }

   public static boolean checkDnEquality(X500Principal dn1, X500Principal dn2) {
      return CertificateUtils.checkDnEquality(dn1, dn2);
   }

   public static boolean checkDnEquality(String dn1, String dn2) {
      return CertificateUtils.checkDnEquality(dn1, dn2);
   }

   public static NX509CertificateEntry generateStockCertificate() throws Exception {
      return (NX509CertificateEntry)getStockBuilder().generateEntry(new NRsaKeyPairGenerator(2048));
   }

   public static NX509CertificateBuilder getStockBuilder() throws Exception {
      Instant now = Instant.now();
      Date notBefore = Date.from(now);
      Date notAfter = Date.from(now.plus(365L, ChronoUnit.DAYS));
      return NX509CertificateBuilder.getInstance(KeyPurpose.SERVER_CERT, 0)
         .withAlias("default")
         .withIssuerDN("CN=Niagara4,O=ForRecoveryPurposes,C=US")
         .withSubjectDN("CN=Niagara4,O=ForRecoveryPurposes,C=US")
         .withNotBefore(notBefore)
         .withNotAfter(notAfter)
         .withExtension(NIssuerAlternativeName.make(false, NGeneralName.makeHostID()))
         .withExtension(NSubjectAlternativeName.make(false, NGeneralName.makeDnsName("Niagara4")));
   }

   public static boolean hasUnrestrictedPolicyFiles() {
      try {
         int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
         return maxKeyLen > 128;
      } catch (NoSuchAlgorithmException e) {
         System.err.println("SEVERE [" + new Date() + "][nre] unable to assess unrestricted policy files (" + e + ")");
         e.printStackTrace();
         return false;
      }
   }

   public static BigInteger generateSerialNo() {
      byte[] sn = new byte[12];
      Adler32 hash = new Adler32();
      hash.update(CertUtils.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getHostId().getBytes());
      ByteArrayUtil.writeInt(sn, 0, (int)hash.getValue());
      SecureRandom random = new SecureRandom();
      ByteArrayUtil.writeLong(sn, 4, random.nextLong());
      return new BigInteger(sn).abs();
   }

   private static void addKeyPurposeExtensions(X509v3CertificateBuilder certBuilder, KeyPurpose purpose, int keyUsage, boolean preferExisting) throws Exception {
      if (certBuilder != null && purpose != null) {
         KeyPurposeId[] usages = null;
         switch (purpose) {
            case CLIENT_CERT:
               keyUsage |= 128;
               usages = new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth};
               break;
            case SERVER_CERT:
               keyUsage |= 160;
               usages = new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth};
               break;
            case CA_CERT:
               keyUsage |= 6;
               if (!preferExisting && certBuilder.hasExtension(OidMap.BASIC_CONSTRAINTS)) {
                  certBuilder.replaceExtension(Extension.basicConstraints, true, new BasicConstraints(true));
               } else if (!certBuilder.hasExtension(OidMap.BASIC_CONSTRAINTS)) {
                  certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
               }
               break;
            case CODE_SIGNING_CERT:
               keyUsage |= 128;
               usages = new KeyPurposeId[]{KeyPurposeId.id_kp_codeSigning};
               certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(usages));
               break;
            default:
               throw new IllegalArgumentException("Invalid key purpose provided");
         }

         if (usages != null) {
            if (certBuilder.hasExtension(OidMap.EXTENDED_KEY_USAGE) && !preferExisting) {
               certBuilder.replaceExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(usages));
            } else if (!certBuilder.hasExtension(OidMap.EXTENDED_KEY_USAGE)) {
               certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(usages));
            }
         }

         if (certBuilder.hasExtension(OidMap.KEY_USAGE) && !preferExisting) {
            certBuilder.replaceExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));
         } else if (!certBuilder.hasExtension(OidMap.KEY_USAGE)) {
            certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));
         }
      }
   }

   public static KeyPair generateRsaKeyPair(int keySize) throws Exception {
      KeyPairGenerator kpGen;
      if (SecurityInitializer.getInstance().isFips()) {
         kpGen = KeyPairGenerator.getInstance("RSA");
      } else {
         kpGen = KeyPairGenerator.getInstance("RSA", "SunRsaSign");
      }

      SecureRandom random = new SecureRandom();
      kpGen.initialize(keySize, random);
      return kpGen.generateKeyPair();
   }

   public static KeyPair generateEcKeyPair(String standardName) throws Exception {
      KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
      ECGenParameterSpec spec = new ECGenParameterSpec(standardName);
      gen.initialize(spec, new SecureRandom());
      return gen.generateKeyPair();
   }

   public static void addUniqueCertificate(X509Certificate cert, ICoreTrustStore trustStore) throws Exception {
      if (trustStore.getCertificateAlias(cert) == null) {
         X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
         RDN cn = x500name.getRDNs(BCStyle.CN)[0];
         String alias = IETFUtils.valueToString(cn.getFirst().getValue());
         String uniqueAlias = getUniqueAlias(alias, trustStore);
         trustStore.setCertificateEntry(uniqueAlias, cert);
         trustStore.save();
      }
   }

   public static String getUniqueAlias(String baseAlias, ICoreTrustStore trustStore) throws Exception {
      int i = 0;
      String uniqueAlias = baseAlias;

      while (trustStore.containsAlias(uniqueAlias)) {
         uniqueAlias = baseAlias + ++i;
      }

      return uniqueAlias;
   }

   public static boolean isPrivateKey(KeyStore keyStore, String alias, char[] pass) throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
      Key key = getKey(keyStore, alias, pass);
      return key instanceof PrivateKey;
   }

   public static boolean isSupportedPrivateKey(Object pemObject) {
      return pemObject instanceof PEMKeyPair
         || pemObject instanceof PEMEncryptedKeyPair
         || pemObject instanceof PKCS8EncryptedPrivateKeyInfo
         || pemObject instanceof PrivateKeyInfo;
   }

   public static boolean isEncryptedPrivateKey(Object pemObject) {
      return pemObject instanceof PEMEncryptedKeyPair || pemObject instanceof PKCS8EncryptedPrivateKeyInfo;
   }

   public static boolean isSupportedCertificate(Object pemObject) {
      return pemObject instanceof X509Certificate || pemObject instanceof X509CertificateHolder;
   }

   public static X509Certificate getCertificateFromPemObject(Object pemObject) {
      try {
         if (pemObject instanceof X509Certificate) {
            return (X509Certificate)pemObject;
         }

         if (pemObject instanceof X509CertificateHolder) {
            return new JcaX509CertificateConverter().getCertificate((X509CertificateHolder)pemObject);
         }
      } catch (CertificateException e) {
         LOG.warning("Could not read certificate from PEM object");
      }

      return null;
   }

   public static X509Certificate[] parsePemCertificates(Reader reader) throws IOException {
      List<X509Certificate> parsedCertificates = new ArrayList<>();

      try {
         PEMParser pemParser = new PEMParser(reader);
         Throwable var3 = null;

         try {
            for (Object pemObject = pemParser.readObject(); pemObject != null; pemObject = pemParser.readObject()) {
               if (isSupportedCertificate(pemObject)) {
                  X509Certificate certificate = getCertificateFromPemObject(pemObject);
                  parsedCertificates.add(certificate);
               } else {
                  LOG.fine("Found non cert object in PEM file. Ignoring.");
               }
            }

            return sortCertChain(parsedCertificates).toArray(new X509Certificate[0]);
         } catch (Throwable var14) {
            var3 = var14;
            throw var14;
         } finally {
            if (pemParser != null) {
               if (var3 != null) {
                  try {
                     pemParser.close();
                  } catch (Throwable var13) {
                     var3.addSuppressed(var13);
                  }
               } else {
                  pemParser.close();
               }
            }
         }
      } catch (IOException e) {
         throw new IOException("Unable to read PEM string", e);
      }
   }

   public static PrivateKey parsePrivateKey(String privateKeyString, String decryptPassword) throws PrivateKeyDecryptionException, IllegalArgumentException {
      try (StringReader sin = new StringReader(privateKeyString)) {
         PEMParser in = new PEMParser(sin);
         Throwable var6 = null;

         PrivateKey privateKey;
         try {
            Object obj = in.readObject();
            if (obj instanceof PEMKeyPair) {
               privateKey = new JcaPEMKeyConverter()
                  .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
                  .getPrivateKey(((PEMKeyPair)obj).getPrivateKeyInfo());
            } else if (obj instanceof PEMEncryptedKeyPair) {
               try {
                  if (decryptPassword == null) {
                     throw new IllegalArgumentException("Private Key Decrypt Password cannot be null if key is encrypted");
                  }

                  JcePEMDecryptorProviderBuilder builder = new JcePEMDecryptorProviderBuilder()
                     .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider());
                  PEMDecryptorProvider decryptor = builder.build(decryptPassword.toCharArray());
                  JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider());
                  privateKey = converter.getPrivateKey(((PEMEncryptedKeyPair)obj).decryptKeyPair(decryptor).getPrivateKeyInfo());
               } catch (Exception e) {
                  throw new PrivateKeyDecryptionException("Unable to decrypt private key. Password may be invalid.", 2);
               }
            } else if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
               if (decryptPassword == null) {
                  throw new IllegalArgumentException("Private Key Decrypt Password cannot be null if key is encrypted");
               }

               try {
                  InputDecryptorProvider pkcs8Prov = new JcePKCSPBEInputDecryptorProviderBuilder()
                     .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
                     .build(decryptPassword.toCharArray());
                  JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider());
                  privateKey = converter.getPrivateKey(((PKCS8EncryptedPrivateKeyInfo)obj).decryptPrivateKeyInfo(pkcs8Prov));
               } catch (Exception e) {
                  throw new PrivateKeyDecryptionException("Unable to decrypt private key. Password may be invalid.", 2);
               }
            } else {
               if (!(obj instanceof PrivateKeyInfo)) {
                  throw new PrivateKeyDecryptionException("Unable to decrypt private key.", 1);
               }

               JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider());
               privateKey = converter.getPrivateKey((PrivateKeyInfo)obj);
            }
         } catch (Throwable var42) {
            var6 = var42;
            throw var42;
         } finally {
            if (in != null) {
               if (var6 != null) {
                  try {
                     in.close();
                  } catch (Throwable var39) {
                     var6.addSuppressed(var39);
                  }
               } else {
                  in.close();
               }
            }
         }

         return privateKey;
      } catch (IllegalArgumentException | PrivateKeyDecryptionException e) {
         throw e;
      } catch (Exception e) {
         throw new PrivateKeyDecryptionException("Unable to decrypt private key.", 1, e);
      }
   }

   static Key getKey(KeyStore keyStore, String alias, char[] pass) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
      try {
         return AccessController.doPrivileged(() -> keyStore.getKey(alias, pass));
      } catch (PrivilegedActionException e) {
         Exception cause = e.getException();
         if (cause instanceof KeyStoreException) {
            throw (KeyStoreException)cause;
         } else if (cause instanceof NoSuchAlgorithmException) {
            throw (NoSuchAlgorithmException)cause;
         } else if (cause instanceof UnrecoverableKeyException) {
            throw (UnrecoverableKeyException)cause;
         } else {
            throw new UnrecoverableKeyException("Could not recover key " + alias);
         }
      }
   }

   public static List<String> getCrlDistributionPointsFromCertificate(X509Certificate certificate) {
      if (certificate == null) {
         return Collections.emptyList();
      }

      byte[] data = certificate.getExtensionValue("2.5.29.31");
      if (data == null) {
         return Collections.emptyList();
      }

      DEROctetString octetString;
      try {
         ASN1InputStream crlDpExtensionInputStream = new ASN1InputStream(new ByteArrayInputStream(data));
         Throwable crlDP = null;

         try {
            octetString = (DEROctetString)crlDpExtensionInputStream.readObject();
         } catch (Throwable var39) {
            crlDP = var39;
            throw var39;
         } finally {
            if (crlDpExtensionInputStream != null) {
               if (crlDP != null) {
                  try {
                     crlDpExtensionInputStream.close();
                  } catch (Throwable var37) {
                     crlDP.addSuppressed(var37);
                  }
               } else {
                  crlDpExtensionInputStream.close();
               }
            }
         }
      } catch (IOException e) {
         return Collections.emptyList();
      }

      byte[] octets = octetString.getOctets();

      CRLDistPoint crlDP;
      try {
         ASN1InputStream crlDpInputStream = new ASN1InputStream(new ByteArrayInputStream(octets));
         Throwable var6 = null;

         try {
            crlDP = CRLDistPoint.getInstance(crlDpInputStream.readObject());
         } catch (Throwable var38) {
            var6 = var38;
            throw var38;
         } finally {
            if (crlDpInputStream != null) {
               if (var6 != null) {
                  try {
                     crlDpInputStream.close();
                  } catch (Throwable var36) {
                     var6.addSuppressed(var36);
                  }
               } else {
                  crlDpInputStream.close();
               }
            }
         }
      } catch (IOException e) {
         return Collections.emptyList();
      }

      List<String> distributionPointUrls = new ArrayList<>();

      for (DistributionPoint distributionPoint : crlDP.getDistributionPoints()) {
         DistributionPointName distributionPointName = distributionPoint.getDistributionPoint();
         if (distributionPointName != null && distributionPointName.getType() == 0) {
            GeneralName[] names = GeneralNames.getInstance(distributionPointName.getName()).getNames();

            for (GeneralName generalName : names) {
               if (generalName.getTagNo() == 6) {
                  String url = BouncyCastleHelper.getIA5StringValue(generalName);
                  distributionPointUrls.add(url);
               }
            }
         }
      }

      return distributionPointUrls;
   }

   public static List<X500Name> getCrlIssuersFromCertificate(X509Certificate certificate) {
      if (certificate == null) {
         return Collections.emptyList();
      }

      byte[] data = certificate.getExtensionValue("2.5.29.31");
      if (data == null) {
         return Collections.emptyList();
      }

      DEROctetString octetString;
      try {
         ASN1InputStream crlDpExtensionInputStream = new ASN1InputStream(new ByteArrayInputStream(data));
         Throwable crlDP = null;

         try {
            octetString = (DEROctetString)crlDpExtensionInputStream.readObject();
         } catch (Throwable var38) {
            crlDP = var38;
            throw var38;
         } finally {
            if (crlDpExtensionInputStream != null) {
               if (crlDP != null) {
                  try {
                     crlDpExtensionInputStream.close();
                  } catch (Throwable var36) {
                     crlDP.addSuppressed(var36);
                  }
               } else {
                  crlDpExtensionInputStream.close();
               }
            }
         }
      } catch (IOException e) {
         return Collections.emptyList();
      }

      byte[] octets = octetString.getOctets();

      CRLDistPoint crlDP;
      try {
         ASN1InputStream crlDpInputStream = new ASN1InputStream(new ByteArrayInputStream(octets));
         Throwable var6 = null;

         try {
            crlDP = CRLDistPoint.getInstance(crlDpInputStream.readObject());
         } catch (Throwable var37) {
            var6 = var37;
            throw var37;
         } finally {
            if (crlDpInputStream != null) {
               if (var6 != null) {
                  try {
                     crlDpInputStream.close();
                  } catch (Throwable var35) {
                     var6.addSuppressed(var35);
                  }
               } else {
                  crlDpInputStream.close();
               }
            }
         }
      } catch (IOException e) {
         return Collections.emptyList();
      }

      List<X500Name> crlIssuers = new ArrayList<>();

      for (DistributionPoint distributionPoint : crlDP.getDistributionPoints()) {
         GeneralNames crlIssuer = distributionPoint.getCRLIssuer();
         if (crlIssuer != null) {
            GeneralName[] names = crlIssuer.getNames();

            for (GeneralName generalName : names) {
               if (generalName.getTagNo() == 4) {
                  crlIssuers.add(X500Name.getInstance(generalName.getName()));
               }
            }
         }
      }

      return crlIssuers;
   }

   public static NX509CertificateBuilder createCertBuilderFromParameters(NCertificateParameters params) throws IOException {
      NX509CertificateBuilder builder = NX509CertificateBuilder.getInstance(params.getKeyPurpose(), params.getKeyUsage());
      builder.withAlias(params.getAlias())
         .withSubjectDN(params.getSubjectDn())
         .withIssuerDN(params.getIssuerDn())
         .withNotBefore(params.getNotBeforeDate())
         .withNotAfter(params.getNotAfterDate());
      Set<NGeneralName> sans = new HashSet<>();
      if (params.getEmail() != null && !params.getEmail().isEmpty()) {
         sans.add(NGeneralName.makeEmailName(params.getEmail()));
      }

      String cnName = null;
      if (params.getSubjectDn() != null && !params.getSubjectDn().isEmpty()) {
         cnName = PemSource.extractCommonName(new X500Name(params.getSubjectDn()));
         sans.add(NGeneralName.makeHostName(cnName));
      }

      if (params.getSubAltNameServer() != null && !params.getSubAltNameServer().isEmpty() && cnName != null && !cnName.equals(params.getSubAltNameServer())) {
         sans.add(NGeneralName.makeHostName(params.getSubAltNameServer()));
      }

      if (params.getSubAltNameUri() != null && !params.getSubAltNameUri().isEmpty()) {
         sans.add(NGeneralName.makeUniformResourceIdentifier(params.getSubAltNameUri()));
      }

      if (!sans.isEmpty()) {
         builder.withExtension(NSubjectAlternativeName.make(false, sans));
      }

      builder.withExtension(NIssuerAlternativeName.make(false, NGeneralName.makeHostID()));
      return builder;
   }

   public static String encodeX509Certificate(X509Certificate cert) throws IOException {
      return PemSource.getPEMString(cert);
   }

   public static String encodeX509CertificateChain(X509Certificate[] certificateChain) throws IOException {
      StringBuilder certificateChainPem = new StringBuilder();

      for (X509Certificate certificate : certificateChain) {
         certificateChainPem.append(PemSource.getPEMString(certificate));
      }

      return certificateChainPem.toString();
   }

   public static X509Certificate decodeX509Certificate(String encoded) throws IOException {
      Object obj = PemSource.getFromPEM(encoded);
      if (obj instanceof X509Certificate) {
         return (X509Certificate)obj;
      }

      if (obj instanceof X509CertificateHolder) {
         try {
            return new JcaX509CertificateConverter()
               .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
               .getCertificate((X509CertificateHolder)obj);
         } catch (CertificateException e) {
            throw new IOException(e);
         }
      } else {
         throw new IOException("unable to decode provided certificate string");
      }
   }

   public static Locale getAsn1FormatLocale() {
      return ASN1_FORMAT_LOCAL;
   }

   public static boolean isFailingFormatLocale() {
      return FAILING_FORMAT_LOCALES.contains(Locale.getDefault(Category.FORMAT));
   }

   static {
      FAILING_FORMAT_LOCALES.add(Locale.forLanguageTag("th-TH"));
      FAILING_FORMAT_LOCALES.add(Locale.forLanguageTag("th-Thai-TH"));
      FAILING_FORMAT_LOCALES.add(Locale.forLanguageTag("th-TH-u-nu-thai-x-lvariant-TH"));
      FAILING_FORMAT_LOCALES.add(Locale.forLanguageTag("ja-JP-u-ca-japanese-x-lvariant-JP"));
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
