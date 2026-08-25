package com.tridium.nre.subscription;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;
import okhttp3.MediaType;
import okhttp3.Response;

public class HttpResponseMessage implements IHttpMessage, Closeable {
   private final Response response;

   public HttpResponseMessage(Response response) {
      this.response = response;
   }

   public int getStatusCode() {
      return this.response.code();
   }

   public Map<String, List<String>> getHeaders() {
      return this.response.headers().toMultimap();
   }

   public InputStream getBody() throws IOException {
      if (this.response.body() != null) {
         return this.response.body().byteStream();
      } else {
         throw new IOException("Missing response body");
      }
   }

   public String getBodyAsString() {
      if (this.response.body() == null) {
         return "";
      }

      try {
         return this.response.body().string();
      } catch (IOException ex) {
         if (EntitlementUtil.LOG.isLoggable(Level.FINE)) {
            EntitlementUtil.LOG.log(Level.WARNING, "Error getting message payload: ", ex);
         } else {
            EntitlementUtil.LOG.log(Level.WARNING, "Error getting message payload: " + ex.getMessage());
         }

         return "";
      }
   }

   public MediaType getContentType() {
      return this.response.body() == null ? MediaType.parse("text/plain") : this.response.body().contentType();
   }

   @Override
   public int getLength() {
      return this.response.body() != null ? Math.toIntExact(this.response.body().contentLength()) : 0;
   }

   @Override
   public byte[] getPayload() {
      if (this.response.body() != null) {
         try {
            return this.response.body().bytes();
         } catch (IOException var2) {
         }
      }

      return new byte[0];
   }

   @Override
   public Map<String, Object> getMetadata() {
      return this.response
         .headers()
         .toMultimap()
         .entrySet()
         .stream()
         .collect(Collectors.toMap(Entry::getKey, e -> String.join(",", (Iterable<? extends CharSequence>)e.getValue())));
   }

   @Override
   public void close() throws IOException {
      if (this.response.body() != null) {
         this.response.close();
      }
   }
}
