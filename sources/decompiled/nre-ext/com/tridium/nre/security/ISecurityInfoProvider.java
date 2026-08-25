package com.tridium.nre.security;

import java.io.File;

public interface ISecurityInfoProvider {
   KeyRing getKeyRing();

   File getSecurityDir();

   String getKeyMaterialName();

   String getKeyRingName();

   int getDefaultMinimumPasswordLength();

   default int getDefaultMaximumPasswordLength() {
      return 64;
   }
}
