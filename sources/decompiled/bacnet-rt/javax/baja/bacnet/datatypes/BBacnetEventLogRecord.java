package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.EventNotificationParameters;
import com.tridium.bacnet.datatypes.BBacnetEventNotification;
import com.tridium.bacnet.datatypes.BTrendEvent;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "timestamp",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   ), @NiagaraProperty(
      name = "logDatum",
      type = "BValue",
      defaultValue = "BBacnetNull.DEFAULT"
   )})
public final class BBacnetEventLogRecord extends BComponent implements BIBacnetDataType {
   public static final Property timestamp = newProperty(0, new BBacnetDateTime(), null);
   public static final Property logDatum = newProperty(0, BBacnetNull.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetEventLogRecord.class);
   private static final int LOG_STATUS_TAG = 0;
   private static final int NOTIFICATION_TAG = 1;
   private static final int TIME_CHANGE_TAG = 2;

   public BBacnetDateTime getTimestamp() {
      return (BBacnetDateTime)this.get(timestamp);
   }

   public void setTimestamp(BBacnetDateTime v) {
      this.set(timestamp, v, null);
   }

   public BValue getLogDatum() {
      return this.get(logDatum);
   }

   public void setLogDatum(BValue v) {
      this.set(logDatum, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      this.getTimestamp().writeAsn(out);
      out.writeClosingTag(0);
      out.writeOpeningTag(1);
      this.writeLogDatum((AsnOutputStream)out);
      out.writeClosingTag(1);
   }

   private void writeLogDatum(AsnOutputStream out) {
      BValue logDatum = this.getLogDatum();
      if (logDatum instanceof BBacnetEventNotification) {
         out.writeOpeningTag(1);
         BBacnetEventNotification eventNotification = (BBacnetEventNotification)logDatum;
         EventNotificationParameters eventParams = eventNotification.getEventNotificationParameters();
         eventParams.writeEncoded(out);
         out.writeClosingTag(1);
      } else {
         if (logDatum instanceof BTrendEvent) {
            BTrendEvent event = (BTrendEvent)logDatum;
            if (event.isLogStatus()) {
               out.writeBitString(0, BTrendEvent.getLogStatus(event.getLong()));
               return;
            }

            if (event.isTimeChange()) {
               out.writeReal(2, BTrendEvent.getTimeChange(event.getLong()));
               return;
            }
         }

         throw new IllegalStateException("BBacnetEventLogRecord: Invalid log-datum choice type: " + logDatum.getType());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      in.skipOpeningTag(0);
      BBacnetDate date = in.readDate();
      BBacnetTime time = in.readTime();
      in.skipClosingTag(0);
      in.skipOpeningTag(1);
      int logDatumChoice = in.peekTag();
      BValue logDatum;
      switch (logDatumChoice) {
         case 0:
            logDatum = BTrendEvent.makeLogStatus(in.readBitString(0));
            break;
         case 1:
            in.skipOpeningTag(1);
            EventNotificationParameters eventParams = new EventNotificationParameters();
            eventParams.readEncoded((AsnInputStream)in);
            logDatum = new BBacnetEventNotification(eventParams);
            in.skipClosingTag(1);
            break;
         case 2:
            logDatum = BTrendEvent.makeTimeChange((long)in.readReal(2));
            break;
         default:
            throw new AsnException("Invalid tag: " + logDatumChoice);
      }

      in.skipClosingTag(1);
      BBacnetDateTime timestamp = this.getTimestamp();
      timestamp.set(BBacnetDateTime.date, date, noWrite);
      timestamp.set(BBacnetDateTime.time, time, noWrite);
      this.set(BBacnetEventLogRecord.logDatum, logDatum, noWrite);
   }

   public String toString(Context context) {
      return this.getTimestamp().toString(context) + '_' + this.getLogDatum().toString(context);
   }
}
