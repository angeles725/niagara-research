package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import javax.baja.alarm.ext.BFaultAlgorithm;
import javax.baja.alarm.ext.fault.BOutOfRangeFaultAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BFacets;
import javax.baja.sys.BNumber;
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
   defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.POSITIVE_INTEGER_VALUE)",
   flags = 64,
   override = true
)
public class BBacnetPositiveIntegerValuePrioritizedDescriptor extends BBacnetAnalogWritableDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(48), null);
   public static final Type TYPE = Sys.loadType(BBacnetPositiveIntegerValuePrioritizedDescriptor.class);

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
   public int asnType() {
      return 2;
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
   public void appendToAsn(AsnOutputStream out, double value) {
      out.writeUnsignedInteger((long)value);
   }

   @Override
   public double readFromAsn(AsnInputStream in) throws AsnException {
      return in.readUnsignedInteger();
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.remove(BBacnetPropertyIdentifier.outOfService);
      v.remove(BBacnetPropertyIdentifier.priorityArray);
      v.remove(BBacnetPropertyIdentifier.relinquishDefault);
      v.remove(BBacnetPropertyIdentifier.eventState);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      v.add(BBacnetPropertyIdentifier.outOfService);
      v.add(BBacnetPropertyIdentifier.priorityArray);
      v.add(BBacnetPropertyIdentifier.relinquishDefault);
      v.add(BBacnetPropertyIdentifier.eventState);
      if (this.getFaultAlgorithm() instanceof BOutOfRangeFaultAlgorithm) {
         v.add(BBacnetPropertyIdentifier.faultHighLimit);
         v.add(BBacnetPropertyIdentifier.faultLowLimit);
      }
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(48) : super.getSlotFacets(s);
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
