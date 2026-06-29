package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.UaNamespaceTranslateable;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.UaNamespaceTranslateable.Context;
import com.prosysopc.ua.stack.common.NamespaceTable;
import com.prosysopc.ua.stack.core.IdType;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NodeId implements UaNamespaceTranslateable<NodeId>, Comparable<NodeId> {
   private static final ByteString NULL_OPAQUE_VALUE = ByteString.EMPTY;
   private static final UUID NULL_GUID_VALUE = new UUID(0L, 0L);
   @Deprecated
   public static final NodeId ZERO = new NodeId(0, UnsignedInteger.getFromBits(0));
   public static final NodeId NULL_NUMERIC = new NodeId(0, UnsignedInteger.getFromBits(0));
   public static final NodeId NULL_STRING = get(IdType.String, 0, "");
   public static final NodeId NULL_GUID = get(IdType.Guid, 0, NULL_GUID_VALUE);
   public static final NodeId NULL_OPAQUE = get(IdType.Opaque, 0, NULL_OPAQUE_VALUE);
   public static final NodeId NULL = NULL_NUMERIC;
   @Deprecated
   public static final NodeId ID = Identifiers.NodeId;
   static final Pattern rX = Pattern.compile("ns=(\\d*);i=(\\d*)");
   static final Pattern rY = Pattern.compile("i=(\\d*)");
   static final Pattern rZ = Pattern.compile("ns=(\\d*);s=(.*)");
   static final Pattern sa = Pattern.compile("s=(.*)");
   static final Pattern sb = Pattern.compile("ns=(\\d*);g=([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");
   static final Pattern sc = Pattern.compile("g=([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");
   static final Pattern sd = Pattern.compile("ns=(\\d*);b=([0-9a-zA-Z\\+/=]*)");
   static final Pattern se = Pattern.compile("b=([0-9a-zA-Z\\+/=]*)");
   public static final NodeId[] EMPTY_ARRAY = new NodeId[0];
   final IdType rU;
   final int namespaceIndex;
   final Object value;
   final boolean isNull;
   final int hash;

   public static NodeId[] arrayFrom(UaNodeId[] var0, NamespaceTable var1) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         NodeId[] var2 = new NodeId[var0.length];

         for (int var3 = 0; var3 < var0.length; var3++) {
            var2[var3] = from(var0[var3], var1);
         }

         return var2;
      }
   }

   @Deprecated
   public static NodeId decode(String var0) throws IllegalArgumentException {
      return parseNodeId(var0);
   }

   @Deprecated
   public static boolean equals(NodeId var0, NodeId var1) {
      return Objects.equals(var0, var1);
   }

   public static NodeId from(UaNodeId var0, NamespaceTable var1) {
      return var0 == null ? null : var0.asNodeId(var1);
   }

   public static NodeId get(IdType var0, int var1, Object var2) {
      if (var0 == IdType.Guid) {
         return new NodeId(var1, (UUID)var2);
      } else if (var0 == IdType.Numeric) {
         return new NodeId(var1, (UnsignedInteger)var2);
      } else if (var0 == IdType.Opaque) {
         return var2 instanceof byte[] ? new NodeId(var1, ByteString.valueOf((byte[])var2)) : new NodeId(var1, (ByteString)var2);
      } else if (var0 == IdType.String) {
         return new NodeId(var1, (String)var2);
      } else {
         throw new IllegalArgumentException("bad type");
      }
   }

   public static boolean isNull(NodeId var0) {
      return var0 == null || var0.isNullNodeId();
   }

   public static NodeId parseNodeId(String var0) throws IllegalArgumentException {
      if (var0 == null) {
         throw new IllegalArgumentException("null arg");
      } else {
         Matcher var1 = sa.matcher(var0);
         if (var1.matches()) {
            String var17 = var1.group(1);
            return new NodeId(0, var17);
         } else {
            var1 = rY.matcher(var0);
            if (var1.matches()) {
               UnsignedInteger var16 = UnsignedInteger.valueOf(var1.group(1));
               return new NodeId(0, var16);
            } else {
               var1 = sc.matcher(var0);
               if (var1.matches()) {
                  UUID var15 = UUID.fromString(var1.group(1));
                  return new NodeId(0, var15);
               } else {
                  var1 = se.matcher(var0);
                  if (var1.matches()) {
                     byte[] var14 = CryptoUtil.base64Decode(var1.group(1));
                     return new NodeId(0, ByteString.valueOf(var14));
                  } else {
                     var1 = rX.matcher(var0);
                     if (var1.matches()) {
                        Integer var13 = Integer.valueOf(var1.group(1));
                        UnsignedInteger var20 = UnsignedInteger.valueOf(var1.group(2));
                        return new NodeId(var13, var20);
                     } else {
                        var1 = rZ.matcher(var0);
                        if (var1.matches()) {
                           Integer var12 = Integer.valueOf(var1.group(1));
                           String var19 = var1.group(2);
                           return new NodeId(var12, var19);
                        } else {
                           var1 = sb.matcher(var0);
                           if (var1.matches()) {
                              Integer var11 = Integer.valueOf(var1.group(1));
                              UUID var18 = UUID.fromString(var1.group(2));
                              return new NodeId(var11, var18);
                           } else {
                              var1 = sd.matcher(var0);
                              if (var1.matches()) {
                                 Integer var2 = Integer.valueOf(var1.group(1));
                                 byte[] var3 = CryptoUtil.base64Decode(var1.group(2));
                                 return new NodeId(var2, ByteString.valueOf(var3));
                              } else {
                                 throw new IllegalArgumentException("Invalid string representation of a nodeId: " + var0);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static NodeId randomGUID(int var0) {
      return new NodeId(var0, UUID.randomUUID());
   }

   public NodeId(int var1, byte[] var2) {
      this(var1, ByteString.valueOf(var2));
   }

   public NodeId(int var1, ByteString var2) {
      if (var1 >= 0 && var1 <= 65535) {
         if (var2 != null && var2.getLength() > 4096) {
            throw new IllegalArgumentException("The length is restricted to 4096 bytes");
         } else {
            this.rU = IdType.Opaque;
            this.value = var2;
            this.namespaceIndex = var1;
            this.isNull = this.calcIsNull();
            this.hash = this.calcHashCode();
         }
      } else {
         throw new IllegalArgumentException("namespaceIndex out of bounds");
      }
   }

   public NodeId(int var1, int var2) {
      this(var1, UnsignedInteger.getFromBits(var2));
   }

   public NodeId(int var1, String var2) {
      if (var1 >= 0 && var1 <= 65535) {
         if (var2 != null && var2.length() > 4096) {
            throw new IllegalArgumentException("The length is restricted to 4096 characters");
         } else {
            this.rU = IdType.String;
            this.value = var2;
            this.namespaceIndex = var1;
            this.isNull = this.calcIsNull();
            this.hash = this.calcHashCode();
         }
      } else {
         throw new IllegalArgumentException("namespaceIndex out of bounds");
      }
   }

   public NodeId(int var1, UnsignedInteger var2) {
      if (var2 == null) {
         throw new IllegalArgumentException("Numeric NodeId cannot be null");
      } else if (var1 >= 0 && var1 <= 65535) {
         this.value = var2;
         this.namespaceIndex = var1;
         this.rU = IdType.Numeric;
         this.isNull = this.calcIsNull();
         this.hash = this.calcHashCode();
      } else {
         throw new IllegalArgumentException("namespaceIndex out of bounds");
      }
   }

   public NodeId(int var1, UUID var2) {
      if (var1 < 0 || var1 > 65535) {
         throw new IllegalArgumentException("namespaceIndex out of bounds");
      } else if (var2 == null) {
         throw new IllegalArgumentException("Numeric NodeId cannot be null");
      } else {
         this.rU = IdType.Guid;
         this.value = var2;
         this.namespaceIndex = var1;
         this.isNull = this.calcIsNull();
         this.hash = this.calcHashCode();
      }
   }

   public int compareTo(NodeId var1) {
      int var2 = this.namespaceIndex - var1.namespaceIndex;
      if (var2 == 0) {
         var2 = this.rU.getValue() - var1.rU.getValue();
      }

      if (var2 == 0) {
         switch (this.rU) {
            case Numeric:
               return ((UnsignedInteger)this.value).compareTo((Number)((UnsignedInteger)var1.value));
            case String:
               return ((String)this.value).compareTo((String)var1.value);
            case Guid:
               return ((UUID)this.value).compareTo((UUID)var1.value);
            case Opaque:
               return ((ByteString)this.value).compareTo((ByteString)var1.value);
            default:
               throw new Error("Unkonwn IdType:" + this.rU);
         }
      } else {
         return var2;
      }
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 == null) {
         return isNull(this);
      } else if (var1 instanceof NodeId) {
         NodeId var3 = (NodeId)var1;
         if (this.hash != var3.hash) {
            return false;
         } else if (isNull(this) || isNull(var3)) {
            return isNull(this) == isNull(var3);
         } else if (var3.namespaceIndex != this.namespaceIndex || var3.rU != this.rU) {
            return false;
         } else {
            return this.value == var3.value ? true : var3.value.equals(this.value);
         }
      } else if (!(var1 instanceof ExpandedNodeId)) {
         return false;
      } else {
         ExpandedNodeId var2 = (ExpandedNodeId)var1;
         if ((var2.namespaceUri == null || var2.namespaceUri == "http://opcfoundation.org/UA/") && var2.isLocal()) {
            if (this.namespaceIndex != var2.namespaceIndex || this.rU != var2.rU) {
               return false;
            } else {
               return this.value == var2.value ? true : this.value.equals(var2.value);
            }
         } else {
            return false;
         }
      }
   }

   public IdType getIdType() {
      return this.rU;
   }

   public int getNamespaceIndex() {
      return this.namespaceIndex;
   }

   public Object getValue() {
      return this.value;
   }

   @Override
   public int hashCode() {
      return this.hash;
   }

   public boolean isNullNodeId() {
      return this.isNull;
   }

   @Override
   public String toString() {
      String var1 = this.namespaceIndex > 0 ? "ns=" + this.namespaceIndex + ";" : "";
      if (this.rU == IdType.Numeric) {
         return var1 + "i=" + this.value;
      } else if (this.rU == IdType.String) {
         return var1 + "s=" + this.value;
      } else if (this.rU == IdType.Guid) {
         return var1 + "g=" + this.value;
      } else if (this.rU == IdType.Opaque) {
         return this.value == null ? var1 + "b=null" : var1 + "b=" + new String(CryptoUtil.base64Encode(((ByteString)this.value).getValue()));
      } else {
         return "error";
      }
   }

   public NodeId withTranslatedNamespaces(Context var1) {
      return get(this.getIdType(), (Integer)var1.getNamespaceIndexTranslation().apply(this.getNamespaceIndex()), this.getValue());
   }

   private int calcHashCode() {
      int var1 = 13 * this.namespaceIndex;
      if (this.value != null) {
         var1 += 3 * this.value.hashCode();
      }

      return var1;
   }

   private boolean calcIsNull() {
      if (this.value == null) {
         return true;
      } else if (this.namespaceIndex != 0) {
         return false;
      } else {
         switch (this.rU) {
            case Numeric:
               return ((UnsignedInteger)this.value).intValue() == 0;
            case String:
               return ((String)this.value).length() == 0;
            case Guid:
               return this.value.equals(NULL_GUID_VALUE);
            case Opaque:
               return this.value.equals(NULL_OPAQUE_VALUE);
            default:
               return false;
         }
      }
   }
}
