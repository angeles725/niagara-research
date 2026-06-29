package com.tridium.bacnet.stack.link.sc.connection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.baja.bacnet.enums.BBacnetErrorCode;

public final class ScConnectionUtil {
   public static final String HUB_SUBPROTOCOL = "hub.bsc.bacnet.org";
   public static final String DC_SUBPROTOCOL = "dc.bsc.bacnet.org";
   public static final int NORMAL = 1000;
   public static final int SHUTDOWN = 1001;
   public static final int PROTOCOL = 1002;
   public static final int BAD_DATA = 1003;
   public static final int UNDEFINED = 1004;
   public static final int NO_CODE = 1005;
   public static final int NO_CLOSE = 1006;
   public static final int ABNORMAL = 1006;
   public static final int BAD_PAYLOAD = 1007;
   public static final int POLICY_VIOLATION = 1008;
   public static final int MESSAGE_TOO_LARGE = 1009;
   public static final int REQUIRED_EXTENSION = 1010;
   public static final int SERVER_ERROR = 1011;
   public static final int SERVICE_RESTART = 1012;
   public static final int TRY_AGAIN_LATER = 1013;
   public static final int INVALID_UPSTREAM_RESPONSE = 1014;
   public static final int FAILED_TLS_HANDSHAKE = 1015;
   private static final Map<Integer, BBacnetErrorCode> STATUS_ERROR_MAP = makeStatusErrorMap();

   private ScConnectionUtil() {
   }

   private static Map<Integer, BBacnetErrorCode> makeStatusErrorMap() {
      Map<Integer, BBacnetErrorCode> map = new HashMap<>();
      map.put(1000, BBacnetErrorCode.websocketClosedByPeer);
      map.put(1001, BBacnetErrorCode.websocketEndpointLeaves);
      map.put(1002, BBacnetErrorCode.websocketProtocolError);
      map.put(1003, BBacnetErrorCode.websocketDataNotAccepted);
      map.put(1006, BBacnetErrorCode.websocketClosedAbnormally);
      map.put(1007, BBacnetErrorCode.websocketDataInconsistent);
      map.put(1008, BBacnetErrorCode.websocketDataAgainstPolicy);
      map.put(1009, BBacnetErrorCode.websocketFrameTooLong);
      map.put(1010, BBacnetErrorCode.websocketExtensionMissing);
      map.put(1011, BBacnetErrorCode.websocketRequestUnavailable);
      return Collections.unmodifiableMap(map);
   }

   public static BBacnetErrorCode getWebSocketError(int statusCode) {
      return STATUS_ERROR_MAP.getOrDefault(statusCode, BBacnetErrorCode.websocketError);
   }
}
