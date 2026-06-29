package com.tridium.fox.session;

import com.tridium.authn.AuthenticationClient;
import com.tridium.authn.BAuthenticationService;
import com.tridium.authn.BDigestAuthenticationScheme;
import com.tridium.authn.BLegacyDigestAuthenticationScheme;
import com.tridium.authn.BSessionIdAuthenticationScheme;
import com.tridium.authn.LoginFailureCause;
import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.crypto.core.exchange.KeyExchange;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.sys.Acceptor;
import com.tridium.fox.sys.BFoxClientConnection;
import com.tridium.fox.sys.BFoxServerConnection;
import com.tridium.fox.sys.BFoxService;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.nre.security.EncryptionAlgorithmBundle;
import com.tridium.nre.security.KeyDerivationAlgorithmBundle;
import com.tridium.nre.security.NullAlgorithmBundle;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.session.SessionManager;
import com.tridium.user.BGlobalPasswordConfiguration;
import com.tridium.user.BUserPasswordConfiguration;
import com.tridium.util.ValueByteBuffer;
import java.io.IOException;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.data.BIDataValue;
import javax.baja.fox.authn.BFoxCallbackHandler;
import javax.baja.fox.authn.BFoxClientAuthnHandler;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.security.AuthenticationException;
import javax.baja.security.BAbstractAuthenticator;
import javax.baja.security.BHttpFoxCredentials;
import javax.baja.security.BICredentials;
import javax.baja.security.BIExtraAttributesCredentials;
import javax.baja.security.BIUserCredentials;
import javax.baja.security.BPasswordAuthenticator;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.security.BUsernameCredential;
import javax.baja.security.kerberos.BKerberosCredentials;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Localizable;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.user.BPasswordStrength;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;
import javax.security.auth.Subject;

public class Tuner extends Thread {
   private static final String FOX_LOGIN_UNSUPPORTED = "foxUnsupported";
   private static final Logger LOGGER = Logger.getLogger("fox");
   private FoxServer server;
   private FoxSession session;
   private String scheme;

   static FoxSession openClient(FoxConnection conn, Socket socket, String user, String password, FoxSession.IFoxSessionListener[] listeners) throws Exception {
      BUsernameAndPassword credentials = new BUsernameAndPassword(user, password);
      return openClient(conn, socket, credentials, listeners);
   }

