package com.prosysopc.ua.stack.encoding.binary;

import com.prosysopc.ua.stack.core.IdType;

public enum NodeIdEncoding {
   TwoByte((byte)0, IdType.Numeric),
   FourByte((byte)1, IdType.Numeric),
   Numeric((byte)2, IdType.Numeric),
   String((byte)3, IdType.String),
   Guid((byte)4, IdType.Guid),
   ByteString((byte)5, IdType.String);

   private final byte tv;
   private final IdType tw;

   public static NodeIdEncoding getNodeIdEncoding(int var0) {
      if (var0 == TwoByte.getBits()) {
         return TwoByte;
      } else if (var0 == FourByte.getBits()) {
         return FourByte;
      } else if (var0 == Numeric.getBits()) {
         return Numeric;
      } else if (var0 == String.getBits()) {
         return String;
      } else if (var0 == Guid.getBits()) {
         return Guid;
      } else {
         return var0 == ByteString.getBits() ? ByteString : null;
      }
   }

   private NodeIdEncoding(byte var3, IdType var4) {
      this.tv = var3;
      this.tw = var4;
   }

   public byte getBits() {
      return this.tv;
   }

   public IdType toIdentifierType() {
      return this.tw;
   }
}
