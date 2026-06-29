package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.stack.DeviceRegistry;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.DataTypeNotSupportedException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BBooleanSchedule;
import javax.baja.schedule.BControlSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"schedule:BooleanSchedule"}
   )}
)
@NiagaraProperty(
   name = "scheduleDataType",
   type = "BEnum",
   defaultValue = "BDynamicEnum.make(BOOLEAN_IDX, BOOL_DATA_TYPE_RANGE)"
)
public class BBacnetBooleanScheduleDescriptor extends BBacnetScheduleDescriptor {
   public static final int BOOLEAN_IDX = 0;
   public static final int ENUMERATED_IDX = 1;
   public static final BEnumRange BOOL_DATA_TYPE_RANGE = BEnumRange.make(
      new String[]{bacnetLexicon.get("BacnetBooleanScheduleDescriptor.boolean"), bacnetLexicon.get("BacnetBooleanScheduleDescriptor.enumerated")}
   );
   public static final Property scheduleDataType = newProperty(0, BDynamicEnum.make(0, BOOL_DATA_TYPE_RANGE), null);
   public static final Type TYPE = Sys.loadType(BBacnetBooleanScheduleDescriptor.class);

   public BEnum getScheduleDataType() {
      return (BEnum)this.get(scheduleDataType);
   }

