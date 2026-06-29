package javax.baja.bacnet.enums.access;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "none",
      ordinal = 0
   ), @Range(
      value = "granted",
      ordinal = 1
   ), @Range(
      value = "muster",
      ordinal = 2
   ), @Range(
      value = "passbackDetected",
      ordinal = 3
   ), @Range(
      value = "duress",
      ordinal = 4
   ), @Range(
      value = "trace",
      ordinal = 5
   ), @Range(
      value = "lockoutMaxAttempts",
      ordinal = 6
   ), @Range(
      value = "lockoutOther",
      ordinal = 7
   ), @Range(
      value = "lockoutRelinquished",
      ordinal = 8
   ), @Range(
      value = "lockedByHigherPriority",
      ordinal = 9
   ), @Range(
      value = "outOfService",
      ordinal = 10
   ), @Range(
      value = "outOfServiceRelinquished",
      ordinal = 11
   ), @Range(
      value = "accompanimentBy",
      ordinal = 12
   ), @Range(
      value = "authenticationFactorRead",
      ordinal = 13
   ), @Range(
      value = "authorizationDelayed",
      ordinal = 14
   ), @Range(
      value = "verificationRequired",
      ordinal = 15
   ), @Range(
      value = "deniedDenyAll",
      ordinal = 128
   ), @Range(
      value = "deniedUnknownCredential",
      ordinal = 129
   ), @Range(
      value = "deniedAuthenticationUnavailable",
      ordinal = 130
   ), @Range(
      value = "deniedAuthenticationFactorTimeout",
      ordinal = 131
   ), @Range(
      value = "deniedIncorrectAuthenticationFactor",
      ordinal = 132
   ), @Range(
      value = "deniedZoneNoAccessRights",
      ordinal = 133
   ), @Range(
      value = "deniedPointNoAccessRights",
      ordinal = 134
   ), @Range(
      value = "deniedNoAccessRights",
      ordinal = 135
   ), @Range(
      value = "deniedOutOfTimeRange",
      ordinal = 136
   ), @Range(
      value = "deniedThreatLevel",
      ordinal = 137
   ), @Range(
      value = "deniedPassback",
      ordinal = 138
   ), @Range(
      value = "deniedUnexpectedLocationUsage",
      ordinal = 139
   ), @Range(
      value = "deniedMaxAttempts",
      ordinal = 140
   ), @Range(
      value = "deniedLowerOccupancyLimit",
      ordinal = 141
   ), @Range(
      value = "deniedUpperOccupancyLimit",
      ordinal = 142
   ), @Range(
      value = "deniedAuthenticationFactorLost",
      ordinal = 143
   ), @Range(
      value = "deniedAuthenticationFactorStolen",
      ordinal = 144
   ), @Range(
      value = "deniedAuthenticationFactorDamaged",
      ordinal = 145
   ), @Range(
      value = "deniedAuthenticationFactorDestroyed",
      ordinal = 146
   ), @Range(
      value = "deniedAuthenticationFactorDisabled",
      ordinal = 147
   ), @Range(
      value = "deniedAuthenticationFactorError",
      ordinal = 148
   ), @Range(
      value = "deniedCredentialUnassigned",
      ordinal = 149
   ), @Range(
      value = "deniedCredentialNotProvisioned",
      ordinal = 150
   ), @Range(
      value = "deniedCredentialNotYetActive",
      ordinal = 151
   ), @Range(
      value = "deniedCredentialExpired",
      ordinal = 152
   ), @Range(
      value = "deniedCredentialManualDisable",
      ordinal = 153
   ), @Range(
      value = "deniedCredentialLockout",
      ordinal = 154
   ), @Range(
      value = "deniedCredentialMaxDays",
      ordinal = 155
   ), @Range(
      value = "deniedCredentialMaxUses",
      ordinal = 156
   ), @Range(
      value = "deniedCredentialInactivity",
      ordinal = 157
   ), @Range(
      value = "deniedCredentialDisabled",
      ordinal = 158
   ), @Range(
      value = "deniedNoAccompaniment",
      ordinal = 159
   ), @Range(
      value = "deniedIncorrectAccompaniment",
      ordinal = 160
   ), @Range(
      value = "deniedLockout",
      ordinal = 161
   ), @Range(
      value = "deniedVerificationFailed",
      ordinal = 162
   ), @Range(
      value = "deniedVerificationTimeout",
      ordinal = 163
   ), @Range(
      value = "deniedOther",
      ordinal = 164
   )}
)
public final class BBacnetAccessEvent extends BFrozenEnum implements BacnetConst {
   public static final int NONE = 0;
   public static final int GRANTED = 1;
   public static final int MUSTER = 2;
   public static final int PASSBACK_DETECTED = 3;
   public static final int DURESS = 4;
   public static final int TRACE = 5;
   public static final int LOCKOUT_MAX_ATTEMPTS = 6;
   public static final int LOCKOUT_OTHER = 7;
   public static final int LOCKOUT_RELINQUISHED = 8;
   public static final int LOCKED_BY_HIGHER_PRIORITY = 9;
   public static final int OUT_OF_SERVICE = 10;
   public static final int OUT_OF_SERVICE_RELINQUISHED = 11;
   public static final int ACCOMPANIMENT_BY = 12;
   public static final int AUTHENTICATION_FACTOR_READ = 13;
   public static final int AUTHORIZATION_DELAYED = 14;
   public static final int VERIFICATION_REQUIRED = 15;
   public static final int DENIED_DENY_ALL = 128;
   public static final int DENIED_UNKNOWN_CREDENTIAL = 129;
   public static final int DENIED_AUTHENTICATION_UNAVAILABLE = 130;
   public static final int DENIED_AUTHENTICATION_FACTOR_TIMEOUT = 131;
   public static final int DENIED_INCORRECT_AUTHENTICATION_FACTOR = 132;
   public static final int DENIED_ZONE_NO_ACCESS_RIGHTS = 133;
   public static final int DENIED_POINT_NO_ACCESS_RIGHTS = 134;
   public static final int DENIED_NO_ACCESS_RIGHTS = 135;
   public static final int DENIED_OUT_OF_TIME_RANGE = 136;
   public static final int DENIED_THREAT_LEVEL = 137;
   public static final int DENIED_PASSBACK = 138;
   public static final int DENIED_UNEXPECTED_LOCATION_USAGE = 139;
   public static final int DENIED_MAX_ATTEMPTS = 140;
   public static final int DENIED_LOWER_OCCUPANCY_LIMIT = 141;
   public static final int DENIED_UPPER_OCCUPANCY_LIMIT = 142;
   public static final int DENIED_AUTHENTICATION_FACTOR_LOST = 143;
   public static final int DENIED_AUTHENTICATION_FACTOR_STOLEN = 144;
   public static final int DENIED_AUTHENTICATION_FACTOR_DAMAGED = 145;
   public static final int DENIED_AUTHENTICATION_FACTOR_DESTROYED = 146;
   public static final int DENIED_AUTHENTICATION_FACTOR_DISABLED = 147;
   public static final int DENIED_AUTHENTICATION_FACTOR_ERROR = 148;
   public static final int DENIED_CREDENTIAL_UNASSIGNED = 149;
   public static final int DENIED_CREDENTIAL_NOT_PROVISIONED = 150;
   public static final int DENIED_CREDENTIAL_NOT_YET_ACTIVE = 151;
   public static final int DENIED_CREDENTIAL_EXPIRED = 152;
   public static final int DENIED_CREDENTIAL_MANUAL_DISABLE = 153;
   public static final int DENIED_CREDENTIAL_LOCKOUT = 154;
   public static final int DENIED_CREDENTIAL_MAX_DAYS = 155;
   public static final int DENIED_CREDENTIAL_MAX_USES = 156;
   public static final int DENIED_CREDENTIAL_INACTIVITY = 157;
   public static final int DENIED_CREDENTIAL_DISABLED = 158;
   public static final int DENIED_NO_ACCOMPANIMENT = 159;
   public static final int DENIED_INCORRECT_ACCOMPANIMENT = 160;
   public static final int DENIED_LOCKOUT = 161;
   public static final int DENIED_VERIFICATION_FAILED = 162;
   public static final int DENIED_VERIFICATION_TIMEOUT = 163;
   public static final int DENIED_OTHER = 164;
   public static final BBacnetAccessEvent none = new BBacnetAccessEvent(0);
   public static final BBacnetAccessEvent granted = new BBacnetAccessEvent(1);
   public static final BBacnetAccessEvent muster = new BBacnetAccessEvent(2);
   public static final BBacnetAccessEvent passbackDetected = new BBacnetAccessEvent(3);
   public static final BBacnetAccessEvent duress = new BBacnetAccessEvent(4);
   public static final BBacnetAccessEvent trace = new BBacnetAccessEvent(5);
   public static final BBacnetAccessEvent lockoutMaxAttempts = new BBacnetAccessEvent(6);
   public static final BBacnetAccessEvent lockoutOther = new BBacnetAccessEvent(7);
   public static final BBacnetAccessEvent lockoutRelinquished = new BBacnetAccessEvent(8);
   public static final BBacnetAccessEvent lockedByHigherPriority = new BBacnetAccessEvent(9);
   public static final BBacnetAccessEvent outOfService = new BBacnetAccessEvent(10);
   public static final BBacnetAccessEvent outOfServiceRelinquished = new BBacnetAccessEvent(11);
   public static final BBacnetAccessEvent accompanimentBy = new BBacnetAccessEvent(12);
   public static final BBacnetAccessEvent authenticationFactorRead = new BBacnetAccessEvent(13);
   public static final BBacnetAccessEvent authorizationDelayed = new BBacnetAccessEvent(14);
   public static final BBacnetAccessEvent verificationRequired = new BBacnetAccessEvent(15);
   public static final BBacnetAccessEvent deniedDenyAll = new BBacnetAccessEvent(128);
   public static final BBacnetAccessEvent deniedUnknownCredential = new BBacnetAccessEvent(129);
   public static final BBacnetAccessEvent deniedAuthenticationUnavailable = new BBacnetAccessEvent(130);
   public static final BBacnetAccessEvent deniedAuthenticationFactorTimeout = new BBacnetAccessEvent(131);
   public static final BBacnetAccessEvent deniedIncorrectAuthenticationFactor = new BBacnetAccessEvent(132);
   public static final BBacnetAccessEvent deniedZoneNoAccessRights = new BBacnetAccessEvent(133);
   public static final BBacnetAccessEvent deniedPointNoAccessRights = new BBacnetAccessEvent(134);
   public static final BBacnetAccessEvent deniedNoAccessRights = new BBacnetAccessEvent(135);
   public static final BBacnetAccessEvent deniedOutOfTimeRange = new BBacnetAccessEvent(136);
   public static final BBacnetAccessEvent deniedThreatLevel = new BBacnetAccessEvent(137);
   public static final BBacnetAccessEvent deniedPassback = new BBacnetAccessEvent(138);
   public static final BBacnetAccessEvent deniedUnexpectedLocationUsage = new BBacnetAccessEvent(139);
   public static final BBacnetAccessEvent deniedMaxAttempts = new BBacnetAccessEvent(140);
   public static final BBacnetAccessEvent deniedLowerOccupancyLimit = new BBacnetAccessEvent(141);
   public static final BBacnetAccessEvent deniedUpperOccupancyLimit = new BBacnetAccessEvent(142);
   public static final BBacnetAccessEvent deniedAuthenticationFactorLost = new BBacnetAccessEvent(143);
   public static final BBacnetAccessEvent deniedAuthenticationFactorStolen = new BBacnetAccessEvent(144);
   public static final BBacnetAccessEvent deniedAuthenticationFactorDamaged = new BBacnetAccessEvent(145);
   public static final BBacnetAccessEvent deniedAuthenticationFactorDestroyed = new BBacnetAccessEvent(146);
   public static final BBacnetAccessEvent deniedAuthenticationFactorDisabled = new BBacnetAccessEvent(147);
   public static final BBacnetAccessEvent deniedAuthenticationFactorError = new BBacnetAccessEvent(148);
   public static final BBacnetAccessEvent deniedCredentialUnassigned = new BBacnetAccessEvent(149);
   public static final BBacnetAccessEvent deniedCredentialNotProvisioned = new BBacnetAccessEvent(150);
   public static final BBacnetAccessEvent deniedCredentialNotYetActive = new BBacnetAccessEvent(151);
   public static final BBacnetAccessEvent deniedCredentialExpired = new BBacnetAccessEvent(152);
   public static final BBacnetAccessEvent deniedCredentialManualDisable = new BBacnetAccessEvent(153);
   public static final BBacnetAccessEvent deniedCredentialLockout = new BBacnetAccessEvent(154);
   public static final BBacnetAccessEvent deniedCredentialMaxDays = new BBacnetAccessEvent(155);
   public static final BBacnetAccessEvent deniedCredentialMaxUses = new BBacnetAccessEvent(156);
   public static final BBacnetAccessEvent deniedCredentialInactivity = new BBacnetAccessEvent(157);
   public static final BBacnetAccessEvent deniedCredentialDisabled = new BBacnetAccessEvent(158);
   public static final BBacnetAccessEvent deniedNoAccompaniment = new BBacnetAccessEvent(159);
   public static final BBacnetAccessEvent deniedIncorrectAccompaniment = new BBacnetAccessEvent(160);
   public static final BBacnetAccessEvent deniedLockout = new BBacnetAccessEvent(161);
   public static final BBacnetAccessEvent deniedVerificationFailed = new BBacnetAccessEvent(162);
   public static final BBacnetAccessEvent deniedVerificationTimeout = new BBacnetAccessEvent(163);
   public static final BBacnetAccessEvent deniedOther = new BBacnetAccessEvent(164);
   public static final BBacnetAccessEvent DEFAULT = none;
   public static final Type TYPE = Sys.loadType(BBacnetAccessEvent.class);
   public static final int MAX_ASHRAE_ID = 164;
   public static final int MAX_RESERVED_ID = 511;
   public static final int MAX_ID = 65535;

   public static BBacnetAccessEvent make(int ordinal) {
      return (BBacnetAccessEvent)none.getRange().get(ordinal, false);
   }

   public static BBacnetAccessEvent make(String tag) {
      return (BBacnetAccessEvent)none.getRange().get(tag);
   }

   private BBacnetAccessEvent(int ordinal) {
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
      return id > 511 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 164 && id <= 511;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 164;
   }
}
