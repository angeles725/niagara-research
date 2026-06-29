package javax.baja.bacnet.datatypes.access;

import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.enums.access.BBacnetAccessAuthenticationFactorDisable;
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
      name = "disable",
      type = "BBacnetAccessAuthenticationFactorDisable",
      defaultValue = "BBacnetAccessAuthenticationFactorDisable.DEFAULT"
   ), @NiagaraProperty(
      name = "authenticationFactor",
      type = "BBacnetAuthenticationFactor",
      defaultValue = "new BBacnetAuthenticationFactor()"
   )})
public final class BBacnetCredentialAuthenticationFactor extends BStruct implements BIBacnetDataType {
   public static final Property disable = newProperty(0, BBacnetAccessAuthenticationFactorDisable.DEFAULT, null);
   public static final Property authenticationFactor = newProperty(0, new BBacnetAuthenticationFactor(), null);
   public static final Type TYPE = Sys.loadType(BBacnetCredentialAuthenticationFactor.class);
   public static final int DISABLE_TAG = 0;
   public static final int AUTH_FACTOR_TAG = 1;

   public BBacnetAccessAuthenticationFactorDisable getDisable() {
      return (BBacnetAccessAuthenticationFactorDisable)this.get(disable);
   }

   public void setDisable(BBacnetAccessAuthenticationFactorDisable v) {
      this.set(disable, v, null);
   }

   public BBacnetAuthenticationFactor getAuthenticationFactor() {
      return (BBacnetAuthenticationFactor)this.get(authenticationFactor);
   }

   public void setAuthenticationFactor(BBacnetAuthenticationFactor v) {
      this.set(authenticationFactor, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetCredentialAuthenticationFactor() {
   }

   public BBacnetCredentialAuthenticationFactor(BBacnetAccessAuthenticationFactorDisable disable, BBacnetAuthenticationFactor authFactor) {
      this.setDisable(disable);
      this.setAuthenticationFactor(authFactor);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("BacnetCredAuthFactor:");
      sb.append(this.getDisable()).append(":").append(this.getAuthenticationFactor());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.getDisable().getOrdinal());
      out.writeOpeningTag(1);
      this.getAuthenticationFactor().writeAsn(out);
      out.writeClosingTag(1);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetAccessAuthenticationFactorDisable disable = BBacnetAccessAuthenticationFactorDisable.make(in.readEnumerated(0));
      BBacnetAuthenticationFactor authenticationFactor = new BBacnetAuthenticationFactor();
      in.skipOpeningTag(1);
      authenticationFactor.readAsn(in);
      in.skipClosingTag(1);
      this.set(BBacnetCredentialAuthenticationFactor.disable, disable, noWrite);
      this.set(BBacnetCredentialAuthenticationFactor.authenticationFactor, authenticationFactor, noWrite);
   }
}
