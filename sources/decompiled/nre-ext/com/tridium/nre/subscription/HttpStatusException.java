package com.tridium.nre.subscription;

import java.io.IOException;

public class HttpStatusException extends IOException {
   private final int statusCode;
   private final String type;

   public HttpStatusException(int statusCode) {
      super("Invalid response: " + statusCode);
      this.statusCode = statusCode;
      this.type = "";
   }

   public HttpStatusException(int statusCode, String type, String message) {
      super(message);
      this.statusCode = statusCode;
      this.type = type;
   }

   public int getStatusCode() {
      return this.statusCode;
   }

   public String getType() {
      return this.type;
   }
}