   public void setScheduleDataType(BEnum v) {
      this.set(scheduleDataType, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   private boolean isAsnBoolean() {
      return this.getScheduleDataType().getOrdinal() == 0;
   }

   public BBacnetBooleanScheduleDescriptor() {
      this.setScheduleDataType(BDynamicEnum.make(this.getScheduleDataType().getOrdinal(), BOOL_DATA_TYPE_RANGE));
   }

   @Override
   public void doWritePresentValue() {
      BBooleanSchedule sched = (BBooleanSchedule)this.getSchedule();
      if (sched != null && sched.getEffective().isEffective(BAbsTime.now())) {
         BStatusBoolean out = sched.getOut();
         byte[] booleanWriteVal = AsnUtil.toAsnNull();
         byte[] enumeratedWriteVal = AsnUtil.toAsnNull();
         if (!out.getStatus().isNull()) {
            booleanWriteVal = AsnUtil.toAsnBoolean(out.getValue());
            enumeratedWriteVal = AsnUtil.toAsnEnumerated(out.getValue() ? 1 : 0);
         }

         SlotCursor<Property> c = this.getListOfObjectPropertyReferences().getProperties();

         while (c.next(BBacnetDeviceObjectPropertyReference.class)) {
            BBacnetDeviceObjectPropertyReference ref = (BBacnetDeviceObjectPropertyReference)c.get();
            byte[] writeVal = booleanWriteVal;
            PropertyInfo pi = BBacnetNetwork.localDevice().getPropertyInfo(ref.getObjectId().getObjectType(), ref.getPropertyId());
            if (ref.isDeviceIdUsed() && !ref.getDeviceId().equals(BBacnetNetwork.localDevice().getObjectId())) {
               BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(ref.getDeviceId());
               if (device != null) {
                  pi = device.getPropertyInfo(ref.getObjectId().getObjectType(), ref.getPropertyId());
               }

               if (pi != null) {
                  switch (pi.getAsnType()) {
                     case 9:
                        writeVal = enumeratedWriteVal;
                  }
               }

               BBacnetAddress addr = DeviceRegistry.getDeviceAddress(ref.getDeviceId());
               if (addr == null) {
                  this.findOrAddRemoteDeviceAndPoint(ref);
                  addr = DeviceRegistry.getDeviceAddress(ref.getDeviceId());
               }

               if (addr != null) {
                  try {
                     client().writeProperty(addr, ref.getObjectId(), ref.getPropertyId(), ref.getPropertyArrayIndex(), writeVal, this.getPriorityForWriting());
                  } catch (BacnetException var13) {
                     log.warning("BacnetException writing schedule output to " + ref + ": " + var13);
                  }
               } else {
                  log.warning("Unable to write Schedule output " + out + " to " + ref + ": unable to resolve device address");
               }
            } else {
               BIBacnetExportObject o = BBacnetNetwork.localDevice().lookupBacnetObject(ref.getObjectId());
               if (pi != null) {
                  switch (pi.getAsnType()) {
                     case 9:
                        writeVal = enumeratedWriteVal;
                  }
               }

               try {
                  ErrorType err = o.writeProperty(
                     new NBacnetPropertyValue(ref.getPropertyId(), ref.getPropertyArrayIndex(), writeVal, this.getPriorityForWriting())
                  );
                  if (err != null) {
                     throw new ErrorException(err);
                  }
               } catch (Exception var12) {
                  log.warning("Unable to write schedule output " + out + " from " + this + " to local object " + ref + ": " + var12);
               }
            }
         }

         this.setLastEffectiveValue((BStatusValue)out.newCopy());
      }
   }

   @Override
   final boolean isScheduleTypeLegal(BWeeklySchedule sched) {
      return sched instanceof BBooleanSchedule;
   }

   @Override
   protected boolean isEqual(int ansTypeOfRefObj, int asnTypeOfSchedule) {
      if (ansTypeOfRefObj == asnTypeOfSchedule) {
         return true;
      } else if (ansTypeOfRefObj == 9) {
         this.setScheduleDataType(BDynamicEnum.make(1, BOOL_DATA_TYPE_RANGE));
         return true;
      } else if (ansTypeOfRefObj == 1) {
         this.setScheduleDataType(BDynamicEnum.make(0, BOOL_DATA_TYPE_RANGE));
         return true;
      } else {
         return false;
      }
   }

   @Override
   int getAsnType() {
      switch (this.getScheduleDataType().getOrdinal()) {
         case 0:
            return 1;
         case 1:
            return 9;
         default:
            throw new IllegalStateException("Invalid Schedule Data Type for " + this + ":" + this.getScheduleDataType().getOrdinal());
      }
   }

   private byte[] encodeToAsn(boolean value) {
      return this.isAsnBoolean() ? AsnUtil.toAsnBoolean(value) : AsnUtil.toAsnEnumerated(value);
   }

   @Override
   final Property getScheduleOutputProperty() {
      return BBooleanSchedule.out;
   }

   @Override
   BStatusValue getEffectiveValueFrom(BStatusValue statusValue) {
      BStatusBoolean ret = new BStatusBoolean(false, BStatus.nullStatus);
      if (statusValue instanceof BStatusNumeric) {
         ret.setValue(((BStatusNumeric)statusValue).getValue() > 0.0);
      } else if (statusValue instanceof BStatusEnum) {
         ret.setValue(((BStatusEnum)statusValue).getValue().getOrdinal() == 0);
      } else if (statusValue instanceof BStatusBoolean) {
         ret = (BStatusBoolean)statusValue.newCopy(true);
      }

      return ret;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BBooleanSchedule sched = (BBooleanSchedule)this.getSchedule();
      if (sched == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 85:
               BAbsTime currentTime = BAbsTime.now();
               BStatusBoolean out;
               if (!sched.isEffective(currentTime) && this.getLastEffectiveValue() != null) {
                  out = (BStatusBoolean)this.getLastEffectiveValue();
               } else {
                  out = sched.getOut();
               }

               return out.getStatus().isNull()
                  ? new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull())
                  : new NReadPropertyResult(pId, ndx, this.encodeToAsn(out.getValue()));
            case 174:
               BStatusBoolean sb = (BStatusBoolean)sched.getDefaultOutput();
               if (sb.getStatus().isNull()) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull());
               }

               return new NReadPropertyResult(pId, ndx, this.encodeToAsn(sb.getValue()));
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BBooleanSchedule sched = (BBooleanSchedule)this.getSchedule();
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
                        int applicationTag = asnIn.peekApplicationTag();
                        switch (applicationTag) {
                           case 0:
                              sched.getIn().set(BStatusValue.status, BStatus.make(sched.getIn().getStatus(), 64, false), BLocalBacnetDevice.getBacnetContext());
                              sched.getOut()
                                 .set(BStatusValue.status, BStatus.make(sched.getOut().getStatus(), 64, true), BLocalBacnetDevice.getBacnetContext());
                              return null;
                           case 1:
                              if (this.isAsnBoolean()) {
                                 BStatusBoolean inval = (BStatusBoolean)sched.getIn().newCopy();
                                 inval.setValue(asnIn.readBoolean());
                                 inval.setStatusNull(false);
                                 sched.set(BBooleanSchedule.in, inval, BLocalBacnetDevice.getBacnetContext());
                                 return null;
                              }
                           case 9:
                              if (!this.isAsnBoolean()) {
                                 BStatusBoolean inval = (BStatusBoolean)sched.getIn().newCopy();
                                 inval.setValue(AsnUtil.fromOnlyBinaryPv(asnIn));
                                 inval.setStatusNull(false);
                                 sched.set(BBooleanSchedule.in, inval, BLocalBacnetDevice.getBacnetContext());
                                 return null;
                              }

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
         } catch (OutOfRangeException var11) {
            log.warning("Value out of range writing property " + pId + " in object " + this.getObjectId() + ": " + var11);
            return new NErrorType(2, 37);
         } catch (DataTypeNotSupportedException var12) {
            log.warning("Datatype not supported writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
            return new NErrorType(2, 47);
         } catch (AsnException var13) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var13);
            return new NErrorType(2, 9);
         } catch (PermissionException var14) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var14);
            return new NErrorType(2, 40);
         } catch (Exception var15) {
            log.warning("Exception writing property " + pId + " in object " + this.getObjectId() + ": " + var15);
            return new NErrorType(2, 0);
         }
      }
   }

   @Override
   protected ErrorType doWriteScheduleDefaultValue(AsnInputStream asnInputStream, int applicationTag) throws Exception {
      BBooleanSchedule sched = (BBooleanSchedule)this.getSchedule();
      switch (applicationTag) {
         case 1:
            if (this.isAsnBoolean()) {
               BStatusBoolean defval = (BStatusBoolean)sched.getDefaultOutput().newCopy();
               defval.setValue(asnIn.readBoolean());
               defval.setStatusNull(false);
               sched.set(BControlSchedule.defaultOutput, defval, BLocalBacnetDevice.getBacnetContext());
               return null;
            }
         case 9:
            if (!this.isAsnBoolean()) {
               BStatusBoolean defval = (BStatusBoolean)sched.getDefaultOutput().newCopy();
               defval.setValue(AsnUtil.fromOnlyBinaryPv(asnIn));
               defval.setStatusNull(false);
               sched.set(BControlSchedule.defaultOutput, defval, BLocalBacnetDevice.getBacnetContext());
               return null;
            }
         default:
            return new NErrorType(2, 9);
      }
   }
}
