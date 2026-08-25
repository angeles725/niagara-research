package com.tridium.niagarad.log;

public class SimpleErrorHandler implements ErrorHandler {
   private MessageBundle lastError;

   @Override
   public void error(String errorMessage) {
      this.lastError = new MessageBundle(errorMessage);
   }

   @Override
   public void error(MessageBundle error) {
      this.lastError = error;
   }

   @Override
   public MessageBundle getLastError() {
      return this.lastError;
   }
}
