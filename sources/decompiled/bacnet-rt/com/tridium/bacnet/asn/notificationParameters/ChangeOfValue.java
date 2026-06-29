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

public class ChangeOfValue extends BacnetNotificationParameters {
   public static final int NEW_VALUE_TAG = 0;
   public static final int CHANGED_BITS_TAG = 0;
   public static final int CHANGED_VALUE_TAG = 1;
   public static final int STATUS_FLAGS_TAG = 1;
   boolean isValue;
   BBacnetBitString changedBits;
   float changedValue;
   BBacnetBitString statusFlags;

   public ChangeOfValue() {
   }

   public ChangeOfValue(BBacnetBitString changedBits, BBacnetBitString statusFlags) {
      this.isValue = false;
      this.changedBits = changedBits;
      this.statusFlags = statusFlags;
   }

   public ChangeOfValue(float changedValue, BBacnetBitString statusFlags) {
      this.isValue = true;
      this.changedValue = changedValue;
      this.statusFlags = statusFlags;
   }

   @Override
   public int getChoiceType() {
      return 2;
   }

   public boolean isValue() {
      return this.isValue;
   }

   public BBacnetBitString getChangedBits() {
      return this.changedBits;
   }

   public float getChangedValue() {
      return this.changedValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(2);
      outputStream.writeOpeningTag(0);
      if (this.isValue) {
         outputStream.writeReal(1, this.changedValue);
      } else {
         outputStream.writeBitString(0, this.changedBits);
      }

      outputStream.writeBitString(1, this.statusFlags);
      outputStream.writeClosingTag(0);
      outputStream.writeClosingTag(2);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      inputStream.skipOpeningTag(2);
      inputStream.skipOpeningTag(0);
      if (inputStream.isValueTag(0)) {
         this.isValue = false;
         this.changedBits = inputStream.readBitString(0);
      } else {
         this.isValue = true;
         this.changedValue = inputStream.readReal(1);
      }

      inputStream.skipClosingTag(0);
      this.statusFlags = inputStream.readBitString(1);
      inputStream.skipClosingTag(2);
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  change-of-value\n");
      if (this.isValue) {
         sb.append("    " + this.changedValue);
      } else {
         sb.append("    " + this.changedBits);
      }

      sb.append("    " + this.statusFlags);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      if (this.isValue) {
         sb.append(".val." + this.changedValue);
      } else {
         sb.append(".bits." + this.changedBits.toString(BacnetConst.facetsContext));
      }

      sb.append(".sF." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags));
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      if (this.isValue) {
         map.put("changedValue", BString.make(String.valueOf(this.changedValue)));
      } else {
         map.put("changedBits", BString.make(this.changedBits.toString()));
      }

      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
   }
}
