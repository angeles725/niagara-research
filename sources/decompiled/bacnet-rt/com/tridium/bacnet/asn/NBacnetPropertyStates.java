package com.tridium.bacnet.asn;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetBinaryPv;
import javax.baja.bacnet.enums.BBacnetDeviceStatus;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetLifeSafetyMode;
import javax.baja.bacnet.enums.BBacnetLifeSafetyState;
import javax.baja.bacnet.enums.BBacnetPolarity;
import javax.baja.bacnet.enums.BBacnetProgramError;
import javax.baja.bacnet.enums.BBacnetProgramRequest;
import javax.baja.bacnet.enums.BBacnetProgramState;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BEnum;
import javax.baja.sys.Context;

public class NBacnetPropertyStates {
   public static final int BOOLEAN_VALUE_TAG = 0;
   public static final int BINARY_VALUE_TAG = 1;
   public static final int EVENT_TYPE_TAG = 2;
   public static final int POLARITY_TAG = 3;
   public static final int PROGRAM_CHANGE_TAG = 4;
   public static final int PROGRAM_STATE_TAG = 5;
   public static final int REASON_FOR_HALT_TAG = 6;
   public static final int RELIABILITY_TAG = 7;
   public static final int STATE_TAG = 8;
   public static final int SYSTEM_STATUS_TAG = 9;
   public static final int UNITS_TAG = 10;
   public static final int UNSIGNED_VALUE_TAG = 11;
   public static final int LIFE_SAFETY_MODE_TAG = 12;
   public static final int LIFE_SAFETY_STATE_TAG = 13;
   int choiceType;
   private boolean booleanValue;
   private long unsignedValue;
   private BEnum enumValue;

   public NBacnetPropertyStates() {
   }

   public NBacnetPropertyStates(boolean booleanValue) {
      this.choiceType = 0;
      this.booleanValue = booleanValue;
   }

   public NBacnetPropertyStates(BBacnetBinaryPv binaryValue) {
      this(1, binaryValue);
   }

   public NBacnetPropertyStates(BBacnetEventType eventType) {
      this(2, eventType);
   }

   public NBacnetPropertyStates(BBacnetPolarity polarity) {
      this(3, polarity);
   }

   public NBacnetPropertyStates(BBacnetProgramRequest programChange) {
      this(4, programChange);
   }

   public NBacnetPropertyStates(BBacnetProgramState programState) {
      this(5, programState);
   }

   public NBacnetPropertyStates(BBacnetProgramError reasonForHalt) {
      this(6, reasonForHalt);
   }

   public NBacnetPropertyStates(BBacnetReliability reliability) {
      this(7, reliability);
   }

   public NBacnetPropertyStates(BBacnetEventState state) {
      this(8, state);
   }

   public NBacnetPropertyStates(BBacnetDeviceStatus systemStatus) {
      this(9, systemStatus);
   }

   public NBacnetPropertyStates(BBacnetEngineeringUnits units) {
      this(10, units);
   }

   public NBacnetPropertyStates(long unsignedValue) {
      this.choiceType = 11;
      this.unsignedValue = unsignedValue;
   }

   public NBacnetPropertyStates(BBacnetLifeSafetyMode lifeSafetyMode) {
      this(12, lifeSafetyMode);
   }

   public NBacnetPropertyStates(BBacnetLifeSafetyState lifeSafetyState) {
      this(13, lifeSafetyState);
   }

   private NBacnetPropertyStates(int choice, BEnum e) {
      this.choiceType = choice;
      this.enumValue = e;
   }

   public int getChoiceType() {
      return this.choiceType;
   }

   public boolean getBooleanValue() {
      return this.booleanValue;
   }

   public BBacnetBinaryPv getBinaryValue() {
      return (BBacnetBinaryPv)this.enumValue;
   }

   public BEnum getEventType() {
      return this.enumValue;
   }

   public BBacnetPolarity getPolarity() {
      return (BBacnetPolarity)this.enumValue;
   }

   public BEnum getProgramRequest() {
      return this.enumValue;
   }

   public BEnum getProgramState() {
      return this.enumValue;
   }

   public BEnum getProgramError() {
      return this.enumValue;
   }

   public BEnum getReliability() {
      return this.enumValue;
   }

   public BEnum getEventState() {
      return this.enumValue;
   }

   public BEnum getSystemStatus() {
      return this.enumValue;
   }

   public BEnum getUnits() {
      return this.enumValue;
   }

   public long getUnsignedValue() {
      return this.unsignedValue;
   }

   public BEnum getLifeSafetyMode() {
      return this.enumValue;
   }

   public BEnum getLifeSafetyState() {
      return this.enumValue;
   }

   public void writeEncoded(AsnOutputStream outputStream) {
      switch (this.choiceType) {
         case 0:
            outputStream.writeBoolean(0, this.booleanValue);
            break;
         case 1:
            outputStream.writeEnumerated(1, this.enumValue.getOrdinal());
            break;
         case 2:
            outputStream.writeEnumerated(2, this.enumValue.getOrdinal());
            break;
         case 3:
            outputStream.writeEnumerated(3, this.enumValue.getOrdinal());
            break;
         case 4:
            outputStream.writeEnumerated(4, this.enumValue.getOrdinal());
            break;
         case 5:
            outputStream.writeEnumerated(5, this.enumValue.getOrdinal());
            break;
         case 6:
            outputStream.writeEnumerated(6, this.enumValue.getOrdinal());
            break;
         case 7:
            outputStream.writeEnumerated(7, this.enumValue.getOrdinal());
            break;
         case 8:
            outputStream.writeEnumerated(8, this.enumValue.getOrdinal());
            break;
         case 9:
            outputStream.writeEnumerated(9, this.enumValue.getOrdinal());
            break;
         case 10:
            outputStream.writeEnumerated(10, this.enumValue.getOrdinal());
            break;
         case 11:
            outputStream.writeUnsignedInteger(11, this.unsignedValue);
            break;
         case 12:
            outputStream.writeEnumerated(12, this.enumValue.getOrdinal());
            break;
         case 13:
            outputStream.writeEnumerated(13, this.enumValue.getOrdinal());
            break;
         default:
            throw new IllegalStateException();
      }
   }

   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      this.choiceType = inputStream.peekTag();
      BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(deviceId);
      BExtensibleEnumList enumList = BBacnetNetwork.localDevice().getEnumerationList();
      if (device != null) {
         enumList = device.getEnumerationList();
      }

