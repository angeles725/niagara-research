package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.datatypes.BTrendEvent;
import com.tridium.bacnet.history.BBacnetBooleanTrendRecord;
import com.tridium.bacnet.history.BBacnetEnumTrendRecord;
import com.tridium.bacnet.history.BBacnetNumericTrendRecord;
import com.tridium.bacnet.history.BBacnetStringTrendRecord;
import com.tridium.bacnet.history.BBacnetTrendRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.history.BHistoryRecord;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.status.BStatus;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BNumber;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "timestamp",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   ), @NiagaraProperty(
      name = "statusFlags",
      type = "BSimple",
      defaultValue = "BBacnetNull.DEFAULT"
   ), @NiagaraProperty(
      name = "timeChange",
      type = "BSimple",
      defaultValue = "BBacnetNull.DEFAULT"
   )})
public class BBacnetLogMultipleRecord extends BComponent implements BIBacnetDataType {
   public static final Property timestamp = newProperty(0, new BBacnetDateTime(), null);
   public static final Property statusFlags = newProperty(0, BBacnetNull.DEFAULT, null);
   public static final Property timeChange = newProperty(0, BBacnetNull.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetLogMultipleRecord.class);
   private BTypeSpec[] recType = null;
   private int logDataChoice;
   private static final Logger loggerBacnetDebug = Logger.getLogger("bacnet.debug");
   private static final Logger loggerBacnet = Logger.getLogger("bacnet");
   public static final int TIMESTAMP_TAG = 0;
   public static final int LOG_DATA_TAG = 1;
   public static final int LOG_STATUS_TAG = 0;
   public static final int LOG_DATA_SEQ_TAG = 1;
   public static final int TIME_CHANGE_TAG = 2;
   public static final int BOOLEAN_VALUE_TAG = 0;
   public static final int REAL_VALUE_TAG = 1;
   public static final int ENUM_VALUE_TAG = 2;
   public static final int UNSIGNED_VALUE_TAG = 3;
   public static final int SIGNED_VALUE_TAG = 4;
   public static final int BITSTRING_VALUE_TAG = 5;
   public static final int NULL_VALUE_TAG = 6;
   public static final int FAILURE_TAG = 7;
   public static final int ANY_VALUE_TAG = 8;

   public BBacnetDateTime getTimestamp() {
      return (BBacnetDateTime)this.get(timestamp);
   }

   public void setTimestamp(BBacnetDateTime v) {
      this.set(timestamp, v, null);
   }

   public BSimple getStatusFlags() {
      return (BSimple)this.get(statusFlags);
   }

   public void setStatusFlags(BSimple v) {
      this.set(statusFlags, v, null);
   }

   public BSimple getTimeChange() {
      return (BSimple)this.get(timeChange);
   }

