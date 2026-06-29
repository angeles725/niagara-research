package com.tridium.bacnet.history;

import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.datatypes.BTrendEvent;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.stack.BBacnetStack;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmService;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetLogRecord;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BBacnetNotificationClassDescriptor;
import javax.baja.bacnet.export.BBacnetTrendLogDescriptor;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.export.BOutOfServiceExt;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.control.BPointExtension;
import javax.baja.history.BFullPolicy;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.BTrendFlags;
import javax.baja.history.BTrendRecord;
import javax.baja.history.HistoryException;
import javax.baja.history.HistoryNotFoundException;
import javax.baja.history.HistoryQuery;
import javax.baja.history.HistorySpaceConnection;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Cursor;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.util.BTypeSpec;

public class BacnetTrendLogUtil implements BacnetConst {
   protected static AsnOutputStream asnOut = new AsnOutputStream();
   private static final Logger logger = Logger.getLogger("bacnet.history");
   public static long MAX_SEQ_NUM = 4294967295L;
   private static final RangeData EMPTY_RESULT = new BacnetTrendLogUtil.ReadLogResult(0L, -1L, new byte[0], false, false, false);

   public static void initHistoryExt(BHistoryExt ext) throws Exception {
      BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = localDevice.lookupBacnetObjectId(ext.getHandleOrd());
      if (objectId == null) {
         BBacnetTrendLogDescriptor descriptor = new BBacnetTrendLogDescriptor();
         int nextInstanceNum = localDevice.getNextInstance(20);
         objectId = BBacnetObjectIdentifier.make(20, nextInstanceNum);
         descriptor.setObjectId(objectId);
         descriptor.setObjectName(SlotPath.unescape(ext.getHistoryName().format(ext)));
         descriptor.setObjectOrd(ext.getHandleOrd(), null);
         String exportName = objectId.toString(BacnetConst.nameContext);
         localDevice.getExportTable().add(exportName, descriptor);
      }
   }

   public static final long incrementSequenceNumber(long currentSeqNum) {
      if (currentSeqNum < 0L) {
         return 1L;
      } else {
         if (++currentSeqNum > MAX_SEQ_NUM) {
            currentSeqNum = 1L;
         }

         return currentSeqNum;
      }
   }

   public static BOutOfServiceExt getOosExt(BHistoryExt hext) {
      BPointExtension[] pexts = hext.getParentPoint().getExtensions();

      for (int i = 0; i < pexts.length; i++) {
         if (pexts[i] instanceof BOutOfServiceExt) {
            return (BOutOfServiceExt)pexts[i];
         }
      }

      return null;
   }

   public static BIHistory getHistory(BIBacnetTrendLogExt ext) {
      BIHistory history = ext.getHistory();
      if (history == null) {
         HistorySpaceConnection conn = getHistoryDbConnection(null);
         Throwable var3 = null;

         try {
            history = getHistory(conn, ext);
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
      }

      return history;
   }

   protected static BIHistory getHistory(HistorySpaceConnection conn, BIBacnetTrendLogExt ext) {
      BIHistory history = ext.getHistory();
      if (history == null) {
         BHistoryConfig config = ext.getHistoryConfig();
         BHistoryId id = config.getId();
         if (conn.exists(id)) {
            history = conn.getHistory(id);
         }
      }

      return history;
   }

   protected static HistorySpaceConnection getHistoryDbConnection(Context cx) {
      BHistoryService service = (BHistoryService)Sys.getService(BHistoryService.TYPE);
      BHistoryDatabase db = service.getDatabase();
      return db.getConnection(HistoryQuery.makeExcludeArchiveDataContext(cx));
   }

   public static BTypeSpec findHistoryTypeByRecords(BBacnetDevice device, BBacnetObjectIdentifier objectId) throws Exception {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("findHistoryTypeByRecords for " + objectId);
      }

      BBacnetLogRecord v = new BBacnetLogRecord();
      BTypeSpec recType = null;
      long itemCount = 1L;
      int position = 1;

      while (recType == null && itemCount > 0L) {
         ReadRangeAck ack = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
            .getClient()
            .readRange(device.getAddress(), objectId, 131, -1, 3, position++, null, 1);
         itemCount = ack.getItemCount();
         if (itemCount > 0L) {
            try {
               AsnUtil.fromAsn(ack.getItemData(), v);
               recType = v.getNiagaraRecordType();
            } catch (AsnException var9) {
               logger.info("Unable to convert ASN-encoded BACnetLogRecord (" + ByteArrayUtil.toHexString(ack.getItemData()) + ") for object " + objectId);
            }
         }
      }

      return recType;
   }

