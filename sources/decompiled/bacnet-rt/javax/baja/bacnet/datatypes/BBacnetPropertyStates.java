package javax.baja.bacnet.datatypes;

import java.util.HashMap;
import java.util.Map;
import javax.baja.bacnet.enums.BBacnetAction;
import javax.baja.bacnet.enums.BBacnetBackupState;
import javax.baja.bacnet.enums.BBacnetBinaryPv;
import javax.baja.bacnet.enums.BBacnetDeviceStatus;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetFileAccessMethod;
import javax.baja.bacnet.enums.BBacnetLifeSafetyMode;
import javax.baja.bacnet.enums.BBacnetLifeSafetyOperation;
import javax.baja.bacnet.enums.BBacnetLifeSafetyState;
import javax.baja.bacnet.enums.BBacnetMaintenance;
import javax.baja.bacnet.enums.BBacnetNodeType;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetPolarity;
import javax.baja.bacnet.enums.BBacnetProgramError;
import javax.baja.bacnet.enums.BBacnetProgramRequest;
import javax.baja.bacnet.enums.BBacnetProgramState;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.enums.BBacnetRestartReason;
import javax.baja.bacnet.enums.BBacnetShedState;
import javax.baja.bacnet.enums.BBacnetSilencedState;
import javax.baja.bacnet.enums.BBacnetWriteStatus;
import javax.baja.bacnet.enums.access.BBacnetAccessCredentialDisable;
import javax.baja.bacnet.enums.access.BBacnetAccessCredentialDisableReason;
import javax.baja.bacnet.enums.access.BBacnetAccessEvent;
import javax.baja.bacnet.enums.access.BBacnetAccessZoneOccupancyState;
import javax.baja.bacnet.enums.access.BBacnetAuthenticationStatus;
import javax.baja.bacnet.enums.access.BBacnetDoorAlarmState;
import javax.baja.bacnet.enums.access.BBacnetDoorSecuredStatus;
import javax.baja.bacnet.enums.access.BBacnetDoorStatus;
import javax.baja.bacnet.enums.access.BBacnetDoorValue;
import javax.baja.bacnet.enums.access.BBacnetLockStatus;
import javax.baja.bacnet.enums.lighting.BBacnetLightingInProgress;
import javax.baja.bacnet.enums.lighting.BBacnetLightingOperation;
import javax.baja.bacnet.enums.lighting.BBacnetLightingTransition;
import javax.baja.bacnet.enums.security.BBacnetSecurityLevel;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.virtual.BBacnetVirtualProperty;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.category.BCategoryMask;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BInteger;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "choice",
   type = "int",
   defaultValue = "0",
   flags = 4
)
public final class BBacnetPropertyStates extends BComponent implements BIBacnetDataType {
   public static final Property choice = newProperty(4, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetPropertyStates.class);
   private static final int MAX_TAG = 254;
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
   public static final int RESTART_REASON_TAG = 14;
   public static final int DOOR_ALARM_TAG = 15;
   public static final int ACTION_TAG = 16;
   public static final int DOOR_SECURED_STATUS_TAG = 17;
   public static final int DOOR_STATUS_TAG = 18;
   public static final int DOOR_VALUE_TAG = 19;
   public static final int FILE_ACCESS_METHOD_TAG = 20;
   public static final int LOCK_STATUS_TAG = 21;
   public static final int LIFE_SAFETY_OPERATION_TAG = 22;
   public static final int MAINTENANCE_TAG = 23;
   public static final int NODE_TYPE_TAG = 24;
   public static final int NOTIFY_TYPE_TAG = 25;
   public static final int SECURITY_LEVEL_TAG = 26;
   public static final int SHED_STATE_TAG = 27;
   public static final int SILENCED_STATE_TAG = 28;
   public static final int RESERVED29_TAG = 29;
   public static final int ACCESS_EVENT_TAG = 30;
   public static final int ZONE_OCCUPANCY_TAG = 31;
   public static final int ACCESS_CREDENTIAL_DISABLE_REASON_TAG = 32;
   public static final int ACCESS_CREDENTIAL_DISABLE_TAG = 33;
   public static final int AUTHENTICATION_STATUS_TAG = 34;
   public static final int BACKUP_STATE_TAG = 36;
   public static final int WRITE_STATUS_TAG = 37;
   public static final int LIGHTING_IN_PROGRESS_TAG = 38;
   public static final int LIGHTING_OPERATION_TAG = 39;
   public static final int LIGHTING_TRANSITION_TAG = 40;
   public static final int INTEGER_VALUE_TAG = 41;
   private static final int MAX_DEFINED_CHOICE = 41;
   private static final int MAX_ASHRAE_CHOICE = 63;
   private static final int MAX_CHOICE = 42949;
   private static final Map<BTypeSpec, Integer> choices = makeChoicesMap();
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static final String[] tags = new String[]{
      lex.getText("BacnetPropertyStates.booleanValue"),
      lex.getText("BacnetPropertyStates.binaryValue"),
      lex.getText("BacnetPropertyStates.eventType"),
      lex.getText("BacnetPropertyStates.polarity"),
      lex.getText("BacnetPropertyStates.programChange"),
      lex.getText("BacnetPropertyStates.programState"),
      lex.getText("BacnetPropertyStates.reasonForHalt"),
      lex.getText("BacnetPropertyStates.reliability"),
      lex.getText("BacnetPropertyStates.state"),
      lex.getText("BacnetPropertyStates.systemStatus"),
      lex.getText("BacnetPropertyStates.units"),
      lex.getText("BacnetPropertyStates.unsignedValue"),
      lex.getText("BacnetPropertyStates.lifeSafetyMode"),
      lex.getText("BacnetPropertyStates.lifeSafetyState"),
      lex.getText("BacnetPropertyStates.restartReason"),
      lex.getText("BacnetPropertyStates.doorAlarmState"),
      lex.getText("BacnetPropertyStates.action"),
      lex.getText("BacnetPropertyStates.doorSecuredStatus"),
      lex.getText("BacnetPropertyStates.doorStatus"),
      lex.getText("BacnetPropertyStates.doorValue"),
      lex.getText("BacnetPropertyStates.fileAccessMethod"),
      lex.getText("BacnetPropertyStates.lockStatus"),
      lex.getText("BacnetPropertyStates.lifeSafetyOperation"),
      lex.getText("BacnetPropertyStates.maintenance"),
      lex.getText("BacnetPropertyStates.nodeType"),
      lex.getText("BacnetPropertyStates.notifyType"),
      lex.getText("BacnetPropertyStates.securityLevel"),
      lex.getText("BacnetPropertyStates.shedState"),
      lex.getText("BacnetPropertyStates.silencedState"),
      lex.getText("BacnetPropertyStates.reserved29"),
      lex.getText("BacnetPropertyStates.accessEvent"),
      lex.getText("BacnetPropertyStates.zoneOccupancyState"),
      lex.getText("BacnetPropertyStates.accessCredentialDisableReason"),
      lex.getText("BacnetPropertyStates.accessCredentialDisable"),
      lex.getText("BacnetPropertyStates.authenticationStatus"),
      lex.getText("BacnetPropertyStates.reserved35"),
      lex.getText("BacnetPropertyStates.backupState"),
      lex.getText("BacnetPropertyStates.writeStatus"),
      lex.getText("BacnetPropertyStates.lightingInProgress"),
      lex.getText("BacnetPropertyStates.lightingOperation"),
      lex.getText("BacnetPropertyStates.lightingTransition"),
      lex.getText("BacnetPropertyStates.integerValue"),
      lex.getText("BacnetPropertyStates.ashrae"),
      lex.getText("BacnetPropertyStates.proprietary"),
      lex.getText("BacnetPropertyStates.invalid")
   };
   private static final int ASHRAE_CHOICE_INDEX = 42;
   private static final int PROPRIETARY_CHOICE_INDEX = 43;
   private static final int INVALID_CHOICE_INDEX = 44;
   public static final String BOOLEAN_VALUE_SLOT_NAME = "booleanValue";
   public static final String BINARY_VALUE_SLOT_NAME = "binaryValue";
   public static final String UNSIGNED_VALUE_SLOT_NAME = "unsignedValue";
   private static final String[] slotNames = new String[]{
      "booleanValue",
      "binaryValue",
      "eventType",
      "polarity",
      "programChange",
      "programState",
      "reasonForHalt",
      "reliability",
      "state",
      "systemStatus",
      "units",
      "unsignedValue",
      "lifeSafetyMode",
      "lifeSafetyState",
      "restartReason",
      "doorAlarmState",
      "action",
      "doorSecuredStatus",
      "doorStatus",
      "doorValue",
      "fileAccessMethod",
      "lockStatus",
      "lifeSafetyOperation",
      "maintenance",
      "nodeType",
      "notifyType",
      "securityLevel",
      "shedState",
      "silencedState",
      "reserved29",
      "accessEvent",
      "zoneOccupancyState",
      "accessCredentialDisableReason",
      "accessCredentialDisable",
      "authenticationStatus",
      "reserved35",
      "backupState",
      "reserved37",
      "lightingInProgress",
      "lightingOperation",
      "lightingTransition",
      "integerValue",
      "ashrae",
      "proprietary"
   };

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BBacnetPropertyStates makeBoolean(boolean value) {
      BBacnetPropertyStates propertyStates = new BBacnetPropertyStates();
      propertyStates.setChoice(0);
      propertyStates.add(slotNames[0], BBoolean.make(value));
      return propertyStates;
   }

