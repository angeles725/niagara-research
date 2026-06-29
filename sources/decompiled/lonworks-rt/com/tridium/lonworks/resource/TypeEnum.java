package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeEnum extends TypeScalar {
   public int enumScope;
   public int enumIndex;

   public TypeEnum(ResFileInputStream in) throws IOException {
      this.nodeType = 7;
      this.enumScope = in.readUnsigned8();
      this.enumIndex = in.readUnsigned16();
      this.parseRangeScale(in);
   }

   @Override
   public void toString(StringBuffer sb) {
      sb.append("TypeEnum").append("\n");
      super.toString(sb);
      sb.append("  enumScope  = ").append(this.enumScope).append(',').append(this.enumIndex).append("\n");
   }
}
