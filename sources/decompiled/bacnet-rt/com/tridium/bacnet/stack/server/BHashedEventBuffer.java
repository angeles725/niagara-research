package com.tridium.bacnet.stack.server;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmDbConnection;
import javax.baja.alarm.BAlarmDatabase;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BUuid;

@NiagaraType
public final class BHashedEventBuffer extends BComponent {
   public static final Type TYPE = Sys.loadType(BHashedEventBuffer.class);
   private static final Logger logger = Logger.getLogger("bacnet.server");

   public Type getType() {
      return TYPE;
   }

   public String toString(Context cx) {
      return "eventCount=" + this.getSlotCount();
   }

   public synchronized void putRecord(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId, BAlarmRecord record) {
      String key = deviceId.toString(BacnetConst.facetsContext) + "_" + objectId.toString(BacnetConst.facetsContext) + "_" + processId;

      try {
         BValue v = this.get(key);
         if (v != null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getName() + ": putRecord:replacing old uuid:" + v + " with new uuid:" + record.getUuid());
            }

            this.set(key, record.getUuid());
         } else {
            this.add(key, record.getUuid());
         }
      } catch (Exception var8) {
      }
   }

   public synchronized void removeRecord(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId) {
      String key = deviceId.toString(BacnetConst.facetsContext) + "_" + objectId.toString(BacnetConst.facetsContext) + "_" + processId;

      try {
         BValue v = this.get(key);
         if (v != null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getName() + ": removeRecord: " + v + " key: " + key);
            }

            this.remove(key);
         }
      } catch (Exception var7) {
         logger.log(Level.SEVERE, this.getName() + ": Failed to remove the record: " + key);
      }
   }

   public synchronized BAlarmRecord getRecord(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId, BUuid uuid, boolean remove) {
      String key = deviceId.toString(BacnetConst.facetsContext) + "_" + objectId.toString(BacnetConst.facetsContext) + "_" + processId;
      return this.getRecord(key, uuid, remove);
   }

   public synchronized BAlarmRecord getRecord(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId, boolean remove) {
      return this.getRecord(deviceId, objectId, processId, null, remove);
   }

   public synchronized boolean checkRecord(BAlarmRecord ackRecord) {
      if (ackRecord == null) {
         return false;
      } else {
         BFacets alarmData = ackRecord.getAlarmData();
         if (alarmData == null) {
            return false;
         } else {
            BString deviceStr = (BString)alarmData.getFacet("deviceId");
            BString objectStr = (BString)alarmData.getFacet("objectId");
            BString processIdStr = (BString)alarmData.getFacet("processId");
            String key = "no record";
            if (deviceStr != null && objectStr != null && processIdStr != null) {
               key = deviceStr + "_" + objectStr + "_" + processIdStr;
            }

            BAlarmRecord currentRecord = this.getRecord(key, null, false);
            return currentRecord == null ? false : ackRecord.getTimestamp().equals(currentRecord.getTimestamp());
         }
      }
   }

   private synchronized BAlarmRecord getRecord(String key, BUuid match, boolean remove) {
      Property property = this.getProperty(key);
      if (property == null) {
         return null;
      } else {
         BUuid uuid = (BUuid)this.get(key);
         if (uuid == null) {
            return null;
         } else if (match != null && !uuid.equals(match)) {
            return null;
         } else {
            Object alarmDb;
            try {
               BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
               BAlarmDatabase alarmDbx = as.getAlarmDb();
               AlarmDbConnection conn = alarmDbx.getDbConnection(null);
               Throwable var9 = null;

               try {
                  return conn.getRecord(uuid);
               } catch (Throwable var35) {
                  var9 = var35;
                  throw var35;
               } finally {
                  if (conn != null) {
                     if (var9 != null) {
                        try {
                           conn.close();
                        } catch (Throwable var34) {
                           var9.addSuppressed(var34);
                        }
                     } else {
                        conn.close();
                     }
                  }
               }
            } catch (Exception var37) {
               logger.log(Level.SEVERE, "Exception occurred in getRecord", (Throwable)var37);
               alarmDb = null;
            } finally {
               if (remove) {
                  try {
                     this.remove(key);
                  } catch (Throwable var33) {
                     logger.log(Level.SEVERE, "Throwable occurred in getRecord", var33);
                  }
               }
            }

            return (BAlarmRecord)alarmDb;
         }
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      BAlarmDatabase alarmDb = null;

      try {
         BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
         alarmDb = as.getAlarmDb();
      } catch (Exception var20) {
      }

      out.startProps();
      out.trTitle("Event Buffer", 2);
      BUuid[] uuids = (BUuid[])this.getChildren(BUuid.class);
      if (alarmDb != null) {
         AlarmDbConnection conn = alarmDb.getDbConnection(null);
         Throwable var6 = null;

         try {
            for (int i = 0; i < uuids.length; i++) {
               BAlarmRecord dbRecord = null;

               try {
                  dbRecord = conn.getRecord(uuids[i]);
               } catch (IOException var19) {
               }

               String recFacets = dbRecord != null ? dbRecord.getAlarmData().toString() : "";
               out.prop(dbRecord, recFacets);
            }
         } catch (Throwable var21) {
            var6 = var21;
            throw var21;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var18) {
                     var6.addSuppressed(var18);
                  }
               } else {
                  conn.close();
               }
            }
         }
      }

      out.endProps();
   }
}
