package com.tridium.bacnet.stack.link.sc;

import com.tridium.authn.BAuthenticationSchemeFolder;
import com.tridium.authn.BAuthenticationService;
import com.tridium.bacnet.stack.link.sc.authentication.BBacnetScAuthenticationScheme;
import com.tridium.bacnet.stack.link.sc.authentication.BBacnetScAuthenticator;
import com.tridium.bacnet.stack.link.sc.connection.BAbstractConnection;
import com.tridium.bacnet.stack.link.sc.connection.BAbstractScWebSocketAcceptor;
import com.tridium.bacnet.stack.link.sc.connection.BAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BHubAcceptingConnection;
import com.tridium.bacnet.stack.link.sc.connection.BInitiatingConnection;
import com.tridium.bacnet.stack.link.sc.connection.DuplicateVmacException;
import com.tridium.bacnet.stack.link.sc.connection.IScConnectionAcceptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.baja.license.Feature;
import javax.baja.naming.BOrdList;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 3
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   )})
@NiagaraActions({@NiagaraAction(
      name = "addScUser"
   ), @NiagaraAction(
      name = "removeScUser"
   )})
public abstract class BAbstractConnectionManager extends BComponent implements IScConnectionAcceptor, BIStatus {
   public static final Property status = newProperty(3, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Action addScUser = newAction(0, null);
   public static final Action removeScUser = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BAbstractConnectionManager.class);
   private final Map<UUID, Long> uuidToVmacMap = new HashMap<>();
   private final Map<Long, BAbstractConnection> vmacToConnectionMap = new ConcurrentHashMap<>();
   private boolean fatalFault;
   protected String trustAnchorFault;
   private int connectionLimit;
   protected BScLinkLayer scLinkLayer;
   private static final BAbstractConnection[] EMPTY_CONNECTION_ARRAY = new BAbstractConnection[0];
   private static final String NO_SC_USER_MESSAGE = ScLinkLayerUtil.LEXICON.getText("abstractConnectionManager.noAssociatedScUser", null);

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public void addScUser() {
      this.invoke(addScUser, null, null);
   }

   public void removeScUser() {
      this.invoke(removeScUser, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final void doAddScUser() {
      BUserService userService = BUserService.getService();
      if (this.scLinkLayer.hasAssociatedUser()) {
         throw new LocalizableRuntimeException("bacnet", "scLinkLayer.userAlreadyExists");
      } else {
         String scSchemeName = addScScheme();
         BUser user = new BUser();
         user.setEnabled(true);
         user.setAuthenticationSchemeName(scSchemeName);
         BBacnetScAuthenticator authenticator = new BBacnetScAuthenticator();
         authenticator.setScPorts(BOrdList.make(this.scLinkLayer.getParentSlotPathOrd()));
         user.setAuthenticator(authenticator);
         userService.add("BACnetSC_" + this.scLinkLayer.getParent().getName() + '?', user);
      }
   }

   private static String addScScheme() {
      BAuthenticationService service = BAuthenticationService.getService();
      BAuthenticationSchemeFolder schemeFolder = service.getAuthenticationSchemes();
      BBacnetScAuthenticationScheme[] scSchemes = (BBacnetScAuthenticationScheme[])schemeFolder.getChildren(BBacnetScAuthenticationScheme.class);
      if (scSchemes.length == 0) {
         String scSchemeName = "BACnetScScheme";
         schemeFolder.add(scSchemeName, new BBacnetScAuthenticationScheme());
         return scSchemeName;
      } else {
         return scSchemes[0].getName();
      }
   }

   public final void doRemoveScUser() {
      boolean hasMultiLayerUser = false;
      BUserService userService = BUserService.getService();

      for (BBacnetScAuthenticator scAuthenticator : BBacnetScAuthenticationScheme.findScAuthenticators()) {
         if (scAuthenticator.isAssociatedWithLinkLayer(this.scLinkLayer)) {
            if (scAuthenticator.getScPorts().size() > 1) {
               hasMultiLayerUser = true;
               scAuthenticator.removeLinkLayer(this.scLinkLayer);
            } else {
               userService.remove(scAuthenticator.getParent());
            }
         }
      }

      if (hasMultiLayerUser) {
         throw new LocalizableRuntimeException("bacnet", "scLinkLayer.userHasMultipleLinkLayers");
      } else {
         removeUnusedScScheme(userService);
      }
   }

   private static void removeUnusedScScheme(BUserService userService) {
      BAuthenticationService service = BAuthenticationService.getService();
      BAuthenticationSchemeFolder schemeFolder = service.getAuthenticationSchemes();
      BBacnetScAuthenticationScheme[] scSchemes = (BBacnetScAuthenticationScheme[])schemeFolder.getChildren(BBacnetScAuthenticationScheme.class);
      if (scSchemes.length > 0) {
         String scSchemeName = scSchemes[0].getName();

         for (BUser user : userService.getUsers()) {
            if (scSchemeName.equals(user.getAuthenticationSchemeName())) {
               return;
            }
         }

         schemeFolder.remove(scSchemes[0]);
      }
   }

   public Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 2:
            this.fwChanged((Property)a);
            break;
         case 11:
            this.fwStarted();
            break;
         case 12:
            this.fwStopped();
      }

      return super.fw(x, a, b, c, d);
   }

