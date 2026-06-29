package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPropertyValue;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.status.BStatus;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class ChangeOfReliability extends BacnetNotificationParameters {
   public static final int RELIABILITY_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   public static final int PROPERTY_VALUES_TAG = 2;
   private BBacnetReliability reliability = BBacnetReliability.DEFAULT;
   private BBacnetBitString statusFlags = BacnetBitStringUtil.getBacnetStatusFlags(BStatus.ok);
   private BBacnetPropertyValue[] values;

   public ChangeOfReliability() {
   }

   public ChangeOfReliability(BBacnetReliability reliability, BBacnetBitString statusFlags, BBacnetPropertyValue[] values) {
      this.reliability = reliability;
      this.statusFlags = statusFlags;
      this.values = values;
   }

   @Override
   public int getChoiceType() {
      return 19;
   }

   public BBacnetReliability getReliability() {
      return this.reliability;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public BBacnetPropertyValue[] getPropertyValues() {
      return this.values;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeOpeningTag(19);
      out.writeEnumerated(0, this.reliability);
      out.writeBitString(1, this.statusFlags);
      out.writeOpeningTag(2);
      if (this.values != null && this.values.length > 0) {
         for (BBacnetPropertyValue value : this.values) {
            value.writeAsn(out);
         }
      }

      out.writeClosingTag(2);
      out.writeClosingTag(19);
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.readEncoded(null, in);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream in) throws AsnException {
      in.skipOpeningTag(19);
      this.reliability = BBacnetReliability.make(in.readEnumerated(0));
      this.statusFlags = in.readBitString(1);
      in.skipOpeningTag(2);
      List<BBacnetPropertyValue> values = new ArrayList<>();

      do {
         BBacnetPropertyValue value = new BBacnetPropertyValue();
         value.readAsn(in);
         values.add(value);
      } while (!in.isClosingTag(2));

      in.skipTag();
      this.values = values.toArray(new BBacnetPropertyValue[0]);
      in.skipClosingTag(19);
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  change-of-reliability\n");
      sb.append("    ").append(this.reliability).append("    ").append(this.statusFlags);
      if (this.values != null) {
         for (BBacnetPropertyValue value : this.values) {
            sb.append("        ").append(value);
         }
      }

      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".reliability.").append(this.reliability).append(".sFlgs.").append(this.statusFlags).append(".values.");
      if (this.values != null) {
         for (BBacnetPropertyValue value : this.values) {
            sb.append("pv.").append(value);
         }
      }

      sb.append(".endValues.");
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("accessEvent", BString.make(String.valueOf(this.reliability)));
      map.put("statusFlags", BString.make(String.valueOf(this.statusFlags)));
      map.put("propertyValues", BString.make(String.valueOf(this.getValues(this.values))));
   }

   private StringBuffer getValues(BBacnetPropertyValue[] values) {
      if (values == null) {
         return null;
      } else {
         StringBuffer propertyValues = new StringBuffer();

         for (int i = 0; i < values.length; i++) {
            if (i == 0) {
               propertyValues.append(values[i].getValue());
            } else {
               propertyValues.append(", " + values[i].getValue());
            }
         }

         return propertyValues;
      }
   }
}
