package com.tridium.crypto.core.cert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.tsp.TimeStampResp;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaCertStoreBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;

public class SigningUtil {
   private static final int BUFFER_SIZE = 32768;

   public static byte[] generateSignature(byte[] dataBytes, String alias, String tsaUrlString, char[] password, KeyStore keyStore) throws Exception {
      Certificate[] cs = keyStore.getCertificateChain(alias);
      if (cs == null) {
         throw new SecurityException("Could not get certificate chain.");
      }

      X509Certificate[] certChain = new X509Certificate[cs.length];

      for (int i = 0; i < cs.length; i++) {
         if (!(cs[i] instanceof X509Certificate)) {
            throw new SecurityException("found non X509 certificate in signer's chain.");
         }

         certChain[i] = (X509Certificate)cs[i];
      }

      if (!isValidSigningCert(alias, keyStore)) {
         throw new SecurityException("Not a valid signing certificate.");
      }

      Key key = CertUtils.getKey(keyStore, alias, password);
      if (!(key instanceof PrivateKey)) {
         throw new SecurityException("not private key");
      }

      PrivateKey privateKey = (PrivateKey)key;
      String keyAlgorithm = privateKey.getAlgorithm();
      String signatureAlgorithm;
      if (keyAlgorithm.equalsIgnoreCase("DSA")) {
         signatureAlgorithm = "SHA1withDSA";
      } else if (keyAlgorithm.equalsIgnoreCase("RSA")) {
         signatureAlgorithm = "SHA256withRSA";
      } else {
         if (!keyAlgorithm.equalsIgnoreCase("EC")) {
            throw new SecurityException("private key is not a DSA or RSA key");
         }

         signatureAlgorithm = "SHA256withECDSA";
      }

      JcaCertStore certStore = new JcaCertStore(Arrays.asList(certChain));
      CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
      gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().build(signatureAlgorithm, privateKey, certChain[0]));
      gen.addCertificates(certStore);
      CMSProcessableByteArray msg = new CMSProcessableByteArray(dataBytes);
      CMSSignedData data = gen.generate(msg);
      SignerInformationStore siStore = data.getSignerInfos();
      Collection<SignerInformation> infoList = siStore.getSigners();
      SignerInformation info = (SignerInformation)infoList.toArray()[0];
      byte[] sigBaseBytes = info.getSignature();
      if (tsaUrlString != null && !tsaUrlString.equals("")) {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] digestBytes = digest.digest(sigBaseBytes);
         TimeStampRequestGenerator timeStampRequestGenerator = new TimeStampRequestGenerator();
         timeStampRequestGenerator.setCertReq(true);
         TimeStampRequest timeStampRequest = timeStampRequestGenerator.generate(TSPAlgorithms.SHA256, digestBytes, BigInteger.valueOf(100L));
         byte[] request = timeStampRequest.getEncoded();
         URL tsaUrl = new URL(tsaUrlString);
         HttpURLConnection con = (HttpURLConnection)tsaUrl.openConnection();
         con.setDoOutput(true);
         con.setDoInput(true);
         con.setRequestMethod("POST");
         con.setRequestProperty("Content-type", "application/timestamp-query");
         con.setRequestProperty("Content-length", String.valueOf(request.length));
         AccessController.doPrivileged(() -> {
            try (OutputStream out = con.getOutputStream()) {
               out.write(request);
               out.flush();
            } catch (Exception var15x) {
            }

            return null;
         });
         if (con.getResponseCode() != 200) {
            throw new IOException("Received HTTP error: " + con.getResponseCode() + " - " + con.getResponseMessage());
         }

         TimeStampToken token = null;

         try (InputStream tokenIn = con.getInputStream()) {
            TimeStampResp resp = TimeStampResp.getInstance(new ASN1InputStream(tokenIn).readObject());
            TimeStampResponse response = new TimeStampResponse(resp);
            response.validate(timeStampRequest);
            token = response.getTimeStampToken();
         }

