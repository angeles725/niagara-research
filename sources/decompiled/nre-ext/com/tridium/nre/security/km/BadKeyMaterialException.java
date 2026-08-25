package com.tridium.nre.security.km;

import java.security.GeneralSecurityException;

public class BadKeyMaterialException extends GeneralSecurityException {
   public BadKeyMaterialException(Exception cause) {
      super(cause);
   }
}
