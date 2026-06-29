package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.stack.DeviceRegistry;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BControlSchedule;
import javax.baja.schedule.BNumericSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"schedule:NumericSchedule"}
   )}
)
public class BBacnetNumericScheduleDescriptor extends BBacnetScheduleDescriptor {
   public static final Type TYPE = Sys.loadType(BBacnetNumericScheduleDescriptor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void doWritePresentValue() {
      BNumericSchedule sched = (BNumericSchedule)this.getSchedule();
      if (sched != null && sched.getEffective().isEffective(BAbsTime.now())) {
         BStatusNumeric out = sched.getOut();
         byte[] writeVal = null;
         if (out.getStatus().isNull()) {
            writeVal = AsnUtil.toAsnNull();
         } else {
            writeVal = AsnUtil.toAsnReal(out.getValue());
         }

         SlotCursor<Property> c = this.getListOfObjectPropertyReferences().getProperties();

         while (c.next(BBacnetDeviceObjectPropertyReference.class)) {
            BBacnetDeviceObjectPropertyReference ref = (BBacnetDeviceObjectPropertyReference)c.get();
            if (ref.isDeviceIdUsed() && !ref.getDeviceId().equals(BBacnetNetwork.localDevice().getObjectId())) {
               BBacnetAddress addr = DeviceRegistry.getDeviceAddress(ref.getDeviceId());
               if (addr == null) {
                  this.findOrAddRemoteDeviceAndPoint(ref);
                  addr = DeviceRegistry.getDeviceAddress(ref.getDeviceId());
               }

               if (addr != null) {
                  try {
                     client().writeProperty(addr, ref.getObjectId(), ref.getPropertyId(), ref.getPropertyArrayIndex(), writeVal, this.getPriorityForWriting());
                  } catch (BacnetException var10) {
                     log.warning("BacnetException writing schedule output to " + ref + ": " + var10);
                  }
               } else {
                  log.warning("Unable to write Schedule output " + out + " to " + ref + ": unable to resolve device address");
               }
            } else {
               BIBacnetExportObject o = BBacnetNetwork.localDevice().lookupBacnetObject(ref.getObjectId());

               try {
                  ErrorType err = o.writeProperty(
                     new NBacnetPropertyValue(ref.getPropertyId(), ref.getPropertyArrayIndex(), writeVal, this.getPriorityForWriting())
                  );
                  if (err != null) {
                     throw new ErrorException(err);
                  }
               } catch (Exception var9) {
                  log.warning("Unable to write schedule output " + out + " from " + this + " to local object " + ref + ": " + var9);
               }
            }
         }

         this.setLastEffectiveValue((BStatusValue)out.newCopy());
      }
   }

   @Override
   final boolean isScheduleTypeLegal(BWeeklySchedule sched) {
      return sched instanceof BNumericSchedule;
   }

   @Override
   int getAsnType() {
      return 4;
   }

   @Override
   final Property getScheduleOutputProperty() {
      return BNumericSchedule.out;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BNumericSchedule sched = (BNumericSchedule)this.getSchedule();
      if (sched == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 85:
               BAbsTime currentTime = BAbsTime.now();
               BStatusNumeric out;
               if (!sched.isEffective(currentTime) && this.getLastEffectiveValue() != null) {
                  out = (BStatusNumeric)this.getLastEffectiveValue();
               } else {
                  out = sched.getOut();
               }

               return out.getStatus().isNull()
                  ? new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull())
                  : new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(out.getValue()));
            case 174:
               BStatusNumeric sf = (BStatusNumeric)sched.getDefaultOutput();
               if (sf.getStatus().isNull()) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull());
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(sf.getValue()));
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BNumericSchedule sched = (BNumericSchedule)this.getSchedule();
      if (sched == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            synchronized (asnIn) {
               asnIn.setBuffer(val);
               switch (pId) {
                  case 85:
                     if (((BStatusValue)sched.get("out")).getStatus().isDisabled()) {
                        switch (asnIn.peekApplicationTag()) {
                           case 0:
                              sched.getIn().set(BStatusValue.status, BStatus.make(sched.getIn().getStatus(), 64, false), BLocalBacnetDevice.getBacnetContext());
                              sched.getOut()
                                 .set(BStatusValue.status, BStatus.make(sched.getOut().getStatus(), 64, true), BLocalBacnetDevice.getBacnetContext());
                              return null;
                           case 4:
                              BStatusNumeric inval = (BStatusNumeric)sched.getIn().newCopy();
                              inval.setValue(asnIn.readReal());
                              inval.setStatusNull(false);
                              sched.set(BNumericSchedule.in, inval, BLocalBacnetDevice.getBacnetContext());
                              return null;
                           default:
                              return new NErrorType(2, 9);
                        }
                     }

                     return new NErrorType(2, 40);
                  default:
                     return super.writeProperty(pId, ndx, val, pri);
               }
            }
         } catch (AsnException var10) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var10);
            return new NErrorType(2, 9);
         } catch (PermissionException var11) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var11);
            return new NErrorType(2, 40);
         } catch (Exception var12) {
            log.warning("Exception writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
            return new NErrorType(2, 0);
         }
      }
   }

   @Override
   BStatusValue getEffectiveValueFrom(BStatusValue statusValue) {
      BStatusNumeric ret = new BStatusNumeric(0.0, BStatus.nullStatus);
      if (statusValue instanceof BStatusEnum) {
         ret.setValue(((BStatusEnum)statusValue).getValue().getOrdinal());
      } else if (statusValue instanceof BStatusBoolean) {
         ret.setValue(((BStatusBoolean)statusValue).getValue() ? 1.0 : 0.0);
      }

      return ret;
   }

   @Override
   protected ErrorType doWriteScheduleDefaultValue(AsnInputStream asnInputStream, int applicationTag) throws Exception {
      BNumericSchedule sched = (BNumericSchedule)this.getSchedule();
      switch (applicationTag) {
         case 4:
            BStatusNumeric defval = (BStatusNumeric)sched.getDefaultOutput().newCopy();
            defval.setValue(asnIn.readReal());
            defval.setStatusNull(false);
            sched.set(BControlSchedule.defaultOutput, defval, BLocalBacnetDevice.getBacnetContext());
            return null;
         default:
            return new NErrorType(2, 9);
      }
   }
}