   private void fwStarted() {
      this.scLinkLayer = (BScLinkLayer)this.getParent();
      this.checkLicense();
      this.updateStatus();
   }

   private void fwChanged(Property property) {
      if (this.isRunning() && property.equals(status)) {
         this.updateStatus();
      }
   }

   private void fwStopped() {
      this.linkCommStop();
   }

   public abstract BAbstractScWebSocketAcceptor getWebSocketAcceptor();

   public void linkCommStart() {
      this.updateStatus();
      if (this.acceptEnabled()) {
         this.getWebSocketAcceptor().linkCommStart();
      }
   }

   public void linkCommStop() {
      this.updateStatus();
      this.disconnectAll();
      this.getWebSocketAcceptor().linkCommStop();
   }

   @Override
   public final boolean canAcceptConnections() {
      BStatus status = this.getStatus();
      return this.acceptEnabled() && !status.isDisabled() && !status.isFault();
   }

   public final boolean canInitiateConnections() {
      BStatus status = this.getStatus();
      return this.initiateEnabled() && !status.isDisabled() && !this.fatalFault;
   }

   abstract BConnectionContainer getConnectionContainer();

   @Override
   public final void activateConnection(BAbstractConnection connection) throws DuplicateVmacException {
      Objects.requireNonNull(connection);
      this.checkCanActivateConnection(connection instanceof BAcceptingConnection);
      this.checkConnectionType(connection);
      UUID uuid = connection.getRemoteUuid();
      Objects.requireNonNull(uuid);
      boolean isLocalConnection = isLocalConnection(connection);
      if (!isLocalConnection && uuid.equals(ScLinkLayerUtil.getLocalDeviceUuid())) {
         throw new DuplicateVmacException("UUID collides with accepting peer's UUID");
      } else {
         long vmac = connection.getRemoteVmac();
         VmacUtil.checkIsDeviceVmac(vmac);
         BAbstractConnection[] displacedConnections = null;
         synchronized (this.uuidToVmacMap) {
            long knownUuidVmac = this.uuidToVmacMap.getOrDefault(uuid, -1L);
            if (knownUuidVmac != -1L) {
               displacedConnections = this.handleKnownDeviceUuid(uuid, vmac, knownUuidVmac);
            } else {
               if (!isLocalConnection && vmac == this.scLinkLayer.getLocalVmac()) {
                  throw new DuplicateVmacException("VMAC collides with accepting peer's VMAC");
               }

               if (this.vmacToConnectionMap.containsKey(vmac)) {
                  throw new DuplicateVmacException("VMAC collides with another initiating peer's VMAC");
               }

               this.logActivateNewDeviceUuid(uuid, vmac);
            }

            this.uuidToVmacMap.put(uuid, vmac);
            this.vmacToConnectionMap.put(vmac, connection);
         }

         disconnect(displacedConnections);
      }
   }

