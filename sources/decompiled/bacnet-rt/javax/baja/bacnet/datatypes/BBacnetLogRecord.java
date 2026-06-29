package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.datatypes.BTrendEvent;
import com.tridium.bacnet.history.BBacnetBooleanTrendRecord;
import com.tridium.bacnet.history.BBacnetEnumTrendRecord;
import com.tridium.bacnet.history.BBacnetNumericTrendRecord;
import com.tridium.bacnet.history.BBacnetStringTrendRecord;
import com.tridium.bacnet.history.BBacnetTrendRecord;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.history.BHistoryRecord;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "timestamp",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   ), @NiagaraProperty(
      name = "logDatum",
      type = "BSimple",
      defaultValue = "BBacnetNull.DEFAULT"
   ), @NiagaraProperty(
      name = "statusFlags",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetStatusFlags\"))",
      facets = {@Facet("BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS")}
   )})
public final class BBacnetLogRecord extends BStruct implements BIBacnetDataType {
   public static final Property timestamp = newProperty(0, new BBacnetDateTime(), null);
   public static final Property logDatum = newProperty(0, BBacnetNull.DEFAULT, null);
   public static final Property statusFlags = newProperty(
      0, BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")), BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS
   );
   public static final Type TYPE = Sys.loadType(BBacnetLogRecord.class);
   private BTypeSpec recType = null;
   private static final Logger loggerBacnetDebug = Logger.getLogger("bacnet.debug");
   private static final Logger loggerBacnet = Logger.getLogger("bacnet");
   public static final int TIMESTAMP_TAG = 0;
   public static final int LOG_DATUM_TAG = 1;
   public static final int STATUS_FLAGS_TAG = 2;
   public static final int LOG_STATUS_TAG = 0;
   public static final int BOOLEAN_VALUE_TAG = 1;
   public static final int REAL_VALUE_TAG = 2;
   public static final int ENUM_VALUE_TAG = 3;
   public static final int UNSIGNED_VALUE_TAG = 4;
   public static final int SIGNED_VALUE_TAG = 5;
   public static final int BITSTRING_VALUE_TAG = 6;
   public static final int NULL_VALUE_TAG = 7;
   public static final int FAILURE_TAG = 8;
   public static final int TIME_CHANGE_TAG = 9;
   public static final int ANY_VALUE_TAG = 10;
   private static Lexicon lex = Lexicon.make("bacnet");
   public static final String LOG_STATUS_STRING = lex.getText("BacnetLogRecord.status");
   public static final String FAILURE_STRING = lex.getText("BacnetLogRecord.failure");
   public static final String TIME_CHANGE_STRING = lex.getText("BacnetLogRecord.timeChange");
   public static final String EVENT_STRING = lex.getText("BacnetLogRecord.event");
   public static final String INVALID_STRING = lex.getText("BacnetLogRecord.invalid");
   public static final String UNKNOWN_STRING = lex.getText("BacnetLogRecord.unknown");
   public static final String LOG_ENABLED_STRING = lex.getText("BacnetLogRecord.enabled");
   public static final String LOG_DISABLED_STRING = lex.getText("BacnetLogRecord.disabled");
   public static final String LOG_BUFFER_PURGED_STRING = lex.getText("BacnetLogRecord.purged");
   public static final String LOG_INTERRUPTED_STRING = lex.getText("BacnetLogRecord.interrupted");
   public static final String SECONDS_STRING = lex.getText("BacnetLogRecord.seconds");

   public BBacnetDateTime getTimestamp() {
      return (BBacnetDateTime)this.get(timestamp);
   }

   public void setTimestamp(BBacnetDateTime v) {
      this.set(timestamp, v, null);
   }

   public BSimple getLogDatum() {
      return (BSimple)this.get(logDatum);
   }

   public void setLogDatum(BSimple v) {
      this.set(logDatum, v, null);
   }

   public BBacnetBitString getStatusFlags() {
      return (BBacnetBitString)this.get(statusFlags);
   }

