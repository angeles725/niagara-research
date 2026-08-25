package com.tridium.crypto.core.cert;

import com.tridium.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import javax.baja.nre.security.IX509Certificate;

public class NHostExemption {
   private static final int VERSION = 3;
   private static final String HOST_NAME = "host";
   private static final String APPROVED_NAME = "approved";
   private static final String CREATED_NAME = "created";
   private static final String CERT_NAME = "certificate";
   private static final String CHANGED_NAME = "changed";
   private static final String CHANGED_CERT_NAME = "changedCert";
   private final String host;
   private IX509Certificate certificate;
   private IX509Certificate changed = null;
   private boolean approved;
   private boolean isTransient;
   private boolean isReverseDns = false;
   private final Date created;
   private byte[] publicKeyHash;

   public static NHostExemption make(IX509Certificate cert, String host, boolean approved) {
      return new NHostExemption(cert, host, approved, false);
   }

   public static NHostExemption make(IX509Certificate cert, String host, boolean approved, boolean isTransient) {
      return new NHostExemption(cert, host, approved, isTransient);
   }

   public static NHostExemption make(String encoded) throws Exception {
      return decodeFromString(encoded);
   }

   private NHostExemption(IX509Certificate cert, String host, boolean approved, boolean isTransient) {
      this(cert, host, approved, isTransient, new Date());
   }

   private NHostExemption(IX509Certificate cert, String host, boolean approved, boolean isTransient, Date created) {
      this.host = host.toLowerCase();
      this.setCertificate(cert);
      this.approved = approved;
      this.created = created;
      this.isTransient = isTransient;
   }

   public void setCertificate(IX509Certificate cert) {
      this.certificate = cert;
      this.publicKeyHash = cert.getPublicKeyHash();
   }

   public IX509Certificate getCertificate() {
      return this.certificate;
   }

   public String getHost() {
      return this.host;
   }

   public void setApproved(boolean approved) {
      this.approved = approved;
   }

   public void approveChanged() {
      this.setCertificate(this.changed);
      this.changed = null;
   }

   public boolean getApproved() {
      return this.approved;
   }

   public void setChanged(NX509Certificate cert) {
      this.changed = cert;
   }

   public IX509Certificate getChanged() {
      return this.changed;
   }

   public byte[] getPublicKeyHash() {
      return this.publicKeyHash;
   }

   public String getSHA1Fingerprint() {
      return this.certificate.getSHA1Fingerprint();
   }

   public Date getCreated() {
      return this.created;
   }

   public boolean isTransient() {
      return this.isTransient;
   }

   public void setTransient(boolean isTransient) {
      this.isTransient = isTransient;
   }

   public boolean isReverseDns() {
      return this.isReverseDns;
   }

   private void setReverseDns(boolean isReverseDns) {
      this.isReverseDns = isReverseDns;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.approved ? 1231 : 1237);
      result = 31 * result + (this.isTransient ? 1231 : 1237);
      result = 31 * result + (this.isReverseDns ? 1231 : 1237);
      result = 31 * result + (this.certificate == null ? 0 : this.certificate.hashCode());
      result = 31 * result + (this.created == null ? 0 : this.created.hashCode());
      result = 31 * result + (this.host == null ? 0 : this.host.hashCode());
      return 31 * result + Arrays.hashCode(this.publicKeyHash);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (!(obj instanceof NHostExemption)) {
         return false;
      }

      NHostExemption other = (NHostExemption)obj;
      if (this.approved != other.approved) {
         return false;
      }

      if (this.isTransient != other.isTransient) {
         return false;
      }

      if (this.isReverseDns != other.isReverseDns) {
         return false;
      }

      if (this.certificate == null) {
         if (other.certificate != null) {
            return false;
         }
      } else if (!this.certificate.equals(other.certificate)) {
         return false;
      }

      if (this.created == null) {
         if (other.created != null) {
            return false;
         }
      } else if (!this.created.equals(other.created)) {
         return false;
      }

      if (this.host == null) {
         if (other.host != null) {
            return false;
         }
      } else if (!this.host.equals(other.host)) {
         return false;
      }

      return Arrays.equals(this.publicKeyHash, other.publicKeyHash);
   }

   public String encodeToString() throws Exception {
      return this.encodeToString(this);
   }

   public String encodeToString(NHostExemption exemption) throws Exception {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(bout);
      out.writeUTF(this.host);
      out.writeUTF(exemption.getCertificate().encodeToString());
      out.writeBoolean(exemption.getApproved());
      out.writeLong(exemption.getCreated().getTime());
      if (this.changed != null) {
         out.writeBoolean(true);
         out.writeUTF(exemption.getChanged().encodeToString());
      } else {
         out.writeBoolean(false);
      }

      out.writeInt(3);
      out.writeBoolean(this.isTransient);
      out.writeBoolean(this.isReverseDns);
      out.close();
      bout.close();
      return Base64.getEncoder().encodeToString(bout.toByteArray());
   }

   public static NHostExemption decodeFromString(String s) throws Exception {
      byte[] data = Base64.getDecoder().decode(s);
      ByteArrayInputStream bin = new ByteArrayInputStream(data);
      ObjectInputStream in = new ObjectInputStream(bin);
      String host = in.readUTF();
      IX509Certificate cert = NX509Certificate.make(in.readUTF());
      boolean approved = in.readBoolean();
      Date created = new Date(in.readLong());
      NHostExemption exemption = new NHostExemption(cert, host, approved, false, created);
      boolean hasChanged = in.readBoolean();
      if (hasChanged) {
         exemption.setChanged(NX509Certificate.make(in.readUTF()));
      }

      int version = 1;
      if (in.available() > 0) {
         version = in.readInt();
      }

      switch (version) {
         case 1:
            if (approved) {
               exemption.setReverseDns(true);
            }
            break;
         case 2:
            if (approved) {
               exemption.setReverseDns(true);
            }

            exemption.setTransient(in.readBoolean());
            break;
         case 3:
            exemption.setTransient(in.readBoolean());
            exemption.setReverseDns(in.readBoolean());
      }

      return exemption;
   }

   public JSONObject getJSON() {
      JSONObject exemptionJSON = new JSONObject();
      exemptionJSON.put("host", this.host);
      exemptionJSON.put("approved", this.getApproved());
      exemptionJSON.put("created", this.getCreated().getTime());
      exemptionJSON.put("certificate", new JSONObject(this.getCertificate().getJSON()));
      JSONObject changedJSON = new JSONObject();
      if (this.changed != null) {
         changedJSON.put("changed", true);
         changedJSON.put("changedCert", new JSONObject(this.changed.getJSON()));
      } else {
         changedJSON.put("changed", false);
      }

      exemptionJSON.put("changed", changedJSON);
      return exemptionJSON;
   }
}
