package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.cert.ext.NX509Extension;
import com.tridium.crypto.core.util.OidMap;
import com.tridium.nre.security.SecurityInitializer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.baja.nre.security.IX509Extension;
import javax.baja.nre.util.ByteArrayUtil;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;

public final class NPKCS10CertificationRequest extends PemSource {
   private JcaPKCS10CertificationRequest csr = null;
   private CertificationRequestInfo info = null;
   private IX509Extension[] extensions = null;

   public static NPKCS10CertificationRequest make(JcaPKCS10CertificationRequest csr) {
      return new NPKCS10CertificationRequest(csr);
   }

   public static NPKCS10CertificationRequest make(String encoded) throws Exception {
      return new NPKCS10CertificationRequest(decodeFromString(encoded));
   }

   private NPKCS10CertificationRequest(JcaPKCS10CertificationRequest csr) {
      this.csr = csr;
      this.info = csr.toASN1Structure().getCertificationRequestInfo();
   }

   @Override
   public int hashCode() {
      return this.csr.hashCode();
   }

   public X500Name getSubjectDN() {
      X500Name name = this.info.getSubject();
      String cleanDN = CertUtils.cleanDN(name.toString());
      return new X500Name(cleanDN);
   }

   public String getSubject() {
      X500Name name = this.info.getSubject();
      String cleanDN = CertUtils.cleanDN(name.toString());
      return extractFriendlyName(cleanDN);
   }

   public String getKeyAlgorithm() {
      try {
         return this.csr.getPublicKey().getAlgorithm();
      } catch (Exception e) {
         return "unknown";
      }
   }

   public int getKeySize() {
      try {
         return getPublicKeyLength(this.csr.getPublicKey());
      } catch (Exception e) {
         return -1;
      }
   }

   public String getSignatureAlgorithm() {
      return getSignatureName(this.csr);
   }

   public int getSignatureSize() {
      return this.csr.getSignature().length;
   }

   public boolean verify() {
      try {
         ContentVerifierProvider contentVerifierProvider = new JcaContentVerifierProviderBuilder()
            .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
            .build(this.csr.getPublicKey());
         return this.csr.isSignatureValid(contentVerifierProvider);
      } catch (Exception e) {
         return false;
      }
   }

   public JcaPKCS10CertificationRequest getRequest() {
      return this.csr;
   }

   public IX509Extension[] getExtensions() throws Exception {
      return getExtensions(this.info);
   }

   public static IX509Extension[] getExtensions(JcaPKCS10CertificationRequest csr) throws Exception {
      return getExtensions(csr.toASN1Structure().getCertificationRequestInfo());
   }

   public static IX509Extension[] getExtensions(CertificationRequestInfo info) throws Exception {
      ArrayList<IX509Extension> extList = new ArrayList<>();
      ASN1Set attributes = info.getAttributes();
      if (attributes != null) {
         for (int i = 0; i < attributes.size(); i++) {
            Attribute attr = Attribute.getInstance(attributes.getObjectAt(i));
            if (attr.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
               Extensions extensions = Extensions.getInstance(attr.getAttrValues().getObjectAt(0));
               Enumeration<?> oids = extensions.oids();

               while (oids.hasMoreElements()) {
                  ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier)oids.nextElement();
                  Extension ext = extensions.getExtension(oid);
                  IX509Extension next = NX509Extension.make(ext);
                  extList.add(next);
               }
            }
         }
      }

      return extList.toArray(new NX509Extension[0]);
   }

   @Override
   public boolean equals(Object obj) {
      if (this.csr == null || obj == null) {
         return false;
      }

      if (!(obj instanceof NPKCS10CertificationRequest)) {
         return false;
      }

      NPKCS10CertificationRequest ncsr = (NPKCS10CertificationRequest)obj;
      return this.csr.equals(ncsr.csr);
   }

   public String encodeToString() throws Exception {
      return PemSource.getPEMString(this.csr);
   }

   public static String encodeToString(CertificationRequest csr) throws Exception {
      return PemSource.getPEMString(csr);
   }

   public static JcaPKCS10CertificationRequest decodeFromString(String encoded) throws Exception {
      return new JcaPKCS10CertificationRequest((PKCS10CertificationRequest)PemSource.getFromPEM(encoded));
   }

   private String indentString(String msg, int indent) {
      StringBuilder resp = new StringBuilder();

      try (BufferedReader reader = new BufferedReader(new StringReader(msg))) {
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            for (int i = 0; i < indent; i++) {
               resp.append(" ");
            }

            resp.append(line).append("\n");
         }
      } catch (Exception var18) {
      }

      return resp.toString();
   }

   @Override
   public String toString() {
      StringWriter swriter = null;
      PrintWriter pwriter = null;
      StringBuilder out = new StringBuilder();
      CertificationRequestInfo info = this.csr.toASN1Structure().getCertificationRequestInfo();
      out.append("Certificate Request:\n");
      out.append("    Data:\n");
      out.append("        Version: " + info.getVersion() + "(0x" + ByteArrayUtil.toHexString(info.getVersion().getValue().toByteArray()) + ")\n");
      out.append("        Subject: " + info.getSubject().toString() + "\n");
      out.append("        Subject Public Key Info:\n");

      try {
         out.append("            Public Key Algorithm: " + this.csr.getPublicKey().getAlgorithm() + "\n");
      } catch (Exception var36) {
      }

      try {
         swriter = null;
         pwriter = null;
         out.append("                pub:\n");
         swriter = new StringWriter();
         pwriter = new PrintWriter(swriter);
         ByteArrayUtil.hexDump("                    ", pwriter, this.csr.getPublicKey().getEncoded(), 0, this.csr.getPublicKey().getEncoded().length);
         pwriter.flush();
         out.append(swriter.toString());
      } catch (Exception var35) {
      } finally {
         if (pwriter != null) {
            pwriter.close();
         }

         if (swriter != null) {
            try {
               swriter.close();
            } catch (IOException var33) {
            }
         }
      }

      try {
         IX509Extension[] extensions = this.getExtensions();

         for (int i = 0; i < extensions.length; i++) {
            out.append(this.indentString(extensions[i].toString(), 12) + "\n");
         }
      } catch (Exception var39) {
      }

      try {
         swriter = null;
         pwriter = null;
         out.append("\n    Signature Algorithm: " + getSignatureName(this.csr) + "\n");
         swriter = new StringWriter();
         pwriter = new PrintWriter(swriter);
         ByteArrayUtil.hexDump(
            "        ", pwriter, this.csr.toASN1Structure().getSignature().getEncoded(), 0, this.csr.toASN1Structure().getSignature().getEncoded().length
         );
         pwriter.flush();
         out.append(swriter.toString());
      } catch (Exception var34) {
      } finally {
         if (pwriter != null) {
            pwriter.close();
         }

         if (swriter != null) {
            try {
               swriter.close();
            } catch (IOException var32) {
            }
         }
      }

      return out.toString();
   }

   public static String getSignatureName(JcaPKCS10CertificationRequest csr) {
      return OidMap.get(csr.getSignatureAlgorithm().getAlgorithm().getId());
   }
}
