package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("noFaultDetected"), @Range("noSensor"), @Range("overRange"), @Range("underRange"), @Range("openLoop"), @Range("shortedLoop"), @Range("noOutput"), @Range("unreliableOther"), @Range("processError"), @Range("multiStateFault"), @Range("configurationError"), @Range(
      value = "communicationFailure",
      ordinal = 12
   ), @Range(
      value = "memberFault",
      ordinal = 13
   ), @Range(
      value = "monitoredObjectFault",
      ordinal = 14
   ), @Range(
      value = "tripped",
      ordinal = 15
   ), @Range(
      value = "lampFailure",
      ordinal = 16
   ), @Range(
      value = "activationFailure",
      ordinal = 17
   ), @Range(
      value = "renewDhcpFailure",
      ordinal = 18
   ), @Range(
      value = "renewFdRegistrationFailure",
      ordinal = 19
   ), @Range(
      value = "restartAutoNegotiationFailure",
      ordinal = 20
   ), @Range(
      value = "restartFailure",
      ordinal = 21
   ), @Range(
      value = "proprietaryCommandFailure",
      ordinal = 22
   ), @Range(
      value = "faultsListed",
      ordinal = 23
   ), @Range(
      value = "referencedObjectFault",
      ordinal = 24
   ), @Range(
      value = "multiStateOutOfRange",
      ordinal = 25
   )}
)
public final class BBacnetReliability extends BFrozenEnum implements BacnetConst {
   public static final int NO_FAULT_DETECTED = 0;
   public static final int NO_SENSOR = 1;
   public static final int OVER_RANGE = 2;
   public static final int UNDER_RANGE = 3;
   public static final int OPEN_LOOP = 4;
   public static final int SHORTED_LOOP = 5;
   public static final int NO_OUTPUT = 6;
   public static final int UNRELIABLE_OTHER = 7;
   public static final int PROCESS_ERROR = 8;
   public static final int MULTI_STATE_FAULT = 9;
   public static final int CONFIGURATION_ERROR = 10;
   public static final int COMMUNICATION_FAILURE = 12;
   public static final int MEMBER_FAULT = 13;
   public static final int MONITORED_OBJECT_FAULT = 14;
   public static final int TRIPPED = 15;
   public static final int LAMP_FAILURE = 16;
   public static final int ACTIVATION_FAILURE = 17;
   public static final int RENEW_DHCP_FAILURE = 18;
   public static final int RENEW_FD_REGISTRATION_FAILURE = 19;
   public static final int RESTART_AUTO_NEGOTIATION_FAILURE = 20;
   public static final int RESTART_FAILURE = 21;
   public static final int PROPRIETARY_COMMAND_FAILURE = 22;
   public static final int FAULTS_LISTED = 23;
   public static final int REFERENCED_OBJECT_FAULT = 24;
   public static final int MULTI_STATE_OUT_OF_RANGE = 25;
   public static final BBacnetReliability noFaultDetected = new BBacnetReliability(0);
   public static final BBacnetReliability noSensor = new BBacnetReliability(1);
   public static final BBacnetReliability overRange = new BBacnetReliability(2);
   public static final BBacnetReliability underRange = new BBacnetReliability(3);
   public static final BBacnetReliability openLoop = new BBacnetReliability(4);
   public static final BBacnetReliability shortedLoop = new BBacnetReliability(5);
   public static final BBacnetReliability noOutput = new BBacnetReliability(6);
   public static final BBacnetReliability unreliableOther = new BBacnetReliability(7);
   public static final BBacnetReliability processError = new BBacnetReliability(8);
   public static final BBacnetReliability multiStateFault = new BBacnetReliability(9);
   public static final BBacnetReliability configurationError = new BBacnetReliability(10);
   public static final BBacnetReliability communicationFailure = new BBacnetReliability(12);
   public static final BBacnetReliability memberFault = new BBacnetReliability(13);
   public static final BBacnetReliability monitoredObjectFault = new BBacnetReliability(14);
   public static final BBacnetReliability tripped = new BBacnetReliability(15);
   public static final BBacnetReliability lampFailure = new BBacnetReliability(16);
   public static final BBacnetReliability activationFailure = new BBacnetReliability(17);
   public static final BBacnetReliability renewDhcpFailure = new BBacnetReliability(18);
   public static final BBacnetReliability renewFdRegistrationFailure = new BBacnetReliability(19);
   public static final BBacnetReliability restartAutoNegotiationFailure = new BBacnetReliability(20);
   public static final BBacnetReliability restartFailure = new BBacnetReliability(21);
   public static final BBacnetReliability proprietaryCommandFailure = new BBacnetReliability(22);
   public static final BBacnetReliability faultsListed = new BBacnetReliability(23);
   public static final BBacnetReliability referencedObjectFault = new BBacnetReliability(24);
   public static final BBacnetReliability multiStateOutOfRange = new BBacnetReliability(25);
   public static final BBacnetReliability DEFAULT = noFaultDetected;
   public static final Type TYPE = Sys.loadType(BBacnetReliability.class);
   public static final int RESERVED_FOR_FUTURE_ADDENDUM = 11;
   public static final int MAX_ASHRAE_ID = 25;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetReliability make(int ordinal) {
      return (BBacnetReliability)noFaultDetected.getRange().get(ordinal, false);
   }

   public static BBacnetReliability make(String tag) {
      return (BBacnetReliability)noFaultDetected.getRange().get(tag);
   }

   private BBacnetReliability(int ordinal) {
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
      } else if (isProprietary(id)) {
         return PROPRIETARY_PREFIX + id;
      } else {
         throw new InvalidEnumException(id);
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
         } else {
            throw var2;
         }
      }
   }

   public static boolean isProprietary(int id) {
      return id > 63 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id == 11 || id > 25 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 25 && id != 11;
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext) ? this.getTag() : this.getDisplayTag(context);
   }
}
