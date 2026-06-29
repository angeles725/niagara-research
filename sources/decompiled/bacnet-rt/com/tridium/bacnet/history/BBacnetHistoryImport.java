package com.tridium.bacnet.history;

import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetLogRecord;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RejectException;
import javax.baja.driver.history.ArchiveException;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BIHistory;
import javax.baja.history.HistorySpaceConnection;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetHistoryImport extends BAbstractBacnetHistory implements BacnetConst {
   public static final Type TYPE = Sys.loadType(BBacnetHistoryImport.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public void doExecute() throws ArchiveException {
      if (this.isRunning()) {
         BBacnetDevice device = this.device();
         if (device.isOperational()) {
            if (!this.device().isServiceSupported("readRange")) {
               throw new ArchiveException(lex.getText("serviceNotSupported.readRange"));
            }

            this.importHistoryData();
         }
      }
   }

   private void importHistoryData() throws ArchiveException {
      BBacnetDevice device = this.device();
      if (!this.overridesConfigured) {
         this.configureOverrides();
      }

      BHistoryDatabase db = this.getHistoryDb();
      this.verifyLocalNameFormat();
      BIHistory history = null;
      HistorySpaceConnection conn = db.getConnection(null);
      Throwable var5 = null;

      try {
         history = this.getOrCreateHistory(conn);
         if (history == null) {
            throw new ArchiveException("Cannot create history for " + this);
         } else {
            long referenceIndex = -1L;
            int rangeType = 6;
            BBacnetDateTime referenceTime = this.getReferenceTime();
            int count = this.getMaxRecordsPerRequest();
            if (count == 0) {
               count = 10;
            }

            try {
               if (this.getAlwaysRequestByReferenceTime()) {
                  rangeType = 7;
               } else if (device.getProtocolRevision() < 3) {
                  rangeType = 4;
               } else {
                  referenceIndex = this.determineNextIndex();
                  if (referenceIndex < 0L) {
                     this.executeOk();
                     return;
                  }
               }

               RangeData prevResponse = null;
               boolean moreItems = true;

               label247: {
                  label246:
                  while (true) {
                     ReadRangeAck var40;
                     while (true) {
                        if (!moreItems) {
                           break label247;
                        }

                        RangeData response = null;

                        try {
                           var40 = this.client().readRange(device.getAddress(), this.getObjectId(), 131, -1, rangeType, referenceIndex, referenceTime, count);
                           break;
                        } catch (RejectException var34) {
                           if (var34.getRejectReason() == 6) {
                              this.setAlwaysRequestByReferenceTime(true);
                              rangeType = 7;
                           }
                        }
                     }

                     if (var40 == null) {
                        break;
                     }

                     if (var40.equals(prevResponse)) {
                        logger.info("Error importing history data for " + this + ": duplicate response (loop?):\nResponse:" + var40);
                        break label247;
                     }

                     prevResponse = var40;
                     byte[] encodedValue = var40.getItemData();
                     if (encodedValue == null || encodedValue.length == 0) {
                        break label247;
                     }

                     moreItems = var40.isMoreItems() || !var40.includesLastItem();
                     this.asnIn.setBuffer(encodedValue);
                     long currentSeqNum = var40.getFirstSequenceNumber();
                     if (currentSeqNum == -1L) {
                        currentSeqNum = BacnetTrendLogUtil.incrementSequenceNumber(this.getLastSequenceNumberProcessed());
                     }

                     Array<BBacnetLogRecord> a = new Array(BBacnetLogRecord.class);

                     while (this.asnIn.peekTag() != -1) {
                        BBacnetLogRecord entry = new BBacnetLogRecord();
                        entry.readAsn(this.asnIn);
                        a.add(entry);
                     }

                     BBacnetLogRecord[] recs = (BBacnetLogRecord[])a.trim();
                     BBacnetLogRecord lstRec = recs[recs.length - 1];
                     this.getReferenceTime().copyFrom(lstRec.getTimestamp());
                     this.setLastSequenceNumberProcessed(var40.getFirstSequenceNumber() + recs.length - 1L);
                     int i = 0;

                     while (true) {
                        if (i >= recs.length) {
                           break label246;
                        }

                        label264: {
                           BBacnetLogRecord entry = recs[i];

                           try {
                              BHistoryRecord rec = entry.initializeNiagaraRecord(history.getConfig().makeRecord(), currentSeqNum);
                              conn.append(history, this.correctTimestamp(rec));
                           } catch (Exception var35) {
                              logger.severe("Ignore trend record " + currentSeqNum + ":" + var35.toString());
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.log(Level.FINE, "Stack Trace:", (Throwable)var35);
                              }
                              break label264;
                           }

                           currentSeqNum = BacnetTrendLogUtil.incrementSequenceNumber(currentSeqNum);
                           i++;
                           continue;
                        }

                        if (moreItems) {
                           referenceIndex = BacnetTrendLogUtil.incrementSequenceNumber(this.getLastSequenceNumberProcessed());
                           referenceTime = this.getReferenceTime();
                        }
                        break;
                     }
                  }

                  logger.info("Error importing history data for " + this + ": null response from device!");
               }

               this.executeOk();
            } catch (Exception var36) {
               logger.log(
                  Level.SEVERE,
                  "Exception reading range (device "
                     + device.getAddress()
                     + ", id "
                     + this.getObjectId()
                     + ", rangeType "
                     + rangeType
                     + ", referenceIndex "
                     + referenceIndex
                     + ", referenceTime "
                     + referenceTime
                     + ", count "
                     + count
                     + ")",
                  (Throwable)var36
               );
               throw new ArchiveException(var36);
            }
         }
      } catch (Throwable var37) {
         var5 = var37;
         throw var37;
      } finally {
         if (conn != null) {
            if (var5 != null) {
               try {
                  conn.close();
               } catch (Throwable var33) {
                  var5.addSuppressed(var33);
               }
            } else {
               conn.close();
            }
         }
      }
   }
}
