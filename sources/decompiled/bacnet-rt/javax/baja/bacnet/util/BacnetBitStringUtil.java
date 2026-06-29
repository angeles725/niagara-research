package javax.baja.bacnet.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.ext.BLimitEnable;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.data.BIDataValue;
import javax.baja.nre.util.IntHashMap;
import javax.baja.status.BStatus;
import javax.baja.sys.BFacets;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.util.BDaysOfWeekBits;
import javax.baja.util.Lexicon;

public class BacnetBitStringUtil {
   public static final BBacnetBitString DEFAULT_STATUS = getBacnetStatusFlags(BStatus.ok);
   private static final Lexicon lexicon = Lexicon.make(Sys.getModuleForClass(BacnetBitStringUtil.class), Sys.getLanguage());
   private static final Logger logger = Logger.getLogger("bacnet.util");
   private static final HashMap<String, Integer> bitStringSizes = new HashMap<>();
   private static final HashMap<String, HashMap<String, BIDataValue>> bitStringsMap = new HashMap<>();
   private static final HashMap<String, HashMap<String, Integer>> bitStringsIndexMap = new HashMap<>();
   private static final IntHashMap propIdToTags = new IntHashMap();
   public static final String BACNET_GENERIC_BITS = "BacnetGenericBitString";
   public static final String BACNET_DAYS_OF_WEEK = "BacnetDaysOfWeek";
   public static final String BACNET_EVENT_TRANSITION_BITS = "BacnetEventTransitionBits";
   public static final String BACNET_LIMIT_ENABLE = "BacnetLimitEnable";
   public static final String BACNET_LOG_STATUS = "BacnetLogStatus";
   public static final String BACNET_OBJECT_TYPES_SUPPORTED = "BacnetObjectTypesSupported";
   public static final String BACNET_RESULT_FLAGS = "BacnetResultFlags";
   public static final String BACNET_SERVICES_SUPPORTED = "BacnetServicesSupported";
   public static final String BACNET_STATUS_FLAGS = "BacnetStatusFlags";
   public static final HashMap<String, BIDataValue> BACNET_GENERIC_BITS_MAP = generateMap1(lexicon, "BacnetGenericBitString");
   public static final HashMap<String, BIDataValue> BACNET_DAYS_OF_WEEK_MAP = generateMap1(lexicon, "BacnetDaysOfWeek");
   public static final HashMap<String, BIDataValue> BACNET_EVENT_TRANSITION_BITS_MAP = generateMap1(lexicon, "BacnetEventTransitionBits");
   public static final HashMap<String, BIDataValue> BACNET_LIMIT_ENABLE_MAP = generateMap1(lexicon, "BacnetLimitEnable");
   public static final HashMap<String, BIDataValue> BACNET_LOG_STATUS_MAP = generateMap1(lexicon, "BacnetLogStatus");
   public static final HashMap<String, BIDataValue> BACNET_OBJECT_TYPES_SUPPORTED_MAP = generateMap1(lexicon, "BacnetObjectTypesSupported");
   public static final HashMap<String, BIDataValue> BACNET_RESULT_FLAGS_MAP = generateMap1(lexicon, "BacnetResultFlags");
   public static final HashMap<String, BIDataValue> BACNET_SERVICES_SUPPORTED_MAP = generateMap1(lexicon, "BacnetServicesSupported");
   public static final HashMap<String, BIDataValue> BACNET_STATUS_FLAGS_MAP = generateMap1(lexicon, "BacnetStatusFlags");
   public static final HashMap<String, Integer> BACNET_GENERIC_BITS_INDEX_MAP = generateMap2(lexicon, "BacnetGenericBitString");
   public static final HashMap<String, Integer> BACNET_DAYS_OF_WEEK_INDEX_MAP = generateMap2(lexicon, "BacnetDaysOfWeek");
   public static final HashMap<String, Integer> BACNET_EVENT_TRANSITION_BITS_INDEX_MAP = generateMap2(lexicon, "BacnetEventTransitionBits");
   public static final HashMap<String, Integer> BACNET_LIMIT_ENABLE_INDEX_MAP = generateMap2(lexicon, "BacnetLimitEnable");
   public static final HashMap<String, Integer> BACNET_LOG_STATUS_INDEX_MAP = generateMap2(lexicon, "BacnetLogStatus");
   public static final HashMap<String, Integer> BACNET_OBJECT_TYPES_SUPPORTED_INDEX_MAP = generateMap2(lexicon, "BacnetObjectTypesSupported");
   public static final HashMap<String, Integer> BACNET_RESULT_FLAGS_INDEX_MAP = generateMap2(lexicon, "BacnetResultFlags");
   public static final HashMap<String, Integer> BACNET_SERVICES_SUPPORTED_INDEX_MAP = generateMap2(lexicon, "BacnetServicesSupported");
   public static final HashMap<String, Integer> BACNET_STATUS_FLAGS_INDEX_MAP = generateMap2(lexicon, "BacnetStatusFlags");
   public static final BFacets BACNET_DAYS_OF_WEEK_FACETS = BFacets.make(BACNET_DAYS_OF_WEEK_MAP);
   public static final BFacets BACNET_EVENT_TRANSITION_BITS_FACETS = BFacets.make(BACNET_EVENT_TRANSITION_BITS_MAP);
   public static final BFacets BACNET_LIMIT_ENABLE_FACETS = BFacets.make(BACNET_LIMIT_ENABLE_MAP);
   public static final BFacets BACNET_LOG_STATUS_FACETS = BFacets.make(BACNET_LOG_STATUS_MAP);
   public static final BFacets BACNET_OBJECT_TYPES_SUPPORTED_FACETS = BFacets.make(BACNET_OBJECT_TYPES_SUPPORTED_MAP);
   public static final BFacets BACNET_RESULT_FLAGS_FACETS = BFacets.make(BACNET_RESULT_FLAGS_MAP);
   public static final BFacets BACNET_SERVICES_SUPPORTED_FACETS = BFacets.make(BACNET_SERVICES_SUPPORTED_MAP);
   public static final BFacets BACNET_STATUS_FLAGS_FACETS = BFacets.make(BACNET_STATUS_FLAGS_MAP);
   public static final Context BacnetDaysOfWeek = BACNET_DAYS_OF_WEEK_FACETS;
   public static final Context BacnetEventTransitionBits = BACNET_EVENT_TRANSITION_BITS_FACETS;
   public static final Context BacnetLimitEnable = BACNET_LIMIT_ENABLE_FACETS;
   public static final Context BacnetLogStatus = BACNET_LOG_STATUS_FACETS;
   public static final Context BacnetObjectTypesSupported = BACNET_OBJECT_TYPES_SUPPORTED_FACETS;
   public static final Context BacnetResultFlags = BACNET_RESULT_FLAGS_FACETS;
   public static final Context BacnetServicesSupported = BACNET_SERVICES_SUPPORTED_FACETS;
   public static final Context BacnetStatusFlags = BACNET_STATUS_FLAGS_FACETS;

