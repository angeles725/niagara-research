package com.tridium.bacnet.datatypes;

import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.server.BEventHandler;
import com.tridium.bacnet.util.BacnetAlarmRecipientUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmDbConnection;
import javax.baja.alarm.BAckState;
import javax.baja.alarm.BAlarmRecipient;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BSourceState;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.export.BBacnetEventSource;
import javax.baja.bacnet.export.BBacnetNotificationClassDescriptor;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BEventSaver extends BAlarmRecipient {
   public static final Type TYPE = Sys.loadType(BEventSaver.class);
   private static final Logger logger = Logger.getLogger("bacnet.server");

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNotificationClassDescriptor;
   }

   public void handleAlarm(BAlarmRecord rec) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetEventSource evtSrc = null;
      boolean traceOn = logger.isLoggable(Level.FINE);

      try {
         if (traceOn) {
            logger.fine("handleAlarm on " + SlotPath.unescape(this.getName()) + ":" + rec + "\n alarmData=" + rec.getAlarmData() + "\n uuid=" + rec.getUuid());
         }

         BObject src = rec.getSource().get(0).get(this);
         BBacnetObjectIdentifier id = BacnetAlarmRecipientUtil.getEventObjectId(src);
         if (id == null) {
            if (traceOn) {
               logger.fine("event not saved: object not exported to BACnet");
            }

            return;
         }

         BSourceState srcSt = rec.getSourceState();
         if (srcSt == BSourceState.alert) {
            if (traceOn) {
               logger.fine("event not saved: alert");
            }

            return;
         }

         try {
            evtSrc = (BBacnetEventSource)local.lookupBacnetObject(id);
         } catch (ClassCastException var26) {
            if (traceOn) {
               logger.fine("event not saved: object not a BacnetEventSource");
            }

            return;
         }

         if (evtSrc == null) {
            if (traceOn) {
               logger.fine("event not saved: event object not exported");
            }

            return;
         }

         BBacnetNotificationClassDescriptor nc = evtSrc.getNotificationClass();
         if (nc == null) {
            if (traceOn) {
               logger.fine("event not saved: alarm class not exported to BACnet");
            }

            return;
         }

         BEventHandler eh = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getEventHandler();
         if (!evtSrc.getEventDetectionEnable()) {
            if (traceOn) {
               logger.fine("Event detection enable is false. Do not add the events in event buffers for " + local.getObjectId() + " " + id);
            }

            return;
         }

         if (rec.getSourceState() == BSourceState.normal && rec.getAckState() == BAckState.acked) {
            eh.removeEventSummary(id);
            if (traceOn) {
               logger.fine("Remove record from outstanding events: " + local.getObjectId() + " " + id + " " + 0L);
            }
         } else {
            BBacnetEventState eventState = BBacnetEventState.make(rec.getSourceState());
            if (traceOn) {
               logger.fine("Save record to " + eventState + " buffer: " + local.getObjectId() + " " + id + " " + 0L);
            }

            eh.addEventSummary(id);
            eh.putRecordToEventBuffer(eventState.getOrdinal(), local.getObjectId(), id, 0L, rec);
            BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);

            try {
               AlarmDbConnection conn = as.getAlarmDb().getDbConnection(null);
               Throwable var13 = null;

               try {
                  conn.update(rec);
               } catch (Throwable var25) {
                  var13 = var25;
                  throw var25;
               } finally {
                  if (conn != null) {
                     if (var13 != null) {
                        try {
                           conn.close();
                        } catch (Throwable var24) {
                           var13.addSuppressed(var24);
                        }
                     } else {
                        conn.close();
                     }
                  }
               }
            } catch (Exception var28) {
               logger.log(Level.SEVERE, "BEventHandler.putRecordToEventBuffer:Unable to update Alarm record", (Throwable)var28);
            }
         }
      } catch (Exception var29) {
         if (traceOn) {
            logger.fine("Exception in BEventSaver.handleAlarm() [" + this.getName() + "]");
         }
      }
   }

   public boolean accept(BAlarmRecord rec) {
      return true;
   }
}
