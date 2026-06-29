package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "recipient",
      type = "BBacnetRecipient",
      defaultValue = "new BBacnetRecipient()"
   ), @NiagaraProperty(
      name = "processIdentifier",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)"
   )})
public final class BBacnetRecipientProcess extends BStruct implements BIBacnetDataType {
   public static final Property recipient = newProperty(0, new BBacnetRecipient(), null);
   public static final Property processIdentifier = newProperty(0, BBacnetUnsigned.make(0L), null);
   public static final Type TYPE = Sys.loadType(BBacnetRecipientProcess.class);
   public static final int RECIPIENT_TAG = 0;
   public static final int PROCESS_ID_TAG = 1;

   public BBacnetRecipient getRecipient() {
      return (BBacnetRecipient)this.get(recipient);
   }

   public void setRecipient(BBacnetRecipient v) {
      this.set(recipient, v, null);
   }

   public BBacnetUnsigned getProcessIdentifier() {
      return (BBacnetUnsigned)this.get(processIdentifier);
   }

   public void setProcessIdentifier(BBacnetUnsigned v) {
      this.set(processIdentifier, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetRecipientProcess() {
   }

   public BBacnetRecipientProcess(BBacnetRecipient recipient, BBacnetUnsigned processIdentifier) {
      this.setRecipient(recipient);
      this.setProcessIdentifier(processIdentifier);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      this.getRecipient().writeAsn(out);
      out.writeClosingTag(0);
      out.writeUnsigned(1, this.getProcessIdentifier());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      in.skipOpeningTag(0);
      BBacnetRecipient recipient = new BBacnetRecipient();
      recipient.readAsn(in);
      in.skipClosingTag(0);
      BBacnetUnsigned processIdentifier = in.readUnsigned(1);
      this.set(BBacnetRecipientProcess.recipient, recipient, noWrite);
      this.set(BBacnetRecipientProcess.processIdentifier, processIdentifier, noWrite);
   }

   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.nameContext)
         ? this.getRecipient().toString(cx) + "_id_" + this.getProcessIdentifier().toString()
         : "recip:" + this.getRecipient().toString(cx) + "; procID:" + this.getProcessIdentifier().toString();
   }
}
