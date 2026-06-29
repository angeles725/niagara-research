package com.tridium.bacnet.stack.link.sc.message;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;

public final class ScMessageUtil {
   public static final int HEADER_MARKER_MORE_OPTIONS = 128;
   public static final int HEADER_MARKER_MUST_UNDERSTAND = 64;
   public static final int HEADER_MARKER_DATA_FLAG = 32;
   public static final int HEADER_MARKER_OPTION_TYPE = 31;
   public static final int SECURE_PATH_HEADER_OPTION_TYPE = 1;
   public static final int PROPRIETARY_HEADER_OPTION_TYPE = 31;
   public static final int VMAC_LENGTH = 6;
   static final int MAX_CONTROL_FLAGS = 15;
   public static final int HAS_ORIGINATING_VMAC = 8;
   public static final int HAS_DESTINATION_VMAC = 4;
   public static final int HAS_DESTINATION_OPTIONS = 2;
   public static final int HAS_DATA_OPTIONS = 1;
   public static final byte BVLC_RESULT = 0;
   public static final byte ENCAPSULATED_NPDU = 1;
   public static final byte ADDRESS_RESOLUTION = 2;
   public static final byte ADDRESS_RESOLUTION_ACK = 3;
   public static final byte ADVERTISEMENT = 4;
   public static final byte ADVERTISEMENT_SOLICITATION = 5;
   public static final byte CONNECT_REQUEST = 6;
   public static final byte CONNECT_ACCEPT = 7;
   public static final byte DISCONNECT_REQUEST = 8;
   public static final byte DISCONNECT_ACK = 9;
   public static final byte HEARTBEAT_REQUEST = 10;
   public static final byte HEARTBEAT_ACK = 11;
   public static final byte PROPRIETARY_MESSAGE = 12;
   public static final int UNSIGNED_BYTE_MAX = 255;
   public static final int UNSIGNED_SHORT_MAX = 65535;
   public static final int MAX_SC_BVLC_LENGTH = 65535;
   private static final int MIN_APDU_LENGTH = 50;
   public static final int MAX_NPDU_HEADER_SIZE = 24;
   public static final int NODE_MIN_NPDU_LENGTH = 74;
   public static final int MIN_BVLC_HEADER_SIZE = 16;
   public static final int NODE_MIN_BVLC_HEADER_OPTIONS_SIZE = 1;
   public static final int NODE_MIN_BVLC_LENGTH = 91;
   public static final int HUB_MIN_NPDU_LENGTH = 1497;
   public static final int HUB_MIN_BVLC_HEADER_OPTIONS_SIZE = 4192;
   public static final int HUB_MIN_BVLC_LENGTH = 5705;
   public static final int MAX_NPDU_LENGTH = 61327;

   private ScMessageUtil() {
   }

   static void checkUnsignedByte(int value, String name) {
      if (value < 0 || value > 255) {
         throw new IllegalArgumentException(name + " must be between zero and unsigned byte max (0xFF) inclusive");
      }
   }

   static void checkUnsignedShort(int value, String name) {
      if (value < 0 || value > 65535) {
         throw new IllegalArgumentException(name + " must be between zero and unsigned short max (0xFFFF) inclusive");
      }
   }

   static void checkResultFunction(int value) {
      if (!isFunctionValid(value)) {
         throw new IllegalArgumentException("resultFunction value " + value + " is not supported");
      }
   }

   static void checkFunction(int value) throws ScReadMessageException {
      if (!isFunctionValid(value)) {
         throw new ScReadMessageException("BVLC function value " + value + " is not valid", BBacnetErrorCode.bvlcFunctionUnknown);
      }
   }

   static void checkNpduLength(int value) {
      if (value < 74 || value > 61327) {
         throw new IllegalArgumentException("maxNpduLength is " + value + " but must be between " + 74 + " and " + '\uef8f' + " inclusive");
      }
   }

   static void checkBvlcLength(int value, int maxNpduLength) {
      int minBvlcLength = getMinBvlcLength(maxNpduLength);
      if (value < minBvlcLength || value > 65535) {
         throw new IllegalArgumentException(
            "maxBvlcLength value of "
               + value
               + " is invalid; for maxNpduLength of "
               + maxNpduLength
               + ", the maxBvlcLength must be between "
               + minBvlcLength
               + " and 65535 inclusive"
         );
      }
   }

   public static int getMinBvlcLength(int maxNpduLength) {
      return maxNpduLength + 16 + 1;
   }

   public static String checkWebSocketUri(String webSocketUri) {
      try {
         URI uri = new URI(webSocketUri);
         if (!"wss".equalsIgnoreCase(uri.getScheme())) {
            return "Scheme must be \"wss\"";
         } else if (uri.getUserInfo() != null) {
            return "Scheme does not support user info";
         } else if (uri.getHost() != null && !uri.getHost().isEmpty()) {
            if (uri.getPort() >= -1 && uri.getPort() <= 65535) {
               String query = uri.getQuery();
               if (query == null || !query.contains("[") && !query.contains("]")) {
                  return uri.getFragment() != null ? "Scheme does not support fragments" : null;
               } else {
                  return "Scheme does not support '[' or ']' characters in the query";
               }
            } else {
               return "Port value not valid";
            }
         } else {
            return "Host may not be empty";
         }
      } catch (URISyntaxException var3) {
         return var3.getReason();
      }
   }

   private static boolean isFunctionValid(int value) {
      switch (value) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
            return true;
         default:
            return false;
      }
   }

   static String readString(ByteBuffer in) throws IOException {
      int available = in.available();
      if (available > 0) {
         byte[] bytes = new byte[available];
         in.readFully(bytes);
         return new String(bytes, StandardCharsets.UTF_8);
      } else {
         return "";
      }
   }
}
