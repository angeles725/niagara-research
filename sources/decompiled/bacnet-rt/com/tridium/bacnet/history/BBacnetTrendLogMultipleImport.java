package com.tridium.bacnet.history;

import com.tridium.bacnet.asn.AsnUtil;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetLogMultipleRecord;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.driver.history.ArchiveException;
import javax.baja.driver.util.BDescriptorState;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistorySpace;
import javax.baja.history.BIHistory;
import javax.baja.history.HistorySpaceConnection;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "localHistoryNames",
   type = "String",
   defaultValue = "",
   flags = 1
)
public class BBacnetTrendLogMultipleImport extends BAbstractBacnetHistory implements BacnetConst {
   public static final Property localHistoryNames = newProperty(1, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetTrendLogMultipleImport.class);
   public static final Logger logger = Logger.getLogger("bacnet.history");
   public static final Lexicon lex = Lexicon.make("bacnet");
   String[] prevNams = null;

   public String getLocalHistoryNames() {
      return this.getString(localHistoryNames);
   }

   public void setLocalHistoryNames(String v) {
      this.setString(localHistoryNames, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void doExecute() throws ArchiveException {
      if (this.isRunning()) {
         BBacnetDevice device = this.device();
         if (!device.isServiceSupported("readRange")) {
            this.executeFail(lex.getText("serviceNotSupported.readRange"));
         } else {
            BHistoryDatabase db = this.getHistoryDb();
            this.verifyLocalNameFormat();
            HistorySpaceConnection conn = db.getConnection(null);
            Throwable var4 = null;

            try {
               BIHistory[] hist = this.getOrCreateHistories(conn);
               long referenceIndex = -1L;
               int rangeType = 6;
               BBacnetDateTime referenceTime = this.getReferenceTime();
               int count = this.getMaxRecordsPerRequest();
               if (count == 0) {
                  count = 10;
               }

               try {
                  referenceIndex = this.determineNextIndex();
                  if (referenceIndex >= 0L) {
                     RangeData prevResponse = null;
                     boolean moreItems = true;

                     while (moreItems) {
                        RangeData response = this.client()
                           .readRange(device.getAddress(), this.getObjectId(), 131, -1, rangeType, referenceIndex, referenceTime, count);
                        if (response == null) {
                           logger.info("Error importing history data for " + this + ": null response from device!");
                           break;
                        }

                        if (response.equals(prevResponse)) {
                           logger.info("Error importing history data for " + this + ": duplicate response (loop?):\nResponse:" + response);
                           break;
                        }

                        prevResponse = response;
                        byte[] encodedValue = response.getItemData();
                        if (encodedValue == null || encodedValue.length == 0) {
                           break;
                        }

                        moreItems = response.isMoreItems() || !response.includesLastItem();
                        this.asnIn.setBuffer(encodedValue);
                        long currentSeqNum = response.getFirstSequenceNumber();
                        if (currentSeqNum == -1L) {
                           currentSeqNum = BacnetTrendLogUtil.incrementSequenceNumber(this.getLastSequenceNumberProcessed());
                        }

                        Array<BBacnetLogMultipleRecord> a = new Array(BBacnetLogMultipleRecord.class);

                        while (this.asnIn.peekTag() != -1) {
                           BBacnetLogMultipleRecord entry = new BBacnetLogMultipleRecord(this.asnIn);
                           a.add(entry);
                        }

                        BBacnetLogMultipleRecord[] recs = (BBacnetLogMultipleRecord[])a.trim();
                        BBacnetLogMultipleRecord lstRec = recs[recs.length - 1];
                        this.getReferenceTime().copyFrom(lstRec.getTimestamp());
                        this.setLastSequenceNumberProcessed(response.getFirstSequenceNumber() + recs.length - 1L);

                        for (int i = 0; i < recs.length; i++) {
                           BBacnetLogMultipleRecord entry = recs[i];

                           for (int n = 0; n < hist.length; n++) {
                              if (hist[n] != null) {
                                 try {
                                    BHistoryRecord rec = entry.initializeNiagaraRecord(hist[n].getConfig().makeRecord(), currentSeqNum, n);
                                    conn.append(hist[n], this.correctTimestamp(rec));
                                 } catch (Exception var42) {
                                    logger.severe("Ignore trend record " + currentSeqNum + ":" + var42.toString());
                                    break;
                                 }
                              }
                           }

                           currentSeqNum = BacnetTrendLogUtil.incrementSequenceNumber(currentSeqNum);
                        }

                        if (moreItems) {
                           referenceIndex = BacnetTrendLogUtil.incrementSequenceNumber(this.getLastSequenceNumberProcessed());
                           referenceTime = this.getReferenceTime();
                        }
                     }

                     this.executeOk();
                     return;
                  }
               } catch (Exception var43) {
                  this.executeFail(var43);
                  logger.log(
                     Level.SEVERE,
                     "Asn Exception reading range (device "
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
                     (Throwable)var43
                  );
                  throw new ArchiveException(var43);
               } finally {
                  this.setState(BDescriptorState.idle);
               }
            } catch (Throwable var45) {
               var4 = var45;
               throw var45;
            } finally {
               if (conn != null) {
                  if (var4 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var41) {
                        var4.addSuppressed(var41);
                     }
                  } else {
                     conn.close();
                  }
               }
            }
         }
      }
   }

   private BBacnetDeviceObjectPropertyReference[] getDeviceObjectProps() throws ArchiveException {
      try {
         byte[] ba = this.client().readProperty(this.device().getAddress(), this.getObjectId(), 132);
         this.oprChange = this.prev == null || !ByteArrayUtil.equals(ba, this.prev);
         this.prev = ba;
         this.asnIn.setBuffer(ba);
         Array<BBacnetDeviceObjectPropertyReference> opra = new Array(BBacnetDeviceObjectPropertyReference.class);

         while (this.asnIn.available() > 0) {
            BBacnetDeviceObjectPropertyReference opr = new BBacnetDeviceObjectPropertyReference();
            opr.readAsn(this.asnIn);
            opra.add(opr);
         }

         return (BBacnetDeviceObjectPropertyReference[])opra.trim();
      } catch (Throwable var4) {
         throw new ArchiveException(var4);
      }
   }

   private String[] getHistoryNames(BBacnetDeviceObjectPropertyReference[] opra) throws ArchiveException {
      if (!this.oprChange && this.prevNams != null) {
         return this.prevNams;
      } else {
         try {
            BBacnetDevice dev = this.device();
            Hashtable<BBacnetObjectIdentifier, String> hash = new Hashtable<>();
            Array<String> namAl = new Array(String.class);

            for (int i = 0; i < opra.length; i++) {
               BBacnetObjectIdentifier objId = opra[i].getObjectId();
               String nam = hash.get(objId);
               if (nam == null) {
                  try {
                     byte[] ba = this.client().readProperty(dev.getAddress(), objId, 77);
                     nam = AsnUtil.fromAsnCharacterString(ba);
                  } catch (Throwable var10) {
                     nam = objId.toShortString();
                  }

                  hash.put(objId, nam);
               }

               if (nam.length() > 0) {
                  int propId = opra[i].getPropertyId();
                  PropertyInfo pi = dev.getPropertyInfo(objId.getId(), propId);
                  if (pi != null) {
                     nam = this.getLocalHistoryName() + "_" + nam + "_" + pi.getName();
                  } else {
                     nam = this.getLocalHistoryName() + "_" + nam + "_p" + propId;
                  }
               }

               namAl.add(nam);
            }

            this.prevNams = (String[])namAl.trim();
            this.setLocalHistoryNames(this.toDelimitedString(this.prevNams));
            return this.prevNams;
         } catch (Throwable var11) {
            throw new ArchiveException(var11);
         }
      }
   }

   private String toDelimitedString(String[] a) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < a.length; i++) {
         if (i > 0) {
            sb.append(",");
         }

         sb.append(a[i]);
      }

