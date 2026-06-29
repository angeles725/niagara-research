package com.tridium.bacnet.stack.link.sc;

import com.tridium.bacnet.stack.link.sc.connection.BAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BInitiatingConnection;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "nodeMaxBvlcLength",
      type = "int",
      defaultValue = "MAX_SC_BVLC_LENGTH",
      facets = {@Facet(
         name = "BFacets.MAX",
         value = "MAX_SC_BVLC_LENGTH"
      ), @Facet(
         name = "BFacets.MIN",
         value = "NODE_MIN_BVLC_LENGTH"
      )}
   ), @NiagaraProperty(
      name = "nodeMaxNpduLength",
      type = "int",
      defaultValue = "MAX_NPDU_LENGTH",
      facets = {@Facet(
         name = "BFacets.MAX",
         value = "MAX_NPDU_LENGTH"
      ), @Facet(
         name = "BFacets.MIN",
         value = "NODE_MIN_NPDU_LENGTH"
      )}
   ), @NiagaraProperty(
      name = "minimumReconnectTime",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(2)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.makeSeconds(1)"
      ), @Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      )}
   ), @NiagaraProperty(
      name = "maximumReconnectTime",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(600)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.makeSeconds(1)"
      ), @Facet(
         name = "BFacets.MAX",
         value = "BRelTime.makeSeconds(600)"
      ), @Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      )}
   ), @NiagaraProperty(
      name = "connectWaitTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(10)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.makeSeconds(1)"
      ), @Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      )}
   ), @NiagaraProperty(
      name = "disconnectWaitTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(10)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.makeSeconds(1)"
      ), @Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      )}
   ), @NiagaraProperty(
      name = "webSocketWaitTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(10)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.makeSeconds(1)"
      ), @Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      )}
   ), @NiagaraProperty(
      name = "initiatingHeartbeatTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(300)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.makeSeconds(1)"
      ), @Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      )}
   ), @NiagaraProperty(
      name = "acceptingHeartbeatTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(500)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.makeSeconds(1)"
      ), @Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      )}
   )})
public final class BScConfiguration extends BComponent {
   public static final Property nodeMaxBvlcLength = newProperty(0, 65535, BFacets.make(BFacets.make("max", 65535), BFacets.make("min", 91)));
   public static final Property nodeMaxNpduLength = newProperty(0, 61327, BFacets.make(BFacets.make("max", 61327), BFacets.make("min", 74)));
   public static final Property minimumReconnectTime = newProperty(
      0,
      BRelTime.makeSeconds(2),
      BFacets.make(BFacets.make(BFacets.make("min", BRelTime.makeSeconds(1)), BFacets.make("showSeconds", true)), BFacets.make("showMilliseconds", false))
   );
   public static final Property maximumReconnectTime = newProperty(
      0,
      BRelTime.makeSeconds(600),
      BFacets.make(
         BFacets.make(
            BFacets.make(BFacets.make("min", BRelTime.makeSeconds(1)), BFacets.make("max", BRelTime.makeSeconds(600))), BFacets.make("showSeconds", true)
         ),
         BFacets.make("showMilliseconds", false)
      )
   );
   public static final Property connectWaitTimeout = newProperty(
      0,
      BRelTime.makeSeconds(10),
      BFacets.make(BFacets.make(BFacets.make("min", BRelTime.makeSeconds(1)), BFacets.make("showSeconds", true)), BFacets.make("showMilliseconds", false))
   );
   public static final Property disconnectWaitTimeout = newProperty(
      0,
      BRelTime.makeSeconds(10),
      BFacets.make(BFacets.make(BFacets.make("min", BRelTime.makeSeconds(1)), BFacets.make("showSeconds", true)), BFacets.make("showMilliseconds", false))
   );
   public static final Property webSocketWaitTimeout = newProperty(
      0,
      BRelTime.makeSeconds(10),
      BFacets.make(BFacets.make(BFacets.make("min", BRelTime.makeSeconds(1)), BFacets.make("showSeconds", true)), BFacets.make("showMilliseconds", false))
   );
   public static final Property initiatingHeartbeatTimeout = newProperty(
      0,
      BRelTime.makeSeconds(300),
      BFacets.make(BFacets.make(BFacets.make("min", BRelTime.makeSeconds(1)), BFacets.make("showSeconds", true)), BFacets.make("showMilliseconds", false))
   );
   public static final Property acceptingHeartbeatTimeout = newProperty(
      0,
      BRelTime.makeSeconds(500),
      BFacets.make(BFacets.make(BFacets.make("min", BRelTime.makeSeconds(1)), BFacets.make("showSeconds", true)), BFacets.make("showMilliseconds", false))
   );
   public static final Type TYPE = Sys.loadType(BScConfiguration.class);
   private static final double IDLE_TIMEOUT_MULTIPLIER = 4.0;
   private BScLinkLayer scLinkLayer;