   static FoxSession openClient(FoxConnection conn, Socket socket, BICredentials credentials, FoxSession.IFoxSessionListener[] listeners) throws Exception {
      byte[] encoded = ValueByteBuffer.marshal((BObject)credentials);
      FoxSession session = new FoxSession(socket, conn, listeners);
      session.setState("client.tune open credentials.len=" + encoded.length);
      Fox.register(session);
      BFoxClientAuthnHandler handler = null;

      try {
         session.setState("client.tune sendHello");
         if (credentials instanceof BHttpFoxCredentials) {
            session.sendHello(null, ((BHttpFoxCredentials)credentials).getSessionId(), true);
         } else {
            session.sendHello(null, true);
         }

         session.setState("client.tune receiveHello");
         session.receiveHello();
         BFoxClientConnection foxClientConnection = (BFoxClientConnection)conn;
         BFoxSession bFoxSession = ((BFoxClientConnection)conn).getFoxSession();
         if (bFoxSession != null) {
            FoxMessage hello = session.getRemoteHello();
            String availableSchemes = hello.getString("availableSchemes", null);
            if (availableSchemes != null) {
               bFoxSession.setAvailableAuthenticationSchemes(availableSchemes.split(","));
               bFoxSession.setDefaultAuthenticationScheme(hello.getString("defaultScheme", null));
            } else {
               bFoxSession.setAvailableAuthenticationSchemes(null);
               bFoxSession.setDefaultAuthenticationScheme(null);
            }
         }

         AuthenticationClient authenticationClient = foxClientConnection.getAuthenticationClient();
         if (authenticationClient == null) {
            throw new FoxAuthenticationException("Rejected", null, null, session);
         } else {
            BIUserCredentials usernameCredential = new BUsernameCredential();
            if (!session.isLegacyConnection()) {
               session.setState("client.tune receiveKerberos");
               FoxMessage kerberos = session.receiveTuning("kerberos");
               boolean useKerberos = kerberos.getBoolean("useKerberos");
               if (useKerberos && bFoxSession != null) {
                  bFoxSession.setAuthenticationScheme("n4Kerberos");
               }

               usernameCredential = authenticationClient.requestUsername(bFoxSession);
               if (usernameCredential == null) {
                  throw new FoxAuthenticationException("Cancelled", null, null, session);
               }

               if (bFoxSession != null) {
                  bFoxSession.setUsernameCredential(usernameCredential);
               }

               Boolean kerbKey = false;
               if (usernameCredential instanceof BKerberosCredentials) {
                  kerbKey = true;
               }

               String requestedUsername = usernameCredential.getUsername();
               FoxMessage usernameMessage = new FoxMessage();
               usernameMessage.add("username", requestedUsername);
               usernameMessage.add("kerbKey", kerbKey);
               addExtraAttributes(usernameCredential, usernameMessage, session);
               session.setState("client.tune sendUsername");
               session.sendTuning("username", usernameMessage);
            }

            boolean retry = false;

            do {
               session.setState("client.tune receiveChallenge");
               FoxMessage challenge = session.receiveTuning("challenge");
               String method = challenge.getString("method");
               if (method.equals("foxUnsupported")) {
                  throw new AuthenticationException(bFoxSession, LoginFailureCause.LOGIN_INTERFACE_NOT_SUPPORTED);
               }

               String keyExchangeMethods = challenge.getString("keyExchangeMethods", null);
               String keyExchangeCiphers = challenge.getString("keyExchangeCiphers", null);
               if (keyExchangeMethods != null) {
                  FoxMessage exchangeResponse = new FoxMessage();
                  KeyDerivationAlgorithmBundle keyDerivationAlgorithmBundle = null;
                  EncryptionAlgorithmBundle encryptionAlgorithmBundle = null;
                  String[] methods = keyExchangeMethods.split(":");

                  for (String m : methods) {
                     CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstance(m);
                     if (bundle instanceof KeyDerivationAlgorithmBundle) {
                        keyDerivationAlgorithmBundle = (KeyDerivationAlgorithmBundle)bundle;
                        break;
                     }
                  }

                  if (keyExchangeCiphers != null) {
                     String[] ciphers = keyExchangeCiphers.split(":");

                     for (String c : ciphers) {
                        CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstance(c);
                        if (bundle instanceof EncryptionAlgorithmBundle) {
                           encryptionAlgorithmBundle = (EncryptionAlgorithmBundle)bundle;
                           break;
                        }
                     }
                  }

                  if (keyDerivationAlgorithmBundle != null && encryptionAlgorithmBundle != null) {
                     String exchangeMethod = keyDerivationAlgorithmBundle.getAlgorithmName();
                     exchangeResponse.add("keyExchangeMethod", exchangeMethod);
                     exchangeResponse.add("keyExchangeCipher", encryptionAlgorithmBundle.getAlgorithmName());
                     session.setKeyExchangeAlgorithmBundle(keyDerivationAlgorithmBundle);
                     session.setEncryptionAlgorithmBundle(encryptionAlgorithmBundle);
                  } else {
                     exchangeResponse.add("keyExchangeMethod", NullAlgorithmBundle.getInstance().getAlgorithmName());
                  }

                  String keyExchangeMethodName = session.getKeyExchangeAlgorithmBundle().getAlgorithmName();
                  String keyExchangeCipherName = session.getEncryptionAlgorithmBundle() == null
                     ? "null"
                     : session.getEncryptionAlgorithmBundle().getAlgorithmName();
                  session.setState("client.tune sendKeyExchangeMethod method=" + keyExchangeMethodName + '/' + keyExchangeCipherName);
                  session.sendTuning("clientKeyExchangeMethod", exchangeResponse);
               }

               session.setState("client.tune receivedChallenge method=" + method);
               foxClientConnection.setAuthenticationScheme(method);
               if (bFoxSession != null) {
                  bFoxSession.setAuthenticationScheme(method);
               }

               BAuthenticationScheme authenticationScheme = BAuthenticationScheme.getSchemeFromName(method);
               if (authenticationScheme == null) {
                  Lexicon lex = Lexicon.make(Tuner.class);
                  throw new AuthenticationException(lex.getText("fox.authScheme.unsupported", new Object[]{method}));
               }

               if (handler == null) {
                  handler = (BFoxClientAuthnHandler)authenticationScheme.getAgentOn(BFoxClientAuthnHandler.class);
                  if (handler != null) {
                     handler.setData(usernameCredential);
                  }
               }

               if (handler == null
                  || !handler.getType().equals(((BFoxClientAuthnHandler)authenticationScheme.getAgentOn(BFoxClientAuthnHandler.class)).getType())) {
                  handler = (BFoxClientAuthnHandler)authenticationScheme.getAgentOn(BFoxClientAuthnHandler.class);
                  if (handler != null) {
                     handler.setData(usernameCredential);
                  }
               }

               if (credentials instanceof BHttpFoxCredentials && handler != null) {
                  handler.setData(credentials);
               }

               if (handler == null) {
                  throw new AuthenticationException(bFoxSession, LoginFailureCause.LOGIN_INTERFACE_NOT_SUPPORTED);
               }

               handler.handleAuthentication(session, authenticationClient);
               retry = receiveWelcome(session, method);
            } while (retry);

            String foxVer = session.getRemoteHello().getString("fox.version", null);
            if (foxVer != null && new Version(foxVer).compareTo(FoxSession.FOX_VERSION_1_0_2) >= 0) {
               session.setState("client.tune receiveHello");
               session.receiveHello();
            }

            if (handler != null) {
               handler.success(session);
            }

            if (usernameCredential instanceof BKerberosCredentials) {
               ((BKerberosCredentials)usernameCredential).destroyTicket();
            }

            session.setState("client.tune starting");
            session.start();
            return session;
         }
      } catch (Error var27) {
         if (handler != null) {
            handler.failure(session);
         }

         session.close(var27);
         throw var27;
      } catch (FoxAuthenticationException var28) {
         String failReason = null;
         if (var28.data != null) {
            FoxMessage fm = var28.data;

            try {
               failReason = fm.getString("authfail_reason");
            } catch (IOException var26) {
            }

            if (failReason != null && failReason.equals("User Lockout")) {
               if (handler != null) {
                  handler.failure(session);
               }

               FoxUserLockoutException ule = new FoxUserLockoutException(session);
               session.close(ule);
               throw new FoxUserLockoutException(session);
            }
         }

         if (handler != null) {
            handler.failure(session);
         }

         session.close(var28);
         throw var28;
      } catch (Exception var29) {
         if (handler != null) {
            handler.failure(session);
         }

         session.close(var29);
         throw var29;
      }
   }

