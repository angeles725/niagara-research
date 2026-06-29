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

public class FloatingLimit extends BacnetNotificationParameters {
   public static final int REFERENCE_VALUE_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   public static final int SETPOINT_VALUE_TAG = 2;
   public static final int ERROR_LIMIT_TAG = 3;
   float referenceValue;
   BBacnetBitString statusFlags;
   float setpointValue;
   float errorLimit;

   public FloatingLimit() {
   }

   public FloatingLimit(float referenceValue, BBacnetBitString statusFlags, float setpointValue, float errorLimit) {
      this.referenceValue = referenceValue;
      this.statusFlags = statusFlags;
      this.setpointValue = setpointValue;
      this.errorLimit = errorLimit;
   }

   @Override
   public int getChoiceType() {
      return 4;
   }

   public float getReferenceValue() {
      return this.referenceValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public float getSetpointValue() {
      return this.setpointValue;
   }

   public float getErrorLimit() {
      return this.errorLimit;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(4);
      outputStream.writeReal(0, this.referenceValue);
      outputStream.writeBitString(1, this.statusFlags);
      outputStream.writeReal(2, this.setpointValue);
      outputStream.writeReal(3, this.errorLimit);
      outputStream.writeClosingTag(4);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(4)) {
         inputStream.skipTag();
         this.referenceValue = inputStream.readReal(0);
         this.statusFlags = inputStream.readBitString(1);
         this.setpointValue = inputStream.readReal(2);
         this.errorLimit = inputStream.readReal(3);
         tag = inputStream.peekTag();
         if (inputStream.isClosingTag(4)) {
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
      StringBuilder sb = new StringBuilder("  floating-limit\n");
      sb.append("    " + this.referenceValue);
      sb.append("    " + this.statusFlags);
      sb.append("    " + this.setpointValue);
      sb.append("    " + this.errorLimit);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".ref." + this.referenceValue);
      sb.append(".sF." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags));
      sb.append(".setpt." + this.setpointValue);
      sb.append(".lim." + this.errorLimit);
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("referenceValue", BString.make(String.valueOf(this.referenceValue)));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
      map.put("setpointValue", BString.make(String.valueOf(this.setpointValue)));
      map.put("errorLimit", BString.make(String.valueOf(this.errorLimit)));
   }
}
