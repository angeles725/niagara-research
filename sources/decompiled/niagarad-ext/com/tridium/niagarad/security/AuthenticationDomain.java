package com.tridium.niagarad.security;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.security.os.NativeAuthenticationDomain;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.platform.IPlatformProvider;
import java.util.Properties;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;
import javax.servlet.http.HttpServletRequest;

public abstract class AuthenticationDomain {
   private final Properties extraUsers = new Properties();
   private final Properties extraAdminUsers = new Properties();
   protected static final Logger log = Logger.getLogger("auth.domain");

   public static AuthenticationDomain make(Properties props, Properties extraUsers, Properties extraAdminUsers, IPlatformProvider platformProvider) {
      String authType = props.getProperty("authtype", null);
      boolean propertiesModified = false;
      if (authType == null) {
         authType = DaemonAuthUtil.getDefaultAuthScheme(platformProvider);
         props.setProperty("authtype", authType);
         propertiesModified = true;
      }

      String supportedAuthTypes = platformProvider.getSupportedAuthenticationTypes();
      if (!supportedAuthTypes.contains("scram-glibc-sha512/file")
         && (props.containsKey("user") || props.containsKey("password") || props.containsKey("_password"))) {
         props.remove("user");
         props.remove("password");
         props.remove("_password");
         propertiesModified = true;
      }

      AuthenticationDomain result;
      if (DaemonAuthUtil.isNativeScheme(authType)) {
         boolean defaultLocal;
         if (props.containsKey("defaultlocal")) {
            defaultLocal = Boolean.parseBoolean(props.getProperty("defaultlocal", "true"));
         } else {
            defaultLocal = platformProvider.getComputerDomain(true) == null;
         }

         String adminGroupIdProp = "";
         if (!platformProvider.providesAccountManagement()) {
            adminGroupIdProp = props.getProperty("admingroupid", "");
         }

         GroupAccount[] adminGroups = platformProvider.getAccounts(adminGroupIdProp, ',');
         result = NativeAuthenticationDomain.make(defaultLocal, adminGroups, platformProvider);
         if (adminGroups == null || adminGroups.length == 0) {
            GroupAccount defaultAdminGroup = platformProvider.getDefaultAdminGroup();
            if (defaultAdminGroup == null || defaultAdminGroup.getPlatformIdentifier() == null) {
               NiagaraDaemon.getFilter()
                  .severe("default admin group \"" + platformProvider.getDefaultAdminGroupName() + "\" not available, can not authenticate");
               return null;
            }

            if (!platformProvider.providesAccountManagement()) {
               props.setProperty("admingroupid", defaultAdminGroup.getPlatformIdentifier());
               propertiesModified = true;
            }
         }
      } else {
         if (!DaemonAuthUtil.isFileScheme(authType)) {
            log.severe("unrecognized authentication type \"" + authType + "\" in domain factory, returning null");
            return null;
         }

         switch (authType) {
            case "scram-glibc-sha512/file":
               result = new SimpleAuthenticationDomain(props, true, platformProvider);
               break;
            default:
               log.severe("unrecognized file authentication type \"" + authType + "\" in domain factory, returning null");
               return null;
         }
      }

      if (propertiesModified) {
         NiagaraDaemon.saveProperties();
      }

      if (extraAdminUsers != null) {
         result.extraAdminUsers.putAll(extraAdminUsers);
      }

      if (extraUsers != null) {
         result.extraUsers.putAll(extraUsers);
      }

      return result;
   }

   public AuthenticationInfo makeAuthInfo(String providedUserName, String providedPasswordValue) {
      if (providedUserName == null || providedUserName.isEmpty()) {
         return null;
      }

      if (providedPasswordValue != null && !providedPasswordValue.isEmpty()) {
         AuthenticationInfo result = null;
         boolean hasAdminAccess = false;
         String extraPasswordValue = null;
         boolean correctCredentialsProvided = false;
         if (this.extraUsers.containsKey(providedUserName)) {
            hasAdminAccess = false;
            extraPasswordValue = this.extraUsers.getProperty(providedUserName, null);
         } else if (this.extraAdminUsers.containsKey(providedUserName)) {
            hasAdminAccess = true;
            extraPasswordValue = this.extraAdminUsers.getProperty(providedUserName, null);
         }

         if (SecurityUtil.equals(providedPasswordValue, extraPasswordValue)) {
            correctCredentialsProvided = true;
         }

         if (correctCredentialsProvided) {
            result = new SimpleAuthenticationInfo(providedUserName, hasAdminAccess, true);
         }

         return result;
      } else {
         return null;
      }
   }

   public String getRealm(HttpServletRequest request) {
      return "NIAGARA";
   }

   public abstract String getDomainType();

   protected AuthenticationDomain() {
   }

   public abstract boolean supportsPasswordClearTextRetrieval();

   public String getPasswordClearText(String userName) {
      String result = null;
      if (this.extraUsers.containsKey(userName)) {
         result = this.extraUsers.getProperty(userName, null);
      } else if (this.extraAdminUsers.containsKey(userName)) {
         result = this.extraAdminUsers.getProperty(userName, null);
      }

      return result;
   }

   public abstract boolean supportsPasswordHashRetrieval();

   public String getPasswordHash(String userName) {
      String result = null;
      if (this.extraUsers.containsKey(userName)) {
         result = this.extraUsers.getProperty(userName, null);
      } else if (this.extraAdminUsers.containsKey(userName)) {
         result = this.extraAdminUsers.getProperty(userName, null);
      }

      return result;
   }

   public Properties getExtraUsers() {
      return this.extraUsers;
   }

   public Properties getExtraAdminUsers() {
      return this.extraAdminUsers;
   }

   public void addExtraUser(String username, String password) {
      this.extraUsers.setProperty(username, password);
   }

   public void addExtraAdminUser(String username, String password) {
      this.extraAdminUsers.setProperty(username, password);
   }

   public boolean isExtraUser(String username) {
      return this.extraUsers.containsKey(username);
   }

   public boolean isExtraAdminUser(String username) {
      return this.extraAdminUsers.containsKey(username);
   }
}