   private static boolean receiveWelcome(FoxSession session, String method) throws IOException, FoxAuthenticationException {
      session.setState("client.tune receiveWelcome");
      FoxFrame frame = session.readFrame();
      if (Objects.equals(frame.command, "retry")) {
         return true;
      } else if (!Objects.equals(frame.command, "welcome")) {
         session.setState("client.tune receivedRejected");
         String fatal = frame.message.getString("fatal", null);
         String msg = frame.message.getString("msg", null);
         FoxAuthenticationException e;
         if (null != msg) {
            e = new FoxAuthenticationException(msg, method, fatal, session);
         } else {
            e = new FoxAuthenticationException("Rejected", method, fatal, session);
         }

         try {
            e.data = frame.message.getMessage("data");
         } catch (IOException var7) {
            e.data = null;
         }

         throw e;
      } else {
         session.remoteWelcome = frame.message;
         session.setState("client.tune receivedWelcome");
         return false;
      }
   }

   static void openServer(FoxServer server, Socket socket, String scheme) throws Exception {
      FoxSession session = null;

      try {
         session = new FoxSession(socket, null);
         int max = Math.max(1, Fox.maxServerSessions);
         if (Fox.getServerSessionCount() >= max) {
            LOGGER.warning("past fox.maxServerSessions limit: " + max);
            session.sendBusy();
         }
      } catch (Throwable var6) {
         if (session != null) {
            session.release();
         }

         throw var6;
      }

      Fox.register(session);

      try {
         session.setState("server.tune spawning");
         new Tuner(server, session, scheme).start();
      } catch (Exception var5) {
         session.close(var5);
         throw var5;
      }
   }