   @Override
   public final void deactivateConnection(BAbstractConnection connection) {
      if (connection == null) {
         this.getLogger().fine("Attempted to deactivate a null connection");
      } else {
         if (connection instanceof BAcceptingConnection) {
            BConnectionContainer connections = this.getConnectionContainer();
            if (connections.shouldCleanupImmediately()) {
               connections.remove(connection);
            }
         }

         UUID deviceUuid = connection.getRemoteUuid();
         if (deviceUuid == null) {
            if (this.getLogger().isLoggable(Level.FINE)) {
               this.getLogger().fine("Connection being deactivated is missing a remote UUID: " + connection.getLogInfo());
            }
         } else {
            long vmac = connection.getRemoteVmac();
            if (!VmacUtil.isDeviceVmac(vmac)) {
               if (this.getLogger().isLoggable(Level.FINE)) {
                  this.getLogger().fine("The remote VMAC of a connection being deactivated is not a device VMAC: " + connection.getLogInfo());
               }
            } else {
               synchronized (this.uuidToVmacMap) {
                  BAbstractConnection activeConnection = this.vmacToConnectionMap.get(vmac);
                  if (activeConnection == connection) {
                     this.vmacToConnectionMap.remove(vmac);
                     Long activeVmac = this.uuidToVmacMap.remove(deviceUuid);
                     if (activeVmac != null && vmac == activeVmac) {
                        this.logDeactivate(deviceUuid, vmac);
                     } else {
                        this.logDeactivateUuidVmacMismatch(deviceUuid, vmac, activeVmac == null ? -1L : activeVmac);
                     }
                  } else if (activeConnection != null) {
                     if (deviceUuid.equals(activeConnection.getRemoteUuid())) {
                        this.logDeactivateSameUuid(deviceUuid, vmac);
                     } else {
                        this.logDeactivateUuidSwitch(deviceUuid, vmac, activeConnection);
                     }
                  } else if (this.uuidToVmacMap.containsKey(deviceUuid)) {
                     this.logDeactivateVmacSwitch(deviceUuid, vmac);
                  } else {
                     this.logDeactivateMissingUuid(deviceUuid, vmac);
                  }
               }
            }
         }
      }
   }

   public abstract boolean acceptEnabled();

   public abstract boolean initiateEnabled();

   protected abstract void checkConnectionType(BAbstractConnection var1);

   protected abstract String getConnectionLimitKey();

   private void checkLicense() {
      Feature feature;
      try {
         feature = Sys.getLicenseManager().getFeature("tridium", "bacnetSc");
         feature.check();
      } catch (Exception var6) {
         feature = null;
         this.getLogger().log(Level.SEVERE, "Unlicensed: " + this.toPathString(), (Throwable)var6);
      }

      int limit = 0;
      if (feature != null) {
         String connLimitStr = feature.get(this.getConnectionLimitKey());
         if (connLimitStr != null) {
            if ("none".equals(TextUtil.toLowerCase(connLimitStr))) {
               limit = Integer.MAX_VALUE;
            } else {
               try {
                  limit = Integer.parseInt(connLimitStr);
               } catch (NumberFormatException var5) {
                  limit = 0;
               }
            }
         }
      }

      this.connectionLimit = limit;
      if (this.connectionLimit < 1) {
         this.fatalFault = true;
         this.setFaultCause(ScLinkLayerUtil.LEXICON.getText("abstractConnectionManager.notLicensed", null));
      }
   }

   protected final boolean isConnectionLimitReached() {
      return this.getActiveConnectionCount() >= this.connectionLimit;
   }

   protected final void checkCanActivateConnection(boolean isAccepting) {
      if (isAccepting) {
         if (!this.canAcceptConnections()) {
            throw new IllegalStateException(this.getDisplayName(null) + " accept connections is disabled or in fault");
         }
      } else if (!this.canInitiateConnections()) {
         throw new IllegalStateException(this.getDisplayName(null) + " initiate connections is disabled or in fault");
      }

      if (this.isConnectionLimitReached()) {
         throw new IllegalStateException(this.getDisplayName(null) + " BACnet/SC license feature connection limit has been reached.");
      }
   }

   protected final void disconnectAll() {
      BAbstractConnection[] connections;
      synchronized (this.uuidToVmacMap) {
         connections = this.vmacToConnectionMap.values().toArray(EMPTY_CONNECTION_ARRAY);
         this.uuidToVmacMap.clear();
         this.vmacToConnectionMap.clear();
      }

      disconnect(connections);
   }

   protected final void disconnectConnections(boolean isAccepted) {
      List<BAbstractConnection> connections = new ArrayList<>();
      synchronized (this.uuidToVmacMap) {
         Iterator<Entry<Long, BAbstractConnection>> iterator = this.vmacToConnectionMap.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Long, BAbstractConnection> entry = iterator.next();
            BAbstractConnection connection = entry.getValue();
            if (isAccepted && connection instanceof BAcceptingConnection || !isAccepted && connection instanceof BInitiatingConnection) {
               connections.add(connection);
               this.uuidToVmacMap.remove(connection.getRemoteUuid());
               iterator.remove();
            }
         }
      }

