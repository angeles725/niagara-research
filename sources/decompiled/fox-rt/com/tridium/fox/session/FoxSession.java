package com.tridium.fox.session;

import com.tridium.authn.LoginFailureCause;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.MessageReader;
import com.tridium.fox.message.MessageWriter;
import com.tridium.fox.sys.BFoxServerConnection;
import com.tridium.fox.sys.BFoxService;
import com.tridium.nre.security.EncryptionAlgorithmBundle;
import com.tridium.nre.security.KeyDerivationAlgorithmBundle;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.NullAlgorithmBundle;
import com.tridium.nre.util.IPAddressUtil;
import com.tridium.session.NiagaraSession;
import com.tridium.session.SessionManager;
import com.tridium.util.ObjectUtil;
import com.tridium.util.ObjectUtil.NameContainer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.baja.license.Feature;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.security.AuditEvent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.user.BUser;
import javax.baja.util.Version;
import javax.net.ssl.SSLSocket;

public class FoxSession implements NiagaraSession {
   private static final AtomicInteger nextId = new AtomicInteger(0);
   static final Version FOX_VERSION_1_0_2 = new Version("1.0.2");
   private long frameSizeLimit = Long.MAX_VALUE;
   static final int FW_FOX_SESSION_ID = 801;
   static final int FW_STATION_FOX_SESSION_ID = 802;
   public static final Integer NON_FW_FOX_SESSION = -1;
   public static final Integer FW_FOX_SESSION = 801;
   public static final Integer FW_STATION_FOX_SESSION = 802;
   public static final String STARTING = "starting";
   public static final String RUNNING = "running";
   public static final String CLOSING = "closing";
   public static final String CLOSED = "closed";
   private static FoxMessage emptyMessage = new FoxMessage();
   private static Class<?> SSLSocketClass = null;
   public Object extra;
   public final Object sessionStateLock = new Object();
   private String id;
   private String superId;
   private final long connectTime;
   private final Socket socket;
   private final MessageReader in;
   private final MessageWriter out;
   private final boolean isServer;
   private String string;
   private final Object sendLock = new Object();
   private int localNextSequence;
   private int remoteNextSequence;
   private FoxMessage remoteHello;
   private String remoteId;
   private String remoteSuperId;
   private int readCount;
   private int writtenCount;
   private volatile long lastReadTicks;
   private volatile long lastWriteTicks;
   private AtomicBoolean isClosed = new AtomicBoolean(false);
   private Throwable closeCause;
   private String state = "<init>";
   private boolean autoLogoffDisabled = false;
   private final SessionSender sender;
   private final SessionReceiver receiver;
   private final SessionDispatcher dispatcher;
   private final SessionBedroom bedroom;
   private final SessionCircuits circuits;
   FoxConnection conn;
   FoxMessage remoteWelcome;
   public static final FoxSession.IFoxSessionListener[] nullListeners;
   private static volatile FoxSession.IFoxSessionListenerFactory[] listenerFactories;
   private volatile FoxSession.IFoxSessionListener[] listeners;
   public boolean promptForPasswordReset;
   BUser user;
   Context sessionContext;
   private final int legacyId;
   private boolean legacyConnection;
   public static final Version VERSION_4;
   public static final Version VERSION_4_1;
   public static final Version VERSION_4_2;
   public static final Version VERSION_4_3;
   public static final Version VERSION_4_4;
   public static final Version VERSION_4_13;
   private static String WB_APP_NAME;
   private static String STATION_APP_NAME;
   private static final Map<FoxSession, Map<String, Object>> cacheMap;
   private KeyDerivationAlgorithmBundle keyExchangeAlgorithmBundle;
   private EncryptionAlgorithmBundle encryptionAlgorithmBundle;
   private byte[] sessionKey;
   private static volatile String niagaraPlatformType;

   public FoxSession(Socket socket, FoxConnection conn) throws IOException {
      this(socket, conn, nullListeners);
      this.initListeners();
   }