   public int getNodeMaxBvlcLength() {
      return this.getInt(nodeMaxBvlcLength);
   }

   public void setNodeMaxBvlcLength(int v) {
      this.setInt(nodeMaxBvlcLength, v, null);
   }

   public int getNodeMaxNpduLength() {
      return this.getInt(nodeMaxNpduLength);
   }

   public void setNodeMaxNpduLength(int v) {
      this.setInt(nodeMaxNpduLength, v, null);
   }

   public BRelTime getMinimumReconnectTime() {
      return (BRelTime)this.get(minimumReconnectTime);
   }

   public void setMinimumReconnectTime(BRelTime v) {
      this.set(minimumReconnectTime, v, null);
   }

   public BRelTime getMaximumReconnectTime() {
      return (BRelTime)this.get(maximumReconnectTime);
   }

   public void setMaximumReconnectTime(BRelTime v) {
      this.set(maximumReconnectTime, v, null);
   }

   public BRelTime getConnectWaitTimeout() {
      return (BRelTime)this.get(connectWaitTimeout);
   }

   public void setConnectWaitTimeout(BRelTime v) {
      this.set(connectWaitTimeout, v, null);
   }

   public BRelTime getDisconnectWaitTimeout() {
      return (BRelTime)this.get(disconnectWaitTimeout);
   }

   public void setDisconnectWaitTimeout(BRelTime v) {
      this.set(disconnectWaitTimeout, v, null);
   }

   public BRelTime getWebSocketWaitTimeout() {
      return (BRelTime)this.get(webSocketWaitTimeout);
   }

   public void setWebSocketWaitTimeout(BRelTime v) {
      this.set(webSocketWaitTimeout, v, null);
   }

   public BRelTime getInitiatingHeartbeatTimeout() {
      return (BRelTime)this.get(initiatingHeartbeatTimeout);
   }

   public void setInitiatingHeartbeatTimeout(BRelTime v) {
      this.set(initiatingHeartbeatTimeout, v, null);
   }

   public BRelTime getAcceptingHeartbeatTimeout() {
      return (BRelTime)this.get(acceptingHeartbeatTimeout);
   }

   public void setAcceptingHeartbeatTimeout(BRelTime v) {
      this.set(acceptingHeartbeatTimeout, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.scLinkLayer = (BScLinkLayer)this.getParent();
   }

   public void changed(Property property, Context context) {
      if (this.isRunning()) {
         if (acceptingHeartbeatTimeout.equals(property)) {
            long timeout = this.getAcceptingSocketIdleTimeout();

            for (BAcceptingConnection connection : this.scLinkLayer.getDirectAcceptingConnections()) {
               connection.setSocketIdleTimeout(timeout);
               connection.resetPeriodicWaitTicket();
            }

            for (BAcceptingConnection connection : this.scLinkLayer.getHubFunctionConnections()) {
               connection.setSocketIdleTimeout(timeout);
               connection.resetPeriodicWaitTicket();
            }
         } else if (initiatingHeartbeatTimeout.equals(property)) {
            long timeout = this.getInitiatingSocketIdleTimeout();
            BHubConnector hubConnector = this.scLinkLayer.getHubConnector();
            hubConnector.getPrimaryConnection().setSocketIdleTimeout(timeout);
            hubConnector.getFailoverConnection().setSocketIdleTimeout(timeout);
            hubConnector.getPrimaryConnection().resetPeriodicWaitTicket();
            hubConnector.getFailoverConnection().resetPeriodicWaitTicket();

            for (BInitiatingConnection connection : this.scLinkLayer.getDirectInitiatingConnections()) {
               connection.setSocketIdleTimeout(timeout);
               connection.resetPeriodicWaitTicket();
            }
         } else if (nodeMaxBvlcLength.equals(property)) {
            this.scLinkLayer.getNodeSwitch().getWebSocketAcceptor().updateWebSocketSettings();
            this.scLinkLayer.getWebSocketInitiator().updateWebSocketSettings();
         }
      }
   }

   public long getInitiatingSocketIdleTimeout() {
      return (long)(this.getInitiatingHeartbeatTimeout().getMillis() * 4.0);
   }

   public long getAcceptingSocketIdleTimeout() {
      return (long)(this.getAcceptingHeartbeatTimeout().getMillis() * 4.0);
   }
}
