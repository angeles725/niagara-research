package javax.baja.bacnet.datatypes.access;

import javax.baja.bacnet.datatypes.BBacnetDeviceObjectReference;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
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
      name = "credentialDataInput",
      type = "BBacnetDeviceObjectReference",
      defaultValue = "new BBacnetDeviceObjectReference()"
   ), @NiagaraProperty(
      name = "index",
      type = "int",
      defaultValue = "0"
   )})
public final class BBacnetAuthenticationPolicyEntry extends BStruct implements BIBacnetDataType {
   public static final Property credentialDataInput = newProperty(0, new BBacnetDeviceObjectReference(), null);
   public static final Property index = newProperty(0, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetAuthenticationPolicyEntry.class);
   public static int CREDENTIAL_DATA_INPUT_TAG = 0;
   public static int INDEX_TAG = 1;

   public BBacnetDeviceObjectReference getCredentialDataInput() {
      return (BBacnetDeviceObjectReference)this.get(credentialDataInput);
   }

   public void setCredentialDataInput(BBacnetDeviceObjectReference v) {
      this.set(credentialDataInput, v, null);
   }

   public int getIndex() {
      return this.getInt(index);
   }

   public void setIndex(int v) {
      this.setInt(index, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAuthenticationPolicyEntry() {
   }

   public BBacnetAuthenticationPolicyEntry(BBacnetDeviceObjectReference deviceObjectReference, int index) {
      this.setCredentialDataInput(deviceObjectReference);
      this.setIndex(index);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("APE(");
      sb.append(this.getIndex());
      sb.append(":");
      sb.append(this.getCredentialDataInput());
      sb.append(")");
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(CREDENTIAL_DATA_INPUT_TAG);
      this.getCredentialDataInput().writeAsn(out);
      out.writeClosingTag(CREDENTIAL_DATA_INPUT_TAG);
      out.writeUnsignedInteger(INDEX_TAG, this.getIndex());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetDeviceObjectReference credentialDataInput = new BBacnetDeviceObjectReference();
      in.skipOpeningTag(CREDENTIAL_DATA_INPUT_TAG);
      credentialDataInput.readAsn(in);
      in.skipClosingTag(CREDENTIAL_DATA_INPUT_TAG);
      int index = in.readUnsignedInt(INDEX_TAG);
      this.set(BBacnetAuthenticationPolicyEntry.credentialDataInput, credentialDataInput, noWrite);
      this.setInt(BBacnetAuthenticationPolicyEntry.index, index, noWrite);
   }
}
