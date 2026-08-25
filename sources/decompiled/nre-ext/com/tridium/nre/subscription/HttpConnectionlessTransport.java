package com.tridium.nre.subscription;

import com.tridium.json.JSONObject;
import com.tridium.json.JSONTokener;
import com.tridium.nre.security.SecurityConstants;
import java.io.IOException;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.baja.nre.util.TextUtil;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient.Builder;
import okhttp3.internal.Util;

public class HttpConnectionlessTransport {
   private static SSLSocketFactory factory;
   private static final HttpConnectionlessTransport.TrustingHostnameVerifier TRUSTING_HOSTNAME_VERIFIER = new HttpConnectionlessTransport.TrustingHostnameVerifier(
      
   );
   private static final HttpConnectionlessTransport.AlwaysTrustManager TRUSTING_MANAGER = new HttpConnectionlessTransport.AlwaysTrustManager();
   private static final int connectTimeout = 15000;
   private static final int readTimeout = 30000;
   private static final int writeTimeout = 15000;
   private OkHttpClient client;

   public HttpConnectionlessTransport() {
      this.makeClient();
   }

   public HttpConnectionlessTransport(Interceptor interceptor) {
      this.makeClient(interceptor);
   }

   private void makeClient() {
      this.makeClient(null);
   }

   private void makeClient(Interceptor interceptor) {
      try {
         prepFactory();
      } catch (Exception e) {
         if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
            EntitlementUtil.LOG.log(Level.WARNING, "Unable to create SSL socket factory", e);
         } else {
            EntitlementUtil.LOG.log(Level.WARNING, "Unable to create SSL socket factory: " + e.getMessage());
         }
      }

