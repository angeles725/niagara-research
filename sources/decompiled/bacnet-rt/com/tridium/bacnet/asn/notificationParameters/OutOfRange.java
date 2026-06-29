package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class OutOfRange extends BacnetNotificationParameters {
   public static final int EXCEEDING_VALUE_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   public static final int DEADBAND_TAG = 2;
   public static final int EXCEEDED_LIMIT_TAG = 3;
   protected Number exceedingValue;
   protected BBacnetBitString statusFlags;
   protected Number deadband;
   protected Number exceededLimit;

   public OutOfRange() {
   }

   public OutOfRange(Number exceedingValue, BBacnetBitString statusFlags, Number deadband, Number exceededLimit) {
      this.exceedingValue = exceedingValue;
      this.statusFlags = statusFlags;
      this.deadband = deadband;
      this.exceededLimit = exceededLimit;
   }

   @Override
   public int getChoiceType() {
      return 5;
   }

   public Number getExceedingValue() {
      return this.exceedingValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public Number getDeadband() {
      return this.deadband;
   }

   public Number getExceededLimit() {
      return this.exceededLimit;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeOpeningTag(this.getChoiceType());
      this.writeNumber(out, 0, this.exceedingValue);
      out.writeBitString(1, this.statusFlags);
      this.writeNumber(out, 2, this.deadband);
      this.writeNumber(out, 3, this.exceededLimit);
      out.writeClosingTag(this.getChoiceType());
   }

   public void writeNumber(AsnOutputStream out, int tag, Number n) {
      out.writeReal(tag, n.doubleValue());
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.readEncoded(null, in);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream in) throws AsnException {
      int tag = in.peekTag();
      if (in.isOpeningTag(this.getChoiceType())) {
         in.skipTag();
         this.exceedingValue = this.readNumber(in, 0);
         this.statusFlags = in.readBitString(1);
         this.deadband = this.readNumber(in, 2);
         this.exceededLimit = this.readNumber(in, 3);
         tag = in.peekTag();
         if (in.isClosingTag(this.getChoiceType())) {
            in.skipTag();
         } else {
            throw new AsnException("Invalid tag: " + tag);
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   public Number readNumber(AsnInputStream in, int tag) throws AsnException {
      return in.readReal(tag);
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   public String getLongName() {
      return " out-of-range\n";
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(this.getLongName());
      sb.append("    " + this.getExceedingValue());
      sb.append("    " + this.getStatusFlags());
      sb.append("    " + this.getDeadband());
      sb.append("    " + this.getExceededLimit());
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".excVal." + this.getExceedingValue());
      sb.append(".sF." + this.getStatusFlags().toString(BacnetBitStringUtil.BacnetStatusFlags));
      sb.append(".dband." + this.getDeadband());
      sb.append(".excLim." + this.getExceededLimit());
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("exceedingValue", BString.make(String.valueOf(this.getExceedingValue())));
      map.put("statusFlags", BString.make(this.getStatusFlags().toString(BacnetBitStringUtil.BacnetStatusFlags)));
      map.put("deadband", BString.make(String.valueOf(this.getDeadband())));
      map.put("exceededLimit", BString.make(String.valueOf(this.getExceededLimit())));
   }
}