   public static BBacnetPropertyStates makeBinaryPv(boolean value) {
      BBacnetPropertyStates propertyStates = new BBacnetPropertyStates();
      propertyStates.setChoice(1);
      propertyStates.add(slotNames[1], BBacnetBinaryPv.make(value));
      return propertyStates;
   }

   public static BBacnetPropertyStates makeUnsigned(long value) {
      BBacnetPropertyStates propertyStates = new BBacnetPropertyStates();
      propertyStates.setChoice(11);
      propertyStates.add(slotNames[11], BBacnetUnsigned.make(value));
      return propertyStates;
   }

   public static BBacnetPropertyStates makeInteger(int value) {
      BBacnetPropertyStates propertyStates = new BBacnetPropertyStates();
      propertyStates.setChoice(41);
      propertyStates.add(slotNames[41], BInteger.make(value));
      return propertyStates;
   }

   public static BBacnetPropertyStates makeEnum(BTypeSpec typeSpec, int ordinal) {
      Integer choice = choices.get(typeSpec);
      if (choice == null) {
         throw new IllegalStateException("BACnetPropertyStates enum type is not supported: " + typeSpec);
      } else {
         BValue value;
         try {
            value = makeEnumValue(choice, ordinal);
         } catch (OutOfRangeException var5) {
            throw new IllegalStateException("BACnetPropertyStates enum type is not supported: " + typeSpec, var5);
         }

         BBacnetPropertyStates propertyStates = new BBacnetPropertyStates();
         propertyStates.setChoice(choice);
         propertyStates.add(slotNames[choice], value);
         return propertyStates;
      }
   }

