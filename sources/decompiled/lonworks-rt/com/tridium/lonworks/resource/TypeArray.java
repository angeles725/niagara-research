package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeArray extends TypeNode {
   public int numElements;
   public TypeNode element;

   public TypeArray(ResFileInputStream in) throws IOException {
      this.numElements = in.readUnsigned16();
      this.element = makeNode(in, true);
   }

   @Override
   public void toString(StringBuffer sb) {
      sb.append("Array").append("\n");
      sb.append("numElements  = ").append(this.numElements).append("\n");
      this.element.toString(sb);
   }
}
