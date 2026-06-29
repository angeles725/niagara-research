package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.UaNamespaceTranslateable;
import com.prosysopc.ua.UaNamespaceTranslateable.Context;
import com.prosysopc.ua.stack.encoding.IEncodeable;
import com.prosysopc.ua.stack.utils.MultiDimensionArrayUtils;
import com.prosysopc.ua.typedictionary.FieldSpecification;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import java.util.Map;
import java.util.function.Function;

public interface Structure extends UaNamespaceTranslateable<Structure>, IEncodeable, Cloneable {
   Object get(String var1);

   void clear();

   Structure clone();

   Object get(FieldSpecification var1);

   @Deprecated
   ExpandedNodeId getBinaryEncodeId();

   @Deprecated
   ExpandedNodeId getJsonEncodeId();

   @Deprecated
   ExpandedNodeId getTypeId();

   @Deprecated
   ExpandedNodeId getXmlEncodeId();

   void set(FieldSpecification var1, Object var2);

   void set(String var1, Object var2);

   StructureSpecification specification();

   Structure.Builder toBuilder();

   Map<String, Object> toFieldNamesMap();

   Map<String, Object> toFieldNamesMap(StructureSpecification var1);

   Map<FieldSpecification, Object> toFieldsMap();

   Map<FieldSpecification, Object> toFieldsMap(StructureSpecification var1);

   default <T> T toMap(Structure.MapKind<T> var1) {
      return (T)var1.a(this);
   }

   default Structure withTranslatedNamespaces(Context var1) {
      Structure.Builder var2 = this.specification().toInstanceBuilder();
      this.toFieldsMap()
         .forEach(
            (var2x, var3) -> {
               if (var3 != null) {
                  Object var4 = MultiDimensionArrayUtils.map(
                     var3,
                     var2x.getCompositeClass(),
                     var2x.getCompositeClass(),
                     var1xx -> var1xx instanceof UaNamespaceTranslateable ? ((UaNamespaceTranslateable)var1xx).withTranslatedNamespaces(var1) : var1xx
                  );
                  var2.set(var2x, var4);
               }
            }
         );
      return var2.build();
   }

   public interface Builder {
      Object get(String var1);

      Structure build();

      Structure.Builder clear();

      Object get(FieldSpecification var1);

      Structure.Builder set(FieldSpecification var1, Object var2);

      Structure.Builder set(String var1, Object var2);

      StructureSpecification specification();
   }

   public static class MapKind<T> {
      private static final Structure.MapKind<Map<String, Object>> TO_FIELD_NAMES = new Structure.MapKind<>(var0 -> var0.toFieldNamesMap());
      private static final Structure.MapKind<Map<FieldSpecification, Object>> TO_FIELDS = new Structure.MapKind<>(var0 -> var0.toFieldsMap());
      private Function<Structure, T> transformer;

      public static Structure.MapKind<Map<String, Object>> toFieldNamesMap() {
         return TO_FIELD_NAMES;
      }

      public static Structure.MapKind<Map<String, Object>> toFieldNamesMap(StructureSpecification var0) {
         return new Structure.MapKind<>(var1 -> var1.toFieldNamesMap(var0));
      }

      public static Structure.MapKind<Map<FieldSpecification, Object>> toFieldsMap() {
         return TO_FIELDS;
      }

      public static Structure.MapKind<Map<FieldSpecification, Object>> toFieldsMap(StructureSpecification var0) {
         return new Structure.MapKind<>(var1 -> var1.toFieldsMap(var0));
      }

      MapKind(Function<Structure, T> var1) {
         this.transformer = var1;
      }

      T a(Structure var1) {
         return this.transformer.apply(var1);
      }
   }
}
