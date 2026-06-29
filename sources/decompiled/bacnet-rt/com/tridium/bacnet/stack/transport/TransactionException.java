package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;

public class TransactionException extends BacnetStackException {
   public TransactionException() {
   }

   public TransactionException(String detailMessage) {
      super(detailMessage);
   }

   @Override
   public String toString() {
      return "Transaction:" + this.getMessage();
   }
}