   public static BDaysOfWeekBits getBDaysOfWeekBits(BBacnetBitString bs) {
      return getBDaysOfWeekBits(bs.getBits());
   }

   public static BDaysOfWeekBits getBDaysOfWeekBits(boolean[] bsbits) {
      int len = 7;
      if (bsbits.length != len) {
         throw new IllegalArgumentException("Invalid bit string length!");
      } else {
         int bit = 2;
         int dowbits = bsbits[6] ? 1 : 0;

         for (int i = 0; i < bsbits.length - 1; i++) {
            if (bsbits[i]) {
               dowbits |= bit;
            }

            bit <<= 1;
         }

         return BDaysOfWeekBits.make(dowbits);
      }
   }

   public static BBacnetBitString getBacnetDaysOfWeek(BDaysOfWeekBits dow) {
      boolean[] bsbits = new boolean[7];
      int dowbits = dow.getBits();
      int bit = 2;

      for (int i = 0; i < 6; i++) {
         bsbits[i] = (dowbits & bit) != 0;
         bit <<= 1;
      }

      bsbits[6] = (dowbits & 1) != 0;
      return BBacnetBitString.make(bsbits, BACNET_DAYS_OF_WEEK_FACETS);
   }

   public static BAlarmTransitionBits getBAlarmTransitionBits(BBacnetBitString bs) {
      return getBAlarmTransitionBits(bs.getBits());
   }

