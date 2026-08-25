package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.cert.ext.NX509Extension;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import javax.baja.nre.security.IX509Extension;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;

public class NSigningParameters {
   private final Date notBefore;
   private final Date notAfter;
   private KeyPurpose keyPurpose = null;
   private String subjectDn = null;
   private final HashMap<ASN1ObjectIdentifier, IX509Extension> extensions = new HashMap<>();

   public static NSigningParameters make(Date notBefore, Date notAfter) {
      return new NSigningParameters(notBefore, notAfter);
   }

   public static NSigningParameters make(Date notBefore, Date notAfter, KeyPurpose purpose) {
      return new NSigningParameters(notBefore, notAfter, purpose);
   }

   private NSigningParameters(Date notBefore, Date notAfter) {
      this.notBefore = notBefore;
      this.notAfter = notAfter;
   }

   private NSigningParameters(Date notBefore, Date notAfter, KeyPurpose purpose) {
      this.notBefore = notBefore;
      this.notAfter = notAfter;
      this.keyPurpose = purpose;
   }

   public String getSubjectDn() {
      return this.subjectDn;
   }

   public void setSubjectDn(String subjectDn) {
      this.subjectDn = subjectDn;
   }

   public KeyPurpose getKeyPurpose() {
      return this.keyPurpose;
   }

   public void setKeyPurpose(KeyPurpose keyPurpose) {
      this.keyPurpose = keyPurpose;
   }

   public Date getNotBefore() {
      return this.notBefore;
   }

   public Date getNotAfter() {
      return this.notAfter;
   }

   public void removeExtension(ASN1ObjectIdentifier oid) {
      this.extensions.put(oid, null);
   }

   public void addExtension(IX509Extension extension) {
      this.extensions.put(extension.getOid(), extension);
   }

   public void addExtensions(NX509Extension[] extensions) {
      for (int i = 0; i < extensions.length; i++) {
         this.extensions.put(extensions[i].getOid(), extensions[i]);
      }
   }

   public NX509Extension[] getExtensions() {
      Collection<IX509Extension> values = this.extensions.values();
      return values.toArray(new NX509Extension[0]);
   }
}
