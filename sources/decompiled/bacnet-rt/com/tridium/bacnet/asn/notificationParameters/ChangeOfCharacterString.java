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

public class ChangeOfCharacterString extends BacnetNotificationParameters {
   public static final int CHANGED_VALUE_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   public static final int ALARM_VALUE_TAG = 2;
   String changedValue;
   BBacnetBitString statusFlags;
   String alarmValue;

   public ChangeOfCharacterString() {
   }

   public ChangeOfCharacterString(String changedValue, BBacnetBitString statusFlags, String alarmValue) {
      this.changedValue = changedValue;
      this.statusFlags = statusFlags;
      this.alarmValue = alarmValue;
   }

   @Override
   public int getChoiceType() {
      return 17;
   }

   public String getChangedValue() {
      return this.changedValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public String getAlarmValue() {
      return this.alarmValue;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(17);
      outputStream.writeCharacterString(0, this.changedValue);
      outputStream.writeBitString(1, this.statusFlags);
      outputStream.writeCharacterString(2, this.alarmValue);
      outputStream.writeClosingTag(17);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(17)) {
         inputStream.skipTag();
         this.changedValue = inputStream.readCharacterString(0);
         this.statusFlags = inputStream.readBitString(1);
         this.alarmValue = inputStream.readCharacterString(2);
         inputStream.peekTag();
         if (inputStream.isClosingTag(17)) {
            inputStream.skipTag();
         } else {
            throw new AsnException("Invalid tag: " + tag);
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  change-of-characterstring\n");
      sb.append("    " + this.changedValue).append("    " + this.statusFlags).append("    " + this.alarmValue);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".changedValue")
         .append(this.changedValue)
         .append(".sF." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags))
         .append(".alarmValue.")
         .append(this.alarmValue);
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("changedValue", BString.make(this.changedValue));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
      map.put("alarmValue", BString.make(this.alarmValue));
   }
}
