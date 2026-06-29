package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.datatypes.BTrendEvent;
import com.tridium.bacnet.history.BBacnetTrendLogAlarmSourceExt;
import com.tridium.bacnet.history.BBacnetTrendRecord;
import com.tridium.bacnet.history.BacnetTrendLogUtil;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetLogRecord;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BControlPoint;
import javax.baja.history.BCollectionInterval;
import javax.baja.history.BFullPolicy;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.BTrendRecord;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Cursor;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"history:IHistory"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "id",
      type = "BHistoryId",
      defaultValue = "BHistoryId.NULL",
      flags = 8
   ), @NiagaraProperty(
      name = "historyOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      flags = 5
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.TREND_LOG)",
      flags = 64
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "firstTimestamp",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 5
   ), @NiagaraProperty(
      name = "firstSeqNum",
      type = "long",
      defaultValue = "0",
      flags = 5
   ), @NiagaraProperty(
      name = "lastTimestamp",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 5
   ), @NiagaraProperty(
      name = "lastSeqNum",
      type = "long",
      defaultValue = "0",
      flags = 5
   )})
public class BBacnetNiagaraHistoryDescriptor extends BBacnetEventSource {
   public static final Property id = newProperty(8, BHistoryId.NULL, null);
   public static final Property historyOrd = newProperty(5, BOrd.DEFAULT, null);
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(20), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Property firstTimestamp = newProperty(5, BAbsTime.NULL, null);
   public static final Property firstSeqNum = newProperty(5, 0, null);
   public static final Property lastTimestamp = newProperty(5, BAbsTime.NULL, null);
   public static final Property lastSeqNum = newProperty(5, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetNiagaraHistoryDescriptor.class);
   private static final int[] ARRAY_PROPS = new int[]{130, 351, 352, 371};
   private static final BIcon icon = BIcon.make(BIcon.std("history.png"), BIcon.std("badges/export.png"));
   private int[] optionalProps;
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private static AsnOutputStream asnOut = new AsnOutputStream();
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 133, 144, 126, 131, 141, 145, 36};
   static Logger log = Logger.getLogger("bacnet.server");

   public BHistoryId getId() {
      return (BHistoryId)this.get(id);
   }

   public void setId(BHistoryId v) {
      this.set(id, v, null);
   }

   public BOrd getHistoryOrd() {
      return (BOrd)this.get(historyOrd);
   }

   public void setHistoryOrd(BOrd v) {
      this.set(historyOrd, v, null);
   }

   @Override
   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   @Override
   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   @Override
   public String getObjectName() {
      return this.getString(objectName);
   }

   @Override
   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public BAbsTime getFirstTimestamp() {
      return (BAbsTime)this.get(firstTimestamp);
   }

   public void setFirstTimestamp(BAbsTime v) {
      this.set(firstTimestamp, v, null);
   }

   public long getFirstSeqNum() {
      return this.getLong(firstSeqNum);
   }

   public void setFirstSeqNum(long v) {
      this.setLong(firstSeqNum, v, null);
   }

   public BAbsTime getLastTimestamp() {
      return (BAbsTime)this.get(lastTimestamp);
   }

   public void setLastTimestamp(BAbsTime v) {
      this.set(lastTimestamp, v, null);
   }

   public long getLastSeqNum() {
      return this.getLong(lastSeqNum);
   }

   public void setLastSeqNum(long v) {
      this.setLong(lastSeqNum, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public final void started() throws Exception {
      super.started();
      if (this.getFirstTimestamp().equals(BAbsTime.NULL)) {
         this.initialize();
      }

      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      this.checkConfiguration();
      if (Sys.isStationStarted()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      HistoryDatabaseConnection conn = getHistoryDbConnection(null);
      Throwable var3 = null;

      try {
         local.unsubscribe(this, this.getHistory(conn));
      } catch (Throwable var12) {
         var3 = var12;
         throw var12;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var11) {
                  var3.addSuppressed(var11);
               }
            } else {
               conn.close();
            }
         }
      }

      this.optionalProps = null;
      this.oldId = null;
      this.oldName = null;
      if (local.isRunning()) {
         local.incrementDatabaseRevision();
      }
   }

   @Override
   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (p.equals(historyOrd)) {
         this.setId(BHistoryId.make(this.getHistoryOrd().parse()[0].getBody()));
         if (this.isRunning()) {
            this.checkConfiguration();
         }
      } else if (this.isRunning()) {
         if (p.equals(objectId)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldId = this.getObjectId();

            try {
               ((BComponent)this.getParent()).rename(this.getPropertyInParent(), this.getObjectId().toString(nameContext));
            } catch (DuplicateSlotException var4) {
            }

            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(objectName)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldName = this.getObjectName();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(id)) {
            this.setHistoryOrd(BOrd.make("history:" + this.getId()));
            this.initialize();
            this.checkConfiguration();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         }
      }
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(20) : super.getSlotFacets(s);
   }

   @Override
   public final BObject getObject() {
      return (BObject)this.getHistory();
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getHistoryOrd();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(historyOrd, objectOrd, cx);
   }

   @Override
   public void checkConfiguration() {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else {
         HistoryDatabaseConnection conn = getHistoryDbConnection(null);
         Throwable var3 = null;

         try {
            local.unsubscribe(this, this.getHistory(conn));
            boolean configOk = true;
            if (this.getHistory(conn) == null) {
               this.setFaultCause("Cannot find exported history");
               configOk = false;
            } else {
               local.subscribe(this, this.getHistory(conn));
            }

            if (!this.getObjectId().isValid()) {
               this.setFaultCause("Invalid Object ID");
               configOk = false;
            }

            if (configOk) {
               String err = BBacnetNetwork.localDevice().export(this);
               if (err != null) {
                  this.duplicate = true;
                  this.setFaultCause(err);
                  configOk = false;
               } else {
                  this.duplicate = false;
               }
            }

            if (configOk) {
               this.setFaultCause("");
            }

            this.setStatus(BStatus.makeFault(this.getStatus(), !configOk));
         } catch (Throwable var13) {
            var3 = var13;
            throw var13;
         } finally {
            if (conn != null) {
               if (var3 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var12) {
                     var3.addSuppressed(var12);
                  }
               } else {
                  conn.close();
               }
            }
         }
      }
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BBacnetTrendLogAlarmSourceExt;
   }

   @Deprecated
   @Override
   protected void updateAlarmInhibit() {
   }

   @Override
   public final boolean isEventInitiationEnabled() {
      return this.getNotificationClass() != null;
   }

   @Override
   public final BEnum getEventState() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BBacnetEventState.make(almExt.getAlarmState());
   }

   @Override
   public BControlPoint getPoint() {
      return null;
   }

   @Override
   public final BBacnetBitString getAckedTransitions() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAckedTransitions());
   }

   @Override
   public final BBacnetTimeStamp[] getEventTimeStamps() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt == null) {
         return null;
      } else {
         BAbsTime normalTime = almExt.getToOffnormalTimes().getNormalTime();
         if (normalTime.isBefore(almExt.getToFaultTimes().getNormalTime())) {
            normalTime = almExt.getToFaultTimes().getNormalTime();
         }

         return new BBacnetTimeStamp[]{
            new BBacnetTimeStamp(almExt.getToOffnormalTimes().getAlarmTime()),
            new BBacnetTimeStamp(almExt.getToFaultTimes().getAlarmTime()),
            new BBacnetTimeStamp(normalTime)
         };
      }
   }

   @Override
   public final BBacnetNotifyType getNotifyType() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : almExt.getNotifyType();
   }

   @Override
   public final BBacnetBitString getEventEnable() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAlarmEnable());
   }

   @Override
   public final int[] getEventPriorities() {
      BBacnetNotificationClassDescriptor nc = this.getNotificationClass();
      return nc == null ? null : nc.getEventPriorities();
   }

   @Override
   public final BBacnetNotificationClassDescriptor getNotificationClass() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt == null) {
         return null;
      } else {
         try {
            BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
            BAlarmClass ac = as.lookupAlarmClass(almExt.getAlarmClass());
            SlotCursor<Property> c = ac.getProperties();
            if (c.next(BBacnetNotificationClassDescriptor.class)) {
               return (BBacnetNotificationClassDescriptor)c.get();
            }
         } catch (ServiceNotFoundException var5) {
            log.log(Level.SEVERE, "getNotificationClass on " + this + ": Unable to find alarm service", (Throwable)var5);
         }

         return null;
      }
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.bufferReady;
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   final BBacnetTrendLogAlarmSourceExt getAlarmExt() {
      SlotCursor<Property> c = this.getProperties();
      if (c.next(BBacnetTrendLogAlarmSourceExt.class)) {
         return (BBacnetTrendLogAlarmSourceExt)c.get();
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": found no associated BacnetTrendLogAlarmSourceExt");
         }

         return null;
      }
   }

   private void initialize() {
      HistoryDatabaseConnection conn = getHistoryDbConnection(null);
      Throwable var2 = null;

      try {
         BIHistory history = this.getHistory(conn);
         if (history == null) {
            this.resetTimestamps();
            return;
         }

         int dataSize = conn.getRecordCount(history);
         if (dataSize >= 1) {
            this.setFirstTimestamp(conn.getFirstTimestamp(history));
            this.setFirstSeqNum(1L);
            this.setLastTimestamp(conn.getLastTimestamp(history));
            this.setLastSeqNum(dataSize);
            return;
         }

         this.resetTimestamps();
      } catch (Throwable var15) {
         var2 = var15;
         throw var15;
      } finally {
         if (conn != null) {
            if (var2 != null) {
               try {
                  conn.close();
               } catch (Throwable var14) {
                  var2.addSuppressed(var14);
               }
            } else {
               conn.close();
            }
         }
      }
   }

   private void resetTimestamps() {
      this.setFirstTimestamp(BAbsTime.NULL);
      this.setFirstSeqNum(0L);
      this.setLastTimestamp(BAbsTime.NULL);
      this.setLastSeqNum(0L);
   }

   public final BIHistory getHistory() {
      if (!this.isRunning()) {
         return null;
      } else {
         try {
            HistoryDatabaseConnection conn = getHistoryDbConnection(null);
            Throwable var2 = null;

            BIHistory var3;
            try {
               var3 = conn.getHistory(this.getId());
            } catch (Throwable var13) {
               var2 = var13;
               throw var13;
            } finally {
               if (conn != null) {
                  if (var2 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var12) {
                        var2.addSuppressed(var12);
                     }
                  } else {
                     conn.close();
                  }
               }
            }

            return var3;
         } catch (Exception var15) {
            logException(Level.SEVERE, this.getObjectId() + ": Exception occurred in getHistory", var15);
            return null;
         }
      }
   }

   public final BIHistory getHistory(HistoryDatabaseConnection conn) {
      if (!this.isRunning()) {
         return null;
      } else {
         try {
            return conn.getHistory(this.getId());
         } catch (Exception var3) {
            logException(Level.SEVERE, this.getObjectId() + ": Exception occurred in getHistory for history ID " + this.getId(), var3);
            return null;
         }
      }
   }

   private static HistoryDatabaseConnection getHistoryDbConnection(Context cx) {
      BHistoryService service = (BHistoryService)Sys.getService(BHistoryService.TYPE);
      BHistoryDatabase db = service.getDatabase();
      return db.getDbConnection(true, cx);
   }

   private void reinitTimestamps(HistoryDatabaseConnection conn, BIHistory history) {
      BAbsTime newFirstTimestamp = conn.getFirstTimestamp(history);
      if (newFirstTimestamp.isNull()) {
         this.setFirstTimestamp(BAbsTime.NULL);
         this.setFirstSeqNum(0L);
         this.setLastTimestamp(BAbsTime.NULL);
      } else {
         BAbsTime oldFirstTimestamp = this.getFirstTimestamp();
         BAbsTime newLastTimestamp = conn.getLastTimestamp(history);
         BAbsTime oldLastTimestamp = this.getLastTimestamp();
         if (!newFirstTimestamp.equals(oldFirstTimestamp)) {
            if (newFirstTimestamp.isAfter(oldLastTimestamp)) {
               this.setFirstTimestamp(newFirstTimestamp);
               this.setFirstSeqNum(getSequenceNumber(this.getLastSeqNum() + 1L));
            } else {
               Cursor<BHistoryRecord> data = conn.timeQuery(history, newFirstTimestamp, oldLastTimestamp).cursor();
               Throwable var8 = null;

               try {
                  long count = countRecords(data, oldLastTimestamp);
                  this.setFirstTimestamp(newFirstTimestamp);
                  this.setFirstSeqNum(getSequenceNumber(this.getLastSeqNum() - count));
               } catch (Throwable var33) {
                  var8 = var33;
                  throw var33;
               } finally {
                  if (data != null) {
                     if (var8 != null) {
                        try {
                           data.close();
                        } catch (Throwable var31) {
                           var8.addSuppressed(var31);
                        }
                     } else {
                        data.close();
                     }
                  }
               }
            }

            if (newFirstTimestamp.isBefore(oldFirstTimestamp)) {
               log.log(
                  Level.WARNING,
                  "History has been altered causing the timestamp of the oldest record to be earlier than the timestamp of the previous oldest record; BACnet Object ID: "
                     + this.getObjectId()
                     + ", history ID: "
                     + history.getId()
               );
            }
         }

         if (!newLastTimestamp.equals(oldLastTimestamp)) {
            if (newLastTimestamp.isAfter(oldLastTimestamp)) {
               Cursor<BHistoryRecord> data = conn.timeQuery(history, oldLastTimestamp, newLastTimestamp).cursor();
               Throwable var37 = null;

               try {
                  long count = countRecords(data, oldLastTimestamp);
                  this.setLastTimestamp(newLastTimestamp);
                  this.setLastSeqNum(getSequenceNumber(this.getLastSeqNum() + count));
               } catch (Throwable var32) {
                  var37 = var32;
                  throw var32;
               } finally {
                  if (data != null) {
                     if (var37 != null) {
                        try {
                           data.close();
                        } catch (Throwable var30) {
                           var37.addSuppressed(var30);
                        }
                     } else {
                        data.close();
                     }
                  }
               }
            } else {
               if (newLastTimestamp.isBefore(oldLastTimestamp)) {
                  log.log(
                     Level.WARNING,
                     "History has been altered causing the timestamp of the newest record to be earlier than the timestamp of the previous newest record; BACnet Object ID: "
                        + this.getObjectId()
                        + ", history ID: "
                        + history.getId()
                  );
               }
            }
         }
      }
   }

   private static long countRecords(Cursor<BHistoryRecord> data, BAbsTime excludeTimestamp) {
      long count = 0L;

      while (data.next()) {
         if (!((BHistoryRecord)data.get()).getTimestamp().equals(excludeTimestamp)) {
            count++;
         }
      }

      return count;
   }

   private static long getSequenceNumber(long newSeqNum) {
      if (newSeqNum > BacnetTrendLogUtil.MAX_SEQ_NUM) {
         newSeqNum -= BacnetTrendLogUtil.MAX_SEQ_NUM;
      }

      return newSeqNum;
   }

   private BBacnetNiagaraHistoryDescriptor.ReadLogResult readRangeAll(int maxSize) {
      long itemCount = 0L;
      long firstFoundSequenceNumber = 0L;
      boolean includesFirstItem = false;
      boolean moreItems = false;
      synchronized (asnOut) {
         asnOut.reset();
         HistoryDatabaseConnection conn = getHistoryDbConnection(null);
         Throwable var10 = null;

         BBacnetNiagaraHistoryDescriptor.ReadLogResult var53;
         try {
            BIHistory dataHistory = this.getHistory(conn);
            this.reinitTimestamps(conn, dataHistory);
            Cursor<BHistoryRecord> data = conn.scan(dataHistory);
            Throwable var13 = null;

            try {
               BTrendRecord r = null;
               long loopCount = this.getFirstSeqNum();

               boolean isNext;
               for (isNext = data.next(); isNext; isNext = data.next()) {
                  r = (BTrendRecord)data.get();
                  if (getLogDatumType(r) >= 0) {
                     AsnOutputStream temp = new AsnOutputStream();

                     try {
                        asnOut.writeTo(temp);
                     } catch (Exception var46) {
                        log.log(Level.WARNING, "Error exporting all trend records, Bacnet Object ID " + this.getObjectId(), (Throwable)var46);
                        temp = asnOut;
                     }

                     BBacnetLogRecord.writeLogRecord(
                        r.getTimestamp(), (BSimple)r.get(r.getValueProperty()), getLogDatumType(r), r.getStatus(), getLogEvent(r).getLong(), asnOut
                     );
                     if (maxSize > 0 && asnOut.size() > maxSize) {
                        asnOut = temp;
                        moreItems = true;
                        break;
                     }

                     if (loopCount == this.getFirstSeqNum()) {
                        includesFirstItem = true;
                     }

                     if (itemCount == 0L) {
                        firstFoundSequenceNumber = loopCount;
                     }

                     itemCount++;
                  }

                  loopCount++;
               }

               var53 = new BBacnetNiagaraHistoryDescriptor.ReadLogResult(
                  itemCount, firstFoundSequenceNumber, asnOut.toByteArray(), includesFirstItem, !isNext, moreItems
               );
            } catch (Throwable var47) {
               var13 = var47;
               throw var47;
            } finally {
               if (data != null) {
                  if (var13 != null) {
                     try {
                        data.close();
                     } catch (Throwable var45) {
                        var13.addSuppressed(var45);
                     }
                  } else {
                     data.close();
                  }
               }
            }
         } catch (Throwable var49) {
            var10 = var49;
            throw var49;
         } finally {
            if (conn != null) {
               if (var10 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var44) {
                     var10.addSuppressed(var44);
                  }
               } else {
                  conn.close();
               }
            }
         }

         return var53;
      }
   }

   private BBacnetNiagaraHistoryDescriptor.ReadLogResult readRangeByPosition(long refIndex, int count, int maxSize) {
      HistoryDatabaseConnection conn = getHistoryDbConnection(null);
      Throwable var6 = null;

      BBacnetNiagaraHistoryDescriptor.ReadLogResult var92;
      try {
         BIHistory dataHistory = this.getHistory(conn);
         this.reinitTimestamps(conn, dataHistory);
         BAbsTime firstTimestamp = conn.getFirstTimestamp(dataHistory);
         BAbsTime lastTimestamp = conn.getLastTimestamp(dataHistory);
         BAbsTime recTimestamp = null;
         long itemCount = 0L;
         boolean includesFirstItem = false;
         boolean includesLastItem = false;
         boolean moreItems = false;
         BTrendRecord r = null;
         synchronized (asnOut) {
            if (count >= 0) {
               asnOut.reset();
               long position = 1L;
               Cursor<BHistoryRecord> data = conn.scan(dataHistory);
               Throwable i = null;

               try {
                  for (; data.next() && itemCount < count; position++) {
                     if (position >= refIndex) {
                        r = (BTrendRecord)data.get();
                        recTimestamp = r.getTimestamp();
                        int logDatumType = getLogDatumType(r);
                        if (logDatumType >= 0) {
                           AsnOutputStream temp = new AsnOutputStream();

                           try {
                              asnOut.writeTo(temp);
                           } catch (Exception var78) {
                              log.log(
                                 Level.WARNING, "Error caching trend records during read by position, Bacnet Object ID " + this.getObjectId(), (Throwable)var78
                              );
                              temp = asnOut;
                           }

                           BBacnetLogRecord.writeLogRecord(
                              recTimestamp, (BSimple)r.get(r.getValueProperty()), logDatumType, r.getStatus(), getLogEvent(r).getLong(), asnOut
                           );
                           if (maxSize > 0 && asnOut.size() > maxSize) {
                              asnOut = temp;
                              moreItems = true;
                              break;
                           }

                           if (recTimestamp.equals(firstTimestamp)) {
                              includesFirstItem = true;
                           }

                           if (recTimestamp.equals(lastTimestamp)) {
                              includesLastItem = true;
                           }

                           itemCount++;
                        }
                     }
                  }
               } catch (Throwable var81) {
                  i = var81;
                  throw var81;
               } finally {
                  if (data != null) {
                     if (i != null) {
                        try {
                           data.close();
                        } catch (Throwable var76) {
                           i.addSuppressed(var76);
                        }
                     } else {
                        data.close();
                     }
                  }
               }
            } else {
               count = -count;
               ArrayList<BTrendRecord> records = new ArrayList<>();
               Cursor<BHistoryRecord> data = conn.scan(dataHistory);
               Throwable var95 = null;

               try {
                  while (data.next()) {
                     r = (BTrendRecord)data.get();
                     if (getLogDatumType(r) >= 0) {
                        records.add(r);
                     }
                  }
               } catch (Throwable var79) {
                  var95 = var79;
                  throw var79;
               } finally {
                  if (data != null) {
                     if (var95 != null) {
                        try {
                           data.close();
                        } catch (Throwable var75) {
                           var95.addSuppressed(var75);
                        }
                     } else {
                        data.close();
                     }
                  }
               }

               int numRecords = records.size();
               int startIndex = 0;
               if (numRecords > count) {
                  startIndex = numRecords - count;
                  numRecords = count;
               }

               asnOut.reset();

               for (int i = startIndex; i < numRecords; i++) {
                  r = records.get(i);
                  recTimestamp = r.getTimestamp();
                  AsnOutputStream temp = new AsnOutputStream();

                  try {
                     asnOut.writeTo(temp);
                  } catch (Exception var77) {
                     log.log(Level.WARNING, "Error caching trend records during read by position, Bacnet Object ID " + this.getObjectId(), (Throwable)var77);
                     temp = asnOut;
                  }

                  BBacnetLogRecord.writeLogRecord(
                     recTimestamp, (BSimple)r.get(r.getValueProperty()), getLogDatumType(r), r.getStatus(), getLogEvent(r).getLong(), asnOut
                  );
                  if (maxSize > 0 && asnOut.size() > maxSize) {
                     asnOut = temp;
                     moreItems = true;
                     break;
                  }

                  if (recTimestamp.equals(firstTimestamp)) {
                     includesFirstItem = true;
                  }

                  if (recTimestamp.equals(lastTimestamp)) {
                     includesLastItem = true;
                  }

                  itemCount++;
               }
            }
         }

         var92 = new BBacnetNiagaraHistoryDescriptor.ReadLogResult(itemCount, -1L, asnOut.toByteArray(), includesFirstItem, includesLastItem, moreItems);
      } catch (Throwable var84) {
         var6 = var84;
         throw var84;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var74) {
                  var6.addSuppressed(var74);
               }
            } else {
               conn.close();
            }
         }
      }

      return var92;
   }

   private BBacnetNiagaraHistoryDescriptor.ReadLogResult readRangeByTime(BBacnetDateTime refTime, int count, int maxSize) {
      BAbsTime referenceTime = refTime.toBAbsTime();
      HistoryDatabaseConnection conn = getHistoryDbConnection(null);
      Throwable var6 = null;

      BBacnetNiagaraHistoryDescriptor.ReadLogResult var87;
      try {
         BIHistory dataHistory = this.getHistory(conn);
         this.reinitTimestamps(conn, dataHistory);
         BAbsTime firstTimestamp = conn.getFirstTimestamp(dataHistory);
         BAbsTime lastTimestamp = conn.getLastTimestamp(dataHistory);
         long itemCount = 0L;
         long firstFoundSequenceNumber = 0L;
         boolean includesFirstItem = false;
         boolean includesLastItem = false;
         boolean moreItems = false;
         synchronized (asnOut) {
            if (count >= 0) {
               asnOut.reset();
               long loopCount = 0L;
               Cursor<BHistoryRecord> data = conn.timeQuery(dataHistory, referenceTime, null).cursor();
               Throwable startIndex = null;

               try {
                  for (BTrendRecord r = null; data.next(); loopCount++) {
                     if (count > itemCount) {
                        r = (BTrendRecord)data.get();
                        BBacnetDateTime recTime = new BBacnetDateTime(r.getTimestamp());
                        if (getLogDatumType(r) >= 0 && !recTime.toBAbsTime().equals(referenceTime)) {
                           AsnOutputStream temp = new AsnOutputStream();

                           try {
                              asnOut.writeTo(temp);
                           } catch (Exception var78) {
                              log.log(
                                 Level.WARNING, "Error caching trend records during read by time, Bacnet Object ID " + this.getObjectId(), (Throwable)var78
                              );
                              temp = asnOut;
                           }

                           BBacnetLogRecord.writeLogRecord(
                              r.getTimestamp(), (BSimple)r.get(r.getValueProperty()), getLogDatumType(r), r.getStatus(), getLogEvent(r).getLong(), asnOut
                           );
                           if (maxSize > 0 && asnOut.size() > maxSize) {
                              asnOut = temp;
                              moreItems = true;
                              break;
                           }

                           if (r.getTimestamp().equals(firstTimestamp)) {
                              includesFirstItem = true;
                           }

                           if (r.getTimestamp().equals(lastTimestamp)) {
                              includesLastItem = true;
                           }

                           if (itemCount == 0L) {
                              firstFoundSequenceNumber = loopCount + 1L;
                           }

                           itemCount++;
                        }
                     }
                  }
               } catch (Throwable var80) {
                  startIndex = var80;
                  throw var80;
               } finally {
                  if (data != null) {
                     if (startIndex != null) {
                        try {
                           data.close();
                        } catch (Throwable var77) {
                           startIndex.addSuppressed(var77);
                        }
                     } else {
                        data.close();
                     }
                  }
               }

               firstFoundSequenceNumber += this.getLastSeqNum() - loopCount;
            } else {
               ArrayList<Object> records = new ArrayList<>();
               int adjustedCount = count * -1;
               Cursor<BHistoryRecord> data = conn.timeQuery(dataHistory, null, referenceTime).cursor();
               Throwable var91 = null;

               try {
                  for (long loopCount = this.getFirstSeqNum(); data.next(); loopCount++) {
                     BBacnetDateTime recTime = new BBacnetDateTime(((BHistoryRecord)data.get()).getTimestamp());
                     if (getLogDatumType((BTrendRecord)data.get()) >= 0 && !recTime.toBAbsTime().equals(referenceTime)) {
                        records.add(data.get());
                        records.add(BLong.make(loopCount));
                     }
                  }
               } catch (Throwable var82) {
                  var91 = var82;
                  throw var82;
               } finally {
                  if (data != null) {
                     if (var91 != null) {
                        try {
                           data.close();
                        } catch (Throwable var76) {
                           var91.addSuppressed(var76);
                        }
                     } else {
                        data.close();
                     }
                  }
               }

               int numRecords = records.size() / 2;
               int startIndex = 0;
               if (numRecords > adjustedCount) {
                  startIndex = numRecords - adjustedCount;
                  numRecords = adjustedCount;
               }

               asnOut.reset();
               BTrendRecord rx = null;

               for (int i = startIndex; i < numRecords; i++) {
                  rx = (BTrendRecord)records.get(i * 2);
                  AsnOutputStream temp = new AsnOutputStream();

                  try {
                     asnOut.writeTo(temp);
                  } catch (Exception var79) {
                     log.log(Level.WARNING, "Error exporting trend records by time, Bacnet Object ID " + this.getObjectId(), (Throwable)var79);
                     temp = asnOut;
                  }

                  BBacnetLogRecord.writeLogRecord(
                     rx.getTimestamp(), (BSimple)rx.get(rx.getValueProperty()), getLogDatumType(rx), rx.getStatus(), getLogEvent(rx).getLong(), asnOut
                  );
                  if (maxSize > 0 && asnOut.size() > maxSize) {
                     asnOut = temp;
                     moreItems = true;
                     break;
                  }

                  if (rx.getTimestamp().equals(firstTimestamp)) {
                     includesFirstItem = true;
                  }

                  if (rx.getTimestamp().equals(lastTimestamp)) {
                     includesLastItem = true;
                  }

                  if (itemCount == 0L) {
                     firstFoundSequenceNumber = ((BLong)records.get(i * 2 + 1)).getLong();
                  }

                  itemCount++;
               }

               records.clear();
            }
         }

         var87 = new BBacnetNiagaraHistoryDescriptor.ReadLogResult(
            itemCount, firstFoundSequenceNumber, asnOut.toByteArray(), includesFirstItem, includesLastItem, moreItems
         );
      } catch (Throwable var85) {
         var6 = var85;
         throw var85;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var75) {
                  var6.addSuppressed(var75);
               }
            } else {
               conn.close();
            }
         }
      }

      return var87;
   }

   private static int getLogDatumType(BTrendRecord record) {
      if (record instanceof BBacnetTrendRecord) {
         return ((BBacnetTrendRecord)record).getLogDatumType();
      } else {
         BValue recordType = record.get(record.getValueProperty());
         if (recordType instanceof BBoolean) {
            return 1;
         } else if (recordType instanceof BDouble) {
            return 2;
         } else if (recordType instanceof BEnum) {
            return 3;
         } else if (recordType instanceof BBacnetUnsigned) {
            return 4;
         } else if (recordType instanceof BFloat) {
            return 2;
         } else if (recordType instanceof BInteger) {
            return 5;
         } else if (recordType instanceof BBacnetBitString) {
            return 6;
         } else if (recordType instanceof BBacnetNull) {
            return 7;
         } else {
            if (recordType instanceof BTrendEvent) {
               BTrendEvent evt = (BTrendEvent)recordType;
               if (evt.isLogStatus()) {
                  return 0;
               }

               if (evt.isFailure()) {
                  return 8;
               }

               if (evt.isTimeChange()) {
                  return 9;
               }
            } else if (recordType instanceof BString) {
               return 10;
            }

            return -1;
         }
      }
   }

   private static BTrendEvent getLogEvent(BTrendRecord record) {
      return record instanceof BBacnetTrendRecord ? ((BBacnetTrendRecord)record).getLogEvent() : BTrendEvent.DEFAULT;
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      ArrayList<PropertyValue> results = new ArrayList<>(refs.length);

      for (int i = 0; i < refs.length; i++) {
         switch (refs[i].getPropertyId()) {
            case 8:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }

               props = this.getOptionalProps();

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 80:
               int[] props = this.getOptionalProps();

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 105:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            default:
               results.add(this.readProperty(refs[i].getPropertyId(), refs[i].getPropertyArrayIndex()));
         }
      }

      return results.toArray(new PropertyValue[0]);
   }

   @Override
   public final RangeData readRange(RangeReference rangeReference) throws RejectException {
      int propertyId = rangeReference.getPropertyId();
      if (!this.hasProperty(propertyId)) {
         return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (propertyId != 131) {
         return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else if (rangeReference.getPropertyArrayIndex() != -1) {
         return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
      } else {
         int maxDataSize = -1;
         if (rangeReference instanceof BacnetConfirmedRequest) {
            maxDataSize = ((BacnetConfirmedRequest)rangeReference).getMaxDataLength();
         }

         int count = rangeReference.getCount();
         switch (rangeReference.getRangeType()) {
            case -1:
               try {
                  BBacnetNiagaraHistoryDescriptor.ReadLogResult rlr = this.readRangeAll(maxDataSize);
                  return new ReadRangeAck(
                     this.getObjectId(), propertyId, -1, rlr.getResultFlags(), rlr.itemCount, rlr.itemCount > 0L ? rlr.firstSequenceNumber : -1L, rlr.itemData
                  );
               } catch (Exception var11) {
                  logException(Level.SEVERE, this.getObjectId() + ": could not readRange all records", var11);
                  return new ReadRangeAck(2, 0);
               }
            case 0:
            case 1:
            case 2:
            case 4:
            case 5:
            default:
               log.info(this.getObjectId() + ": unsupported ReadRange Range Type: " + rangeReference.getRangeType());
               return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.parameterOutOfRange);
            case 3:
               long refIndex = rangeReference.getReferenceIndex();

               try {
                  BBacnetNiagaraHistoryDescriptor.ReadLogResult rlr = this.readRangeByPosition(refIndex, count, maxDataSize);
                  return new ReadRangeAck(
                     this.getObjectId(), propertyId, -1, rlr.getResultFlags(), rlr.itemCount, rlr.itemCount > 0L ? rlr.firstSequenceNumber : -1L, rlr.itemData
                  );
               } catch (Exception var10) {
                  logException(Level.SEVERE, this.getObjectId() + ": could not readRange by position", var10);
                  return new ReadRangeAck(2, 0);
               }
            case 6:
               logger.warning("BY_SEQUENCE_NUMBER is not supported for NiagaraHistoryDescriptor, transaction rejected");
               throw new RejectException(6);
            case 7:
               BBacnetDateTime refTime = rangeReference.getReferenceTime();

               try {
                  BBacnetNiagaraHistoryDescriptor.ReadLogResult rlr = this.readRangeByTime(refTime, count, maxDataSize);
                  return new ReadRangeAck(
                     this.getObjectId(), propertyId, -1, rlr.getResultFlags(), rlr.itemCount, rlr.itemCount > 0L ? rlr.firstSequenceNumber : -1L, rlr.itemData
                  );
               } catch (Exception var9) {
                  logException(Level.SEVERE, this.getObjectId() + ": could not readRange by time", var9);
                  return new ReadRangeAck(2, 0);
               }
         }
      }
   }

   private boolean hasProperty(int propertyId) {
      for (int id : REQUIRED_PROPS) {
         if (id == propertyId) {
            return true;
         }
      }

      for (int idx : this.getOptionalProps()) {
         if (idx == propertyId) {
            return true;
         }
      }

      return propertyId == 371;
   }

   @Override
   public final ErrorType writeProperty(PropertyValue val) throws BacnetException {
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      if (!this.hasProperty(propertyId)) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (propertyId != 131) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else {
         return propertyValue.getPropertyArrayIndex() != -1
            ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
            : BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
      }
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      if (!this.hasProperty(propertyId)) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (propertyId != 131) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else {
         return propertyValue.getPropertyArrayIndex() != -1
            ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
            : BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
      }
   }

   boolean isArray(int propId) {
      for (int arrayPropId : ARRAY_PROPS) {
         if (propId == arrayPropId) {
            return true;
         }
      }

      return false;
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (ndx >= 0) {
         if (!this.isArray(pId)) {
            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
         }
      } else if (ndx < -1) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 42));
      }

      switch (pId) {
         case 28:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
         case 36:
            return this.readEventState();
         case 75:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
         case 77:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
         case 79:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
         case 126:
            long maxRecords = 4294967295L;
            BIHistory hist = this.getHistory();
            if (hist.getConfig().getCapacity().isByRecordCount()) {
               maxRecords = hist.getConfig().getCapacity().getMaxRecords();
            } else if (hist.getConfig().getCapacity().isByStorageSize()) {
               maxRecords = hist.getConfig().getCapacity().getMaxStorage();
            }

            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(maxRecords));
         case 131:
            log.info("ReadProperty for logBuffer is not accessible except via ReadRange for NiagaraHistoryDescriptor");
            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 27));
         case 133:
            log.info("ReadProperty for enable is not accessible for NiagaraHistoryDescriptor");
            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 27));
         case 141:
            HistoryDatabaseConnection conn = getHistoryDbConnection(null);
            Throwable var7 = null;

            NReadPropertyResult var8;
            try {
               var8 = new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(conn.getRecordCount(this.getHistory(conn))));
            } catch (Throwable var17) {
               var7 = var17;
               throw var17;
            } finally {
               if (conn != null) {
                  if (var7 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var16) {
                        var7.addSuppressed(var16);
                     }
                  } else {
                     conn.close();
                  }
               }
            }

            return var8;
         case 144:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getHistory().getConfig().getFullPolicy().equals(BFullPolicy.stop)));
         case 145:
            return this.readTotalRecordCount();
         default:
            return this.readOptionalProperty(pId, ndx);
      }
   }

   private PropertyValue readTotalRecordCount() {
      HistoryDatabaseConnection conn = getHistoryDbConnection(null);
      Throwable var2 = null;

      NReadPropertyResult var4;
      try {
         BIHistory history = this.getHistory(conn);
         this.reinitTimestamps(conn, history);
         var4 = new NReadPropertyResult(145, -1, AsnUtil.toAsnUnsigned(this.getLastSeqNum()));
      } catch (Throwable var13) {
         var2 = var13;
         throw var13;
      } finally {
         if (conn != null) {
            if (var2 != null) {
               try {
                  conn.close();
               } catch (Throwable var12) {
                  var2.addSuppressed(var12);
               }
            } else {
               conn.close();
            }
         }
      }

      return var4;
   }

   private PropertyValue readEventState() {
      if (!this.getEventDetectionEnable()) {
         return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(0));
      } else {
         BBacnetTrendLogAlarmSourceExt alarmExt = this.getAlarmExt();
         return alarmExt == null
            ? new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(0))
            : new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(BBacnetEventState.fromBAlarmState(alarmExt.getAlarmState())));
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      if (ndx >= 0) {
         if (!this.isArray(pId)) {
            return new NErrorType(2, 50);
         }
      } else if (ndx < -1) {
         return new NErrorType(2, 42);
      }

      try {
         switch (pId) {
            case 28:
               this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
               return null;
            case 36:
            case 75:
            case 79:
            case 126:
            case 131:
            case 133:
            case 141:
            case 144:
            case 145:
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(this.getObjectId() + ": attempted to write read-only property " + BBacnetPropertyIdentifier.tag(pId));
               }

               return new NErrorType(2, 40);
            case 77:
               return BacUtil.setObjectName(this, objectName, val);
            default:
               return this.writeOptionalProperty(pId, ndx, val, pri);
         }
      } catch (AsnException var6) {
         logException(Level.INFO, this.getObjectId() + ": AsnException writing property " + BBacnetPropertyIdentifier.tag(pId), var6);
         return new NErrorType(2, 9);
      } catch (PermissionException var7) {
         logException(Level.INFO, this.getObjectId() + ": PermissionException writing property " + BBacnetPropertyIdentifier.tag(pId), var7);
         return new NErrorType(2, 40);
      }
   }

   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         switch (pId) {
            case 0:
               return this.readAckedTransitions(almExt.getAckedTransitions());
            case 17:
               BBacnetNotificationClassDescriptor nc = this.getNotificationClass();
               if (nc == null) {
                  return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(nc.getNotificationClass()));
            case 35:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAlarmEnable())));
            case 72:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(almExt.getNotifyType()));
            case 130:
               return this.readEventTimeStamps(
                  almExt.getToOffnormalTimes().getAlarmTime(), almExt.getToFaultTimes().getAlarmTime(), almExt.getToNormalTimes().getAlarmTime(), ndx
               );
            case 137:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getNotificationThreshold()));
            case 140:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getRecordsSinceNotification()));
            case 173:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getLastNotifyRecord()));
            case 351:
               return this.readEventMessageTexts(ndx);
            case 353:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getEventDetectionEnable()));
         }
      }

      if (pId == 134) {
         BCollectionInterval collInt = this.getHistory().getConfig().getInterval();
         long logInt = collInt.isIrregular() ? 0L : collInt.getInterval().getMillis() / 10L;
         return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(logInt));
      } else {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
      }
   }

   private NReadPropertyResult readAckedTransitions(BAlarmTransitionBits ackedTrans) {
      if (this.getEventDetectionEnable()) {
         BAlarmTransitionBits eventTrans = this.readEventTransition(ackedTrans);
         return new NReadPropertyResult(0, -1, AsnUtil.toAsnBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(eventTrans)));
      } else {
         return new NReadPropertyResult(0, -1, AsnUtil.toAsnBitString(ACKED_TRANS_DEFAULT));
      }
   }

   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      try {
         BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
         if (almExt != null) {
            switch (pId) {
               case 0:
               case 35:
               case 130:
               case 137:
               case 140:
               case 173:
               case 351:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(this.getObjectId() + ": attempted to write read-only property " + BBacnetPropertyIdentifier.tag(pId));
                  }

                  return new NErrorType(2, 40);
               case 17:
                  int ncinst = AsnUtil.fromAsnUnsignedInt(val);
                  if (ncinst > 4194302) {
                     return new NErrorType(2, 37);
                  }

                  BBacnetObjectIdentifier ncid = BBacnetObjectIdentifier.make(15, ncinst);
                  BBacnetNotificationClassDescriptor nc = (BBacnetNotificationClassDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(ncid);
                  if (nc == null) {
                     return new NErrorType(2, 37);
                  }

                  BAlarmClass ac = nc.getAlarmClass();
                  almExt.setString(BBacnetTrendLogAlarmSourceExt.alarmClass, ac.getName(), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 72:
                  this.set(
                     BBacnetTrendLogAlarmSourceExt.notifyType, BBacnetNotifyType.make(AsnUtil.fromAsnEnumerated(val)), BLocalBacnetDevice.getBacnetContext()
                  );
                  return null;
               case 353:
                  this.setBoolean(eventDetectionEnable, AsnUtil.fromAsnBoolean(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
            }
         }

         if (pId == 134) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": attempted to write read-only property " + BBacnetPropertyIdentifier.tag(pId));
            }

            return new NErrorType(2, 40);
         }
      } catch (AsnException var10) {
         logException(Level.INFO, this.getObjectId() + ": AsnException writing property " + BBacnetPropertyIdentifier.tag(pId), var10);
         return new NErrorType(2, 9);
      } catch (PermissionException var11) {
         logException(Level.INFO, this.getObjectId() + ": PermissionException writing property " + BBacnetPropertyIdentifier.tag(pId), var11);
         return new NErrorType(2, 40);
      } catch (IllegalArgumentException var12) {
         logException(Level.INFO, this.getObjectId() + ": IllegalArgumentException writing property " + BBacnetPropertyIdentifier.tag(pId), var12);
         return new NErrorType(2, 37);
      }

      return new NErrorType(2, 32);
   }

   private int[] getOptionalProps() {
      ArrayList<BBacnetPropertyIdentifier> v = new ArrayList<>();
      v.add(BBacnetPropertyIdentifier.description);
      v.add(BBacnetPropertyIdentifier.logInterval);
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         v.add(BBacnetPropertyIdentifier.notificationThreshold);
         v.add(BBacnetPropertyIdentifier.recordsSinceNotification);
         v.add(BBacnetPropertyIdentifier.lastNotifyRecord);
         v.add(BBacnetPropertyIdentifier.notificationClass);
         v.add(BBacnetPropertyIdentifier.eventEnable);
         v.add(BBacnetPropertyIdentifier.ackedTransitions);
         v.add(BBacnetPropertyIdentifier.notifyType);
         v.add(BBacnetPropertyIdentifier.eventTimeStamps);
         v.add(BBacnetPropertyIdentifier.eventMessageTexts);
         v.add(BBacnetPropertyIdentifier.eventDetectionEnable);
      }

      this.optionalProps = new int[v.size()];

      for (int i = 0; i < this.optionalProps.length; i++) {
         this.optionalProps[i] = ((BEnum)v.get(i)).getOrdinal();
      }

      return this.optionalProps;
   }

   private static void logException(Level level, String message, Exception e) {
      if (logger.isLoggable(Level.FINE)) {
         logger.log(level, message + "; exception: " + e.getLocalizedMessage(), (Throwable)e);
      } else if (logger.isLoggable(level)) {
         logger.log(level, message + "; exception: " + e.getLocalizedMessage());
      }
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetNiagaraHistoryDescriptor", 2);
      out.prop("history", this.getHistory());
      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("duplicate", this.duplicate);
      out.endProps();
   }

   public BIcon getIcon() {
      return icon;
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, this.getOptionalProps());
   }

   static class ReadLogResult implements RangeData {
      long itemCount;
      long firstSequenceNumber;
      byte[] itemData;
      boolean includeFirst;
      boolean includeLast;
      boolean moreItems;

      ReadLogResult(long ic, long fsn, byte[] id, boolean inclFirst, boolean inclLast, boolean more) {
         this.itemCount = ic;
         this.firstSequenceNumber = fsn;
         this.itemData = id;
         this.includeFirst = inclFirst;
         this.includeLast = inclLast;
         this.moreItems = more;
      }

      @Override
      public BBacnetBitString getResultFlags() {
         return BBacnetBitString.make(new boolean[]{this.includeFirst, this.includeLast, this.moreItems});
      }

      @Override
      public boolean includesFirstItem() {
         return this.includeFirst;
      }

      @Override
      public boolean includesLastItem() {
         return this.includeLast;
      }

      @Override
      public boolean isMoreItems() {
         return this.moreItems;
      }

      @Override
      public long getItemCount() {
         return this.itemCount;
      }

      @Override
      public long getFirstSequenceNumber() {
         return this.firstSequenceNumber;
      }

      @Override
      public byte[] getItemData() {
         return this.itemData;
      }

      @Override
      public ErrorType getError() {
         return null;
      }

      @Override
      public int getErrorClass() {
         return -1;
      }

      @Override
      public int getErrorCode() {
         return -1;
      }

      @Override
      public boolean isError() {
         return false;
      }

      @Override
      public int getPropertyId() {
         return 131;
      }

      @Override
      public int getPropertyArrayIndex() {
         return -1;
      }

      @Override
      public void writeAsn(AsnOutput out) {
      }

      @Override
      public void readAsn(AsnInput in) {
      }
   }
}
