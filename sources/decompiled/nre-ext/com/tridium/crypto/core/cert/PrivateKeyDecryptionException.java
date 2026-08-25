package com.tridium.crypto.core.cert;

public class PrivateKeyDecryptionException extends Exception {
   public static final int UNKNOWN = 1;
   public static final int BAD_PWD = 2;
   private final int causeCode;

   public PrivateKeyDecryptionException(String msg, int causeCode) {
      super(msg);
      this.causeCode = causeCode;
   }

   public PrivateKeyDecryptionException(String msg, int causeCode, Throwable cause) {
      super(msg, cause);
      this.causeCode = causeCode;
   }

   public PrivateKeyDecryptionException(Throwable t, int causeCode) {
      super(t);
      this.causeCode = causeCode;
   }

   public int getCauseCode() {
      return this.causeCode;
   }
}
