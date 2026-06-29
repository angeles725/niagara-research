package com.tridium.lonworks.resource;

import java.io.IOException;

public class Type {
   public int index;
   public int recSize;
   public boolean obsolete = false;
   public int flags = 0;
   public int typeSize;
   public TypeNode node;
   public String prefix;
   public int scope;

   public Type(ResFileInputStream in, String prefix, int scope) throws IOException {
      this.prefix = prefix;
      this.scope = scope;
      this.index = in.readUnsigned16();
      this.recSize = in.readUnsigned16();
      if (in.majorFmtVer > 2) {
         this.obsolete = in.readBool();
      }

      if (in.majorFmtVer >= 6) {
         this.flags = in.readUnsigned16();
      }

      this.typeSize = in.readUnsigned16();
      this.node = TypeNode.makeNode(in);
   }

   public void toString(StringBuffer sb) {
      sb.append("index     = ").append(this.index).append("\n");
      sb.append(" recSize   = ").append(this.recSize).append("\n");
      sb.append(" typeSize  = ").append(this.typeSize).append("\n");
      if (this.obsolete) {
         sb.append(" obsolete  = ").append(this.obsolete).append("\n");
      }

      if (this.flags != 0) {
         sb.append(" flags  = ").append(Integer.toString(this.flags, 16)).append("\n");
      }

      sb.append(" prefix    = ").append(this.prefix).append("\n");
      this.node.toString(sb);
   }

   public boolean isSnvt() {
      return this.prefix.equals("SNVT_");
   }

   public boolean isScpt() {
      return this.prefix.equals("SCPT_");
   }
}