   public FoxSession(Socket socket, FoxConnection conn, FoxSession.IFoxSessionListener[] listeners) throws IOException {
      this.listeners = nullListeners;
      this.promptForPasswordReset = true;
      this.legacyConnection = false;
      this.keyExchangeAlgorithmBundle = NullAlgorithmBundle.getInstance();
      this.encryptionAlgorithmBundle = null;

      try {
         this.id = SessionManager.generateSessionId(FoxSession.class, 25);
         this.legacyId = nextId();
         this.conn = conn;
         this.socket = socket;
         this.isServer = conn == null;
         if (this.isServer) {
            this.frameSizeLimit = Fox.getPreAuthFrameSizeLimit();
         }

         this.in = new MessageReader(new BufferedInputStream(socket.getInputStream()), socket.getRemoteSocketAddress().toString());
         this.connectTime = System.currentTimeMillis();
         this.out = new MessageWriter(new BufferedOutputStream(socket.getOutputStream()));
         this.localNextSequence = this.isServer ? 0 : 1;
         this.remoteNextSequence = this.isServer ? 1 : 0;
         this.sender = new SessionSender(this);
         this.receiver = new SessionReceiver(this);
         this.dispatcher = new SessionDispatcher(this);
         this.bedroom = new SessionBedroom();
         this.circuits = new SessionCircuits(this);
         this.lastReadTicks = Fox.clock.ticks();
         this.lastWriteTicks = Fox.clock.ticks();
         socket.setSoTimeout(Fox.soTimeout);
         socket.setTcpNoDelay(Fox.tcpNoDelay);
         StringBuilder s = new StringBuilder();
         s.append(this.id).append(": ").append(this.isServer ? "Server " : "Client ");
         if (!this.isServer) {
            s.append(socket.getLocalPort()).append("->");
         }

         s.append(this.getRemoteHost()).append(':').append(this.getRemotePort());
         this.string = s.toString();
         this.listeners = listeners;
      } catch (Throwable var5) {
         this.release();
         throw var5;
      }
   }

   private static int nextId() {
      return nextId.getAndIncrement();
   }

   public Map<String, String> getSessionInfo() {
      return new HashMap<>();
   }

   public String getId() {
      return this.id;
   }

   public String getSuperId() {
      return this.superId;
   }

   public void setSuperId(String superId) {
      NiagaraBasicPermission initPermission = new NiagaraBasicPermission("MODIFY_SESSION_IDS");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(initPermission);
      }