   public static long getMaxRecords(BIBacnetTrendLogExt tlog) {
      BHistoryConfig config = tlog.getHistoryConfig();
      long maxRecords = 4294967295L;
      if (config.getCapacity().isByRecordCount()) {
         maxRecords = config.getCapacity().getMaxRecords();
      } else if (config.getCapacity().isByStorageSize()) {
         long maxBytes = config.getCapacity().getMaxStorage();
         int bytesPerRecord = 1;

         try {
            bytesPerRecord = config.getRecordSize();
         } catch (Exception var8) {
            logger.info("Unable to determine bytes per record for BacnetTrendLogExt " + tlog);
         }

         maxRecords = maxBytes / bytesPerRecord;
      }

      return maxRecords;
   }

   public static BBacnetTrendLogAlarmSourceExt getAlarmExt(BIBacnetTrendLogExt ext) {
      if (ext == null) {
         return null;
      } else {
         SlotCursor<Property> c = ((BComplex)ext).getProperties();
         Throwable var2 = null;

         BBacnetTrendLogAlarmSourceExt var3;
         try {
            if (!c.next(BBacnetTrendLogAlarmSourceExt.class)) {
               return null;
            }

            var3 = (BBacnetTrendLogAlarmSourceExt)c.get();
         } catch (Throwable var13) {
            var2 = var13;
            throw var13;
         } finally {
            if (c != null) {
               if (var2 != null) {
                  try {
                     c.close();
                  } catch (Throwable var12) {
                     var2.addSuppressed(var12);
                  }
               } else {
                  c.close();
               }
            }
         }

         return var3;
      }
   }

