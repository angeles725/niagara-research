package javax.baja.bacnet.datatypes;

import java.util.GregorianCalendar;
import java.util.logging.Logger;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "choice",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,2)")}
   ), @NiagaraProperty(
      name = "date",
      type = "BBacnetDate",
      defaultValue = "BBacnetDate.DEFAULT"
   ), @NiagaraProperty(
      name = "dateRange",
      type = "BBacnetDateRange",
      defaultValue = "new BBacnetDateRange()"
   ), @NiagaraProperty(
      name = "weekNDay",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.BACNET_WEEK_N_DAY",
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"bacnet:BacnetWeekNDayFE\""
      ), @Facet(
         name = "BFacets.UX_FIELD_EDITOR",
         value = "\"bacnet:BacnetWeekNDayEditor\""
      )}
   )})
public final class BBacnetCalendarEntry extends BStruct implements BIBacnetDataType {
   public static final Property choice = newProperty(0, 0, BFacets.makeInt(0, 2));
   public static final Property date = newProperty(0, BBacnetDate.DEFAULT, null);
   public static final Property dateRange = newProperty(0, new BBacnetDateRange(), null);
   public static final Property weekNDay = newProperty(
      0,
      BBacnetOctetString.BACNET_WEEK_N_DAY,
      BFacets.make(BFacets.make("fieldEditor", "bacnet:BacnetWeekNDayFE"), BFacets.make("uxFieldEditor", "bacnet:BacnetWeekNDayEditor"))
   );
   public static final Type TYPE = Sys.loadType(BBacnetCalendarEntry.class);
   public static final int DATE_TAG = 0;
   public static final int DATE_RANGE_TAG = 1;
   public static final int WEEK_N_DAY_TAG = 2;
   private static GregorianCalendar GREG = new GregorianCalendar();
   private static final Logger logger = Logger.getLogger("bacnet.debug");
   private static int MAX_ITERATIONS = 366;
   public static final int MAX_ENCODED_SIZE = 12;

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public BBacnetDate getDate() {
      return (BBacnetDate)this.get(date);
   }

   public void setDate(BBacnetDate v) {
      this.set(date, v, null);
   }

   public BBacnetDateRange getDateRange() {
      return (BBacnetDateRange)this.get(dateRange);
   }

   public void setDateRange(BBacnetDateRange v) {
      this.set(dateRange, v, null);
   }

   public BBacnetOctetString getWeekNDay() {
      return (BBacnetOctetString)this.get(weekNDay);
   }

