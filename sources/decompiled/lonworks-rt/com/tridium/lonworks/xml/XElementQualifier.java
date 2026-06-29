package com.tridium.lonworks.xml;

import java.util.Objects;
import java.util.StringTokenizer;
import javax.baja.lonworks.londata.BLonElementQualifiers;

public class XElementQualifier implements Cloneable {
   String enumDef = null;
   String defaultValue = null;
   String engUnit = null;
   String tag0 = null;
   String tag1 = null;
   public String name;
   public String branch = null;
   private String elemtype = "na";
   private float resolution = 1.0F;
   private float offset = 0.0F;
   private boolean hasMinimum = false;
   private Number minimum = null;
   private boolean hasMaximum = false;
   private Number maximum = null;
   private boolean hasOffset = false;
   private int byteOffset = -1;
   private int bitOffset = -1;
   private boolean hasInvalid = false;
   private Number invalidValue = 0L;
   private int length = 0;
   private String snvtType = "";
   private String typeDef = "";
   private static final float DEFAULT_RESOLUTION = 1.0F;
   private static final float DEFAULT_OFFSET = 0.0F;
   public static final int C8 = 0;
   public static final int S8 = 1;
   public static final int U8 = 2;
   public static final int S16 = 3;
   public static final int U16 = 4;
   public static final int S32 = 5;
   public static final int B8 = 6;
   public static final int E8 = 7;
   public static final int F32 = 8;
   public static final int EB = 9;
   public static final int BB = 10;
   public static final int UB = 11;
   public static final int SB = 12;
   public static final int ST = 13;
   public static final int NA = 14;
   public static final int REF = 15;
   public static final int F64 = 16;
   public static final int U32 = 17;
   public static final int S64 = 18;
   public static final int U64 = 19;
   private static final String[] QualTypes = new String[]{
      "c8", "s8", "u8", "s16", "u16", "s32", "b8", "e8", "f32", "eb", "bb", "ub", "sb", "st", "na", "ref", "f64", "u32", "s64", "u64"
   };

   public XElementQualifier() {
   }

   public XElementQualifier(String n, String q) {
      this.name = n;
      this.decodeFromString(q);
   }

   public XElementQualifier(String n, BLonElementQualifiers e) {
      this.name = n;
      this.elemtype = e.getElemtype().getTag();
      this.hasMinimum = e.hasMinimum();
      this.minimum = e.getMinimumN();
      this.hasMaximum = e.hasMaximum();
      this.maximum = e.getMaximumN();
      this.resolution = e.getResolution();
      this.offset = e.getOffset();
      this.hasOffset = e.hasOffset();
      this.byteOffset = e.getByteOffset();
      this.bitOffset = e.getBitOffset();
      this.hasInvalid = e.hasInvalidValue();
      this.invalidValue = e.getInvalidValueN();
      this.length = e.getLength();
   }

