package com.tridium.bacnet.stack.link.sc.authentication;

import com.tridium.authn.LoginFailureCause;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.web.session.NiagaraWebSession;
import com.tridium.web.session.WebSessionUtil;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.web.authn.BWebCallbackHandler;
import javax.net.ssl.SSLSession;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@NiagaraType(
   agent = {@AgentOn(
      types = {"bacnet:BacnetScAuthenticationScheme"}
   )}
)
public final class BBacnetScWebCallbackHandler extends BWebCallbackHandler {
   public static final Type TYPE = Sys.loadType(BBacnetScWebCallbackHandler.class);
   private String username;

   public Type getType() {
      return TYPE;
   }

   public int handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
      NiagaraWebSession session = null;

      try {
         session = WebSessionUtil.getSession(request.getSession());
         Locale locale = request.getLocale();
         Context context = new BasicContext(null, locale != null ? locale.toLanguageTag() : Sys.getLanguage());
         if (!request.isSecure()) {
            BBacnetScAuthenticator.logger.warning(ScLinkLayerUtil.LEXICON.getText("authFailure.noTls", context));
            return handleError(session);
         } else {
            SSLSession sslSession = (SSLSession)request.getAttribute("org.eclipse.jetty.servlet.request.ssl_session");
            if (sslSession != null && sslSession.getProtocol().equalsIgnoreCase("tlsv1.3")) {
               X509Certificate[] clientCertChain = (X509Certificate[])request.getAttribute("javax.servlet.request.X509Certificate");
               if (clientCertChain == null) {
                  BBacnetScAuthenticator.logger.warning(ScLinkLayerUtil.LEXICON.getText("authFailure.noClientCert", context));
                  return handleError(session);
               } else {
                  this.username = findUserFromCertificateChain(clientCertChain);
                  if (this.username == null) {
                     BBacnetScAuthenticator.logger.warning(ScLinkLayerUtil.LEXICON.getText("authFailure.noUser", context));
                     return handleError(session);
                  } else {
                     return 0;
                  }
               }
            } else {
               BBacnetScAuthenticator.logger.warning(ScLinkLayerUtil.LEXICON.getText("authFailure.tlsVersion", context));
               return handleError(session);
            }
         }
      } catch (Exception var8) {
         if (BBacnetScAuthenticator.logger.isLoggable(Level.FINE)) {
            BBacnetScAuthenticator.logger.log(Level.WARNING, var8.getMessage(), (Throwable)var8);
         } else {
            BBacnetScAuthenticator.logger.log(Level.WARNING, var8.getMessage());
         }

         return handleError(session);
      }
   }

   private static int handleError(NiagaraWebSession session) {
      if (session != null) {
         session.setAttribute("loginFailureCause", LoginFailureCause.CLIENT_CERT_FAILURE);
      }

      return 3;
   }

   public String getUsername() {
      return this.username;
   }

   private static String findUserFromCertificateChain(X509Certificate[] clientCertChain) {
      List<? extends Certificate> sortedClientCertChain = null;
      int bestMatchingIndex = Integer.MAX_VALUE;
      String bestMatchingUser = null;

      for (BUser user : BUserService.getService().getUsers()) {
         if (user.getAuthenticationScheme() instanceof BBacnetScAuthenticationScheme) {
            if (sortedClientCertChain == null) {
               sortedClientCertChain = CertUtils.sortCertChain(Arrays.asList(clientCertChain));
            }

            BBacnetScAuthenticator authenticator = (BBacnetScAuthenticator)user.getAuthenticator();
            PKIXCertPathBuilderResult result = authenticator.verify(sortedClientCertChain);
            if (result != null) {
               int currentIndex = getTrustedCertIndex(result);
               if (currentIndex < bestMatchingIndex) {
                  bestMatchingUser = user.getName();
                  bestMatchingIndex = currentIndex;
               }
            }
         }
      }

      return bestMatchingUser;
   }

   private static int getTrustedCertIndex(PKIXCertPathBuilderResult result) {
      List<? extends Certificate> certPath = result.getCertPath().getCertificates();
      Certificate matchingCert = result.getTrustAnchor().getTrustedCert();
      int currentIndex = certPath.indexOf(matchingCert);
      return currentIndex == -1 ? certPath.size() : currentIndex;
   }

   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      if (callbacks != null && callbacks.length != 0) {
         for (Callback callback : callbacks) {
            if (!(callback instanceof BacnetScCallback)) {
               throw new UnsupportedCallbackException(callback, "Callback " + callback.getClass().getName() + " is not supported.");
            }

            ((BacnetScCallback)callback).setUsername(this.username);
         }
      } else {
         throw new IOException("invalid callback array provided");
      }
   }
}