   public void setWeekNDay(BBacnetOctetString v) {
      this.set(weekNDay, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetCalendarEntry() {
   }

   public BBacnetCalendarEntry(BBacnetDate date) {
      this.setChoice(0);
      this.setDate(date);
   }

   public BBacnetCalendarEntry(BBacnetDateRange dateRange) {
      this.setChoice(1);
      this.setDateRange(dateRange);
   }

   public BBacnetCalendarEntry(BBacnetOctetString weekNDay) {
      this.setChoice(2);
      this.setWeekNDay(weekNDay);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("BBacnetCalendarEntry:").append(this.getChoice()).append(" ");
      switch (this.getChoice()) {
         case 0:
            sb.append(this.getDate().toString(cx));
            break;
         case 1:
            sb.append(this.getDateRange().toString(cx));
            break;
         case 2:
            Object var3;
            if (cx != null) {
               var3 = new BasicContext(cx, BFacets.make("bacOctetStr", BString.make("weekNDay")));
            } else {
               var3 = BFacets.make("bacOctetStr", BString.make("weekNDay"));
            }

            sb.append(this.getWeekNDay().toString((Context)var3));
      }

      return sb.toString();
   }

   public BValue getCalendarEntry() {
      switch (this.getChoice()) {
         case 0:
            return this.getDate();
         case 1:
            return this.getDateRange();
         case 2:
            return this.getWeekNDay();
         default:
            throw new IllegalStateException();
      }
   }

   public void setCalendarEntry(BValue e) {
      this.setCalendarEntry(e, null);
   }

   public void setCalendarEntry(BValue e, Context cx) {
      Type t = e.getType();
      if (t == BBacnetDate.TYPE) {
         this.setInt(choice, 0, cx);
         this.set(date, e, cx);
      } else if (t == BBacnetDateRange.TYPE) {
         this.setInt(choice, 1, cx);
         this.set(dateRange, e.newCopy(), cx);
      } else if (t == BBacnetOctetString.TYPE) {
         this.setInt(choice, 2, cx);
         this.set(weekNDay, e, cx);
      }
   }

   public boolean isActive(BAbsTime at) {
      BBacnetDate d = BBacnetDate.make(at);
      switch (this.getChoice()) {
         case 0:
            return this.getDate().dateEquals(d);
         case 1:
            return this.getDateRange().getStartDate().isNotAfter(d) && this.getDateRange().getEndDate().isNotBefore(d);
         case 2:
            int month = at.getMonth().getOrdinal() + 1;
            int dayOfMonth = at.getDay();
            int dayOfWeek = at.getWeekday().getOrdinal();
            if (dayOfWeek == 0) {
               dayOfWeek = 7;
            }

            byte[] weekNDay = this.getWeekNDay().getBytes();
            boolean result = true;
            if (weekNDay.length < 3) {
               throw new IllegalStateException();
            } else {
               if (weekNDay[0] != -1 && weekNDay[0] != month) {
                  result = false;
               }

               if (weekNDay[1] == -1) {
                  if (weekNDay[2] != -1 && weekNDay[2] != dayOfWeek) {
                     result = false;
                  }

                  return result;
               } else {
                  switch (weekNDay[1]) {
                     case 1:
                        return dayOfMonth >= 1 && dayOfMonth <= 7;
                     case 2:
                        return dayOfMonth >= 8 && dayOfMonth <= 14;
                     case 3:
                        return dayOfMonth >= 15 && dayOfMonth <= 21;
                     case 4:
                        return dayOfMonth >= 22 && dayOfMonth <= 28;
                     case 5:
                        return dayOfMonth >= 29 && dayOfMonth <= 31;
                     case 6:
                        int maxDOM = GREG.getActualMaximum(5);
                        return dayOfMonth > maxDOM - 7 && dayOfMonth <= maxDOM;
                     default:
                        logger.severe("Incorrect weekOfMonth configuration for BBacnetWeekNDay in BBacnetCalendarEntry!");
                        throw new IllegalStateException();
                  }
               }
            }
         default:
            throw new IllegalArgumentException("Invalid calendar entry type:" + this.getChoice());
      }
   }

   public BAbsTime nextDate(BAbsTime time) {
      BBacnetDate d = BBacnetDate.make(time);
      switch (this.getChoice()) {
         case 0:
            if (d.isNotAfter(this.getDate())) {
               return this.getDate().makeBAbsTime(time);
            }
            break;
         case 1:
            if (d.isNotAfter(this.getDateRange().getEndDate())) {
               if (d.isNotBefore(this.getDateRange().getStartDate())) {
                  return time;
               }

               return this.getDateRange().getStartDate().makeBAbsTime(time);
            }
            break;
         case 2:
            for (int i = 0; i < MAX_ITERATIONS; i++) {
               if (this.isActive(time)) {
                  return time;
               }

               time = time.nextDay();
            }
            break;
         default:
            throw new IllegalStateException();
      }

      return null;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      switch (this.getChoice()) {
         case 0:
            out.writeDate(0, this.getDate());
            break;
         case 1:
            out.writeOpeningTag(1);
            this.getDateRange().writeAsn(out);
            out.writeClosingTag(1);
            break;
         case 2:
            out.writeOctetString(2, this.getWeekNDay());
            break;
         default:
            throw new IllegalStateException("Invalid calendar entry type:" + this.getChoice());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int choice = in.peekTag();
      switch (choice) {
         case 0:
            this.set(date, in.readDate(0), noWrite);
            break;
         case 1:
            in.skipOpeningTag(1);
            BBacnetDateRange dateRange = new BBacnetDateRange();
            dateRange.readAsn(in);
            in.skipClosingTag(1);
            this.set(BBacnetCalendarEntry.dateRange, dateRange, noWrite);
            break;
         case 2:
            this.set(weekNDay, in.readBacnetOctetString(2), noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + choice);
      }

      this.setInt(BBacnetCalendarEntry.choice, choice, noWrite);
   }
}
