package javax.baja.bacnet.datatypes.security;

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
      name = "algorithm",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "keyId",
      type = "int",
      defaultValue = "-1"
   )})
public final class BBacnetKeyIdentifier extends BStruct implements BIBacnetDataType {
   public static final Property algorithm = newProperty(0, 0, null);
   public static final Property keyId = newProperty(0, -1, null);
   public static final Type TYPE = Sys.loadType(BBacnetKeyIdentifier.class);
   public static final int ALGORITHM_TAG = 0;
   public static final int KEY_ID_TAG = 1;

   public int getAlgorithm() {
      return this.getInt(algorithm);
   }

   public void setAlgorithm(int v) {
      this.setInt(algorithm, v, null);
   }

   public int getKeyId() {
      return this.getInt(keyId);
   }

   public void setKeyId(int v) {
      this.setInt(keyId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetKeyIdentifier() {
   }

   public BBacnetKeyIdentifier(int algorithm, int keyId) {
      this.setAlgorithm(algorithm);
      this.setKeyId(keyId);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BBacnetKeyIdentifier:");
      sb.append(this.getAlgorithm()).append(":").append(this.getKeyId());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsignedInteger(0, this.getAlgorithm());
      out.writeUnsignedInteger(1, this.getKeyId());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int alg = in.readUnsignedInt(0);
      this.setInt(algorithm, alg, noWrite);
      int key = in.readUnsignedInt(0);
      this.setInt(keyId, key, noWrite);
   }
}
