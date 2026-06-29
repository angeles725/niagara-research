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
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BControlSchedule;
import javax.baja.schedule.BEnumSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusEnum;
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
      types = {"schedule:EnumSchedule"}
   )}
)
@NiagaraProperty(
   name = "scheduleDataType",
   type = "BEnum",
   defaultValue = "BDynamicEnum.make(LOCAL_UNSIGNED, ENUM_DATA_TYPE_RANGE)"
)
public class BBacnetEnumScheduleDescriptor extends BBacnetScheduleDescriptor {
   private static final int LOCAL_UNSIGNED = 0;
   private static final int LOCAL_ENUMERATED = 1;
   private static final int LOCAL_INTEGER = 2;
   private static final BEnumRange ENUM_DATA_TYPE_RANGE = BEnumRange.make(
      new String[]{AsnUtil.getAsnTypeName(2), AsnUtil.getAsnTypeName(9), AsnUtil.getAsnTypeName(3)}
   );
   public static final Property scheduleDataType = newProperty(0, BDynamicEnum.make(0, ENUM_DATA_TYPE_RANGE), null);
   public static final Type TYPE = Sys.loadType(BBacnetEnumScheduleDescriptor.class);

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

   @Override
   public void started() throws Exception {
      super.started();
      this.setScheduleDataType(BDynamicEnum.make(this.getScheduleDataType().getOrdinal(), ENUM_DATA_TYPE_RANGE));
   }

   private boolean isUnsigned() {
      return this.getScheduleDataType().getOrdinal() == 0;
   }

   private boolean isEnumerated() {
      return this.getScheduleDataType().getOrdinal() == 1;
   }

   private boolean isInteger() {
      return this.getScheduleDataType().getOrdinal() == 2;
   }

   @Override
   public void doWritePresentValue() {
      BEnumSchedule sched = (BEnumSchedule)this.getSchedule();
      if (sched != null && sched.getEffective().isEffective(BAbsTime.now())) {
         BStatusEnum out = sched.getOut();
         byte[] writeVal = AsnUtil.toAsnNull();
         if (!out.getStatus().isNull()) {
            switch (this.getScheduleDataType().getOrdinal()) {
               case 0:
                  writeVal = AsnUtil.toAsnUnsigned(out.getValue().getOrdinal());
                  break;
               case 1:
                  writeVal = AsnUtil.toAsnEnumerated(out.getValue().getOrdinal());
                  break;
               case 2:
                  writeVal = AsnUtil.toAsnInteger(out.getValue().getOrdinal());
                  break;
               default:
                  throw new IllegalStateException("Invalid Schedule Data Type for " + this + ":" + this.getScheduleDataType().getOrdinal());
            }
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
      return sched instanceof BEnumSchedule;
   }

   @Override
   protected boolean isEqual(int ansTypeOfRefObj, int asnTypeOfSchedule) {
      if (ansTypeOfRefObj == asnTypeOfSchedule) {
         return true;
      } else if (ansTypeOfRefObj == 2) {
         this.setScheduleDataType(BDynamicEnum.make(ENUM_DATA_TYPE_RANGE.getOrdinals()[0], ENUM_DATA_TYPE_RANGE));
         return true;
      } else if (ansTypeOfRefObj == 9) {
         this.setScheduleDataType(BDynamicEnum.make(ENUM_DATA_TYPE_RANGE.getOrdinals()[1], ENUM_DATA_TYPE_RANGE));
         return true;
      } else if (ansTypeOfRefObj == 3) {
         this.setScheduleDataType(BDynamicEnum.make(ENUM_DATA_TYPE_RANGE.getOrdinals()[2], ENUM_DATA_TYPE_RANGE));
         return true;
      } else {
         return false;
      }
   }

   @Override
   int getAsnType() {
      switch (this.getScheduleDataType().getOrdinal()) {
         case 0:
            return 2;
         case 1:
            return 9;
         case 2:
            return 3;
         default:
            throw new IllegalStateException("Invalid Schedule Data Type for " + this + ":" + this.getScheduleDataType().getOrdinal());
      }
   }

   @Override
   final Property getScheduleOutputProperty() {
      return BEnumSchedule.out;
   }

   @Override
   protected PropertyValue readProperty(int pId, int ndx) {
      BEnumSchedule sched = (BEnumSchedule)this.getSchedule();
      if (sched == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 85:
               BAbsTime currentTime = BAbsTime.now();
               BStatusEnum out;
               if (!sched.isEffective(currentTime) && this.getLastEffectiveValue() != null) {
                  out = (BStatusEnum)this.getLastEffectiveValue();
               } else {
                  out = sched.getOut();
               }

               return new NReadPropertyResult(pId, ndx, this.encodeAsn(out));
            case 174:
               BStatusEnum sms = (BStatusEnum)sched.getDefaultOutput();
               return new NReadPropertyResult(pId, ndx, this.encodeAsn(sms));
            default:
               return super.readProperty(pId, ndx);
         }
      }
   }

   @Override
   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BEnumSchedule sched = (BEnumSchedule)this.getSchedule();
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
                        BStatusEnum inval = (BStatusEnum)sched.getIn().newCopy();
                        BDynamicEnum ms = inval.getValue();
                        switch (asnIn.peekApplicationTag()) {
                           case 0:
                              sched.getOut()
                                 .set(BStatusValue.status, BStatus.make(sched.getOut().getStatus(), 64, true), BLocalBacnetDevice.getBacnetContext());
                              return null;
                           case 2:
                              if (this.isUnsigned()) {
                                 inval.setValue(BDynamicEnum.make(asnIn.readUnsignedInt(), ms.getRange()));
                                 sched.set(BEnumSchedule.in, inval, BLocalBacnetDevice.getBacnetContext());
                                 return null;
                              }
                           case 3:
                              if (this.isInteger()) {
                                 inval.setValue(BDynamicEnum.make(asnIn.readInteger(), ms.getRange()));
                                 sched.set(BEnumSchedule.in, inval, BLocalBacnetDevice.getBacnetContext());
                                 return null;
                              }
                           case 9:
                              if (this.isEnumerated()) {
                                 inval.setValue(BDynamicEnum.make(asnIn.readEnumerated(), ms.getRange()));
                                 sched.set(BEnumSchedule.in, inval, BLocalBacnetDevice.getBacnetContext());
                                 return null;
                              }
                           case 1:
                           case 4:
                           case 5:
                           case 6:
                           case 7:
                           case 8:
                           default:
                              return new NErrorType(2, 9);
                        }
                     }

