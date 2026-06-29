package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class CommandFailure extends BacnetNotificationParameters {
   public static final int COMMAND_VALUE_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   public static final int FEEDBACK_VALUE_TAG = 2;
   byte[] commandValue;
   BBacnetBitString statusFlags;
   byte[] feedbackValue;
   private static final Logger logger = Logger.getLogger("bacnet.asn");

   public CommandFailure() {
   }

   public CommandFailure(byte[] commandValue, BBacnetBitString statusFlags, byte[] feedbackValue) {
      this.commandValue = commandValue;
      this.statusFlags = statusFlags;
      this.feedbackValue = feedbackValue;
   }

   @Override
   public int getChoiceType() {
      return 3;
   }

   public byte[] getCommandValue() {
      return this.commandValue;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public byte[] getFeedbackValue() {
      return this.feedbackValue;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(3);
      outputStream.writeEncodedValue(0, this.commandValue);
      outputStream.writeBitString(1, this.statusFlags);
      outputStream.writeEncodedValue(2, this.feedbackValue);
      outputStream.writeClosingTag(3);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(3)) {
         inputStream.skipTag();
         this.commandValue = inputStream.readEncodedValue(0);
         this.statusFlags = inputStream.readBitString(1);
         this.feedbackValue = inputStream.readEncodedValue(2);
         tag = inputStream.peekTag();
         if (inputStream.isClosingTag(3)) {
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
      StringBuilder sb = new StringBuilder("  command-failure\n");
      sb.append("    " + ByteArrayUtil.toHexString(this.commandValue));
      sb.append("    " + this.statusFlags);
      sb.append("    " + ByteArrayUtil.toHexString(this.feedbackValue));
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".cmdVal." + ByteArrayUtil.toHexString(this.commandValue));
      sb.append(".sF." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags));
      sb.append(".fbVal." + ByteArrayUtil.toHexString(this.feedbackValue));
      return sb.toString().replace(' ', '.');
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("commandValue", BString.make(ByteArrayUtil.toHexString(this.commandValue)));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
      map.put("feedbackValue", BString.make(ByteArrayUtil.toHexString(this.feedbackValue)));
   }
}
