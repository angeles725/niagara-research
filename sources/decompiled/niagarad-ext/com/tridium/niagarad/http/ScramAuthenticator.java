package com.tridium.niagarad.http;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.crypto.core.exchange.IKeyExchanger;
import com.tridium.crypto.core.exchange.KeyExchange;
import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.security.AuthenticationDomain;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.niagarad.security.SimpleAuthenticationDomain;
import com.tridium.niagarad.security.os.NativeAuthIdentity;
import com.tridium.niagarad.security.os.NativeAuthenticationDomain;
import com.tridium.niagarad.servlet.DebugServlet;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.nre.auth.BCrypt;
import com.tridium.nre.auth.BCryptAlgorithmBundle;
import com.tridium.nre.auth.GlibcSha256CryptAlgorithmBundle;
import com.tridium.nre.auth.GlibcSha512CryptAlgorithmBundle;
import com.tridium.nre.auth.PasswordHashAlgorithm;
import com.tridium.nre.auth.Pbkdf2;
import com.tridium.nre.auth.QnxPlatformAlgorithmBundle;
import com.tridium.nre.auth.Scram;
import com.tridium.nre.auth.ScramAlgorithmBundle;
import com.tridium.nre.auth.ScramServer;
import com.tridium.nre.auth.Sha256Crypt;
import com.tridium.nre.auth.Sha512Crypt;
import com.tridium.nre.auth.ScramServer.IUserKeyFactory;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.security.EncryptionAlgorithmBundle;
import com.tridium.nre.security.KeyDerivationAlgorithmBundle;
import com.tridium.nre.security.NullAlgorithmBundle;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.util.IPAddressUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

public final class ScramAuthenticator extends Authenticator {
   private static final String HTTP_SESSION_ATTR_SCRAM = "scramServer";
   private static final String HTTP_SESSION_ATTR_USERNAME = "username";
   private static final String ACTION_CLIENT_FIRST_SCRAM_MESSAGE = "sendClientFirstMessage";
   private static final String ACTION_CLIENT_FINAL_SCRAM_MESSAGE = "sendClientFinalMessage";
   private static final String ACTION_CLIENT_FIRST_SRP6_MESSAGE = "sendClientFirstSrp6Message";
   private static final String ACTION_CLIENT_FINAL_SRP6_MESSAGE = "sendClientFinalSrp6Message";
   private static final String KEY_CLIENT_FIRST_SCRAM_MESSAGE = "clientFirstMessage";
   private static final String KEY_CLIENT_FINAL_SCRAM_MESSAGE = "clientFinalMessage";
   private static final String KEY_CLIENT_FIRST_SRP6_MESSAGE = "keyExchangeClientA";
   private static final String KEY_CLIENT_FINAL_SRP6_MESSAGE = "keyExchangeM1";
   private static final String ACTION_SERVER_AUTH_FAILED = "authenticationFailed";
   private static final String KEY_SERVER_AUTHN_FAILURE_CAUSE = "authnFailureCause";
   private final WeakHashMap<HttpSession, ScramAuthenticator.KeyExchangeSettings> sessionKeyExchanges = new WeakHashMap<>();
   private final String authScheme;
   private final String authType;
   private final ScramAlgorithmBundle algorithmBundle;
   private final AuthenticationDomain authDomain;
   private final IPlatformProvider platformProvider;
   private final int defaultSaltLength;
   private final int defaultIterationCount;
   private static final Logger log = Logger.getLogger("auth.scram");

   public ScramAuthenticator(AuthenticationDomain authDomain, String authScheme, IPlatformProvider platformProvider) {
      this.authDomain = authDomain;
      this.authScheme = authScheme;
      this.authType = authScheme + "/" + authDomain.getDomainType();
      this.platformProvider = platformProvider;
      switch (this.authType) {
         case "scram-bcrypt/native":
            this.algorithmBundle = BCryptAlgorithmBundle.getInstance();
            this.defaultIterationCount = 10;
            this.defaultSaltLength = 16;
            break;
         case "scram-glibc-sha256/native":
            this.algorithmBundle = GlibcSha256CryptAlgorithmBundle.getInstance();
            this.defaultIterationCount = 5000;
            this.defaultSaltLength = 16;
            break;
         case "scram-glibc-sha512/native":
            this.algorithmBundle = GlibcSha512CryptAlgorithmBundle.getInstance();
            this.defaultIterationCount = 5000;
            this.defaultSaltLength = 16;
            break;
         case "scram-glibc-sha512/file":
            this.algorithmBundle = GlibcSha512CryptAlgorithmBundle.getInstance();
            this.defaultIterationCount = 10000;
            this.defaultSaltLength = 16;
            break;
         case "scram-sha512/native":
            this.algorithmBundle = QnxPlatformAlgorithmBundle.getInstance();
            this.defaultIterationCount = 10000;
            this.defaultSaltLength = 32;
            break;
         default:
            throw new UnsupportedOperationException("Unknown SCRAM hash algorithm \"" + this.authType + "\"");
      }
   }

