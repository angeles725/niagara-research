package javax.baja.bacnet.datatypes;

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
      name = "multiplier",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)"
   ), @NiagaraProperty(
      name = "moduloDivide",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)"
   )})
public final class BBacnetPrescale extends BStruct implements BIBacnetDataType {
   public static final Property multiplier = newProperty(0, BBacnetUnsigned.make(0L), null);
   public static final Property moduloDivide = newProperty(0, BBacnetUnsigned.make(0L), null);
   public static final Type TYPE = Sys.loadType(BBacnetPrescale.class);
   public static final int MULTIPLIER_TAG = 0;
   public static final int MODULO_DIVIDE_TAG = 1;

   public BBacnetUnsigned getMultiplier() {
      return (BBacnetUnsigned)this.get(multiplier);
   }

   public void setMultiplier(BBacnetUnsigned v) {
      this.set(multiplier, v, null);
   }

   public BBacnetUnsigned getModuloDivide() {
      return (BBacnetUnsigned)this.get(moduloDivide);
   }

   public void setModuloDivide(BBacnetUnsigned v) {
      this.set(moduloDivide, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetPrescale() {
   }

   public BBacnetPrescale(long mult, long mod) {
      this.setMultiplier(BBacnetUnsigned.make(mult));
      this.setModuloDivide(BBacnetUnsigned.make(mod));
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsigned(0, this.getMultiplier());
      out.writeUnsigned(1, this.getModuloDivide());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetUnsigned multiplier = in.readUnsigned(0);
      BBacnetUnsigned moduloDivide = in.readUnsigned(1);
      this.set(BBacnetPrescale.multiplier, multiplier, noWrite);
      this.set(BBacnetPrescale.moduloDivide, moduloDivide, noWrite);
   }

   public String toString(Context cx) {
      return "Prescale:mult=" + this.getMultiplier() + " mod=" + this.getModuloDivide();
   }
}