         AttributeTable table = info.getUnsignedAttributes();
         ASN1ObjectIdentifier oid = PKCSObjectIdentifiers.id_aa_signatureTimeStampToken;
         Attribute signatureTimeStamp = new Attribute(oid, new DERSet(token.toCMSSignedData().toASN1Structure()));
         if (table == null) {
            table = new AttributeTable(signatureTimeStamp);
         } else {
            table.add(oid, signatureTimeStamp);
         }

         info = SignerInformation.replaceUnsignedAttributes(info, table);
         siStore = new SignerInformationStore(Arrays.asList(info));
         data = CMSSignedData.replaceSigners(data, siStore);
      }

      return data.getEncoded();
   }

   public static void verifySignature(byte[] data, byte[] signature, KeyStore[] trustStores) throws Exception {
      if (signature != null && signature.length > 0) {
         CMSSignedData sigData = new CMSSignedData(new CMSProcessableByteArray(data), signature);
         Store<?> certStore = sigData.getCertificates();
         SignerInformationStore signerStore = sigData.getSignerInfos();
         Collection<SignerInformation> signerCollection = signerStore.getSigners();
         SignerInformation signerInfo = signerCollection.iterator().next();
         Collection<?> signerCertCollection = certStore.getMatches(signerInfo.getSID());
         X509CertificateHolder signerCert = (X509CertificateHolder)signerCertCollection.iterator().next();
         if (!signerInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().build(signerCert))) {
            throw new SecurityException("failed signing verification");
         }

         AttributeTable unsignedAttrTable = signerInfo.getUnsignedAttributes();
         Date signingDate;
         if (unsignedAttrTable != null) {
            Attribute token = unsignedAttrTable.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            TimeStampToken tsToken = new TimeStampToken(ContentInfo.getInstance(token.getAttrValues().getObjectAt(0).toASN1Primitive().getEncoded()));
            signingDate = tsToken.getTimeStampInfo().getGenTime();
            Store<?> tscertStore = tsToken.getCertificates();
            Collection<?> tsCerts = tscertStore.getMatches(tsToken.getSID());
            Exception toThrow = null;

            for (KeyStore trustStore : trustStores) {
               try {
                  PKIXCertPathBuilderResult tsResult = checkCertPath(tsToken.getSID(), tscertStore, trustStore, signingDate);
                  toThrow = null;
                  break;
               } catch (Exception e) {
                  if (toThrow == null) {
                     toThrow = e;
                  }
               }
            }

            if (toThrow != null) {
               throw new SecurityException("failed signing verification", toThrow);
            }

            try {
               JcaSimpleSignerInfoVerifierBuilder sigInfVerBuilder = new JcaSimpleSignerInfoVerifierBuilder();
               SignerInformationVerifier verifier = sigInfVerBuilder.build((X509CertificateHolder & X509CertificateHolder)tsCerts.toArray()[0]);
               tsToken.validate(verifier);
            } catch (Exception e) {
               throw new SecurityException("failed signing verification", e);
            }
         } else {
            Date notBefore = signerCert.getNotBefore();
            Date notAfter = signerCert.getNotAfter();
            Date now = new Date();
            signingDate = now;
            if (notBefore.compareTo(now) > 0) {
               throw new SecurityException("certificate isn't valid yet");
            }

            if (notAfter.compareTo(now) < 0) {
               throw new SecurityException("certificate has expired");
            }
         }

         if (trustStores.length <= 0) {
            throw new SecurityException("no trust stores provided");
         }

         Exception toThrow = null;

         for (KeyStore trustStore : trustStores) {
            try {
               PKIXCertPathBuilderResult result = checkCertPath(signerInfo.getSID(), certStore, trustStore, signingDate);
               toThrow = null;
               break;
            } catch (Exception e) {
               if (toThrow == null) {
                  toThrow = e;
               }
            }
         }

         if (toThrow != null) {
            throw new CertificateNotTrustedException("Certificate not trusted", toThrow, getRootCertificate(certStore));
         }
      } else {
         throw new SecurityException("failed signing verification");
      }
   }

   public static boolean isTimestamped(byte[] data, byte[] signature) {
      try {
         CMSSignedData sigData = new CMSSignedData(new CMSProcessableByteArray(data), signature);
         SignerInformationStore signerStore = sigData.getSignerInfos();
         Collection<SignerInformation> signerCollection = signerStore.getSigners();
         SignerInformation signerInfo = signerCollection.iterator().next();
         AttributeTable unsignedAttrTable = signerInfo.getUnsignedAttributes();
         if (unsignedAttrTable != null) {
            Attribute token = unsignedAttrTable.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            if (token != null) {
               return true;
            }
         }
      } catch (Exception var8) {
      }

      return false;
   }

   public static PKIXCertPathBuilderResult checkCertPath(SignerId signerId, Store<?> certs, KeyStore trustStore, Date date) throws Exception {
      CertStore store = new JcaCertStoreBuilder().addCertificates(certs).build();
      CertPathBuilder pathBuilder = CertPathBuilder.getInstance("PKIX");
      X509CertSelector targetConstraints = new X509CertSelector();
      targetConstraints.setIssuer(signerId.getIssuer().getEncoded());
      targetConstraints.setSerialNumber(signerId.getSerialNumber());
      PKIXBuilderParameters params = new PKIXBuilderParameters(trustStore, targetConstraints);
      params.addCertStore(store);
      params.setRevocationEnabled(false);
      if (date != null) {
         params.setDate(date);
      }

      return (PKIXCertPathBuilderResult)pathBuilder.build(params);
   }

   public static boolean isValidSigningCert(String alias, KeyStore store) {
      try {
         if (!store.isKeyEntry(alias)) {
            return false;
         }

         try {
            PrivateKey pkey = (PrivateKey)CertUtils.getKey(store, alias, new char[0]);
            if (pkey != null) {
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

   public static Certificate getRootCertificate(Store<?> certs) throws Exception {
      CertStore store = new JcaCertStoreBuilder().addCertificates(certs).build();

      for (Certificate cert : store.getCertificates(null)) {
         try {
            cert.verify(cert.getPublicKey());
            return cert;
         } catch (Exception ignored) {
         }
      }

      return null;
   }

   public static Certificate getRootCertificate(byte[] data, byte[] signature) throws Exception {
      CMSSignedData sigData = new CMSSignedData(new CMSProcessableByteArray(data), signature);
      Store<?> certStore = sigData.getCertificates();
      return getRootCertificate(certStore);
   }

   public static void checkKey(KeyStore keyStore, String alias, char[] password) throws Exception {
      if (!keyStore.containsAlias(alias)) {
         throw new SecurityException("Entry not found for alias " + alias);
      }

      Certificate[] cs = keyStore.getCertificateChain(alias);
      if (cs == null) {
         throw new SecurityException("Could not get certificate chain.");
      }

      X509Certificate[] certChain = new X509Certificate[cs.length];

      for (int i = 0; i < cs.length; i++) {
         if (!(cs[i] instanceof X509Certificate)) {
            throw new SecurityException("Found non X509 certificate in signer's chain.");
         }

         certChain[i] = (X509Certificate)cs[i];
      }

      if (!isValidSigningCert(alias, keyStore)) {
         throw new SecurityException("Not a valid signing certificate.");
      }

      Key key = null;

      try {
         key = CertUtils.getKey(keyStore, alias, password);
      } catch (UnrecoverableKeyException e) {
         throw new SecurityException("Incorrect private key password.");
      }

      if (!(key instanceof PrivateKey)) {
         throw new SecurityException("Not a private key.");
      }
   }
}
