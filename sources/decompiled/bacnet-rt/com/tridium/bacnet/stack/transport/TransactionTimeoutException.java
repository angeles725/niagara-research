package com.tridium.bacnet.stack.transport;

public class TransactionTimeoutException extends TransactionException {
   public TransactionTimeoutException() {
   }

   public TransactionTimeoutException(String detailMessage) {
      super(detailMessage);
   }

   @Override
   public String toString() {
      return "TransactionTimeoutException: " + this.getMessage();
   }
}