   public static BBacnetNotificationClassDescriptor getNotificationClass(BIBacnetTrendLogExt ext) {
      BBacnetTrendLogAlarmSourceExt almExt = getAlarmExt(ext);
      if (almExt == null) {
         return null;
      } else {
         try {
            BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
            BAlarmClass ac = as.lookupAlarmClass(almExt.getAlarmClass());
            BBacnetObjectIdentifier objectId = BBacnetNetwork.localDevice().lookupBacnetObjectId(ac.getHandleOrd());
            if (objectId != null) {
               return (BBacnetNotificationClassDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(objectId);
            }
         } catch (ServiceNotFoundException var5) {
            logger.log(Level.INFO, "getNotificationClass on " + ext + ":Unable to find alarm service!", (Throwable)var5);
         }

         return null;
      }
   }

   static NErrorType getFailureError(BIBacnetTrendLogExt ext) {
      SlotCursor<Property> c = ((BComplex)ext).getParent().getProperties();
      Throwable var2 = null;

      try {
         if (c.next(BBacnetProxyExt.class)) {
            ErrorType extError = ((BBacnetProxyExt)c.get()).getLastReadError();
            if (extError != null) {
               return new NErrorType(extError.getErrorClass(), extError.getErrorCode());
            }
         }

         return null;
      } catch (Throwable var14) {
         var2 = var14;
         throw var14;
      } finally {
         if (c != null) {
            if (var2 != null) {
               try {
                  c.close();
               } catch (Throwable var13) {
                  var2.addSuppressed(var13);
               }
            } else {
               c.close();
            }
         }
      }
   }

   public static void writeEvent(BIBacnetTrendLogExt ext, BAbsTime timestamp, BStatus status, long sequenceNumber, BTrendEvent event) throws IOException {
      BHistoryExt hext = (BHistoryExt)ext;
      BBacnetTrendRecord rec = ext.getRecord();
      BTrendFlags tf = rec.getTrendFlags();
      BBacnetTrendLogAlarmSourceExt almExt = getAlarmExt(ext);

      try {
         HistorySpaceConnection conn = getHistoryDbConnection(null);
         Throwable var11 = null;

         try {
            BIHistory history = getHistory(conn, ext);
            if (history != null && conn.getRecordCount(history) != 0) {
               rec.set(timestamp, status, sequenceNumber, event, tf.set(4, true));
               if (!event.isLogDisabled() && !event.isTimeChange()) {
                  appendRecord(ext, rec);
               } else {
                  extAppend(ext, rec);
               }
            } else if (event.isLogStatus()) {
               BBacnetBitString logStatus = event.getLogStatus();
               BTrendEvent trendEvent = BTrendEvent.makeLogStatus(BBacnetBitString.make(logStatus, 1, true));
               rec.set(timestamp, status, sequenceNumber, trendEvent, tf.set(4, true));
               if (trendEvent.isLogDisabled()) {
                  extAppend(ext, rec);
               } else {
                  appendRecord(ext, rec);
               }
            } else {
               BTrendEvent evt = ext.getEnabled() ? BTrendEvent.LOG_STATUS_ENABLED_BUFFER_PURGED : BTrendEvent.LOG_STATUS_DISABLED_BUFFER_PURGED;
               BAbsTime timestampMinusOne = timestamp.subtract(BRelTime.make(1L));
               rec.set(timestampMinusOne, status, sequenceNumber, evt, tf.set(4, true));
               if (evt.isLogDisabled()) {
                  extAppend(ext, rec);
               } else {
                  appendRecord(ext, rec);
               }

               if (!hext.getStatus().isFault()) {
                  ext.setTotalRecordCount(sequenceNumber);
                  if (almExt != null) {
                     almExt.incrementRecordsSinceNotification();
                  }
               }

               sequenceNumber = incrementSequenceNumber(sequenceNumber);
               rec.set(timestamp, status, sequenceNumber, event, tf.set(4, true));
               appendRecord(ext, rec);
            }

            if (!hext.getStatus().isFault()) {
               ext.setTotalRecordCount(sequenceNumber);
               if (almExt != null) {
                  almExt.incrementRecordsSinceNotification();
               }

               checkForBufferFull(ext);
            }
         } catch (Throwable var29) {
            var11 = var29;
            throw var29;
         } finally {
            if (conn != null) {
               if (var11 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var28) {
                     var11.addSuppressed(var28);
                  }
               } else {
                  conn.close();
               }
            }
         }
      } finally {
         rec.setTrendFlags(tf);
      }
   }

