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
   range = {@Range("defaultTerminal"), @Range("ansiX3_64"), @Range("decVt52"), @Range("decVt100"), @Range("decVt220"), @Range("hp700_94"), @Range("ibm3130")}
)
public final class BBacnetVtClass extends BFrozenEnum implements BacnetConst {
   public static final int DEFAULT_TERMINAL = 0;
   public static final int ANSI_X3_64 = 1;
   public static final int DEC_VT_52 = 2;
   public static final int DEC_VT_100 = 3;
   public static final int DEC_VT_220 = 4;
   public static final int HP_700_94 = 5;
   public static final int IBM_3130 = 6;
   public static final BBacnetVtClass defaultTerminal = new BBacnetVtClass(0);
   public static final BBacnetVtClass ansiX3_64 = new BBacnetVtClass(1);
   public static final BBacnetVtClass decVt52 = new BBacnetVtClass(2);
   public static final BBacnetVtClass decVt100 = new BBacnetVtClass(3);
   public static final BBacnetVtClass decVt220 = new BBacnetVtClass(4);
   public static final BBacnetVtClass hp700_94 = new BBacnetVtClass(5);
   public static final BBacnetVtClass ibm3130 = new BBacnetVtClass(6);
   public static final BBacnetVtClass DEFAULT = defaultTerminal;
   public static final Type TYPE = Sys.loadType(BBacnetVtClass.class);
   public static final int MAX_ASHRAE_ID = 6;
   public static final int MAX_RESERVED_ID = 63;
   public static final int MAX_ID = 65535;

   public static BBacnetVtClass make(int ordinal) {
      return (BBacnetVtClass)defaultTerminal.getRange().get(ordinal, false);
   }

   public static BBacnetVtClass make(String tag) {
      return (BBacnetVtClass)defaultTerminal.getRange().get(tag);
   }

   private BBacnetVtClass(int ordinal) {
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
      return id > 6 && id <= 63;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 6;
   }
}
