package javax.baja.bacnet;

import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.util.Lexicon;

public interface BacnetConst {
   Lexicon bacnetLexicon = Lexicon.make("bacnet");
   int VENDOR_ID_TRIDIUM = 36;
   @Deprecated
   int PROPERTY_ID_NOT_USED = -1;
   @Deprecated
   int PROPERTY_ARRAY_INDEX_NOT_USED = -1;
   @Deprecated
   int NO_PRIORITY = -1;
   @Deprecated
   int RANGE_NOT_USED = -1;
   @Deprecated
   int SEQUENCE_NUMBER_NOT_USED = -1;
   @Deprecated
   int REFERENCE_INDEX_NOT_USED = -1;
   int NOT_USED = -1;
   @Deprecated
   int BACNET_SF_MASK = 15;
   @Deprecated
   int NIAGARA_SF_MASK = 112;
   int BACNET_SBITS_MASK = 43;
   @Deprecated
   long MAX_BACNET_UNSIGNED = 4294967295L;
   String PROPRIETARY_PREFIX = bacnetLexicon.getText("enum.proprietary");
   String ASHRAE_PREFIX = bacnetLexicon.getText("enum.ashrae");
   int PROPRIETARY_PREFIX_LENGTH = PROPRIETARY_PREFIX.length();
   int ASHRAE_PREFIX_LENGTH = ASHRAE_PREFIX.length();
   int BAC_JANUARY = 1;
   int BAC_FEBRUARY = 2;
   int BAC_MARCH = 3;
   int BAC_APRIL = 4;
   int BAC_MAY = 5;
   int BAC_JUNE = 6;
   int BAC_JULY = 7;
   int BAC_AUGUST = 8;
   int BAC_SEPTEMBER = 9;
   int BAC_OCTOBER = 10;
   int BAC_NOVEMBER = 11;
   int BAC_DECEMBER = 12;
   int FIRST_WEEK = 1;
   int SECOND_WEEK = 2;
   int THIRD_WEEK = 3;
   int FOURTH_WEEK = 4;
   int FIFTH_WEEK = 5;
   int LAST_SEVEN_DAYS = 6;
   int BAC_MONDAY = 1;
   int BAC_TUESDAY = 2;
   int BAC_WEDNESDAY = 3;
   int BAC_THURSDAY = 4;
   int BAC_FRIDAY = 5;
   int BAC_SATURDAY = 6;
   int BAC_SUNDAY = 7;
   int NIAGARA_SUNDAY = 0;
   int ASN_NULL = 0;
   int ASN_BOOLEAN = 1;
   int ASN_UNSIGNED = 2;
   int ASN_INTEGER = 3;
   int ASN_REAL = 4;
   int ASN_DOUBLE = 5;
   int ASN_OCTET_STRING = 6;
   int ASN_CHARACTER_STRING = 7;
   int ASN_BIT_STRING = 8;
   int ASN_ENUMERATED = 9;
   int ASN_DATE = 10;
   int ASN_TIME = 11;
   int ASN_OBJECT_IDENTIFIER = 12;
   int ASHRAE_RESERVED_13 = 13;
   int ASHRAE_RESERVED_14 = 14;
   int ASHRAE_RESERVED_15 = 15;
   int ASN_CONSTRUCTED_DATA = -1;
   int ASN_BACNET_ARRAY = -2;
   int ASN_BACNET_LIST = -3;
   int ASN_ANY = -4;
   int ASN_CHOICE = -5;
   int ASN_UNKNOWN_PROPRIETARY = -6;
   String[] ASN_PRIMITIVE_TAGS = new String[]{
      "NULL",
      "BOOLEAN",
      "Unsigned",
      "INTEGER",
      "REAL",
      "Double",
      "OCTET STRING",
      "CharacterString",
      "BIT STRING",
      "ENUMERATED",
      "Date",
      "Time",
      "BacnetObjectIdentifier"
   };
   String DYNAMIC_OBJECTS_FOLDER_NAME = "dynamicObjects";
   String DYNAMIC_POINTS_CREATED_FOR_EVENT_ENROLLMENT = "dynamicPoints";
   int ZERO = 0;
   int ONE = 1;
   int TWO = 2;
   int THREE = 3;
   int FOUR = 4;
   int FIVE = 5;
   int THOUSAND = 1000;
   int COV_LIFETIME_LIMIT = 28800;
   Context noWrite = new BasicContext() {
      public boolean equals(Object o) {
         return this == o;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:noWrite";
      }
   };
   Context nameContext = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:nameContext";
      }
   };
   Context facetsContext = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:facetsContext";
      }
   };
   Context objectIdContext = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:objectIdContext";
      }
   };
   Context fallback = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:fallbackContext";
      }
   };
   Context deviceRegistryContext = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:deviceRegistryContext";
      }
   };
   Context debugContext = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:debugContext";
      }
   };
}