   public static void writeRecord(BIBacnetTrendLogExt ext, BAbsTime timestamp, BStatusValue out) throws IOException {
      if (ext.getEnabled()) {
         BHistoryExt hext = (BHistoryExt)ext;
         BBacnetTrendRecord rec = ext.getRecord();
         BTrendFlags tf = rec.getTrendFlags();
         BBacnetTrendLogAlarmSourceExt almExt = getAlarmExt(ext);
         long sequenceNumber = incrementSequenceNumber(ext.getTotalRecordCount());
         boolean bufferPurged = false;

         try {
            HistorySpaceConnection conn = getHistoryDbConnection(null);
            Throwable errorFound = null;

            try {
               BIHistory history = getHistory(conn, ext);
               if (history == null || conn.getRecordCount(history) == 0) {
                  bufferPurged = true;
               }
            } catch (Throwable var21) {
               errorFound = var21;
               throw var21;
            } finally {
               if (conn != null) {
                  if (errorFound != null) {
                     try {
                        conn.close();
                     } catch (Throwable var20) {
                        errorFound.addSuppressed(var20);
                     }
                  } else {
                     conn.close();
                  }
               }
            }
         } catch (HistoryException var23) {
            bufferPurged = true;
         }

         if (bufferPurged) {
            BAbsTime timestampMinusOne = timestamp.subtract(BRelTime.make(1L));
            appendRecord(ext, rec.set(timestampMinusOne, out.getStatus(), sequenceNumber, BTrendEvent.LOG_STATUS_ENABLED_BUFFER_PURGED, tf.set(4, true)));
            if (!hext.getStatus().isFault()) {
               ext.setTotalRecordCount(sequenceNumber);
               if (almExt != null) {
                  almExt.incrementRecordsSinceNotification();
               }
            }
         }

         BTrendEvent event = BTrendEvent.DEFAULT;
         NErrorType errorFound = getFailureError(ext);
         if (errorFound != null) {
            event = BTrendEvent.makeFailure(errorFound);
            rec.setTrendFlags(tf.set(4, true));
         }

         sequenceNumber = incrementSequenceNumber(ext.getTotalRecordCount());
         rec.set(timestamp, out, sequenceNumber, event, BTrendFlags.DEFAULT);
         appendRecord(ext, rec);
         if (!hext.getStatus().isFault() && almExt != null) {
            almExt.incrementRecordsSinceNotification();
         }

         ext.setTotalRecordCount(sequenceNumber);
         checkForBufferFull(ext);
      }
   }

