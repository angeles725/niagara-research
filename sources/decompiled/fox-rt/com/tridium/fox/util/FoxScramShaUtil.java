package com.tridium.fox.util;

import com.tridium.authn.BLegacyDigestAuthenticationScheme;
import com.tridium.authn.GenericReportableAuthenticationException;
import com.tridium.authn.LoginFailureCause;
import com.tridium.authn.ScramServerCallback;
import com.tridium.authn.UserKeyFactory;
import com.tridium.crypto.core.exchange.IKeyExchanger;
import com.tridium.crypto.core.exchange.KeyExchange;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.FoxAuthenticationException;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.FoxUserLockoutException;
import com.tridium.fox.sys.BFoxClientConnection;
import com.tridium.nre.auth.NiagaraStationAlgorithmBundle;
import com.tridium.nre.auth.ScramClient;
import com.tridium.nre.auth.ScramServer;
import com.tridium.nre.security.SecretBytes;
import java.io.EOFException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.authn.AuthenticationUtil;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.security.AuthenticationException;
import javax.baja.security.BPasswordCache;
import javax.baja.security.BPbkdf2HmacSha256PasswordEncoder;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;

public class FoxScramShaUtil {
   public static String handleScramServerCallback(FoxSession session, ScramServerCallback callback, Type schemeType) {
      ScramServer server = new ScramServer(NiagaraStationAlgorithmBundle.getInstance(), new UserKeyFactory(schemeType));
      String username = null;

      try {
         session.setState("server.tune receive firstMessage");
         FoxMessage in = session.receiveTuning("authMessage1");
         String clientFirstMessage = in.getString("authHandshake1");
         username = ScramServer.extractUsername(clientFirstMessage);
         String serverFirstMessage = server.createServerFirstMessage(clientFirstMessage);
         if (session.isLegacyConnection()) {
            BUserService userService = (BUserService)Sys.getService(BUserService.TYPE);
            BAuthenticationScheme scheme = userService.getAuthenticationSchemeForUser(username);
            if (!(scheme instanceof BLegacyDigestAuthenticationScheme)) {
               throw new Exception("AX connection requires a user with the AXDigestAuthenticationScheme.");
            }
         } else {
            BUserService userService = (BUserService)Sys.getService(BUserService.TYPE);
            BAuthenticationScheme scheme = userService.getAuthenticationSchemeForUser(username);
            if (scheme instanceof BLegacyDigestAuthenticationScheme) {
               throw new Exception("N4 connection cannot be made with the AXDigestAuthenticationScheme.");
            }
         }

         FoxMessage out = new FoxMessage();
         out.add("authHandshake1", serverFirstMessage);
         session.setState("server.tune send scramsha1-serverFirstMessage");
         session.sendTuning("authMessage1", out);
         session.setState("server.tune receive scramsha1-clientFinalMessage");
         in = session.receiveTuning("authMessage2");
         String clientFinalMessage = in.getString("authHandshake2");
         String serverFinalMessage = server.createServerFinalMessage(clientFinalMessage);
         out = new FoxMessage();
         out.add("authHandshake2", serverFinalMessage);
         session.setState("server.tune send scramsha1-serverFinalMessage");
         session.sendTuning("authMessage2", out);
         if (session.supportsKeyExchange()) {
            IKeyExchanger keyExchangerServer = KeyExchange.makeServer(session.getKeyExchangeAlgorithmBundle());
            keyExchangerServer.init();
            BUserService service = (BUserService)Sys.getService(BUserService.TYPE);
            BUser user = service.getUser(username);
            BPbkdf2HmacSha256PasswordEncoder passwordEncoder = (BPbkdf2HmacSha256PasswordEncoder)((BPasswordCache)user.getAuthenticator()).getPasswordEncoder();
            SecretBytes saltedPassword = new SecretBytes(passwordEncoder.getKey(), false);
            Throwable decoder = null;

            try {
               keyExchangerServer.doInitialStep(saltedPassword);
            } catch (Throwable var32) {
               decoder = var32;
               throw var32;
            } finally {
               if (saltedPassword != null) {
                  if (decoder != null) {
                     try {
                        saltedPassword.close();
                     } catch (Throwable var31) {
                        decoder.addSuppressed(var31);
                     }
                  } else {
                     saltedPassword.close();
                  }
               }
            }

            Encoder encoder = Base64.getEncoder();
            Decoder decoderx = Base64.getDecoder();
            session.setState("server.tune receive srp6-clientFirstMessage");
            in = session.receiveTuning("srp6ClientA");
            boolean keyExchange = in.getBoolean("keyExchange", true);
            if (keyExchange) {
               String A_encoded = in.getString("keyExchangeClientA");
               byte[] A = decoderx.decode(A_encoded);
               byte[] B = keyExchangerServer.doExchangeStep(A);
               if (B == null) {
                  out = new FoxMessage();
                  out.add("keyExchangeServerB", (String)null);
                  session.setState("server.tune send srp6-serverFirstMessage");
                  session.sendTuning("srp6ServerB", out);
                  throw new Exception();
               }

               out = new FoxMessage();
               out.add("keyExchangeServerB", encoder.encodeToString(B));
               session.setState("server.tune send srp6-serverFirstMessage");
               session.sendTuning("srp6ServerB", out);
               session.setState("server.tune receive srp6-clientVerificationMessage");
               in = session.receiveTuning("srp6M1");
               String M1_encoded = in.getString("keyExchangeM1");
               byte[] M1 = decoderx.decode(M1_encoded);
               byte[] M2 = keyExchangerServer.doExchangeStep(M1);
               if (M2 == null) {
                  out = new FoxMessage();
                  out.add("keyExchangeM2", (String)null);
                  session.setState("server.tune send srp6-serverVerificationMessage");
                  session.sendTuning("srp6M2", out);
               } else {
                  out = new FoxMessage();
                  out.add("keyExchangeM2", encoder.encodeToString(M2));
                  session.setState("server.tune send srp6-serverVerificationMessage");
                  session.sendTuning("srp6M2", out);
               }

               byte[] sessionKey = keyExchangerServer.getKey();
               if (sessionKey != null) {
                  AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
                     session.setSessionKey(sessionKey);
                     return null;
                  }));
               }
            }
         }
      } catch (EOFException | SecurityException var34) {
      } catch (Exception var35) {
         Logger.getLogger("authentication").warning("Could not authenticate: " + var35.getLocalizedMessage());
         AuthenticationUtil.debug(Level.WARNING, "Callback error with " + callback.getClass().getName(), var35);
      }

      callback.setServer(server);
      callback.setUsername(username);
      return username;
   }

   public static void handleClientAuthentication(FoxSession session, BUsernameAndPassword cred, String scheme) throws FoxAuthenticationException {
      try {
         ScramClient client = new ScramClient(
            NiagaraStationAlgorithmBundle.getInstance(), cred.getUsername(), AccessController.doPrivileged(cred.getPassword()::getValue)
         );
         FoxMessage out = new FoxMessage();
         out.add("authInput", "authInputScram");
         out.add("authHandshake1", client.createClientFirstMessage());
         session.setState("client.tune send scramsha1-clientFirstMessage");
         session.sendTuning("authMessage1", out);
         FoxMessage in = session.receiveTuning("authMessage1");
         String serverFirstMessage = in.getString("authHandshake1");
         String clientFinalMessage = client.createClientFinalMessage(serverFirstMessage);
         out = new FoxMessage();
         out.add("authHandshake2", clientFinalMessage);
         session.setState("client.tune send scramsha1-clientFinalMessage");
         session.sendTuning("authMessage2", out);
         IKeyExchanger keyExchangerClient = null;
         byte[] A = null;
         if (session.supportsKeyExchange()) {
            keyExchangerClient = KeyExchange.makeClient(session.getKeyExchangeAlgorithmBundle());
            keyExchangerClient.init();
            SecretBytes saltedPassword = client.getSaltedPassword();
            Throwable encoder = null;

            try {
               A = keyExchangerClient.doInitialStep(saltedPassword);
            } catch (Throwable var27) {
               encoder = var27;
               throw var27;
            } finally {
               if (saltedPassword != null) {
                  if (encoder != null) {
                     try {
                        saltedPassword.close();
                     } catch (Throwable var26) {
                        encoder.addSuppressed(var26);
                     }
                  } else {
                     saltedPassword.close();
                  }
               }
            }
         }

         in = session.receiveTuning("authMessage2");
         String serverFinalMessage = in.getString("authHandshake2");
         session.setState("client.tune process scramsha1-serverFinalMessage");
         client.processServerFinalMessage(serverFinalMessage);
         if (keyExchangerClient != null) {
            Encoder encoder = Base64.getEncoder();
            Decoder decoder = Base64.getDecoder();
            out = new FoxMessage();
            out.add("keyExchangeClientA", encoder.encodeToString(A));
            session.setState("client.tune send srp6-clientFirstMessage");
            session.sendTuning("srp6ClientA", out);
            in = session.receiveTuning("srp6ServerB");
            String B_encoded = in.getString("keyExchangeServerB");
            if (B_encoded == null) {
               throw new Exception();
            }

            session.setState("client.tune process srp6-serverFirstMessage");
            byte[] B = decoder.decode(B_encoded);
            byte[] M1 = keyExchangerClient.doExchangeStep(B);
            out = new FoxMessage();
            out.add("keyExchangeM1", encoder.encodeToString(M1));
            session.setState("client.tune send srp6-clientVerificationMessage");
            session.sendTuning("srp6M1", out);
            in = session.receiveTuning("srp6M2");
            String M2_encoded = in.getString("keyExchangeM2");
            if (M2_encoded == null) {
               throw new Exception();
            }

            session.setState("client.tune process srp6-serverVerificationMessage");
            byte[] M2 = decoder.decode(M2_encoded);
            keyExchangerClient.doExchangeStep(M2);
            byte[] sessionKey = keyExchangerClient.getKey();
            if (sessionKey != null) {
               session.setSessionKey(sessionKey);
            }
         }
      } catch (FoxUserLockoutException var29) {
         throw new FoxUserLockoutException(scheme, null, session);
      } catch (AssertionError var30) {
         throw new AuthenticationException(
            LoginFailureCause.FIPS_PASSWORD_LENGTH.getDefaultFailureMessage(),
            new GenericReportableAuthenticationException(LoginFailureCause.FIPS_PASSWORD_LENGTH),
            ((BFoxClientConnection)session.conn()).getFoxSession()
         );
      } catch (Exception var31) {
         throw new FoxAuthenticationException("Rejected", scheme, null, session);
      }
   }
}
