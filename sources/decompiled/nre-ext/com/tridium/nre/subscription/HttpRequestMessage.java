package com.tridium.nre.subscription;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class HttpRequestMessage implements IHttpMessage {
   private final HttpRequestMessage.Method method;
   private final URL url;
   private final String mimeType;
   private final Map<String, Object> requestHeaders;
   private final String body;

   public HttpRequestMessage(HttpRequestMessage.Method method, URL url, Map<String, Object> requestHeaders, String mimeType, String body) {
      Objects.requireNonNull(url, "HttpRequestMessage: url must not be null");
      this.method = method;
      this.url = url;
      this.mimeType = mimeType;
      this.requestHeaders = Collections.unmodifiableMap(requestHeaders);
      this.body = body;
   }

   public HttpRequestMessage.Method getMethod() {
      return this.method;
   }

   public URL getUrl() {
      return this.url;
   }

   public String getMimeType() {
      return this.mimeType;
   }

   public Map<String, Object> getRequestHeaders() {
      return this.requestHeaders;
   }

   public String getBody() {
      return this.body;
   }

   @Override
   public int getLength() {
      return this.body != null ? this.body.length() : 0;
   }

   @Override
   public byte[] getPayload() {
      return this.body != null ? this.body.getBytes() : new byte[0];
   }

   @Override
   public Map<String, Object> getMetadata() {
      return this.getRequestHeaders();
   }

   public enum Method {
      GET,
      POST,
      PUT,
      DELETE;
   }
}
