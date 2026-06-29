package javax.baja.bacnet.enums;

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
   range = {@Range("unsilenced"), @Range("audibleSilenced"), @Range("visibleSilenced"), @Range("allSilenced")}
)
public final class BBacnetSilencedState extends BFrozenEnum implements BacnetConst {
   public static final int UNSILENCED = 0;
   public static final int AUDIBLE_SILENCED = 1;
   public static final int VISIBLE_SILENCED = 2;
   public static final int ALL_SILENCED = 3;
   public static final BBacnetSilencedState unsilenced = new BBacnetSilencedState(0);
   public static final BBacnetSilencedState audibleSilenced = new BBacnetSilencedState(1);
   public static final BBacnetSilencedState visibleSilenced = new BBacnetSilencedState(2);
   public static final BBacnetSilencedState allSilenced = new BBacnetSilencedState(3);
   public static final BBacnetSilencedState DEFAULT = unsilenced;
   public static final Type TYPE = Sys.loadType(BBacnetSilencedState.class);
   public static final int MAX_ASHRAE_ID = 3;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetSilencedState make(int ordinal) {
      return (BBacnetSilencedState)unsilenced.getRange().get(ordinal, false);
   }

   public static BBacnetSilencedState make(String tag) {
      return (BBacnetSilencedState)unsilenced.getRange().get(tag);
   }

   private BBacnetSilencedState(int ordinal) {
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
      return id > 3 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 3;
   }
}
