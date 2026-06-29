package com.tridium.bacnet.stack.link.sc.authentication;

import javax.security.auth.callback.Callback;

final class BacnetScCallback implements Callback {
   private String username;

   String getUsername() {
      return this.username;
   }

   void setUsername(String username) {
      this.username = username;
   }
}