   public ScramAlgorithmBundle getAlgorithmBundle() {
      return this.algorithmBundle;
   }

   @Override
   public AuthenticationDomain getAuthDomain() {
      return this.authDomain;
   }

   @Override
   public String getAuthScheme() {
      return this.authScheme;
   }

   @Override
   public String getAuthType() {
      return this.authType;
   }

   @Override
   public Logger getLog() {
      return log;
   }

   @Override
   public AuthenticationInfo makeAuthInfo(HttpServletRequest req, HttpServletResponse resp) {
      HttpSession session = req.getSession(false);
      boolean challengeOnAuthFailure = true;
      Authenticator.RequestCredentials credentials = null;
      if (session == null) {
         if (log.isLoggable(Level.FINEST)) {
            log.finest("request for uri '" + TextUtil.truncate(req.getRequestURI(), 25) + "' does not have a session, request authentication required");
         }
      } else {
         if (log.isLoggable(Level.FINEST)) {
            log.finest(
               "request for uri '"
                  + TextUtil.truncate(req.getRequestURI(), 25)
                  + "' using session '"
                  + SecurityUtil.calculateSessionIdHash(session.getId())
                  + "'"
            );
         }

         if (this.detectSessionFixation(req, log)) {
            log.warning("possible session fixation detected, rejecting request and invalidating session");
            challengeOnAuthFailure = false;
         } else {
            ScramServer authServer = (ScramServer)session.getAttribute("scramServer");
            if (authServer == null) {
               log.finest("server not found in session");
            } else {
               log.finest("found server");
               if (!authServer.isAuthenticated()) {
                  log.finest("server not authenticated");
               } else {
                  credentials = this.getRequestCredentials(req);
                  if (credentials == null) {
                     log.finest("credentials not found");
                  } else {
                     label100: {
                        AuthenticationInfo authInfo;
                        if (this.authDomain instanceof NativeAuthenticationDomain) {
                           if (this.isStationConnection(req, credentials.getUsername())) {
                              authInfo = this.authDomain.makeAuthInfo(credentials.getUsername(), credentials.getPassword());
                           } else {
                              NativeAuthenticationDomain nativeAuthDomain = (NativeAuthenticationDomain)this.authDomain;
                              NativeAuthIdentity nativeIdentity = new NativeAuthIdentity(
                                 credentials.getUsername(), null, this.authDomain.getRealm(req), this.platformProvider
                              );
                              authInfo = nativeAuthDomain.makeAuthInfo(nativeIdentity, false, this.platformProvider);
                           }
                        } else {
                           if (!(this.authDomain instanceof SimpleAuthenticationDomain)) {
                              if (log.isLoggable(Level.FINEST)) {
                                 log.finest("unrecognized authentication domain '" + this.authDomain.getDomainType() + "', credentials not authenticated");
                              }
                              break label100;
                           }

                           authInfo = this.authDomain.makeAuthInfo(credentials.getUsername(), credentials.getPassword());
                        }

                        if (authInfo == null) {
                           log.finest("could not create authentication information for request, credentials not authenticated");
                        } else if (!this.validateExtraConditions(authInfo, req)) {
                           log.finest("extra conditions not met");
                        } else {
                           log.finest("server authenticated");
                           ScramAuthenticator.KeyExchangeSettings keyExchangerSettings = this.sessionKeyExchanges.get(session);
                           if (keyExchangerSettings == null) {
                              log.finest("key exchange settings not found for session, authenticating request");
                           } else {
                              if (keyExchangerSettings.keyExchangerServer == null || keyExchangerSettings.keyExchangerServer.getKey() != null) {
                                 if (!CsrfTokenUtil.csrfTokenExists(req)) {
                                    String csrfToken = CsrfTokenUtil.getCsrfToken(req);
                                    if (log.isLoggable(Level.FINEST)) {
                                       log.finest(
                                          "adding CSRF token '" + csrfToken + "' to session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "'"
                                       );
                                    }

                                    resp.setHeader("DaemonCSRFToken", csrfToken);
                                 }

                                 if (req.getHeader("Authorization") != null && !session.isNew()) {
                                    String newSessionId = req.changeSessionId();
                                    if (log.isLoggable(Level.FINEST)) {
                                       log.finest("authorization header present, rotated session id to '" + newSessionId + "'");
                                    }
                                 }

                                 NiagaraDaemon.getInstance().webServer.sessionAuthenticated(session, authInfo, req);
                                 log.finest("authentication completed successfully");
                                 return authInfo;
                              }

                              log.finest("key exchange not complete");
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      NiagaraDaemon.getInstance().webServer.sessionRejected(session, credentials != null ? credentials.getUsername() : "", req);
      if (challengeOnAuthFailure) {
         log.finest("client not authenticated, challenging");
         this.challenge(req, resp);
      } else {
         this.invalidateSession(req);
         Http.sendError(req, resp, 401);
      }

      return null;
   }

   @Override
   public Authenticator.RequestCredentials getRequestCredentials(HttpServletRequest req) {
      HttpSession session = req.getSession(false);
      if (session == null) {
         return null;
      } else {
         String username = (String)session.getAttribute("username");
         if (username == null || username.length() == 0) {
            return null;
         } else if (this.authDomain == null) {
            return null;
         } else {
            return !(this.authDomain instanceof SimpleAuthenticationDomain) && !this.isStationConnection(req, username)
               ? new Authenticator.RequestCredentials(username, null)
               : new Authenticator.RequestCredentials(username, this.authDomain.getPasswordHash(username));
         }
      }
   }

   @Override
   public void challenge(HttpServletRequest req, HttpServletResponse resp) {
      String realm = this.authDomain.getRealm(req);
      String authHeader = req.getHeader("Authorization");
      if (authHeader == null) {
         this.invalidateSession(req);
         if (Http.isNiagaraClient(req)) {
            String initialKeyExchangeMessage = generateInitialKeyExchangeMessage(req);
            if (log.isLoggable(Level.FINEST)) {
               log.finest("sent server key exchange request: " + initialKeyExchangeMessage);
            }

            resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" " + initialKeyExchangeMessage);
         } else if (DebugServlet.debugEnabled) {
            try {
               String loginUri = "/login";
               String originalUri = req.getRequestURI();
               if (originalUri != null && !originalUri.startsWith(loginUri)) {
                  String originalAddress = req.getLocalAddr();
                  if (originalAddress.startsWith("[") && originalAddress.endsWith("]")) {
                     originalAddress = originalAddress.substring(1, originalAddress.length() - 1);
                  }

                  InetAddress address = IPAddressUtil.getLocalHost(IPAddressUtil.numericStringToInetAddress(originalAddress));
                  String redirectAddress = address.getHostAddress();
                  if (address instanceof Inet6Address) {
                     redirectAddress = "[" + redirectAddress + "]";
                  }

                  WebServer server = NiagaraDaemon.getInstance().webServer;
                  if (req.getLocalPort() != server.getHttpsPort()) {
                     resp.sendRedirect("http://" + redirectAddress + ":" + server.getHttpPort() + loginUri);
                  } else {
                     resp.sendRedirect("https://" + redirectAddress + ":" + server.getHttpsPort() + loginUri);
                  }
               }
            } catch (IOException e) {
               log.severe("error redirecting to platform login page (" + e + ")");
            }
         }
      } else {
         Hashtable<String, String> params = new Hashtable<>();
         String[] pairs = TextUtil.split(authHeader, ' ');
         if (pairs.length != 0 && this.getAuthScheme().equalsIgnoreCase(pairs[0])) {
            try {
               for (int i = 1; i < pairs.length; i++) {
                  int index = pairs[i].indexOf(61);
                  String key = pairs[i].substring(0, index);
                  String value = pairs[i].substring(index + 1);
                  params.put(decodeUrl(key), value);
               }
            } catch (IndexOutOfBoundsException ignored) {
               resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
               this.invalidateSession(req);
               return;
            }

            String clientAction = params.get("action");
            switch (clientAction) {
               case "sendClientFirstMessage":
                  if (log.isLoggable(Level.FINEST)) {
                     log.finest("scram step 1: sendClientFirstMessage");
                  }

                  String clientFirstMessage = params.get("clientFirstMessage");
                  String username = ScramServer.extractUsername(clientFirstMessage);
                  if (username == null || username.length() == 0) {
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  ScramAuthenticator.KeyExchangeSettings keyExchangeSettingsForSession = new ScramAuthenticator.KeyExchangeSettings();
                  String serverKeyExchangeConfirmation = processKeyExchangeFinalResponse(req, keyExchangeSettingsForSession, params);
                  boolean enforceKeyExchange = true;
                  if (!Http.isNiagaraClient(req)) {
                     enforceKeyExchange = !DebugServlet.debugEnabled;
                  }

                  if (req.isSecure() || DaemonAuthUtil.isLocalConnection(req)) {
                     enforceKeyExchange = false;
                  }

                  if (log.isLoggable(Level.FINEST)) {
                     log.finest("do key exchange: " + keyExchangeSettingsForSession.doSecureKeyExchange);
                     log.finest("enforce key exchange: " + enforceKeyExchange);
                  }

                  if (!keyExchangeSettingsForSession.doSecureKeyExchange && enforceKeyExchange) {
                     log.fine("failed authentication attempt: client does not support secure key exchange");
                     String failureCause = "Client does not support required secure key exchange";

                     try {
                        failureCause = URLEncoder.encode(failureCause, "UTF-8");
                     } catch (UnsupportedEncodingException var36) {
                     }

                     resp.setHeader(
                        "WWW-Authenticate",
                        this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed" + " " + "authnFailureCause" + "=" + failureCause
                     );
                     this.invalidateSession(req);
                     return;
                  }

                  ScramAuthenticator.HttpUserKeyFactory userKeyFactory = new ScramAuthenticator.HttpUserKeyFactory();
                  ScramServer authServer = new ScramServer(this.getAlgorithmBundle(), userKeyFactory);
                  boolean userExists = false;

                  String serverFirstMessage;
                  try {
                     serverFirstMessage = authServer.createServerFirstMessage(clientFirstMessage);
                     userExists = true;
                  } catch (Exception exception) {
                     if (userKeyFactory.getUserKey(username) != null) {
                        resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                        this.invalidateSession(req);
                        NiagaraDaemon.getInstance().webServer.sessionRejected(null, username != null ? username : "", req);
                        return;
                     }

                     serverFirstMessage = createDefaultServerFirstMessage(clientFirstMessage, this.defaultSaltLength, this.defaultIterationCount);
                  }

                  if (keyExchangeSettingsForSession.doSecureKeyExchange) {
                     log.finest("initializing key exchange server...");
                     keyExchangeSettingsForSession.keyExchangerServer = KeyExchange.makeServer(keyExchangeSettingsForSession.keyDerivationAlgorithmBundle);
                     keyExchangeSettingsForSession.keyExchangerServer.init();
                     if (userExists) {
                        try {
                           SecretBytes saltedPassword = this.getSaltedPasswordBytes(username);
                           Throwable var20 = null;

                           try {
                              keyExchangeSettingsForSession.keyExchangerServer.doInitialStep(saltedPassword);
                           } catch (Throwable var37) {
                              var20 = var37;
                              throw var37;
                           } finally {
                              if (saltedPassword != null) {
                                 if (var20 != null) {
                                    try {
                                       saltedPassword.close();
                                    } catch (Throwable var34) {
                                       var20.addSuppressed(var34);
                                    }
                                 } else {
                                    saltedPassword.close();
                                 }
                              }
                           }
                        } catch (Exception e) {
                           resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                           this.invalidateSession(req);
                           return;
                        }
                     }

                     log.finest("key exchange server initialized");
                  }

                  HttpSession session = this.createNewSession(req, false);
                  if (log.isLoggable(Level.FINEST)) {
                     log.finest("created new session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "' for request");
                  }

                  session.setAttribute("scramServer", authServer);
                  session.setAttribute("username", username);
                  if (log.isLoggable(Level.FINEST)) {
                     log.finest("saving key exchange settings to session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "'");
                  }

                  this.sessionKeyExchanges.put(session, keyExchangeSettingsForSession);
                  resp.setHeader(
                     "WWW-Authenticate",
                     this.getAuthScheme()
                        + " realm=\""
                        + realm
                        + "\" action=sendServerFirstMessage serverFirstMessage="
                        + serverFirstMessage
                        + serverKeyExchangeConfirmation
                  );
                  break;
               case "sendClientFinalMessage":
                  if (log.isLoggable(Level.FINEST)) {
                     log.finest("scram step 2: sendClientFinalMessage");
                  }

                  String clientFinalMessage = params.get("clientFinalMessage");
                  HttpSession requestSession = req.getSession(false);
                  if (requestSession == null) {
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  ScramServer authServer = (ScramServer)requestSession.getAttribute("scramServer");
                  if (authServer == null) {
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  String username = (String)requestSession.getAttribute("username");
                  if (username == null) {
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  String serverFinalMessage;
                  try {
                     serverFinalMessage = authServer.createServerFinalMessage(clientFinalMessage);
                  } catch (Exception ignored) {
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     NiagaraDaemon.getInstance().webServer.sessionRejected(null, username != null ? username : "", req);
                     return;
                  }

                  resp.setHeader(
                     "WWW-Authenticate",
                     this.getAuthScheme() + " realm=\"" + realm + "\" action=sendServerFinalMessage serverFinalMessage=" + serverFinalMessage
                  );
                  this.promoteSessionReference(requestSession);
                  break;
               case "sendClientFirstSrp6Message":
                  if (log.isLoggable(Level.FINEST)) {
                     log.finest("key exchange step 1: sendClientFirstSrp6Message");
                  }

                  String clientFirstSrp6Message = params.get("keyExchangeClientA");
                  byte[] A = Base64.getDecoder().decode(clientFirstSrp6Message);
                  ScramAuthenticator.KeyExchangeSettings keyExchangeSettings = this.sessionKeyExchanges.get(req.getSession(false));
                  if (keyExchangeSettings == null) {
                     log.finest("missing key exchange settings for session in step 1");
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  log.finest("generating server first SRP6 message");
                  byte[] B = keyExchangeSettings.keyExchangerServer.doExchangeStep(A);
                  if (B == null) {
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  String serverFirstMessage = Base64.getEncoder().encodeToString(B);
                  resp.setHeader(
                     "WWW-Authenticate",
                     this.getAuthScheme() + " realm=\"" + realm + "\" action=sendServerFirstSrp6Message keyExchangeServerB=" + serverFirstMessage
                  );
                  break;
               case "sendClientFinalSrp6Message":
                  if (log.isLoggable(Level.FINEST)) {
                     log.finest("key exchange step 2: sendClientFinalSrp6Message");
                  }

                  String clientFinalSrp6Message = params.get("keyExchangeM1");
                  byte[] M1 = Base64.getDecoder().decode(clientFinalSrp6Message);
                  ScramAuthenticator.KeyExchangeSettings keyExchangeSettings = this.sessionKeyExchanges.get(req.getSession(false));
                  if (keyExchangeSettings == null) {
                     log.finest("missing key exchange settings for session in step 2");
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  log.finest("generating server final SRP6 message");
                  byte[] M2 = keyExchangeSettings.keyExchangerServer.doExchangeStep(M1);
                  if (M2 == null) {
                     resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                     this.invalidateSession(req);
                     return;
                  }

                  String serverFinalMessage = Base64.getEncoder().encodeToString(M2);
                  resp.setHeader(
                     "WWW-Authenticate",
                     this.getAuthScheme() + " realm=\"" + realm + "\" action=sendServerFinalSrp6Message keyExchangeM2=" + serverFinalMessage
                  );
                  break;
               default:
                  resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
                  this.invalidateSession(req);
            }
         } else {
            resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + realm + "\" action=" + "authenticationFailed");
            this.invalidateSession(req);
         }
      }
   }

   private SecretBytes getSaltedPasswordBytes(String username) throws Exception {
      String key = createScramSpecification(this.authDomain.getPasswordHash(username));
      if (key == null) {
         throw new Exception();
      } else {
         int index = key.lastIndexOf(":");
         if (index != -1 && index != key.length() - 1) {
            return new SecretBytes(ByteArrayUtil.hexStringToBytes(key.substring(index + 1)), true);
         } else {
            throw new Exception();
         }
      }
   }

   public String getPasswordHash(String clearPassword, byte[] salt) throws Exception {
      PasswordHashAlgorithm passwordHashAlgorithm = this.algorithmBundle.getKeyDerivationAlgorithmType();
      String resultingPasswordHash;
      switch (passwordHashAlgorithm) {
         case pbkdf2:
            byte[] processedSalt = TextUtil.bytesToHexString(salt).getBytes(StandardCharsets.UTF_8);
            byte[] saltedPassword = Pbkdf2.deriveKey(processedSalt, 10000, clearPassword, this.algorithmBundle);
            resultingPasswordHash = "@S,10000@" + Base64.getEncoder().encodeToString(saltedPassword) + "@" + Base64.getEncoder().encodeToString(processedSalt);
            break;
         case glibc_sha256:
            resultingPasswordHash = Sha256Crypt.Sha256_crypt(clearPassword, new String(salt), 10000);
            break;
         case glibc_sha512:
            resultingPasswordHash = Sha512Crypt.Sha512_crypt(clearPassword, new String(salt), 10000);
            break;
         case bcrypt:
            resultingPasswordHash = BCrypt.hashpw(clearPassword, "$2$10$" + new String(salt));
            break;
         default:
            throw new UnsupportedOperationException("Unknown password hash algorithm '" + passwordHashAlgorithm + "'");
      }

      return resultingPasswordHash;
   }

   private static String createScramSpecification(String rawPasswordHash) {
      if (rawPasswordHash != null && rawPasswordHash.length() != 0) {
         String scramString;
         try {
            boolean isQNX = false;
            boolean isQNXDES = false;
            char tokenCharacter = ' ';
            if (rawPasswordHash.startsWith("$")) {
               tokenCharacter = '$';
            } else if (rawPasswordHash.startsWith("@")) {
               isQNX = true;
               tokenCharacter = '@';
            } else {
               if (rawPasswordHash.length() != 13) {
                  throw new UnsupportedOperationException("Unsupported shadow hash token '" + rawPasswordHash.charAt(0) + "'");
               }

               isQNX = true;
               isQNXDES = true;
            }

            int currentToken = 0;
            String[] hashTokens = null;
            String hashSpecification;
            if (!isQNXDES) {
               hashTokens = TextUtil.split(rawPasswordHash.substring(1), tokenCharacter);
               currentToken = 0;
               hashSpecification = hashTokens[currentToken++];
               if (hashSpecification.contains(",")) {
                  hashSpecification = hashSpecification.substring(0, hashSpecification.indexOf(","));
               }
            } else {
               hashSpecification = "d";
            }

            String algorithm;
            String salt;
            String iterations;
            String hash;
            switch (hashSpecification) {
               case "d":
               case "m":
               case "s":
               case "1":
                  throw new UnsupportedOperationException("Explicitly unsupported shadow hash algorithm \"" + hashSpecification + "\"");
               case "S": {
                  assert hashTokens != null;
                  algorithm = String.valueOf(PasswordHashAlgorithm.pbkdf2.ordinal());
                  iterations = String.valueOf(4096);
                  if (hashTokens[0].contains(",")) {
                     iterations = TextUtil.split(hashTokens[0], ',')[1];
                  }

                  String rawHash = hashTokens[currentToken++];
                  hash = TextUtil.bytesToHexString(Base64.getDecoder().decode(rawHash));
                  String rawSalt = hashTokens[currentToken];
                  String unencodedSalt = new String(Base64.getDecoder().decode(rawSalt));
                  salt = TextUtil.bytesToHexString(unencodedSalt.getBytes(StandardCharsets.UTF_8));
                  break;
               }
               case "2a": {
                  assert hashTokens != null;
                  algorithm = String.valueOf(PasswordHashAlgorithm.bcrypt.ordinal());
                  iterations = hashTokens[currentToken++];
                  String rawSalt = hashTokens[currentToken].substring(0, 23);
                  salt = TextUtil.bytesToHexString(rawSalt.getBytes(StandardCharsets.UTF_8));
                  String rawHash = hashTokens[currentToken].substring(23);
                  hash = TextUtil.bytesToHexString(rawHash.getBytes(StandardCharsets.UTF_8));
                  break;
               }
               case "5":
               case "6":
                  assert hashTokens != null;
                  algorithm = String.valueOf(
                     "5".equalsIgnoreCase(hashSpecification) ? PasswordHashAlgorithm.glibc_sha256.ordinal() : PasswordHashAlgorithm.glibc_sha512.ordinal()
                  );
                  iterations = String.valueOf(5000);
                  if (hashTokens.length == 4) {
                     String roundsSpec = hashTokens[currentToken++];
                     if (!roundsSpec.startsWith("rounds=")) {
                        throw new UnsupportedOperationException(
                           "Exception \"rounds=\" as second hash token, found \"" + TextUtil.truncate(roundsSpec, 7) + "\""
                        );
                     }

                     iterations = TextUtil.split(roundsSpec, '=')[1];
                  }

                  String rawSalt = hashTokens[currentToken++];
                  salt = TextUtil.bytesToHexString(rawSalt.getBytes(StandardCharsets.UTF_8));
                  String rawHash = hashTokens[currentToken];
                  hash = TextUtil.bytesToHexString(rawHash.getBytes(StandardCharsets.UTF_8));
                  break;
               default:
                  throw new UnsupportedOperationException("Unrecognized shadow hash specifier \"" + hashSpecification + "\"");
            }

            assert algorithm != null;
            assert salt != null;
            assert iterations != null;
            assert hash != null;
            scramString = salt + ":" + iterations + ":" + hash;
         } catch (Throwable e) {
            log.log(Level.SEVERE, "failed to create scram auth specification (" + e + ")", e);
            scramString = null;
         }

         return scramString;
      } else {
         return null;
      }
   }

   private boolean isStationConnection(HttpServletRequest req, String username) {
      return DaemonAuthUtil.isLocalConnection(req)
         && NiagaraDaemon.getInstance().getStationRegistry().appRunning()
         && (this.authDomain.isExtraUser(username) || this.authDomain.isExtraAdminUser(username));
   }

   protected static String decodeUrl(String s) {
      StringBuilder buf = new StringBuilder(s.length() + 10);
      char[] c = s.toCharArray();

      for (int i = 0; i < c.length; i++) {
         if (c[i] == '+') {
            buf.append(' ');
         } else if (c[i] == '%') {
            i++;
            int val = 0;
            val += fromHex(c[i++]) * 16;
            val += fromHex(c[i]);
            buf.append((char)val);
         } else {
            buf.append(c[i]);
         }
      }

      return buf.toString();
   }

   private static int fromHex(char ch) {
      if (ch >= '0' && ch <= '9') {
         return ch - 48;
      } else if (ch >= 'A' && ch <= 'F') {
         return ch - 65 + 10;
      } else {
         throw new IllegalArgumentException("Invalid hex character: " + ch);
      }
   }

   private static String createDefaultServerFirstMessage(String clientFirstMessage, int defaultSaltLength, int defaultIterationCount) {
      Properties clientProps = Scram.parseMessage(clientFirstMessage.substring(3));
      String clientNonce = clientProps.getProperty("r");
      byte[] nonceVal = new byte[16];
      new SecureRandom().nextBytes(nonceVal);
      String serverNonce = Base64.getEncoder().encodeToString(nonceVal);
      String userName = clientProps.getProperty("n");
      byte[] salt = new byte[defaultSaltLength];

      byte[] userNameBytes;
      try {
         MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
         userNameBytes = messageDigest.digest(userName.getBytes(StandardCharsets.UTF_8));
      } catch (NoSuchAlgorithmException nsae) {
         userNameBytes = TextUtil.pad(userName, defaultSaltLength).getBytes(StandardCharsets.UTF_8);
      }

      System.arraycopy(userNameBytes, 0, salt, 0, Math.min(userNameBytes.length, defaultSaltLength));
      String iterationCount = String.valueOf(defaultIterationCount);
      return "r=" + clientNonce + serverNonce + ",s=" + Base64.getEncoder().encodeToString(salt) + ",i=" + iterationCount;
   }

   @Override
   public boolean supportsSecureKeyExchange() {
      return true;
   }

   @Override
   public boolean keyExchangeEnabled(HttpSession session) {
      ScramAuthenticator.KeyExchangeSettings sessionKeyExchangeSettings = this.sessionKeyExchanges.get(session);
      return sessionKeyExchangeSettings == null ? false : sessionKeyExchangeSettings.doSecureKeyExchange;
   }

   @Override
   public byte[] extractSessionKey(HttpSession session) {
      ScramAuthenticator.KeyExchangeSettings sessionKeyExchangeSettings = this.sessionKeyExchanges.get(session);
      if (sessionKeyExchangeSettings == null) {
         return null;
      } else if (sessionKeyExchangeSettings.keyExchangerServer != null) {
         byte[] returnKey = sessionKeyExchangeSettings.keyExchangerServer.getKey();
         sessionKeyExchangeSettings.keyExchangerServer = null;
         return returnKey;
      } else {
         return null;
      }
   }

   @Override
   public EncryptionAlgorithmBundle getEncryptionAlgorithmBundle(HttpSession session) {
      ScramAuthenticator.KeyExchangeSettings sessionKeyExchangeSettings = this.sessionKeyExchanges.get(session);
      return sessionKeyExchangeSettings == null ? null : sessionKeyExchangeSettings.encryptionAlgorithmBundle;
   }

   private static String generateInitialKeyExchangeMessage(HttpServletRequest req) {
      StringBuilder keyExchangeMessage = new StringBuilder();
      if (req.isSecure()) {
         keyExchangeMessage.append("keyExchange").append("=").append("none");
         keyExchangeMessage.append(" ").append("keyExchangeMethods").append("=").append(NullAlgorithmBundle.getInstance().getAlgorithmName());
      } else if (DaemonAuthUtil.isLocalConnection(req)) {
         keyExchangeMessage.append("keyExchange").append("=").append("nonePendingUser");
         keyExchangeMessage.append(" ").append("keyExchangeMethods").append("=").append(KeyExchange.getPreferredKeyExchangeMethods());
         keyExchangeMessage.append(" ").append("keyExchangeCiphers").append("=").append(KeyExchange.getPreferredKeyExchangeCiphers());
      } else {
         keyExchangeMessage.append("keyExchange").append("=").append("srp6");
         keyExchangeMessage.append(" ").append("keyExchangeMethods").append("=").append(KeyExchange.getPreferredKeyExchangeMethods());
         keyExchangeMessage.append(" ").append("keyExchangeCiphers").append("=").append(KeyExchange.getPreferredKeyExchangeCiphers());
      }

      return keyExchangeMessage.toString();
   }

   private static String processKeyExchangeFinalResponse(
      HttpServletRequest req, ScramAuthenticator.KeyExchangeSettings keyExchangeSettings, Hashtable<String, String> params
   ) {
      StringBuilder serverKeyExchangeConfirmation = new StringBuilder(" ");
      String clientKeyExchangeStatus = params.getOrDefault("keyExchange", "none");
      String clientKeyExchangeMethod = params.getOrDefault("keyExchangeMethod", null);
      String clientKeyExchangeCipher = params.getOrDefault("keyExchangeCipher", null);
      if (log.isLoggable(Level.FINEST)) {
         log.finest(
            "received client key exchange response: keyExchange: "
               + clientKeyExchangeStatus
               + " "
               + "keyExchangeMethod"
               + ": "
               + clientKeyExchangeMethod
               + " "
               + "keyExchangeCipher"
               + ": "
               + clientKeyExchangeCipher
         );
      }

      switch (clientKeyExchangeStatus) {
         case "none":
            keyExchangeSettings.doSecureKeyExchange = false;
            break;
         case "nonePendingConfirmation":
            if (!req.isSecure() && !DaemonAuthUtil.isLocalConnection(req)) {
               keyExchangeSettings.doSecureKeyExchange = true;
               serverKeyExchangeConfirmation.append("keyExchangeConfirmation").append("=").append("srp6");
            } else {
               keyExchangeSettings.doSecureKeyExchange = false;
               serverKeyExchangeConfirmation.append("keyExchangeConfirmation").append("=").append("none");
            }
            break;
         case "srp6":
            keyExchangeSettings.doSecureKeyExchange = true;
      }

      CryptographicAlgorithmBundle keyBundle = CryptographicAlgorithmBundle.getInstance(clientKeyExchangeMethod);
      if (keyBundle instanceof KeyDerivationAlgorithmBundle) {
         keyExchangeSettings.keyDerivationAlgorithmBundle = (KeyDerivationAlgorithmBundle)keyBundle;
      }

      if (clientKeyExchangeCipher != null) {
         CryptographicAlgorithmBundle encryptBundle = CryptographicAlgorithmBundle.getInstance(clientKeyExchangeCipher);
         if (encryptBundle instanceof EncryptionAlgorithmBundle) {
            keyExchangeSettings.encryptionAlgorithmBundle = (EncryptionAlgorithmBundle)encryptBundle;
         }
      }

      return serverKeyExchangeConfirmation.toString();
   }

   @Override
   public void sessionDestroyed(HttpSessionEvent se) {
      HttpSession session = se.getSession();
      if (session != null) {
         if (log.isLoggable(Level.FINEST)) {
            log.finest("session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "' destroyed, removing authenticator attributes");
         }

         session.removeAttribute("scramServer");
         session.removeAttribute("username");
         this.sessionKeyExchanges.remove(session);
      }

      super.sessionDestroyed(se);
   }

   protected class HttpUserKeyFactory implements IUserKeyFactory {
      HttpUserKeyFactory() {
      }

      public String getUserKey(String userName) {
         return ScramAuthenticator.createScramSpecification(ScramAuthenticator.this.authDomain.getPasswordHash(userName));
      }
   }

   private static final class KeyExchangeSettings {
      boolean doSecureKeyExchange = false;
      IKeyExchanger keyExchangerServer = null;
      KeyDerivationAlgorithmBundle keyDerivationAlgorithmBundle;
      EncryptionAlgorithmBundle encryptionAlgorithmBundle;

      private KeyExchangeSettings() {
      }
   }
}