   public void setStatusFlags(BBacnetBitString v) {
      this.set(statusFlags, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetLogRecord() {
   }

   public BBacnetLogRecord(BBacnetDateTime dt, BSimple s) {
      this.setTimestamp(dt);
      this.setLogDatum(s);
   }

   public BBacnetLogRecord(BAbsTime bt, BSimple s) {
      this.getTimestamp().fromBAbsTime(bt);
      this.setLogDatum(s);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      this.getTimestamp().writeAsn(out);
      out.writeClosingTag(0);
      out.writeOpeningTag(1);
      writeLogDatum(out, this.getLogDatum(), this.getLogDatumType(), this.getLogDatumEvent().getLong(), BacnetBitStringUtil.getBStatus(this.getStatusFlags()));
      out.writeClosingTag(1);
      out.writeBitString(2, this.getStatusFlags());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      in.skipOpeningTag(0);
      BBacnetDateTime timestamp = new BBacnetDateTime();
      timestamp.readAsn(in);
      in.skipClosingTag(0);
      in.skipOpeningTag(1);
      int logDatumChoice = in.peekTag();
      BValue logDatum;
      BTypeSpec recType;
      switch (logDatumChoice) {
         case 0:
            logDatum = BTrendEvent.makeLogStatus(in.readBitString(0));
            recType = null;
            break;
         case 1:
            logDatum = BBoolean.make(in.readBoolean(1));
            recType = BBacnetBooleanTrendRecord.TYPE.getTypeSpec();
            break;
         case 2:
            logDatum = BFloat.make(in.readReal(2));
            recType = BBacnetNumericTrendRecord.TYPE.getTypeSpec();
            break;
         case 3:
            logDatum = BDynamicEnum.make(in.readEnumerated(3));
            recType = BBacnetEnumTrendRecord.TYPE.getTypeSpec();
            break;
         case 4:
            logDatum = in.readUnsigned(4);
            recType = BBacnetNumericTrendRecord.TYPE.getTypeSpec();
            break;
         case 5:
            logDatum = in.readSigned(5);
            recType = BBacnetNumericTrendRecord.TYPE.getTypeSpec();
            break;
         case 6:
            logDatum = in.readBitString(6);
            recType = BBacnetStringTrendRecord.TYPE.getTypeSpec();
            break;
         case 7:
            logDatum = in.readNull(7);
            recType = BBacnetStringTrendRecord.TYPE.getTypeSpec();
            break;
         case 8:
            in.skipOpeningTag(8);
            NErrorType failure = new NErrorType();
            failure.readEncoded(in);
            in.skipClosingTag(8);
            logDatum = BTrendEvent.makeFailure(failure);
            recType = null;
            break;
         case 9:
            logDatum = BTrendEvent.makeTimeChange((long)in.readReal(9));
            recType = null;
            break;
         case 10:
            in.skipOpeningTag(10);
            if (in.peekApplicationTag() == 7) {
               logDatum = BString.make(in.readCharacterString());
               in.skipClosingTag(10);
            } else {
               while (in.available() > 0 && !in.isClosingTag(10)) {
                  in.skipTag();
               }

               loggerBacnetDebug.info(this + ".readAsn:logDatumChoice " + logDatumChoice + " not yet supported");
               logDatum = BBacnetNull.DEFAULT;
            }

            recType = BBacnetStringTrendRecord.TYPE.getTypeSpec();
            break;
         default:
            throw new AsnException("Invalid tag: " + logDatumChoice);
      }

      in.skipClosingTag(1);
      in.peekTag();
      BBacnetBitString statusFlags = in.isValueTag(2) ? in.readBitString(2) : BBacnetBitString.emptyBitString(4);
      this.set(BBacnetLogRecord.timestamp, timestamp, noWrite);
      this.set(BBacnetLogRecord.logDatum, logDatum, noWrite);
      this.set(BBacnetLogRecord.statusFlags, statusFlags, noWrite);
      this.recType = recType;
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getTimestamp().toString(context))
         .append('_')
         .append(this.getLogDatum().toString(context))
         .append('_')
         .append(this.getStatusFlags().toString(this.getSlotFacets(statusFlags)));
      return sb.toString();
   }

   private int getLogDatumType() {
      Type t = this.getLogDatum().getType();
      if (t.is(BBoolean.TYPE)) {
         return 1;
      } else if (t.is(BDouble.TYPE) || t.is(BFloat.TYPE)) {
         return 2;
      } else if (this.getLogDatum() instanceof BEnum) {
         return 3;
      } else if (t.is(BBacnetUnsigned.TYPE)) {
         return 4;
      } else if (t.is(BInteger.TYPE)) {
         return 5;
      } else if (t.is(BBacnetBitString.TYPE)) {
         return 6;
      } else if (t.is(BBacnetNull.TYPE)) {
         return 7;
      } else {
         if (t.is(BTrendEvent.TYPE)) {
            BTrendEvent evt = (BTrendEvent)this.getLogDatum();
            if (evt.isLogStatus()) {
               return 0;
            }

            if (evt.isFailure()) {
               return 8;
            }

            if (evt.isTimeChange()) {
               return 9;
            }
         }

         return 10;
      }
   }

   private BTrendEvent getLogDatumEvent() {
      Type t = this.getLogDatum().getType();
      return t.is(BTrendEvent.TYPE) ? (BTrendEvent)this.getLogDatum() : BTrendEvent.DEFAULT;
   }

   public static void writeLogRecord(
      BBacnetDateTime timestamp, BSimple logDatum, int logDatumChoice, BBacnetBitString statusFlags, long trendEvent, AsnOutput out
   ) {
      out.writeOpeningTag(0);
      timestamp.writeAsn(out);
      out.writeClosingTag(0);
      out.writeOpeningTag(1);
      writeLogDatum(out, logDatum, logDatumChoice, trendEvent, BacnetBitStringUtil.getBStatus(statusFlags));
      out.writeClosingTag(1);
      out.writeBitString(2, statusFlags);
   }

   public static void writeLogRecord(BAbsTime timestamp, BSimple logDatum, int logDatumChoice, BStatus statusFlags, long trendEvent, AsnOutput out) {
      out.writeOpeningTag(0);
      out.writeDate(timestamp);
      out.writeTime(timestamp);
      out.writeClosingTag(0);
      out.writeOpeningTag(1);
      writeLogDatum(out, logDatum, logDatumChoice, trendEvent, statusFlags);
      out.writeClosingTag(1);
      out.writeBitString(2, BacnetBitStringUtil.getBacnetStatusFlags(statusFlags));
   }

   private static void writeLogDatum(AsnOutput out, BSimple logDatum, int logDatumChoice, long trendEvent, BStatus status) {
      try {
         if (status.isNull()) {
            out.writeNull(7);
            return;
         }

         if ((trendEvent & 355L) == 355L) {
            trendEvent = 0L;
            logDatumChoice = 7;
         }

         switch (logDatumChoice) {
            case 0:
               out.writeBitString(0, BTrendEvent.getLogStatus(trendEvent));
               break;
            case 1:
               if (logDatum instanceof BBoolean) {
                  out.writeBoolean(1, (BBoolean)logDatum);
               } else if (logDatum instanceof BEnum) {
                  out.writeBoolean(1, ((BEnum)logDatum).isActive());
               }
               break;
            case 2:
               out.writeReal(2, (BNumber)logDatum);
               break;
            case 3:
               out.writeEnumerated(3, (BEnum)logDatum);
               break;
            case 4:
               if (logDatum instanceof BNumber) {
                  out.writeUnsignedInteger(4, ((BNumber)logDatum).getLong());
               } else if (logDatum instanceof BEnum) {
                  out.writeUnsignedInteger(4, ((BEnum)logDatum).getOrdinal());
               } else if (logDatum instanceof BBacnetUnsigned) {
                  out.writeUnsigned(4, (BBacnetUnsigned)logDatum);
               }
               break;
            case 5:
               if (logDatum instanceof BNumber) {
                  out.writeSignedInteger(5, ((BNumber)logDatum).getInt());
               } else if (logDatum instanceof BEnum) {
                  out.writeSignedInteger(5, ((BEnum)logDatum).getOrdinal());
               }
               break;
            case 6:
               if (logDatum instanceof BBacnetBitString) {
                  out.writeBitString(6, (BBacnetBitString)logDatum);
               } else if (logDatum instanceof BString) {
                  try {
                     BBacnetBitString bs = (BBacnetBitString)BBacnetBitString.DEFAULT.decodeFromString(logDatum.toString());
                     out.writeBitString(6, bs);
                  } catch (IOException var7) {
                     out.writeOpeningTag(8);
                     out.writeEnumerated(1);
                     out.writeEnumerated(0);
                     out.writeClosingTag(8);
                  }
               }
               break;
            case 7:
               out.writeNull(7);
               break;
            case 8:
               NErrorType failure = BTrendEvent.getFailure(trendEvent);
               out.writeOpeningTag(8);
               out.writeEnumerated(failure.getErrorClass());
               out.writeEnumerated(failure.getErrorCode());
               out.writeClosingTag(8);
               break;
            case 9:
               out.writeReal(9, BTrendEvent.getTimeChange(trendEvent));
               break;
            case 10:
               if (logDatum instanceof BString) {
                  out.writeOpeningTag(10);
                  out.writeCharacterString((BString)logDatum);
                  out.writeClosingTag(10);
               } else {
                  loggerBacnetDebug.info(
                     "BacnetLogRecord.writeLogDatum:logDatumChoice "
                        + logDatumChoice
                        + " not yet supported: logDatum="
                        + logDatum
                        + " ["
                        + logDatum.getType()
                        + "]"
                  );
               }
               break;
            default:
               loggerBacnet.info("Invalid logDatumChoice!");
         }
      } catch (ClassCastException var8) {
         loggerBacnet.log(Level.INFO, "Incompatible logDatum/logDatumChoice!", (Throwable)var8);
      }
   }

   public BTypeSpec getNiagaraRecordType() {
      return this.recType;
   }

   public BHistoryRecord initializeNiagaraRecord(BHistoryRecord record, long seqNum) {
      BBacnetTrendRecord rec = (BBacnetTrendRecord)record;
      rec.setTimestamp(this.getTimestamp().toBAbsTime());
      rec.setStatus(BacnetBitStringUtil.getBStatus(this.getStatusFlags()));
      rec.setSequenceNumber(seqNum);
      if (this.getLogDatum() instanceof BBacnetNull) {
         rec.setLogEvent(BTrendEvent.DEFAULT);
         rec.setStatus(BStatus.makeNull(rec.getStatus(), true));
         return rec;
      } else if (this.recType == null) {
         rec.setLogEvent((BTrendEvent)this.getLogDatum());
         rec.setTrendFlags(rec.getTrendFlags().set(4, true));
         return rec;
      } else {
         Type t = rec.getType();
         if (t == BBacnetBooleanTrendRecord.TYPE) {
            BSimple ld = this.getLogDatum();
            if (ld.getType().is(BDynamicEnum.TYPE)) {
               boolean value = false;
               boolean trueOrFalse = false;
               BDynamicEnum datum = (BDynamicEnum)ld;
               int val = datum.getOrdinal();
               if (val == 0 || val == 1) {
                  value = val != 0;
                  trueOrFalse = true;
               }

               if (trueOrFalse) {
                  ((BBacnetBooleanTrendRecord)rec).setValue(value);
               } else {
                  loggerBacnet.info(
                     "Error, could not encode non-zero/one enumerated logDatum to a BBooleanHistoryRecord (" + this.getLogDatum().toString() + ")"
                  );
               }
            } else if (ld.getType().is(BBoolean.TYPE)) {
               ((BBacnetBooleanTrendRecord)rec).setValue(((BBoolean)this.getLogDatum()).getBoolean());
            } else {
               loggerBacnet.info("Error, could not encode logDatum to a BBooleanHistoryRecord (" + this.getLogDatum().toString() + ")");
            }
         } else if (t == BBacnetNumericTrendRecord.TYPE) {
            BSimple ld = this.getLogDatum();
            if (ld.getType().is(BBacnetUnsigned.TYPE)) {
               ((BBacnetNumericTrendRecord)rec).setValue(((BBacnetUnsigned)this.getLogDatum()).getLong());
            } else {
               ((BBacnetNumericTrendRecord)rec).setValue(((BNumber)this.getLogDatum()).getDouble());
            }
         } else if (t == BBacnetEnumTrendRecord.TYPE) {
            BSimple ld = this.getLogDatum();
            if (ld.getType().is(BBacnetUnsigned.TYPE)) {
               ((BBacnetEnumTrendRecord)rec).setValue(BDynamicEnum.make(((BBacnetUnsigned)this.getLogDatum()).getInt()));
            } else if (ld.getType().is(BDynamicEnum.TYPE)) {
               ((BBacnetEnumTrendRecord)rec).setValue((BDynamicEnum)this.getLogDatum());
            } else {
               loggerBacnet.info("Error, could not encode logDatum to a BEnumTrendRecord (" + this.getLogDatum().toString() + ")");
            }
         } else if (t == BBacnetStringTrendRecord.TYPE) {
            try {
               ((BBacnetStringTrendRecord)rec).setValue(this.getLogDatum().encodeToString());
            } catch (Exception var11) {
               loggerBacnet.log(Level.INFO, "Error, could not encode logDatum to a string (" + this.getLogDatum().toString() + ")", (Throwable)var11);
               ((BBacnetStringTrendRecord)rec).setValue("Error, could not encode " + this.getLogDatum().toString());
            }
         }

         return rec;
      }
   }
}
