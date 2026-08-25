package com.tridium.nre.security;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.ICoreCryptoManager;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStoreBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;

public final class TextFileSignatureVerifier {
   private static final String SIG_START = "-----BEGIN NIAGARA SIGNATURE-----";
   private static final String SIG_END = "-----END NIAGARA SIGNATURE-----";

   private TextFileSignatureVerifier() {
   }

   public static void verifyFile(String fileName, TextFileSignatureVerifier.CommentLine comment, ISecurityInfoProvider info) throws Exception {
      verifyFile(fileName, comment, CoreCryptoManager.get(info));
   }

   public static void verifyFile(String fileName, TextFileSignatureVerifier.CommentLine comment, ICoreCryptoManager ccm) throws Exception {
      try {
         boolean inSigBlock = false;
         StringBuilder sigString = new StringBuilder();

         byte[] bBytes;
         try (
            BufferedReader bReader = new BufferedReader(new FileReader(fileName));
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
         ) {
            String line;
            while ((line = bReader.readLine()) != null) {
               if (line.contains("-----BEGIN NIAGARA SIGNATURE-----")) {
                  if (inSigBlock) {
                     throw new IllegalArgumentException("an invalid formatted signature blocks seems to be present");
                  }

                  inSigBlock = true;
               } else if (line.contains("-----END NIAGARA SIGNATURE-----")) {
                  if (!inSigBlock) {
                     throw new IllegalArgumentException("an invalid formatted signature blocks seems to be present");
                  }

                  inSigBlock = false;
               } else if (inSigBlock) {
                  if (line.startsWith(comment.toString())) {
                     sigString.append(line.substring(comment.size()).trim());
                  }
               } else {
                  bOut.write(line.getBytes(StandardCharsets.UTF_8));
               }
            }

            bBytes = bOut.toByteArray();
         }

         if (sigString.length() <= 0) {
            throw new SecurityException(fileName + " failed signing verification");
         }

         CMSSignedData sigData = new CMSSignedData(new CMSProcessableByteArray(bBytes), Base64.getDecoder().decode(sigString.toString()));
         Store<?> certStore = sigData.getCertificates();
         SignerInformationStore signerStore = sigData.getSignerInfos();
         Collection<SignerInformation> signerCollection = signerStore.getSigners();
         SignerInformation signerInfo = signerCollection.iterator().next();
         Collection<?> signerCertCollection = certStore.getMatches(signerInfo.getSID());
         X509CertificateHolder signerCert = (X509CertificateHolder)signerCertCollection.iterator().next();
         if (!signerInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().build(signerCert))) {
            throw new SecurityException(fileName + " failed signing verification");
         }

         AttributeTable unsignedAttrTable = signerInfo.getUnsignedAttributes();
         Date signingDate;
         if (unsignedAttrTable != null) {
            Attribute token = unsignedAttrTable.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            TimeStampToken tsToken = new TimeStampToken(ContentInfo.getInstance(token.getAttrValues().getObjectAt(0).toASN1Primitive().getEncoded()));
            signingDate = tsToken.getTimeStampInfo().getGenTime();
            Store<?> tscertStore = tsToken.getCertificates();
            Collection<?> tsCerts = tscertStore.getMatches(tsToken.getSID());

            try {
               PKIXCertPathBuilderResult e = checkCertPath(tsToken.getSID(), tscertStore, ccm.getSystemTrustStore().getKeyStore(), signingDate);
            } catch (Exception e) {
               try {
                  PKIXCertPathBuilderResult verifier = checkCertPath(tsToken.getSID(), tscertStore, ccm.getUserTrustStore().getKeyStore(), signingDate);
               } catch (Exception e2) {
                  throw new SecurityException(fileName + " failed signing verification", e2);
               }
            }

            try {
               JcaSimpleSignerInfoVerifierBuilder sigInfVerBuilder = new JcaSimpleSignerInfoVerifierBuilder();
               SignerInformationVerifier verifier = sigInfVerBuilder.build((X509CertificateHolder & X509CertificateHolder)tsCerts.toArray()[0]);
               tsToken.validate(verifier);
            } catch (Exception e) {
               throw new SecurityException(fileName + " failed signing verification", e);
            }
         } else {
            Date notBefore = signerCert.getNotBefore();
            Date notAfter = signerCert.getNotAfter();
            Date now = new Date();
            signingDate = now;
            if (notBefore.compareTo(now) > 0) {
               throw new SecurityException(fileName + " certificate isn't valid yet");
            }

            if (notAfter.compareTo(now) < 0) {
               throw new SecurityException(fileName + " certificate has expired");
            }
         }

         PKIXCertPathBuilderResult var61 = checkCertPath(signerInfo.getSID(), certStore, ccm.getSystemTrustStore().getKeyStore(), signingDate);
         if (SecurityConstants.canCheckTpk()) {
            X509Certificate cert = new JcaX509CertificateConverter().getCertificate(signerCert);
            SecurityConstants.checkTpk(fileName, cert);
         }
      } catch (Exception se) {
         if (SecurityConstants.canCheckTpk()) {
            throw se;
         }
      }
   }

   private static PKIXCertPathBuilderResult checkCertPath(SignerId signerId, Store<?> certs, KeyStore trustStore, Date date) throws Exception {
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

   public enum CommentLine {
      POLICY("// "),
      SECURITY("# ");

      private final String comment;
      private final int size;

      CommentLine(String comment) {
         this.comment = comment;
         this.size = comment.length();
      }

      public int size() {
         return this.size;
      }

      @Override
      public String toString() {
         return this.comment;
      }
   }
}
