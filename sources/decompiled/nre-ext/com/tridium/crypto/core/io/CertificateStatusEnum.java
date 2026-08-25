package com.tridium.crypto.core.io;

public enum CertificateStatusEnum {
   OK(0),
   BAD_PASSWORD(1),
   BAD_KEY(2),
   MISSING_KEY(3),
   BAD_DEFAULT(4);

   private final int val;

   CertificateStatusEnum(int val) {
      this.val = val;
   }

   public int getVal() {
      return this.val;
   }
}
