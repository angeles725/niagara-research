package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeFloat extends TypeScalar {
   public double fMinValid;
   public double fMaxValid;

   public TypeFloat(ResFileInputStream in, int nodeType) throws IOException {
      this.nodeType = nodeType;
      this.rangeScaleControl = in.readUnsigned8();
      if ((this.rangeScaleControl & 128) != 0) {
         this.fMinValid = Double.NEGATIVE_INFINITY;
      } else if ((this.rangeScaleControl & 64) != 0) {
         this.fMinValid = -1.0;
      } else if ((this.rangeScaleControl & 32) != 0) {
         this.fMinValid = 0.0;
      } else if (nodeType == 16) {
         this.fMinValid = in.readDouble();
      } else {
         this.fMinValid = in.readFloat();
      }

      if ((this.rangeScaleControl & 16) != 0) {
         this.fMaxValid = Double.POSITIVE_INFINITY;
      } else if ((this.rangeScaleControl & 8) != 0) {
         this.fMaxValid = 0.0;
      } else if (nodeType == 16) {
         this.fMaxValid = in.readDouble();
      } else {
         this.fMaxValid = in.readFloat();
      }
   }

   @Override
   public void toString(StringBuffer sb) {
      sb.append("TypeFloat").append("\n");
      super.toString(sb);
   }

   @Override
   public Number getMinimum() {
      return this.fMinValid;
   }

   @Override
   public Number getMaximum() {
      return this.fMaxValid;
   }
}
