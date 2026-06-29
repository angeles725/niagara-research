package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeReference extends TypeNode {
   public int scope;
   public int index;
   public int typeSize;

   public TypeReference(ResFileInputStream in) throws IOException {
      this.scope = in.readUnsigned8();
      this.index = in.readUnsigned16();
      this.typeSize = in.readUnsigned8();
   }

   @Override
   public void toString(StringBuffer sb) {
      sb.append("Reference").append("\n");
      super.toString(sb);
      sb.append("  scope  = ").append(this.scope).append(',').append(this.index).append("\n");
      sb.append("  typeSize  = ").append(this.typeSize).append("\n");
   }
}
