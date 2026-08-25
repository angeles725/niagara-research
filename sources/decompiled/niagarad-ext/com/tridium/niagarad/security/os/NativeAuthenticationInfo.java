package com.tridium.niagarad.security.os;

import com.tridium.niagarad.security.SimpleAuthenticationInfo;
import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.platform.IPlatformProvider;
import java.util.ArrayList;

public class NativeAuthenticationInfo extends SimpleAuthenticationInfo {
   NativeAuthIdentity identity;

   public NativeAuthIdentity getIdentity() {
      return this.identity;
   }

   public static NativeAuthenticationInfo make(NativeAuthIdentity user, ArrayList<GroupAccount> adminGroups, IPlatformProvider platformProvider) {
      if (user.getAccount() == null) {
         return null;
      }

      for (GroupAccount currentGroup : adminGroups) {
         if (platformProvider.isGroupMember(user.getAccount().getPlatformIdentifier(), currentGroup.getPlatformIdentifier())) {
            return new NativeAuthenticationInfo(user, true, platformProvider);
         }
      }

      return null;
   }

   private NativeAuthenticationInfo(NativeAuthIdentity pIdentity, boolean hostAdminAccess, IPlatformProvider platformProvider) {
      super(pIdentity.getAccount().getAccountName(), hostAdminAccess, false);
      this.identity = new NativeAuthIdentity(
         pIdentity.getAccount().getFullyQualifiedName(), pIdentity.getPassword(), pIdentity.getAccount().getDomain(), platformProvider
      );
   }
}