      switch (this.choiceType) {
         case 0:
            this.booleanValue = inputStream.readBoolean(0);
            break;
         case 1:
            this.enumValue = BBacnetBinaryPv.make(inputStream.readEnumerated(1));
            break;
         case 2:
            int eventTypeOrdinal = inputStream.readEnumerated(2);
            this.enumValue = enumList.getEventType().getRange().get(eventTypeOrdinal);
            break;
         case 3:
            this.enumValue = BBacnetPolarity.make(inputStream.readEnumerated(3));
            break;
         case 4:
            this.enumValue = BBacnetProgramRequest.make(inputStream.readEnumerated(4));
            break;
         case 5:
            this.enumValue = BBacnetProgramState.make(inputStream.readEnumerated(5));
            break;
         case 6:
            int reasonForHaltOrdinal = inputStream.readEnumerated(6);
            this.enumValue = enumList.getProgramError().getRange().get(reasonForHaltOrdinal);
            break;
         case 7:
            int reliabilityOrdinal = inputStream.readEnumerated(7);
            this.enumValue = enumList.getReliability().getRange().get(reliabilityOrdinal);
            break;
         case 8:
            int stateOrdinal = inputStream.readEnumerated(8);
            this.enumValue = enumList.getEventState().getRange().get(stateOrdinal);
            break;
         case 9:
            int systemStatusOrdinal = inputStream.readEnumerated(9);
            this.enumValue = enumList.getDeviceStatus().getRange().get(systemStatusOrdinal);
            break;
         case 10:
            int unitsOrdinal = inputStream.readEnumerated(10);
            this.enumValue = enumList.getEngineeringUnits().getRange().get(unitsOrdinal);
            break;
         case 11:
            this.unsignedValue = inputStream.readUnsignedInteger(11);
            break;
         case 12:
            int lifeSafetyModeOrdinal = inputStream.readEnumerated(12);
            this.enumValue = enumList.getLifeSafetyMode().getRange().get(lifeSafetyModeOrdinal);
            break;
         case 13:
            int lifeSafetyStateOrdinal = inputStream.readEnumerated(13);
            this.enumValue = enumList.getLifeSafetyState().getRange().get(lifeSafetyStateOrdinal);
            break;
         default:
            throw new IllegalStateException();
      }
   }

   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("NBacnetPropertyStates: \n");
      switch (this.choiceType) {
         case 0:
            sb.append("  booleanValue: " + this.booleanValue);
            break;
         case 1:
            sb.append("  binaryValue: " + this.enumValue);
            break;
         case 2:
            sb.append("  eventType: " + this.enumValue);
            break;
         case 3:
            sb.append("  polarity: " + this.enumValue);
            break;
         case 4:
            sb.append("  programChange: " + this.enumValue);
            break;
         case 5:
            sb.append("  programState: " + this.enumValue);
            break;
         case 6:
            sb.append("  programError: " + this.enumValue);
            break;
         case 7:
            sb.append("  reliability: " + this.enumValue);
            break;
         case 8:
            sb.append("  state: " + this.enumValue);
            break;
         case 9:
            sb.append("  systemStatus: " + this.enumValue);
            break;
         case 10:
            sb.append("  units: " + this.enumValue);
            break;
         case 11:
            sb.append("  unsignedValue: " + this.unsignedValue);
            break;
         case 12:
            sb.append("  lifeSafetyMode: " + this.enumValue);
            break;
         case 13:
            sb.append("  lifeSafetyState: " + this.enumValue);
      }

      return sb.toString();
   }

   public String toFacetString() {
      StringBuilder sb = new StringBuilder();
      switch (this.choiceType) {
         case 0:
            sb.append("booleanValue-" + this.booleanValue);
            break;
         case 1:
            sb.append("binaryValue-" + this.enumValue);
            break;
         case 2:
            sb.append("eventType-" + this.enumValue);
            break;
         case 3:
            sb.append("polarity-" + this.enumValue);
            break;
         case 4:
            sb.append("programChange-" + this.enumValue);
            break;
         case 5:
            sb.append("programState-" + this.enumValue);
            break;
         case 6:
            sb.append("programError-" + this.enumValue);
            break;
         case 7:
            sb.append("reliability-" + this.enumValue);
            break;
         case 8:
            sb.append("state-" + this.enumValue);
            break;
         case 9:
            sb.append("systemStatus-" + this.enumValue);
            break;
         case 10:
            sb.append("units-" + this.enumValue);
            break;
         case 11:
            sb.append("unsignedValue-" + this.unsignedValue);
            break;
         case 12:
            sb.append("lifeSafetyMode-" + this.enumValue);
            break;
         case 13:
            sb.append("lifeSafetyState-" + this.enumValue);
      }

      return sb.toString();
   }
}
