package javax.baja.lonworks.londata;

import java.util.HashMap;
import javax.baja.data.BIDataValue;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.units.BUnit;

public class LonFacetsUtil {
   public static final String TYPE = "type";
   public static final String MIN = "min";
   public static final String MAX = "max";
   public static final String RES = "res";
   public static final String OFF = "off";
   public static final String BYT = "byt";
   public static final String BIT = "bit";
   public static final String LEN = "len";
   public static final String INVLD = "invld";
   public static final String UNITS = "units";

   public static BLonElementQualifiers getElementQualifiers(BLonData londata, Property prop) throws LonException {
      BLonElementQualifiers e = getQualifiers(prop.getFacets());
      return e.hasOffset()
         ? e
         : BLonElementQualifiers.make(
            e.getElemtype(),
            e.hasMinimum(),
            e.getMinimum(),
            e.hasMaximum(),
            e.getMaximum(),
            e.getResolution(),
            e.getOffset(),
            true,
            getByteOffset(londata, prop),
            0,
            e.hasInvalidValue(),
            e.getInvalidValue(),
            e.getLength()
         );
   }

   private static int getByteOffset(BLonData londata, Property prop) {
      LonFacetsUtil.EqSearch eqs = new LonFacetsUtil.EqSearch();
      eqs.sLd = londata;
      eqs.prop = prop;
      BLonData ld = londata;

      while (ld.getParent() instanceof BLonData) {
         ld = (BLonData)ld.getParent();
      }

      getByteOffset(eqs, ld);
      return eqs.offset;
   }

   private static void getByteOffset(LonFacetsUtil.EqSearch eqs, BLonData ld) {
      SlotCursor<Property> c = ld.getProperties();

      while (c.nextObject()) {
         BObject obj = c.get();
         if (BLonData.class.isInstance(obj)) {
            getByteOffset(eqs, (BLonData)obj);
            if (eqs.found) {
               return;
            }
         } else if (BLonPrimitive.class.isInstance(obj)) {
            Property prop = c.property();
            if (prop == eqs.prop && ld == eqs.sLd) {
               eqs.found = true;
               return;
            }

            BLonElementQualifiers e = getQualifiers(prop.getFacets());
            if (e.hasOffset()) {
               eqs.offset = e.getByteOffset();
            }

            try {
               eqs.offset = eqs.offset + e.getDataByteLength();
            } catch (Exception var7) {
               System.out.println(var7);
            }
         }
      }
   }

   public static BLonElementQualifiers getQualifiers(BFacets f) {
      Object pkl = f.getPickle();
      if (pkl != null && pkl instanceof BLonElementQualifiers) {
         return (BLonElementQualifiers)pkl;
      } else {
         BLonElementQualifiers elemQual = makeLonQualifiers(f, -1);
         BFacets.makePickle(f, elemQual);
         return elemQual;
      }
   }

   public static BFacets makeFacets(BLonElementQualifiers elemQual, BUnit unit) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      if (unit != null) {
         map.put("units", unit);
      }

      BLonElementType elemtype = elemQual.getElemtype();
      map.put("type", BString.make(elemtype.getTag()));
      if (elemQual.getByteOffset() > 0) {
         map.put("byt", BInteger.make(elemQual.getByteOffset()));
      }

      switch (elemtype.getOrdinal()) {
         case 0:
         case 6:
         case 7:
            break;
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 8:
         case 16:
         case 17:
         case 18:
         case 19:
            float resolution = elemQual.getResolution();
            if (resolution != 0.0F && resolution != 1.0F) {
               map.put("res", BFloat.make(elemQual.getResolution()));
            }

            if (elemQual.getOffset() != 0.0F) {
               map.put("off", BFloat.make(elemQual.getOffset()));
            }

            addMinMax(map, elemQual);
            addPrec(map, elemQual);
            break;
         case 9:
         case 10:
         case 11:
            map.put("bit", BInteger.make(elemQual.getBitOffset()));
            map.put("len", BInteger.make(elemQual.getLength()));
            break;
         case 12:
         case 13:
            map.put("bit", BInteger.make(elemQual.getBitOffset()));
            map.put("len", BInteger.make(elemQual.getLength()));
            addMinMax(map, elemQual);
            map.put("precision", BInteger.make(0));
            break;
         case 14:
         case 15:
            map.put("len", BInteger.make(elemQual.getLength()));
            break;
         default:
            System.out.println("LonFacetsUtil.makeFacets did not handle elemtype=" + elemtype);
      }

