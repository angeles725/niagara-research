package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import java.util.Vector;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BNumber;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:NumericPoint"}
   )}
)
@NiagaraProperty(
   name = "objectId",
   type = "BBacnetObjectIdentifier",
   defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.POSITIVE_INTEGER_VALUE)",
   flags = 64,
   override = true
)
public class BBacnetPositiveIntegerValueDescriptor extends BBacnetAnalogValueDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(48), null);
   public static final Type TYPE = Sys.loadType(BBacnetPositiveIntegerValueDescriptor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected final void validate() {
      super.validate();
      BNumber min = (BNumber)this.getPoint().getFacets().getFacet("min");
      if (min != null && this.setFaultOnOutOfRangeValue(min)) {
         this.setFaultCause(lex.getText("export.configurationFault.postiveIntergerValue.minValue", new Object[]{0L, 4294967295L}));
      } else {
         BNumber max = (BNumber)this.getPoint().getFacets().getFacet("max");
         if (max != null && this.setFaultOnOutOfRangeValue(max)) {
            this.setFaultCause(lex.getText("export.configurationFault.postiveIntergerValue.maxValue", new Object[]{0L, 4294967295L}));
         }
      }
   }

   private boolean setFaultOnOutOfRangeValue(BNumber min) {
      if (min.getLong() >= 0L && min.getLong() <= 4294967295L) {
         return false;
      } else {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
         return true;
      }
   }

   @Override
   public byte[] convertToAsn(double value) {
      return AsnUtil.toAsnUnsigned((long)value);
   }

   @Override
   public double convertFromAsn(byte[] value) throws AsnException {
      return AsnUtil.fromAsnUnsignedInteger(value);
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.remove(BBacnetPropertyIdentifier.outOfService);
      v.remove(BBacnetPropertyIdentifier.eventState);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      v.add(BBacnetPropertyIdentifier.outOfService);
      v.add(BBacnetPropertyIdentifier.eventState);
   }
}
