package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.schedule.BBacnetScheduleDeviceExt;
import com.tridium.bacnet.schedule.ScheduleSupport0;
import com.tridium.bacnet.schedule.ScheduleType;
import java.util.logging.Level;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAny;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.schedule.BBooleanSchedule;
import javax.baja.schedule.BCalendarSchedule;
import javax.baja.schedule.BEnumSchedule;
import javax.baja.schedule.BNumericSchedule;
import javax.baja.schedule.BStringSchedule;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetDiscoverSchedulesJob extends BBacnetDiscoverJob {
   public static final Type TYPE = Sys.loadType(BBacnetDiscoverSchedulesJob.class);
   private static final int[] SCHEDULE_PROPS_PRE_V4 = new int[]{85, 54, 28};
   private static final int[] SCHEDULE_PROPS_V4 = new int[]{174, 85, 54, 28};
   private BBacnetScheduleDeviceExt deviceExt;
   private static final AsnInputStream asnIn = new AsnInputStream();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetDiscoverSchedulesJob() {
   }

   public BBacnetDiscoverSchedulesJob(BBacnetScheduleDeviceExt deviceExt) {
      super(deviceExt);
      this.deviceExt = deviceExt;
   }

   @Override
   protected boolean doForId(BBacnetObjectIdentifier objectId) {
      int objectType = objectId.getObjectType();
      return objectType == 17 || objectType == 6;
   }

   @Override
   int[] getDiscoveryPropIds(BBacnetObjectIdentifier objectId) {
      int objectType = objectId.getObjectType();
      if (objectType == 6) {
         return DESCRIPTION_ONLY;
      } else if (objectType == 17) {
         return this.device.getProtocolRevision() >= 4 ? SCHEDULE_PROPS_V4 : SCHEDULE_PROPS_PRE_V4;
      } else {
         return DESCRIPTION_ONLY;
      }
   }

   @Override
   void addDiscoveryChild(BBacnetDiscoverJob.IdVals iv) {
      ScheduleType st = this.findScheduleType(iv);
      BDiscoverySchedule ds = new BDiscoverySchedule(iv.name, iv.id, st.getDataType(), st.getScheduleType());
      BBacnetDiscoverJob.PropVal desc = iv.get(28);
      if (desc != null) {
         ds.setDescription(desc.toString());
      }

      this.add(null, ds);
   }

   private ScheduleType findScheduleType(BBacnetDiscoverJob.IdVals iv) {
      if (iv.id.getObjectType() == 6) {
         return new ScheduleType(BCalendarSchedule.TYPE.toString(), "");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("findScheduletype: iv=" + iv);
         }

         ScheduleType st = null;
         if (this.device.getProtocolRevision() >= 4) {
            BBacnetDiscoverJob.PropVal def = iv.get(174);
            st = this.getScheduleType(def);
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("st from Sched_Def: def=" + (def == null ? "null" : def.dbg()) + "; st=" + st);
            }
         }

         if (st != null) {
            return st;
         } else {
            BBacnetDiscoverJob.PropVal pv = iv.get(85);
            st = this.getScheduleType(pv);
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("st from Pres_Val: def=" + (pv == null ? "null" : pv.dbg()) + "; st=" + st);
            }

            if (st != null) {
               return st;
            } else {
               BBacnetDiscoverJob.PropVal loopr = iv.get(54);
               st = this.getScheduleType(loopr);
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("st from LoOPRs: loopr=" + (pv == null ? "null" : pv.dbg()) + "; st=" + st);
               }

               if (st != null) {
                  return st;
               } else {
                  this.log().message("Trying to determine schedule type for " + iv.id + " from Weekly_Schedule...");
                  int i = 0;
                  byte[] encodedValue = null;

                  try {
                     for (i = 1; i <= 7; i++) {
                        encodedValue = client().readProperty(this.device.getAddress(), iv.id, 123, i);
                        synchronized (asnIn) {
                           asnIn.setBuffer(encodedValue);
                           st = this.supp().getScheduleType(0, asnIn);
                        }

                        if (st != null) {
                           return st;
                        }
                     }
                  } catch (AsnException var15) {
                     this.log()
                        .failed(
                           "   ERROR: Unable to convert ASN-encoded Weekly_Schedule["
                              + i
                              + "] ("
                              + ByteArrayUtil.toHexString(encodedValue)
                              + ") for object "
                              + iv.id
                        );
                     logger.info(
                        "Unable to convert ASN-encoded Weekly_Schedule[" + i + "] (" + ByteArrayUtil.toHexString(encodedValue) + ") for object " + iv.id
                     );
                  } catch (BacnetException var16) {
                     logger.info("Error reading Weekly_Schedule[" + i + "] for " + iv.id + ":" + var16);
                  }

                  this.log().message("Trying to determine schedule type for " + iv.id + " from Excepion_Schedule...");

                  try {
                     int excSchedLen = AsnUtil.fromAsnUnsignedInt(client().readProperty(this.device.getAddress(), iv.id, 38, 0));

                     for (i = 1; i <= excSchedLen; i++) {
                        encodedValue = client().readProperty(this.device.getAddress(), iv.id, 38, i);
                        synchronized (asnIn) {
                           asnIn.setBuffer(encodedValue);

                           int tag;
                           for (tag = asnIn.peekTag(); tag != -1 && !asnIn.isOpeningTag(2); tag = asnIn.peekTag()) {
                              asnIn.skipTag();
                           }

                           if (tag != -1) {
                              st = this.supp().getScheduleType(2, asnIn);
                           }
                        }

                        if (st != null) {
                           return st;
                        }
                     }
                  } catch (AsnException var13) {
                     this.log()
                        .failed(
                           "   ERROR: Unable to convert ASN-encoded Exception_Schedule["
                              + i
                              + "] ("
                              + ByteArrayUtil.toHexString(encodedValue)
                              + ") for object "
                              + iv.id
                        );
                     logger.info(
                        "Unable to convert ASN-encoded Exception_Schedule[" + i + "] (" + ByteArrayUtil.toHexString(encodedValue) + ") for object " + iv.id
                     );
                  } catch (Exception var14) {
                     logger.info("Error reading Exception_Schedule[" + i + "] for " + iv.id + ":" + var14);
                  }

                  this.log().failed("Unable to determine schedule type for " + iv.id + "!! Defaulting to String schedule type...");
                  return new ScheduleType(BStringSchedule.TYPE.toString());
               }
            }
         }
      }
   }

   private ScheduleType getScheduleType(BBacnetDiscoverJob.PropVal pv) {
      if (pv == null) {
         return null;
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("getScheduleType: pv=" + pv);
         }

         BValue v = pv.val;
         if (v == null) {
            return null;
         } else {
            if (v instanceof BBacnetAny) {
               v = ((BBacnetAny)v).getValue();
            }

            if (v instanceof BBacnetNull) {
               return null;
            } else if (v instanceof BBacnetListOf) {
               BBacnetListOf list = (BBacnetListOf)v;
               SlotCursor<Property> sc = list.getProperties();
               ScheduleType schType = null;

               while (sc.next(BBacnetDeviceObjectPropertyReference.class)) {
                  schType = this.scheduleTypeFromRef((BBacnetDeviceObjectPropertyReference)sc.get());
                  if (schType != null) {
                     return schType;
                  }
               }

               return null;
            } else {
               int dataType = AsnUtil.getAsnType(v.getType());
               String dt = AsnUtil.getAsnTypeName(dataType);
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("  v=" + v + " [" + v.getType() + "]; dt=" + dt);
               }

               if (dataType != 0) {
                  switch (dataType) {
                     case 0:
                        return null;
                     case 1:
                        return new ScheduleType(BBooleanSchedule.TYPE.toString(), dt);
                     case 2:
                     case 3:
                     case 9:
                        return new ScheduleType(BEnumSchedule.TYPE.toString(), dt);
                     case 4:
                     case 5:
                        return new ScheduleType(BNumericSchedule.TYPE.toString(), dt);
                     case 6:
                     case 7:
                     case 8:
                  }
               }

               return new ScheduleType(BStringSchedule.TYPE.toString(), dt);
            }
         }
      }
   }

   private ScheduleType scheduleTypeFromRef(BBacnetDeviceObjectPropertyReference dop) {
      int objectType = dop.getObjectId().getObjectType();
      int propertyId = dop.getPropertyId();
      PropertyInfo info = this.device().getPropertyInfo(objectType, propertyId);
      if (info != null) {
         String dt = AsnUtil.getAsnTypeName(info.getAsnType());
         switch (info.getAsnType()) {
            case 0:
            case 6:
            case 7:
            case 8:
            case 10:
            case 11:
            case 12:
               return new ScheduleType(BStringSchedule.TYPE.toString(), dt);
            case 1:
               return new ScheduleType(BBooleanSchedule.TYPE.toString(), dt);
            case 2:
            case 3:
            case 9:
               return new ScheduleType(BEnumSchedule.TYPE.toString(), dt);
            case 4:
            case 5:
               return new ScheduleType(BNumericSchedule.TYPE.toString(), dt);
         }
      }

      return null;
   }

   private ScheduleSupport0 supp() {
      return this.deviceExt.getSupport();
   }
}
