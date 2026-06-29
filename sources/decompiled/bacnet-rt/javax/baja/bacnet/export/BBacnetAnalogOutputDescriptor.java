package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
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
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ANALOG_OUTPUT)",
      flags = 64,
      override = true
   ), @NiagaraProperty(
      name = "deviceType",
      type = "String",
      defaultValue = ""
   )})
public class BBacnetAnalogOutputDescriptor extends BBacnetAnalogWritableDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(1), null);
   public static final Property deviceType = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetAnalogOutputDescriptor.class);

   public String getDeviceType() {
      return this.getString(deviceType);
   }

   public void setDeviceType(String v) {
      this.setString(deviceType, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(1) : super.getSlotFacets(s);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      v.add(BBacnetPropertyIdentifier.deviceType);
      v.add(BBacnetPropertyIdentifier.interfaceValue);
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      switch (pId) {
         case 31:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDeviceType()));
         case 387:
            return this.readInterfaceValue();
         default:
            return super.readOptionalProperty(pId, ndx);
      }
   }

   @Override
   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      switch (pId) {
         case 31:
         case 387:
            return new NErrorType(2, 40);
         default:
            return super.writeOptionalProperty(pId, ndx, val, pri);
      }
   }
}
