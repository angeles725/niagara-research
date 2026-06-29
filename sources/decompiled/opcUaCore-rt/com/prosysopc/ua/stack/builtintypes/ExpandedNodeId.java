package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.UaExpandedNodeId;
import com.prosysopc.ua.UaNamespace;
import com.prosysopc.ua.UaNamespaceTranslateable;
import com.prosysopc.ua.UaNamespaceTranslateable.Context;
import com.prosysopc.ua.stack.common.ServerTable;
import com.prosysopc.ua.stack.core.IdType;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.utils.CryptoUtil;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExpandedNodeId implements UaNamespaceTranslateable<ExpandedNodeId>, Comparable<ExpandedNodeId> {
   private static final Logger logger = LoggerFactory.getLogger(ExpandedNodeId.class);
   public static final ExpandedNodeId NULL_NUMERIC = new ExpandedNodeId(NodeId.NULL_NUMERIC);
   public static final ExpandedNodeId NULL_STRING = new ExpandedNodeId(NodeId.NULL_STRING);
   public static final ExpandedNodeId NULL_GUID = new ExpandedNodeId(NodeId.NULL_GUID);
   public static final ExpandedNodeId NULL_OPAQUE = new ExpandedNodeId(NodeId.NULL_OPAQUE);
   public static final ExpandedNodeId NULL = NULL_NUMERIC;
   @Deprecated
   public static final NodeId ID = Identifiers.ExpandedNodeId;
   public static final ExpandedNodeId[] EMPTY_ARRAY = new ExpandedNodeId[0];
   final IdType rU;
   final int namespaceIndex;
   final UnsignedInteger aN;
   final String namespaceUri;
   final Object value;

   public static ExpandedNodeId[] arrayFrom(UaExpandedNodeId[] var0, ServerTable var1) {
      if (var0 == null) {
         return null;
      } else if (var0.length == 0) {
         return EMPTY_ARRAY;
      } else {
         ExpandedNodeId[] var2 = new ExpandedNodeId[var0.length];

         for (int var3 = 0; var3 < var0.length; var3++) {
            var2[var3] = from(var0[var3], var1);
         }

         return var2;
      }
   }

   public static ExpandedNodeId from(UaExpandedNodeId var0, ServerTable var1) {
      return var0 == null ? null : var0.asExpandedNodeId(var1);
   }

   public static boolean isNull(ExpandedNodeId var0) {
      return var0 == null || var0.isNullNodeId();
   }

   public static ExpandedNodeId parseExpandedNodeId(String var0) {
      int var1 = 0;
      int var2 = 0;
      String var3 = null;
      String var4 = var0;
      if (var0.startsWith("svr=")) {
         int var5 = var0.indexOf(59);
         if (var5 < 0) {
            throwExpandedNodeIdIllegalArgumentException(var0);
         }

         String var6 = var0.substring(4, var5);
         var1 = Integer.parseInt(var6);
         var4 = var0.substring(var5 + 1);
      }

      if (var4.startsWith("nsu=")) {
         int var7 = var4.indexOf(59);
         if (var7 < 0) {
            throwExpandedNodeIdIllegalArgumentException(var0);
         }

         var3 = var4.substring(4, var7);
         var4 = var4.substring(var7 + 1);
      } else if (var4.startsWith("ns=")) {
         int var8 = var4.indexOf(59);
         if (var8 < 0) {
            throwExpandedNodeIdIllegalArgumentException(var0);
         }

         String var10 = var4.substring(3, var8);
         var2 = Integer.parseInt(var10);
         var4 = var4.substring(var8 + 1);
      }

      if (!var4.startsWith("i=") && !var4.startsWith("s=") && !var4.startsWith("g=") && !var4.startsWith("b=")) {
         throwExpandedNodeIdIllegalArgumentException(var0);
      }

      NodeId var9 = NodeId.parseNodeId(var4);
      return var3 != null
         ? new ExpandedNodeId(UnsignedInteger.valueOf(var1), decodeNamespaceUri(var3), var9.getValue())
         : new ExpandedNodeId(UnsignedInteger.valueOf(var1), var2, var9.getValue());
   }

   private static String decodeNamespaceUri(String var0) {
      return var0 == null ? null : var0.replace("%3B", ";").replace("%25", "%");
   }

   private static String encodeNamespaceUri(String var0) {
      return var0 == null ? null : var0.replace("%", "%25").replace(";", "%3B");
   }

   private static void throwExpandedNodeIdIllegalArgumentException(String var0) throws IllegalArgumentException {
      throw new IllegalArgumentException("String is not a valid ExpandedNodeId: " + var0);
   }

   public ExpandedNodeId(NodeId var1) {
      this(null, var1.getNamespaceIndex(), var1.getValue());
   }

   public ExpandedNodeId(String var1, Object var2) {
      this(UnsignedInteger.ZERO, var1, var2);
   }

   public ExpandedNodeId(UnsignedInteger var1, int var2, Object var3) {
      if (var2 >= 0 && var2 <= 65535) {
         this.aN = var1 == null ? UnsignedInteger.ZERO : var1;
         if (var3 instanceof Integer) {
            var3 = UnsignedInteger.getFromBits((Integer)var3);
         }

         if (var3 instanceof byte[]) {
            var3 = ByteString.valueOf((byte[])var3);
         }

         this.value = var3;
         this.namespaceIndex = var2;
         this.namespaceUri = null;
         if (var3 == null) {
            this.rU = IdType.String;
         } else if (var3 instanceof UnsignedInteger) {
            this.rU = IdType.Numeric;
         } else if (var3 instanceof String) {
            this.rU = IdType.String;
         } else if (var3 instanceof UUID) {
            this.rU = IdType.Guid;
         } else {
            if (!(var3 instanceof ByteString)) {
               throw new IllegalArgumentException("value cannot be " + var3.getClass().getName());
            }

            this.rU = IdType.Opaque;
         }
      } else {
         throw new IllegalArgumentException("namespaceIndex out of bounds");
      }
   }

   public ExpandedNodeId(UnsignedInteger var1, NodeId var2) {
      this(var1, var2.getNamespaceIndex(), var2.getValue());
   }

   public ExpandedNodeId(UnsignedInteger var1, String var2, Object var3) {
      if (var2 == null) {
         throw new NullPointerException("namespaceUri; value=" + var3);
      } else if (var2.isEmpty()) {
         throw new IllegalArgumentException("namespaceUri not defined");
      } else {
         this.aN = var1 == null ? UnsignedInteger.ZERO : var1;
         if (var3 instanceof Integer) {
            var3 = UnsignedInteger.valueOf(((Integer)var3).intValue());
         }

         if (var3 instanceof byte[]) {
            var3 = ByteString.valueOf((byte[])var3);
         }

         this.value = var3;
         this.namespaceUri = var2;
         this.namespaceIndex = 0;
         if (var3 == null) {
            this.rU = IdType.String;
         } else if (var3 instanceof UnsignedInteger) {
            this.rU = IdType.Numeric;
         } else if (var3 instanceof String) {
            this.rU = IdType.String;
         } else if (var3 instanceof UUID) {
            this.rU = IdType.Guid;
         } else {
            if (!(var3 instanceof ByteString)) {
               throw new IllegalArgumentException("value cannot be " + var3.getClass().getName());
            }

            this.rU = IdType.Opaque;
         }
      }
   }

   public int compareTo(ExpandedNodeId var1) {
      int var2;
      if (this.namespaceUri != null && var1.namespaceUri != null) {
         var2 = this.namespaceUri.compareTo(var1.namespaceUri);
      } else {
         var2 = this.namespaceIndex - var1.namespaceIndex;
      }

      if (var2 == 0) {
         var2 = this.rU.getValue() - var1.rU.getValue();
      }

      if (var2 == 0) {
         switch (this.rU) {
            case Numeric:
               var2 = ((UnsignedInteger)this.value).compareTo((Number)((UnsignedInteger)var1.value));
               break;
            case String:
               var2 = ((String)this.value).compareTo((String)var1.value);
               break;
            case Guid:
               var2 = ((UUID)this.value).compareTo((UUID)var1.value);
               break;
            case Opaque:
               var2 = ((ByteString)this.value).compareTo((ByteString)var1.value);
         }
      }

      return var2;
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 == null) {
         return this.equals(NULL);
      } else if (var1 instanceof NodeId) {
         if ((this.namespaceUri == null || this.namespaceUri == "http://opcfoundation.org/UA/") && this.isLocal()) {
            NodeId var3 = (NodeId)var1;
            if (var3.namespaceIndex != this.namespaceIndex || var3.rU != this.rU) {
               return false;
            } else {
               return this.value == var3.value ? true : var3.value.equals(this.value);
            }
         } else {
            return false;
         }
      } else if (!(var1 instanceof ExpandedNodeId)) {
         return false;
      } else {
         ExpandedNodeId var2 = (ExpandedNodeId)var1;
         if (this.namespaceUri != null) {
            if (var2.namespaceUri == null || !var2.namespaceUri.equals(this.namespaceUri)) {
               return false;
            }
         } else {
            if (var2.namespaceUri != null) {
               return false;
            }

            if (var2.namespaceIndex != this.namespaceIndex) {
               return false;
            }
         }

         if (!this.isLocal()) {
            if (var2.isLocal() || !var2.aN.equals(this.aN)) {
               return false;
            }
         } else if (!var2.isLocal()) {
            return false;
         }

         if (var2.rU != this.rU) {
            return false;
         } else if (this.value == var2.value) {
            return true;
         } else {
            return var2.value != null ? var2.value.equals(this.value) : this.value == null;
         }
      }
   }

   public IdType getIdType() {
      return this.rU;
   }

   public int getNamespaceIndex() {
      return this.namespaceIndex;
   }

   public String getNamespaceUri() {
      return this.namespaceUri;
   }

   public UnsignedInteger getServerIndex() {
      return this.aN;
   }

   public Object getValue() {
      return this.value;
   }

   @Override
   public int hashCode() {
      int var1 = 0;
      if (this.value != null) {
         var1 += 3 * this.value.hashCode();
      }

      if (this.aN != null) {
         var1 += this.aN.hashCode() * 17;
      }

      return var1;
   }

   public boolean isAbsolute() {
      return this.namespaceUri != null && !this.namespaceUri.isEmpty() || !this.isLocal();
   }

   public boolean isLocal() {
      return this.aN == null || this.aN.getValue() == 0L;
   }

   public boolean isNullNodeId() {
      int var1;
      if (this.namespaceUri != null && !this.namespaceUri.isEmpty()) {
         if (!"http://opcfoundation.org/UA/".equals(this.namespaceUri)) {
            return false;
         }

         var1 = 0;
      } else {
         var1 = this.namespaceIndex;
      }

      return NodeId.get(this.rU, var1, this.value).isNullNodeId();
   }

   @Override
   public String toString() {
      String var1 = !this.isLocal() ? "svr=" + this.aN + ";" : "";
      String var2 = this.namespaceUri != null
         ? "nsu=" + encodeNamespaceUri(this.namespaceUri) + ";"
         : (this.namespaceIndex > 0 ? "ns=" + this.namespaceIndex + ";" : "");
      if (this.rU == IdType.Numeric) {
         return var1 + var2 + "i=" + this.value;
      } else if (this.rU == IdType.String) {
         return var1 + var2 + "s=" + this.value;
      } else if (this.rU == IdType.Guid) {
         return var1 + var2 + "g=" + this.value;
      } else if (this.rU == IdType.Opaque) {
         return this.value == null ? var1 + var2 + "b=null" : var1 + var2 + "b=" + new String(CryptoUtil.base64Encode(((ByteString)this.value).getValue()));
      } else {
         return "error";
      }
   }

   public ExpandedNodeId withTranslatedNamespaces(Context var1) {
      UnsignedInteger var2 = (UnsignedInteger)var1.getServerIndexTranslation().apply(this.getServerIndex());
      return this.getNamespaceUri() != null
         ? new ExpandedNodeId(var2, ((UaNamespace)var1.getNamespaceTranslation().apply(UaNamespace.from(this.getNamespaceUri()))).getURI(), this.getValue())
         : new ExpandedNodeId(var2, (Integer)var1.getNamespaceIndexTranslation().apply(this.getNamespaceIndex()), this.getValue());
   }
}
