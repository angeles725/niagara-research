package javax.baja.bacnet.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "choice",
      type = "int",
      defaultValue = "1",
      flags = 4,
      facets = {@Facet("BFacets.makeInt(0,2)")}
   ), @NiagaraProperty(
      name = "time",
      type = "BBacnetTime",
      defaultValue = "BBacnetTime.DEFAULT"
   ), @NiagaraProperty(
      name = "sequenceNumber",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT"
   ), @NiagaraProperty(
      name = "dateTime",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   )})
public final class BBacnetTimeStamp extends BStruct implements BIBacnetDataType {
   public static final Property choice = newProperty(4, 1, BFacets.makeInt(0, 2));
   public static final Property time = newProperty(0, BBacnetTime.DEFAULT, null);
   public static final Property sequenceNumber = newProperty(0, BBacnetUnsigned.DEFAULT, null);
   public static final Property dateTime = newProperty(0, new BBacnetDateTime(), null);
   public static final Type TYPE = Sys.loadType(BBacnetTimeStamp.class);
   public static final int TIME_TAG = 0;
   public static final int SEQUENCE_NUMBER_TAG = 1;
   public static final int DATE_TIME_TAG = 2;
   public static final int MAX_SEQUENCE_NUMBER = 65535;
   private static final Logger logger = Logger.getLogger("bacnet");
   private static final String[] FACET_TAGS = new String[]{"ts_t.", "ts_sn.", "ts_dt."};
   private static final String[] DEBUG_TAGS = new String[]{"timeStamp{time}:", "timeStamp{sequenceNumber}:", "timeStamp{dateTime}:"};

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public BBacnetTime getTime() {
      return (BBacnetTime)this.get(time);
   }

   public void setTime(BBacnetTime v) {
      this.set(time, v, null);
   }

   public BBacnetUnsigned getSequenceNumber() {
      return (BBacnetUnsigned)this.get(sequenceNumber);
   }

   public void setSequenceNumber(BBacnetUnsigned v) {
      this.set(sequenceNumber, v, null);
   }

   public BBacnetDateTime getDateTime() {
      return (BBacnetDateTime)this.get(dateTime);
   }