      return sb.toString();
   }

   private static String[] getStringArray(String s) {
      StringTokenizer st = new StringTokenizer(s, ",");
      int tokCnt = st.countTokens();
      String[] b = new String[tokCnt];

      for (int i = 0; i < tokCnt; i++) {
         b[i] = st.nextToken();
      }

      return b;
   }

   public String[] getLocalHistoryNamesArray() {
      return getStringArray(this.getLocalHistoryNames());
   }

   private BIHistory[] getOrCreateHistories(HistorySpaceConnection conn) {
      BBacnetDeviceObjectPropertyReference[] opra = this.getDeviceObjectProps();
      String[] histNames = this.getHistoryNames(opra);
      BIHistory[] hista = new BIHistory[opra.length];

      for (int i = 0; i < opra.length; i++) {
         hista[i] = null;
         BHistoryId id = BHistoryId.make(this.device().getName(), histNames[i]);
         if (id != null) {
            hista[i] = conn.getHistory(id);
            if (hista[i] == null) {
               BTypeSpec ts = this.getTypeSpec(opra[i]);
               if (ts != null) {
                  hista[i] = this.createHistory(conn, ts, id);
               }
            }
         }
      }

      return hista;
   }

   public BIHistory[] getHistories() {
      BHistorySpace space = (BHistorySpace)BOrd.make("history:").get(this);
      String[] histNams = getStringArray(this.getLocalHistoryNames());
      if (space != null && histNams.length != 0) {
         BIHistory[] hists = new BIHistory[histNams.length];

         for (int i = 0; i < hists.length; i++) {
            BHistoryId id = BHistoryId.make(this.getParent().getParent().getName(), histNams[i]);
            HistorySpaceConnection conn = space.getConnection(null);
            Throwable var7 = null;

            try {
               hists[i] = conn.getHistory(id);
            } catch (Throwable var16) {
               var7 = var16;
               throw var16;
            } finally {
               if (conn != null) {
                  if (var7 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var15) {
                        var7.addSuppressed(var15);
                     }
                  } else {
                     conn.close();
                  }
               }
            }
         }

         return hists;
      } else {
         return new BIHistory[0];
      }
   }
}
