package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import com.tridium.bacnet.asn.NBacnetPropertyStates;
import java.util.HashMap;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class ChangeOfState extends BacnetNotificationParameters {
   public static final int NEW_STATE_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   NBacnetPropertyStates newState;
   BBacnetBitString statusFlags;

   public ChangeOfState() {
   }

   public ChangeOfState(NBacnetPropertyStates newState, BBacnetBitString statusFlags) {
      this.newState = newState;
      this.statusFlags = statusFlags;
   }

   @Override
   public int getChoiceType() {
      return 1;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public NBacnetPropertyStates getNewState() {
      return this.newState;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(1);
      outputStream.writeOpeningTag(0);
      this.newState.writeEncoded(outputStream);
      outputStream.writeClosingTag(0);
      outputStream.writeBitString(1, this.statusFlags);
      outputStream.writeClosingTag(1);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(1)) {
         inputStream.skipTag();
         tag = inputStream.peekTag();
         if (inputStream.isOpeningTag(0)) {
            inputStream.skipTag();
            this.newState = new NBacnetPropertyStates();
            this.newState.readEncoded(deviceId, inputStream);
            tag = inputStream.peekTag();
            if (inputStream.isClosingTag(0)) {
               inputStream.skipTag();
               this.statusFlags = inputStream.readBitString(1);
               tag = inputStream.peekTag();
               if (inputStream.isClosingTag(1)) {
                  inputStream.skipTag();
               } else {
                  throw new AsnException("Invalid tag: " + tag);
               }
            } else {
               throw new AsnException("Invalid tag: " + tag);
            }
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
      StringBuilder sb = new StringBuilder("  change-of-state\n");
      sb.append("    " + this.newState);
      sb.append("    " + this.statusFlags);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".state." + this.newState.toString(BacnetConst.facetsContext));
      sb.append(".sF." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags));
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("newState", BString.make(this.newState.toString()));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
   }
}
