package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeBitfield extends TypeScalar {
   public boolean bitfSigned;
   public int bitfOffset;
   public int bitfSize;

   public TypeBitfield(ResFileInputStream in) throws IOException {
      this.nodeType = 11;
      int bitfStuff = in.readUnsigned8();
      this.bitfSigned = (bitfStuff & 128) != 0;
      this.bitfOffset = (bitfStuff & 112) >> 4;
      this.bitfSize = bitfStuff & 15;
      this.parseRangeScale(in);
   }

   @Override
   protected long getMinPossible() {
      return this.bitfSigned ? -1 << this.bitfSize - 1 : 0L;
   }

   @Override
   protected long getMaxPossible() {
      return this.bitfSigned ? 1 << this.bitfSize - 1 : (1 << this.bitfSize) - 1;
   }

   @Override
   protected long readRange(ResFileInputStream in) throws IOException {
      return this.bitfSigned ? in.readSigned8() : in.readUnsigned8();
   }

   @Override
   public void toString(StringBuffer sb) {
      sb.append("TypeBitField").append("\n");
      super.toString(sb);
      sb.append("  bit - ");
      if (this.bitfSigned) {
         sb.append("signed  ");
      }

      sb.append(" Offset = ").append(this.bitfOffset);
      sb.append(" Size = ").append(this.bitfSize).append("\n");
   }
}
