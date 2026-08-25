package com.tridium.crypto.core.cert;

import javax.baja.nre.security.IKeyPurpose;

public enum KeyPurpose implements IKeyPurpose {
   CLIENT_CERT(0),
   SERVER_CERT(1),
   CA_CERT(2),
   CODE_SIGNING_CERT(3);

   private final int id;

   KeyPurpose(int id) {
      this.id = id;
   }

   @Override
   public int getValue() {
      return this.id;
   }
}
