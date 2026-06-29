package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeStruct extends TypeNode {
   public int numFields;
   public TypeNode[] fields;

   public TypeStruct(ResFileInputStream in) throws IOException {
      this.numFields = in.readUnsigned16();
      this.fields = new TypeNode[this.numFields];

      for (int i = 0; i < this.numFields; i++) {
         this.fields[i] = makeNode(in);
      }
   }

   @Override
   public void toString(StringBuffer sb) {
      if (this.nodeType == 9) {
         sb.append("Struct").append("\n");
      } else {
         sb.append("Union").append("\n");
      }

      super.toString(sb);
      sb.append("numFields = ").append(this.numFields).append("\n");

      for (int i = 0; i < this.numFields; i++) {
         this.fields[i].toString(sb);
      }
   }
}
