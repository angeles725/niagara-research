package com.tridium.fox.sys;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.FoxString;
import com.tridium.fox.message.FoxTuple;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.session.ServerException;
import com.tridium.fox.sys.broker.BBrokerChannel;
import com.tridium.fox.sys.broker.BFoxComponentSpace;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import com.tridium.space.BIGatewaySpace;
import com.tridium.sys.Nre;
import com.tridium.sys.station.Station;
import com.tridium.sys.station.Station.Message;
import com.tridium.sys.station.Station.RemoteListener;
import com.tridium.util.NiagaraRpcUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BFileSystem;
import javax.baja.file.BScopedFileSpace;
import javax.baja.io.ValueDocDecoder;
import javax.baja.io.ValueDocEncoder;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nav.BNavRoot;
import javax.baja.nav.NavEvent;
import javax.baja.nav.NavListener;
import javax.baja.net.NotConnectedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.rpc.TransportType;
import javax.baja.security.BIProtected;
import javax.baja.space.BComponentSpace;
import javax.baja.space.BSpace;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;
import javax.baja.virtual.BVirtualComponentSpace;
import javax.baja.virtual.BVirtualGateway;

@NiagaraType
public class BSysChannel extends BFoxChannel implements NavListener, RemoteListener {
   public static final Type TYPE = Sys.loadType(BSysChannel.class);
   static byte[] noPayload = new byte[0];
   private Type[] stationMixIns = new Type[0];
   private static final Logger niagaraRpcLog = Logger.getLogger("niagaraRpc");
   private static final String javaVmName = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("java.vm.name")));
   private static final String javaVmVersion = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("java.vm.version")));
   private static final String osArch = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("os.arch")));
   private static final String osName = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("os.name")));
   private static final String osVersion = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("os.version")));
   public static final Version VER_4_14 = new Version("4.14");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BSysChannel() {
      super("sys");
   }

   @Override
   protected boolean allowRoutingRequestToReachableStation(FoxRequest req) {
      return "stationCall".equals(req.command);
   }

   @Override
   protected Version getMinVersionAlongRouteForRequest(FoxRequest req) {
      return VER_4_14;
   }

   @Override
   public void checkProcess(FoxRequest req) throws Throwable {
   }

   @Override
   public FoxResponse process(FoxRequest request) throws Exception {
      String command = request.command;
      if (command == "navEvent") {
         return this.navEvent(request);
      } else if (command == "summary") {
         return this.summary(request);
      } else if (command == "stationCall") {
         return this.stationCall(request);
      } else if (command == "stationEvent") {
         return this.stationEvent(request);
      } else if (command == "listLocalSpaces") {
         return this.listLocalSpaces(request);
      } else if (command == "makeBrokerChannel") {
         return this.makeBrokerChannel(request);
      } else if (command == "subNavEvents") {
         return this.subscribeNavEvents(request);
      } else if (command == "unsubNavEvents") {
         return this.unsubscribeNavEvents(request);
      } else if (command == "niagaraRpc") {
         return this.niagaraRpc(request);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   @Override
   public void sessionClosed(Throwable cause) throws Exception {
      BNavRoot.INSTANCE.removeNavListener(this);
      Station.removeRemoteListener(this);
   }

   public String[][] summary() throws Exception {
      FoxRequest req = this.makeRequest("summary");
      if (this.isTraceOn()) {
         this.trace("c:summary");
      }

      FoxResponse resp = this.sendSync(req);
      String[][] pairs = new String[resp.count][];

      for (int i = 0; i < resp.count; i++) {
         FoxString msg = (FoxString)resp.tuples[i];
         pairs[i] = new String[]{msg.name, msg.value};
      }

      return pairs;
   }

   public FoxResponse summary(FoxRequest req) throws Exception {
      if (this.isTraceOn()) {
         this.trace("s:listLocalSpaces");
      }

      Context cx = this.getSessionContext();
      FoxResponse resp = new FoxResponse(req);
      resp.add("stationName", "" + Sys.getStation().getStationName());
      resp.add("host", "" + Sys.getLocalHost(null));
      resp.add("hostModel", "" + Nre.getHostModel());
      resp.add("hostModelVersion", "" + Nre.getHostModelVersion());
      resp.add("product", "" + Nre.getHostProduct());
      resp.add("hostId", "" + Nre.getHostId());
      resp.add("niagaraVersion", "" + Sys.getBajaVersion());
      resp.add("javaVersion", "" + javaVmName + " " + javaVmVersion);
      resp.add("osVersion", "" + osArch + " " + osName + " " + osVersion);
      resp.add("locale", "" + Locale.getDefault());
      resp.add("currentTime", "" + BAbsTime.make().toString(cx));
      return resp;
   }

   public byte[] stationCall(String id, byte[] payload, String... reachableStations) throws Exception {
      if (payload == null) {
         payload = noPayload;
      }

      FoxRequest req = this.makeRequest("stationCall");
      req.add("id", id);
      req.add("payload", payload);
      if (this.isTraceOn()) {
         this.trace("c:stationCall " + id);
      }

      if (reachableStations == null || reachableStations.length != 0) {
         req = (FoxRequest)makeFoxMessageWithReachableStationRoute(this, req, VER_4_14, reachableStations);
      }

      FoxResponse resp = this.sendSync(req);
      return resp.getBlob("payload");
   }

   public FoxResponse stationCall(FoxRequest req) throws Exception {
      String id = req.getString("id");
      byte[] payload = req.getBlob("payload");
      if (this.isTraceOn()) {
         this.trace("s:stationCall " + id);
      }

      Message msg = Station.remoteCall(new Message(id, payload));
      if (msg != null) {
         payload = msg.payload;
      }

      if (payload == null) {
         payload = noPayload;
      }

      FoxResponse resp = new FoxResponse(req);
      resp.add("payload", payload);
      return resp;
   }

   public void stationEvent(Message event) throws Exception {
      FoxRequest req = this.makeRequest("stationEvent");
      req.add("id", event.id);
      req.add("payload", event.payload);
      if (this.isTraceOn()) {
         this.trace("c:stationEvent " + event.id);
      }

      this.sendAsync(req);
   }

   public FoxResponse stationEvent(FoxRequest req) throws Exception {
      String id = req.getString("id");
      byte[] payload = req.getBlob("payload");
      if (this.isTraceOn()) {
         this.trace("s:stationEvent " + id);
      }

      if (id.equals("stationFault")) {
         String text = null;
         if (payload.length > 0) {
            text = Lexicon.make("baja").getText(new String(payload));
         }

         this.getFoxSession().stationFault = text;
      } else if (id.equals("mixIns")) {
         this.stationMixIns = Station.decodeMixIns(payload);
      }

      return null;
   }

   public Type[] getStationMixIns() {
      return (Type[])this.stationMixIns.clone();
   }

   public HashMap<String, String> listLocalSpaces() throws Exception {
      FoxRequest req = this.makeRequest("listLocalSpaces");
      if (this.isTraceOn()) {
         this.trace("c:listLocalSpaces");
      }

      FoxResponse resp = this.sendSync(req);
      FoxTuple[] msgs = resp.list("space");
      String[] schemes = new String[msgs.length];
      HashMap<String, String> schemeIds = new HashMap<>();

      for (int i = 0; i < msgs.length; i++) {
         FoxMessage msg = (FoxMessage)msgs[i];
         String schemeId = msg.getString("schemeId");
         schemeIds.put(schemeId, schemeId);
      }

      return schemeIds;
   }

   public FoxResponse listLocalSpaces(FoxRequest req) throws Exception {
      if (this.isTraceOn()) {
         this.trace("s:listLocalSpaces");
      }

      FoxResponse resp = new FoxResponse(req);
      BINavNode[] roots = BLocalHost.INSTANCE.getNavChildren();

      for (int i = 0; i < roots.length; i++) {
         BINavNode root = roots[i];
         if (root instanceof BSpace) {
            BSpace space = (BSpace)root;
            if (space instanceof BIProtected) {
               BIProtected protectedSpace = (BIProtected)space;
               if (protectedSpace == BFileSystem.INSTANCE) {
                  protectedSpace = BScopedFileSpace.STATION_HOME;
               }

               if (!this.getPermissionsFor(protectedSpace).hasOperatorRead()) {
                  if (space.getNavName().equals("station")) {
                     String[] lexiconArgs = new String[1];
                     if (this.getConnection() instanceof BFoxServerConnection) {
                        lexiconArgs[0] = this.getServerConnection().getUser().getUsername();
                     } else {
                        lexiconArgs[0] = this.getClientConnection().getCredentials().getUsername();
                     }

                     throw new LocalizableRuntimeException("fox", "error.NoPermissionForStation", lexiconArgs);
                  }
                  continue;
               }
            }

            FoxMessage msg = new FoxMessage("space");
            msg.add("schemeId", space.getNavName());
            msg.add("type", space.getType().toString());
            resp.add(msg);
         }
      }

      return resp;
   }

   public BBrokerChannel makeBrokerChannel(BFoxComponentSpace space) throws Exception {
      FoxRequest req = this.makeRequest("makeBrokerChannel");
      req.add("ord", "" + space.getOrdInSession());
      if (this.isTraceOn()) {
         this.trace("c:makebrokerChannel");
      }

      FoxResponse resp = this.sendSync(req);
      String channelName = resp.getString("channelName");
      BFoxChannelRegistry registry = this.getConnection().getChannels();
      BBrokerChannel channel = (BBrokerChannel)registry.get(channelName, BBrokerChannel.TYPE);
      channel.initClient(space);
      return channel;
   }

   public FoxResponse makeBrokerChannel(FoxRequest req) throws Exception {
      BOrd ord = BOrd.make(req.getString("ord"));
      BComponentSpace space = (BComponentSpace)ord.get();
      String channelName;
      if (space instanceof BVirtualComponentSpace) {
         BVirtualGateway gateway = ((BVirtualComponentSpace)space).getVirtualGateway();
         channelName = "virt_" + gateway.getHandle();
      } else if (space instanceof BIGatewaySpace) {
         channelName = "gw_" + ((BIGatewaySpace)space).getGateway().getHandle();
      } else {
         channelName = space.getNavName();
      }

      BFoxChannelRegistry registry = this.getConnection().getChannels();
      BBrokerChannel channel = (BBrokerChannel)registry.get(channelName, BBrokerChannel.TYPE);
      channel.initServer(space);
      if (this.isTraceOn()) {
         this.trace("s:makebrokerChannel " + ord + " -> " + channelName);
      }

      FoxResponse resp = new FoxResponse(req);
      resp.add("channelName", channelName);
      return resp;
   }

   public void subscribeNavEvents() throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:subNavEvents");
      }

      this.sendAsync(this.makeRequest("subNavEvents"));
   }

   private FoxResponse subscribeNavEvents(FoxRequest req) {
      if (this.isTraceOn()) {
         this.trace("s:subNavEvents");
      }

      BNavRoot.INSTANCE.addNavListener(this);
      Station.addRemoteListener(this);
      Station.broadcastStationFault();
      Station.broadcastStationMixIns();
      return null;
   }

   public void unsubscribeNavEvents() throws Exception {
      if (this.isTraceOn()) {
         this.trace("c:unsubNavEvents");
      }

      try {
         this.sendAsync(this.makeRequest("unsubNavEvents"));
      } catch (NotConnectedException var2) {
      }
   }

   private FoxResponse unsubscribeNavEvents(FoxRequest req) {
      if (this.isTraceOn()) {
         this.trace("s:unsubNavEvents");
      }

      BNavRoot.INSTANCE.removeNavListener(this);
      Station.removeRemoteListener(this);
      return null;
   }

   public void navEvent(NavEvent event) {
      try {
         if (this.isTraceOn()) {
            this.trace("c:navEvent " + event);
         }

         String ord = event.getParentOrd().toString();
         if (!ord.startsWith("local:")) {
            return;
         }

         if (ord.startsWith("local:|fox:") || ord.startsWith("local:|foxs:")) {
            return;
         }

         if (event.getParent() instanceof BComponent) {
            return;
         }

         int pipe = ord.indexOf(124);
         if (pipe < 0) {
            return;
         }

         String relOrd = ord.substring(pipe + 1);
         FoxRequest req = this.makeRequest("navEvent");
         req.add("ord", relOrd);
         switch (event.getId()) {
            case 1:
               req.add("id", "a");
               req.add("n", event.getNewChildName());
               break;
            case 2:
               req.add("id", "v");
               req.add("o", event.getOldChildName());
               break;
            case 3:
               req.add("id", "r");
               req.add("o", event.getOldChildName());
               req.add("n", event.getNewChildName());
               break;
            case 4:
               StringBuilder o = new StringBuilder();
               String[] names = event.getNewOrder();

               for (int i = 0; i < names.length; i++) {
                  o.append(names[i]).append('|');
               }

               req.add("id", "o");
               req.add("o", o.toString());
               break;
            case 5:
               req.add("id", "p");
               req.add("o", event.getOldChildName());
               break;
            case 6:
               req.add("id", "c");
               req.add("o", event.getOldChildName());
               req.add("n", event.getNewChildName());
               break;
            default:
               throw new IllegalStateException(event.toString());
         }

         this.sendAsync(req);
      } catch (Exception var9) {
         if (this.getConnection().isConnected()) {
            var9.printStackTrace();
         }
      }
   }

   private FoxResponse navEvent(FoxRequest req) throws Exception {
      String relOrd = req.getString("ord");
      BOrd parentOrd = BOrd.make(this.getFoxSession().getAbsoluteOrd(), relOrd);
      String id = req.getString("id");
      if (this.isTraceOn()) {
         this.trace("s:navEvent " + SecurityUtil.calculateSessionIdHash(id) + " " + parentOrd);
      }

      NavEvent event = null;
      if (id.equals("a")) {
         String newName = req.getString("n");
         event = NavEvent.makeAdded(parentOrd, newName, null);
      } else if (id.equals("v")) {
         String oldName = req.getString("o");
         event = NavEvent.makeRemoved(parentOrd, oldName, null);
      } else if (id.equals("r")) {
         String oldName = req.getString("o");
         String newName = req.getString("n");
         event = NavEvent.makeRenamed(parentOrd, oldName, newName, null);
      } else if (id.equals("o")) {
         String order = req.getString("o");
         ArrayList<String> v = new ArrayList<>();
         StringTokenizer st = new StringTokenizer(order, "|");

         while (st.hasMoreTokens()) {
            v.add(st.nextToken());
         }

         String[] newOrder = v.toArray(new String[0]);
         event = NavEvent.makeReordered(parentOrd, newOrder, null);
      } else if (id.equals("p")) {
         String oldName = req.getString("o");
         event = NavEvent.makeReplaced(parentOrd, oldName, null);
      } else if (id.equals("c")) {
         String oldMask = req.getString("o");
         String newMask = req.getString("n");
         event = NavEvent.makeRecategorized(parentOrd, oldMask, newMask, null);
      } else {
         System.out.println("ERROR: Unknown event type: " + id);
         Thread.dumpStack();
      }

      if (event != null) {
         BNavRoot.INSTANCE.fireNavEvent(event);
      }

      return null;
   }

   public <R> Optional<R> niagaraRpc(BOrd ord, String methodName, Object... args) throws Exception {
      Version remoteVersion = new Version(this.getConnection().session().getRemoteHello().getString("app.version", ""));
      if (remoteVersion.compareTo(FoxSession.VERSION_4_1) < 0) {
         throw new UnsupportedOperationException("RPC requires Niagara 4.1 or later");
      } else {
         BObject target = ord.get();
         boolean isLegacyRpc = NiagaraRpcUtil.isWhitelistedLegacyRpc(target.getType().getTypeSpec(), methodName);
         JSONArray argsArray;
         if (isLegacyRpc) {
            argsArray = NiagaraRpcUtil.encodeLegacyArgs(args);
         } else {
            argsArray = new JSONArray();

            for (Object o : args) {
               argsArray.put(NiagaraRpcUtil.convertFromCollection(o));
            }
         }

         FoxRequest req = this.makeRequest("niagaraRpc");
         req.add("ord", ord.relativizeToSession().toString());
         req.add("methodName", methodName);
         req.add("args", argsArray.toString());
         req.add("legacyRpc", isLegacyRpc);
         if (this.isTraceOn()) {
            this.trace("c:niagaraRpc " + ord + " -> " + methodName);
         }

         FoxResponse resp = this.sendSync(req);
         String respJson = resp.getString("response");
         JSONObject respObj = new JSONObject(respJson);
         if (respObj.has("exception")) {
            throw new ServerException("Cannot invoke RPC", BSysChannel.NiagaraRpcServerException.make(respObj.getJSONObject("exception")));
         } else if (respObj.has("value")) {
            Object value = respObj.get("value");
            return Optional.ofNullable((R)(isLegacyRpc ? ValueDocDecoder.unmarshal((String)value) : NiagaraRpcUtil.convertToCollection(value)));
         } else {
            return Optional.empty();
         }
      }
   }

   private FoxResponse niagaraRpc(FoxRequest req) throws Exception {
      BOrd ord = BOrd.make(req.getString("ord"));
      String methodName = req.getString("methodName");
      JSONArray args = new JSONArray(req.getString("args"));
      boolean isLegacyRpc = req.getBoolean("legacyRpc", false);
      if (this.isTraceOn()) {
         this.trace("s:niagaraRpc " + ord + " -> " + methodName);
      }

      JSONObject respObj = new JSONObject();
      BFoxServerConnection connection = this.getServerConnection();
      if (connection == null) {
         throw new IllegalStateException("Cannot find Server Connection");
      } else {
         try {
            String remoteAddr = Objects.toString(connection.session().getRemoteHost(), "");
            Optional<Object> retVal = NiagaraRpcUtil.rpc(
               TransportType.fox, connection.session().isSecure(), remoteAddr, ord, methodName, args, this.getSessionContext()
            );
            if (retVal.isPresent()) {
               Object value = retVal.get();
               if (isLegacyRpc) {
                  value = ValueDocEncoder.marshal(NiagaraRpcUtil.wrapLegacyRpcResult(value));
               }

               respObj.put("value", value);
            }
         } catch (Exception var11) {
            if (niagaraRpcLog.isLoggable(Level.FINE)) {
               niagaraRpcLog.log(Level.FINE, "Cannot invoke Server side Fox Niagara RPC", (Throwable)var11);
            }

            if (!(var11 instanceof SecurityException) && !(var11.getCause() instanceof SecurityException)) {
               respObj.put("exception", BSysChannel.NiagaraRpcServerException.encodeStackTrace(var11));
            } else {
               Exception genEx = new Exception(Lexicon.make("baja").getText("niagaraRpc.securityMessage"));
               respObj.put("exception", BSysChannel.NiagaraRpcServerException.encodeStackTrace(genEx));
            }
         }

         FoxResponse resp = new FoxResponse(req);
         resp.add("response", respObj.toString());
         return resp;
      }
   }

   public static final class NiagaraRpcServerException extends RuntimeException {
      private NiagaraRpcServerException(String message) {
         super("Error from Server-side: " + message, null, false, true);
      }

      private static BSysChannel.NiagaraRpcServerException make(JSONObject obj) {
         String message = JSONUtil.getString(obj, "m");
         List<JSONObject> list = JSONUtil.toUnmodifiableList(obj.getJSONArray("st"));
         StackTraceElement[] elements = list.stream()
            .map(
               element -> new StackTraceElement(
                  JSONUtil.getString(element, "cn"),
                  JSONUtil.getString(element, "mn"),
                  element.has("fn") ? JSONUtil.getString(element, "fn") : null,
                  element.getInt("ln")
               )
            )
            .toArray(StackTraceElement[]::new);
         BSysChannel.NiagaraRpcServerException err = new BSysChannel.NiagaraRpcServerException(message);
         err.setStackTrace(elements);
         return err;
      }

      private static JSONObject encodeStackTrace(Exception exception) {
         JSONObject resp = new JSONObject();
         resp.put("m", Objects.toString(exception.getMessage(), "unknown"));
         JSONArray array = new JSONArray();
         Arrays.stream(exception.getStackTrace()).map(element -> {
            JSONObject traceObj = new JSONObject();
            traceObj.put("cn", element.getClassName());
            traceObj.put("mn", element.getMethodName());
            if (element.getFileName() != null) {
               traceObj.put("fn", element.getFileName());
            }

            traceObj.put("ln", element.getLineNumber());
            return traceObj;
         }).forEach(array::put);
         resp.put("st", array);
         return resp;
      }
   }
}
