package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import java.util.Vector;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
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
   defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.LARGE_ANALOG_VALUE)",
   flags = 64,
   override = true
)
public class BBacnetLargeAnalogValueDescriptor extends BBacnetAnalogValueDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(46), null);
   public static final Type TYPE = Sys.loadType(BBacnetLargeAnalogValueDescriptor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public byte[] convertToAsn(double value) {
      return AsnUtil.toAsnDouble(value);
   }

   @Override
   public double convertFromAsn(byte[] value) throws AsnException {
      return AsnUtil.fromAsnDouble(value);
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
