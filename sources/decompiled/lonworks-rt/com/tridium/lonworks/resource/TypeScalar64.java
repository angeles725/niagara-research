package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeScalar64 extends TypeScalar {
   public long lMinValid = 0L;
   public long lMaxValid = 0L;
   public long lInvalid = 0L;

   public TypeScalar64(ResFileInputStream in, int nodeType) throws IOException {
      this.nodeType = nodeType;
      this.rangeScaleControl = in.readUnsigned8();
      if ((this.rangeScaleControl & 128) != 0) {
         if (nodeType == 17) {
            this.lMinValid = Long.MIN_VALUE;
         } else {
            this.lMinValid = 0L;
         }
      } else if ((this.rangeScaleControl & 64) != 0) {
         this.lMinValid = -1L;
      } else if ((this.rangeScaleControl & 32) != 0) {
         this.lMinValid = 0L;
      } else {
         this.lMinValid = in.readSigned64();
      }

      if ((this.rangeScaleControl & 16) != 0) {
         if (nodeType == 17) {
            this.lMaxValid = Long.MAX_VALUE;
         } else {
            this.lMaxValid = -1L;
         }
      } else if ((this.rangeScaleControl & 8) != 0) {
         this.lMaxValid = 0L;
      } else {
         this.lMaxValid = in.readSigned64();
      }

      this.readScale(in);
      this.readInvalid(in);
      float res = (float)(this.scaleA * Math.pow(10.0, this.scaleB));
      if ((this.rangeScaleControl & 128) != 0) {
         this.lMinValid = (long)((float)this.lMinValid * res);
      }

      if ((this.rangeScaleControl & 16) != 0) {
         this.lMaxValid = (long)((float)this.lMaxValid * res);
      }
   }

   @Override
   protected void readInvalid(ResFileInputStream in) throws IOException {
      if (in.majorFmtVer >= 3) {
         switch (this.nodeType) {
            case 17:
            case 18:
               this.invalidPresent = in.readBool();
               if (this.invalidPresent) {
                  this.lInvalid = in.readSigned64();
               }
         }
      }
   }

   @Override
   public void toString(StringBuffer sb) {
      sb.append("TypeScalar64").append("\n");
      super.toString(sb);
   }

   @Override
   public Number getMinimum() {
      return this.lMinValid;
   }

   @Override
   public Number getMaximum() {
      return this.lMaxValid;
   }

   @Override
   public Number getInvalid() {
      return this.lInvalid;
   }
}
