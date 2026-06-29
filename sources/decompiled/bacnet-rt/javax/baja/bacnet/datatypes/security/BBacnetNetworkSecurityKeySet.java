package javax.baja.bacnet.datatypes.security;

import java.util.Collection;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "keyRevision",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "activationTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   ), @NiagaraProperty(
      name = "expirationTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   )})
public final class BBacnetNetworkSecurityKeySet extends BComponent implements BIBacnetDataType {
   public static final Property keyRevision = newProperty(0, 0, null);
   public static final Property activationTime = newProperty(0, new BBacnetDateTime(), null);
   public static final Property expirationTime = newProperty(0, new BBacnetDateTime(), null);
   public static final Type TYPE = Sys.loadType(BBacnetNetworkSecurityKeySet.class);
   public static final int KEY_REVISION_TAG = 0;
   public static final int ACTIVATION_TAG = 1;
   public static final int EXPIRATION_TAG = 2;
   public static final int KEY_IDS_TAG = 3;

   public int getKeyRevision() {
      return this.getInt(keyRevision);
   }

   public void setKeyRevision(int v) {
      this.setInt(keyRevision, v, null);
   }

   public BBacnetDateTime getActivationTime() {
      return (BBacnetDateTime)this.get(activationTime);
   }

   public void setActivationTime(BBacnetDateTime v) {
      this.set(activationTime, v, null);
   }

   public BBacnetDateTime getExpirationTime() {
      return (BBacnetDateTime)this.get(expirationTime);
   }

   public void setExpirationTime(BBacnetDateTime v) {
      this.set(expirationTime, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetNetworkSecurityKeySet() {
   }

   public BBacnetNetworkSecurityKeySet(BBacnetDateTime activationTime, BBacnetDateTime expirationTime, int keyRevision, Collection<BBacnetKeyIdentifier> keyIds) {
      this.setActivationTime(activationTime);
      this.setExpirationTime(expirationTime);
      this.setKeyRevision(keyRevision);
      int i = 0;

      for (BBacnetKeyIdentifier keyId : keyIds) {
         this.add("keyId" + i++, keyId, null);
      }
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BBacnetNetworkSecurityKeySet:");
      sb.append(this.getActivationTime()).append(":").append(this.getExpirationTime()).append(":").append(this.getKeyRevision());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsignedInteger(0, this.getKeyRevision());
      out.writeOpeningTag(1);
      this.getActivationTime().writeAsn(out);
      out.writeClosingTag(1);
      out.writeOpeningTag(2);
      this.getExpirationTime().writeAsn(out);
      out.writeClosingTag(2);
      out.writeOpeningTag(3);

      for (BBacnetKeyIdentifier keyId : (BBacnetKeyIdentifier[])this.getChildren(BBacnetKeyIdentifier.class)) {
         keyId.writeAsn(out);
      }

      out.writeClosingTag(3);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      this.set(keyRevision, in.readUnsigned(0), noWrite);
      in.skipOpeningTag(1);
      this.getActivationTime().readAsn(in);
      in.skipClosingTag(1);
      in.skipOpeningTag(1);
      this.getActivationTime().readAsn(in);
      in.skipClosingTag(1);
      in.skipOpeningTag(3);
      int i = 0;

      do {
         BBacnetKeyIdentifier keyId = new BBacnetKeyIdentifier();
         keyId.readAsn(in);
         this.add("keyId" + i++, keyId, null);
      } while (!in.isClosingTag(3));

      in.skipClosingTag(3);
   }
}