      return BFacets.make(map);
   }

   private static void addMinMax(HashMap<String, BIDataValue> map, BLonElementQualifiers elemQual) {
      map.put("min", elemQual.hasMinimum() ? toBNumber(elemQual.getMinimumN()) : getMinimumMin(elemQual));
      map.put("max", elemQual.hasMaximum() ? toBNumber(elemQual.getMaximumN()) : getMaximumMax(elemQual));
      if (elemQual.hasInvalidValue()) {
         map.put("invld", toBNumber(elemQual.getInvalidValueN()));
      }
   }

   private static BNumber getMaximumMax(BLonElementQualifiers elemQual) {
      double max = 0.0;
      switch (elemQual.getElemtype().getOrdinal()) {
         case 1:
            max = 127.0;
            break;
         case 2:
            max = 255.0;
            break;
         case 3:
            max = 32767.0;
            break;
         case 4:
            max = 65535.0;
            break;
         case 5:
            max = 2.147483647E9;
            break;
         case 6:
         case 7:
         case 9:
         case 10:
         case 11:
         case 14:
         case 15:
         default:
            max = Double.POSITIVE_INFINITY;
            break;
         case 8:
            max = Double.POSITIVE_INFINITY;
            break;
         case 12:
            max = (1 << elemQual.getLength()) - 1;
            break;
         case 13:
            max = (1 << elemQual.getLength() - 1) - 1;
            break;
         case 16:
            max = 4.294967295E9;
            break;
         case 17:
            max = Double.POSITIVE_INFINITY;
            break;
         case 18:
            max = Math.pow(2.0, 63.0) - 1.0;
            break;
         case 19:
            max = 1.844674407370955E19;
      }

      if (max == elemQual.getInvalidValue()) {
         max--;
      }

      max = max * elemQual.getResolution() - elemQual.getOffset();
      return (BNumber)(max < Float.MAX_VALUE ? BFloat.make((float)max) : BDouble.make(max));
   }

   private static BNumber getMinimumMin(BLonElementQualifiers elemQual) {
      double min = 0.0;
      switch (elemQual.getElemtype().getOrdinal()) {
         case 1:
            min = -128.0;
            break;
         case 2:
         case 4:
         case 12:
         case 16:
         case 19:
            min = 0.0;
            break;
         case 3:
            min = -32768.0;
            break;
         case 5:
            min = -2.1474836E9F;
            break;
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 14:
         case 15:
         default:
            min = Double.NEGATIVE_INFINITY;
            break;
         case 13:
            min = -(1 << elemQual.getLength() - 1);
            break;
         case 17:
            min = Double.NEGATIVE_INFINITY;
            break;
         case 18:
            min = -9.223372E18F;
      }

      min = min * elemQual.getResolution() - elemQual.getOffset();
      return (BNumber)(min > Float.MIN_VALUE ? BFloat.make((float)min) : BDouble.make(min));
   }

   private static void addPrec(HashMap<String, BIDataValue> map, BLonElementQualifiers elemQual) {
      float res = elemQual.getResolution();
      int pre = elemQual.getElemtype().equals(BLonElementType.f32) ? 2 : 0;
      if (res < 1.0F) {
         pre = (int)Math.ceil(-(Math.log(res) / Math.log(10.0)));
      }

      map.put("precision", BInteger.make(pre));
   }

   private static BLonElementQualifiers makeLonQualifiers(BFacets f, int bOffset) {
      float resolution = 1.0F;
      float offset = 0.0F;
      boolean hasMinimum = false;
      Number minimum = 0.0F;
      boolean hasMaximum = false;
      Number maximum = 0.0F;
      boolean hasOffset = false;
      int bitOffset = 0;
      int byteOffset = 0;
      boolean hasInvalid = false;
      Number invalidValue = Float.NaN;
      int length = 0;
      BLonElementType elemtype = (BLonElementType)BLonElementType.na.getRange().get(((BString)f.getFacet("type")).getString());
      if (elemtype == null) {
         return BLonElementQualifiers.NONE;
      } else {
         BObject s;
         if ((s = f.getFacet("res")) != null) {
            resolution = ((BFloat)s).getFloat();
         }

         if ((s = f.getFacet("off")) != null) {
            offset = ((BFloat)s).getFloat();
         }

         if ((s = f.getFacet("min")) != null) {
            minimum = parseNumber(s);
            hasMinimum = true;
         }

         if ((s = f.getFacet("max")) != null) {
            maximum = parseNumber(s);
            hasMaximum = true;
         }

         if ((s = f.getFacet("byt")) != null) {
            byteOffset = ((BInteger)s).getInt();
            if (byteOffset > 0) {
               hasOffset = true;
            }
         }

         if ((s = f.getFacet("bit")) != null) {
            bitOffset = ((BInteger)s).getInt();
            hasOffset = true;
         }

         if ((s = f.getFacet("len")) != null) {
            length = ((BInteger)s).getInt();
         }

         if ((s = f.getFacet("invld")) != null) {
            invalidValue = parseNumber(s);
            hasInvalid = true;
         }

         if (!hasOffset && bOffset >= 0) {
            byteOffset = bOffset;
            hasOffset = true;
         }

         return BLonElementQualifiers.make(
            elemtype, hasMinimum, minimum, hasMaximum, maximum, resolution, offset, hasOffset, byteOffset, bitOffset, hasInvalid, invalidValue, length
         );
      }
   }

   private static Number parseNumber(BObject s) {
      if (s instanceof BLong) {
         return ((BLong)s).getLong();
      } else {
         return (Number)(s instanceof BDouble ? ((BDouble)s).getDouble() : ((BFloat)s).getFloat());
      }
   }

   private static BNumber toBNumber(Number n) {
      if (n instanceof Long) {
         return BLong.make((Long)n);
      } else {
         return (BNumber)(n instanceof Double ? BDouble.make((Double)n) : BFloat.make((Float)n));
      }
   }

   public static BFacets makeFacets(BLonElementType elemtype, BUnit unit) {
      return makeFacets(BLonElementQualifiers.make(elemtype, 0), unit);
   }

   public static BFacets makeFacets(BLonElementType elemtype, int len, BUnit unit) {
      return makeFacets(BLonElementQualifiers.make(elemtype, len), unit);
   }

   public static BFacets makeFacets(
      BLonElementType elemtype,
      boolean hasMinimum,
      float minimum,
      boolean hasMaximum,
      float maximum,
      float resolution,
      float offset,
      boolean hasOffsets,
      int byteOffset,
      int bitOffset,
      boolean hasInvalid,
      float invalidValue,
      int length,
      BUnit unit
   ) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      map.put("type", BString.make(elemtype.getTag()));
      if (unit != null) {
         map.put("units", unit);
      }

      if (hasOffsets) {
         map.put("byt", BInteger.make(byteOffset));
      }

      switch (elemtype.getOrdinal()) {
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 8:
         case 16:
         case 17:
            if (resolution != 0.0F && resolution != 1.0F) {
               map.put("res", BFloat.make(resolution));
            }

            if (offset != 0.0F) {
               map.put("off", BFloat.make(offset));
            }

            if (hasMinimum) {
               map.put("min", BFloat.make(minimum));
            }

            if (hasMaximum) {
               map.put("max", BFloat.make(maximum));
            }

            if (hasInvalid) {
               map.put("invld", BFloat.make(invalidValue));
            }
         case 6:
         case 7:
         default:
            break;
         case 12:
            if (!hasMinimum) {
               map.put("min", BFloat.make(0.0F));
            }
         case 9:
         case 10:
         case 11:
         case 13:
            map.put("bit", BInteger.make(bitOffset));
            map.put("len", BInteger.make(length));
            if (hasMinimum) {
               map.put("min", BFloat.make(minimum));
            }

            if (hasMaximum) {
               map.put("max", BFloat.make(maximum));
            }

            if (hasInvalid) {
               map.put("invld", BFloat.make(invalidValue));
            }
            break;
         case 14:
         case 15:
            map.put("len", BInteger.make(length));
      }

      return BFacets.make(map);
   }

   public static BFacets makeFacets(BLonElementType elemtype, float minimum, float maximum, float resolution, BUnit unit) {
      BLonElementQualifiers eq = BLonElementQualifiers.make(elemtype, true, minimum, true, maximum, resolution, 0.0F, false, 0, 0, false, 0.0F, 0);
      return makeFacets(eq, unit);
   }

   public static BFacets makeFacets(BLonElementType elemtype, float minimum, float maximum, float resolution, int byteOffset, BUnit unit) {
      BLonElementQualifiers eq = BLonElementQualifiers.make(elemtype, true, minimum, true, maximum, resolution, 0.0F, false, byteOffset, 0, false, 0.0F, 0);
      return makeFacets(eq, unit);
   }

   public static BFacets makeFacets(BLonElementType elemtype, float minimum, BUnit unit) {
      BLonElementQualifiers eq = BLonElementQualifiers.make(elemtype, true, minimum, false, 0.0F, 1.0F, 0.0F, false, 0, 0, false, 0.0F, 0);
      return makeFacets(eq, unit);
   }

   public static BFacets makeFacets(BLonElementType elemtype, int byteOffset) {
      BLonElementQualifiers eq = BLonElementQualifiers.make(elemtype, false, 0.0F, false, 0.0F, 1.0F, 0.0F, false, byteOffset, 0, false, 0.0F, 0);
      return makeFacets(eq, null);
   }

   public static BFacets makeFacets(BLonElementType elemtype, int byteOffset, int bitOffset, int len, BUnit unit) {
      BLonElementQualifiers eq = BLonElementQualifiers.make(elemtype, false, 0.0F, false, 0.0F, 1.0F, 0.0F, false, byteOffset, bitOffset, false, 0.0F, len);
      return makeFacets(eq, unit);
   }

   public static BFacets makeFacets(BLonElementType elemtype, float minimum, float maximum, float resolution, float invalidValue, BUnit unit) {
      BLonElementQualifiers eq = BLonElementQualifiers.make(elemtype, true, minimum, true, maximum, resolution, 0.0F, false, 0, 0, true, invalidValue, 0);
      return makeFacets(eq, unit);
   }

   public static BFacets makeFacets(
      BLonElementType elemtype, float minimum, float maximum, float resolution, float invalidValue, int byteOffset, int bitOffset, BUnit unit
   ) {
      BLonElementQualifiers eq = BLonElementQualifiers.make(
         elemtype, true, minimum, true, maximum, resolution, 0.0F, false, byteOffset, bitOffset, true, invalidValue, 0
      );
      return makeFacets(eq, unit);
   }

   private static class EqSearch {
      BLonData sLd;
      Property prop;
      int offset = 0;
      boolean found = false;

      private EqSearch() {
      }
   }
}