   public void setTimeChange(BSimple v) {
      this.set(timeChange, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetLogMultipleRecord() {
   }

   public BBacnetLogMultipleRecord(AsnInput in) throws AsnException {
      this.readAsn(in);
   }

   @Override
   public void writeAsn(AsnOutput out) {
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      this.recType = null;
      in.skipOpeningTag(0);
      BBacnetDateTime timestamp = new BBacnetDateTime();
      timestamp.readAsn(in);
      in.skipClosingTag(0);
      in.skipOpeningTag(1);
      int logDataChoice = in.peekTag();
      switch (logDataChoice) {
         case 0:
            BTrendEvent logStatus = BTrendEvent.makeLogStatus(in.readBitString(0));
            in.skipClosingTag(1);
            this.set(statusFlags, logStatus, noWrite);
            break;
         case 1:
            in.skipOpeningTag(1);
            List<BSimple> logData = this.processDataSeq(in);
            in.skipClosingTag(1);
            in.skipClosingTag(1);
            this.removeAll(noWrite);
            int length = logData.size();

            for (int i = 0; i < length; i++) {
               this.addData(logData.get(i), i);
            }
            break;
         case 2:
            BTrendEvent timeChange = BTrendEvent.makeTimeChange((long)in.readReal(2));
            in.skipClosingTag(1);
            this.set(BBacnetLogMultipleRecord.timeChange, timeChange, noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + logDataChoice);
      }

      this.set(BBacnetLogMultipleRecord.timestamp, timestamp, noWrite);
   }

   private List<BSimple> processDataSeq(AsnInput in) throws AsnException {
      List<BSimple> logData = new ArrayList<>();
      Array<BTypeSpec> typeSpecs = new Array(BTypeSpec.class);

      for (int choice = in.peekTag(); !in.isClosingTag(1); choice = in.peekTag()) {
         BSimple logDatum;
         BTypeSpec typeSpec;
         switch (choice) {
            case 0:
               logDatum = BBoolean.make(in.readBoolean(0));
               typeSpec = BBacnetBooleanTrendRecord.TYPE.getTypeSpec();
               break;
            case 1:
               logDatum = in.readFloat(1);
               typeSpec = BBacnetNumericTrendRecord.TYPE.getTypeSpec();
               break;
            case 2:
               logDatum = BDynamicEnum.make(in.readEnumerated(2));
               typeSpec = BBacnetEnumTrendRecord.TYPE.getTypeSpec();
               break;
            case 3:
               logDatum = in.readUnsigned(3);
               typeSpec = BBacnetNumericTrendRecord.TYPE.getTypeSpec();
               break;
            case 4:
               logDatum = in.readSigned(4);
               typeSpec = BBacnetNumericTrendRecord.TYPE.getTypeSpec();
               break;
            case 5:
               logDatum = in.readBitString(5);
               typeSpec = BBacnetStringTrendRecord.TYPE.getTypeSpec();
               break;
            case 6:
               logDatum = in.readNull(6);
               typeSpec = BBacnetNull.TYPE.getTypeSpec();
               break;
            case 7:
               in.skipOpeningTag(7);
               NErrorType failure = new NErrorType();
               failure.readEncoded(in);
               in.skipClosingTag(7);
               logDatum = BTrendEvent.makeFailure(failure);
               typeSpec = null;
               break;
            case 8:
               in.skipOpeningTag(8);
               if (in.peekApplicationTag() == 7) {
                  logDatum = BString.make(in.readCharacterString());
                  in.skipClosingTag(8);
               } else {
                  while (in.available() > 0 && !in.isClosingTag(8)) {
                     in.skipTag();
                  }

                  loggerBacnetDebug.info(this + ".readAsn:logDatumChoice " + choice + " not yet supported");
                  logDatum = BBacnetNull.DEFAULT;
               }

               typeSpec = BBacnetStringTrendRecord.TYPE.getTypeSpec();
               break;
            default:
               throw new AsnException("Invalid tag: " + choice);
         }

         logData.add(logDatum);
         typeSpecs.add(typeSpec);
      }

      this.recType = (BTypeSpec[])typeSpecs.trim();
      return logData;
   }

   private void addData(BSimple d, int n) {
      this.add(this.getSeqName(n), d, noWrite);
   }

   private String getSeqName(int n) {
      return "data" + n;
   }

   public BTypeSpec getNiagaraRecordType(int n) {
      return this.recType[n];
   }

   public BHistoryRecord initializeNiagaraRecord(BHistoryRecord record, long seqNum, int ndx) {
      BBacnetTrendRecord rec = (BBacnetTrendRecord)record;
      rec.setTimestamp(this.getTimestamp().toBAbsTime());
      rec.setSequenceNumber(seqNum);
      if (!this.isLogData()) {
         if (this.isLogStatus()) {
            rec.setLogEvent((BTrendEvent)this.getStatusFlags());
         } else {
            rec.setLogEvent((BTrendEvent)this.getTimeChange());
         }

         rec.setTrendFlags(rec.getTrendFlags().set(4, true));
         return rec;
      } else {
         BSimple o = (BSimple)this.get(this.getSeqName(ndx));
         rec.setLogEvent(BTrendEvent.DEFAULT);
         Type t = rec.getType();
         if (t == BBacnetBooleanTrendRecord.TYPE) {
            if (o instanceof BBacnetNull) {
               ((BBacnetBooleanTrendRecord)rec).setValue(false);
               rec.setLogEvent(BTrendEvent.DEFAULT);
               rec.setStatus(BStatus.makeNull(rec.getStatus(), true));
            } else {
               ((BBacnetBooleanTrendRecord)rec).setValue(((BBoolean)o).getBoolean());
            }
         } else if (t == BBacnetNumericTrendRecord.TYPE) {
            if (o instanceof BBacnetNull) {
               ((BBacnetNumericTrendRecord)rec).setValue(0.0);
               rec.setLogEvent(BTrendEvent.DEFAULT);
               rec.setStatus(BStatus.makeNull(rec.getStatus(), true));
            } else if (o.getType().is(BBacnetUnsigned.TYPE)) {
               ((BBacnetNumericTrendRecord)rec).setValue(((BBacnetUnsigned)o).getLong());
            } else {
               ((BBacnetNumericTrendRecord)rec).setValue(((BNumber)o).getDouble());
            }
         } else if (t == BBacnetEnumTrendRecord.TYPE) {
            if (o instanceof BBacnetNull) {
               ((BBacnetEnumTrendRecord)rec).setValue(BDynamicEnum.make(0));
               rec.setLogEvent(BTrendEvent.DEFAULT);
               rec.setStatus(BStatus.makeNull(rec.getStatus(), true));
            } else {
               ((BBacnetEnumTrendRecord)rec).setValue((BDynamicEnum)o);
            }
         } else if (t == BBacnetStringTrendRecord.TYPE) {
            try {
               if (o instanceof BBacnetNull) {
                  ((BBacnetStringTrendRecord)rec).setValue("");
                  rec.setLogEvent(BTrendEvent.DEFAULT);
                  rec.setStatus(BStatus.makeNull(rec.getStatus(), true));
               } else {
                  ((BBacnetStringTrendRecord)rec).setValue(o.encodeToString());
               }
            } catch (Exception var9) {
               loggerBacnet.log(Level.INFO, "Error, could not encode logDatum to a string (" + o.toString() + ")", (Throwable)var9);
               ((BBacnetStringTrendRecord)rec).setValue("Error, could not encode " + o.toString());
            }
         }

         return rec;
      }
   }

   public boolean isLogStatus() {
      return this.logDataChoice == 0;
   }

   public boolean isLogData() {
      return this.logDataChoice == 1;
   }

   public int getLogDataChoice() {
      return this.logDataChoice;
   }

   public BTypeSpec[] getTypeSpecs() {
      return this.recType;
   }
}
