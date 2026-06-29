package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetLifeSafetyMode;
import javax.baja.bacnet.enums.BBacnetLifeSafetyOperation;
import javax.baja.bacnet.enums.BBacnetLifeSafetyState;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class ChangeOfLifeSafety extends BacnetNotificationParameters {
   public static final int NEW_STATE_TAG = 0;
   public static final int NEW_MODE_TAG = 1;
   public static final int STATUS_FLAGS_TAG = 2;
   public static final int OPERATION_EXPECTED_TAG = 3;
   BBacnetLifeSafetyState newState;
   BBacnetLifeSafetyMode newMode;
   BBacnetBitString statusFlags = BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags"));
   BBacnetLifeSafetyOperation operationExpected;

   public ChangeOfLifeSafety() {
   }

   public ChangeOfLifeSafety(BBacnetLifeSafetyState ns, BBacnetLifeSafetyMode nm, BBacnetBitString sf, BBacnetLifeSafetyOperation oe) {
      this.newState = ns;
      this.newMode = nm;
      this.statusFlags = sf;
      this.operationExpected = oe;
   }

   @Override
   public int getChoiceType() {
      return 8;
   }

   public BBacnetLifeSafetyState getNewState() {
      return this.newState;
   }

   public BBacnetLifeSafetyMode getNewMode() {
      return this.newMode;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public BBacnetLifeSafetyOperation getOperationExpected() {
      return this.operationExpected;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(8);
      outputStream.writeEnumerated(0, this.newState.getOrdinal());
      outputStream.writeEnumerated(1, this.newMode.getOrdinal());
      outputStream.writeBitString(2, this.statusFlags);
      outputStream.writeEnumerated(3, this.operationExpected.getOrdinal());
      outputStream.writeClosingTag(8);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      inputStream.skipOpeningTag(8);
      this.newState = BBacnetLifeSafetyState.make(inputStream.readEnumerated(0));
      this.newMode = BBacnetLifeSafetyMode.make(inputStream.readEnumerated(1));
      this.statusFlags = inputStream.readBitString(2);
      this.operationExpected = BBacnetLifeSafetyOperation.make(inputStream.readEnumerated(3));
      inputStream.skipClosingTag(8);
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  change-of-life-safety\n");
      sb.append("    " + this.newState);
      sb.append("    " + this.newMode);
      sb.append("    " + this.statusFlags);
      sb.append("    " + this.operationExpected);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".newState." + this.newState);
      sb.append(".newMode." + this.newMode);
      sb.append(".status." + this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags));
      sb.append(".operationExpected." + this.operationExpected);
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("newState", BString.make(this.newState.getTag()));
      map.put("newMode", BString.make(this.newMode.getTag()));
      map.put("statusFlags", BString.make(this.statusFlags.toString(BacnetBitStringUtil.BacnetStatusFlags)));
      map.put("operationExpected", BString.make(this.operationExpected.getTag()));
   }
}
