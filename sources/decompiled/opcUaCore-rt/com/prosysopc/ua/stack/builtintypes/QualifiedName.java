package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.UaNamespaceTranslateable;
import com.prosysopc.ua.UaQualifiedName;
import com.prosysopc.ua.UaNamespaceTranslateable.Context;
import com.prosysopc.ua.stack.common.NamespaceTable;
import com.prosysopc.ua.stack.core.Identifiers;
import java.util.Comparator;

public final class QualifiedName implements UaNamespaceTranslateable<QualifiedName>, Comparable<QualifiedName> {
   @Deprecated
   public static final NodeId ID = Identifiers.QualifiedName;
   public static final QualifiedName NULL = new QualifiedName(UnsignedShort.valueOf(0), null);
   public static final QualifiedName DEFAULT_BINARY_ENCODING = new QualifiedName("Default Binary");
   public static final QualifiedName DEFAULT_XML_ENCODING = new QualifiedName("Default XML");
   public static final QualifiedName[] EMPTY_ARRAY = new QualifiedName[0];
   private int namespaceIndex;
   private String name;

   public static QualifiedName[] arrayFrom(UaQualifiedName[] var0, NamespaceTable var1) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         QualifiedName[] var2 = new QualifiedName[var0.length];

         for (int var3 = 0; var3 < var0.length; var3++) {
            var2[var3] = from(var0[var3], var1);
         }

         return var2;
      }
   }

   public static QualifiedName from(UaQualifiedName var0, NamespaceTable var1) {
      return var0 == null ? null : var0.toQualifiedName(var1);
   }

   public static boolean isNull(QualifiedName var0) {
      return var0 == null || var0.equals(NULL);
   }

   public static boolean isNullOrEmpty(QualifiedName var0) {
      return isNull(var0) ? true : "".equals(var0.name);
   }

   public static QualifiedName parseQualifiedName(String var0) {
      String[] var1 = var0.split(":");
      UnsignedShort var2 = UnsignedShort.ZERO;
      String var3 = var0;
      if (var1.length > 1) {
         try {
            var2 = UnsignedShort.parseUnsignedShort(var1[0]);
            var3 = var0.substring(var1[0].length() + 1);
         } catch (NumberFormatException var5) {
         } catch (IllegalArgumentException var6) {
         }
      }

      return new QualifiedName(var2, var3);
   }

   public QualifiedName(int var1, String var2) {
      if (var1 >= UnsignedShort.MIN_VALUE.intValue() && var1 <= UnsignedShort.MAX_VALUE.intValue()) {
         this.namespaceIndex = var1;
         this.name = var2;
      } else {
         throw new IllegalArgumentException("namespace index out of bounds");
      }
   }

   public QualifiedName(String var1) {
      if (var1 == null) {
         throw new IllegalArgumentException("name argument must not be null");
      } else {
         this.namespaceIndex = 0;
         this.name = var1;
      }
   }

   public QualifiedName(UnsignedShort var1, String var2) {
      this.namespaceIndex = var1.intValue();
      this.name = var2;
   }

   public int compareTo(QualifiedName var1) {
      int var2 = Integer.compare(this.namespaceIndex, var1.namespaceIndex);
      return var2 != 0 ? var2 : Comparator.<String>nullsFirst((var0, var1x) -> var0.compareTo(var1x)).compare(this.name, var1.name);
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return isNull(this);
      } else if (this.getClass() != var1.getClass()) {
         return false;
      } else {
         QualifiedName var2 = (QualifiedName)var1;
         if (this.namespaceIndex != var2.namespaceIndex) {
            return false;
         } else {
            if (this.name == null) {
               if (var2.name != null) {
                  return false;
               }
            } else if (!this.name.equals(var2.name)) {
               return false;
            }

            return true;
         }
      }
   }

   public String getName() {
      return this.name;
   }

   public int getNamespaceIndex() {
      return this.namespaceIndex;
   }

   @Override
   public int hashCode() {
      byte var1 = 31;
      int var2 = 1;
      var2 = 31 * var2 + (this.name == null ? 0 : this.name.hashCode());
      return 31 * var2 + this.namespaceIndex;
   }

   @Override
   public String toString() {
      return this.namespaceIndex > 0 ? this.namespaceIndex + ":" + this.name : this.name;
   }

   public QualifiedName withTranslatedNamespaces(Context var1) {
      return new QualifiedName((Integer)var1.getNamespaceIndexTranslation().apply(this.getNamespaceIndex()), this.name);
   }
}
