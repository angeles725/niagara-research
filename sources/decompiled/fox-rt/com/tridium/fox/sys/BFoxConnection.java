package com.tridium.fox.sys;

import com.tridium.fox.encoding.BogCodec;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.FoxAsyncCallbacks;
import com.tridium.fox.session.FoxCircuit;
import com.tridium.fox.session.FoxConnection;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.InvalidChannelException;
import com.tridium.fox.sys.spy.FoxIndexSpy;
import com.tridium.fox.sys.spy.FoxLog;
import com.tridium.sys.Nre;
import com.tridium.sys.license.Brand;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.baja.net.NotConnectedException;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.Spy;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BStation;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;
import javax.baja.util.Version;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "state",
      type = "String",
      defaultValue = "Not connected",
      flags = 3
   ), @NiagaraProperty(
      name = "lastConnectTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT",
      flags = 65537
   ), @NiagaraProperty(
      name = "lastDisconnectTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT",
      flags = 65537
   ), @NiagaraProperty(
      name = "lastDisconnectCause",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "channels",
      type = "BFoxChannelRegistry",
      defaultValue = "new BFoxChannelRegistry()",
      flags = 7
   )})
public abstract class BFoxConnection extends BComponent implements FoxConnection {
   public static final Property state = newProperty(3, "Not connected", null);
   public static final Property lastConnectTime = newProperty(65537, BAbsTime.DEFAULT, null);
   public static final Property lastDisconnectTime = newProperty(65537, BAbsTime.DEFAULT, null);
   public static final Property lastDisconnectCause = newProperty(1, "", null);
   public static final Property channels = newProperty(7, new BFoxChannelRegistry(), null);
   public static final Type TYPE = Sys.loadType(BFoxConnection.class);
   private static final BIcon icon = BIcon.std("navOnly/clientConnection.png");
   protected final FoxLog log = FoxLog.make("fox");
   private FoxSession session;
   private Version remoteVersion;
   private final Map<String, ? super FoxConnectionTarget> connectionTargets = new HashMap<>();
   Integer fwConnectionType = FoxSession.NON_FW_FOX_SESSION;

   public String getState() {
      return this.getString(state);
   }

   public void setState(String v) {
      this.setString(state, v, null);
   }

   public BAbsTime getLastConnectTime() {
      return (BAbsTime)this.get(lastConnectTime);
   }

   public void setLastConnectTime(BAbsTime v) {
      this.set(lastConnectTime, v, null);
   }

   public BAbsTime getLastDisconnectTime() {
      return (BAbsTime)this.get(lastDisconnectTime);
   }

   public void setLastDisconnectTime(BAbsTime v) {
      this.set(lastDisconnectTime, v, null);
   }

   public String getLastDisconnectCause() {
      return this.getString(lastDisconnectCause);
   }

   public void setLastDisconnectCause(String v) {
      this.setString(lastDisconnectCause, v, null);
   }

   public BFoxChannelRegistry getChannels() {
      return (BFoxChannelRegistry)this.get(channels);
   }

