package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.BFaultAlgorithm;
import javax.baja.alarm.ext.fault.BOutOfRangeFaultAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BControlPoint;
import javax.baja.control.BNumericPoint;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:NumericPoint"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.ANALOG_INPUT)",
      flags = 64,
      override = true
   ), @NiagaraProperty(
      name = "deviceType",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "updateInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.make(UPDATE_INTERVAL)",
      flags = 1
   )})
public class BBacnetAnalogInputDescriptor extends BBacnetAnalogPointDescriptor {
   public static final long UPDATE_INTERVAL = 10000L;
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(0), null);
   public static final Property deviceType = newProperty(0, "", null);
   public static final Property updateInterval = newProperty(1, BRelTime.make(10000L), null);
   public static final Type TYPE = Sys.loadType(BBacnetAnalogInputDescriptor.class);

   public String getDeviceType() {
      return this.getString(deviceType);
   }

   public void setDeviceType(String v) {
      this.setString(deviceType, v, null);
   }

   public BRelTime getUpdateInterval() {
      return (BRelTime)this.get(updateInterval);
   }

   public void setUpdateInterval(BRelTime v) {
      this.set(updateInterval, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Deprecated
   public BStatusNumeric getBacnetValue() {
      throw new BajaRuntimeException("Method getBacnetValue() is deprecated!");
   }

   @Deprecated
   public void setBacnetValue(BStatusNumeric v) {
      throw new BajaRuntimeException("Method setBacnetValue() is deprecated!");
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(0) : super.getSlotFacets(s);
   }

   @Override
   protected final boolean isPointTypeLegal(BControlPoint pt) {
      return pt instanceof BNumericPoint;
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BNumericPoint pt = (BNumericPoint)this.getPoint();
      if (pt == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 85:
                  BOutOfServiceExt outOfServiceExt = this.getOosExt();
                  if (outOfServiceExt.getOutOfService()) {
                     outOfServiceExt.set(BOutOfServiceExt.presentValue, BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  }

                  return new NErrorType(2, 40);
            }
         } catch (AsnException var7) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
            return new NErrorType(2, 9);
         } catch (PermissionException var8) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var8);
            return new NErrorType(2, 40);
         }

         return super.writeProperty(pId, ndx, val, pri);
      }
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      v.add(BBacnetPropertyIdentifier.deviceType);
      v.add(BBacnetPropertyIdentifier.updateInterval);
      v.add(BBacnetPropertyIdentifier.interfaceValue);
      if (this.getFaultAlgorithm() instanceof BOutOfRangeFaultAlgorithm) {
         v.add(BBacnetPropertyIdentifier.faultHighLimit);
         v.add(BBacnetPropertyIdentifier.faultLowLimit);
      }
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      switch (pId) {
         case 31:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDeviceType()));
         case 118:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.getUpdateInterval().getMillis() / 10L));
         case 387:
            return this.readInterfaceValue();
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
         case 31:
         case 118:
         case 387:
            return new NErrorType(2, 40);
         case 35:
            BAlarmSourceExt almExt = this.getAlarmExt();
            if (almExt != null) {
               almExt.set(
                  BAlarmSourceExt.alarmEnable,
                  BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                  BLocalBacnetDevice.getBacnetContext()
               );
               return null;
            }
            break;
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
