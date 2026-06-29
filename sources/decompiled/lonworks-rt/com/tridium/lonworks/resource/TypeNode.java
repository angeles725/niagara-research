package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeNode {
   public static final int NVT_TYPE_UNKNOWN = 0;
   public static final int NVT_TYPE_SIGNED_CHAR = 1;
   public static final int NVT_TYPE_UNSIGNED_CHAR = 2;
   public static final int NVT_TYPE_SIGNED_SHORT = 3;
   public static final int NVT_TYPE_UNSIGNED_SHORT = 4;
   public static final int NVT_TYPE_SIGNED_LONG = 5;
   public static final int NVT_TYPE_UNSIGNED_LONG = 6;
   public static final int NVT_TYPE_ENUM = 7;
   public static final int NVT_TYPE_ARRAY = 8;
   public static final int NVT_TYPE_STRUCT = 9;
   public static final int NVT_TYPE_UNION = 10;
   public static final int NVT_TYPE_BITF = 11;
   public static final int NVT_TYPE_FLOAT = 12;
   public static final int NVT_TYPE_SIGNED_QUAD = 13;
   public static final int NVT_TYPE_REFERENCE = 14;
   public static final int NVT_TYPE_UNSIGNED_QUAD = 15;
   public static final int NVT_TYPE_DOUBLE_FLOAT = 16;
   public static final int NVT_TYPE_SIGNED_INT64 = 17;
   public static final int NVT_TYPE_UNSIGNED_INT64 = 18;
   protected static final int BITF_SIGNED_MASK = 128;
   protected static final int BITF_OFFSET_MASK = 112;
   protected static final int BITF_OFFSET_SHIFT = 4;
   protected static final int BITF_SIZE_MASK = 15;
   int resNmScope;
   int resCmtScope;
   int resUntScope;
   int resNmIndex;
   int resCmtIndex;
   int resUntIndex;
   public String name = "";
   public int nodeType;

   public static TypeNode makeNode(ResFileInputStream in) throws IOException {
      return makeNode(in, false);
   }

   public static TypeNode makeNode(ResFileInputStream in, boolean arrayFlag) throws IOException {
      String name = "";
      if (!arrayFlag) {
         name = in.readLString();
      }

      int resNmScope = in.readUnsigned8();
      int resNmIndex = in.readUnsigned24();
      int resCmtScope = in.readUnsigned8();
      int resCmtIndex = in.readUnsigned24();
      int resUntScope = in.readUnsigned8();
      int resUntIndex = in.readUnsigned24();
      int nodeType = in.readUnsigned8();
      TypeNode node = null;
      switch (nodeType) {
         case 1:
            node = new TypeScalar(in, nodeType);
            break;
         case 2:
            node = new TypeScalar(in, nodeType);
            break;
         case 3:
            node = new TypeScalar(in, nodeType);
            break;
         case 4:
            node = new TypeScalar(in, nodeType);
            break;
         case 5:
            node = new TypeScalar(in, nodeType);
            break;
         case 6:
            node = new TypeScalar(in, nodeType);
            break;
         case 7:
            node = new TypeEnum(in);
            break;
         case 8:
            node = new TypeArray(in);
            break;
         case 9:
            node = new TypeStruct(in);
            break;
         case 10:
            node = new TypeStruct(in);
            break;
         case 11:
            node = new TypeBitfield(in);
            break;
         case 12:
            node = new TypeFloat(in, nodeType);
            break;
         case 13:
            node = new TypeScalar(in, nodeType);
            break;
         case 14:
            node = new TypeReference(in);
            break;
         case 15:
            node = new TypeScalar(in, nodeType);
            break;
         case 16:
            node = new TypeFloat(in, nodeType);
            break;
         case 17:
            node = new TypeScalar64(in, nodeType);
            break;
         case 18:
            node = new TypeScalar64(in, nodeType);
            break;
         default:
            throw new RuntimeException("Invalid NVT_TYPE : " + nodeType);
      }

      node.resNmScope = resNmScope;
      node.resCmtScope = resCmtScope;
      node.resUntScope = resUntScope;
      node.resNmIndex = resNmIndex;
      node.resCmtIndex = resCmtIndex;
      node.resUntIndex = resUntIndex;
      node.name = name;
      node.nodeType = nodeType;
      return node;
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      this.toString(sb);
      return sb.toString();
   }

   public void toString(StringBuffer sb) {
      sb.append("  name          = ").append(this.name).append("\n");
      sb.append("  lang   Name = ")
         .append(this.resNmScope)
         .append(',')
         .append(this.resNmIndex)
         .append("  Comment = ")
         .append(this.resCmtScope)
         .append(',')
         .append(this.resCmtIndex)
         .append("  Unit = ")
         .append(this.resUntScope)
         .append(',')
         .append(this.resUntIndex)
         .append("\n");
   }
}
