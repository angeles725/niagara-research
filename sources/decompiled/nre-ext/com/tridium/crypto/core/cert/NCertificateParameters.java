package com.tridium.crypto.core.cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Date;

public class NCertificateParameters {
   private String alias;
   private String subjectDn;
   private String issuerDn;
   private Date notBeforeDate;
   private Date notAfterDate;
   private int keySize;
   private KeyPurpose keyPurpose;
   private String subAltNameServer;
   private String subAltNameUri;
   private String email;
   private String password;
   private int keyUsage;

   public NCertificateParameters(
      String alias,
      String subjectDn,
      String issuerDn,
      Date notBeforeDate,
      Date notAfterDate,
      int keySize,
      KeyPurpose keyPurpose,
      String subAltName,
      String email,
      String password
   ) {
      this(alias, subjectDn, issuerDn, notBeforeDate, notAfterDate, keySize, keyPurpose, subAltName, null, email, password, 0);
   }

   public NCertificateParameters(
      String alias,
      String subjectDn,
      String issuerDn,
      Date notBeforeDate,
      Date notAfterDate,
      int keySize,
      KeyPurpose keyPurpose,
      String subAltNameServer,
      String subAltNameUri,
      String email,
      String password,
      int keyUsage
   ) {
      this.alias = alias;
      this.subjectDn = subjectDn;
      this.issuerDn = issuerDn;
      this.notBeforeDate = notBeforeDate;
      this.notAfterDate = notAfterDate;
      this.keySize = keySize;
      this.keyPurpose = keyPurpose;
      this.subAltNameServer = subAltNameServer;
      this.subAltNameUri = subAltNameUri;
      this.email = email;
      this.password = password;
      this.keyUsage = keyUsage;
   }

   public NCertificateParameters(String encodedString) throws IOException, ClassNotFoundException {
      this.decodeFromString(encodedString);
   }

   public String getAlias() {
      return this.alias;
   }

   public String getSubjectDn() {
      return this.subjectDn;
   }

   public String getIssuerDn() {
      return this.issuerDn;
   }

   public Date getNotBeforeDate() {
      return this.notBeforeDate;
   }

   public Date getNotAfterDate() {
      return this.notAfterDate;
   }

   public int getKeySize() {
      return this.keySize;
   }

   public KeyPurpose getKeyPurpose() {
      return this.keyPurpose;
   }

   public String getSubAltNameServer() {
      return this.subAltNameServer;
   }

   public String getSubAltNameUri() {
      return this.subAltNameUri;
   }

   public String getEmail() {
      return this.email;
   }

   public String getPassword() {
      return this.password;
   }

   public int getKeyUsage() {
      return this.keyUsage;
   }

   public String encodeToString() throws IOException {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bout);
      out.writeObject(this.alias);
      out.writeObject(this.subjectDn);
      out.writeObject(this.issuerDn);
      out.writeObject(this.notBeforeDate);
      out.writeObject(this.notAfterDate);
      out.writeInt(this.keySize);
      out.writeObject(this.keyPurpose);
      out.writeObject(this.subAltNameServer);
      out.writeObject(this.subAltNameUri);
      out.writeObject(this.email);
      out.writeObject(this.password);
      out.writeInt(this.keyUsage);
      out.close();
      bout.close();
      return Base64.getEncoder().encodeToString(bout.toByteArray());
   }

   private void decodeFromString(String encodedString) throws IOException, ClassNotFoundException {
      byte[] data = Base64.getDecoder().decode(encodedString);

      try (
         ByteArrayInputStream bin = new ByteArrayInputStream(data);
         ObjectInputStream in = new ObjectInputStream(bin);
      ) {
         this.alias = (String)in.readObject();
         this.subjectDn = (String)in.readObject();
         this.issuerDn = (String)in.readObject();
         this.notBeforeDate = (Date)in.readObject();
         this.notAfterDate = (Date)in.readObject();
         this.keySize = in.readInt();
         this.keyPurpose = (KeyPurpose)in.readObject();
         this.subAltNameServer = (String)in.readObject();
         this.subAltNameUri = (String)in.readObject();
         this.email = (String)in.readObject();
         this.password = (String)in.readObject();
         if (in.available() > 0) {
            this.keyUsage = in.readInt();
         } else {
            this.keyUsage = 0;
         }
      }
   }
}
