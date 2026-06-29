package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.offnormal.BBooleanChangeOfStateAlgorithm;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BBooleanPoint;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatusBoolean;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"control:BooleanPoint"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.BINARY_INPUT)",
      flags = 64,
      override = true
   ), @NiagaraProperty(
      name = "deviceType",
      type = "String",
      defaultValue = ""
   )})
public class BBacnetBinaryInputDescriptor extends BBacnetBinaryPointDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(3), null);
   public static final Property deviceType = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetBinaryInputDescriptor.class);

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

   @Deprecated
   public BStatusBoolean getBacnetValue() {
      throw new BajaRuntimeException("Method getBacnetValue() is deprecated!");
   }

   @Deprecated
   public void setBacnetValue(BStatusBoolean v) {
      throw new BajaRuntimeException("Method setBacnetValue() is deprecated!");
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(3) : super.getSlotFacets(s);
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.changeOfState;
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BAlarmSourceExt ? ((BAlarmSourceExt)ext).getOffnormalAlgorithm() instanceof BBooleanChangeOfStateAlgorithm : false;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BBooleanPoint pt = (BBooleanPoint)this.getPoint();
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
      BBooleanPoint pt = (BBooleanPoint)this.getPoint();
      if (pt == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 84:
                  return this.writePolarityProperty(pt, val);
               case 85:
                  BOutOfServiceExt outOfServiceExt = this.getOosExt();
                  if (outOfServiceExt.getOutOfService()) {
                     synchronized (asnIn) {
                        asnIn.setBuffer(val);
                        int tag = asnIn.peekTag();
                        if (tag != 9) {
                           throw new AsnException("Invalid tag: " + tag);
                        }

                        int pv = asnIn.readEnumerated();
                        if (pv == 0) {
                           outOfServiceExt.set(BOutOfServiceExt.presentValue, BBoolean.FALSE, BLocalBacnetDevice.getBacnetContext());
                        } else {
                           if (pv != 1) {
                              return new NErrorType(2, 37);
                           }

                           outOfServiceExt.set(BOutOfServiceExt.presentValue, BBoolean.TRUE, BLocalBacnetDevice.getBacnetContext());
                        }
                     }

                     return null;
                  } else {
                     return new NErrorType(2, 40);
                  }
               default:
                  return super.writeProperty(pId, ndx, val, pri);
            }
         } catch (AsnException var12) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
            return new NErrorType(2, 9);
         } catch (PermissionException var13) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var13);
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
         v.add(BBacnetPropertyIdentifier.alarmValue);
      }

      v.add(BBacnetPropertyIdentifier.deviceType);
      v.add(BBacnetPropertyIdentifier.interfaceValue);
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         BBooleanChangeOfStateAlgorithm alg = (BBooleanChangeOfStateAlgorithm)almExt.getOffnormalAlgorithm();
         if (pId == 6) {
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(alg.getAlarmValue()));
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
               try {
                  BBooleanChangeOfStateAlgorithm alg = (BBooleanChangeOfStateAlgorithm)almExt.getOffnormalAlgorithm();
                  if (pId == 6) {
                     alg.setBoolean(BBooleanChangeOfStateAlgorithm.alarmValue, AsnUtil.fromOnlyBinaryPv(val), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  }

                  if (pId == 35) {
                     almExt.set(
                        BAlarmSourceExt.alarmEnable,
                        BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                        BLocalBacnetDevice.getBacnetContext()
                     );
                     return null;
                  }
               } catch (OutOfRangeException var7) {
                  log.warning("OutOfRangeException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
                  return new NErrorType(2, 37);
               } catch (AsnException var8) {
                  log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var8);
                  return new NErrorType(2, 9);
               } catch (PermissionException var9) {
                  log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var9);
                  return new NErrorType(2, 40);
               }
            }

            return super.writeOptionalProperty(pId, ndx, val, pri);
      }
   }
}
