package javax.baja.bacnet.enums;

import java.util.Arrays;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("analogInput"), @Range("analogOutput"), @Range("analogValue"), @Range("binaryInput"), @Range("binaryOutput"), @Range("binaryValue"), @Range("calendar"), @Range("command"), @Range("device"), @Range("eventEnrollment"), @Range("file"), @Range("group"), @Range("loop"), @Range("multiStateInput"), @Range("multiStateOutput"), @Range("notificationClass"), @Range("program"), @Range("schedule"), @Range("averaging"), @Range("multiStateValue"), @Range("trendLog"), @Range("lifeSafetyPoint"), @Range("lifeSafetyZone"), @Range("accumulator"), @Range("pulseConverter"), @Range("eventLog"), @Range("globalGroup"), @Range("trendLogMultiple"), @Range("loadControl"), @Range("structuredView"), @Range("accessDoor"), @Range("unassigned31"), @Range("accessCredential"), @Range("accessPoint"), @Range("accessRights"), @Range("accessUser"), @Range("accessZone"), @Range("credentialDataInput"), @Range("networkSecurity"), @Range("bitstringValue"), @Range("characterStringValue"), @Range("datePatternValue"), @Range("dateValue"), @Range("dateTimePatternValue"), @Range("dateTimeValue"), @Range("integerValue"), @Range("largeAnalogValue"), @Range("octetStringValue"), @Range("positiveIntegerValue"), @Range("timePatternValue"), @Range("timeValue"), @Range("notificationForwarder"), @Range("alertEnrollment"), @Range("channel"), @Range("lightingOutput"), @Range("binaryLightingOutput")}
)
public final class BBacnetObjectType extends BFrozenEnum implements BacnetConst {
   public static final int ANALOG_INPUT = 0;
   public static final int ANALOG_OUTPUT = 1;
   public static final int ANALOG_VALUE = 2;
   public static final int BINARY_INPUT = 3;
   public static final int BINARY_OUTPUT = 4;
   public static final int BINARY_VALUE = 5;
   public static final int CALENDAR = 6;
   public static final int COMMAND = 7;
   public static final int DEVICE = 8;
   public static final int EVENT_ENROLLMENT = 9;
   public static final int FILE = 10;
   public static final int GROUP = 11;
   public static final int LOOP = 12;
   public static final int MULTI_STATE_INPUT = 13;
   public static final int MULTI_STATE_OUTPUT = 14;
   public static final int NOTIFICATION_CLASS = 15;
   public static final int PROGRAM = 16;
   public static final int SCHEDULE = 17;
   public static final int AVERAGING = 18;
   public static final int MULTI_STATE_VALUE = 19;
   public static final int TREND_LOG = 20;
   public static final int LIFE_SAFETY_POINT = 21;
   public static final int LIFE_SAFETY_ZONE = 22;
   public static final int ACCUMULATOR = 23;
   public static final int PULSE_CONVERTER = 24;
   public static final int EVENT_LOG = 25;
   public static final int GLOBAL_GROUP = 26;
   public static final int TREND_LOG_MULTIPLE = 27;
   public static final int LOAD_CONTROL = 28;
   public static final int STRUCTURED_VIEW = 29;
   public static final int ACCESS_DOOR = 30;
   public static final int UNASSIGNED_31 = 31;
   public static final int ACCESS_CREDENTIAL = 32;
   public static final int ACCESS_POINT = 33;
   public static final int ACCESS_RIGHTS = 34;
   public static final int ACCESS_USER = 35;
   public static final int ACCESS_ZONE = 36;
   public static final int CREDENTIAL_DATA_INPUT = 37;
   public static final int NETWORK_SECURITY = 38;
   public static final int BITSTRING_VALUE = 39;
   public static final int CHARACTER_STRING_VALUE = 40;
   public static final int DATE_PATTERN_VALUE = 41;
   public static final int DATE_VALUE = 42;
   public static final int DATE_TIME_PATTERN_VALUE = 43;
   public static final int DATE_TIME_VALUE = 44;
   public static final int INTEGER_VALUE = 45;
   public static final int LARGE_ANALOG_VALUE = 46;
   public static final int OCTET_STRING_VALUE = 47;
   public static final int POSITIVE_INTEGER_VALUE = 48;
   public static final int TIME_PATTERN_VALUE = 49;
   public static final int TIME_VALUE = 50;
   public static final int NOTIFICATION_FORWARDER = 51;
   public static final int ALERT_ENROLLMENT = 52;
   public static final int CHANNEL = 53;
   public static final int LIGHTING_OUTPUT = 54;
   public static final int BINARY_LIGHTING_OUTPUT = 55;
   public static final BBacnetObjectType analogInput = new BBacnetObjectType(0);
   public static final BBacnetObjectType analogOutput = new BBacnetObjectType(1);
   public static final BBacnetObjectType analogValue = new BBacnetObjectType(2);
   public static final BBacnetObjectType binaryInput = new BBacnetObjectType(3);
   public static final BBacnetObjectType binaryOutput = new BBacnetObjectType(4);
   public static final BBacnetObjectType binaryValue = new BBacnetObjectType(5);
   public static final BBacnetObjectType calendar = new BBacnetObjectType(6);
   public static final BBacnetObjectType command = new BBacnetObjectType(7);
   public static final BBacnetObjectType device = new BBacnetObjectType(8);
   public static final BBacnetObjectType eventEnrollment = new BBacnetObjectType(9);
   public static final BBacnetObjectType file = new BBacnetObjectType(10);
   public static final BBacnetObjectType group = new BBacnetObjectType(11);
   public static final BBacnetObjectType loop = new BBacnetObjectType(12);
   public static final BBacnetObjectType multiStateInput = new BBacnetObjectType(13);
   public static final BBacnetObjectType multiStateOutput = new BBacnetObjectType(14);
   public static final BBacnetObjectType notificationClass = new BBacnetObjectType(15);
   public static final BBacnetObjectType program = new BBacnetObjectType(16);
   public static final BBacnetObjectType schedule = new BBacnetObjectType(17);
   public static final BBacnetObjectType averaging = new BBacnetObjectType(18);
   public static final BBacnetObjectType multiStateValue = new BBacnetObjectType(19);
   public static final BBacnetObjectType trendLog = new BBacnetObjectType(20);
   public static final BBacnetObjectType lifeSafetyPoint = new BBacnetObjectType(21);
   public static final BBacnetObjectType lifeSafetyZone = new BBacnetObjectType(22);
   public static final BBacnetObjectType accumulator = new BBacnetObjectType(23);
   public static final BBacnetObjectType pulseConverter = new BBacnetObjectType(24);
   public static final BBacnetObjectType eventLog = new BBacnetObjectType(25);
   public static final BBacnetObjectType globalGroup = new BBacnetObjectType(26);
   public static final BBacnetObjectType trendLogMultiple = new BBacnetObjectType(27);
   public static final BBacnetObjectType loadControl = new BBacnetObjectType(28);
   public static final BBacnetObjectType structuredView = new BBacnetObjectType(29);
   public static final BBacnetObjectType accessDoor = new BBacnetObjectType(30);
   public static final BBacnetObjectType unassigned31 = new BBacnetObjectType(31);
   public static final BBacnetObjectType accessCredential = new BBacnetObjectType(32);
   public static final BBacnetObjectType accessPoint = new BBacnetObjectType(33);
   public static final BBacnetObjectType accessRights = new BBacnetObjectType(34);
   public static final BBacnetObjectType accessUser = new BBacnetObjectType(35);
   public static final BBacnetObjectType accessZone = new BBacnetObjectType(36);
   public static final BBacnetObjectType credentialDataInput = new BBacnetObjectType(37);
   public static final BBacnetObjectType networkSecurity = new BBacnetObjectType(38);
   public static final BBacnetObjectType bitstringValue = new BBacnetObjectType(39);
   public static final BBacnetObjectType characterStringValue = new BBacnetObjectType(40);
   public static final BBacnetObjectType datePatternValue = new BBacnetObjectType(41);
   public static final BBacnetObjectType dateValue = new BBacnetObjectType(42);
   public static final BBacnetObjectType dateTimePatternValue = new BBacnetObjectType(43);
   public static final BBacnetObjectType dateTimeValue = new BBacnetObjectType(44);
   public static final BBacnetObjectType integerValue = new BBacnetObjectType(45);
   public static final BBacnetObjectType largeAnalogValue = new BBacnetObjectType(46);
   public static final BBacnetObjectType octetStringValue = new BBacnetObjectType(47);
   public static final BBacnetObjectType positiveIntegerValue = new BBacnetObjectType(48);
   public static final BBacnetObjectType timePatternValue = new BBacnetObjectType(49);
   public static final BBacnetObjectType timeValue = new BBacnetObjectType(50);
   public static final BBacnetObjectType notificationForwarder = new BBacnetObjectType(51);
   public static final BBacnetObjectType alertEnrollment = new BBacnetObjectType(52);
   public static final BBacnetObjectType channel = new BBacnetObjectType(53);
   public static final BBacnetObjectType lightingOutput = new BBacnetObjectType(54);
   public static final BBacnetObjectType binaryLightingOutput = new BBacnetObjectType(55);
   public static final BBacnetObjectType DEFAULT = analogInput;
   public static final Type TYPE = Sys.loadType(BBacnetObjectType.class);
   public static final int MAX_ASHRAE_ID = 55;
   public static final int MAX_RESERVED_ID = 127;
   public static final int MAX_ID = 1023;
   private static final String INVALID_OBJECT_TYPE = "INVALID";
   private static final String[] shortTags = makeShortTags();
   private static final BFacets[] objectIdFacets = new BFacets[56];