   public void changed(Property p, Context cx) {
      if (this.isRunning()) {
         BComplex parent = this.getParent();
         if (parent != null) {
            parent.asComponent().changed(this.getPropertyInParent(), cx);
         }
      }
   }

   public void subscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childSubscribed(this);
      }
   }

   public void unsubscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childUnsubscribed(this);
      }
   }

   public BCategoryMask getAppliedCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getAppliedCategoryMask() : super.getAppliedCategoryMask();
   }

   public BCategoryMask getCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getCategoryMask() : super.getCategoryMask();
   }

   public BPermissions getPermissions(Context cx) {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getPermissions(cx) : super.getPermissions(cx);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getTag());
      int choice = this.getChoice();
      if (choice >= 0 && choice != 63) {
         if (choice <= 41) {
            sb.append(this.get(slotNames[choice]));
         } else if (choice < 63) {
            sb.append(this.get(slotNames[42]));
         } else if (choice <= 254) {
            sb.append(this.get(slotNames[43]));
         } else {
            sb.append(this.get(slotNames[42]));
         }

         return sb.toString();
      } else {
         return sb.toString();
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      int choice = this.getChoice();
      if (choice < 0) {
         throw new IllegalStateException("Invalid BACnetPropertyStates choice: " + this.getChoice());
      } else if (choice == 63) {
         throw new IllegalStateException("Choice value 63 reserved to extend support for tag number greater than 254");
      } else {
         if (choice <= 41) {
            switch (choice) {
               case 0:
                  out.writeBoolean(0, (BBoolean)this.get(slotNames[choice]));
                  break;
               case 1:
               case 2:
               case 3:
               case 4:
               case 5:
               case 6:
               case 7:
               case 8:
               case 9:
               case 10:
               case 12:
               case 13:
               case 14:
               case 15:
               case 16:
               case 17:
               case 18:
               case 19:
               case 20:
               case 21:
               case 22:
               case 23:
               case 24:
               case 25:
               case 26:
               case 27:
               case 28:
               case 29:
               case 30:
               case 31:
               case 32:
               case 33:
               case 34:
               case 36:
               case 37:
               case 38:
               case 39:
               case 40:
                  out.writeEnumerated(choice, (BEnum)this.get(slotNames[choice]));
                  break;
               case 11:
                  out.writeUnsigned(11, (BBacnetUnsigned)this.get(slotNames[choice]));
               case 35:
               default:
                  break;
               case 41:
                  out.writeSignedInteger(choice, (BInteger)this.get(slotNames[choice]));
            }
         } else if (choice < 63) {
            out.writeUnsigned(choice, (BBacnetUnsigned)this.get(slotNames[42]));
         } else if (choice <= 254) {
            out.writeUnsigned(choice, (BBacnetUnsigned)this.get(slotNames[43]));
         } else {
            if (choice > 42949) {
               throw new IllegalStateException(
                  "BACnetPropertyStates choice " + choice + " is too large to be encoded by ASN; choice should be less than " + 'Ʂ'
               );
            }

            long value = ((BBacnetUnsigned)this.get(slotNames[42])).getLong();
            long extendedValue = choice * 100000L + value;
            out.writeUnsigned(63, BBacnetUnsigned.make(extendedValue));
         }
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (tag >= 0 && tag <= 254) {
         int choice = tag;
         String slotName;
         BValue value;
         if (tag <= 41) {
            slotName = slotNames[tag];
            switch (tag) {
               case 0:
                  value = BBoolean.make(in.readBoolean(0));
                  break;
               case 11:
                  value = in.readUnsigned(11);
                  break;
               case 41:
                  value = in.readSigned(41);
                  break;
               default:
                  value = makeEnumValue(tag, in.readEnumerated(tag));
            }
         } else if (tag < 63) {
            slotName = slotNames[42];
            value = in.readUnsigned(tag);
         } else if (tag == 63) {
            long extendedValue = in.readUnsignedInteger(63);
            if (extendedValue < 25500000L) {
               throw new OutOfRangeException("Extended choice values must be at least 255; value: " + extendedValue);
            }

            long extendedChoice = extendedValue / 100000L;
            if (extendedChoice > 42949L) {
               throw new OutOfRangeException("Extended choice value greater than 42949 are not supported: " + extendedChoice);
            }

            choice = (int)extendedChoice;
            slotName = slotNames[42];
            value = BBacnetUnsigned.make(extendedValue - extendedChoice * 100000L);
         } else {
            slotName = slotNames[43];
            value = in.readUnsigned(tag);
         }

         this.removeAll(noWrite);
         this.setInt(BBacnetPropertyStates.choice, choice, noWrite);
         this.add(slotName, value, noWrite);
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   private static BValue makeEnumValue(int tag, int ordinal) throws OutOfRangeException {
      switch (tag) {
         case 1:
            return BBacnetBinaryPv.make(ordinal);
         case 2:
            return makeDynamicEnum(ordinal, BBacnetEventType.DEFAULT);
         case 3:
            return BBacnetPolarity.make(ordinal);
         case 4:
            return BBacnetProgramRequest.make(ordinal);
         case 5:
            return BBacnetProgramState.make(ordinal);
         case 6:
            return makeDynamicEnum(ordinal, BBacnetProgramError.DEFAULT);
         case 7:
            return makeDynamicEnum(ordinal, BBacnetReliability.DEFAULT);
         case 8:
            return makeDynamicEnum(ordinal, BBacnetEventState.DEFAULT);
         case 9:
            return makeDynamicEnum(ordinal, BBacnetDeviceStatus.DEFAULT);
         case 10:
            return makeDynamicEnum(ordinal, BBacnetEngineeringUnits.DEFAULT);
         case 11:
         case 29:
         case 35:
         default:
            throw new OutOfRangeException("Enum choice value not supported: " + tag);
         case 12:
            return makeDynamicEnum(ordinal, BBacnetLifeSafetyMode.DEFAULT);
         case 13:
            return makeDynamicEnum(ordinal, BBacnetLifeSafetyState.DEFAULT);
         case 14:
            return makeDynamicEnum(ordinal, BBacnetRestartReason.DEFAULT);
         case 15:
            return makeDynamicEnum(ordinal, BBacnetDoorAlarmState.DEFAULT);
         case 16:
            return BBacnetAction.make(ordinal);
         case 17:
            return BBacnetDoorSecuredStatus.make(ordinal);
         case 18:
            return makeDynamicEnum(ordinal, BBacnetDoorStatus.DEFAULT);
         case 19:
            return BBacnetDoorValue.make(ordinal);
         case 20:
            return BBacnetFileAccessMethod.make(ordinal);
         case 21:
            return BBacnetLockStatus.make(ordinal);
         case 22:
            return makeDynamicEnum(ordinal, BBacnetLifeSafetyOperation.DEFAULT);
         case 23:
            return makeDynamicEnum(ordinal, BBacnetMaintenance.DEFAULT);
         case 24:
            return BBacnetNodeType.make(ordinal);
         case 25:
            return BBacnetNotifyType.make(ordinal);
         case 26:
            return makeDynamicEnum(ordinal, BBacnetSecurityLevel.DEFAULT);
         case 27:
            return BBacnetShedState.make(ordinal);
         case 28:
            return makeDynamicEnum(ordinal, BBacnetSilencedState.DEFAULT);
         case 30:
            return makeDynamicEnum(ordinal, BBacnetAccessEvent.DEFAULT);
         case 31:
            return makeDynamicEnum(ordinal, BBacnetAccessZoneOccupancyState.DEFAULT);
         case 32:
            return makeDynamicEnum(ordinal, BBacnetAccessCredentialDisableReason.DEFAULT);
         case 33:
            return makeDynamicEnum(ordinal, BBacnetAccessCredentialDisable.DEFAULT);
         case 34:
            return BBacnetAuthenticationStatus.make(ordinal);
         case 36:
            return BBacnetBackupState.make(ordinal);
         case 37:
            return BBacnetWriteStatus.make(ordinal);
         case 38:
            return BBacnetLightingInProgress.make(ordinal);
         case 39:
            return makeDynamicEnum(ordinal, BBacnetLightingOperation.DEFAULT);
         case 40:
            return makeDynamicEnum(ordinal, BBacnetLightingTransition.DEFAULT);
      }
   }

   private static BDynamicEnum makeDynamicEnum(int ordinal, BFrozenEnum enumDefault) {
      return BDynamicEnum.make(ordinal, enumDefault.getRange());
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetPropertyStates", 2);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }

   private static Map<BTypeSpec, Integer> makeChoicesMap() {
      Map<BTypeSpec, Integer> choices = new HashMap<>();
      choices.put(BBacnetBinaryPv.TYPE.getTypeSpec(), 1);
      choices.put(BBacnetEventType.TYPE.getTypeSpec(), 2);
      choices.put(BBacnetPolarity.TYPE.getTypeSpec(), 3);
      choices.put(BBacnetProgramRequest.TYPE.getTypeSpec(), 4);
      choices.put(BBacnetProgramState.TYPE.getTypeSpec(), 5);
      choices.put(BBacnetProgramError.TYPE.getTypeSpec(), 6);
      choices.put(BBacnetReliability.TYPE.getTypeSpec(), 7);
      choices.put(BBacnetEventState.TYPE.getTypeSpec(), 8);
      choices.put(BBacnetDeviceStatus.TYPE.getTypeSpec(), 9);
      choices.put(BBacnetEngineeringUnits.TYPE.getTypeSpec(), 10);
      choices.put(BBacnetLifeSafetyMode.TYPE.getTypeSpec(), 12);
      choices.put(BBacnetLifeSafetyState.TYPE.getTypeSpec(), 13);
      choices.put(BBacnetRestartReason.TYPE.getTypeSpec(), 14);
      choices.put(BBacnetDoorAlarmState.TYPE.getTypeSpec(), 15);
      choices.put(BBacnetAction.TYPE.getTypeSpec(), 16);
      choices.put(BBacnetDoorSecuredStatus.TYPE.getTypeSpec(), 17);
      choices.put(BBacnetDoorStatus.TYPE.getTypeSpec(), 18);
      choices.put(BBacnetDoorValue.TYPE.getTypeSpec(), 19);
      choices.put(BBacnetFileAccessMethod.TYPE.getTypeSpec(), 20);
      choices.put(BBacnetLockStatus.TYPE.getTypeSpec(), 21);
      choices.put(BBacnetLifeSafetyOperation.TYPE.getTypeSpec(), 22);
      choices.put(BBacnetMaintenance.TYPE.getTypeSpec(), 23);
      choices.put(BBacnetNodeType.TYPE.getTypeSpec(), 24);
      choices.put(BBacnetNotifyType.TYPE.getTypeSpec(), 25);
      choices.put(BBacnetSecurityLevel.TYPE.getTypeSpec(), 26);
      choices.put(BBacnetShedState.TYPE.getTypeSpec(), 27);
      choices.put(BBacnetSilencedState.TYPE.getTypeSpec(), 28);
      choices.put(BBacnetAccessEvent.TYPE.getTypeSpec(), 30);
      choices.put(BBacnetAccessZoneOccupancyState.TYPE.getTypeSpec(), 31);
      choices.put(BBacnetAccessCredentialDisableReason.TYPE.getTypeSpec(), 32);
      choices.put(BBacnetAccessCredentialDisable.TYPE.getTypeSpec(), 33);
      choices.put(BBacnetAuthenticationStatus.TYPE.getTypeSpec(), 34);
      choices.put(BBacnetBackupState.TYPE.getTypeSpec(), 36);
      choices.put(BBacnetWriteStatus.TYPE.getTypeSpec(), 37);
      choices.put(BBacnetLightingInProgress.TYPE.getTypeSpec(), 38);
      choices.put(BBacnetLightingOperation.TYPE.getTypeSpec(), 39);
      choices.put(BBacnetLightingTransition.TYPE.getTypeSpec(), 40);
      return choices;
   }

   private String getTag() {
      int ch = this.getChoice();
      if (ch < 0 || ch == 63) {
         return tags[44];
      } else if (ch <= 41) {
         return tags[ch];
      } else if (ch < 63) {
         return tags[42];
      } else {
         return ch <= 254 ? tags[43] : tags[42];
      }
   }

   public BValue getValue() {
      int ch = this.getChoice();
      if (ch < 0 || ch == 63) {
         return null;
      } else if (ch <= 41) {
         return this.get(slotNames[ch]);
      } else if (ch < 63) {
         return this.get(slotNames[42]);
      } else {
         return ch <= 254 ? this.get(slotNames[43]) : this.get(slotNames[42]);
      }
   }
}
