package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.offnormal.BBooleanCommandFailureAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BBooleanWritable;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:BooleanWritable"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.BINARY_OUTPUT)",
      flags = 64,
      override = true
   ), @NiagaraProperty(
      name = "deviceType",
      type = "String",
      defaultValue = ""
   )})
public class BBacnetBinaryOutputDescriptor extends BBacnetBinaryWritableDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(4), null);
   public static final Property deviceType = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetBinaryOutputDescriptor.class);

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
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(4) : super.getSlotFacets(s);
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.commandFailure;
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BAlarmSourceExt ? ((BAlarmSourceExt)ext).getOffnormalAlgorithm() instanceof BBooleanCommandFailureAlgorithm : false;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BBooleanWritable pt = (BBooleanWritable)this.getPoint();
      if (pt == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 84:
               return this.readPolarityProperty(pt);
            case 85:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(pt.getOut().getValue()));
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BBooleanWritable pt = (BBooleanWritable)this.getPoint();
      if (pt == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            return pId == 84 ? this.writePolarityProperty(pt, val) : super.writeProperty(pId, ndx, val, pri);
         } catch (AsnException var7) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
            return new NErrorType(2, 9);
         } catch (PermissionException var8) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var8);
            return new NErrorType(2, 40);
         }
      }
   }

   @Override
   protected void addRequiredProps(Vector v) {
      super.addRequiredProps(v);
      v.add(BBacnetPropertyIdentifier.polarity);
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
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         BBooleanCommandFailureAlgorithm alg = (BBooleanCommandFailureAlgorithm)almExt.getOffnormalAlgorithm();
         if (pId == 40) {
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(alg.getFeedbackValue().getValue()));
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
               switch (pId) {
                  case 35:
                     almExt.set(
                        BAlarmSourceExt.alarmEnable,
                        BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                        BLocalBacnetDevice.getBacnetContext()
                     );
                     return null;
                  case 40:
                     return new NErrorType(2, 40);
               }
            }

            return super.writeOptionalProperty(pId, ndx, val, pri);
      }
   }
}