   public XElementQualifier copy() {
      try {
         return (XElementQualifier)this.clone();
      } catch (Exception var2) {
         return null;
      }
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public String getElementType() {
      return this.elemtype;
   }

   public void setElementType(String elemtype) {
      this.elemtype = elemtype;
   }

   public float getResolution() {
      return this.resolution;
   }

   public void setResolution(float resolution) {
      this.resolution = resolution;
   }

   public float getOffset() {
      return this.offset;
   }

   public void setOffset(float offset) {
      this.offset = offset;
   }

   public boolean hasByteOffset() {
      return this.hasOffset;
   }

   public int getByteOffset() {
      return this.byteOffset;
   }

   public void setByteOffset(int byteOffset) {
      this.byteOffset = byteOffset;
      this.hasOffset = true;
   }

   public int getBitOffset() {
      return this.bitOffset;
   }

   public void setBitOffset(int bitOffset) {
      this.bitOffset = bitOffset;
      this.hasOffset = true;
   }

   public int getLength() {
      return this.length;
   }

   public void setLength(int length) {
      this.length = length;
   }

   public int getSize() {
      return this.length;
   }

   public void setSize(int length) {
      this.length = length;
   }

   public String getEnumDef() {
      return this.enumDef;
   }

   public void setEnumDef(String ed) {
      this.enumDef = ed;
   }

   public String getDefault() {
      return this.defaultValue;
   }

   public void setDefault(String d) {
      this.defaultValue = d;
   }

   public String getEngUnit() {
      return this.engUnit;
   }

   public void setEngUnit(String eu) {
      this.engUnit = eu;
   }

   public Number getMinimum() {
      return this.minimum;
   }

   public void setMinimum(Number min) {
      this.minimum = min;
      this.hasMinimum = true;
   }

   public boolean hasMinimum() {
      return this.hasMinimum;
   }

   public Number getMaximum() {
      return this.maximum;
   }

   public void setMaximum(Number max) {
      this.maximum = max;
      this.hasMaximum = true;
   }

   public boolean hasMaximum() {
      return this.hasMaximum;
   }

   public Number getInvalid() {
      return this.invalidValue;
   }

   public void setInvalid(Number invld) {
      this.invalidValue = invld;
      this.hasInvalid = true;
   }

   public boolean hasInvalid() {
      return this.hasInvalid;
   }

   public String getTag0() {
      return this.tag0;
   }

   public void setTag0(String tag0) {
      this.tag0 = tag0;
   }

   public String getTag1() {
      return this.tag1;
   }

   public void setTag1(String tag1) {
      this.tag1 = tag1;
   }

   public String getSnvtType() {
      return this.snvtType;
   }

   public void setSnvtType(String snvtType) {
      this.snvtType = snvtType;
   }

   public boolean isRef() {
      return this.elemtype.equals(QualTypes[15]);
   }

   public String getTypeDef() {
      return this.typeDef;
   }

   public void setTypeDef(String type) {
      this.typeDef = type;
   }

   public String getUnionBranch() {
      return this.branch;
   }

   public void setUnionBranch(String u) {
      this.branch = u;
   }

   public int accumulateByteCount(int currCnt) {
      int len;
      switch (qualTypeFromString(this.getElementType())) {
         case 0:
         case 1:
         case 2:
         case 6:
         case 7:
         case 9:
         case 10:
         case 11:
         case 12:
            len = 1;
            break;
         case 3:
         case 4:
            len = 2;
            break;
         case 5:
         case 8:
         case 17:
            len = 4;
            break;
         case 13:
         case 14:
            len = this.getLength();
            break;
         case 15:
         default:
            throw new RuntimeException("Attempt to accumlate Element type " + this.getElementType());
         case 16:
         case 18:
         case 19:
            len = 8;
      }

      if (this.hasOffset) {
         int newCount = len + this.getByteOffset();
         return newCount > currCnt ? newCount : currCnt;
      } else {
         return currCnt + len;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof XElementQualifier)) {
         return false;
      } else if (this == obj) {
         return true;
      } else {
         XElementQualifier eq = (XElementQualifier)obj;
         return this.elemtype.equals(eq.elemtype)
            && this.resolution == eq.resolution
            && this.offset == eq.offset
            && this.hasMinimum == eq.hasMinimum
            && (!this.hasMinimum || Objects.equals(this.minimum, eq.minimum))
            && this.hasMaximum == eq.hasMaximum
            && (!this.hasMaximum || Objects.equals(this.maximum, eq.maximum))
            && this.hasInvalid == eq.hasInvalid
            && (!this.hasInvalid || Objects.equals(this.invalidValue, eq.invalidValue))
            && this.length == eq.length;
      }
   }

   @Override
   public int hashCode() {
      return this.encodeToString().hashCode();
   }