   public static BAlarmTransitionBits getBAlarmTransitionBits(boolean[] bits) {
      return (BAlarmTransitionBits)getNiagaraBitString(bits, "BacnetEventTransitionBits", BAlarmTransitionBits.class);
   }

   public static BBacnetBitString getBacnetEventTransitionBits(BAlarmTransitionBits at) {
      return getBacnetBitString(at, "BacnetEventTransitionBits", BAlarmTransitionBits.class, BACNET_EVENT_TRANSITION_BITS_FACETS);
   }

   public static BLimitEnable getBLimitEnable(BBacnetBitString bs) {
      return getBLimitEnable(bs.getBits());
   }

   public static BLimitEnable getBLimitEnable(boolean[] bits) {
      BLimitEnable le = new BLimitEnable();
      le.setLowLimitEnable(bits[0]);
      le.setHighLimitEnable(bits[1]);
      return le;
   }

   public static BBacnetBitString getBacnetLimitEnable(BLimitEnable le) {
      return BBacnetBitString.make(new boolean[]{le.getLowLimitEnable(), le.getHighLimitEnable()}, BACNET_LIMIT_ENABLE_FACETS);
   }

   public static BStatus getBStatus(BBacnetBitString bs) {
      return getBStatus(bs.getBits());
   }

   public static BStatus getBStatus(boolean[] bits) {
      int ibits = 0;
      if (bits[0]) {
         ibits |= 8;
      }

      if (bits[1]) {
         ibits |= 2;
      }

      if (bits[2]) {
         ibits |= 32;
      }

      if (bits[3]) {
         ibits |= 1;
      }

      return BStatus.make(ibits);
   }

   public static BBacnetBitString getBacnetStatusFlags(BStatus status) {
      return BBacnetBitString.make(new boolean[]{status.isAlarm(), status.isFault(), status.isOverridden(), status.isDisabled()});
   }

   public static BBacnetBitString getBacnetStatusFlags(String s) {
      return BBacnetBitString.make(
         new boolean[]{s.indexOf("alarm") >= 0, s.indexOf("fault") >= 0, s.indexOf("overridden") >= 0, s.indexOf("outOfService") >= 0}
      );
   }

   private static Object getNiagaraBitString(boolean[] bsbits, String bsName, Class<?> niagaraClass) {
      int ibits = 0;

      try {
         int len = getBitStringLength(bsName);
         if (bsbits.length != len) {
            throw new IllegalArgumentException("Invalid bit string length!" + bsbits.length + " != " + len);
         } else {
            int bit = 1;

            for (int i = 0; i < len; i++) {
               if (bsbits[i]) {
                  ibits |= bit;
               }

               bit <<= 1;
            }

            Method maker = niagaraClass.getMethod("make", int.class);
            return maker.invoke(null, ibits);
         }
      } catch (Exception var7) {
         logger.log(Level.SEVERE, "Error creating Bacnet bit string!", (Throwable)var7);
         return null;
      }
   }

   private static BBacnetBitString getBacnetBitString(BSimple bitstr, String bsName, Class<?> niagaraClass, BFacets tags) {
      try {
         int len = getBitStringLength(bsName);
         boolean[] bsbits = new boolean[len];
         Method getBits = niagaraClass.getMethod("getBits", (Class<?>[])null);
         int ibits = (Integer)getBits.invoke(bitstr, (Object[])null);
         int bit = 1;

         for (int i = 0; i < len; i++) {
            bsbits[i] = (ibits & bit) != 0;
            bit <<= 1;
         }

         return BBacnetBitString.make(bsbits, tags);
      } catch (Exception var10) {
         logger.log(Level.SEVERE, "Error creating Bacnet bit string!", (Throwable)var10);
         return BBacnetBitString.make(new boolean[0]);
      }
   }

