package com.tridium.niagarad.security.os;

import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.platform.IPlatformProvider;

public class NativeAuthIdentity {
   private final UserAccount account;
   private final String password;

   public NativeAuthIdentity(String userName, String password, String defaultRealm, IPlatformProvider platformProvider) {
      this.account = (UserAccount)platformProvider.getAccountFromName(userName, defaultRealm, true);
      this.password = password;
   }

   public UserAccount getAccount() {
      return this.account;
   }

   String getPassword() {
      return this.password;
   }
}