   public String encodeToString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.elemtype);
      switch (qualTypeFromString(this.elemtype)) {
         case 0:
         case 7:
            if (this.hasOffset) {
               sb.append(" byt=").append(this.byteOffset);
            }

            this.addResOffset(sb);
            break;
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 8:
         case 16:
         case 17:
            if (this.hasOffset) {
               sb.append(" byt=").append(this.byteOffset);
            }

            this.addResOffset(sb);
            this.addMinMax(sb);
            break;
         case 9:
         case 10:
         case 11:
         case 12:
            sb.append(" byt=").append(this.byteOffset);
            sb.append(" bit=").append(this.bitOffset);
            sb.append(" len=").append(this.length);
            this.addResOffset(sb);
            this.addMinMax(sb);
            break;
         case 13:
         case 14:
            sb.append(" len=").append(this.length);
            break;
         case 15:
            sb.append(" type=").append(this.typeDef);
            if (this.hasOffset) {
               sb.append(" byt=").append(this.byteOffset);
            }
      }

      return sb.toString();
   }

   private void addMinMax(StringBuilder sb) {
      if (this.hasMinimum) {
         sb.append(" min=").append(this.minimum.doubleValue());
      }

      if (this.hasMaximum) {
         sb.append(" max=").append(this.maximum.doubleValue());
      }

      if (this.hasInvalid) {
         sb.append(" invld=").append(this.invalidValue.longValue());
      }
   }

   private void addResOffset(StringBuilder sb) {
      if (this.resolution != 1.0F) {
         sb.append(" res=").append(this.resolution);
      }

      if (this.offset != 0.0F) {
         sb.append(" off=").append(this.offset);
      }
   }

   public void decodeFromString(String s) {
      StringTokenizer st = new StringTokenizer(s, " ");
      this.elemtype = st.nextToken();

      while (st.hasMoreTokens()) {
         String tok = st.nextToken();
         int equalPos = tok.indexOf("=");
         if (equalPos >= 0) {
            String qual = tok.substring(0, equalPos);
            String val = tok.substring(equalPos + 1);
            if (qual.equals("res")) {
               this.resolution = Float.valueOf(val);
            } else if (qual.equals("off")) {
               this.offset = Float.valueOf(val);
            } else if (qual.equals("min")) {
               try {
                  this.minimum = this.readNumber(val);
                  this.hasMinimum = true;
               } catch (Exception var9) {
               }
            } else if (qual.equals("max")) {
               try {
                  this.maximum = this.readNumber(val);
                  this.hasMaximum = true;
               } catch (Exception var8) {
               }
            } else if (qual.equals("byt")) {
               this.byteOffset = Integer.parseInt(val);
               this.hasOffset = true;
            } else if (qual.equals("bit")) {
               this.bitOffset = Integer.parseInt(val);
               this.hasOffset = true;
            } else if (qual.equals("len")) {
               this.length = Integer.parseInt(val);
            } else if (qual.equals("invld")) {
               this.invalidValue = this.readNumber(val);
               this.hasInvalid = true;
            } else if (qual.equals("type")) {
               this.typeDef = val;
            } else if (qual.equals("typ")) {
               this.snvtType = val;
            }
         }
      }
   }

   private Number readNumber(String v) {
      return (Number)(v.indexOf(46) > 0 ? Double.valueOf(v) : Long.valueOf(v));
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.name).append(" ");
      if (this.enumDef != null) {
         sb.append(" enumDef=").append(this.enumDef);
      }

      if (this.engUnit != null) {
         sb.append(" engUnit=").append(this.engUnit);
      }

      if (this.defaultValue != null) {
         sb.append(" defaultValue=").append(this.defaultValue);
      }

      if (this.tag0 != null) {
         sb.append(" tag0=").append(this.tag0);
      }

      if (this.tag1 != null) {
         sb.append(" tag1=").append(this.tag1);
      }

      if (this.branch != null) {
         sb.append(" branch=").append(this.branch);
      }

      return sb.toString();
   }

   public static String qualTypeToString(int ndx) {
      return XLonDataUtil.getString(QualTypes, ndx, QualTypes[14]);
   }

   public static int qualTypeFromString(String type) {
      return XLonDataUtil.fromString(QualTypes, type, 0);
   }
}
