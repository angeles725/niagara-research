package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.BitField;
import com.prosysopc.ua.stack.common.StatusCodeDescriptions;
import com.prosysopc.ua.stack.core.Identifiers;
import java.util.Locale;

public final class StatusCode implements BitField {
   @Deprecated
   public static final NodeId ID = Identifiers.StatusCode;
   public static final StatusCode[] EMPTY_ARRAY = new StatusCode[0];
   public static final int SEVERITY_MASK = -1073741824;
   public static final int SEVERITY_GOOD = 0;
   public static final int SEVERITY_UNCERTAIN = 1073741824;
   public static final int SEVERITY_BAD = Integer.MIN_VALUE;
   public static final StatusCode GOOD = getFromBits(0);
   public static final StatusCode BAD = getFromBits(Integer.MIN_VALUE);
   public static final int RES1_MASK = 805306368;
   public static final int SUBCODE_MASK = 268369920;
   public static final int STRUCTURECHANGED_MASK = 32768;
   public static final int SEMANTICSCHANGED_MASK = 16384;
   public static final int RES2_MASK = 12288;
   public static final int INFOTYPE_MASK = 3072;
   public static final int INFOTYPE_DATAVALUE = 1024;
   public static final int INFOBITS_MASK = 1023;
   public static final int LIMITBITS_MASK = 768;
   public static final int LIMITBITS_NONE = 0;
   public static final int LIMITBITS_LOW = 256;
   public static final int LIMITBITS_HIGH = 512;
   public static final int LIMITBITS_CONSTANT = 768;
   public static final int OVERFLOW_MASK = 1152;
   public static final int OVERFLOW_BIT = 128;
   public static final int HISTORIANBITS_MASK = 31;
   public static final int HISTORIANBITS_RAW = 0;
   public static final int HISTORIANBITS_CALCULATED = 1;
   public static final int HISTORIANBITS_INTERPOLATED = 2;
   public static final int HISTORIANBITS_RESERVED = 3;
   public static final int HISTORIANBITS_PARTIAL = 4;
   public static final int HISTORIANBITS_EXTRADATA = 8;
   public static final int HISTORIANBITS_MULTIVALUE = 16;
   private final int value;

   public static StatusCode getFromBits(int var0) {
      return new StatusCode(var0);
   }

   public static StatusCode valueOf(UnsignedInteger var0) {
      return new StatusCode(var0);
   }

   @Deprecated
   public StatusCode(UnsignedInteger var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("Parameter value must be non-null");
      } else {
         this.value = var1.intValue();
      }
   }

   private StatusCode(int var1) {
      this.value = var1;
   }

   @Override
   public boolean equals(Object var1) {
      if (!(var1 instanceof StatusCode)) {
         return false;
      } else {
         StatusCode var2 = (StatusCode)var1;
         return this.value == var2.value;
      }
   }

   public boolean equalsStatusCode(StatusCode var1) {
      return this.isStatusCode(var1.getValue());
   }

   public String getDescription() {
      String var1 = StatusCodeDescriptions.getStatusCodeDescription(this.value);
      return var1 == null ? "" : var1;
   }

   public int getHistorianBits() {
      return this.value & 31;
   }

   public int getInfotype() {
      return this.value & 3072;
   }

   public int getLimitBits() {
      return this.value & 768;
   }

   public String getName() {
      if (this.value == GOOD.value) {
         return "GOOD";
      } else if (this.value == BAD.value) {
         return "BAD";
      } else {
         String var1 = StatusCodeDescriptions.getStatusCode(this.value);
         return var1 == null ? "" : var1;
      }
   }

   public int getSeverity() {
      return this.value & -1073741824;
   }

   public int getSubcode() {
      return this.value & 268369920;
   }

   public UnsignedInteger getValue() {
      return UnsignedInteger.getFromBits(this.value);
   }

   public int getValueAsIntBits() {
      return this.value;
   }

   @Override
   public int hashCode() {
      return this.value;
   }

   public boolean isBad() {
      return (this.value & -1073741824) == Integer.MIN_VALUE;
   }

   public boolean isBitSet(int var1) {
      return this.getValue().isBitSet(var1);
   }

   public boolean isGood() {
      return (this.value & -1073741824) == 0;
   }

   public boolean isNotBad() {
      return (this.value & -1073741824) != Integer.MIN_VALUE;
   }

   public boolean isNotGood() {
      return (this.value & -1073741824) != 0;
   }

   public boolean isNotUncertain() {
      return (this.value & -1073741824) != 1073741824;
   }

   public boolean isOverflow() {
      return (this.value & 1152) != 0;
   }

   public boolean isSemanticsChanged() {
      return (this.value & 16384) != 0;
   }

   public boolean isStatusCode(UnsignedInteger var1) {
      int var2 = -805371904;
      return (var1.intValue() & var2) == (this.value & var2);
   }

   public boolean isStructureChanged() {
      return (this.value & 32768) != 0;
   }

   public boolean isUncertain() {
      return (this.value & -1073741824) == 1073741824;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "%s (0x%08X) \"%s\"", this.getName(), this.value, this.getDescription());
   }
}
