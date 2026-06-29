package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import javax.baja.alarm.ext.BFaultAlgorithm;
import javax.baja.alarm.ext.fault.BOutOfRangeFaultAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:NumericWritable"}
   )}
)
@NiagaraProperty(
   name = "objectId",
   type = "BBacnetObjectIdentifier",
   defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ANALOG_VALUE)",
   flags = 64,
   override = true
)
public class BBacnetAnalogValuePrioritizedDescriptor extends BBacnetAnalogWritableDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(2), null);
   public static final Type TYPE = Sys.loadType(BBacnetAnalogValuePrioritizedDescriptor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected boolean commandabilityRequired() {
      return false;
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(2) : super.getSlotFacets(s);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      if (this.getFaultAlgorithm() instanceof BOutOfRangeFaultAlgorithm) {
         v.add(BBacnetPropertyIdentifier.faultHighLimit);
         v.add(BBacnetPropertyIdentifier.faultLowLimit);
      }
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      switch (pId) {
         case 388:
            BFaultAlgorithm faultAlgorithmx = this.getFaultAlgorithm();
            if (faultAlgorithmx instanceof BOutOfRangeFaultAlgorithm) {
               return new NReadPropertyResult(pId, ndx, this.convertToAsn(((BOutOfRangeFaultAlgorithm)faultAlgorithmx).getHighLimit()));
            }
            break;
         case 389:
            BFaultAlgorithm faultAlgorithm = this.getFaultAlgorithm();
            if (faultAlgorithm instanceof BOutOfRangeFaultAlgorithm) {
               return new NReadPropertyResult(pId, ndx, this.convertToAsn(((BOutOfRangeFaultAlgorithm)faultAlgorithm).getLowLimit()));
            }
      }

      return super.readOptionalProperty(pId, ndx);
   }

   @Override
   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      switch (pId) {
         case 388:
            BFaultAlgorithm faultAlgorithmx = this.getFaultAlgorithm();
            if (faultAlgorithmx instanceof BOutOfRangeFaultAlgorithm) {
               faultAlgorithmx.setDouble(BOutOfRangeFaultAlgorithm.highLimit, this.convertFromAsn(val), BLocalBacnetDevice.getBacnetContext());
               return null;
            }
            break;
         case 389:
            BFaultAlgorithm faultAlgorithm = this.getFaultAlgorithm();
            if (faultAlgorithm instanceof BOutOfRangeFaultAlgorithm) {
               faultAlgorithm.setDouble(BOutOfRangeFaultAlgorithm.lowLimit, this.convertFromAsn(val), BLocalBacnetDevice.getBacnetContext());
               return null;
            }
      }

      return super.writeOptionalProperty(pId, ndx, val, pri);
   }
}