      disconnect(connections.toArray(EMPTY_CONNECTION_ARRAY));
   }

   protected final int getActiveConnectionCount() {
      return this.vmacToConnectionMap.size();
   }

   protected final boolean hasActiveConnection(long vmac) {
      return this.vmacToConnectionMap.containsKey(vmac);
   }

   protected final Collection<BAbstractConnection> getActiveConnections() {
      return this.vmacToConnectionMap.values();
   }

   protected final BAbstractConnection getActiveConnection(long vmac) {
      return this.vmacToConnectionMap.get(vmac);
   }

   private static void disconnect(BAbstractConnection[] connections) {
      if (connections != null) {
         for (BAbstractConnection connection : connections) {
            if (connection != null) {
               connection.disconnect();
            }
         }
      }
   }

   private BAbstractConnection[] handleKnownDeviceUuid(UUID uuid, long newVmac, long oldVmac) {
      BAbstractConnection knownUuidConnection = this.vmacToConnectionMap.remove(oldVmac);
      if (knownUuidConnection != null) {
         this.logActivateKnownDeviceUuidDisconnect(uuid, newVmac, oldVmac);
      } else {
         this.logActivateKnownDeviceUuidConnectionMissing(uuid, newVmac, oldVmac);
      }

      BAbstractConnection replacedConnection = this.vmacToConnectionMap.remove(newVmac);
      if (replacedConnection != null) {
         this.uuidToVmacMap.remove(replacedConnection.getRemoteUuid());
         this.logActivateKnownDeviceUuidReplaceVmac(uuid, newVmac, replacedConnection);
      }

      return new BAbstractConnection[]{knownUuidConnection, replacedConnection};
   }

   private static boolean isLocalConnection(BAbstractConnection connection) {
      return connection instanceof BHubAcceptingConnection && ((BHubAcceptingConnection)connection).isLocal();
   }

   public void setTrustAnchorFault(String faultCause) {
      this.trustAnchorFault = faultCause;
      this.updateStatus();
   }

   public void updateStatus() {
      if (this.scLinkLayer != null) {
         int newStatus = 0;
         if ((this.acceptEnabled() || this.initiateEnabled()) && this.scLinkLayer.isCommStarted()) {
            newStatus &= -2;
         } else {
            newStatus |= 1;
         }

         if (this.fatalFault) {
            newStatus |= 2;
         } else if (this.acceptEnabled()) {
            String acceptorFaultCause = this.getWebSocketAcceptor().getFaultCause();
            if (!this.scLinkLayer.hasAssociatedUser()) {
               newStatus |= 2;
               this.setFaultCause(NO_SC_USER_MESSAGE);
            } else if (this.trustAnchorFault != null) {
               newStatus |= 2;
               this.setFaultCause(this.trustAnchorFault);
            } else if (acceptorFaultCause != null) {
               newStatus |= 2;
               this.setFaultCause(acceptorFaultCause);
            } else {
               newStatus &= -3;
               this.setFaultCause("");
            }
         } else {
            newStatus &= -3;
            this.setFaultCause("");
         }

         if (newStatus != this.getStatus().getBits()) {
            this.setStatus(BStatus.make(newStatus));
         }
      }
   }

   public boolean isFatalFault() {
      return this.fatalFault;
   }

   private void logActivateNewDeviceUuid(UUID deviceUuid, long vmac) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine("Activating connection for new device [" + deviceUuid + "], VMAC [" + VmacUtil.vmacToString(vmac) + ']');
      }
   }

   private void logActivateKnownDeviceUuidDisconnect(UUID uuid, long newVmac, long oldVmac) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         if (oldVmac == newVmac) {
            this.getLogger()
               .fine(
                  "Activating a new connection for a known device ["
                     + uuid
                     + "]; required disconnecting an existing connection at the same VMAC: ["
                     + VmacUtil.vmacToString(oldVmac)
                     + ']'
               );
         } else {
            this.getLogger()
               .fine(
                  "Activating a new connection for a known device ["
                     + uuid
                     + "] with the new VMAC ["
                     + VmacUtil.vmacToString(newVmac)
                     + "]; required disconnecting an existing connection at VMAC ["
                     + VmacUtil.vmacToString(oldVmac)
                     + ']'
               );
         }
      }
   }

   private void logActivateKnownDeviceUuidReplaceVmac(UUID uuid, long newVmac, BAbstractConnection oldVmacConnection) {
      if (this.getLogger().isLoggable(Level.INFO)) {
         this.getLogger()
            .info(
               "Activating a new connection for known device ["
                  + uuid
                  + "] forced a disconnection with another device ["
                  + oldVmacConnection.getRemoteUuid()
                  + "] due to a duplicate VMAC: ["
                  + VmacUtil.vmacToString(newVmac)
                  + ']'
            );
      }
   }

   private void logActivateKnownDeviceUuidConnectionMissing(UUID uuid, long newVmac, long oldVmac) {
      this.getLogger()
         .warning(
            "Activating a new connection for a known device ["
               + uuid
               + "] with the new VMAC ["
               + VmacUtil.vmacToString(newVmac)
               + "]; an existing connection to be disconnected at the old VMAC ["
               + VmacUtil.vmacToString(oldVmac)
               + "] could not be found"
         );
   }

   private void logDeactivate(UUID uuid, long vmac) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger().fine("Deactivating connection for device [" + uuid + "], VMAC [" + VmacUtil.vmacToString(vmac) + ']');
      }
   }

   private void logDeactivateUuidVmacMismatch(UUID uuid, long vmac, long activeVmac) {
      if (this.getLogger().isLoggable(Level.WARNING)) {
         this.getLogger()
            .warning(
               "Mismatch of device UUID ["
                  + uuid
                  + "] and VMAC during deactivateConnection; expected VMAC: ["
                  + VmacUtil.vmacToString(vmac)
                  + "] actual VMAC: ["
                  + VmacUtil.vmacToString(activeVmac)
                  + ']'
            );
      }
   }

   private void logDeactivateSameUuid(UUID uuid, long vmac) {
      if (this.getLogger().isLoggable(Level.FINE)) {
         this.getLogger()
            .fine(
               "Ignoring request to deactivate a connection that has already been replaced for device ["
                  + uuid
                  + "] at VMAC ["
                  + VmacUtil.vmacToString(vmac)
                  + ']'
            );
      }
   }

   private void logDeactivateUuidSwitch(UUID uuid, long vmac, BAbstractConnection activeConnection) {
      if (this.getLogger().isLoggable(Level.INFO)) {
         this.getLogger()
            .info(
               "Ignoring request to deactivate a connection that was replaced by another device with the same VMAC ["
                  + VmacUtil.vmacToString(vmac)
                  + "]; requested device: ["
                  + uuid
                  + "]; active device: ["
                  + activeConnection.getRemoteUuid()
                  + ']'
            );
      }
   }

   private void logDeactivateMissingUuid(UUID uuid, long vmac) {
      if (this.getLogger().isLoggable(Level.WARNING)) {
         this.getLogger()
            .warning("Attempt to deactivate a connection for device [" + uuid + "] with no active connections; VMAC: [" + VmacUtil.vmacToString(vmac) + ']');
      }
   }

   private void logDeactivateVmacSwitch(UUID uuid, long vmac) {
      if (this.getLogger().isLoggable(Level.INFO)) {
         this.getLogger()
            .info(
               "Ignoring request to deactivate a connection that was replaced for device ["
                  + uuid
                  + "] due to a change of VMAC; old VMAC: ["
                  + VmacUtil.vmacToString(vmac)
                  + "] new VMAC: ["
                  + VmacUtil.vmacToString(this.uuidToVmacMap.getOrDefault(uuid, -1L))
                  + ']'
            );
      }
   }

   public void spy(SpyWriter out) throws Exception {
      if (Sys.isStation()) {
         synchronized (this.uuidToVmacMap) {
            if (!this.uuidToVmacMap.isEmpty()) {
               out.startTable(true);
               out.tr().th("VMAC").th("Device UUID").endTr();

               for (Entry<UUID, Long> connectionEntry : this.uuidToVmacMap.entrySet()) {
                  out.tr(VmacUtil.vmacToString(connectionEntry.getValue()), connectionEntry.getKey());
               }

               out.endTable();
            }
         }
      }

      super.spy(out);
   }
}
