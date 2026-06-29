package javax.baja.bacnet.datatypes.security;

import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.enums.security.BBacnetSecurityLevel;
import javax.baja.bacnet.enums.security.BBacnetSecurityPolicy;
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
      name = "portId",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "securityLevel",
      type = "BBacnetSecurityPolicy",
      defaultValue = "BBacnetSecurityPolicy.plainNonTrusted"
   )})
public final class BBacnetNetworkSecurityPolicy extends BStruct implements BIBacnetDataType {
   public static final Property portId = newProperty(0, 0, null);
   public static final Property securityLevel = newProperty(0, BBacnetSecurityPolicy.plainNonTrusted, null);
   public static final Type TYPE = Sys.loadType(BBacnetNetworkSecurityPolicy.class);
   public static final int PORT_ID_TAG = 0;
   public static final int SECURITY_LEVEL_TAG = 1;

   public int getPortId() {
      return this.getInt(portId);
   }

   public void setPortId(int v) {
      this.setInt(portId, v, null);
   }

   public BBacnetSecurityPolicy getSecurityLevel() {
      return (BBacnetSecurityPolicy)this.get(securityLevel);
   }

   public void setSecurityLevel(BBacnetSecurityPolicy v) {
      this.set(securityLevel, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetNetworkSecurityPolicy() {
   }

   public BBacnetNetworkSecurityPolicy(int portId, BBacnetSecurityPolicy securityLevel) {
      this.setPortId(portId);
      this.setSecurityLevel(securityLevel);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BBacnetNetworkSecurityPolicy:");
      sb.append(this.getPortId()).append(":").append(this.getSecurityLevel());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsignedInteger(0, this.getPortId());
      out.writeEnumerated(1, this.getSecurityLevel());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int port = in.readUnsignedInt(0);
      this.setInt(portId, port, noWrite);
      int ordinal = in.readEnumerated(1);
      this.set(securityLevel, BBacnetSecurityLevel.make(ordinal), noWrite);
   }
}
