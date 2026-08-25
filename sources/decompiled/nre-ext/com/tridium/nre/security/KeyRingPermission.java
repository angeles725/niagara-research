package com.tridium.nre.security;

import java.security.BasicPermission;

public class KeyRingPermission extends BasicPermission {
   public KeyRingPermission(String name) {
      super(name);
   }
}
