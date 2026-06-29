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

public class ChangeOfBitstring extends BacnetNotificationParameters {
   public static final int REFERENCED_BITSTRING_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   BBacnetBitString referencedBitstring;
   BBacnetBitString statusFlags;

   public ChangeOfBitstring() {
   }

   public ChangeOfBitstring(BBacnetBitString referencedBitstring, BBacnetBitString statusFlags) {
      this.referencedBitstring = referencedBitstring;
      this.statusFlags = statusFlags;
   }

   @Override
   public int getChoiceType() {
      return 0;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public BBacnetBitString getReferencedBitstring() {
      return this.referencedBitstring;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(0);
      outputStream.writeBitString(0, this.referencedBitstring);
      outputStream.writeBitString(1, this.statusFlags);
      outputStream.writeClosingTag(0);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(0)) {
         inputStream.skipTag();
         this.referencedBitstring = inputStream.readBitString(0);
         this.statusFlags = inputStream.readBitString(1);
         inputStream.peekTag();
         if (inputStream.isClosingTag(0)) {
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
      StringBuilder sb = new StringBuilder("  change-of-bitstring\n");
      sb.append("    " + this.referencedBitstring);
      sb.append("    " + this.statusFlags);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".refBS." + this.referencedBitstring.toString(BacnetConst.facetsContext));
      sb.append(".sF." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags));
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("referencedBitstring", BString.make(this.referencedBitstring.toString()));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
   }
}