   public static BBacnetBitString decode(String s, String bitStringName) {
      StringTokenizer st = new StringTokenizer(s, ";");
      HashMap<String, Integer> m = getBitStringIndexMap(bitStringName);
      if (m == null) {
         throw new IllegalArgumentException("Bit String " + bitStringName + " not known to BacnetBitStringUtil!");
      } else {
         boolean[] bits = new boolean[getBitStringLength(bitStringName)];

         while (st.hasMoreTokens()) {
            Integer ndx = m.get(st.nextToken());
            if (ndx != null) {
               bits[ndx] = true;
            }
         }

         return BBacnetBitString.make(bits, BFacets.make(getBitStringMap(bitStringName)));
      }
   }

   public static Context getBitStringTags(int objectType, int propertyId) {
      return (Context)propIdToTags.get(propertyId);
   }

   public static int getBitStringLength(String name) {
      Integer i = bitStringSizes.get(name);
      return i == null ? 0 : i;
   }

   public static String[] getTags(Context cx) {
      if (cx == null) {
         return new String[0];
      } else {
         int i = 0;
         ArrayList<String> a = new ArrayList<>();

         BString tag;
         do {
            tag = (BString)cx.getFacet("bit" + i);
            if (tag != null) {
               a.add(tag.getString());
            }

            i++;
         } while (tag != null);

         return a.toArray(new String[0]);
      }
   }

   public static int getBitIndex(String mapName, String bitName) {
      HashMap<String, Integer> m = getBitStringIndexMap(mapName);
      if (m != null) {
         Integer id = m.get(bitName);
         if (id != null) {
            return id;
         }
      }

      return -1;
   }

   public static HashMap<String, BIDataValue> getBitStringMap(String name) {
      return bitStringsMap.get(name);
   }

   public static HashMap<String, Integer> getBitStringIndexMap(String name) {
      return bitStringsIndexMap.get(name);
   }

   private static HashMap<String, BIDataValue> generateMap1(Lexicon lexicon, String name) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      map.put("bsName", BString.make(name));

      try {
         int len = Integer.parseInt(lexicon.get(name + ".numBits"));
         bitStringSizes.put(name, len);

         for (int i = 0; i < len; i++) {
            String key = "bit" + i;
            BString value = BString.make(lexicon.get(name + "." + key));
            map.put(key, value);
         }

         bitStringsMap.put(name, map);
      } catch (NullPointerException var7) {
         logger.log(Level.SEVERE, "Error creating bit string map: " + name, (Throwable)var7);
      }

      return map;
   }

   private static HashMap<String, Integer> generateMap2(Lexicon lexicon, String name) {
      HashMap<String, Integer> map = new HashMap<>();

      try {
         int len = Integer.parseInt(lexicon.get(name + ".numBits"));

         for (int i = 0; i < len; i++) {
            String bitNum = "bit" + i;
            String key = lexicon.get(name + "." + bitNum);
            map.put(key, i);
         }

         bitStringsIndexMap.put(name, map);
      } catch (NullPointerException var7) {
         logger.log(Level.SEVERE, "Error creating bit string index map: " + name, (Throwable)var7);
      }

      return map;
   }

   static {
      propIdToTags.put(35, BacnetEventTransitionBits);
      propIdToTags.put(0, BacnetEventTransitionBits);
      propIdToTags.put(1, BacnetEventTransitionBits);
      propIdToTags.put(52, BacnetLimitEnable);
      propIdToTags.put(96, BacnetObjectTypesSupported);
      propIdToTags.put(97, BacnetServicesSupported);
      propIdToTags.put(111, BacnetStatusFlags);
   }
}