   public static BBacnetObjectType make(int ordinal) {
      return (BBacnetObjectType)analogInput.getRange().get(ordinal, false);
   }

   public static BBacnetObjectType make(String tag) {
      return (BBacnetObjectType)analogInput.getRange().get(tag);
   }

   private BBacnetObjectType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      if (DEFAULT.getRange().isOrdinal(id)) {
         return DEFAULT.getRange().getTag(id);
      } else if (isAshrae(id)) {
         return ASHRAE_PREFIX + id;
      } else {
         return isProprietary(id) ? PROPRIETARY_PREFIX + id : "INVALID:" + id;
      }
   }

   public static int ordinal(String tag) {
      try {
         return DEFAULT.getRange().tagToOrdinal(tag);
      } catch (InvalidEnumException var2) {
         if (tag.startsWith(ASHRAE_PREFIX)) {
            return Integer.parseInt(tag.substring(ASHRAE_PREFIX_LENGTH));
         } else if (tag.startsWith(PROPRIETARY_PREFIX)) {
            return Integer.parseInt(tag.substring(PROPRIETARY_PREFIX_LENGTH));
         } else if (tag.startsWith("INVALID")) {
            return -1;
         } else {
            throw var2;
         }
      }
   }

   public static boolean isProprietary(int id) {
      return id > 127 && id <= 1023;
   }

   public static boolean isAshrae(int id) {
      return id > 55 && id <= 127;
   }

   public static boolean isValid(int id) {
      return id <= 1023;
   }

   public static boolean isFixed(int id) {
      return id <= 55;
   }

   @Deprecated
   public static boolean hasStatusFlags(int ordinal) {
      return hasStatusFlags(ordinal, null);
   }

   public static boolean hasStatusFlags(int ordinal, BBacnetDevice device) {
      int pr = 0;
      if (device != null) {
         pr = device.getProtocolRevision();
      }

      return hasStatusFlags(ordinal, pr);
   }

   public static boolean hasStatusFlags(int ordinal, int pr) {
      if (ordinal > 55) {
         return false;
      } else {
         switch (ordinal) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 12:
            case 13:
            case 14:
            case 16:
               return pr >= 0;
            case 6:
            case 10:
            case 11:
            case 18:
            case 26:
            case 29:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 51:
            case 52:
            default:
               return false;
            case 7:
            case 8:
            case 15:
            case 55:
               return pr >= 16;
            case 9:
               return pr >= 13;
            case 17:
            case 23:
            case 24:
               return pr >= 4;
            case 19:
               return pr >= 1;
            case 20:
            case 25:
            case 27:
               return pr >= 7;
            case 21:
            case 22:
               return pr >= 2;
            case 28:
            case 30:
               return pr >= 6;
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
               return pr >= 10;
            case 53:
            case 54:
               return pr >= 14;
         }
      }
   }

   @Deprecated
   public static boolean canSupportCov(int ordinal) {
      return canSupportCov(ordinal, null);
   }

   public static boolean canSupportCov(int ordinal, BBacnetDevice device) {
      int pr = 0;
      if (device != null) {
         pr = device.getProtocolRevision();
      }

      return canSupportCov(ordinal, pr);
   }

   public static boolean canSupportCov(int ordinal, int pr) {
      if (ordinal > 55) {
         return false;
      } else {
         switch (ordinal) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 12:
            case 13:
            case 14:
               return pr >= 0;
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 15:
            case 16:
            case 17:
            case 18:
            case 20:
            case 23:
            case 25:
            case 26:
            case 27:
            case 29:
            case 31:
            case 32:
            case 34:
            case 35:
            case 36:
            case 38:
            case 39:
            case 51:
            case 52:
            case 53:
            default:
               return false;
            case 19:
               return pr >= 1;
            case 21:
            case 22:
               return pr >= 2;
            case 24:
               return pr >= 4;
            case 28:
            case 30:
               return pr >= 6;
            case 33:
            case 37:
               return pr >= 9;
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
               return pr >= 10;
            case 54:
               return pr >= 14;
            case 55:
               return pr >= 16;
         }
      }
   }

   public static long bitmask(int... bits) {
      long mask = 0L;

      for (int i = 0; i < bits.length; i++) {
         mask |= 1L << bits[i];
      }

      return mask;
   }

   public static String getShortTag(int objectType) {
      return objectType >= 0 && objectType < shortTags.length ? shortTags[objectType] : objectType + ":";
   }

   private static String[] makeShortTags() {
      String[] shortTags = new String[56];
      Arrays.fill(shortTags, "");
      shortTags[0] = "AI";
      shortTags[1] = "AO";
      shortTags[2] = "AV";
      shortTags[3] = "BI";
      shortTags[4] = "BO";
      shortTags[5] = "BV";
      shortTags[6] = "CAL";
      shortTags[7] = "CMD";
      shortTags[8] = "DEV";
      shortTags[9] = "EE";
      shortTags[10] = "FILE";
      shortTags[11] = "GRP";
      shortTags[12] = "LP";
      shortTags[13] = "MSI";
      shortTags[14] = "MSO";
      shortTags[15] = "NC";
      shortTags[16] = "PGM";
      shortTags[17] = "SCH";
      shortTags[18] = "AVG";
      shortTags[19] = "MSV";
      shortTags[20] = "LOG";
      shortTags[21] = "LSP";
      shortTags[22] = "LSZ";
      shortTags[23] = "ACC";
      shortTags[24] = "PC";
      shortTags[25] = "ELOG";
      shortTags[26] = "GGRP";
      shortTags[27] = "TLM";
      shortTags[28] = "LCO";
      shortTags[29] = "SVO";
      shortTags[30] = "DOOR";
      shortTags[31] = "31:";
      shortTags[32] = "ACRD";
      shortTags[33] = "APNT";
      shortTags[34] = "ARGT";
      shortTags[35] = "AUSR";
      shortTags[36] = "AZNE";
      shortTags[37] = "CRDI";
      shortTags[38] = "SEC";
      shortTags[39] = "BSV";
      shortTags[40] = "CSV";
      shortTags[41] = "DTP";
      shortTags[42] = "DTV";
      shortTags[43] = "DTMP";
      shortTags[44] = "DTMV";
      shortTags[45] = "INT";
      shortTags[46] = "LAV";
      shortTags[47] = "OSV";
      shortTags[48] = "PINT";
      shortTags[49] = "TMP";
      shortTags[50] = "TMV";
      shortTags[51] = "NF";
      shortTags[52] = "AE";
      shortTags[53] = "CHA";
      shortTags[54] = "LO";
      shortTags[55] = "BLO";
      return shortTags;
   }

   public static BFacets getObjectIdFacets(int objectType) {
      if (objectType >= 0 && objectType < objectIdFacets.length) {
         BFacets facets = objectIdFacets[objectType];
         if (facets == null) {
            facets = BFacets.makeEnum(BEnumRange.make(new int[]{objectType}, new String[]{tag(objectType)}));
            objectIdFacets[objectType] = facets;
         }

         return facets;
      } else {
         return null;
      }
   }
}
