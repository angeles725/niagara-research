package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.BitField;
import com.prosysopc.ua.UaNamespaceTranslateable;
import com.prosysopc.ua.UaOptionSet;
import com.prosysopc.ua.UaNamespaceTranslateable.Context;
import com.prosysopc.ua.UaOptionSet.Builder;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils.ArrayIterator;
import com.prosysopc.ua.typedictionary.EnumerationSpecification;
import com.prosysopc.ua.typedictionary.OptionSetSpecification;
import com.prosysopc.ua.typedictionary.OptionSetStructure;
import com.prosysopc.ua.typedictionary.OptionSetStructureSpecification;
import com.prosysopc.ua.typedictionary.OptionSpecification;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class Variant implements UaNamespaceTranslateable<Variant> {
   private static final Set<Class<?>> validFinalClasses;
   public static final Variant NULL = new Variant(null);
   public static final Variant[] EMPTY_ARRAY = new Variant[0];
   final Object value;
   final Class<?> sf;

   public static Object[] asObjectArray(Variant... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return new Object[0];
      } else {
         Object[] var1 = new Object[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = var0[var2] == null ? null : var0[var2].getValue();
         }

         return var1;
      }
   }

   public static Variant[] asVariantArray(Object... var0) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return new Variant[0];
      } else {
         Variant[] var1 = new Variant[var0.length];

         for (int var2 = 0; var2 < var0.length; var2++) {
            var1[var2] = new Variant(var0[var2]);
         }

         return var1;
      }
   }

   private static Object byteArraysToByteStrings(Object var0) {
      Class var1 = var0.getClass();
      if (byte[].class.isAssignableFrom(var1)) {
         return ByteString.valueOf((byte[])var0);
      } else if (byte[][].class.isAssignableFrom(var1)) {
         byte[][] var5 = (byte[][])var0;
         ByteString[] var6 = new ByteString[var5.length];

         for (int var7 = 0; var7 < var5.length; var7++) {
            var6[var7] = ByteString.valueOf(var5[var7]);
         }

         return var6;
      } else {
         int[] var2 = calculateArrayDimensions(var0, true);
         ArrayIterator var3 = MultiDimensionArrayUtils.arrayIterator(var0, var2);
         ArrayList var4 = new ArrayList();

         while (var3.hasNext()) {
            var4.add(ByteString.valueOf((byte[])var3.next()));
         }

         return MultiDimensionArrayUtils.demuxArray(var4.toArray(), var2, ByteString.class);
      }
   }

   private static int[] calculateArrayDimensions(Object var0, boolean var1) {
      int var2 = MultiDimensionArrayUtils.getDimension(var0);
      if (var1) {
         var2--;
      }

      int[] var3 = new int[var2];
      if (var2 == 0) {
         return var3;
      } else {
         Object var4 = var0;

         for (int var5 = 0; var5 < var2; var5++) {
            Object[] var6 = (Object[])var4;
            var3[var5] = var6.length;
            if (var6.length == 0) {
               break;
            }

            var4 = var6[0];
         }

         return var3;
      }
   }

   private static Object enumsToInts(Object var0) {
      Class var1 = var0.getClass();
      if (Enumeration.class.isAssignableFrom(var1)) {
         return ((Enumeration)var0).getValue();
      } else if (Enumeration[].class.isAssignableFrom(var1)) {
         Enumeration[] var6 = (Enumeration[])var0;
         Integer[] var7 = new Integer[var6.length];

         for (int var8 = 0; var8 < var6.length; var8++) {
            if (var6[var8] == null) {
               var7[var8] = null;
            } else {
               var7[var8] = var6[var8].getValue();
            }
         }

         return var7;
      } else {
         int[] var2 = calculateArrayDimensions(var0, false);
         Enumeration[] var3 = (Enumeration[])MultiDimensionArrayUtils.muxArray(var0, var2);
         Integer[] var4 = new Integer[var3.length];

         for (int var5 = 0; var5 < var3.length; var5++) {
            if (var3[var5] == null) {
               var4[var5] = null;
            } else {
               var4[var5] = var3[var5].getValue();
            }
         }

         return MultiDimensionArrayUtils.demuxArray(var4, var2);
      }
   }

   private static Class<?> numericOptionSetsToCompositeClass(Object var0) {
      Class var1 = var0.getClass();
      if (UaOptionSet.class.isAssignableFrom(var1)) {
         return ((UaOptionSet)var0).getValue().getClass();
      } else if (UaOptionSet[].class.isAssignableFrom(var1)) {
         UaOptionSet[] var5 = (UaOptionSet[])var0;

         for (int var6 = 0; var6 < var5.length; var6++) {
            if (var5[var6] != null) {
               return var5[var6].getValue().getClass();
            }
         }

         throw new IllegalArgumentException("Variant cannot accept OptionSetDataType arrays that consists only of nulls");
      } else {
         int[] var2 = calculateArrayDimensions(var0, false);
         UaOptionSet[] var3 = (UaOptionSet[])MultiDimensionArrayUtils.muxArray(var0, var2);

         for (int var4 = 0; var4 < var3.length; var4++) {
            if (var3[var4] != null) {
               return var3[var4].getValue().getClass();
            }
         }

         throw new IllegalArgumentException("Variant cannot accept OptionSetDataType arrays that consists only of nulls");
      }
   }

   private static Object numericOptionSetsToInts(Object var0) {
      Class var1 = var0.getClass();
      if (UaOptionSet.class.isAssignableFrom(var1)) {
         return ((UaOptionSet)var0).getValue();
      } else if (UaOptionSet[].class.isAssignableFrom(var1)) {
         UaOptionSet[] var6 = (UaOptionSet[])var0;
         Object[] var7 = (Object[])Array.newInstance(numericOptionSetsToCompositeClass(var0), var6.length);

         for (int var8 = 0; var8 < var6.length; var8++) {
            if (var6[var8] == null) {
               var7[var8] = null;
            } else {
               var7[var8] = var6[var8].getValue();
            }
         }

         return var7;
      } else {
         int[] var2 = calculateArrayDimensions(var0, false);
         UaOptionSet[] var3 = (UaOptionSet[])MultiDimensionArrayUtils.muxArray(var0, var2);
         Object[] var4 = (Object[])Array.newInstance(numericOptionSetsToCompositeClass(var0), var3.length);

         for (int var5 = 0; var5 < var4.length; var5++) {
            if (var3[var5] == null) {
               var4[var5] = null;
            } else {
               var4[var5] = var3[var5].getValue();
            }
         }

         return MultiDimensionArrayUtils.demuxArray(var4, var2);
      }
   }

   public Variant(Object var1) {
      if (var1 == null) {
         this.value = null;
         this.sf = null;
      } else {
         Class var2 = MultiDimensionArrayUtils.getComponentType(var1.getClass());
         if (UaOptionSet.class.isAssignableFrom(var2) && !OptionSetStructure.class.isAssignableFrom(var2)) {
            this.sf = numericOptionSetsToCompositeClass(var1);
            this.value = numericOptionSetsToInts(var1);
         } else if (Enumeration.class.isAssignableFrom(var2)) {
            this.value = enumsToInts(var1);
            this.sf = Integer.class;
         } else if (byte.class.isAssignableFrom(var2)) {
            this.value = byteArraysToByteStrings(var1);
            this.sf = ByteString.class;
         } else {
            this.e(var2);
            this.value = var1;
            this.sf = var2;
         }
      }
   }

   public <T> T asClass(Class<T> var1, T var2) {
      if (this.value == null) {
         return (T)var2;
      } else {
         try {
            return (T)var1.cast(this.value);
         } catch (ClassCastException var4) {
            return (T)var2;
         }
      }
   }

   public <T extends Enum<T> & Enumeration> Object asEnum(Class<T> var1) {
      if (this.value == null) {
         return null;
      } else if (!Integer.class.equals(this.sf)) {
         throw new ClassCastException(
            "Variant.asEnum can only be called on non-null Variants that have compositeClass of Integer, was: " + this.toStringWithType()
         );
      } else {
         Enum[] var2 = (Enum[])var1.getEnumConstants();
         HashMap var3 = new HashMap();

         for (Enum var7 : var2) {
            var3.put(((Enumeration)var7).getValue(), var7);
         }

         Class var9 = this.value.getClass();
         if (Integer.class.equals(var9)) {
            Integer var12 = (Integer)this.value;
            return var3.get(var12);
         } else if (Integer[].class.equals(var9)) {
            Integer[] var11 = (Integer[])this.value;
            Enum[] var14 = (Enum[])Array.newInstance(var1, var11.length);

            for (int var16 = 0; var16 < var11.length; var16++) {
               var14[var16] = (Enum)var3.get(var11[var16]);
            }

            return var14;
         } else {
            int[] var10 = calculateArrayDimensions(this.value, false);
            Integer[] var13 = (Integer[])MultiDimensionArrayUtils.muxArray(this.value, var10);
            Enum[] var15 = (Enum[])Array.newInstance(var1, var13.length);

            for (int var8 = 0; var8 < var15.length; var8++) {
               var15[var8] = (Enum)var3.get(var13[var8]);
            }

            return MultiDimensionArrayUtils.demuxArray(var15, var10);
         }
      }
   }

   public Object asEnum(EnumerationSpecification var1) {
      if (this.value == null) {
         return null;
      } else if (!Integer.class.equals(this.sf)) {
         throw new ClassCastException(
            "Variant.asEnum can only be called on non-null Variants that have compositeClass of Integer, was: " + this.toStringWithType()
         );
      } else {
         Class var2 = var1.getJavaClass();
         return MultiDimensionArrayUtils.map(this.value, Integer.class, var2, var1x -> var1.getByValue(var1x));
      }
   }

   public Object asOptionSet(final OptionSetSpecification var1) {
      if (this.value == null) {
         return null;
      } else if (var1 instanceof OptionSetStructureSpecification) {
         return this.value;
      } else if (!var1.getBaseTypeJavaClass().isAssignableFrom(this.getCompositeClass())) {
         throw new ClassCastException("Expected " + var1.getBaseTypeJavaClass() + " but Variant composite class is " + this.getCompositeClass());
      } else {
         return MultiDimensionArrayUtils.map(this.value, this.getCompositeClass(), var1.getBaseTypeJavaClass(), new Function() {
            @Override
            public Object apply(Object var1x) {
               if (var1x == null) {
                  return null;
               } else {
                  BitField var2 = (BitField)var1x;
                  Builder var3 = var1.toInstanceBuilder();

                  for (OptionSpecification var5 : var1.getOptions()) {
                     if (var2.isBitSet(var5.getBitPosition())) {
                        var3.add(new OptionSpecification[]{var5});
                     }
                  }

                  return var3.build();
               }
            }
         });
      }
   }

   public boolean booleanValue() {
      if (this.value instanceof Boolean) {
         return (Boolean)this.value;
      } else if (this.isNumber()) {
         return this.longValue() != 0L;
      } else if (this.isEmpty()) {
         throw new ClassCastException("Variant null cannot be cast to boolean");
      } else if (this.getCompositeClass().equals(String.class)) {
         String var1 = ((String)this.getValue()).toLowerCase(Locale.ROOT);
         if (var1.equals("true") || var1.equals("1")) {
            return true;
         } else if (!var1.equals("false") && !var1.equals("0")) {
            throw new ClassCastException("Variant String cannot be cast to boolean: " + var1);
         } else {
            return false;
         }
      } else {
         return this.asClass(Boolean.class, false);
      }
   }

   public byte byteValue() {
      return this.toNumber().byteValue();
   }

   public double doubleValue() {
      return this.toNumber().doubleValue();
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 == null) {
         return false;
      } else if (!(var1 instanceof Variant)) {
         return false;
      } else {
         Variant var2 = (Variant)var1;
         if (this.value == null && var2.value == null) {
            return true;
         } else if (this.value == null && var2.value != null) {
            return false;
         } else if (this.value != null && var2.value == null) {
            return false;
         } else {
            Class var3 = this.value.getClass();
            if (!var3.equals(var2.value.getClass())) {
               return false;
            } else {
               return !this.isArray() ? this.value.equals(var2.value) : Arrays.deepEquals((Object[])this.value, (Object[])var2.value);
            }
         }
      }
   }

   public float floatValue() {
      return this.toNumber().floatValue();
   }

   public int[] getArrayDimensions() {
      return calculateArrayDimensions(this.value, false);
   }

   public Class<?> getCompositeClass() {
      return this.sf;
   }

   public int getDimension() {
      return MultiDimensionArrayUtils.getDimension(this.value);
   }

   public Object getValue() {
      return this.value;
   }

   @Override
   public int hashCode() {
      if (this.value == null) {
         return 0;
      } else {
         return !this.isArray() ? this.value.hashCode() : Arrays.deepHashCode((Object[])this.value);
      }
   }

   public int intValue() {
      return this.toNumber().intValue();
   }

   public boolean isArray() {
      return this.value == null ? false : this.value.getClass().isArray();
   }

   public boolean isComparable() {
      return this.value instanceof Comparable;
   }

   public boolean isEmpty() {
      return this.value == null;
   }

   public boolean isNumber() {
      return this.value instanceof Number || this.value instanceof Boolean;
   }

   public long longValue() {
      return this.toNumber().longValue();
   }

   public short shortValue() {
      return this.toNumber().shortValue();
   }

   public Number toNumber() {
      if (this.value instanceof Boolean) {
         return this.booleanValue() ? 1 : 0;
      } else if (this.isNumber()) {
         return (Number)this.value;
      } else {
         throw new ClassCastException("Variant is not a Number; CompositeClass=" + this.getCompositeClass());
      }
   }

   @Override
   public String toString() {
      return this.toString(false);
   }

   public String toString(boolean var1) {
      if (this.value == null) {
         return "(null)";
      } else {
         String var2 = "";
         String var3;
         if (this.isArray()) {
            if (var1) {
               int[] var4 = this.getArrayDimensions();
               StringBuilder var5 = new StringBuilder();
               var5.append(var4[0]);

               for (int var6 = 1; var6 < this.getDimension(); var6++) {
                  var5.append(",").append(var4[var6]);
               }

               var2 = String.format(Locale.ROOT, "(%s[%s]) ", this.compositeClassToString(), var5.toString());
            }

            var3 = MultiDimensionArrayUtils.toString(this.value);
         } else {
            if (var1) {
               var2 = String.format(Locale.ROOT, "(%s) ", this.compositeClassToString());
            }

            var3 = MultiDimensionArrayUtils.toString(this.value);
         }

         return var2 + var3;
      }
   }

   public String toStringWithType() {
      return this.toString(true);
   }

   public boolean valueEquals(Variant var1) {
      if (var1 == null) {
         return false;
      } else if (!this.isEmpty() && !var1.isEmpty()) {
         if (this.getCompositeClass().equals(var1.getCompositeClass())) {
            return this.equals(var1);
         } else if (this.isNumber() && var1.isNumber()) {
            return this.floatValue() == var1.floatValue();
         } else {
            Object var2 = var1.asClass(this.getCompositeClass(), null);
            return var2 != null ? this.equals(new Variant(var2)) : var1.equals(this.asClass(var1.getCompositeClass(), null));
         }
      } else {
         return false;
      }
   }

   public Variant withTranslatedNamespaces(Context var1) {
      Object var2 = MultiDimensionArrayUtils.map(
         this.value,
         this.getCompositeClass(),
         this.getCompositeClass(),
         var1x -> var1x instanceof UaNamespaceTranslateable ? ((UaNamespaceTranslateable)var1x).withTranslatedNamespaces(var1) : var1x
      );
      return new Variant(var2);
   }

   protected String compositeClassToString() {
      return this.getCompositeClass().getSimpleName();
   }

   void e(Class<?> var1) {
      if (!validFinalClasses.contains(var1)) {
         if (!DateTime.class.isAssignableFrom(var1)) {
            if (!ExtensionObject.class.isAssignableFrom(var1)) {
               if (!DataValue.class.isAssignableFrom(var1)) {
                  if (!DiagnosticInfo.class.isAssignableFrom(var1)) {
                     if (!Variant.class.isAssignableFrom(var1)) {
                        if (!Structure.class.isAssignableFrom(var1)) {
                           if (!BigDecimal.class.isAssignableFrom(var1)) {
                              throw new IllegalArgumentException("Variant cannot be " + var1.getCanonicalName());
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static {
      HashSet var0 = new HashSet();
      var0.add(Boolean.class);
      var0.add(Byte.class);
      var0.add(UnsignedByte.class);
      var0.add(Short.class);
      var0.add(UnsignedShort.class);
      var0.add(Integer.class);
      var0.add(UnsignedInteger.class);
      var0.add(Long.class);
      var0.add(UnsignedLong.class);
      var0.add(Float.class);
      var0.add(Double.class);
      var0.add(String.class);
      var0.add(UUID.class);
      var0.add(XmlElement.class);
      var0.add(NodeId.class);
      var0.add(ExpandedNodeId.class);
      var0.add(StatusCode.class);
      var0.add(QualifiedName.class);
      var0.add(LocalizedText.class);
      var0.add(ByteString.class);
      validFinalClasses = Collections.unmodifiableSet(var0);
   }
}
