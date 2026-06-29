package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import java.util.logging.Level;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.fault.BEnumFaultAlgorithm;
import javax.baja.alarm.ext.offnormal.BEnumChangeOfStateAlgorithm;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.EnumRangeWrapper;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
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
@NiagaraProperty(
   name = "objectId",
   type = "BBacnetObjectIdentifier",
   defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.MULTI_STATE_VALUE)",
   flags = 64,
   override = true
)
public class BBacnetMultiStateValuePrioritizedDescriptor extends BBacnetMultiStateWritableDescriptor {
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(19), null);
   public static final Type TYPE = Sys.loadType(BBacnetMultiStateValuePrioritizedDescriptor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(19) : super.getSlotFacets(s);
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.changeOfState;
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BAlarmSourceExt ? ((BAlarmSourceExt)ext).getOffnormalAlgorithm() instanceof BEnumChangeOfStateAlgorithm : false;
   }

   @Override
   protected void addOptionalProps(Vector v) {
      super.addOptionalProps(v);
      v.add(BBacnetPropertyIdentifier.priorityArray);
      v.add(BBacnetPropertyIdentifier.relinquishDefault);
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         v.add(BBacnetPropertyIdentifier.alarmValues);
         if (BBacnetNetwork.bacnet().setAndGetShouldSupportFaults()) {
            v.add(BBacnetPropertyIdentifier.faultValues);
         }
      }
   }

   @Override
   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         try {
            if (pId == 7) {
               synchronized (asnOut) {
                  asnOut.reset();
                  BEnumChangeOfStateAlgorithm alg = (BEnumChangeOfStateAlgorithm)almExt.getOffnormalAlgorithm();
                  int[] vals = alg.getAlarmValues().getOrdinals();

                  for (int i = 0; i < vals.length; i++) {
                     asnOut.writeUnsignedInteger(vals[i]);
                  }

                  return new NReadPropertyResult(pId, ndx, asnOut.toByteArray());
               }
            }

            if (pId == 39) {
               if (!BBacnetNetwork.bacnet().setAndGetShouldSupportFaults()) {
                  return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
               }

               synchronized (asnOut) {
                  asnOut.reset();
                  BEnumFaultAlgorithm alg = (BEnumFaultAlgorithm)almExt.getFaultAlgorithm();
                  int[] validVals = alg.getValidValues().getOrdinals();
                  BEnumRange r = (BEnumRange)this.getPoint().getFacets().getFacet("range");
                  int[] rangeVals = r.getOrdinals();

                  for (int i = 0; i < rangeVals.length; i++) {
                     boolean valid = false;

                     for (int j = 0; j < validVals.length; j++) {
                        if (rangeVals[i] == validVals[j]) {
                           valid = true;
                           break;
                        }
                     }

                     if (!valid) {
                        asnOut.writeUnsignedInteger(rangeVals[i]);
                     }
                  }

                  return new NReadPropertyResult(pId, ndx, asnOut.toByteArray());
               }
            }
         } catch (Exception var15) {
            logger.log(Level.SEVERE, "AsnException occurred in readOptionalProperty", (Throwable)var15);
            return new NReadPropertyResult(pId, ndx, new NErrorType(0, 25));
         }
      }

      return super.readOptionalProperty(pId, ndx);
   }

   @Override
   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         try {
            if (pId == 7) {
               BEnumChangeOfStateAlgorithm alg = (BEnumChangeOfStateAlgorithm)almExt.getOffnormalAlgorithm();
               BEnumRange alarmValueRange = (BEnumRange)this.getPoint().getFacets().getFacet("range");
               EnumRangeWrapper enumRangeWrapper = getWritableEnumRange(val, alarmValueRange, false);
               if (enumRangeWrapper.getErrorType() == null) {
                  alg.set(BEnumChangeOfStateAlgorithm.alarmValues, enumRangeWrapper.getEnumRange(), BLocalBacnetDevice.getBacnetContext());
                  return null;
               }

               return enumRangeWrapper.getErrorType();
            }

            if (pId == 35) {
               almExt.set(
                  BAlarmSourceExt.alarmEnable,
                  BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                  BLocalBacnetDevice.getBacnetContext()
               );
               return null;
            }

            if (pId == 39) {
               if (!BBacnetNetwork.bacnet().setAndGetShouldSupportFaults()) {
                  return new NErrorType(2, 32);
               }

               BEnumFaultAlgorithm alg = (BEnumFaultAlgorithm)almExt.getFaultAlgorithm();
               BEnumRange faultValueRange = (BEnumRange)this.getPoint().getFacets().getFacet("range");
               EnumRangeWrapper enumRangeWrapper = getWritableEnumRange(val, faultValueRange, true);
               if (enumRangeWrapper.getErrorType() == null) {
                  alg.set(BEnumFaultAlgorithm.validValues, enumRangeWrapper.getEnumRange(), BLocalBacnetDevice.getBacnetContext());
                  return null;
               }

               return enumRangeWrapper.getErrorType();
            }
         } catch (AsnException var9) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var9);
            return new NErrorType(2, 9);
         } catch (PermissionException var10) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var10);
            return new NErrorType(2, 40);
         }
      }

      return super.writeOptionalProperty(pId, ndx, val, pri);
   }
}
