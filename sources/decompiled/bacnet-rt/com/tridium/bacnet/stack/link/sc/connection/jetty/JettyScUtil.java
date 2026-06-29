package com.tridium.bacnet.stack.link.sc.connection.jetty;

import org.eclipse.jetty.websocket.api.WebSocketPolicy;

final class JettyScUtil {
   static final int MAX_TEXT_MESSAGE_SIZE = 0;
   static final int MAX_TEXT_MESSAGE_BUFFER_SIZE = 1;
   static final int MAX_BINARY_MESSAGE_BUFFER_SIZE = 1;

   private JettyScUtil() {
   }

   static void configurePolicy(WebSocketPolicy policy, int maxBinaryMessageSize) {
      policy.setMaxTextMessageBufferSize(1);
      policy.setMaxTextMessageSize(0);
      policy.setMaxBinaryMessageBufferSize(1);
      policy.setMaxBinaryMessageSize(maxBinaryMessageSize);
   }
}
