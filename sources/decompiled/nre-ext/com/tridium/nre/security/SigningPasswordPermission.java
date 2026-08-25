package com.tridium.nre.security;

import java.security.BasicPermission;

public final class SigningPasswordPermission extends BasicPermission {
   public SigningPasswordPermission(String moduleName) {
      super(moduleName);
   }
}
