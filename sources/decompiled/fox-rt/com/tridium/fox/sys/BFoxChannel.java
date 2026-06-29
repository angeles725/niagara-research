package com.tridium.fox.sys;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.crypto.core.exchange.KeyExchange;
import com.tridium.fox.encoding.BogCodec;
import com.tridium.fox.encoding.DecoderFactory;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxCircuit;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.IncompatibleVersionException;
import com.tridium.fox.session.InvalidChannelException;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.session.ServerException;
import com.tridium.fox.sys.spy.FoxLog;
import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.EncryptionAlgorithmBundle;
import com.tridium.nre.security.EncryptionKeySource;
import com.tridium.nre.security.ISecretBytesSupplier;
import com.tridium.nre.security.SecretBytes;
import com.tridium.sys.transfer.TransferListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.InvalidKeyException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import javax.baja.io.BIContextEncodable;
import javax.baja.io.ValueDocDecoder;
import javax.baja.io.ValueDocEncoder;
import javax.baja.io.ValueDocDecoder.BogTypeResolver;
import javax.baja.net.NotConnectedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.security.BAbstractPasswordEncoder;
import javax.baja.security.BAes256CbcPasswordEncoder;
import javax.baja.security.BAes256PasswordEncoder;
import javax.baja.security.BAliasedAes256CbcPasswordEncoder;
import javax.baja.security.BAliasedAes256PasswordEncoder;
import javax.baja.security.BIProtected;
import javax.baja.security.BPassword;
import javax.baja.security.BPbkdf2HmacSha256PasswordEncoder;
import javax.baja.security.BPermissions;
import javax.baja.security.PasswordEncodingContext;
import javax.baja.status.BIStatus;
import javax.baja.sync.SyncDecoder;
import javax.baja.sync.SyncEncoder;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BSimple;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Version;
import javax.baja.xml.XException;