   public void setDateTime(BBacnetDateTime v) {
      this.set(dateTime, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetTimeStamp() {
   }

   public BBacnetTimeStamp(BBacnetTime time) {
      this.setChoice(0);
      this.setTime(time);
   }

   public BBacnetTimeStamp(BBacnetUnsigned sequenceNumber) {
      this.setChoice(1);
      this.setSequenceNumber(sequenceNumber);
   }

   public BBacnetTimeStamp(BBacnetDateTime dateTime) {
      this.setChoice(2);
      this.setDateTime(dateTime);
   }

   public BBacnetTimeStamp(BAbsTime babsTime) {
      this.fromBAbsTime(babsTime);
   }

   public BValue getTimeStamp() {
      switch (this.getChoice()) {
         case 0:
            return this.getTime();
         case 1:
            return this.getSequenceNumber();
         case 2:
            return this.getDateTime();
         default:
            throw new IllegalStateException();
      }
   }

   public void setTimeStamp(BValue ts) {
      this.setTimeStamp(ts, null);
   }

   public void setTimeStamp(BValue ts, Context cx) {
      if (ts.getType() == BBacnetTime.TYPE) {
         this.setInt(choice, 0, cx);
         this.set(time, ts, cx);
      } else if (ts.getType() == BBacnetUnsigned.TYPE) {
         this.setInt(choice, 1, cx);
         this.set(sequenceNumber, ts, cx);
      } else if (ts.getType() == BBacnetDateTime.TYPE) {
         this.setInt(choice, 2, cx);
         this.set(dateTime, ts, cx);
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      switch (this.getChoice()) {
         case 0:
            out.writeTime(0, this.getTime());
            break;
         case 1:
            out.writeUnsigned(1, this.getSequenceNumber());
            break;
         case 2:
            out.writeOpeningTag(2);
            this.getDateTime().writeAsn(out);
            out.writeClosingTag(2);
            break;
         default:
            throw new IllegalStateException("Invalid timestamp type:" + this.getChoice());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int choice = in.peekTag();
      switch (choice) {
         case 0:
            this.set(time, in.readTime(0), noWrite);
            break;
         case 1:
            this.set(sequenceNumber, in.readUnsigned(1), noWrite);
            break;
         case 2:
            in.skipOpeningTag(2);
            BBacnetDateTime dateTime = new BBacnetDateTime();
            dateTime.readAsn(in);
            in.skipClosingTag(2);
            this.set(BBacnetTimeStamp.dateTime, dateTime, noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + choice);
      }

      this.setInt(BBacnetTimeStamp.choice, choice, noWrite);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(tag(this.getChoice(), cx));
      sb.append(this.getTimeStamp().toString(cx));
      return sb.toString();
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(tag(this.getChoice(), BacnetConst.debugContext));
      sb.append(this.getTime().toString()).append(";").append(this.getSequenceNumber().toString()).append(";").append(this.getDateTime().toString());
      return sb.toString();
   }

   public static BBacnetTimeStamp fromText(String text) {
      if (text.startsWith(FACET_TAGS[0])) {
         return new BBacnetTimeStamp(BBacnetTime.fromString(text.substring(FACET_TAGS[0].length())));
      } else if (text.startsWith(FACET_TAGS[1])) {
         return new BBacnetTimeStamp(BBacnetUnsigned.make(Long.parseLong(text.substring(FACET_TAGS[1].length()))));
      } else if (text.startsWith(FACET_TAGS[2])) {
         return new BBacnetTimeStamp(BBacnetDateTime.fromString(text.substring(FACET_TAGS[2].length())));
      } else {
         throw new IllegalArgumentException(text);
      }
   }

   private static String tag(int choice, Context cx) {
      if (cx == null) {
         return "";
      } else {
         return cx.equals(BacnetConst.facetsContext) ? FACET_TAGS[choice] : DEBUG_TAGS[choice];
      }
   }

   public BAbsTime toBAbsTime() {
      switch (this.getChoice()) {
         case 0:
            logger.fine("converting BBacnetTime > BAbsTime using Niagara date");
            return BAbsTime.make(BAbsTime.now(), this.getTime().toBTime());
         case 1:
            logger.fine("using Niagara time to replace Sequence Number Timestamp!");
            return BAbsTime.now();
         case 2:
            BBacnetDate d = this.getDateTime().getDate();
            BBacnetTime t = this.getDateTime().getTime();
            if (isMin(d, t)) {
               return BAbsTime.NULL;
            }

            return BAbsTime.make(d.getYear(), d.getBMonth(), d.getDayOfMonth(), t.getHour(), t.getMinute(), t.getSecond(), t.getHundredth() * 10);
         default:
            return BAbsTime.make();
      }
   }

   private static boolean isMin(BBacnetDate date, BBacnetTime time) {
      return date.getYear() == 1900
         && date.getMonth() == 1
         && date.getDayOfMonth() == 1
         && time.getHour() == 0
         && time.getMinute() == 0
         && time.getSecond() == 0
         && time.getHundredth() == 0;
   }

   public void fromBAbsTime(BAbsTime babsTime) {
      this.fromBAbsTime(babsTime, 2);
   }

   public void fromBAbsTime(BAbsTime babsTime, int timestampType) {
      this.setChoice(timestampType);
      switch (timestampType) {
         case 0:
            this.setChoice(0);
            this.setTime(BBacnetTime.make(babsTime));
            break;
         case 1:
            throw new IllegalArgumentException("Cannot convert BAbsTime to Bacnet Sequence Number!");
         case 2:
            this.setChoice(2);
            this.setDateTime(new BBacnetDateTime(babsTime));
      }
   }

   public static void encodeTimeStamp(BAbsTime t, AsnOutput out) {
      out.writeOpeningTag(2);
      out.writeDate(t);
      out.writeTime(t);
      out.writeClosingTag(2);
   }

   public void encode(DataOutput out) throws IOException {
      int choice = this.getChoice();
      out.writeInt(choice);
      switch (choice) {
         case 0:
            this.getTime().encode(out);
            break;
         case 1:
            this.getSequenceNumber().encode(out);
            break;
         case 2:
            this.getDateTime().getDate().encode(out);
            this.getDateTime().getTime().encode(out);
      }
   }

   public void decode(DataInput in) throws IOException {
      int choice = in.readInt();
      this.setChoice(choice);
      switch (choice) {
         case 0:
            this.setTime((BBacnetTime)this.getTime().decode(in));
            break;
         case 1:
            this.setSequenceNumber((BBacnetUnsigned)this.getSequenceNumber().decode(in));
            break;
         case 2:
            BBacnetDateTime dt = this.getDateTime();
            dt.setDate((BBacnetDate)dt.getDate().decode(in));
            dt.setTime((BBacnetTime)dt.getTime().decode(in));
      }
   }
}