   private Tuner(FoxServer server, FoxSession session, String scheme) {
      super("Fox:Tuner:" + SecurityUtil.calculateSessionIdHash(session.getId()));
      this.server = server;
      this.session = session;
      this.scheme = scheme;
   }

   @Override
   public final void run() {
      try {
         this.session.setState("server.tune receiveHello");

         try {
            this.session.receiveHello();
         } catch (IncompatibleVersionException var24) {
            FoxMessage rejected = new FoxMessage();
            rejected.add("fatal", var24.getMessage());
            rejected.add("fox.version", "Niagara 4");
            this.session.setState("server.tune sendHello");
            this.session.sendTuning("hello", rejected);
            this.session.close(var24);
            return;
         }

         BFoxService service = (BFoxService)Sys.getService(BFoxService.TYPE);
         String foxVer = this.session.getRemoteHello().getString("fox.version", null);
         if (!service.allowLegacyClients() && (foxVer == null || new Version(foxVer).compareTo(FoxSession.FOX_VERSION_1_0_2) < 0)) {
            FoxMessage rejected = new FoxMessage();
            rejected.add("fatal", "Legacy FOX clients not allowed");
            rejected.add("fox.version", "Niagara 4 (Legacy clients not allowed)");
            this.session.setState("server.tune sendHello");
            this.session.sendTuning("hello", rejected);
            this.session.close(new IncompatibleVersionException("Legacy FOX clients not allowed"));
            return;
         }

         this.session.setState("server.tune makeConnection");
         this.session.conn = this.server.makeConnection(this.session, this.session.getRemoteHello());
         if (this.scheme.equalsIgnoreCase("fox") && service.getFoxsOnly() && service.getFoxsEnabled()) {
            this.session.setState("server.tune sendRedirect");
            this.session.sendRedirect(service.getFoxsPort().getPublicServerPort());
            throw new FoxsRedirectException(service.getFoxsPort().getPublicServerPort());
         }

         try {
            Acceptor.accept(this.session);
         } catch (Throwable var23) {
            FoxMessage msg = this.session.initHello(null, null, this.session.getId(), null, this.session.getLegacyId(), false);
            msg.add("exception", var23.getMessage());
            this.session.sendTuning("hello", msg);
            throw var23;
         }

         this.session.setState("server.tune sendHello");
         boolean includeLegacyAttributes = false;
         if (service.allowLegacyClients()
            && foxVer != null
            && new Version(foxVer).compareTo(FoxSession.FOX_VERSION_1_0_2) < 0
            && this.session.getRemoteHello().getOptional("app.name") != null
            && this.session.getRemoteHello().getOptional("app.version") != null
            && this.session.getRemoteHello().getOptional("vm.name") != null
            && this.session.getRemoteHello().getOptional("vm.version") != null
            && this.session.getRemoteHello().getOptional("os.name") != null
            && this.session.getRemoteHello().getOptional("os.version") != null) {
            includeLegacyAttributes = true;
         }

         FoxMessage msg = this.session.initHello(null, null, this.session.getId(), null, this.session.getLegacyId(), includeLegacyAttributes);
         BAuthenticationService authnService = (BAuthenticationService)Sys.getService(BAuthenticationService.TYPE);
         if (authnService.getStrictAuthentication()) {
            List<BAuthenticationScheme> availableSchemes = authnService.getSchemesWithAgent(BFoxClientAuthnHandler.class);
            StringJoiner schemes = new StringJoiner(",");

            for (BAuthenticationScheme scheme : availableSchemes) {
               schemes.add(scheme.getName());
            }

            msg.add("availableSchemes", schemes.toString());
            msg.add("defaultScheme", authnService.getDefaultSchemeWithAgent(BFoxClientAuthnHandler.class).getName());
         }

         this.session.sendTuning("hello", msg);
         BAuthenticationScheme authnScheme = null;
         BUser requestedUser = null;
         Boolean kerbKey = false;
         boolean unknownUser = false;
         if (this.session.isLegacyConnection()) {
            authnScheme = new BLegacyDigestAuthenticationScheme();
         } else {
            boolean useKerberos = false;

            for (BAuthenticationScheme scheme : authnService.getSupportedSchemes()) {
               if (scheme.getSchemeName().equals("n4Kerberos")) {
                  useKerberos = true;
                  break;
               }
            }

            FoxMessage kerberos = new FoxMessage();
            kerberos.add("useKerberos", useKerberos);
            this.session.setState("server.tune sendKerberos");
            this.session.sendTuning("kerberos", kerberos);
            FoxMessage usernameMessage = this.session.receiveTuning("username");
            String username = usernameMessage.getString("username");
            kerbKey = usernameMessage.getBoolean("kerbKey");
            if (this.session.getRemoteHello().getString("requestedId", null) != null) {
               authnScheme = new BSessionIdAuthenticationScheme();
            } else {
               BUserService userService = (BUserService)Sys.getService(BUserService.TYPE);
               requestedUser = userService.getUser(username);
               if (authnService.getStrictAuthentication()) {
                  String selectedScheme = usernameMessage.getString("scheme", null);
                  if (selectedScheme != null) {
                     try {
                        authnScheme = authnService.getAuthenticationScheme(selectedScheme);
                        if (authnScheme.getAgentOn(BFoxClientAuthnHandler.class) == null) {
                           authnScheme = null;
                           LOGGER.warning(
                              String.format("Requested authentication scheme name <%s> does not support fox login. Falling back to default.", selectedScheme)
                           );
                        }
                     } catch (Exception var22) {
                        LOGGER.warning(String.format("Requested authentication scheme name <%s> not found. Falling back to default.", selectedScheme));
                     }
                  }

                  if (authnScheme == null) {
                     authnScheme = authnService.getDefaultSchemeWithAgent(BFoxClientAuthnHandler.class);
                  }
               } else {
                  if (requestedUser == null) {
                     requestedUser = new BUser();
                     unknownUser = true;
                  }

                  try {
                     authnScheme = userService.getAuthenticationSchemeForUser(requestedUser);
                  } catch (Exception var21) {
                     authnScheme = null;
                  }
               }
            }
         }

         List<BAuthenticationScheme> schemes;
         if (!unknownUser && (!kerbKey || authnScheme != null && authnScheme.getSchemeName().equals("n4Kerberos"))) {
            schemes = new ArrayList<>();
            if (authnScheme != null) {
               if (authnScheme.getAgentOn(BFoxCallbackHandler.class) != null) {
                  schemes.add(authnScheme);
               } else {
                  LOGGER.warning(
                     String.format("User <%s> attempting fox login with unsupported authentication scheme <%s>", requestedUser.getName(), authnScheme.getName())
                  );
               }
            }
         } else {
            schemes = authnService.getRemoteSchemes();
         }

         AuthenticationException ae = null;
         ListIterator<BAuthenticationScheme> iterator = schemes.listIterator();

         while (iterator.hasNext()) {
            BAuthenticationScheme schemex = iterator.next();
            if (schemex.getAgentOn(BFoxCallbackHandler.class) == null || kerbKey && !schemex.getSchemeName().equals("n4Kerberos")) {
               iterator.remove();
            }
         }

         if (schemes.isEmpty()) {
            schemes.add(new BDigestAuthenticationScheme());
         }

         if (schemes.isEmpty()) {
            FoxMessage challenge = new FoxMessage();
            String method = unknownUser ? "n4digest" : "foxUnsupported";
            challenge.add("method", method);
            challenge.add("useKeyExchange", false);
            this.session.setState("server.tune sendChallenge " + method);
            this.session.sendTuning("challenge", challenge);
            this.rejectAuthentication(this.session, null, ae);
         }

         for (int i = 0; i < schemes.size(); i++) {
            authnScheme = schemes.get(i);
            FoxMessage challenge = new FoxMessage();
            challenge.add("method", authnScheme.getSchemeName());
            if (!this.session.isSecure() && !this.session.isLegacyConnection()) {
               challenge.add("keyExchangeMethods", authnScheme.getKeyExchangeMethodName());
               challenge.add("keyExchangeCiphers", KeyExchange.getPreferredKeyExchangeCiphers());
            } else {
               challenge.add("keyExchangeMethods", NullAlgorithmBundle.getInstance().getAlgorithmName());
            }

            this.session.setState("server.tune sendChallenge " + authnScheme.getSchemeName());
            this.session.sendTuning("challenge", challenge);
            if (!this.session.isLegacyConnection()) {
               this.session.setState("server.tune receiveClientKeyExchange");
               FoxMessage response = this.session.receiveTuning("clientKeyExchangeMethod");
               String clientKeyExchangeMethod = response.getString("keyExchangeMethod", null);
               String clientKeyExchangeCipher = response.getString("keyExchangeCipher", null);
               KeyDerivationAlgorithmBundle keyDerivationAlgorithmBundle = null;
               EncryptionAlgorithmBundle encryptionAlgorithmBundle = null;
               if (clientKeyExchangeMethod != null) {
                  CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstance(clientKeyExchangeMethod);
                  if (bundle instanceof KeyDerivationAlgorithmBundle) {
                     keyDerivationAlgorithmBundle = (KeyDerivationAlgorithmBundle)bundle;
                  }
               }

               if (clientKeyExchangeCipher != null) {
                  CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstance(clientKeyExchangeCipher);
                  if (bundle instanceof EncryptionAlgorithmBundle) {
                     encryptionAlgorithmBundle = (EncryptionAlgorithmBundle)bundle;
                  }
               }

               if (keyDerivationAlgorithmBundle != null && encryptionAlgorithmBundle != null) {
                  this.session.setKeyExchangeAlgorithmBundle(keyDerivationAlgorithmBundle);
                  this.session.setEncryptionAlgorithmBundle(encryptionAlgorithmBundle);
                  this.session
                     .setState(
                        "server.tune receievedClientKeyExchange method="
                           + keyDerivationAlgorithmBundle.getAlgorithmName()
                           + '/'
                           + encryptionAlgorithmBundle.getAlgorithmName()
                     );
               }
            }

            BFoxCallbackHandler handler = null;

            try {
               handler = (BFoxCallbackHandler)authnScheme.getAgentOn(BFoxCallbackHandler.class);
               Objects.requireNonNull(handler);
               handler.init(this.session);
               BUser user = authnService.authenticate(this.session, requestedUser, handler, authnScheme);
               this.authenticateAttempt(this.session, handler.getUsername());
               this.server.connectionAuthenticated(this.session.conn, this.session, this.session.getRemoteHello());
               this.authenticateSuccess(this.session, user);
               this.acceptAuthentication(this.session);
               break;
            } catch (AuthenticationException var25) {
               if (i == schemes.size() - 1) {
                  if (handler != null) {
                     this.authenticateAttempt(this.session, handler.getUsername());
                  }

                  this.rejectAuthentication(this.session, null, var25);
               } else {
                  FoxMessage retry = new FoxMessage();
                  this.session.setState("server.tune sendRetry");
                  this.session.sendTuning("retry", retry);
               }
            }
         }
      } catch (Throwable var26) {
         this.session.close(var26);
      }
   }

