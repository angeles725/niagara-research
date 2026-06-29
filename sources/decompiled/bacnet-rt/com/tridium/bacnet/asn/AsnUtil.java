package com.tridium.bacnet.asn;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetAny;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.datatypes.BBacnetTimeValue;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.IntHashMap;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

public class AsnUtil implements AsnConst {
   private static final int MAX_ASN_COUNT = 10;
   private static AsnOutputStream[] asnPool = new AsnOutputStream[10];
   private static int asnCnt = 0;
   static Lexicon lex = Lexicon.make("bacnet");
   private static final Logger logger = Logger.getLogger("bacnet.asn");
   private static IntHashMap asnNamesByType = new IntHashMap();
   private static HashMap<String, Integer> asnTypesByName = new HashMap<>();
   private static IntHashMap sizesBySpec = new IntHashMap();

   public static String getAsnTypeName(int asnType) {
      String name = (String)asnNamesByType.get(asnType);
      return name == null ? "???" : name;
   }

   public static String getAsnTypeName(BInteger asnType) {
      return asnType == null ? "" : getAsnTypeName(asnType.getInt());
   }

   public static int getAsnType(String asnTypeName) {
      Integer type = asnTypesByName.get(asnTypeName);
      return type == null ? -1 : type;
   }

   public static BBacnetNull fromAsnNull(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BBacnetNull var2;
      try {
         var2 = asnIn.readNull();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnNull() {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeNull();
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnNull(BBacnetNull n) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeNull();
      return releaseAsn(asnOut);
   }

   public static boolean fromAsnBoolean(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      boolean var3;
      try {
         int tag = asnIn.peekTag();
         if (tag == 1) {
            return asnIn.readBoolean();
         }

         if (tag != 9) {
            throw new AsnException("Invalid tag: " + tag);
         }

         var3 = asnIn.readEnumerated() != 0;
      } finally {
         asnIn.release();
      }

      return var3;
   }

   public static boolean fromOnlyAsnBoolean(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      boolean var3;
      try {
         int tag = asnIn.peekTag();
         if (tag != 1) {
            throw new AsnException("Invalid tag: " + tag);
         }

         var3 = asnIn.readBoolean();
      } finally {
         asnIn.release();
      }

      return var3;
   }

   public static boolean fromOnlyBinaryPv(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      boolean var2;
      try {
         var2 = fromOnlyBinaryPv(asnIn);
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static boolean fromOnlyBinaryPv(AsnInput asnIn) throws AsnException {
      int tag = asnIn.peekTag();
      if (tag == 9) {
         int val = asnIn.readEnumerated();
         if (val == 0) {
            return false;
         } else if (val == 1) {
            return true;
         } else {
            throw new OutOfRangeException("Invalid boolean value");
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   public static byte[] toAsnBoolean(boolean b) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeBoolean(b);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnBoolean(BBoolean b) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeBoolean(b.getBoolean());
      return releaseAsn(asnOut);
   }

   public static long fromAsnUnsignedInteger(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      long var2;
      try {
         var2 = asnIn.readUnsignedInteger();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnUnsigned(long i) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeUnsignedInteger(i);
      return releaseAsn(asnOut);
   }

   public static int fromAsnUnsignedInt(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      int var4;
      try {
         long ret = asnIn.readUnsignedInteger();
         if (ret > 2147483647L) {
            throw new OutOfRangeException("Unsigned int is too big:" + ret);
         }

         var4 = (int)ret;
      } finally {
         asnIn.release();
      }

      return var4;
   }

   public static byte[] toAsnUnsigned(BBacnetUnsigned u) {
      return toAsnUnsigned(u.getUnsigned());
   }

   public static byte[] toAsnUnsigned(BEnum e) {
      return toAsnUnsigned(e.getOrdinal());
   }

   public static BBacnetUnsigned fromAsnUnsigned(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BBacnetUnsigned var2;
      try {
         var2 = BBacnetUnsigned.make(asnIn.readUnsignedInteger());
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static int fromAsnSignedInteger(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      int var2;
      try {
         var2 = asnIn.readSignedInteger();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnInteger(int i) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeSignedInteger(i);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnInteger(BInteger i) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeSignedInteger(i.getInt());
      return releaseAsn(asnOut);
   }

   public static int fromAsnInteger(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      int var2;
      try {
         var2 = asnIn.readInteger();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static float fromAsnReal(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      float var2;
      try {
         var2 = asnIn.readReal();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnReal(double f) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeReal(f);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnReal(BFloat f) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeReal(f.getFloat());
      return releaseAsn(asnOut);
   }

   public static double fromAsnDouble(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      double var2;
      try {
         var2 = asnIn.readDouble();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnDouble(double d) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeDouble(d);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnDouble(BDouble d) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeDouble(d.getDouble());
      return releaseAsn(asnOut);
   }

   public static BBacnetOctetString fromAsnOctetString(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BBacnetOctetString var2;
      try {
         var2 = BBacnetOctetString.make(asnIn.readOctetString());
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnOctetString(byte[] b) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeOctetString(b);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnOctetString(BBacnetOctetString b) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeOctetString(b);
      return releaseAsn(asnOut);
   }

   public static String fromAsnCharacterString(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      String var2;
      try {
         var2 = asnIn.readCharacterString();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnCharacterString(String s) {
      return toAsnCharacterString(s, BBacnetNetwork.localDevice().getCharacterSet());
   }

   public static byte[] toAsnCharacterString(String s, BCharacterSetEncoding encoding) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeCharacterString(s, encoding);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnCharacterString(BString s) {
      return toAsnCharacterString(s.getString(), BBacnetNetwork.localDevice().getCharacterSet());
   }

   public static BCharacterSetEncoding getCharacterSetEncoding(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BCharacterSetEncoding var2;
      try {
         var2 = asnIn.peekEncoding();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static BBacnetBitString fromAsnBitString(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BBacnetBitString var2;
      try {
         var2 = asnIn.readBitString();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnBitString(boolean[] bits) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeBitString(bits);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnBitString(BBacnetBitString bs) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeBitString(bs.getBits());
      return releaseAsn(asnOut);
   }

   public static int fromAsnEnumerated(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      int var2;
      try {
         var2 = asnIn.readEnumerated();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnEnumerated(int value) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeEnumerated(value);
      return releaseAsn(asnOut);
   }

   public static byte[] toAsnEnumerated(boolean value) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeEnumerated(value ? 1 : 0);
      return releaseAsn(asnOut);
   }

   public static BEnum fromAsnEnumerated(BEnum discrete, byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BEnum var3;
      try {
         var3 = discrete.getRange().get(asnIn.readEnumerated());
      } finally {
         asnIn.release();
      }

      return var3;
   }

   public static byte[] toAsnEnumerated(BEnum discrete) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeEnumerated(discrete.getOrdinal());
      return releaseAsn(asnOut);
   }

   public static BBacnetDate fromAsnDate(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BBacnetDate var2;
      try {
         var2 = asnIn.readDate();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnDate(BBacnetDate date) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeDate(date);
      return releaseAsn(asnOut);
   }

   public static BBacnetTime fromAsnTime(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BBacnetTime var2;
      try {
         var2 = asnIn.readTime();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnTime(BBacnetTime time) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeTime(time);
      return releaseAsn(asnOut);
   }

   public static BBacnetObjectIdentifier fromAsnObjectId(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BBacnetObjectIdentifier var2;
      try {
         var2 = asnIn.readObjectIdentifier();
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static byte[] toAsnObjectId(BBacnetObjectIdentifier objectId) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeObjectIdentifier(objectId);
      return releaseAsn(asnOut);
   }

   public static BValue fromAsn(int asnType, byte[] encodedValue) throws AsnException {
      return fromAsn(asnType, encodedValue, null);
   }

   public static BValue fromAsn(byte[] encodedValue, BValue obj) throws AsnException {
      if (obj != null) {
         Type t = obj.getType();
         if (t == BBacnetNull.TYPE) {
            return fromAsnNull(encodedValue);
         }

         if (t == BBoolean.TYPE) {
            return BBoolean.make(fromAsnBoolean(encodedValue));
         }

         if (t == BBacnetUnsigned.TYPE) {
            return fromAsnUnsigned(encodedValue);
         }

         if (t == BInteger.TYPE) {
            return BInteger.make(fromAsnInteger(encodedValue));
         }

         if (t == BFloat.TYPE) {
            return BFloat.make(fromAsnReal(encodedValue));
         }

         if (t == BDouble.TYPE) {
            return BDouble.make(fromAsnDouble(encodedValue));
         }

         if (t == BBacnetOctetString.TYPE) {
            return fromAsnOctetString(encodedValue);
         }

         if (t == BString.TYPE) {
            return BString.make(fromAsnCharacterString(encodedValue));
         }

         if (t == BBacnetBitString.TYPE) {
            return fromAsnBitString(encodedValue);
         }

         if (t.is(BEnum.TYPE)) {
            return ((BEnum)obj).getRange().get(fromAsnEnumerated(encodedValue));
         }

         if (t == BBacnetDate.TYPE) {
            return fromAsnDate(encodedValue);
         }

         if (t == BBacnetTime.TYPE) {
            return fromAsnTime(encodedValue);
         }

         if (t == BBacnetObjectIdentifier.TYPE) {
            return fromAsnObjectId(encodedValue);
         }

         if (t.is(BIBacnetDataType.TYPE)) {
            AsnInputStream asnIn = AsnInputStream.make(encodedValue);

            BValue var4;
            try {
               ((BIBacnetDataType)obj).readAsn(asnIn);
               var4 = obj;
            } finally {
               asnIn.release();
            }

            return var4;
         }
      }

      return asnToValue(encodedValue);
   }

   public static BValue fromAsn(int asnType, byte[] encodedValue, BValue obj) throws AsnException {
      switch (asnType) {
         case -6:
            return asnToValue(encodedValue);
         case -5:
         case -4:
         case -3:
         case -2:
         case -1:
            AsnInputStream asnIn = AsnInputStream.make(encodedValue);

            BValue var4;
            try {
               ((BIBacnetDataType)obj).readAsn(asnIn);
               var4 = obj;
            } finally {
               asnIn.release();
            }

            return var4;
         case 0:
            return BBacnetNull.DEFAULT;
         case 1:
            return BBoolean.make(fromAsnBoolean(encodedValue));
         case 2:
            if (obj != null && obj.getType() == BDynamicEnum.TYPE) {
               return ((BDynamicEnum)obj).getRange().get(fromAsnUnsignedInt(encodedValue));
            }

            return fromAsnUnsigned(encodedValue);
         case 3:
            return BInteger.make(fromAsnInteger(encodedValue));
         case 4:
            return BFloat.make(fromAsnReal(encodedValue));
         case 5:
            return BDouble.make(fromAsnDouble(encodedValue));
         case 6:
            return fromAsnOctetString(encodedValue);
         case 7:
            return BString.make(fromAsnCharacterString(encodedValue));
         case 8:
            return fromAsnBitString(encodedValue);
         case 9:
            if (obj == null) {
               return BInteger.make(fromAsnEnumerated(encodedValue));
            }

            return ((BEnum)obj).getRange().get(fromAsnEnumerated(encodedValue));
         case 10:
            return fromAsnDate(encodedValue);
         case 11:
            return fromAsnTime(encodedValue);
         case 12:
            return fromAsnObjectId(encodedValue);
         default:
            throw new AsnException("Invalid tag: " + asnType);
      }
   }

   public static BValue[] fromAsn(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BValue[] var2;
      try {
         var2 = fromAsn(asnIn, -1);
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static BValue asnToValue(PropertyInfo info, byte[] encodedValue) throws AsnException {
      return asnToValue(info, encodedValue, null);
   }

   private static BValue asnToValue(PropertyInfo info, byte[] encodedValue, BValue val) throws AsnException {
      try {
         if (info == null) {
            return asnToValue(encodedValue);
         } else {
            switch (info.getAsnType()) {
               case -6:
                  return asnToValue(encodedValue);
               case -5:
               case -1:
                  BTypeSpec tspecx = BTypeSpec.make(info.getType());
                  BValue o = (BValue)tspecx.getInstance();
                  AsnInputStream asnIn = AsnInputStream.make(encodedValue);

                  BValue var34;
                  try {
                     ((BIBacnetDataType)o).readAsn(asnIn);
                     var34 = o;
                  } finally {
                     asnIn.release();
                  }

                  return var34;
               case -4:
                  return BBacnetAny.make(encodedValue);
               case -3:
                  BTypeSpec tspecx = BTypeSpec.make(info.getType());
                  Type tx = tspecx.getResolvedType();
                  BBacnetListOf list = new BBacnetListOf(tx);
                  AsnInputStream asnIn = AsnInputStream.make(encodedValue);

                  BBacnetListOf var10;
                  try {
                     list.readAsn(asnIn);
                     var10 = list;
                  } finally {
                     asnIn.release();
                  }

                  return var10;
               case -2:
                  BTypeSpec tspec = BTypeSpec.make(info.getType());
                  Type t = tspec.getResolvedType();
                  BBacnetArray array;
                  if (info.getSize() >= 0) {
                     array = new BBacnetArray(t, info.getSize());
                  } else {
                     array = new BBacnetArray(t);
                  }

                  AsnInputStream asnIn = AsnInputStream.make(encodedValue);

                  BBacnetArray list;
                  try {
                     array.readAsn(asnIn);
                     list = array;
                  } finally {
                     asnIn.release();
                  }

                  return list;
               case 0:
                  return BBacnetNull.DEFAULT;
               case 1:
                  return BBoolean.make(fromAsnBoolean(encodedValue));
               case 2:
                  return fromAsnUnsigned(encodedValue);
               case 3:
                  return BInteger.make(fromAsnInteger(encodedValue));
               case 4:
                  return BFloat.make(fromAsnReal(encodedValue));
               case 5:
                  return BDouble.make(fromAsnDouble(encodedValue));
               case 6:
                  return fromAsnOctetString(encodedValue);
               case 7:
                  return BString.make(fromAsnCharacterString(encodedValue));
               case 8:
                  return fromAsnBitString(encodedValue);
               case 9:
                  BTypeSpec tspec = BTypeSpec.make(info.getType());
                  BEnum e = (BEnum)tspec.getInstance();
                  if (info.isExtensible()) {
                     return BDynamicEnum.make(fromAsnEnumerated(encodedValue), BEnumRange.make(e.getType()));
                  }

                  return e.getRange().get(fromAsnEnumerated(encodedValue));
               case 10:
                  return fromAsnDate(encodedValue);
               case 11:
                  return fromAsnTime(encodedValue);
               case 12:
                  return fromAsnObjectId(encodedValue);
               default:
                  throw new AsnException("Invalid tag: " + info.getAsnType());
            }
         }
      } catch (Exception var27) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Unable to decode Asn value:" + ByteArrayUtil.toHexString(encodedValue), (Throwable)var27);
         }

         throw new AsnException("Asn conversion failed");
      }
   }

   public static BValue asnToValue(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BValue var2;
      try {
         var2 = asnToValue(asnIn, -1);
      } finally {
         asnIn.release();
      }

      return var2;
   }

   public static BValue asnToValue(AsnInput in, int closingTag) throws AsnException {
      synchronized (in) {
         BValue[] vals = fromAsn(in, closingTag);
         if (vals.length == 1) {
            return vals[0];
         } else {
            BComponent c = new BComponent();

            for (int i = 0; i < vals.length; i++) {
               c.add(null, vals[i]);
            }

            return c;
         }
      }
   }

   public static BValue[] fromAsn(AsnInput in, int closingTag) throws AsnException {
      Array<BValue> a = new Array(BValue.class);
      synchronized (in) {
         int tag = in.peekTag();
         BValue o = null;

         while (tag != -1 && !in.isClosingTag(closingTag)) {
            o = null;
            if (in.isApplicationTag(tag)) {
               switch (tag) {
                  case 0:
                     o = in.readNull();
                     break;
                  case 1:
                     o = BBoolean.make(in.readBoolean());
                     break;
                  case 2:
                     o = in.readUnsigned();
                     break;
                  case 3:
                     o = in.readSigned();
                     break;
                  case 4:
                     o = in.readFloat();
                     break;
                  case 5:
                     o = BDouble.make(in.readDouble());
                     break;
                  case 6:
                     o = in.readBacnetOctetString();
                     break;
                  case 7:
                     o = BString.make(in.readCharacterString());
                     break;
                  case 8:
                     o = in.readBitString();
                     break;
                  case 9:
                     o = BDynamicEnum.make(in.readEnumerated());
                     break;
                  case 10:
                     o = in.readDate();
                     break;
                  case 11:
                     o = in.readTime();
                     break;
                  case 12:
                     o = in.readObjectIdentifier();
               }

               tag = in.peekTag();
            } else if (in.isOpeningTag(tag)) {
               in.skipTag();
               o = asnToValue(in, tag);
               tag = in.peekTag();
            } else if (in.isValueTag(tag)) {
               byte[] b = in.readContextTaggedData();
               o = BString.make("context-tagged data [" + tag + "]:" + ByteArrayUtil.toHexString(b));
               tag = in.peekTag();
            } else if (in.isClosingTag(tag)) {
               in.skipTag();
               tag = in.peekTag();
            }

            if (o != null) {
               if (a.size() == 0 && tag == -1) {
                  return new BValue[]{o};
               }

               a.add(o);
            }
         }

         return (BValue[])a.trim();
      }
   }

   public static byte[] toAsn(BValue o) {
      Type t = o.getType();
      if (t == BBacnetNull.TYPE) {
         return toAsnNull();
      } else if (t == BBoolean.TYPE) {
         return toAsnBoolean((BBoolean)o);
      } else if (t == BBacnetUnsigned.TYPE) {
         return toAsnUnsigned((BBacnetUnsigned)o);
      } else if (t == BInteger.TYPE) {
         return toAsnInteger((BInteger)o);
      } else if (t == BFloat.TYPE) {
         return toAsnReal((BFloat)o);
      } else if (t == BDouble.TYPE) {
         return toAsnDouble((BDouble)o);
      } else if (t == BBacnetOctetString.TYPE) {
         return toAsnOctetString((BBacnetOctetString)o);
      } else if (t == BString.TYPE) {
         return toAsnCharacterString((BString)o);
      } else if (t == BBacnetBitString.TYPE) {
         return toAsnBitString((BBacnetBitString)o);
      } else if (o instanceof BEnum) {
         return toAsnEnumerated((BEnum)o);
      } else if (t == BBacnetDate.TYPE) {
         return toAsnDate((BBacnetDate)o);
      } else if (t == BBacnetTime.TYPE) {
         return toAsnTime((BBacnetTime)o);
      } else if (t == BBacnetObjectIdentifier.TYPE) {
         return toAsnObjectId((BBacnetObjectIdentifier)o);
      } else if (t.is(BIBacnetDataType.TYPE)) {
         AsnOutputStream asnOut = getAsnOut();
         asnOut.reset();
         ((BIBacnetDataType)o).writeAsn(asnOut);
         return releaseAsn(asnOut);
      } else {
         return toAsnCharacterString(o.toString());
      }
   }

   public static byte[] toAsn(int asnType, BValue o) {
      switch (asnType) {
         case -5:
         case -4:
         case -3:
         case -2:
         case -1:
            AsnOutputStream asnOut = getAsnOut();
            asnOut.reset();
            ((BIBacnetDataType)o).writeAsn(asnOut);
            return releaseAsn(asnOut);
         case 0:
            return toAsnNull((BBacnetNull)o);
         case 1:
            return toAsnBoolean((BBoolean)o);
         case 2:
            return toAsnUnsigned((BBacnetUnsigned)o);
         case 3:
            return toAsnInteger(((BNumber)o).getInt());
         case 4:
            return toAsnReal(((BNumber)o).getFloat());
         case 5:
            return toAsnDouble(((BNumber)o).getDouble());
         case 6:
            return toAsnOctetString((BBacnetOctetString)o);
         case 7:
            return toAsnCharacterString((BString)o);
         case 8:
            return toAsnBitString((BBacnetBitString)o);
         case 9:
            return toAsnEnumerated((BEnum)o);
         case 10:
            return toAsnDate((BBacnetDate)o);
         case 11:
            return toAsnTime((BBacnetTime)o);
         case 12:
            return toAsnObjectId((BBacnetObjectIdentifier)o);
         default:
            throw new IllegalArgumentException("Invalid tag: " + asnType);
      }
   }

   public static int getAsnType(Type t) {
      if (t == BBacnetNull.TYPE) {
         return 0;
      } else if (t == BBoolean.TYPE) {
         return 1;
      } else if (t == BBacnetUnsigned.TYPE) {
         return 2;
      } else if (t == BInteger.TYPE) {
         return 3;
      } else if (t == BFloat.TYPE) {
         return 4;
      } else if (t == BDouble.TYPE) {
         return 5;
      } else if (t == BBacnetOctetString.TYPE) {
         return 6;
      } else if (t == BString.TYPE) {
         return 7;
      } else if (t == BBacnetBitString.TYPE) {
         return 8;
      } else if (t.is(BEnum.TYPE)) {
         return 9;
      } else if (t == BBacnetDate.TYPE) {
         return 10;
      } else if (t == BBacnetTime.TYPE) {
         return 11;
      } else {
         return t == BBacnetObjectIdentifier.TYPE ? 12 : -6;
      }
   }

   public static int getAsnType(byte[] encodedValue) throws AsnException {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      int var3;
      try {
         asnIn.setBuffer(encodedValue);
         int tag = asnIn.skipTag();
         if (asnIn.peekApplicationTag() != -1) {
            return -1;
         }

         var3 = tag;
      } finally {
         asnIn.release();
      }

      return var3;
   }

   public static BStatus asnStatusFlagsToBStatus(byte[] encodedValue) {
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      Object var3;
      try {
         return asnIn.readStatusFlags();
      } catch (AsnException var7) {
         var3 = null;
      } finally {
         asnIn.release();
      }

      return (BStatus)var3;
   }

   public static BStatus addPriority(BStatus status, byte[] encodedPriority) {
      if (encodedPriority != null) {
         BStatus s = BStatus.make(status, "bac", BString.make("def"));
         AsnInputStream asnIn = AsnInputStream.make(encodedPriority);

         BStatus activeLevel;
         try {
            int tag = 0;
            int activeLevelx = 0;

            for (int var13 = 1; var13 <= 16; var13++) {
               tag = asnIn.skipTag();
               if (tag != 0) {
                  return BStatus.make(s, "bac", BInteger.make(var13));
               }
            }

            return s;
         } catch (AsnException var9) {
            activeLevel = status;
         } finally {
            asnIn.release();
         }

         return activeLevel;
      } else {
         return status;
      }
   }

   public static byte[] statusToAsnStatusFlags(BStatus s) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeStatusFlags(s);
      return releaseAsn(asnOut);
   }

   public static byte[] toBacnetDateTime(BAbsTime t) {
      AsnOutputStream asnOut = getAsnOut();
      asnOut.reset();
      asnOut.writeDate(t);
      asnOut.writeTime(t);
      return releaseAsn(asnOut);
   }

   public static int getSize(BTypeSpec typeSpec) {
      BInteger i = (BInteger)sizesBySpec.get(typeSpec.hashCode());
      return i == null ? 10 : i.getInt();
   }

   private static AsnOutputStream getAsnOut() {
      synchronized (asnPool) {
         return asnCnt > 0 ? asnPool[--asnCnt] : new AsnOutputStream();
      }
   }

   private static byte[] releaseAsn(AsnOutputStream aos) {
      byte[] ba = aos.toByteArray();
      synchronized (asnPool) {
         if (asnCnt < asnPool.length) {
            asnPool[asnCnt++] = aos;
         }

         return ba;
      }
   }

   public static final NErrorType peekTagAndPerform(AsnInput asnIn, int matchTag, int errorCode, Runnable perform) throws AsnException {
      int tag = asnIn.peekTag();
      if (matchTag != tag) {
         return new NErrorType(2, errorCode);
      } else {
         perform.run();
         return null;
      }
   }

   static {
      asnNamesByType.put(0, lex.getText("asn.null"));
      asnNamesByType.put(1, lex.getText("asn.boolean"));
      asnNamesByType.put(2, lex.getText("asn.unsigned"));
      asnNamesByType.put(3, lex.getText("asn.integer"));
      asnNamesByType.put(4, lex.getText("asn.real"));
      asnNamesByType.put(5, lex.getText("asn.double"));
      asnNamesByType.put(6, lex.getText("asn.octetString"));
      asnNamesByType.put(7, lex.getText("asn.characterString"));
      asnNamesByType.put(8, lex.getText("asn.bitString"));
      asnNamesByType.put(9, lex.getText("asn.enumerated"));
      asnNamesByType.put(10, lex.getText("asn.date"));
      asnNamesByType.put(11, lex.getText("asn.time"));
      asnNamesByType.put(12, lex.getText("asn.objectId"));
      asnNamesByType.put(13, lex.getText("asn.reserved13"));
      asnNamesByType.put(14, lex.getText("asn.reserved14"));
      asnNamesByType.put(15, lex.getText("asn.reserved15"));
      asnNamesByType.put(-1, lex.getText("asn.constructed"));
      asnNamesByType.put(-2, lex.getText("asn.array"));
      asnNamesByType.put(-3, lex.getText("asn.list"));
      asnNamesByType.put(-4, lex.getText("asn.any"));
      asnNamesByType.put(-5, lex.getText("asn.choice"));
      asnNamesByType.put(-6, lex.getText("asn.unknown"));
      asnTypesByName.put(lex.getText("asn.null"), 0);
      asnTypesByName.put(lex.getText("asn.boolean"), 1);
      asnTypesByName.put(lex.getText("asn.unsigned"), 2);
      asnTypesByName.put(lex.getText("asn.integer"), 3);
      asnTypesByName.put(lex.getText("asn.real"), 4);
      asnTypesByName.put(lex.getText("asn.double"), 5);
      asnTypesByName.put(lex.getText("asn.octetString"), 6);
      asnTypesByName.put(lex.getText("asn.characterString"), 7);
      asnTypesByName.put(lex.getText("asn.bitString"), 8);
      asnTypesByName.put(lex.getText("asn.enumerated"), 9);
      asnTypesByName.put(lex.getText("asn.date"), 10);
      asnTypesByName.put(lex.getText("asn.time"), 11);
      asnTypesByName.put(lex.getText("asn.objectId"), 12);
      asnTypesByName.put(lex.getText("asn.reserved13"), 13);
      asnTypesByName.put(lex.getText("asn.reserved14"), 14);
      asnTypesByName.put(lex.getText("asn.reserved15"), 15);
      asnTypesByName.put(lex.getText("asn.constructed"), -1);
      asnTypesByName.put(lex.getText("asn.array"), -2);
      asnTypesByName.put(lex.getText("asn.list"), -3);
      asnTypesByName.put(lex.getText("asn.any"), -4);
      asnTypesByName.put(lex.getText("asn.choice"), -5);
      asnTypesByName.put(lex.getText("asn.unknown"), -6);
      sizesBySpec.put(BBacnetNull.TYPE.getTypeSpec().hashCode(), BInteger.make(1));
      sizesBySpec.put(BBoolean.TYPE.getTypeSpec().hashCode(), BInteger.make(1));
      sizesBySpec.put(BBacnetUnsigned.TYPE.getTypeSpec().hashCode(), BInteger.make(5));
      sizesBySpec.put(BInteger.TYPE.getTypeSpec().hashCode(), BInteger.make(5));
      sizesBySpec.put(BFloat.TYPE.getTypeSpec().hashCode(), BInteger.make(5));
      sizesBySpec.put(BDouble.TYPE.getTypeSpec().hashCode(), BInteger.make(9));
      sizesBySpec.put(BBacnetOctetString.TYPE.getTypeSpec().hashCode(), BInteger.make(27));
      sizesBySpec.put(BString.TYPE.getTypeSpec().hashCode(), BInteger.make(28));
      sizesBySpec.put(BBacnetBitString.TYPE.getTypeSpec().hashCode(), BInteger.make(8));
      sizesBySpec.put(BEnum.TYPE.getTypeSpec().hashCode(), BInteger.make(3));
      sizesBySpec.put(BBacnetDate.TYPE.getTypeSpec().hashCode(), BInteger.make(5));
      sizesBySpec.put(BBacnetTime.TYPE.getTypeSpec().hashCode(), BInteger.make(5));
      sizesBySpec.put(BBacnetObjectIdentifier.TYPE.getTypeSpec().hashCode(), BInteger.make(5));
      sizesBySpec.put(BBacnetDateTime.TYPE.getTypeSpec().hashCode(), BInteger.make(10));
      sizesBySpec.put(BBacnetTimeStamp.TYPE.getTypeSpec().hashCode(), BInteger.make(12));
      sizesBySpec.put(BBacnetTimeValue.TYPE.getTypeSpec().hashCode(), BInteger.make(10));
   }
}
