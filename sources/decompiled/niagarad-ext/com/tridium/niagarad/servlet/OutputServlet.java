package com.tridium.niagarad.servlet;

import com.tridium.niagarad.io.OutputSocket;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class OutputServlet extends WebSocketServlet {
   public static void updatePolicy(boolean server, WebSocketPolicy policy) {
      if (server) {
         policy.setMaxTextMessageBufferSize(1024);
         policy.setMaxTextMessageSize(1024);
         policy.setMaxBinaryMessageBufferSize(1024);
         policy.setMaxBinaryMessageSize(1024);
         policy.setInputBufferSize(1024);
      } else {
         policy.setInputBufferSize(8192);
      }

      policy.setAsyncWriteTimeout(30000L);
      policy.setIdleTimeout(30000L);
   }

   public void configure(WebSocketServletFactory factory) {
      updatePolicy(true, factory.getPolicy());
      factory.register(OutputSocket.class);
   }
}