   protected BUserService authenticateAttempt(FoxSession session, String username) {
      BFoxServerConnection conn = (BFoxServerConnection)session.conn();
      conn.setLastLoginTime(Clock.time());
      if (null != username) {
         conn.setLastLoginUsername(username);
      }

      conn.setLastLoginAddress(session.getRemoteHost() + ":" + session.getRemotePort());
      conn.setLastLoginApp(session.getRemoteHello().getString("app.name", "") + " " + session.getRemoteHello().getString("app.version", ""));
      return (BUserService)Sys.getService(BUserService.TYPE);
   }

   public void acceptAuthentication(FoxSession session) throws Exception {
      FoxMessage welcome = new FoxMessage();
      if (SecurityInitializer.getInstance().isFips()) {
         welcome.add("fips", true);
      }

      BUser user = session.getUser();
      BAuthenticationScheme scheme = user.getAuthenticationScheme();
      BAbstractAuthenticator authenticator = user.getAuthenticator();
      BUserPasswordConfiguration userPassConfig = null;
      BGlobalPasswordConfiguration[] globalPassConfig = (BGlobalPasswordConfiguration[])scheme.getChildren(BGlobalPasswordConfiguration.class);
      if (authenticator instanceof BPasswordAuthenticator) {
         userPassConfig = ((BPasswordAuthenticator)authenticator).getPasswordConfig();
      }

      if (userPassConfig != null && globalPassConfig.length > 0) {
         BAbsTime expTime = userPassConfig.getExpiration();
         BRelTime warningPeriod = globalPassConfig[0].getWarningPeriod();
         TypeInfo USER_SYNC_EXT_TYPE = BTypeSpec.make("niagaraDriver", "UserSyncExt").getTypeInfo();
         Property syncExtProp = user.getProperty("syncExt");
         boolean networkUser = user.getNetworkUser()
            && Flags.isReadonly(user.getParent(), user.getPropertyInParent())
            && syncExtProp != null
            && USER_SYNC_EXT_TYPE != null
            && syncExtProp.getType().is(USER_SYNC_EXT_TYPE);
         boolean forceReset = false;
         if (!expTime.isNull() && expTime.isBefore(BAbsTime.now())) {
            forceReset = true;
         } else if (!expTime.isNull() && expTime.subtract(warningPeriod).isBefore(BAbsTime.now())) {
            welcome.add("passwordExpires", expTime.getMillis());
            welcome.add("networkUser", networkUser);
         }

         forceReset = userPassConfig.getForceResetAtNextLogin() || forceReset;
         if (forceReset) {
            welcome.add("forceReset", true);
            BPasswordStrength strength = globalPassConfig[0].getPasswordStrength();
            welcome.add("minLength", strength.getMinimumLength());
            welcome.add("minLowerCase", strength.getMinimumLowerCase());
            welcome.add("minUpperCase", strength.getMinimumUpperCase());
            welcome.add("minDigits", strength.getMinimumDigits());
            welcome.add("minSpecial", strength.getMinimumSpecial());
            welcome.add("maxLength", strength.getMaximumLength());
            welcome.add("illegalNetworkUserState", networkUser);
         }
      }

      session.setState("server.tune sendWelcome");
      session.sendTuning("welcome", welcome);
      String foxVersion = session.getRemoteHello().getString("fox.version", null);
      if (foxVersion != null && new Version(foxVersion).compareTo(FoxSession.FOX_VERSION_1_0_2) >= 0) {
         session.setState("server.tune sendHello");

         try {
            Acceptor.accept(session);
         } catch (Throwable var17) {
            FoxMessage msg = session.initHello(null, null, session.getId(), null, session.getLegacyId(), true);
            msg.add("exception", var17.getMessage());
            session.sendTuning("hello", msg);
            throw var17;
         }

         session.sendHello(null, true);
      } else {
         Acceptor.accept(session);
      }

      session.setState("server.tune starting");

      try {
         AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
            try {
               Subject subject = SessionManager.getAuthenticatedSubjectFromSession(session.getSuperId());
               Subject.doAs(subject, (PrivilegedExceptionAction<Void>)(() -> {
                  session.start();
                  return null;
               }));
               return null;
            } catch (PrivilegedActionException var2x) {
               throw var2x.getException();
            }
         }));
      } catch (PrivilegedActionException var16) {
         throw var16.getException();
      }
   }

   public void rejectAuthentication(FoxSession session, Throwable fatal, Throwable msg) throws Exception {
      FoxMessage rejected = this.generateRejectedMessage(fatal, msg, session.getSessionContext());
      session.setState("server.tune sendRejected");
      session.sendTuning("rejected", rejected);
      throw new FoxAuthenticationException("client login failed", session);
   }

   protected FoxMessage generateRejectedMessage(Throwable fatal, Throwable msg, Context cx) {
      FoxMessage rejected = new FoxMessage();
      if (fatal != null) {
         rejected.add("fatal", this.toFatalMessage(fatal, cx));
         if (fatal instanceof FoxAuthenticationException) {
            rejected.add("data", ((FoxAuthenticationException)fatal).data);
         }
      }

      if (msg != null) {
         rejected.add("msg", this.toFatalMessage(msg, cx));
         if (fatal instanceof FoxAuthenticationException) {
            rejected.add("data", ((FoxAuthenticationException)fatal).data);
         }

         if (msg instanceof AuthenticationException) {
            String d = msg.getMessage();
            FoxMessage dataMessage = new FoxMessage();
            dataMessage.add("authfail_reason", d);
            rejected.add("data", dataMessage);
         }
      }

      return rejected;
   }

   public String toFatalMessage(Throwable e, Context cx) {
      if (e instanceof Localizable) {
         return ((Localizable)e).toString(cx);
      } else {
         String s = e.getClass().getName();
         int dot = s.lastIndexOf(46);
         if (dot > 0) {
            s = s.substring(dot + 1);
         }

         String msg = e.getMessage();
         if (msg != null && msg.length() > 0) {
            s = s + ": " + msg;
         }

         return s;
      }
   }

   protected void authenticateSuccess(FoxSession session, BUser user) {
      synchronized (session.getSessionStateLock()) {
         FoxMessage hello = session.getRemoteHello();
         String lang = hello.getString("lang", null);
         String vmUuid = hello.getString("vmUuid", null);
         Map<String, BIDataValue> map = new HashMap<>();
         map.put("foxSessionId", BString.make(session.getId()));
         if (vmUuid != null) {
            map.put("foxRemoteVmUuid", BString.make(vmUuid));
         }

         map.put("skipEncodingSensitive", BBoolean.TRUE);
         session.setUser(user);
         session.setSessionContext(new BasicContext(new BasicContext(user, lang), BFacets.make(map)));
      }
   }

   private static void addExtraAttributes(BICredentials credentials, FoxMessage message, FoxSession session) throws FoxAuthenticationException {
      if (credentials instanceof BIExtraAttributesCredentials) {
         Map<String, Object> extraAttributes = ((BIExtraAttributesCredentials)credentials).getExtraAttributes();

         for (Entry<String, Object> entry : extraAttributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Boolean) {
               message.add(key, (Boolean)value);
            } else if (value instanceof Integer) {
               message.add(key, (Integer)value);
            } else if (value instanceof Double) {
               message.add(key, (Double)value);
            } else if (value instanceof String) {
               message.add(key, (String)value);
            } else if (value instanceof Long) {
               message.add(key, (Long)value);
            } else {
               if (!(value instanceof byte[])) {
                  throw new FoxAuthenticationException("Credentials supplied extra attribute of unsupported type: " + value.getClass(), session);
               }

               message.add(key, (byte[])value);
            }
         }
      }
   }
}