   private static void checkForBufferFull(BIBacnetTrendLogExt ext) {
      long bufferSize = getMaxRecords(ext);
      HistorySpaceConnection conn = getHistoryDbConnection(null);
      Throwable var4 = null;

      try {
         BIHistory history = getHistory(conn, ext);
         if (history == null) {
            return;
         }

         int recordCount = conn.getRecordCount(history);
         if (recordCount == bufferSize - 1L && ext.getHistoryConfig().getFullPolicy() == BFullPolicy.stop) {
            ((BHistoryExt)ext).setEnabled(false);
         }
      } catch (Throwable var15) {
         var4 = var15;
         throw var15;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var14) {
                  var4.addSuppressed(var14);
               }
            } else {
               conn.close();
            }
         }
      }
   }

   private static void appendRecord(BIBacnetTrendLogExt ext, BTrendRecord rec) throws IOException {
      HistorySpaceConnection conn = getHistoryDbConnection(null);
      Throwable var3 = null;

      try {
         BIHistory history = getHistory(conn, ext);
         Cursor<BHistoryRecord> c = conn.timeQuery(history, conn.getLastTimestamp(history), conn.getLastTimestamp(history)).cursor();
         Throwable var7 = null;

         BBacnetTrendRecord lastRecord;
         try {
            c.next();
            lastRecord = (BBacnetTrendRecord)c.get();
         } catch (Throwable var30) {
            var7 = var30;
            throw var30;
         } finally {
            if (c != null) {
               if (var7 != null) {
                  try {
                     c.close();
                  } catch (Throwable var29) {
                     var7.addSuppressed(var29);
                  }
               } else {
                  c.close();
               }
            }
         }

         if (lastRecord.getLogEvent().equals(BTrendEvent.DEFAULT)) {
            ext.append(rec);
         } else {
            extAppend(ext, rec);
         }
      } catch (Throwable var32) {
         var3 = var32;
         throw var32;
      } finally {
         if (conn != null) {
            if (var3 != null) {
               try {
                  conn.close();
               } catch (Throwable var28) {
                  var3.addSuppressed(var28);
               }
            } else {
               conn.close();
            }
         }
      }
   }

   private static void extAppend(BIBacnetTrendLogExt ext, BTrendRecord record) {
      BHistoryExt historyExt = (BHistoryExt)ext;
      HistorySpaceConnection conn = getHistoryDbConnection(null);
      Throwable var4 = null;

      try {
         BIHistory history = getHistory(conn, ext);
         if (historyExt.isRunning() && Sys.atSteadyState()) {
            try {
               synchronized (ext) {
                  if (history != null) {
                     conn.append(history, record);
                  }
               }

               if (history != null) {
                  historyExt.updateStatus();
               }
            } catch (HistoryNotFoundException var19) {
               logger.info("Unable to append record to history: record=" + record + " history=" + ext);
            } catch (Exception var20) {
               logger.info("Unable to append record to history: record=" + record + " history=" + ext);
            }
         }
      } catch (Throwable var21) {
         var4 = var21;
         throw var21;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var17) {
                  var4.addSuppressed(var17);
               }
            } else {
               conn.close();
            }
         }
      }
   }

   private static int getLogDatumChoice(BBacnetTrendRecord r, Integer pointAsnType) {
      switch (r.getLogDatumType()) {
         case 0:
         case 6:
         case 8:
         case 9:
            return r.getLogDatumType();
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 7:
         default:
            if (pointAsnType == null) {
               return r.getLogDatumType();
            } else {
               switch (pointAsnType) {
                  case -2:
                     if (r instanceof BBacnetBooleanTrendRecord) {
                        return 3;
                     } else if (r instanceof BBacnetEnumTrendRecord) {
                        return 4;
                     } else {
                        if (r instanceof BBacnetNumericTrendRecord) {
                           return 2;
                        }

                        return r.getLogDatumType();
                     }
                  case -1:
                  case 5:
                  case 6:
                  case 7:
                  case 10:
                  case 11:
                  case 12:
                  default:
                     return 10;
                  case 0:
                     return 7;
                  case 1:
                     return 1;
                  case 2:
                     return 4;
                  case 3:
                     return 5;
                  case 4:
                     return 2;
                  case 8:
                     return 6;
                  case 9:
                     return 3;
               }
            }
      }
   }

   public static RangeData readRangeAll(BIBacnetTrendLogExt ext, int maxSize, Integer pointAsnType) {
      HistorySpaceConnection conn = getHistoryDbConnection(null);
      Throwable var4 = null;

      RangeData var40;
      try {
         BIHistory dataHistory = getHistory(conn, ext);
         if (dataHistory == null || conn.getRecordCount(dataHistory) == 0) {
            return EMPTY_RESULT;
         }

         BAbsTime firstTimestamp = conn.getFirstTimestamp(dataHistory);
         long firstPossibleSeqNum = 1L;
         int recordCount = conn.getRecordCount(dataHistory);
         Cursor<BHistoryRecord> data = conn.timeQuery(dataHistory, firstTimestamp, firstTimestamp).cursor();
         Throwable var11 = null;

         try {
            if (data.next()) {
               firstPossibleSeqNum = ((BBacnetTrendRecord)data.get()).getSequenceNumber();
            }
         } catch (Throwable var35) {
            var11 = var35;
            throw var35;
         } finally {
            if (data != null) {
               if (var11 != null) {
                  try {
                     data.close();
                  } catch (Throwable var34) {
                     var11.addSuppressed(var34);
                  }
               } else {
                  data.close();
               }
            }
         }

         var40 = readRangeBySequence(ext, firstPossibleSeqNum, recordCount, maxSize, pointAsnType);
      } catch (Throwable var37) {
         var4 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var4 != null) {
               try {
                  conn.close();
               } catch (Throwable var33) {
                  var4.addSuppressed(var33);
               }
            } else {
               conn.close();
            }
         }
      }

      return var40;
   }

   public static RangeData readRangeByPosition(BIBacnetTrendLogExt ext, long position, int count, int maxSize, Integer pointAsnType) {
      HistorySpaceConnection conn = getHistoryDbConnection(null);
      Throwable var7 = null;

      RangeData var17;
      try {
         BIHistory dataHistory = getHistory(conn, ext);
         if (dataHistory == null || conn.getRecordCount(dataHistory) == 0) {
            return EMPTY_RESULT;
         }

         long recordCount = conn.getRecordCount(dataHistory);
         long lastUsedSeqNum = ext.getTotalRecordCount();
         long firstUsedSeqNum = lastUsedSeqNum - recordCount + 1L;
         if (firstUsedSeqNum < 1L) {
            firstUsedSeqNum += MAX_SEQ_NUM;
         }

         long seqNumAtPosition = firstUsedSeqNum + position - 1L;
         var17 = readRangeBySequence(ext, seqNumAtPosition, count, maxSize, pointAsnType);
      } catch (Throwable var27) {
         var7 = var27;
         throw var27;
      } finally {
         if (conn != null) {
            if (var7 != null) {
               try {
                  conn.close();
               } catch (Throwable var26) {
                  var7.addSuppressed(var26);
               }
            } else {
               conn.close();
            }
         }
      }

      return var17;
   }

   public static RangeData readRangeByTime(BIBacnetTrendLogExt ext, BBacnetDateTime refTime, int count, int maxSize, Integer pointAsnType) {
      HistorySpaceConnection conn = getHistoryDbConnection(null);
      Throwable var6 = null;

      try {
         BIHistory dataHistory = getHistory(conn, ext);
         if (dataHistory == null || conn.getRecordCount(dataHistory) == 0) {
            return EMPTY_RESULT;
         } else {
            BAbsTime referenceTime = refTime.toBAbsTime();
            long seqNum = 1L;
            if (count >= 0) {
               Cursor<BHistoryRecord> data = conn.timeQuery(dataHistory, referenceTime, null).cursor();
               Throwable var74 = null;

               try {
                  BAbsTime firstDataTimestamp = null;

                  while (data.next()) {
                     BBacnetDateTime recTime = new BBacnetDateTime(((BHistoryRecord)data.get()).getTimestamp());
                     if (!recTime.toBAbsTime().equals(referenceTime)) {
                        firstDataTimestamp = ((BHistoryRecord)data.get()).getTimestamp();
                        break;
                     }
                  }

                  if (firstDataTimestamp == null) {
                     return EMPTY_RESULT;
                  }

                  seqNum = ((BBacnetTrendRecord)data.get()).getSequenceNumber();
               } catch (Throwable var64) {
                  var74 = var64;
                  throw var64;
               } finally {
                  if (data != null) {
                     if (var74 != null) {
                        try {
                           data.close();
                        } catch (Throwable var63) {
                           var74.addSuppressed(var63);
                        }
                     } else {
                        data.close();
                     }
                  }
               }
            } else {
               BAbsTime lastDataTimestamp = null;
               BBacnetTrendRecord lastDataRecord = null;
               Cursor<BHistoryRecord> data = conn.timeQuery(dataHistory, null, referenceTime).cursor();
               Throwable recTime = null;

               try {
                  while (data.next()) {
                     BBacnetDateTime recTimex = new BBacnetDateTime(((BHistoryRecord)data.get()).getTimestamp());
                     if (!recTimex.toBAbsTime().equals(referenceTime)) {
                        lastDataRecord = (BBacnetTrendRecord)data.get();
                     }
                  }
               } catch (Throwable var66) {
                  recTime = var66;
                  throw var66;
               } finally {
                  if (data != null) {
                     if (recTime != null) {
                        try {
                           data.close();
                        } catch (Throwable var62) {
                           recTime.addSuppressed(var62);
                        }
                     } else {
                        data.close();
                     }
                  }
               }

               if (lastDataRecord == null) {
                  return EMPTY_RESULT;
               }

               lastDataTimestamp = lastDataRecord.getTimestamp();
               if (lastDataTimestamp != null) {
                  seqNum = lastDataRecord.getSequenceNumber();
               }
            }

            return readRangeBySequence(ext, seqNum, count, maxSize, pointAsnType);
         }
      } catch (Throwable var68) {
         var6 = var68;
         throw var68;
      } finally {
         if (conn != null) {
            if (var6 != null) {
               try {
                  conn.close();
               } catch (Throwable var61) {
                  var6.addSuppressed(var61);
               }
            } else {
               conn.close();
            }
         }
      }
   }

   public static RangeData readRangeBySequence(BIBacnetTrendLogExt ext, long refSeqNum, int count, int maxSize, Integer pointAsnType) {
      if (count == 0) {
         return EMPTY_RESULT;
      } else {
         HistorySpaceConnection conn = getHistoryDbConnection(null);
         Throwable var7 = null;

         RangeData rec;
         try {
            BIHistory dataHistory = getHistory(conn, ext);
            if (dataHistory == null || conn.getRecordCount(dataHistory) == 0) {
               return EMPTY_RESULT;
            }

            int itemCount = 0;
            int itemLimit = Math.abs(count);
            boolean includesFirst = false;
            boolean includesLast = false;
            boolean moreItems = false;
            if (count > 0) {
               Cursor<BHistoryRecord> data = conn.scan(dataHistory, false);
               Throwable var94 = null;

               try {
                  data.next();
                  BBacnetTrendRecord firstRec = (BBacnetTrendRecord)data.get();
                  long firstRecSeqNum = firstRec.getSequenceNumber();
                  if (firstRecSeqNum > refSeqNum + count) {
                     return EMPTY_RESULT;
                  }

                  long seqNum = findFirstRequestedRecord(data, refSeqNum);
                  if (seqNum == firstRecSeqNum) {
                     includesFirst = true;
                  }

                  if (seqNum < 0L) {
                     return EMPTY_RESULT;
                  }

                  BBacnetTrendRecord recx = (BBacnetTrendRecord)data.get();
                  ByteArrayOutputStream itemData = new ByteArrayOutputStream();
                  AsnOutputStream out = new AsnOutputStream();

                  do {
                     out.reset();
                     writeLogRecord(recx, pointAsnType, out);
                     if (exceedsMaxSize(maxSize, itemData, out)) {
                        moreItems = true;
                        break;
                     }

                     appendToItemData(itemData, out);
                     itemCount++;
                     if (!data.next()) {
                        includesLast = true;
                        break;
                     }

                     recx = (BBacnetTrendRecord)data.get();
                  } while (itemCount < itemLimit);

                  return new BacnetTrendLogUtil.ReadLogResult(itemCount, seqNum, itemData.toByteArray(), includesFirst, includesLast, moreItems);
               } catch (Throwable var86) {
                  var94 = var86;
                  throw var86;
               } finally {
                  if (data != null) {
                     if (var94 != null) {
                        try {
                           data.close();
                        } catch (Throwable var85) {
                           var94.addSuppressed(var85);
                        }
                     } else {
                        data.close();
                     }
                  }
               }
            }

            Cursor<BHistoryRecord> data = conn.scan(dataHistory, true);
            Throwable var15 = null;

            try {
               data.next();
               BBacnetTrendRecord lastRec = (BBacnetTrendRecord)data.get();
               long lastRecSeqNum = lastRec.getSequenceNumber();
               if (lastRecSeqNum < refSeqNum + count) {
                  return EMPTY_RESULT;
               }

               long seqNumx = findLastRequestedRecord(data, refSeqNum);
               if (seqNumx == lastRecSeqNum) {
                  includesLast = true;
               }

               long firstSeqNum = seqNumx;
               if (seqNumx >= 0L) {
                  BBacnetTrendRecord recx = (BBacnetTrendRecord)data.get();
                  ByteArrayOutputStream itemData = new ByteArrayOutputStream();
                  ByteArrayOutputStream temp = new ByteArrayOutputStream();
                  AsnOutputStream out = new AsnOutputStream();

                  do {
                     out.reset();
                     writeLogRecord(recx, pointAsnType, out);
                     if (exceedsMaxSize(maxSize, itemData, out)) {
                        moreItems = true;
                        break;
                     }

                     prependToItemData(itemData, temp, out);
                     firstSeqNum = recx.getSequenceNumber();
                     itemCount++;
                     if (!data.next()) {
                        includesFirst = true;
                        break;
                     }

                     recx = (BBacnetTrendRecord)data.get();
                  } while (itemCount < itemLimit);

                  return new BacnetTrendLogUtil.ReadLogResult(itemCount, firstSeqNum, itemData.toByteArray(), includesFirst, includesLast, moreItems);
               }

               rec = EMPTY_RESULT;
            } catch (Throwable var88) {
               var15 = var88;
               throw var88;
            } finally {
               if (data != null) {
                  if (var15 != null) {
                     try {
                        data.close();
                     } catch (Throwable var84) {
                        var15.addSuppressed(var84);
                     }
                  } else {
                     data.close();
                  }
               }
            }
         } catch (Throwable var90) {
            var7 = var90;
            throw var90;
         } finally {
            if (conn != null) {
               if (var7 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var83) {
                     var7.addSuppressed(var83);
                  }
               } else {
                  conn.close();
               }
            }
         }

         return rec;
      }
   }

   private static long findFirstRequestedRecord(Cursor<BHistoryRecord> c, long refSeqNum) {
      BBacnetTrendRecord r = (BBacnetTrendRecord)c.get();
      long seqnum = r.getSequenceNumber();

      while (seqnum < refSeqNum) {
         if (!c.next()) {
            return -1L;
         }

         r = (BBacnetTrendRecord)c.get();
         seqnum = r.getSequenceNumber();
      }

      return seqnum;
   }

   private static long findLastRequestedRecord(Cursor<BHistoryRecord> c, long refSeqNum) {
      BBacnetTrendRecord r = (BBacnetTrendRecord)c.get();
      long seqnum = r.getSequenceNumber();

      while (seqnum > refSeqNum) {
         if (!c.next()) {
            return -1L;
         }

         r = (BBacnetTrendRecord)c.get();
         seqnum = r.getSequenceNumber();
      }

      return seqnum;
   }

   private static void writeLogRecord(BBacnetTrendRecord rec, Integer pointAsnType, AsnOutputStream out) {
      var logDatum = (BSimple & BSimple)(rec.getStatus().isNull() ? BBacnetNull.DEFAULT : rec.get(rec.getValueProperty()));
      int logDatumChoice = getLogDatumChoice(rec, pointAsnType);
      BBacnetLogRecord.writeLogRecord(rec.getTimestamp(), logDatum, logDatumChoice, rec.getStatus(), rec.getLogEvent().getLong(), out);
   }

   private static boolean exceedsMaxSize(int maxSize, ByteArrayOutputStream itemData, AsnOutputStream out) {
      return maxSize > 0 && itemData.size() + out.size() > maxSize;
   }

   private static void appendToItemData(ByteArrayOutputStream itemData, AsnOutputStream out) {
      try {
         out.writeTo(itemData);
      } catch (IOException var3) {
         throw new RuntimeException("Error appending to itemData output stream", var3);
      }
   }

   private static void prependToItemData(ByteArrayOutputStream itemData, ByteArrayOutputStream temp, AsnOutputStream out) {
      try {
         temp.reset();
         itemData.writeTo(temp);
         itemData.reset();
         out.writeTo(itemData);
         temp.writeTo(itemData);
      } catch (IOException var4) {
         throw new RuntimeException("Error prepending to itemData output stream", var4);
      }
   }

   static class ReadLogResult implements RangeData {
      long itemCount;
      long firstSequenceNumber;
      byte[] itemData;
      boolean includesFirst;
      boolean includesLast;
      boolean moreItems;

      ReadLogResult(long itemCount, long firstSequenceNumber, byte[] itemData, boolean includesFirst, boolean includesLast, boolean moreItems) {
         this.itemCount = itemCount;
         this.firstSequenceNumber = firstSequenceNumber;
         this.itemData = itemData;
         this.includesFirst = includesFirst;
         this.includesLast = includesLast;
         this.moreItems = moreItems;
      }

      @Override
      public BBacnetBitString getResultFlags() {
         return BBacnetBitString.make(new boolean[]{this.includesFirst, this.includesLast, this.moreItems});
      }

      @Override
      public boolean includesFirstItem() {
         return this.includesFirst;
      }

      @Override
      public boolean includesLastItem() {
         return this.includesLast;
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
