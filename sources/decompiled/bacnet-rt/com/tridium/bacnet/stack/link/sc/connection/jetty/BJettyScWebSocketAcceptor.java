package com.tridium.bacnet.stack.link.sc.connection.jetty;

import com.tridium.authn.BAuthenticationService;
import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.bacnet.stack.link.sc.authentication.BBacnetScAuthenticationScheme;
import com.tridium.bacnet.stack.link.sc.authentication.BBacnetScAuthenticator;
import com.tridium.bacnet.stack.link.sc.connection.BAbstractScWebSocketAcceptor;
import com.tridium.bacnet.stack.link.sc.connection.IScConnectionAcceptor;
import java.io.IOException;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BAbstractAuthenticator;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.web.servlets.SecurityCheckServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@NiagaraType
public final class BJettyScWebSocketAcceptor extends BAbstractScWebSocketAcceptor {
   public static final Type TYPE = Sys.loadType(BJettyScWebSocketAcceptor.class);
   private final BJettyScWebSocketAcceptor.JettyScWebSocketServlet servlet = new BJettyScWebSocketAcceptor.JettyScWebSocketServlet();
   private IScConnectionAcceptor connectionAcceptor;
   private BScLinkLayer scLinkLayer;
   private WebSocketPolicy webSocketPolicy;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BJettyScWebSocketAcceptor make(String servletName) {
      BJettyScWebSocketAcceptor acceptor = new BJettyScWebSocketAcceptor();
      acceptor.setServletName(servletName);
      return acceptor;
   }

   @Override
   public void started() throws Exception {
      this.connectionAcceptor = (IScConnectionAcceptor)this.getParent();
      this.scLinkLayer = ScLinkLayerUtil.getScLinkLayer(this);
      super.started();
   }

   public HttpServlet getHttpServlet() {
      return this.servlet;
   }

   @Override
   public void updateWebSocketSettings() {
      try {
         this.webSocketPolicy.setMaxBinaryMessageSize(this.connectionAcceptor.getMaxBvlcLength());
      } catch (Exception var2) {
         ScLinkLayerUtil.logException(
            this.connectionAcceptor.getLogger(), new StringBuilder("Failed to set the web socket acceptor's policy max binary message size"), var2
         );
      }
   }

   private class JettyScWebSocketServlet extends WebSocketServlet implements SecurityCheckServlet {
      private JettyScWebSocketServlet() {
      }

      public void configure(WebSocketServletFactory factory) {
         BJettyScWebSocketAcceptor.this.webSocketPolicy = factory.getPolicy();
         JettyScUtil.configurePolicy(BJettyScWebSocketAcceptor.this.webSocketPolicy, BJettyScWebSocketAcceptor.this.connectionAcceptor.getMaxBvlcLength());
         factory.setCreator(new JettyScWebSocketCreator(BJettyScWebSocketAcceptor.this.connectionAcceptor));
      }

      protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
         if (!BJettyScWebSocketAcceptor.this.connectionAcceptor.canAcceptConnections()) {
            response.sendError(410);
         } else {
            BUser user = BUser.getCurrentAuthenticatedUser();
            if (user == null) {
               response.sendError(401);
            } else {
               BAbstractAuthenticator authenticator = user.getAuthenticator();
               if (authenticator instanceof BBacnetScAuthenticator
                  && ((BBacnetScAuthenticator)authenticator).isAssociatedWithLinkLayer(BJettyScWebSocketAcceptor.this.scLinkLayer)) {
                  super.service(request, response);
               } else {
                  response.sendError(401);
               }
            }
         }
      }

      public BAuthenticationScheme getConfiguredAuthenticationScheme() {
         try {
            BAuthenticationService authService = BAuthenticationService.getService();
            BBacnetScAuthenticationScheme[] bacnetSchemes = (BBacnetScAuthenticationScheme[])authService.getAuthenticationSchemes()
               .getChildren(BBacnetScAuthenticationScheme.class);
            if (bacnetSchemes.length == 0) {
               this.log("Station has no BacnetScAuthenticationScheme to handle BACnet/SC authentication.");
               return null;
            } else {
               return bacnetSchemes[0];
            }
         } catch (Exception var3) {
            this.log("Could not find BacnetScAuthenticationScheme to handle BACnet/SC authentication. Cause is: " + var3.getMessage());
            return null;
         }
      }
   }
}