      AccessController.doPrivileged(
         () -> {
            Builder clientBuilder = new Builder()
               .connectionSpecs(Collections.singletonList(ConnectionSpec.RESTRICTED_TLS))
               .connectTimeout(15000L, TimeUnit.MILLISECONDS)
               .writeTimeout(30000L, TimeUnit.MILLISECONDS)
               .readTimeout(15000L, TimeUnit.MILLISECONDS);
            if (interceptor != null) {
               clientBuilder.addInterceptor(interceptor);
            }

            if (!SecurityConstants.canCheckTpk()) {
               String entitlementServer = SubscriptionLicenseUtil.getLicenseProperties()
                  .getProperty("license.entitlementUrl", "https://www.niagaracentralapis.honeywell.com");
               if (entitlementServer.startsWith("http://")) {
                  clientBuilder = clientBuilder.connectionSpecs(Collections.singletonList(ConnectionSpec.CLEARTEXT));
               } else {
                  clientBuilder = clientBuilder.hostnameVerifier(TRUSTING_HOSTNAME_VERIFIER).sslSocketFactory(factory, TRUSTING_MANAGER);
               }
            }

            this.client = clientBuilder.build();
            return this.client;
         }
      );
   }

   protected void send(HttpMessageWrapper<? extends IHttpMessage> payload) throws IOException {
      try {
         if (!(payload.getMessage() instanceof HttpRequestMessage)) {
            IOException ioEx = new IOException("Invalid message type for HTTP transport " + payload.getMessage().getClass().getName());
            payload.getTransportFuture().completeExceptionally(ioEx);
            throw ioEx;
         }

         HttpResponseMessage responseMessage = AccessController.doPrivileged(() -> {
            HttpRequestMessage message = (HttpRequestMessage)payload.getMessage();
            Request request = makeRequest(message);
            Response response = this.client.newCall(request).execute();
            if (response.isSuccessful()) {
               return new HttpResponseMessage(response);
            }

            String errorMessage = "";
            String errorType = "";
            if (response.body() != null) {
               String responseType = Objects.requireNonNull(response.body().contentType()).subtype();
               if (responseType.toLowerCase().contains("json")) {
                  JSONObject jsonErr = new JSONObject(new JSONTokener(response.body().string()));
                  if (jsonErr.has("error") && jsonErr.has("error_description")) {
                     errorMessage = jsonErr.optString("error_description", Objects.toString(jsonErr));
                     errorType = jsonErr.optString("error", "");
                  } else {
                     errorMessage = jsonErr.optString("message", Objects.toString(jsonErr));
                     errorType = jsonErr.optString("type", "");
                  }

                  errorMessage = TextUtil.capitalize(errorMessage);
               } else {
                  String host = "unknown host";
                  if (response.handshake() != null && response.handshake().peerPrincipal() != null) {
                     host = response.handshake().peerPrincipal().getName();
                     int cnIdx = host.indexOf("CN=");
                     if (cnIdx > -1) {
                        host = host.substring(cnIdx + 3);
                        int commaIdx = host.indexOf(44);
                        if (commaIdx > -1) {
                           host = host.substring(0, commaIdx);
                        }
                     }
                  }

                  errorMessage = String.format("Unexpected response from %s. Check proxy and firewall settings", host);
                  if (EntitlementUtil.LOG.isLoggable(Level.FINEST)) {
                     EntitlementUtil.LOG.finest(errorMessage);
                     if (response.body() != null) {
                        EntitlementUtil.LOG.finest("Response body:\n" + response.body().string());
                     }

                     if (response.handshake() != null) {
                        List<Certificate> peerCertificates = response.handshake().peerCertificates();
                        EntitlementUtil.LOG.finest("Certificate Chain:");

                        for (Certificate cert : peerCertificates) {
                           EntitlementUtil.LOG.finest(cert.toString());
                        }
                     }
                  }
               }
            }

            if (errorMessage.isEmpty()) {
               errorMessage = response.message();
            }

            throw new HttpStatusException(response.code(), errorType, errorMessage);
         });
         payload.getTransportFuture().complete(responseMessage);
      } catch (PrivilegedActionException e) {
         Exception inner = e.getException();
         payload.getTransportFuture().completeExceptionally(inner);
      }
   }

   private static Request makeRequest(HttpRequestMessage message) {
      okhttp3.Request.Builder reqBuilder = new okhttp3.Request.Builder();
      reqBuilder.url(message.getUrl());
      if (message.getBody() != null) {
         MediaType mimeType = MediaType.get(message.getMimeType());
         RequestBody body = RequestBody.create(message.getBody(), mimeType);
         if (message.getMethod() == HttpRequestMessage.Method.POST) {
            reqBuilder.post(body);
         } else if (message.getMethod() == HttpRequestMessage.Method.PUT) {
            reqBuilder.put(body);
         } else if (message.getMethod() == HttpRequestMessage.Method.DELETE) {
            reqBuilder.delete(body);
         } else {
            EntitlementUtil.LOG.warning(() -> String.format("Unsupported: HTTP Message of type %s with a body, dropping body.", message.getMethod()));
         }
      } else if (message.getMethod() == HttpRequestMessage.Method.GET) {
         reqBuilder.get();
      } else if (message.getMethod() == HttpRequestMessage.Method.DELETE) {
         reqBuilder.delete();
      } else if (message.getMethod() == HttpRequestMessage.Method.POST) {
         reqBuilder.post(Util.EMPTY_REQUEST);
      } else {
         EntitlementUtil.LOG.warning(() -> String.format("Unsupported: HTTP Message of type %s with no body, using GET.", message.getMethod()));
      }

      Map<String, Object> headers = message.getRequestHeaders();
      if (headers != null) {
         headers.forEach((key, value) -> reqBuilder.addHeader(key, value.toString()));
      }

      return reqBuilder.build();
   }

   static synchronized SSLSocketFactory prepFactory() throws NoSuchAlgorithmException, KeyManagementException {
      if (factory == null) {
         SSLContext ctx = SSLContext.getInstance("TLS");
         ctx.init(null, new TrustManager[]{new HttpConnectionlessTransport.AlwaysTrustManager()}, null);
         factory = ctx.getSocketFactory();
      }

      return factory;
   }

   private static class AlwaysTrustManager implements X509TrustManager {
      private AlwaysTrustManager() {
      }

      @Override
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
         if (SecurityConstants.canCheckTpk()) {
            throw new CertificateException("Use of AlwaysTrustManager outside of Development Build not allowed");
         }
      }

      @Override
      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
         if (SecurityConstants.canCheckTpk()) {
            throw new CertificateException("Use of AlwaysTrustManager outside of Development Build not allowed");
         }
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
         if (!SecurityConstants.canCheckTpk()) {
            return new X509Certificate[0];
         } else {
            throw new RuntimeException("Use of AlwaysTrustManager outside of Development Build not allowed");
         }
      }
   }

   private static final class TrustingHostnameVerifier implements HostnameVerifier {
      private TrustingHostnameVerifier() {
      }

      @Override
      public boolean verify(String hostname, SSLSession session) {
         if (!SecurityConstants.canCheckTpk()) {
            EntitlementUtil.LOG.finest(() -> String.format("Hostname verification for %s", hostname));
            return true;
         } else {
            throw new RuntimeException("Use of TrustingHostnameVerifier outside of Development Build not allowed");
         }
      }
   }
}
