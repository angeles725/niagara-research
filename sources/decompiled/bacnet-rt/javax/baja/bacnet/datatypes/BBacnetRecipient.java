package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.worker.IBacnetAddress;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "choice",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,1)")}
   ), @NiagaraProperty(
      name = "device",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.DEVICE)"
   ), @NiagaraProperty(
      name = "address",
      type = "BBacnetAddress",
      defaultValue = "new BBacnetAddress()"
   )})
public final class BBacnetRecipient extends BStruct implements BIBacnetDataType, IBacnetAddress {
   public static final Property choice = newProperty(0, 0, BFacets.makeInt(0, 1));
   public static final Property device = newProperty(0, BBacnetObjectIdentifier.make(8), null);
   public static final Property address = newProperty(0, new BBacnetAddress(), null);
   public static final Type TYPE = Sys.loadType(BBacnetRecipient.class);
   public static final int DEVICE_TAG = 0;
   public static final int ADDRESS_TAG = 1;

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public BBacnetObjectIdentifier getDevice() {
      return (BBacnetObjectIdentifier)this.get(device);
   }

   public void setDevice(BBacnetObjectIdentifier v) {
      this.set(device, v, null);
   }

   @Override
   public BBacnetAddress getAddress() {
      return (BBacnetAddress)this.get(address);
   }

   public void setAddress(BBacnetAddress v) {
      this.set(address, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetRecipient() {
   }

   public BBacnetRecipient(BBacnetObjectIdentifier device) {
      this.setChoice(0);
      this.setDevice(device);
   }

   public BBacnetRecipient(BBacnetAddress address) {
      this.setChoice(1);
      this.getAddress().copyFrom(address);
   }

   public boolean isDevice() {
      return this.getChoice() == 0;
   }

   public boolean isAddress() {
      return this.getChoice() == 1;
   }

   public BValue getRecipient() {
      return (BValue)(this.getChoice() == 0 ? this.getDevice() : this.getAddress());
   }

   public void setRecipient(BValue v) {
      this.setRecipient(v, null);
   }

   public void setRecipient(BValue v, Context cx) {
      Type t = v.getType();
      if (t == BBacnetObjectIdentifier.TYPE) {
         this.setInt(choice, 0, cx);
         this.set(device, v, cx);
      } else if (t == BBacnetAddress.TYPE) {
         this.setInt(choice, 1, cx);
         this.getAddress().copyFrom((BBacnetAddress)v, cx);
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      switch (this.getChoice()) {
         case 0:
            out.writeObjectIdentifier(0, this.getDevice());
            break;
         case 1:
            out.writeOpeningTag(1);
            this.getAddress().writeAsn(out);
            out.writeClosingTag(1);
            break;
         default:
            throw new IllegalStateException("Invalid recipient type:" + this.getChoice());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int choice = in.peekTag();
      switch (choice) {
         case 0:
            this.set(device, in.readObjectIdentifier(0), noWrite);
            break;
         case 1:
            in.skipOpeningTag(1);
            BBacnetAddress address = new BBacnetAddress();
            address.readAsn(in);
            in.skipClosingTag(1);
            this.set(BBacnetRecipient.address, address, noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + choice);
      }

      this.setInt(BBacnetRecipient.choice, choice, noWrite);
   }

   public boolean equivalent(Object o) {
      if (o instanceof BBacnetRecipient) {
         BBacnetRecipient other = (BBacnetRecipient)o;
         int choice = this.getChoice();
         if (choice != other.getChoice()) {
            return false;
         }

         switch (choice) {
            case 0:
               return this.getDevice().equals(other.getDevice());
            case 1:
               int networkNumber = this.getAddress().getNetworkNumber();
               if (networkNumber != other.getAddress().getNetworkNumber()) {
                  return false;
               }

               return this.getAddress().macEquals(other.getAddress().getMacAddress().getAddr());
         }
      }

      return false;
   }

   public String toString(Context context) {
      return this.getRecipient().toString(context);
   }
}