@NiagaraType
public abstract class BFoxChannel extends BComponent {
   public static final Type TYPE = Sys.loadType(BFoxChannel.class);
   private static final BIcon icon = BIcon.std("bookmark.png");
   protected static final int ROUTED_CIRCUIT_BUFFER_SIZE = AccessController.doPrivileged(
      (PrivilegedAction<Integer>)(() -> Integer.getInteger("niagara.fox.routedCircuitBufferSize", 8192))
   );
   protected static final BiConsumer<FoxCircuit, FoxCircuit> DEFAULT_CIRCUIT_ROUTER = (sourceCircuit, destCircuit) -> {
      boolean bytesWritten = false;

      try {
         FoxMessage req = sourceCircuit.readMessage();
         InputStream destInput = destCircuit.getInputStream();
         OutputStream sourceOutput = sourceCircuit.getOutputStream();
         destCircuit.writeMessage(req);
         destCircuit.flush();

         int len;
         for (byte[] byteBuffer = new byte[ROUTED_CIRCUIT_BUFFER_SIZE];
            (len = destInput.read(byteBuffer, 0, ROUTED_CIRCUIT_BUFFER_SIZE)) >= 0;
            bytesWritten = true
         ) {
            sourceOutput.write(byteBuffer, 0, len);
         }
      } catch (Exception var9) {
         RuntimeException ex;
         if (var9 instanceof LocalizableRuntimeException && "fox.channel.unsupportedRemoteVersion".equals(((LocalizableRuntimeException)var9).getLexiconKey())) {
            ex = new LocalizableRuntimeException(
               "fox", "fox.channel.unsupportedRemoteVersionAlongRoute", ((LocalizableRuntimeException)var9).getLexiconArguments()
            );
         } else {
            ex = new UnreachableStationException(var9);
         }

         if (!bytesWritten && sourceCircuit.isOpen()) {
            try {
               sourceCircuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(ex));
               sourceCircuit.flush();
            } catch (Exception var8) {
            }
         }

         throw ex;
      }
   };
   private static final Set<String> LEGACY_BLACKLIST_TYPES;
   static final FoxResponse UNHANDLED_RESPONSE;
   private static final String KEY_HANDSHAKE_MESSAGE = "Simplify, then add lightness";
   private static final String REQ_TARGET_STATION_ROUTE = "nrsRoute";
   public static final Version VER_4_13;
   static final FoxLog foxLog;
   public final FoxLog log;
   private SecretBytes sharedEncodingKey = null;
   protected EncryptionAlgorithmBundle encryptionAlgorithmBundle = null;

   public Type getType() {
      return TYPE;
   }

   protected BFoxChannel(String logName) {
      this.log = FoxLog.make("fox." + logName);
   }

   final void fwSessionOpened() throws Exception {
      if (this.useSharedKeyEncryption()) {
         FoxSession session = this.getConnection().session();
         if (!session.isServer()) {
            this.initializeSharedKey(session);
         }
      }
   }

   final void fwSessionClosed(Throwable cause) {
      if (this.sharedEncodingKey != null) {
         this.sharedEncodingKey.close();
         this.sharedEncodingKey = null;
      }
   }

   public void sessionOpened() throws Exception {
   }

   public void sessionClosed(Throwable cause) throws Exception {
   }

   final FoxResponse fwProcess(FoxRequest request) throws Throwable {
      if (this.allowRoutingRequestToReachableStation(request)) {
         FoxResponse resp = checkRouteRequestToReachableStation(this, request);
         if (resp != UNHANDLED_RESPONSE) {
            return resp;
         }
      }

      String var4 = request.command;
      switch (var4) {
         case "initializeSharedKey":
            return this.initializeSharedKey(request);
         case "acceptSharedKey":
            return this.acceptSharedKey(request);
         default:
            return UNHANDLED_RESPONSE;
      }
   }

   public void checkProcess(FoxRequest req) throws Throwable {
      if (this.getConnection().session().isLegacyConnection()) {
         if (foxLog.isTraceOn()) {
            foxLog.trace("N4 station blocked AX station request (channel: " + req.channel + ", command: " + req.command + ")");
         }

         throw new IncompatibleVersionException("Niagara4 station cannot process NiagaraAX station request");
      }
   }

   final boolean fwCircuitOpened(FoxCircuit circuit) throws Throwable {
      return this.allowRoutingCircuitToReachableStation(circuit) ? checkRouteCircuitToReachableStation(this, circuit) : false;
   }

   public void checkProcessCircuit(FoxCircuit circuit) throws Throwable {
      if (this.getConnection().session().isLegacyConnection()) {
         if (foxLog.isTraceOn()) {
            foxLog.trace("N4 station blocked AX station circuit request (channel: " + circuit.channel + ", command: " + circuit.command + ")");
         }

         throw new IncompatibleVersionException("Niagara4 station cannot process NiagaraAX station request");
      }
   }

   public abstract FoxResponse process(FoxRequest var1) throws Throwable;

   public void circuitOpened(FoxCircuit circuit) throws Throwable {
      throw new InvalidCommandException(circuit.command);
   }

   public final FoxRequest makeRequest(String command) {
      return new FoxRequest(this.getName(), command);
   }

   public void checkSendRequest(FoxRequest req) throws Exception {
   }

   public void checkOpenCircuit(String command, FoxMessage metadata) throws Exception {
   }

   public final FoxCircuit openCircuit(String command) throws Exception {
      return this.openCircuit(command, null);
   }

   public final FoxCircuit openCircuit(String command, FoxMessage metadata) throws Exception {
      this.checkOpenCircuit(command, metadata);
      return this.getConnection().session().openCircuit(this.getName(), command, metadata);
   }

   public final FoxResponse sendSync(FoxRequest request) throws Exception {
      this.checkSendRequest(request);

      try {
         return this.getConnection().sendSync(request);
      } catch (NullPointerException var3) {
         if (this.getConnection() == null) {
            throw new NotConnectedException();
         } else {
            throw var3;
         }
      }
   }

   public final void sendAsync(FoxRequest request) throws Exception {
      this.checkSendRequest(request);

      try {
         this.getConnection().sendAsync(request);
      } catch (NullPointerException var3) {
         if (this.getConnection() == null) {
            throw new NotConnectedException();
         } else {
            throw var3;
         }
      }
   }

   public Map<String, Integer> getCircuitCommandThreadPriorities() {
      return null;
   }

   public BIcon getIcon() {
      return icon;
   }

   public final BFoxConnection getConnection() {
      try {
         return (BFoxConnection)this.getParent().getParent();
      } catch (NullPointerException var2) {
         return null;
      }
   }

   public final BFoxClientConnection getClientConnection() {
      try {
         return (BFoxClientConnection)this.getParent().getParent();
      } catch (NullPointerException var2) {
         return null;
      }
   }

   public final BFoxServerConnection getServerConnection() {
      try {
         return (BFoxServerConnection)this.getParent().getParent();
      } catch (NullPointerException var2) {
         return null;
      }
   }

   public final Context getSessionContext() {
      return this.getServerConnection().getSessionContext();
   }

   public final BPermissions getPermissionsFor(Object object) {
      return this.getPermissionsFor(object, true);
   }

   public final BPermissions getPermissionsFor(Object object, boolean dumpError) {
      try {
         if (object instanceof BIProtected) {
            return ((BIProtected)object).getPermissions(this.getSessionContext());
         }
      } catch (Exception var4) {
         if (dumpError) {
            var4.printStackTrace();
         }
      }

      return BPermissions.all;
   }

   public final BFoxSession getFoxSession() {
      return this.getClientConnection().getFoxSession();
   }

   protected ValueDocEncoder makeDefaultEncoder(OutputStream stream, Context cx) throws Exception {
      return new BFoxChannel.LegacyEncoder(this, stream, this.makeEncodingContext(cx));
   }

   protected ValueDocDecoder makeDefaultDecoder(InputStream stream, Context cx) throws Exception {
      return new ValueDocDecoder(stream, this.makeDecodingContext(cx));
   }

   protected SyncEncoder makeDefaultSyncEncoder(OutputStream stream, Context baseContext) throws Exception {
      return new BFoxChannel.FoxSyncEncoder(this, stream, this.makeEncodingContext(baseContext));
   }

   protected SyncDecoder makeDefaultSyncDecoder(InputStream stream, Context baseContext) throws Exception {
      return new SyncDecoder(stream, this.makeDecodingContext(baseContext));
   }

   protected void encodeValue(FoxMessage req, String name, BValue object, Context context) throws IOException {
      BogCodec.add(req, name, object, this.makeEncodingContext(context));
   }

   protected BValue decodeValue(FoxMessage msg, String key, Object arg) throws Exception {
      return (BValue)DecoderFactory.decode(msg, key, arg, this.makeDecodingContext(null));
   }

   protected String marshal(BValue value) throws Exception {
      return this.marshal(value, null);
   }

   protected String marshal(BValue value, Context cx) throws Exception {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      BFoxChannel.LegacyEncoder encoder = new BFoxChannel.LegacyEncoder(this, out, this.makeEncodingContext(cx));
      encoder.encode(value);
      encoder.close();
      return new String(out.toByteArray());
   }

   protected BValue unmarshal(String xml) throws Exception {
      return this.unmarshal(xml, null);
   }

   protected BValue unmarshal(String xml, Context cx) throws Exception {
      return ValueDocDecoder.unmarshal(xml, this.makeDecodingContext(cx));
   }

   protected void encodeSimple(FoxMessage msg, String key, BSimple value, Context baseContext) throws IOException {
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      DataOutput out = new DataOutputStream(outStream);
      if (value instanceof BIContextEncodable) {
         ((BIContextEncodable)value).encode(out, this.makeEncodingContext(baseContext));
      } else {
         value.encode(out);
      }

      msg.add(key, outStream.toByteArray());
   }

   protected boolean useSharedKeyEncryption() {
      return false;
   }

   protected Context makeEncryptionContext(Context baseContext, boolean outgoing) {
      return !this.useSharedKeyEncryption()
         ? PasswordEncodingContext.updateForNone(baseContext)
         : AccessController.doPrivileged((PrivilegedAction<Context>)(() -> PasswordEncodingContext.updateContext(baseContext, pContext -> {
            if (!pContext.hasEncryptionKey()) {
               if (this.sharedEncodingKey != null) {
                  if (outgoing) {
                     pContext.setDecryptionUndefined();
                     pContext.setEncryptionKey(EncryptionKeySource.shared, Optional.of(ISecretBytesSupplier.wrap(this.sharedEncodingKey.newCopy())));
                     pContext.setEncryptionAlgorithmBundle(this.encryptionAlgorithmBundle);
                  } else {
                     pContext.setEncryptionKey(EncryptionKeySource.keyring, Optional.empty());
                     pContext.setDecryptionKey(EncryptionKeySource.shared, Optional.of(ISecretBytesSupplier.wrap(this.sharedEncodingKey.newCopy())));
                  }
               } else {
                  pContext.setEncryptionAndDecryptionKey(EncryptionKeySource.none, Optional.empty());
               }
            }
         })));
   }

   protected Context makeEncodingContext(Context baseContext) {
      return this.makeEncryptionContext(baseContext, true);
   }

   protected Context makeDecodingContext(Context baseContext) {
      return this.makeEncryptionContext(baseContext, false);
   }

   private void initializeSharedKey(FoxSession session) throws Exception {
      if (session.hasSessionKey()) {
         this.encryptionAlgorithmBundle = session.getEncryptionAlgorithmBundle();
         int keySize = this.encryptionAlgorithmBundle.getKeySize() / 8;
         byte[] salt = new byte[keySize];
         new SecureRandom().nextBytes(salt);
         this.sharedEncodingKey = new SecretBytes(session.makeSharedSecret(salt), 0, keySize);
         byte[] iv = new byte[keySize];
         new SecureRandom().nextBytes(iv);

         try {
            byte[] message = Aes256PasswordManager.encrypt(
               "Simplify, then add lightness".getBytes(), iv, this.sharedEncodingKey.get(), this.getAesTransformation()
            );
            FoxRequest req = this.makeRequest("initializeSharedKey");
            Encoder encoder = Base64.getEncoder();
            req.add("salt", encoder.encodeToString(salt));
            req.add("iv", encoder.encodeToString(iv));
            req.add("message", encoder.encodeToString(message));
            this.sendSync(req);
         } catch (InvalidKeyException var8) {
            this.log.warning("Could not negotiate shared key", var8);
         } catch (ServerException var9) {
            if (var9.getMessage().contains("InvalidChannelException")) {
               InvalidChannelException ice = new InvalidChannelException(var9.toString());
               ice.initCause(var9);
               throw ice;
            }

            throw var9;
         }
      } else if (session.isSecure() && !session.isLegacyConnection()) {
         byte[] randomBytes = new byte[16];
         BPassword keyPbk = BPassword.make(ByteArrayUtil.toHexString(randomBytes), BPbkdf2HmacSha256PasswordEncoder.ENCODING_TYPE);
         this.sharedEncodingKey = new SecretBytes(((BPbkdf2HmacSha256PasswordEncoder)keyPbk.getPasswordEncoder()).getKey(), true);

         try {
            FoxRequest req = this.makeRequest("acceptSharedKey");
            Encoder encoder = Base64.getEncoder();
            req.add("key", encoder.encodeToString(this.sharedEncodingKey.get()));
            req.add("encryptionAlgorithmBundles", KeyExchange.getPreferredKeyExchangeCiphers256());
            FoxResponse resp = this.sendSync(req);
            String algorithmBundle;
            if (resp != null) {
               algorithmBundle = resp.getString("encryptionAlgorithmBundle", "aes-256.1");
            } else {
               algorithmBundle = "aes-256.1";
            }

            this.encryptionAlgorithmBundle = (EncryptionAlgorithmBundle)CryptographicAlgorithmBundle.getInstance(algorithmBundle);
         } catch (ServerException var10) {
            if (!var10.getMessage().contains("InvalidCommandException")) {
               if (var10.getMessage().contains("InvalidChannelException")) {
                  InvalidChannelException ice = new InvalidChannelException(var10.toString());
                  ice.initCause(var10);
                  throw ice;
               }

               throw var10;
            }

            this.log.warning("Remote host's broker channel (pre-release version?) uses incompatible password value encoding", var10);
         }
      }
   }

   private FoxResponse initializeSharedKey(FoxRequest req) throws Exception {
      FoxSession session = this.getConnection().session();
      Decoder decoder = Base64.getDecoder();
      byte[] iv = decoder.decode(req.getString("iv"));
      byte[] salt = decoder.decode(req.getString("salt"));
      byte[] message = decoder.decode(req.getString("message"));
      byte[] key = session.makeSharedSecret(salt);
      int keySize = session.getSharedKeySize() / 8;
      this.sharedEncodingKey = new SecretBytes(key, 0, keySize);
      SecurityUtil.zeroByteArray(key);
      this.encryptionAlgorithmBundle = session.getEncryptionAlgorithmBundle();
      byte[] decryptedMessage = Aes256PasswordManager.decrypt(this.sharedEncodingKey.get(), message, iv, this.getAesTransformation());
      if (!"Simplify, then add lightness".equals(new String(decryptedMessage))) {
         this.sharedEncodingKey.close();
         this.sharedEncodingKey = null;
         throw new Exception("Could not decrypt message");
      } else {
         return null;
      }
   }

   private FoxResponse acceptSharedKey(FoxRequest req) throws Exception {
      if (!this.getServerConnection().session().isSecure()) {
         throw new IOException("Not a TLS connection");
      } else {
         Decoder decoder = Base64.getDecoder();
         byte[] key = decoder.decode(req.getString("key"));
         this.sharedEncodingKey = new SecretBytes(key, true);
         String algorithmBundles = req.getString("encryptionAlgorithmBundles", "aes-256.1");
         String[] bundles = algorithmBundles.split(":");

         for (String bundle : bundles) {
            CryptographicAlgorithmBundle algorithmBundle = CryptographicAlgorithmBundle.getInstance(bundle);
            if (algorithmBundle instanceof EncryptionAlgorithmBundle) {
               this.encryptionAlgorithmBundle = (EncryptionAlgorithmBundle)algorithmBundle;
               break;
            }
         }

         if (this.encryptionAlgorithmBundle == null) {
            this.encryptionAlgorithmBundle = (EncryptionAlgorithmBundle)CryptographicAlgorithmBundle.getInstance("aes-256.1");
         }

         FoxResponse resp = new FoxResponse();
         resp.add("encryptionAlgorithmBundle", this.encryptionAlgorithmBundle.getAlgorithmName());
         return resp;
      }
   }

   protected String getAesTransformation() {
      return this.encryptionAlgorithmBundle instanceof AesAlgorithmBundle
         ? ((AesAlgorithmBundle)this.encryptionAlgorithmBundle).getAesTransformation()
         : "AES/GCM/NoPadding";
   }

   public boolean isTraceOn() {
      return this.log.isTraceOn();
   }

   public void trace(String s) {
      System.out.print("-- ");
      System.out.print(this.log.getLogName());
      System.out.print(" ");
      System.out.println(s);
      this.log.logRecOnly(0, s);
   }

   protected static void verifyRemoteVersion(BFoxChannel channel, Version minimumVersion) {
      Version remoteVersion = new Version(channel.getConnection().session().getRemoteHello().getString("app.version", ""));
      if (remoteVersion.compareTo(minimumVersion) < 0) {
         throw new LocalizableRuntimeException("fox", "fox.channel.unsupportedRemoteVersion", new Object[]{minimumVersion.toString()});
      }
   }

   private BSimple getSimple(BSimple simple) throws IOException {
      if (simple instanceof BPassword && this.getConnection().getRemoteVersion().compareTo(BAliasedAes256PasswordEncoder.MIN_VERSION) < 0) {
         BAbstractPasswordEncoder encoder = ((BPassword)simple).getPasswordEncoder();
         if (encoder instanceof BAliasedAes256PasswordEncoder) {
            try {
               BAbstractPasswordEncoder newEncoder = new BAes256PasswordEncoder();
               newEncoder.encode(AccessController.doPrivileged(encoder::getValue));
               simple = BPassword.make(newEncoder);
            } catch (Exception var6) {
               throw new IOException("could not transcode password", var6);
            }
         }
      }

      if (simple instanceof BPassword && this.getAesTransformation().equals("AES/CBC/PKCS5Padding")) {
         BAbstractPasswordEncoder encoder = ((BPassword)simple).getPasswordEncoder();
         if (encoder instanceof BAliasedAes256PasswordEncoder) {
            try {
               BAbstractPasswordEncoder newEncoder = new BAliasedAes256CbcPasswordEncoder(((BAliasedAes256PasswordEncoder)encoder).getKeyAlias());
               newEncoder.encode(AccessController.doPrivileged(encoder::getValue));
               simple = BPassword.make(newEncoder);
            } catch (Exception var5) {
               throw new IOException("could not transcode password", var5);
            }
         } else if (encoder instanceof BAes256PasswordEncoder) {
            try {
               BAbstractPasswordEncoder newEncoder = new BAes256CbcPasswordEncoder();
               newEncoder.encode(AccessController.doPrivileged(encoder::getValue));
               simple = BPassword.make(newEncoder);
            } catch (Exception var4) {
               throw new IOException("could not transcode password", var4);
            }
         }
      }

      return simple;
   }

   protected static final BValue checkNewInstance(
      BFoxChannel channel, ValueDocDecoder decoder, BComplex parent, String propName, Property prop, BValue newInstance, boolean skipLegacyEncodings
   ) throws RuntimeException {
      if (newInstance == null) {
         return null;
      } else if (isUnsupportedLegacyValue(channel, newInstance)) {
         if (skipLegacyEncodings) {
            try {
               decoder.skip();
               return null;
            } catch (XException var8) {
               throw var8;
            } catch (Exception var9) {
               throw new XException(var9);
            }
         } else {
            throw new SecurityException("Niagara4 station cannot decode " + newInstance.getType() + " from AX station");
         }
      } else {
         return newInstance;
      }
   }

   private static boolean isUnsupportedLegacyValue(BFoxChannel channel, BValue val) {
      return !channel.getConnection().session().isLegacyConnection() ? false : isBlacklistedLegacyType(val.getType());
   }

   public static final boolean isBlacklistedLegacyType(Type type) {
      return LEGACY_BLACKLIST_TYPES.contains(type.toString());
   }

   protected static FoxMessage makeFoxMessageWithReachableStationRoute(
      BFoxChannel channel, FoxMessage baseMessage, Version minRouteVersion, String... targetStationRoute
   ) {
      if (targetStationRoute != null && targetStationRoute.length >= 1) {
         FoxMessage message = baseMessage != null ? baseMessage : new FoxMessage();
         NiagaraStation station = channel.getConnection().getConnectionTarget(NiagaraStation.class).orElse(null);
         String remoteStationName = station != null ? station.getStationName() : null;
         boolean foundReachableStationName = false;

         for (String stationName : targetStationRoute) {
            if (foundReachableStationName || !stationName.equals(remoteStationName)) {
               foundReachableStationName = true;
               message.add("nrsRoute", stationName);
            }
         }

         if (foundReachableStationName) {
            if (minRouteVersion == null || minRouteVersion.isNull() || minRouteVersion.compareTo(VER_4_13) < 0) {
               minRouteVersion = VER_4_13;
            }

            verifyRemoteVersion(channel, minRouteVersion);
         }

         return message;
      } else {
         return baseMessage;
      }
   }

   protected boolean allowRoutingRequestToReachableStation(FoxRequest req) {
      return false;
   }

   protected Version getMinReachableStationVersionForRequest(FoxRequest req) {
      return null;
   }

   protected Version getMinVersionAlongRouteForRequest(FoxRequest req) {
      return null;
   }

   protected FoxResponse routeRequestToDestinationChannel(BFoxChannel destChannel, FoxRequest req) throws Exception {
      return destChannel.sendSync(req);
   }

   private static FoxResponse checkRouteRequestToReachableStation(BFoxChannel sourceChannel, FoxRequest req) throws Throwable {
      if (req.getOptional("nrsRoute") == null) {
         return UNHANDLED_RESPONSE;
      } else {
         String[] targetStationRoute = req.listStrings("nrsRoute");
         int routeLength = targetStationRoute.length;
         String firstRemoteStationName = null;
         if (routeLength > 0 && !targetStationRoute[routeLength - 1].equals(Sys.getStation().getStationName())) {
            req.remove("nrsRoute");
            Version minRemoteVersion = sourceChannel.getMinReachableStationVersionForRequest(req);
            boolean setVersionForRoute = false;

            for (String stationName : targetStationRoute) {
               if (firstRemoteStationName != null || !stationName.equals(Sys.getStation().getStationName())) {
                  if (firstRemoteStationName == null) {
                     firstRemoteStationName = stationName;
                  } else {
                     req.add("nrsRoute", stationName);
                     if (!setVersionForRoute) {
                        minRemoteVersion = sourceChannel.getMinVersionAlongRouteForRequest(req);
                        if (minRemoteVersion == null || minRemoteVersion.isNull() || minRemoteVersion.compareTo(VER_4_13) < 0) {
                           minRemoteVersion = VER_4_13;
                        }

                        setVersionForRoute = true;
                     }
                  }
               }
            }

            if (firstRemoteStationName != null) {
               return doRouteRequestToReachableStation(firstRemoteStationName, sourceChannel, req, minRemoteVersion);
            }
         }

         return UNHANDLED_RESPONSE;
      }
   }

   private static FoxResponse doRouteRequestToReachableStation(String stationName, BFoxChannel sourceChannel, FoxRequest req, Version minRemoteVersion) throws Throwable {
      BFoxClientConnection.Interest interest = new BFoxClientConnection.StringInterest("FoxChannel - RouteRequest at " + Clock.ticks());
      BFoxClientConnection connection = null;

      FoxResponse var9;
      try {
         NiagaraNetwork network = (NiagaraNetwork)Sys.getService(Sys.getType("niagaraDriver:NiagaraNetwork"));
         BComponent station = (BComponent)network.getStation(stationName);
         if (station == null) {
            throw new UnreachableStationException(
               "Could not find station '" + stationName + "' in the NiagaraNetwork of station '" + Sys.getStation().getStationName() + '\''
            );
         }

         if (station instanceof BIStatus
            && (((BIStatus)station).getStatus().isDisabled() || ((BIStatus)station).getStatus().isDown() || ((BIStatus)station).getStatus().isFault())) {
            throw UnreachableStationException.makeUnoperationalStationException(stationName, Sys.getStation().getStationName());
         }

         connection = (BFoxClientConnection)station.get("clientConnection");
         connection.engageNoRetry(interest);
         BFoxChannel destChannel = connection.getChannels().get(sourceChannel.getName(), sourceChannel.getType());
         if (minRemoteVersion != null) {
            verifyRemoteVersion(destChannel, minRemoteVersion);
         }

         var9 = sourceChannel.routeRequestToDestinationChannel(destChannel, req);
      } catch (Exception var13) {
         Exception ex = var13;
         boolean isLocalizable = false;
         if (var13 instanceof LocalizableRuntimeException) {
            isLocalizable = true;
            if ("fox.channel.unsupportedRemoteVersion".equals(((LocalizableRuntimeException)var13).getLexiconKey())) {
               ex = new LocalizableRuntimeException(
                  "fox", "fox.channel.unsupportedRemoteVersionAlongRoute", ((LocalizableRuntimeException)var13).getLexiconArguments()
               );
            }
         }

         if (sourceChannel.log.log().isLoggable(Level.FINE)) {
            sourceChannel.log.log().log(Level.WARNING, "Failed to route request through remote station " + stationName, (Throwable)ex);
         }

         if (!isLocalizable && !(ex instanceof UnreachableStationException)) {
            throw new UnreachableStationException(ex);
         }

         throw ex;
      } finally {
         if (connection != null && connection.isEngaged(interest)) {
            connection.disengage(interest);
         }
      }

      return var9;
   }

   protected boolean allowRoutingCircuitToReachableStation(FoxCircuit circuit) {
      return false;
   }

   protected Version getMinReachableStationVersionForCircuit(FoxCircuit circuit) {
      return null;
   }

   protected Version getMinVersionAlongRouteForCircuit(FoxCircuit circuit) {
      return null;
   }

   protected BiConsumer<FoxCircuit, FoxCircuit> getCircuitRouter(String sourceCircuitCommand, BFoxChannel sourceChannel) {
      return DEFAULT_CIRCUIT_ROUTER;
   }

   private static boolean checkRouteCircuitToReachableStation(BFoxChannel channel, FoxCircuit circuit) throws Throwable {
      if (circuit.metadata != null && circuit.metadata.getOptional("nrsRoute") != null) {
         String[] targetStationRoute = circuit.metadata.listStrings("nrsRoute");
         int routeLength = targetStationRoute.length;
         String firstRemoteStationName = null;
         if (routeLength > 0 && !targetStationRoute[routeLength - 1].equals(Sys.getStation().getStationName())) {
            circuit.metadata.remove("nrsRoute");
            Version minRemoteVersion = channel.getMinReachableStationVersionForCircuit(circuit);
            boolean setVersionForRoute = false;

            for (String stationName : targetStationRoute) {
               if (firstRemoteStationName != null || !stationName.equals(Sys.getStation().getStationName())) {
                  if (firstRemoteStationName == null) {
                     firstRemoteStationName = stationName;
                  } else {
                     circuit.metadata.add("nrsRoute", stationName);
                     if (!setVersionForRoute) {
                        minRemoteVersion = channel.getMinVersionAlongRouteForCircuit(circuit);
                        if (minRemoteVersion == null || minRemoteVersion.isNull() || minRemoteVersion.compareTo(VER_4_13) < 0) {
                           minRemoteVersion = VER_4_13;
                        }

                        setVersionForRoute = true;
                     }
                  }
               }
            }

            if (firstRemoteStationName != null) {
               doRouteCircuitToReachableStation(firstRemoteStationName, channel, circuit, minRemoteVersion);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static void doRouteCircuitToReachableStation(String stationName, BFoxChannel sourceChannel, FoxCircuit sourceCircuit, Version minRemoteVersion) throws Throwable {
      BFoxClientConnection connection = null;
      BFoxClientConnection.Interest interest = null;
      FoxCircuit destinationCircuit = null;
      boolean circuitRouted = false;

      try {
         NiagaraNetwork network = (NiagaraNetwork)Sys.getService(Sys.getType("niagaraDriver:NiagaraNetwork"));
         BComponent station = (BComponent)network.getStation(stationName);
         if (station == null) {
            throw new UnreachableStationException(
               "Could not find station '" + stationName + "' in the NiagaraNetwork of station '" + Sys.getStation().getStationName() + '\''
            );
         }

         if (station instanceof BIStatus
            && (((BIStatus)station).getStatus().isDisabled() || ((BIStatus)station).getStatus().isDown() || ((BIStatus)station).getStatus().isFault())) {
            throw UnreachableStationException.makeUnoperationalStationException(stationName, Sys.getStation().getStationName());
         }

         interest = new BFoxClientConnection.StringInterest("FoxChannel - RouteCircuit at " + Clock.ticks());
         connection = (BFoxClientConnection)station.get("clientConnection");
         connection.engageNoRetry(interest);
         BFoxChannel destChannel = connection.getChannels().get(sourceChannel.getName(), sourceChannel.getType());
         if (minRemoteVersion != null) {
            verifyRemoteVersion(destChannel, minRemoteVersion);
         }

         String sourceCircuitCommand = sourceCircuit.command;
         destinationCircuit = destChannel.openCircuit(sourceCircuitCommand, sourceCircuit.metadata);
         circuitRouted = true;
         destChannel.getCircuitRouter(sourceCircuitCommand, sourceChannel).accept(sourceCircuit, destinationCircuit);
      } catch (Exception var38) {
         Throwable ex = var38;
         if (var38 instanceof LocalizableRuntimeException
            && "fox.channel.unsupportedRemoteVersion".equals(((LocalizableRuntimeException)var38).getLexiconKey())) {
            ex = new LocalizableRuntimeException(
               "fox", "fox.channel.unsupportedRemoteVersionAlongRoute", ((LocalizableRuntimeException)var38).getLexiconArguments()
            );
         } else if (var38 instanceof BajaRuntimeException && var38.getCause() != null) {
            ex = var38.getCause();
         }

         if (sourceChannel.log.log().isLoggable(Level.FINE)) {
            sourceChannel.log.log().log(Level.WARNING, "Failed to route circuit through remote station " + stationName, ex);
         }

         if (!circuitRouted && sourceCircuit.isOpen()) {
            try {
               sourceCircuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(ex));
               sourceCircuit.flush();
            } catch (Exception var37) {
            }
         }
      } finally {
         if (destinationCircuit != null) {
            if (destinationCircuit.getInputStream() != null) {
               try {
                  destinationCircuit.getInputStream().close();
               } catch (IOException var36) {
               }
            }

            if (destinationCircuit.getOutputStream() != null) {
               try {
                  destinationCircuit.getOutputStream().close();
               } catch (IOException var35) {
               }
            }

            try {
               destinationCircuit.close();
            } catch (RuntimeException var34) {
            }
         }

         if (sourceCircuit.getInputStream() != null) {
            try {
               sourceCircuit.getInputStream().close();
            } catch (IOException var33) {
            }
         }

         if (sourceCircuit.getOutputStream() != null) {
            try {
               sourceCircuit.getOutputStream().close();
            } catch (IOException var32) {
            }
         }

         if (connection != null && connection.isEngaged(interest)) {
            connection.disengage(interest);
         }
      }
   }

   static {
      HashSet<String> blacklistTypes = new HashSet<>();
      Collections.addAll(blacklistTypes, "baja:Password", "baja:UsernameAndPassword", "baja:PasswordHistory");

      try {
         Collections.addAll(
            blacklistTypes,
            AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.legacy.blacklistTypes", null))).split(";")
         );
      } catch (Exception var2) {
      }

      LEGACY_BLACKLIST_TYPES = Collections.unmodifiableSet(blacklistTypes);
      UNHANDLED_RESPONSE = new FoxResponse();
      VER_4_13 = new Version("4.13");
      foxLog = FoxLog.make("fox");
   }

   protected static class FoxSyncEncoder extends SyncEncoder {
      BFoxChannel channel;

      public FoxSyncEncoder(BFoxChannel channel, OutputStream out, Context cx) throws Exception {
         super(out, cx);
         this.channel = channel;
      }

      protected boolean encodePropertyValue(BComplex parent, Property prop, int depth, BPermissions permissions, Context context) throws IOException {
         FoxSession session = this.channel.getConnection().session();
         if (session.isServer() && !session.supportsSecureData() && prop.getType().is(BPassword.TYPE)) {
            BPassword newValue = BPassword.DEFAULT;
            String s = this.encodeSimple(newValue);
            this.plugin.attrSafe("v", s);
            this.plugin.end().newLine();
            return true;
         } else {
            return super.encodePropertyValue(parent, prop, depth, permissions, context);
         }
      }

      protected String encodeSimple(BSimple simple) throws IOException {
         return super.encodeSimple(this.channel.getSimple(simple));
      }
   }

   protected static class LegacyEncoder extends ValueDocEncoder {
      BFoxChannel channel;

      public LegacyEncoder(BFoxChannel channel, OutputStream out, Context context) throws IOException {
         super(out, context);
         this.channel = channel;
      }

      protected void encodingValue(BValue val, Context cx) throws IOException {
         if (val != null) {
            if (BFoxChannel.isUnsupportedLegacyValue(this.channel, val)) {
               throw new SecurityException("Niagara4 station cannot encode " + val.getType() + " to AX station");
            }
         }
      }

      protected String encodeSimple(BSimple simple) throws IOException {
         return super.encodeSimple(this.channel.getSimple(simple));
      }
   }

   protected static class LegacyTypeResolver extends BogTypeResolver {
      BFoxChannel channel;
      boolean skipLegacyEncodings = false;

      public LegacyTypeResolver(BFoxChannel channel, boolean skipLegacyEncodings) {
         this.channel = channel;
         this.skipLegacyEncodings = skipLegacyEncodings;
      }

      public BValue newInstance(ValueDocDecoder decoder, BComplex parent, String propName, Property prop, String typeStr) {
         BValue result = super.newInstance(decoder, parent, propName, prop, typeStr);
         return BFoxChannel.checkNewInstance(this.channel, decoder, parent, propName, prop, result, this.skipLegacyEncodings);
      }

      public void setSkipLegacyEncodings(boolean skipLegacyEncodings) {
         this.skipLegacyEncodings = skipLegacyEncodings;
      }

      public boolean getSkipLegacyEncodings() {
         return this.skipLegacyEncodings;
      }
   }

   public class TransferStatusPipe implements TransferListener {
      FoxCircuit circuit;

      public TransferStatusPipe(FoxCircuit circuit) {
         this.circuit = circuit;
      }

      public void updateStatus(String status) {
         try {
            FoxMessage msg = new FoxMessage();
            msg.add("s", status);
            this.circuit.writeMessage(msg);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }
}
