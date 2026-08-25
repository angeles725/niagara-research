package com.tridium.niagarad.security.os;

import com.tridium.niagarad.security.AuthenticationDomain;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.util.IPAddressUtil;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

public class NativeAuthenticationDomain extends AuthenticationDomain {
   private final IPlatformProvider platformProvider;
   protected String realm;
   private ArrayList<GroupAccount> adminGroups;
   protected boolean local;

   public static NativeAuthenticationDomain make(boolean localRealmDefault, GroupAccount[] adminGroups, IPlatformProvider platformProvider) {
      return new NativeAuthenticationDomain(localRealmDefault, adminGroups, platformProvider);
   }

   private NativeAuthenticationDomain(boolean localRealmDefault, GroupAccount[] adminGroups, IPlatformProvider platformProvider) {
      this.platformProvider = platformProvider;
      String compName = platformProvider.getComputerName();
      String compDomain = platformProvider.getComputerDomain(true);
      this.adminGroups = null;
      if (!localRealmDefault && compDomain != null) {
         this.local = false;
         if (compName != null) {
            this.realm = compName + "." + compDomain;
         } else {
            this.realm = compDomain;
         }
      } else {
         this.local = true;
         this.realm = compName;
      }

      if (adminGroups != null) {
         for (GroupAccount adminGroup : adminGroups) {
            if (this.adminGroups == null) {
               this.adminGroups = new ArrayList<>();
            }

            this.adminGroups.add(adminGroup);
         }
      }

      if (this.adminGroups == null) {
         this.adminGroups = new ArrayList<>();
         this.adminGroups.add(platformProvider.getDefaultAdminGroup());
      }
   }

   @Override
   public AuthenticationInfo makeAuthInfo(String providedUserName, String providedPasswordValue) {
      AuthenticationInfo result = super.makeAuthInfo(providedUserName, providedPasswordValue);
      if (result != null) {
         return result;
      }

      if (providedUserName == null || providedUserName.isEmpty()) {
         return null;
      }

      if (providedPasswordValue != null && !providedPasswordValue.isEmpty()) {
         UserAccount account = this.platformProvider.getAccountFromCredentials(providedUserName, providedPasswordValue, this.local);
         if (account != null) {
            NativeAuthIdentity identity = new NativeAuthIdentity(providedUserName, providedPasswordValue, account.getDomain(), this.platformProvider);
            result = NativeAuthenticationInfo.make(identity, this.adminGroups, this.platformProvider);
         }

         return result;
      } else {
         return null;
      }
   }

   public AuthenticationInfo makeAuthInfo(NativeAuthIdentity identity, boolean authenticateUser, IPlatformProvider platformProvider) {
      return authenticateUser && !identity.getAccount().isPasswordValid(platformProvider, identity.getPassword())
         ? null
         : NativeAuthenticationInfo.make(identity, this.adminGroups, platformProvider);
   }

   @Override
   public String getRealm(HttpServletRequest request) {
      if ((!this.local || this.realm != null)
         && !this.realm.equals("")
         && !this.realm.equalsIgnoreCase("localhost")
         && !this.realm.equalsIgnoreCase("localhost.localdomain")) {
         return this.realm;
      }

      InetAddress remoteAddress = IPAddressUtil.numericStringToInetAddress(request.getRemoteAddr());
      return IPAddressUtil.removeScopeSpec(IPAddressUtil.getLocalHost(remoteAddress).getHostAddress());
   }

   @Override
   public String getDomainType() {
      return "native";
   }

   @Override
   public boolean supportsPasswordClearTextRetrieval() {
      return false;
   }

   @Override
   public boolean supportsPasswordHashRetrieval() {
      String supportedAuthenticationTypes = this.platformProvider.getSupportedAuthenticationTypes();
      return supportedAuthenticationTypes.contains("scram-sha512/native")
         || supportedAuthenticationTypes.contains("scram-glibc-sha512/native")
         || supportedAuthenticationTypes.contains("scram-glibc-sha256/native")
         || supportedAuthenticationTypes.contains("scram-bcrypt/native");
   }

   @Override
   public String getPasswordHash(String userName) {
      String passwordHash = super.getPasswordHash(userName);
      if (passwordHash == null) {
         passwordHash = this.platformProvider.getPasswordHash(userName);
      }

      return passwordHash;
   }
}
