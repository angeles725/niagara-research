package com.tridium.bacnet.stack.link.sc.message;

import java.util.Objects;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.enums.BBacnetErrorCode;

public final class ScReadMessageException extends BacnetException {
   private final BBacnetErrorCode errorCode;
   private final int headerMarker;

   public ScReadMessageException(String message, BBacnetErrorCode errorCode) {
      super(message);
      Objects.requireNonNull(errorCode, "errorCode parameter must not be null");
      this.errorCode = errorCode;
      this.headerMarker = 0;
   }

   public ScReadMessageException(String message, Throwable cause, BBacnetErrorCode errorCode) {
      super(message, cause);
      Objects.requireNonNull(errorCode, "errorCode parameter must not be null");
      this.errorCode = errorCode;
      this.headerMarker = 0;
   }

   public ScReadMessageException(String message, BBacnetErrorCode errorCode, int headerMarker) {
      super(message);
      Objects.requireNonNull(errorCode, "errorCode parameter must not be null");
      this.errorCode = errorCode;
      this.headerMarker = headerMarker;
   }

   public BBacnetErrorCode getErrorCode() {
      return this.errorCode;
   }

   public int getHeaderMarker() {
      return this.headerMarker;
   }
}