      this.superId = superId;
   }

   public String getRemoteHost() {
      return this.socket.getInetAddress().getHostAddress();
   }

   public long getCreationTime() {
      return this.connectTime;
   }

   public void invalidate(LoginFailureCause cause) {
      this.close(null, cause);
      SessionManager.removeSession(this);
   }

   public void invalidate() {
      this.invalidate(null);
   }

   public void doSetAuthenticated(BUser user) {
   }

   public String getAuditTarget() {
      FoxConnection connection = this.conn();
      if (connection instanceof BFoxServerConnection) {
         BFoxServerConnection serverConn = (BFoxServerConnection)connection;
         if (serverConn.getSpace() != null) {
            return serverConn.toPathString();
         } else {
            BFoxService foxService = (BFoxService)Sys.getService(BFoxService.TYPE);
            String prefixPath = foxService.getServerConnections().toPathString();
            String stationName = this.getRemoteHello() != null ? this.getRemoteHello().getString("station.name", null) : null;
            return stationName != null
               ? prefixPath + "/Station_" + stationName
               : prefixPath + '/' + ObjectUtil.generateUniqueSlotName("Session", (NameContainer)foxService.getServerConnections().fw(1));
         }
      } else {
         return connection.toString();
      }
   }

   public String getAuditOldValue(String operation) {
      return this.getRemoteHello().getString("app.name", "") + " " + this.getRemoteHello().getString("app.version", "");
   }

   public AuditEvent makeAuditEvent(String operation, BUser user) {
      Objects.requireNonNull(operation);
      Objects.requireNonNull(user);
      return BFoxService.auditConnection(operation, this)
         ? new AuditEvent(
            operation,
            this.getAuditTarget(),
            this.getAuditSlotName(),
            this.getAuditOldValue(operation),
            this.getAuditValue(operation, user),
            user.getUsername()
         )
         : null;
   }

   public boolean getAutoLogoffDisabled() {
      return this.autoLogoffDisabled;
   }

   public int getLegacyId() {
      return this.legacyId;
   }

   public boolean isLegacyConnection() {
      return this.legacyConnection;
   }

   public int getRemotePort() {
      return this.socket.getPort();
   }

   public final FoxMessage getRemoteHello() {
      return this.remoteHello;
   }

   public FoxMessage getRemoteWelcome() {
      return this.remoteWelcome;
   }

   public String getRemoteId() {
      return this.remoteId;
   }

   public String getRemoteSuperId() {
      return this.remoteSuperId;
   }

   public void setRemoteSuperId(String remoteSuperId) {
      this.remoteSuperId = remoteSuperId;
   }

   public FoxConnection conn() {
      return this.conn;
   }

   public boolean isServer() {
      return this.isServer;
   }

   public boolean isClosed() {
      return this.isClosed.get();
   }

   public long getConnectTime() {
      return this.connectTime;
   }

   public int getFramesReadCount() {
      return this.readCount;
   }

   public int getFramesWrittenCount() {
      return this.writtenCount;
   }

   public long getLastReadTicks() {
      return this.lastReadTicks;
   }

   public long getLastWriteTicks() {
      return this.lastWriteTicks;
   }

   public final String getState() {
      return this.state;
   }

   @Override
   public final String toString() {
      return this.string;
   }

   public final BUser getUser() {
      return this.user;
   }

   public final void setUser(BUser user) {
      this.user = user;
   }

   public final Context getSessionContext() {
      return this.sessionContext;
   }

   public final void setSessionContext(Context context) {
      this.sessionContext = context;
   }

   public final void start() {
      this.frameSizeLimit = Long.MAX_VALUE;
      this.initListeners();
      this.setState("starting");
      this.receiver.start();
      this.sender.start();
      this.dispatcher.start();
      this.conn.sessionOpened(this);
      this.setState("running");
   }

   public final void close(Throwable cause) {
      this.close(cause, null);
   }

   public final void close(Throwable cause, LoginFailureCause failureCause) {
      this.sender.sendClose(failureCause);
      if (!this.isClosed.getAndSet(true)) {
         this.setState("closing", cause);
         Fox.unregister(this);
         this.sender.kill();
         this.receiver.kill();
         this.dispatcher.kill();
         this.bedroom.wakeAll();
         this.circuits.kill();
         long startWait = Fox.clock.ticks();

         while (this.sender.isRunning() && Fox.clock.ticks() - startWait < Fox.sessionCloseTimeout) {
            synchronized (this.sessionStateLock) {
               try {
                  this.sessionStateLock.wait(200L);
               } catch (InterruptedException var10) {
               }
            }
         }

         try {
            this.socket.close();
         } catch (Exception var9) {
         }

         synchronized (this.sessionStateLock) {
            if (this.conn != null) {
               this.conn.sessionClosed(this, cause, failureCause);
            }
         }

         this.setState("closed", cause);
         this.listeners = nullListeners;
         this.user = null;
         this.sessionContext = null;
         Map<String, Object> map = cacheMap.remove(this);
         if (map != null) {
            map.values().forEach(obj -> {
               if (obj instanceof AutoCloseable) {
                  try {
                     ((AutoCloseable)obj).close();
                  } catch (Exception var2x) {
                  }
               }
            });
         }

         if (this.sessionKey != null) {
            SecurityUtil.zeroByteArray(this.sessionKey);
            this.sessionKey = null;
         }
      }
   }

   protected void release() {
      SessionManager.releaseSessionId(this.getClass(), this.getId());
   }

   public Object getSessionStateLock() {
      return this.sessionStateLock;
   }

   public boolean supportsKeyExchange() {
      return !this.isLegacyConnection() && !(this.keyExchangeAlgorithmBundle instanceof NullAlgorithmBundle);
   }

   public KeyDerivationAlgorithmBundle getKeyExchangeAlgorithmBundle() {
      return this.keyExchangeAlgorithmBundle;
   }

   public void setKeyExchangeAlgorithmBundle(KeyDerivationAlgorithmBundle bundle) {
      this.keyExchangeAlgorithmBundle = bundle;
   }

   public EncryptionAlgorithmBundle getEncryptionAlgorithmBundle() {
      return this.encryptionAlgorithmBundle;
   }

   public void setEncryptionAlgorithmBundle(EncryptionAlgorithmBundle encryptionAlgorithmBundle) {
      this.encryptionAlgorithmBundle = encryptionAlgorithmBundle;
   }

   public int getSharedKeySize() {
      return this.encryptionAlgorithmBundle != null ? this.encryptionAlgorithmBundle.getKeySize() : 128;
   }

   public boolean hasSessionKey() {
      return this.supportsKeyExchange() && this.sessionKey != null;
   }

   public byte[] makeSharedSecret(byte[] salt) {
      if (!this.hasSessionKey()) {
         return null;
      } else {
         try {
            MessageDigest digest = MessageDigest.getInstance("SHA512");
            digest.update(salt, 0, salt.length);
            digest.update(this.sessionKey, 0, this.sessionKey.length);
            return digest.digest();
         } catch (NoSuchAlgorithmException var3) {
            throw new SecurityException("Reguired MessageDigest algorithm \"SHA512\" not implemented by supported Security Providers.", var3);
         }
      }
   }

   public void setSessionKey(byte[] key) throws IOException {
      if (!this.supportsKeyExchange()) {
         throw new IOException("Session does not support key exchange");
      } else {
         NiagaraBasicPermission setSessionKeyPermission = new NiagaraBasicPermission("SET_FOX_SESSION_KEY");
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(setSessionKeyPermission);
         }

         this.sessionKey = key;
      }
   }

   public final boolean isSecure() {
      return this.socket instanceof SSLSocket;
   }

   public final boolean supportsSecureData() {
      return this.isSecure() || this.supportsKeyExchange();
   }

   final void sendHello(String username, boolean includeLegacyAttributes) throws Exception {
      this.sendHello(username, null, includeLegacyAttributes);
   }

   final void sendHello(String username, String requestedId, boolean includeLegacyAttributes) throws Exception {
      FoxMessage msg = this.initHello(null, username, this.id, requestedId, this.legacyId, includeLegacyAttributes);
      this.sendTuning("hello", msg);
   }

   void sendRedirect(int port) throws Exception {
      FoxMessage msg = new FoxMessage();
      msg.add("port", port);
      this.sendTuning("redirect", msg);
   }

   final FoxMessage initHello(FoxMessage message, String username, String id, String requestedId, int legacyId, boolean includeLegacyAttributes) throws Exception {
      FoxMessage msg = new FoxMessage();
      msg.add("fox.version", "1.0.2");
      msg.add("id", legacyId);
      msg.add("n4Id", id);
      msg.add("n4SuperId", this.superId);
      if (includeLegacyAttributes) {
         if (requestedId != null) {
            msg.add("requestedId", requestedId);
         }

         if (username != null) {
            msg.add("user", username);
         }
      }

      msg.add("hostName", Fox.hostName);
      if (this.getRemoteHello() != null) {
         String hostAddress = this.getRemoteHello().getString("hostAddress", null);
         if (hostAddress != null) {
            InetAddress hint = IPAddressUtil.numericStringToInetAddress(hostAddress);
            if (hint != null) {
               msg.add("hostAddress", IPAddressUtil.removeScopeSpec(Sys.getLocalHost(hint).getHostAddress()));
            }
         }
      }

      if (msg.getString("hostAddress", null) == null) {
         msg.add("hostAddress", Fox.hostAddress);
      }

      msg.add("app.name", Fox.appName);
      if (includeLegacyAttributes) {
         msg.add("app.version", Fox.appVersion);
         msg.add("vm.name", Fox.vmName);
         msg.add("vm.version", Fox.vmVersion);
         msg.add("os.name", Fox.osName);
         msg.add("os.version", Fox.osVersion);
         this.conn.initHello(msg);
         msg.remove("niagaraPlatformType");
         String platformType = getNiagaraPlatformType();
         if (!platformType.isEmpty()) {
            msg.add("niagaraPlatformType", platformType);
         }
      }

      return msg;
   }

   static String getNiagaraPlatformType() {
      if (niagaraPlatformType == null) {
         try {
            Feature f = Sys.getLicenseManager().checkFeature("tridium", "platform");
            niagaraPlatformType = f.get("type", "");
         } catch (Exception var1) {
            niagaraPlatformType = "";
         }
      }

      return niagaraPlatformType;
   }

   final void receiveHello() throws Exception {
      FoxFrame frame = this.readFrame();
      if (!Objects.equals(frame.channel, "fox")) {
         throw new InvalidChannelException("Expecting 'fox', not '" + frame.channel + "'");
      } else if (!Objects.equals(frame.command, "hello")) {
         if (Objects.equals(frame.command, "redirect")) {
            throw new FoxsRedirectException(frame.message.getInt("port"));
         } else if (Objects.equals(frame.command, "busy")) {
            throw new FoxBusyException();
         } else {
            throw new InvalidCommandException("Expecting 'hello', not '" + frame.command + "'");
         }
      } else if (frame.replyNumber != -1) {
         throw new IOException("Invalid reply number " + frame.replyNumber);
      } else {
         FoxMessage msg = this.remoteHello = frame.message;
         String ver = msg.getString("fox.version");
         if (!ver.startsWith("1.0")) {
            throw new IOException("Unsupported fox.version '" + ver + "'");
         } else if (msg.getString("exception", null) != null) {
            throw Fox.exceptionTranslator.messageToException(msg);
         } else {
            try {
               this.remoteId = msg.getString("n4Id");
               this.remoteSuperId = msg.getString("n4SuperId", null);
            } catch (IOException var5) {
               this.legacyConnection = true;
               this.remoteId = Integer.toString(msg.getInt("id"));
            }

            if (ver != null && new Version(ver).compareTo(FOX_VERSION_1_0_2) < 0) {
               if (Fox.appName.equals(WB_APP_NAME)
                  && msg.getString("app.name", "").equals(STATION_APP_NAME)
                  && new Version(msg.getString("app.version", "0")).compareTo(VERSION_4) < 0) {
                  throw new IncompatibleVersionException("NiagaraAX Station Connections Unsupported");
               }

               if (Fox.appName.equals(STATION_APP_NAME)
                  && msg.getString("app.name", "").equals(WB_APP_NAME)
                  && new Version(msg.getString("app.version", "0")).compareTo(VERSION_4) < 0) {
                  throw new IncompatibleVersionException("Niagara4 Station Connections Unsupported");
               }
            }

            if (Fox.appName.equals(STATION_APP_NAME) && msg.getString("app.name", "").equals(STATION_APP_NAME)) {
               this.autoLogoffDisabled = true;
            }
         }
      }
   }

   public final void sendTuning(String command, FoxMessage msg) throws Exception {
      this.writeFrame(new FoxFrame(97, this.localNextSequence++, -1, "fox", command, msg));
   }

   void sendBusy() throws Exception {
      this.writeFrame(new FoxFrame(101, this.localNextSequence++, -1, "fox", "busy", new FoxMessage()));
   }

   public final FoxMessage receiveTuning(String command) throws Exception {
      FoxFrame frame = this.readFrame();
      if (!Objects.equals(frame.channel, "fox")) {
         throw new InvalidChannelException("Expecting 'fox', not '" + frame.channel + "'");
      } else if (command != null && !Objects.equals(frame.command, command)) {
         boolean hasMessage = frame.message != null;
         String failReason = null;
         if (hasMessage) {
            try {
               FoxMessage dataMessage = frame.message.getMessage("data");
               if (dataMessage != null) {
                  failReason = dataMessage.getString("authfail_reason");
               }
            } catch (IOException var6) {
            }

            if (failReason != null && failReason.equals("User Lockout")) {
               throw new FoxUserLockoutException(this);
            }
         }

         throw new InvalidCommandException("Expecting '" + command + "', not '" + frame.command + "'");
      } else {
         return frame.message;
      }
   }

   public final FoxCircuit openCircuit(String channel, String command) throws Exception {
      return this.openCircuit(channel, command, null);
   }

   public final FoxCircuit openCircuit(String channel, String command, FoxMessage metadata) throws Exception {
      FoxCircuit circuit = this.circuits.alloc(channel, command, metadata);

      try {
         circuit.sendOpen();
         return circuit;
      } catch (Exception var6) {
         this.closeCircuit(circuit);
         throw var6;
      }
   }

   void closeCircuit(FoxCircuit circuit) {
      try {
         circuit.sendClose();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      this.circuits.free(circuit);
   }

   public final FoxResponse sendSync(FoxRequest req) throws Exception {
      if (this.isClosed.get()) {
         throw new IOException("Session closed");
      } else {
         SessionBedroom.Bed bed = this.bedroom.getBed(Thread.currentThread());
         this.send(115, bed.replyNumber, req.channel, req.command, req, null);
         this.bedroom.sleep(bed);
         FoxFrame reply = bed.reply;
         if (reply == null) {
            IOException e = new IOException("Request timed out: " + req.channel + "." + req.command);
            this.close(e);
            throw e;
         } else if (reply.frameType == 114) {
            return (FoxResponse)reply.message;
         } else if (reply.frameType == 110) {
            return null;
         } else if (bed.reply.frameType == 101) {
            throw Fox.exceptionTranslator.messageToException(reply.message);
         } else {
            IOException e = new IOException("Invalid reply frame type: " + (char)reply.frameType);
            this.close(e);
            throw e;
         }
      }
   }

   public final void sendAsync(FoxRequest req) throws Exception {
      this.sendAsync(req, null);
   }

   public final void sendAsync(FoxRequest req, FoxAsyncCallbacks callbacks) throws Exception {
      if (this.isClosed.get()) {
         throw new IOException("Session closed");
      } else {
         this.send(97, -1, req.channel, req.command, req, callbacks);
      }
   }

   final void sendReply(FoxFrame req, FoxMessage reply) throws InterruptedException {
      if (req.replyNumber != -1) {
         if (reply == null) {
            this.send(110, req.replyNumber, req.channel, req.command, emptyMessage, null);
         } else {
            this.send(114, req.replyNumber, req.channel, req.command, reply, null);
         }
      }
   }

   final void sendError(FoxFrame req, Throwable exception) throws InterruptedException {
      if (req != null && req.replyNumber != -1) {
         FoxMessage reply = Fox.exceptionTranslator.exceptionToMessage(exception);
         this.send(101, req.replyNumber, req.channel, req.command, reply, null);
      }
   }

   private void send(int type, int replyNo, String p, String c, FoxMessage msg, FoxAsyncCallbacks cb) throws InterruptedException {
      synchronized (this.sendLock) {
         int seqNo = this.localNextSequence;
         this.localNextSequence = (this.localNextSequence + 1) % Integer.MAX_VALUE;
         FoxFrame frame = new FoxFrame(type, seqNo, replyNo, p, c, msg);
         frame.callbacks = cb;
         this.sender.enqueue(frame);
      }
   }

   void requestReceived(FoxFrame request) throws InterruptedException {
      try {
         if (Objects.equals(request.channel, "circuit")) {
            this.circuits.circuitRequestReceived(request);
         } else if (Objects.equals(request.channel, "fox") && Objects.equals(request.command, "close")) {
            this.processFoxChannelRequest((FoxRequest)request.message);
         } else {
            this.dispatcher.enqueue(request);
         }
      } catch (Throwable var3) {
         this.conn.error("Error in request", var3);
         var3.printStackTrace();
         request.dump();
         this.sendError(request, var3);
      }
   }

   void replyReceived(FoxFrame reply) {
      try {
         this.bedroom.wake(reply);
      } catch (Throwable var3) {
         if (!this.isClosed.get()) {
            this.conn.error("Error in reply " + reply.replyNumber, var3);
            var3.printStackTrace();
            reply.dump();
         }
      }
   }

   FoxResponse processFoxChannelRequest(FoxRequest req) {
      String command = req.command;
      if (Objects.equals(command, "ping")) {
         return this.ping(req);
      } else if (Objects.equals(command, "close")) {
         try {
            String causeName = req.getString("cause");
            LoginFailureCause cause = LoginFailureCause.valueOf(causeName);
            this.close(null, cause);
         } catch (IOException var5) {
         }

         return null;
      } else {
         throw new InvalidCommandException("fox." + command);
      }
   }

   public void ping() throws Exception {
      this.sendSync(new FoxRequest("fox", "ping"));
   }

   private FoxResponse ping(FoxRequest req) {
      return new FoxResponse(req);
   }

   public final void setState(String state) {
      this.setState(state, null);
   }

   public final void setState(String state, Throwable throwable) {
      this.state = state;
      if (Fox.traceSessionStates) {
         System.out.println("-- Fox [" + SecurityUtil.calculateSessionIdHash(this.id) + "] STATE " + state);
      }

      if (this.listeners != nullListeners) {
         for (FoxSession.IFoxSessionListener l : this.listeners) {
            l.stateChanged(this, state, throwable);
         }
      }
   }

   public void spy(PrintWriter out) throws Exception {
      out.print("<table border='0' cellspacing='0'>");
      out.print("<tr><th colspan='2' bgcolor='#d0d0d0'>");
      out.print(this.string);
      out.print("</th></tr>\n");
      this.dumpProps(out);
      out.print("</table>\n");
   }

   public void dumpProps(PrintWriter out) throws Exception {
      this.dump(out, "id", this.id);
      this.dump(out, "state", this.state);
      this.dump(out, "connectTime", new Date(this.connectTime));
      this.dump(out, "remoteId", this.remoteId);
      this.dump(out, "readCount", this.readCount);
      this.dump(out, "lastRead", Fox.clock.ticks() - this.lastReadTicks + "ms");
      this.dump(out, "writtenCount", this.writtenCount);
      this.dump(out, "lastWrite", Fox.clock.ticks() - this.lastWriteTicks + "ms");
      this.dump(out, "isClosed", this.isClosed.get());
      this.dump(out, "closeCause", this.closeCause);
      this.dump(out, "socket", this.socket);
      this.dump(out, "sender", this.sender);
      this.dump(out, "receiver", this.receiver);
      this.dump(out, "dispatcher", this.dispatcher);
      this.dump(out, "bedroom", this.bedroom);
      this.dump(out, "circuits", "<pre>" + this.circuits + "</pre>");
      if (this.remoteHello != null) {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         bout.write("<pre>".getBytes());
         this.remoteHello.dump(bout);
         bout.write("</pre>".getBytes());
         this.dump(out, "remoteHello", new String(bout.toByteArray()));
      }

      if (this.remoteWelcome != null) {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         bout.write("<pre>".getBytes());
         this.remoteWelcome.dump(bout);
         bout.write("</pre>".getBytes());
         this.dump(out, "remoteWelcome", new String(bout.toByteArray()));
      }

      this.dump(out, "fwConnType", this.fwConnType(null));
   }

   private void dump(PrintWriter out, Object name, boolean value) {
      this.dump(out, name, String.valueOf(value));
   }

   private void dump(PrintWriter out, Object name, int value) {
      this.dump(out, name, String.valueOf(value));
   }

   private void dump(PrintWriter out, Object name, Object value) {
      out.print("<tr><th align='left'>");
      out.print(name);
      out.print("</th><td align='left' nowrap='true'>");
      out.print(value);
      out.print("</td></tr>\n");
   }

   public void dump() {
      System.out.println("id:           " + this.id);
      System.out.println("state:        " + this.state);
      System.out.println("connectTime:  " + new Date(this.connectTime));
      System.out.println("remoteId:     " + this.remoteId);
      System.out.println("readCount:    " + this.readCount);
      System.out.println("lastRead:     " + (Fox.clock.ticks() - this.lastReadTicks) + "ms");
      System.out.println("writtenCount: " + this.writtenCount);
      System.out.println("lastWrite:    " + (Fox.clock.ticks() - this.lastWriteTicks) + "ms");
      System.out.println("isClosed:     " + this.isClosed.get());
      System.out.println("closeCause:   " + this.closeCause);
      System.out.println("socket:       " + this.socket);
      System.out.println("sender:       " + this.sender);
      System.out.println("receiver:     " + this.receiver);
      System.out.println("dispatcher:   " + this.dispatcher);
      System.out.println("bedroom:      " + this.bedroom);
      System.out.println("circuits:     " + this.circuits);
      System.out.println("fwConnType    " + this.fwConnType(null));
   }

   FoxFrame readFrame() throws IOException {
      FoxFrame f;
      do {
         this.in.setReadLimit(this.frameSizeLimit);
         f = FoxFrame.read(this.in);
         this.readCount++;
         this.lastReadTicks = Fox.clock.ticks();
      } while (f.frameType == 107);

      if (f.frameType != 99) {
         if (f.sequenceNumber != this.remoteNextSequence) {
            throw new IOException("Invalid sequence number " + f.sequenceNumber + " expecting " + this.remoteNextSequence);
         }

         this.remoteNextSequence = (this.remoteNextSequence + 1) % Integer.MAX_VALUE;
      }

      if (Fox.traceReadFrame) {
         System.out.print("-- Fox [" + SecurityUtil.calculateSessionIdHash(this.id) + "] READ (" + this.in.getBytesRead() + " bytes) ");
         f.dump();
      }

      if (this.listeners != nullListeners) {
         FoxFrame lsFrame = f;
         if (Objects.equals(f.channel, "fox") && Objects.equals(f.command, "login")) {
            lsFrame = new FoxFrame(f.frameType, f.sequenceNumber, f.replyNumber, f.channel, f.command, emptyMessage);
         }

         for (FoxSession.IFoxSessionListener l : this.listeners) {
            l.readFrame(this, lsFrame);
         }
      }

      return f;
   }

   void writeFrame(FoxFrame frame) throws IOException {
      if (Fox.traceWriteFrame) {
         System.out.print("-- Fox [" + SecurityUtil.calculateSessionIdHash(this.id) + "] WRITE ");
         frame.dump();
      }

      if (this.listeners != nullListeners) {
         FoxFrame lsFrame = frame;
         if (Objects.equals(frame.channel, "fox") && Objects.equals(frame.command, "login")) {
            lsFrame = new FoxFrame(frame.frameType, frame.sequenceNumber, frame.replyNumber, frame.channel, frame.command, emptyMessage);
         }

         for (FoxSession.IFoxSessionListener l : this.listeners) {
            l.writeFrame(this, lsFrame);
         }
      }

      this.writtenCount++;
      this.lastWriteTicks = Fox.clock.ticks();
      frame.write(this.out);
      this.out.flush();
      if (frame.callbacks != null) {
         frame.callbacks.asyncMessageSent(this, frame.message);
      }
   }

   private Integer fwConnType(Integer newVal) {
      if (this.conn != null) {
         try {
            if (this.conn instanceof BObject) {
               Object fwConnType = ((BObject)this.conn).fw(803, newVal, null, null, null);
               if (fwConnType instanceof Integer) {
                  return (Integer)fwConnType;
               }
            }
         } catch (Throwable var3) {
            var3.printStackTrace();
         }
      }

      return NON_FW_FOX_SESSION;
   }

   public static void registerListenerFactory(FoxSession.IFoxSessionListenerFactory factory) {
      synchronized (FoxSession.class) {
         Array<FoxSession.IFoxSessionListenerFactory> lsArray = new Array(FoxSession.IFoxSessionListenerFactory.class);
         lsArray.addAll(listenerFactories);
         if (!lsArray.contains(factory)) {
            lsArray.add(factory);
            listenerFactories = (FoxSession.IFoxSessionListenerFactory[])lsArray.trim();
         }
      }

      for (FoxSession currentSession : Fox.getSessions()) {
         currentSession.initListeners();
      }
   }

   public static void unregisterListenerFactory(FoxSession.IFoxSessionListenerFactory factory) {
      synchronized (FoxSession.class) {
         Array<FoxSession.IFoxSessionListenerFactory> lsArray = new Array(FoxSession.IFoxSessionListenerFactory.class);
         lsArray.addAll(listenerFactories);
         if (lsArray.remove(factory)) {
            listenerFactories = (FoxSession.IFoxSessionListenerFactory[])lsArray.trim();
         }
      }

      for (FoxSession currentSession : Fox.getSessions()) {
         currentSession.initListeners();
      }
   }

   private void initListeners() {
      this.listeners = createListeners(this.conn);
   }

   public static FoxSession.IFoxSessionListener[] createListeners(FoxConnection conn) {
      FoxSession.IFoxSessionListener[] newListeners = nullListeners;
      if (listenerFactories.length > 0) {
         Array<FoxSession.IFoxSessionListener> lsArray = null;

         for (FoxSession.IFoxSessionListenerFactory factory : listenerFactories) {
            FoxSession.IFoxSessionListener listener = factory.make(conn);
            if (listener != null) {
               if (lsArray == null) {
                  lsArray = new Array(FoxSession.IFoxSessionListener.class);
               }

               lsArray.add(listener);
            }
         }

         if (lsArray != null) {
            newListeners = (FoxSession.IFoxSessionListener[])lsArray.trim();
         }
      }

      return newListeners;
   }

   public Socket getSocket() {
      return this.socket;
   }

   public final <T> T getFromCache(String key, Function<String, T> mappingFunc) {
      return (T)cacheMap.computeIfAbsent(this, v -> new ConcurrentHashMap<>()).computeIfAbsent(key, mappingFunc);
   }

   public final <T> Optional<T> getFromCache(String key) {
      Map<String, Object> map = cacheMap.get(this);
      return map == null ? Optional.empty() : Optional.ofNullable((T)map.get(key));
   }

   public final void clearFromCache(String key) {
      Map<String, Object> map = cacheMap.get(this);
      if (map != null) {
         Object val = map.remove(key);
         if (map.isEmpty()) {
            cacheMap.remove(this);
         }

         if (val != null && val instanceof AutoCloseable) {
            try {
               ((AutoCloseable)val).close();
            } catch (Exception var5) {
            }
         }
      }
   }

   static {
      try {
         SSLSocketClass = Class.forName("javax.net.ssl.SSLSocket");
      } catch (Exception var1) {
      }

      nullListeners = new FoxSession.IFoxSessionListener[0];
      listenerFactories = new FoxSession.IFoxSessionListenerFactory[0];
      VERSION_4 = new Version("4.0.0");
      VERSION_4_1 = new Version("4.1.0");
      VERSION_4_2 = new Version("4.2.0");
      VERSION_4_3 = new Version("4.3.0");
      VERSION_4_4 = new Version("4.4.0");
      VERSION_4_13 = new Version("4.13.0");
      WB_APP_NAME = "Workbench";
      STATION_APP_NAME = "Station";
      cacheMap = new ConcurrentHashMap<>();
      niagaraPlatformType = null;
   }

   public interface IFoxSessionListener {
      void connectionAborted(String var1, Throwable var2);

      void stateChanged(FoxSession var1, String var2, Throwable var3);

      void readFrame(FoxSession var1, FoxFrame var2);

      void writeFrame(FoxSession var1, FoxFrame var2);
   }

   public interface IFoxSessionListenerFactory {
      FoxSession.IFoxSessionListener make(FoxConnection var1);
   }
}
