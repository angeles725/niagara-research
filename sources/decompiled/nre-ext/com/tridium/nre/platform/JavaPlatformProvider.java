package com.tridium.nre.platform;

abstract class JavaPlatformProvider implements IPlatformProvider {
   boolean load() {
      return this.doLoad();
   }

   abstract boolean doLoad();
}
