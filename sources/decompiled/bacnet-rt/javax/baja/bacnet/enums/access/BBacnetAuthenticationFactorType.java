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
      value = "undefined",
      ordinal = 0
   ), @Range(
      value = "error",
      ordinal = 1
   ), @Range(
      value = "custom",
      ordinal = 2
   ), @Range(
      value = "simpleNumber16",
      ordinal = 3
   ), @Range(
      value = "simpleNumber32",
      ordinal = 4
   ), @Range(
      value = "simpleNumber56",
      ordinal = 5
   ), @Range(
      value = "simpleAlphaNumeric",
      ordinal = 6
   ), @Range(
      value = "abaTrack2",
      ordinal = 7
   ), @Range(
      value = "wiegand26",
      ordinal = 8
   ), @Range(
      value = "wiegand37",
      ordinal = 9
   ), @Range(
      value = "wiegand37Facility",
      ordinal = 10
   ), @Range(
      value = "facility16Card32",
      ordinal = 11
   ), @Range(
      value = "facility32Card32",
      ordinal = 12
   ), @Range(
      value = "fascN",
      ordinal = 13
   ), @Range(
      value = "fascNBcd",
      ordinal = 14
   ), @Range(
      value = "fascNLarge",
      ordinal = 15
   ), @Range(
      value = "fascNLargeBcd",
      ordinal = 16
   ), @Range(
      value = "gsa75",
      ordinal = 17
   ), @Range(
      value = "chuid",
      ordinal = 18
   ), @Range(
      value = "chuidFull",
      ordinal = 19
   ), @Range(
      value = "guid",
      ordinal = 20
   ), @Range(
      value = "cbeffA",
      ordinal = 21
   ), @Range(
      value = "cbeffB",
      ordinal = 22
   ), @Range(
      value = "cbeffC",
      ordinal = 23
   ), @Range(
      value = "userPassword",
      ordinal = 24
   )}
)
public final class BBacnetAuthenticationFactorType extends BFrozenEnum implements BacnetConst {
   public static final int UNDEFINED = 0;
   public static final int ERROR = 1;
   public static final int CUSTOM = 2;
   public static final int SIMPLE_NUMBER_16 = 3;
   public static final int SIMPLE_NUMBER_32 = 4;
   public static final int SIMPLE_NUMBER_56 = 5;
   public static final int SIMPLE_ALPHA_NUMERIC = 6;
   public static final int ABA_TRACK_2 = 7;
   public static final int WIEGAND_26 = 8;
   public static final int WIEGAND_37 = 9;
   public static final int WIEGAND_37FACILITY = 10;
   public static final int FACILITY_16CARD_32 = 11;
   public static final int FACILITY_32CARD_32 = 12;
   public static final int FASC_N = 13;
   public static final int FASC_NBCD = 14;
   public static final int FASC_NLARGE = 15;
   public static final int FASC_NLARGE_BCD = 16;
   public static final int GSA_75 = 17;
   public static final int CHUID = 18;
   public static final int CHUID_FULL = 19;
   public static final int GUID = 20;
   public static final int CBEFF_A = 21;
   public static final int CBEFF_B = 22;
   public static final int CBEFF_C = 23;
   public static final int USER_PASSWORD = 24;
   public static final BBacnetAuthenticationFactorType undefined = new BBacnetAuthenticationFactorType(0);
   public static final BBacnetAuthenticationFactorType error = new BBacnetAuthenticationFactorType(1);
   public static final BBacnetAuthenticationFactorType custom = new BBacnetAuthenticationFactorType(2);
   public static final BBacnetAuthenticationFactorType simpleNumber16 = new BBacnetAuthenticationFactorType(3);
   public static final BBacnetAuthenticationFactorType simpleNumber32 = new BBacnetAuthenticationFactorType(4);
   public static final BBacnetAuthenticationFactorType simpleNumber56 = new BBacnetAuthenticationFactorType(5);
   public static final BBacnetAuthenticationFactorType simpleAlphaNumeric = new BBacnetAuthenticationFactorType(6);
   public static final BBacnetAuthenticationFactorType abaTrack2 = new BBacnetAuthenticationFactorType(7);
   public static final BBacnetAuthenticationFactorType wiegand26 = new BBacnetAuthenticationFactorType(8);
   public static final BBacnetAuthenticationFactorType wiegand37 = new BBacnetAuthenticationFactorType(9);
   public static final BBacnetAuthenticationFactorType wiegand37Facility = new BBacnetAuthenticationFactorType(10);
   public static final BBacnetAuthenticationFactorType facility16Card32 = new BBacnetAuthenticationFactorType(11);
   public static final BBacnetAuthenticationFactorType facility32Card32 = new BBacnetAuthenticationFactorType(12);
   public static final BBacnetAuthenticationFactorType fascN = new BBacnetAuthenticationFactorType(13);
   public static final BBacnetAuthenticationFactorType fascNBcd = new BBacnetAuthenticationFactorType(14);
   public static final BBacnetAuthenticationFactorType fascNLarge = new BBacnetAuthenticationFactorType(15);
   public static final BBacnetAuthenticationFactorType fascNLargeBcd = new BBacnetAuthenticationFactorType(16);
   public static final BBacnetAuthenticationFactorType gsa75 = new BBacnetAuthenticationFactorType(17);
   public static final BBacnetAuthenticationFactorType chuid = new BBacnetAuthenticationFactorType(18);
   public static final BBacnetAuthenticationFactorType chuidFull = new BBacnetAuthenticationFactorType(19);
   public static final BBacnetAuthenticationFactorType guid = new BBacnetAuthenticationFactorType(20);
   public static final BBacnetAuthenticationFactorType cbeffA = new BBacnetAuthenticationFactorType(21);
   public static final BBacnetAuthenticationFactorType cbeffB = new BBacnetAuthenticationFactorType(22);
   public static final BBacnetAuthenticationFactorType cbeffC = new BBacnetAuthenticationFactorType(23);
   public static final BBacnetAuthenticationFactorType userPassword = new BBacnetAuthenticationFactorType(24);
   public static final BBacnetAuthenticationFactorType DEFAULT = undefined;
   public static final Type TYPE = Sys.loadType(BBacnetAuthenticationFactorType.class);
   public static final int MAX_ASHRAE_ID = 24;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetAuthenticationFactorType make(int ordinal) {
      return (BBacnetAuthenticationFactorType)undefined.getRange().get(ordinal, false);
   }

   public static BBacnetAuthenticationFactorType make(String tag) {
      return (BBacnetAuthenticationFactorType)undefined.getRange().get(tag);
   }

   private BBacnetAuthenticationFactorType(int ordinal) {
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
      return id > 24 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 24;
   }
}