   public void setChannels(BFoxChannelRegistry v) {
      this.set(channels, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BFoxConnection() {
      this.set(channels, BFoxChannelRegistry.getPrototype().newCopy());
   }

   public final boolean isConnected() {
      return this.session != null && !this.session.isClosed();
   }

   public void assertConnected() throws NotConnectedException {
      if (this.session == null || this.session.isClosed()) {
         throw new NotConnectedException();
      }
   }

   public FoxSession session() {
      return this.session;
   }

   public Version getRemoteVersion() {
      return this.remoteVersion;
   }

   public void close() {
      this.setState("Not connected");
      if (this.session != null) {
         this.session.close(null);
      }
   }

   @Deprecated
   public NiagaraStation getParentStation() {
      return this.getConnectionTarget(NiagaraStation.class).orElse(null);
   }

   public void addConnectionTarget(FoxConnectionTarget target) {
      synchronized (this.connectionTargets) {
         this.connectionTargets.put(target.getClass().getName(), target);
      }
   }

   public void removeConnectionTarget(FoxConnectionTarget target) {
      synchronized (this.connectionTargets) {
         if (target != null && target == this.connectionTargets.get(target.getClass().getName())) {
            this.connectionTargets.remove(target.getClass().getName());
         }
      }
   }

   public <T extends FoxConnectionTarget> Optional<T> getConnectionTarget(Class<T> targetClass) {
      synchronized (this.connectionTargets) {
         for (Object candidate : this.connectionTargets.values()) {
            if (targetClass.isAssignableFrom(candidate.getClass())) {
               return Optional.of((T)candidate);
            }
         }

         return Optional.empty();
      }
   }

   @Override
   public void initHello(FoxMessage hello) throws Exception {
      BFoxSession foxSession = null;
      if (this instanceof BFoxClientConnection) {
         foxSession = ((BFoxClientConnection)this).getFoxSession();
      }

      BStation station = Sys.getStation();
      if (station != null && foxSession == null) {
         hello.add("station.name", station.getStationName());
      }

      Optional<NiagaraStation> parent = this.getConnectionTarget(NiagaraStation.class);
      if (parent.isPresent()) {
         parent.get().initHello(hello);
      }

      hello.add("lang", Sys.getLanguage());
      hello.add("timeZone", BTimeZone.getLocal().encodeToString());
      hello.add("hostId", Nre.getHostId());
      hello.add("vmUuid", Nre.vmUuid);

      try {
         hello.add("brandId", Brand.getBrandId());
      } catch (Exception var7) {
      }

      if (station != null) {
         BFacets sysInfo = station.getSysInfo();
         BogCodec.add(hello, "sysInfo", sysInfo, null);
         String realms = sysInfo.gets("realms", null);
         if (realms != null) {
            hello.add("realms", realms);
         }
      }
   }

   @Override
   public void sessionOpened(FoxSession session) {
      this.session = session;
      this.log.opened(session);
      this.setState("Connected");
      this.setLastConnectTime(Clock.time());
      this.getChannels().sessionOpened();

      try {
         this.remoteVersion = new Version(session.getRemoteHello().getString("app.version"));
      } catch (Exception var3) {
         this.log.error("Cannot retrieve version from remote hello", var3);
      }
   }

   @Override
   public void sessionClosed(FoxSession session, Throwable cause) {
      this.log.closed(session, cause);
      this.session = null;
      this.setState("Not connected");
      this.setLastDisconnectTime(Clock.time());
      this.setLastDisconnectCause(cause == null ? "" : cause.toString());
      this.getChannels().sessionClosed(cause);
   }

   @Override
   public FoxResponse process(FoxRequest req) throws Throwable {
      BFoxChannel channel = this.getChannel(req.channel);
      channel.checkProcess(req);
      FoxResponse resp = channel.fwProcess(req);
      return resp != BFoxChannel.UNHANDLED_RESPONSE ? resp : channel.process(req);
   }

   @Override
   public void circuitOpened(FoxCircuit circuit) throws Throwable {
      BFoxChannel channel = this.getChannel(circuit.channel);
      channel.checkProcessCircuit(circuit);
      if (!channel.fwCircuitOpened(circuit)) {
         channel.circuitOpened(circuit);
      }
   }

   private BFoxChannel getChannel(String channelName) {
      BFoxChannel channel = (BFoxChannel)this.getChannels().get(channelName);
      if (channel != null) {
         return channel;
      } else {
         BFoxChannel proto = (BFoxChannel)BFoxChannelRegistry.getPrototype().get(channelName);
         if (proto != null) {
            return this.getChannels().get(channelName, proto.getType());
         } else {
            throw new InvalidChannelException(channelName);
         }
      }
   }

   @Override
   public Thread makeThread(ThreadGroup group, Runnable runnable, String name) {
      return new Thread(group, runnable, name);
   }

   @Override
   public void error(String msg, Throwable cause) {
      this.log.error(msg, cause);
   }

   public FoxResponse sendSync(FoxRequest req) throws Exception {
      FoxSession session = this.session;
      if (session == null) {
         throw new NotConnectedException();
      } else {
         return session.sendSync(req);
      }
   }

   public void sendAsync(FoxRequest req) throws Exception {
      this.sendAsync(req, null);
   }

   public void sendAsync(FoxRequest req, FoxAsyncCallbacks cb) throws Exception {
      FoxSession session = this.session;
      if (session == null) {
         throw new NotConnectedException();
      } else {
         session.sendAsync(req, cb);
      }
   }

   public BIcon getIcon() {
      return icon;
   }

   public final Object fw(int x, Object a, Object b, Object c, Object d) {
      if (x == 803) {
         if (a instanceof Integer) {
            this.fwConnectionType = (Integer)a;
         }

         return this.fwConnectionType;
      } else {
         return super.fw(x, a, b, c, d);
      }
   }

   static {
      Spy.ROOT.add("fox", new FoxIndexSpy());
   }
}
