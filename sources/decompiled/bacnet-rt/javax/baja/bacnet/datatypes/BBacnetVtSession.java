package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "localVtSessionId",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("BFacets.makeInt(0, 255)")}
   ), @NiagaraProperty(
      name = "remoteVtSessionId",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("BFacets.makeInt(0, 255)")}
   ), @NiagaraProperty(
      name = "remoteVtAddress",
      type = "BBacnetAddress",
      defaultValue = "BBacnetAddress.DEFAULT"
   )})
public final class BBacnetVtSession extends BStruct implements BIBacnetDataType {
   public static final Property localVtSessionId = newProperty(0, BBacnetUnsigned.DEFAULT, BFacets.makeInt(0, 255));
   public static final Property remoteVtSessionId = newProperty(0, BBacnetUnsigned.DEFAULT, BFacets.makeInt(0, 255));
   public static final Property remoteVtAddress = newProperty(0, BBacnetAddress.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetVtSession.class);
   public static final int MAX_ENCODED_SIZE = 15;

   public BBacnetUnsigned getLocalVtSessionId() {
      return (BBacnetUnsigned)this.get(localVtSessionId);
   }

   public void setLocalVtSessionId(BBacnetUnsigned v) {
      this.set(localVtSessionId, v, null);
   }

   public BBacnetUnsigned getRemoteVtSessionId() {
      return (BBacnetUnsigned)this.get(remoteVtSessionId);
   }

   public void setRemoteVtSessionId(BBacnetUnsigned v) {
      this.set(remoteVtSessionId, v, null);
   }

   public BBacnetAddress getRemoteVtAddress() {
      return (BBacnetAddress)this.get(remoteVtAddress);
   }

   public void setRemoteVtAddress(BBacnetAddress v) {
      this.set(remoteVtAddress, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsigned(this.getLocalVtSessionId());
      out.writeUnsigned(this.getRemoteVtSessionId());
      this.getRemoteVtAddress().writeAsn(out);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetUnsigned localVtSessionId = in.readUnsigned();
      BBacnetUnsigned remoteVtSessionId = in.readUnsigned();
      this.getRemoteVtAddress().readAsn(in);
      this.set(BBacnetVtSession.localVtSessionId, localVtSessionId, noWrite);
      this.set(BBacnetVtSession.remoteVtSessionId, remoteVtSessionId, noWrite);
   }

   public String toString(Context context) {
      return "BACnetVTSession: localVtSessionId = "
         + this.getLocalVtSessionId().toString(context)
         + "; remoteVtSessionId = "
         + this.getRemoteVtSessionId().toString(context)
         + "; remoteVtAddress = ["
         + this.getRemoteVtAddress().toString(context)
         + ']';
   }
}
