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

public class UnsignedRange extends BacnetNotificationParameters {
   public static final int EXCEEDING_VALUE_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   public static final int EXCEEDING_LIMIT_TAG = 2;
   long exceedingValue;
   BBacnetBitString statusFlags;
   long exceedingLimit;

   public UnsignedRange() {
   }

   public UnsignedRange(long exceedingValue, BBacnetBitString statusFlags, long exceedingLimit) {
      this.exceedingValue = exceedingValue;
      this.statusFlags = statusFlags;
      this.exceedingLimit = exceedingLimit;
   }

   @Override
   public int getChoiceType() {
      return 11;
   }

   public long getExceedingValue() {
      return this.exceedingValue;
   }

   public void setExceedingValue(long exceedingValue) {
      this.exceedingValue = exceedingValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public void setStatusFlags(BBacnetBitString statusFlags) {
      this.statusFlags = statusFlags;
   }

   public long getExceedingLimit() {
      return this.exceedingLimit;
   }

   public void setExceedingLimit(long exceedingLimit) {
      this.exceedingLimit = exceedingLimit;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(11);
      outputStream.writeUnsignedInteger(0, this.exceedingValue);
      outputStream.writeBitString(1, this.statusFlags);
      outputStream.writeUnsignedInteger(2, this.exceedingLimit);
      outputStream.writeClosingTag(11);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(11)) {
         inputStream.skipTag();
         this.exceedingValue = inputStream.readUnsignedInteger(0);
         this.statusFlags = inputStream.readBitString(1);
         this.exceedingLimit = inputStream.readUnsignedInteger(2);
         tag = inputStream.peekTag();
         if (inputStream.isClosingTag(11)) {
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
      StringBuilder sb = new StringBuilder("  unsigned-range\n");
      sb.append("    " + this.exceedingValue);
      sb.append("    " + this.statusFlags);
      sb.append("    " + this.exceedingLimit);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".exVal." + this.exceedingValue);
      sb.append(".stFla." + this.statusFlags);
      sb.append(".exLim." + this.exceedingLimit);
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("exceedingValue", BString.make(String.valueOf(this.exceedingValue)));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
      map.put("exceededLimit", BString.make(String.valueOf(this.exceedingLimit)));
   }
}
