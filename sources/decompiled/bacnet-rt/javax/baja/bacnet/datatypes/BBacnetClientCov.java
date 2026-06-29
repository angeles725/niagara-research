package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "increment",
   type = "BStatusNumeric",
   defaultValue = "new BStatusNumeric()"
)
public final class BBacnetClientCov extends BStruct implements BIBacnetDataType {
   public static final Property increment = newProperty(0, new BStatusNumeric(), null);
   public static final Type TYPE = Sys.loadType(BBacnetClientCov.class);

   public BStatusNumeric getIncrement() {
      return (BStatusNumeric)this.get(increment);
   }

   public void setIncrement(BStatusNumeric v) {
      this.set(increment, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetClientCov() {
      this.getIncrement().setStatusNull(true);
   }

   public BBacnetClientCov(double realIncrement) {
      this.setRealIncrement(realIncrement);
   }

   public void setRealIncrement(double v) {
      this.getIncrement().setValue(v);
      this.getIncrement().setStatusNull(false);
   }

   public void setDefaultIncrement() {
      this.getIncrement().setStatusNull(true);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      if (this.getIncrement().getStatus().isNull()) {
         out.writeNull();
      } else {
         out.writeReal(this.getIncrement().getValue());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekApplicationTag();
      switch (tag) {
         case 0:
            this.setDefaultIncrement();
            break;
         case 4:
            this.setRealIncrement(in.readReal());
            break;
         default:
            throw new AsnException("Invalid tag: " + tag);
      }
   }

   public String toString(Context context) {
      return this.getIncrement().toString(context);
   }
}
