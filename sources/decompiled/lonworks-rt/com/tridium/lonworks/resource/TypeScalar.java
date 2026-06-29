package com.tridium.lonworks.resource;

import java.io.IOException;

public class TypeScalar extends TypeNode {
   protected static final int LM_BIT = 128;
   protected static final int LN_BIT = 64;
   protected static final int LZ_BIT = 32;
   protected static final int UM_BIT = 16;
   protected static final int UZ_BIT = 8;
   protected static final int SA_BIT = 4;
   protected static final int SB_BIT = 2;
   protected static final int SC_BIT = 1;
   protected int rangeScaleControl;
   protected long minValid;
   protected long maxValid;
   public int scaleA = 1;
   public int scaleB = 0;
   public int scaleC = 0;
   public boolean invalidPresent = false;
   public long invalid;

   public TypeScalar() {
   }

   public TypeScalar(ResFileInputStream in, int nodeType) throws IOException {
      this.nodeType = nodeType;
      this.parseRangeScale(in);
   }

   public void parseRangeScale(ResFileInputStream in) throws IOException {
      this.rangeScaleControl = in.readUnsigned8();
      if ((this.rangeScaleControl & 128) != 0) {
         this.minValid = this.getMinPossible();
      } else if ((this.rangeScaleControl & 64) != 0) {
         this.minValid = -1L;
      } else if ((this.rangeScaleControl & 32) != 0) {
         this.minValid = 0L;
      } else {
         this.minValid = this.readRange(in);
      }

      if ((this.rangeScaleControl & 16) != 0) {
         this.maxValid = this.getMaxPossible();
      } else if ((this.rangeScaleControl & 8) != 0) {
         this.maxValid = 0L;
      } else {
         this.maxValid = this.readRange(in);
      }

      this.readScale(in);
      this.readInvalid(in);
   }

   protected long getMinPossible() {
      switch (this.nodeType) {
         case 1:
         case 3:
         case 7:
            return -128L;
         case 2:
         case 4:
            return 0L;
         case 5:
            return -32768L;
         case 6:
            return 0L;
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 14:
         default:
            throw new RuntimeException("Invalid NVT_TYPE " + this.nodeType);
         case 13:
            return -2147483648L;
         case 15:
            return 0L;
      }
   }

   protected long getMaxPossible() {
      switch (this.nodeType) {
         case 1:
         case 3:
         case 7:
            return 127L;
         case 2:
         case 4:
            return 255L;
         case 5:
            return 32767L;
         case 6:
            return 65535L;
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 14:
         default:
            throw new RuntimeException("Invalid NVT_TYPE " + this.nodeType);
         case 13:
            return 2147483647L;
         case 15:
            return 4294967295L;
      }
   }

   protected long readRange(ResFileInputStream in) throws IOException {
      switch (this.nodeType) {
         case 1:
         case 3:
         case 7:
            return in.readSigned8();
         case 2:
         case 4:
            return in.readUnsigned8();
         case 5:
            return in.readSigned16();
         case 6:
            return in.readUnsigned16();
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 14:
         default:
            throw new RuntimeException("Invalid NVT_TYPE " + this.nodeType);
         case 13:
            return in.readSigned32();
         case 15:
            return in.readUnsigned32();
      }
   }

   protected void readScale(ResFileInputStream in) throws IOException {
      if ((this.rangeScaleControl & 4) != 0) {
         this.scaleA = in.readSigned16();
      }

      if ((this.rangeScaleControl & 2) != 0) {
         this.scaleB = in.readSigned16();
      }

      if ((this.rangeScaleControl & 1) != 0) {
         this.scaleC = in.readSigned16();
      }
   }

   protected void readInvalid(ResFileInputStream in) throws IOException {
      if (in.majorFmtVer >= 3) {
         switch (this.nodeType) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 13:
               this.invalidPresent = in.readBool();
               if (this.invalidPresent) {
                  this.invalid = in.readSigned32();
               }
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 14:
            default:
               break;
            case 15:
               this.invalidPresent = in.readBool();
               if (this.invalidPresent) {
                  this.invalid = in.readUnsigned32();
               }
         }
      }
   }

   @Override
   public void toString(StringBuffer sb) {
      sb.append("Scalar").append("\n");
      super.toString(sb);
      sb.append("rangeScaleControl  = ").append(Integer.toString(this.rangeScaleControl, 16)).append("\n");
      if (this.hasMinimum() || this.hasMaximum()) {
         sb.append("  range  = ").append(this.getMinimum()).append(" - ").append(this.getMaximum()).append("\n");
      }

      if (this.hasInvalid()) {
         sb.append("  invalid  = ").append(this.getInvalid()).append("\n");
      }

      if ((this.rangeScaleControl & 7) != 0) {
         sb.append("  scale A=").append(this.scaleA).append(" B=").append(this.scaleB).append(" C=").append(this.scaleC).append("\n");
      } else {
         sb.append("default scale->1").append("\n");
      }
   }

   public boolean hasMinimum() {
      return (this.rangeScaleControl & 128) == 0;
   }

   public Number getMinimum() {
      return this.minValid;
   }

   public boolean hasMaximum() {
      return (this.rangeScaleControl & 16) == 0;
   }

   public Number getMaximum() {
      return this.maxValid;
   }

   public boolean hasInvalid() {
      return this.invalidPresent;
   }

   public Number getInvalid() {
      return this.invalid;
   }
}
