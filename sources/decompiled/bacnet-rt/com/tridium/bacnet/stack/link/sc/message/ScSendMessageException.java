package com.tridium.bacnet.stack.link.sc.message;

import javax.baja.bacnet.BacnetException;

public final class ScSendMessageException extends BacnetException {
   private final ScBvlcMessage scBvlcMessage;
   private final byte[] scBvlcMessageBytes;

   public ScSendMessageException(String message) {
      super(message);
      this.scBvlcMessage = null;
      this.scBvlcMessageBytes = null;
   }

   public ScSendMessageException(ScBvlcMessage scBvlcMessage, String message, Throwable cause) {
      super(message, cause);
      this.scBvlcMessage = scBvlcMessage;
      this.scBvlcMessageBytes = null;
   }

   public ScSendMessageException(byte[] scBvlcMessageBytes, String message, Throwable cause) {
      super(message, cause);
      this.scBvlcMessage = null;
      this.scBvlcMessageBytes = scBvlcMessageBytes;
   }

   public ScBvlcMessage getScMessage() {
      return this.scBvlcMessage;
   }

   public byte[] getScMessageBytes() {
      return this.scBvlcMessageBytes;
   }
}
