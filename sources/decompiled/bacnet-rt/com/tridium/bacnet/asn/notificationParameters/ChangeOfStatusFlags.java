package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetAny;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class ChangeOfStatusFlags extends BacnetNotificationParameters {
   public static final int PRESENT_VALUE_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   BBacnetAny presentValue;
   BBacnetBitString statusFlags;

   public ChangeOfStatusFlags() {
   }

   public ChangeOfStatusFlags(BBacnetAny presentValue, BBacnetBitString statusFlags) {
      this.presentValue = presentValue;
      this.statusFlags = statusFlags;
   }

   @Override
   public int getChoiceType() {
      return 18;
   }

   public BBacnetAny getPresentValue() {
      return this.presentValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeOpeningTag(18);
      if (this.presentValue != null) {
         out.writeOpeningTag(0);
         this.presentValue.writeAsn(out);
         out.writeClosingTag(0);
      }

      out.writeBitString(1, this.statusFlags);
      out.writeClosingTag(18);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream in) throws AsnException {
      in.skipOpeningTag(18);
      BBacnetAny presentValue = null;
      in.peekTag();
      if (in.isOpeningTag(0)) {
         in.skipTag();
         presentValue = new BBacnetAny();
         presentValue.readAsn(in);
         in.skipClosingTag(0);
      }

      BBacnetBitString statusFlags = in.readBitString(1);
      in.skipClosingTag(18);
      this.presentValue = presentValue;
      this.statusFlags = statusFlags;
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  change-of-status-flags\n");
      sb.append("    " + this.presentValue);
      sb.append("    " + this.statusFlags);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".state." + (this.presentValue == null ? "null" : this.presentValue.toString(BacnetConst.facetsContext)));
      sb.append(".sF." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags));
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("presentValue", BString.make(this.presentValue == null ? "null" : this.presentValue.toString()));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
   }
}
