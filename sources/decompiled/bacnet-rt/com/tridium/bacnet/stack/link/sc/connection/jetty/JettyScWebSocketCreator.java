package com.tridium.bacnet.stack.link.sc.connection.jetty;

import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.connection.BAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.IScConnectionAcceptor;
import java.io.IOException;
import java.util.Objects;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

public final class JettyScWebSocketCreator implements WebSocketCreator {
   private final IScConnectionAcceptor connectionAcceptor;

   public JettyScWebSocketCreator(IScConnectionAcceptor connectionAcceptor) {
      Objects.requireNonNull(connectionAcceptor, "connectionAcceptor must not be null");
      this.connectionAcceptor = connectionAcceptor;
   }

   public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
      if (!servletUpgradeRequest.getSubProtocols().contains(this.connectionAcceptor.getSubProtocol())) {
         this.connectionAcceptor.getLogger().fine("missing sub-protocol while attempting to create a JettyScWebSocket");
         this.sendError(servletUpgradeResponse, 400, "needed subprotocol not found");
         return null;
      } else {
         servletUpgradeResponse.setAcceptedSubProtocol(this.connectionAcceptor.getSubProtocol());

         BAcceptingConnection connection;
         try {
            connection = this.connectionAcceptor.fetchConnection();
         } catch (Exception var5) {
            ScLinkLayerUtil.logException(
               this.connectionAcceptor.getLogger(), new StringBuilder("fetch connection failure while attempting to create a JettyScWebSocket"), var5
            );
            this.sendError(servletUpgradeResponse, 500, "connection not available");
            return null;
         }

         String localConnectionToken = servletUpgradeRequest.getHeader("Niagara-Local-Connection-Token");
         if (localConnectionToken != null) {
            connection.validateLocalConnectionToken(localConnectionToken);
         }

         return new JettyScWebSocket(connection);
      }
   }

   private void sendError(ServletUpgradeResponse servletUpgradeResponse, int status, String msg) {
      try {
         servletUpgradeResponse.sendError(status, msg);
      } catch (IOException var5) {
         ScLinkLayerUtil.logException(
            this.connectionAcceptor.getLogger(), new StringBuilder("Could not sendError due to a missing sub-protocol or fetchConnection exception"), var5
         );
      }
   }
}