                     return new NErrorType(2, 40);
                  default:
                     return super.writeProperty(pId, ndx, val, pri);
               }
            }
         } catch (AsnException var11) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var11);
            return new NErrorType(2, 9);
         } catch (PermissionException var12) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
            return new NErrorType(2, 40);
         } catch (Exception var13) {
            log.warning("Exception writing property " + pId + " in object " + this.getObjectId() + ": " + var13);
            return new NErrorType(2, 0);
         }
      }
   }

   @Override
   protected ErrorType doWriteScheduleDefaultValue(AsnInputStream asnInputStream, int applicationTag) throws Exception {
      BEnumSchedule sched = (BEnumSchedule)this.getSchedule();
      BStatusEnum defval = (BStatusEnum)sched.getDefaultOutput().newCopy();
      BDynamicEnum ms = defval.getValue();
      switch (applicationTag) {
         case 2:
            if (this.isUnsigned()) {
               defval.setValue(BDynamicEnum.make(asnIn.readUnsignedInt(), ms.getRange()));
               defval.setStatusNull(false);
               sched.set(BControlSchedule.defaultOutput, defval, BLocalBacnetDevice.getBacnetContext());
               return null;
            }
         case 3:
            if (this.isInteger()) {
               defval.setValue(BDynamicEnum.make(asnIn.readInteger(), ms.getRange()));
               defval.setStatusNull(false);
               sched.set(BControlSchedule.defaultOutput, defval, BLocalBacnetDevice.getBacnetContext());
               return null;
            }
         case 9:
            if (this.isEnumerated()) {
               defval.setValue(BDynamicEnum.make(asnIn.readEnumerated(), ms.getRange()));
               defval.setStatusNull(false);
               sched.set(BControlSchedule.defaultOutput, defval, BLocalBacnetDevice.getBacnetContext());
               return null;
            }
         default:
            return new NErrorType(2, 9);
      }
   }

   @Override
   BStatusValue getEffectiveValueFrom(BStatusValue statusValue) {
      BEnumSchedule schedule = (BEnumSchedule)this.getSchedule();
      BEnumRange range = (BEnumRange)schedule.getFacets().get("range");
      BStatusEnum ret = new BStatusEnum(BDynamicEnum.make(0, range), BStatus.nullStatus);
      if (statusValue instanceof BStatusEnum) {
         ret = (BStatusEnum)statusValue.newCopy(true);
      } else {
         ret.setValue(BDynamicEnum.make(0, range));
      }

      return ret;
   }

   private byte[] encodeAsn(BStatusEnum se) {
      if (se.getStatus().isNull()) {
         return AsnUtil.toAsnNull();
      } else {
         int ordinal = se.getValue().getOrdinal();
         switch (this.getScheduleDataType().getOrdinal()) {
            case 0:
               return AsnUtil.toAsnUnsigned(ordinal);
            case 1:
               return AsnUtil.toAsnEnumerated(ordinal);
            case 2:
               return AsnUtil.toAsnInteger(ordinal);
            default:
               throw new IllegalStateException("Invalid Schedule Data Type for " + this + ":" + this.getScheduleDataType().getOrdinal());
         }
      }
   }
}
