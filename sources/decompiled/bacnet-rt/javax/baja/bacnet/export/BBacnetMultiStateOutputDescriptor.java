package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import java.util.Vector;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.offnormal.BEnumCommandFailureAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:EnumWritable"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "deviceType",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.MULTI_STATE_OUTPUT)",
      flags = 64,
      override = true
   )})
public class BBacnetMultiStateOutputDescriptor extends BBacnetMultiStateWritableDescriptor {
   public static final Property deviceType = newProperty(0, "", null);
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(14), null);
   public static final Type TYPE = Sys.loadType(BBacnetMultiStateOutputDescriptor.class);

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
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(14) : super.getSlotFacets(s);
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.commandFailure;
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BAlarmSourceExt ? ((BAlarmSourceExt)ext).getOffnormalAlgorithm() instanceof BEnumCommandFailureAlgorithm : false;
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.add(BBacnetPropertyIdentifier.priorityArray);
      v.add(BBacnetPropertyIdentifier.relinquishDefault);
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         v.add(BBacnetPropertyIdentifier.feedbackValue);
      }

      v.add(BBacnetPropertyIdentifier.deviceType);
      v.add(BBacnetPropertyIdentifier.interfaceValue);
   }

   @Override
   public RangeData readRange(RangeReference rangeReference) throws RejectException {
      int propertyId = rangeReference.getPropertyId();
      return !this.hasProperty(propertyId)
         ? new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   public ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      this.getPoint();
      int propertyId = propertyValue.getPropertyId();
      return !this.hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   public ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      this.getPoint();
      int propertyId = propertyValue.getPropertyId();
      return !this.hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         BEnumCommandFailureAlgorithm alg = (BEnumCommandFailureAlgorithm)almExt.getOffnormalAlgorithm();
         if (pId == 40) {
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(alg.getFeedbackValue().getValue().getOrdinal()));
         }
      }

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
            BAlarmSourceExt almExt = this.getAlarmExt();
            if (almExt != null) {
               if (pId == 40) {
                  return new NErrorType(2, 40);
               }

               if (pId == 35) {
                  almExt.set(
                     BAlarmSourceExt.alarmEnable,
                     BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                     BLocalBacnetDevice.getBacnetContext()
                  );
                  return null;
               }
            }

            return super.writeOptionalProperty(pId, ndx, val, pri);
      }
   }
}
