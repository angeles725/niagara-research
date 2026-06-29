package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.status.BStatus;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;

public class ChangeOfDiscreteValue extends BacnetNotificationParameters {
   private static final int NEW_VALUE_TAG = 0;
   private static final int STATUS_FLAGS_TAG = 1;
   private static final int NEW_VALUE_DATE_TIME_TAG = 0;
   private BValue newValue;
   private BBacnetBitString statusFlags = BacnetBitStringUtil.getBacnetStatusFlags(BStatus.ok);

   public ChangeOfDiscreteValue() {
   }

   public ChangeOfDiscreteValue(BValue newValue, BBacnetBitString statusFlags) {
      this.newValue = newValue;
      this.statusFlags = statusFlags;
   }

   @Override
   public int getChoiceType() {
      return 21;
   }

   public BValue getNewValue() {
      return this.newValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeOpeningTag(21);
      this.writeNewValue(out);
      out.writeBitString(1, this.getStatusFlags());
      out.writeClosingTag(21);
   }

   private void writeNewValue(AsnOutputStream out) {
      out.writeOpeningTag(0);
      if (this.newValue instanceof BBoolean) {
         out.writeBoolean(((BBoolean)this.newValue).getBoolean());
      } else if (this.newValue instanceof BBacnetUnsigned) {
         out.writeUnsignedInteger(((BBacnetUnsigned)this.newValue).getUnsigned());
      } else if (this.newValue instanceof BInteger) {
         out.writeSignedInteger(((BInteger)this.newValue).getInt());
      } else if (this.newValue instanceof BEnum) {
         out.writeEnumerated(((BEnum)this.newValue).getOrdinal());
      } else if (this.newValue instanceof BString) {
         out.writeCharacterString(((BString)this.newValue).getString());
      } else if (this.newValue instanceof BBacnetOctetString) {
         out.writeOctetString((BBacnetOctetString)this.newValue);
      } else if (this.newValue instanceof BBacnetDate) {
         out.writeDate((BBacnetDate)this.newValue);
      } else if (this.newValue instanceof BBacnetTime) {
         out.writeTime((BBacnetTime)this.newValue);
      } else if (this.newValue instanceof BBacnetObjectIdentifier) {
         out.writeObjectIdentifier((BBacnetObjectIdentifier)this.newValue);
      } else {
         if (!(this.newValue instanceof BBacnetDateTime)) {
            throw new IllegalStateException("Invalid ChangeOfDiscreteValue.newValue type: " + (this.newValue != null ? this.newValue.getType() : "null"));
         }

         out.writeOpeningTag(0);
         ((BBacnetDateTime)this.newValue).writeAsn(out);
         out.writeClosingTag(0);
      }

      out.writeClosingTag(0);
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.readEncoded(null, in);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream in) throws AsnException {
      in.skipOpeningTag(21);
      this.readNewValue(in);
      this.statusFlags = in.readBitString(1);
      in.skipClosingTag(21);
   }

   private void readNewValue(AsnInputStream in) throws AsnException {
      in.skipOpeningTag(0);
      int tag = in.peekTag();
      switch (tag) {
         case 1:
            this.newValue = BBoolean.make(in.readBoolean());
            break;
         case 2:
            this.newValue = in.readUnsigned();
            break;
         case 3:
            this.newValue = in.readSigned();
            break;
         case 4:
         case 5:
         case 8:
         default:
            in.skipOpeningTag(0);
            this.newValue = new BBacnetDateTime();
            ((BBacnetDateTime)this.newValue).readAsn(in);
            in.skipClosingTag(0);
            break;
         case 6:
            this.newValue = BBacnetOctetString.make(in.readOctetString());
            break;
         case 7:
            this.newValue = BString.make(in.readCharacterString());
            break;
         case 9:
            this.newValue = BDynamicEnum.make(in.readEnumerated());
            break;
         case 10:
            this.newValue = in.readDate();
            break;
         case 11:
            this.newValue = in.readTime();
            break;
         case 12:
            this.newValue = in.readObjectIdentifier();
      }

      in.skipClosingTag(0);
   }

   @Override
   public String toString() {
      return "  change-of-discrete-value\n    " + this.newValue + "    " + this.statusFlags;
   }

   @Override
   public String toFacetString() {
      return "evtVals.newValue." + this.newValue + ".statusFlags." + this.statusFlags;
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("newValue", BString.make(String.valueOf(this.newValue)));
      map.put("statusFlags", BString.make(this.getStatusFlags().toString(BacnetBitStringUtil.BacnetStatusFlags)));
   }
}
