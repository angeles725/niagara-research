package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.auth.NativeAccount;
import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.util.LegacyStorageUtil;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccountManagementServlet extends DaemonServlet {
   private Logger filter;
   private final IPlatformProvider platformProvider;

   public AccountManagementServlet(IPlatformProvider platformProvider) {
      super("acctmgt");
      this.platformProvider = platformProvider;
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("acctmgt");
      return true;
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      Properties props = NiagaraDaemon.props;
      String[] supportedAuthTypes = TextUtil.split(this.platformProvider.getSupportedAuthenticationTypes(), ',');
      boolean nativeSchemeFound = false;

      for (String supportedAuthType : supportedAuthTypes) {
         if (DaemonAuthUtil.isNativeScheme(supportedAuthType)) {
            nativeSchemeFound = true;
            break;
         }
      }

      if (!nativeSchemeFound) {
         return 501;
      }

      boolean defaultLocal;
      if (props.containsKey("defaultlocal")) {
         defaultLocal = Boolean.valueOf(props.getProperty("defaultlocal", "true"));
      } else {
         defaultLocal = this.platformProvider.getComputerDomain(true) == null;
      }

      String domain = this.platformProvider.getComputerDomain(true);
      String machine = this.platformProvider.getComputerName();
      String defaultRealm = defaultLocal ? machine : domain;
      String username = this.getServer().getAuthenticator().getRequestUserName(request);
      if (query.containsKey("update")) {
         if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
            MessageBundle msg = new MessageBundle("invalid CSRF token in request");
            handler.error(msg);
            this.filter.severe("invalid CSRF token in request");
            return 403;
         }

         String action = query.get("update", null);
         if (action == null) {
            return 400;
         }

         if (this.platformProvider.isAuthenticationReadonly()) {
            MessageBundle msg = new MessageBundle("authentication settings are readonly");
            handler.error(msg);
            this.filter.severe("authentication settings are readonly");
            return 400;
         }

         switch (action) {
            case "adduser":
               String sharedKeyAttributeNamex = "sharedKey_" + query.get("sharedKeyName", null);
               SharedSecretKey sharedSecretKeyx = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeNamex);
               String user = query.get("user", null);
               String comment = query.get("comment", "");
               String encodedPassword = query.get("password", null);
               boolean passwordHashed = Boolean.valueOf(query.get("hash", "false"));
               if (user == null) {
                  MessageBundle msg = new MessageBundle("Missing user argument");
                  handler.error(msg);
                  return 400;
               } else {
                  try {
                     user = this.decodeMessage(user, sharedSecretKeyx);
                  } catch (Exception e) {
                     MessageBundle msg = new MessageBundle("Could not decrypt user");
                     handler.error(msg);
                     return 400;
                  }

                  if (NativeAccount.fullyQualifiedToUsername(user).equals(this.platformProvider.getDefaultUsername())) {
                     MessageBundle msg = new MessageBundle("Invalid user argument");
                     handler.error(msg);
                     return 400;
                  } else if (NativeAccount.RESERVED_NAMES.contains(NativeAccount.fullyQualifiedToUsername(user))) {
                     MessageBundle msg = new MessageBundle("Invalid user argument");
                     handler.error(msg);
                     return 400;
                  } else {
                     UserAccount userAccount = (UserAccount)this.platformProvider.getAccountFromName(user, defaultRealm, true);
                     if (userAccount != null) {
                        MessageBundle msg = new MessageBundle("User " + user + " already exists");
                        handler.error(msg);
                        return 400;
                     } else if (encodedPassword == null) {
                        MessageBundle msg = new MessageBundle("Missing password argument");
                        handler.error(msg);
                        return 400;
                     } else {
                        String decodedPassword;
                        try {
                           decodedPassword = LegacyStorageUtil.decode(this.decodeMessage(encodedPassword, sharedSecretKeyx));
                        } catch (Exception e) {
                           MessageBundle msg = new MessageBundle("Invalid password argument");
                           handler.error(msg);
                           return 400;
                        }

                        boolean commentValid = true;

                        for (int i = 0; i < comment.length(); i++) {
                           char c = comment.charAt(i);
                           if (!"-=+()@._ ".contains(String.valueOf(c)) && !Character.isLetterOrDigit(c)) {
                              commentValid = false;
                              break;
                           }

                           if (i >= 64) {
                              commentValid = false;
                              break;
                           }
                        }

                        if (!commentValid) {
                           MessageBundle msg = new MessageBundle("Invalid comment argument");
                           handler.error(msg);
                           return 400;
                        } else if (decodedPassword.equals(this.platformProvider.getDefaultPassword())) {
                           MessageBundle msg = new MessageBundle("Invalid password argument");
                           handler.error(msg);
                           return 400;
                        } else {
                           String userId = this.platformProvider.addUserAccount(user, decodedPassword, comment, passwordHashed);
                           if (userId == null) {
                              MessageBundle msg = new MessageBundle("Failed to add user \"" + user + "\"");
                              handler.error(msg);
                              this.filter.severe("failed to add user \"" + user + "\"");
                              return 500;
                           } else {
                              content.w("<user account=\"" + user + "\" id=\"" + userId + "\"/>\n");
                              this.filter.info("user \"" + username + "\" added user \"" + user + "\"");
                              return 200;
                           }
                        }
                     }
                  }
               }
            case "deluser":
               String userArgx = query.get("user", null);
               String userIdArgx = query.get("userid", null);
               if (userArgx == null && userIdArgx == null) {
                  MessageBundle msg = new MessageBundle("Missing user/userid argument");
                  handler.error(msg);
                  return 400;
               } else {
                  UserAccount userAccount = userIdArgx != null
                     ? (UserAccount)this.platformProvider.getAccountFromId(userIdArgx, true)
                     : (UserAccount)this.platformProvider.getAccountFromName(userArgx, defaultRealm, true);
                  if (userAccount == null) {
                     MessageBundle msg = new MessageBundle("User " + (userIdArgx != null ? userIdArgx : userArgx) + " does not exist");
                     handler.error(msg);
                     return 400;
                  } else if (NativeAccount.RESERVED_NAMES.contains(userAccount.getAccountName())) {
                     MessageBundle msg = new MessageBundle("Invalid user argument");
                     handler.error(msg);
                     return 400;
                  } else if (userAccount.getAccountName().equals(this.getServer().getAuthenticator().getRequestUserName(request))) {
                     MessageBundle msg = new MessageBundle("Invalid user argument");
                     handler.error(msg);
                     return 400;
                  } else {
                     if (!this.platformProvider.removeUserAccount(userAccount.getPlatformIdentifier())) {
                        MessageBundle msg = new MessageBundle("Failed to remove user \"" + userAccount.getAccountName() + "\"");
                        handler.error(msg);
                        this.filter.severe("failed to remove user \"" + userAccount.getAccountName() + "\"");
                        return 500;
                     }

                     this.filter.info("user \"" + username + "\" removed user \"" + userAccount.getAccountName() + "\"");
                     return 200;
                  }
               }
            case "addgrpmbr":
            case "delgrpmbr":
               boolean add = action.equals("addgrpmbr");
               String memberArg = query.get("member", null);
               String memberIdArg = query.get("memberid", null);
               if (memberArg == null && memberIdArg == null) {
                  MessageBundle msg = new MessageBundle("Missing member/memberid argument");
                  handler.error(msg);
                  return 400;
               } else {
                  UserAccount memberAccount = memberIdArg != null
                     ? (UserAccount)this.platformProvider.getAccountFromId(memberIdArg, true)
                     : (UserAccount)this.platformProvider.getAccountFromName(memberArg, defaultRealm, true);
                  String groupArg = query.get("group", null);
                  String groupIdArg = query.get("groupid", null);
                  if (groupArg == null && groupIdArg == null) {
                     MessageBundle msg = new MessageBundle("Missing group/groupid argument");
                     handler.error(msg);
                     return 400;
                  } else {
                     GroupAccount groupAccount = groupIdArg != null
                        ? (GroupAccount)this.platformProvider.getAccountFromId(groupIdArg, false)
                        : (GroupAccount)this.platformProvider.getAccountFromName(groupArg, defaultRealm, false);
                     if (groupAccount == null) {
                        MessageBundle msg = new MessageBundle("Group not found");
                        handler.error(msg);
                        return 400;
                     } else if (memberAccount == null) {
                        MessageBundle msg = new MessageBundle("Member not found");
                        handler.error(msg);
                        return 400;
                     } else if (NativeAccount.RESERVED_NAMES.contains(memberAccount.getAccountName())) {
                        MessageBundle msg = new MessageBundle("Invalid user argument");
                        handler.error(msg);
                        return 400;
                     } else if (add) {
                        if (this.platformProvider.isGroupMember(memberAccount.getPlatformIdentifier(), groupAccount.getPlatformIdentifier())) {
                           MessageBundle msg = new MessageBundle("User is already a member of group");
                           handler.error(msg);
                           return 400;
                        } else {
                           if (!this.platformProvider.addUserToGroup(memberAccount.getPlatformIdentifier(), groupAccount.getPlatformIdentifier())) {
                              MessageBundle msg = new MessageBundle(
                                 "Failed to add \"" + memberAccount.getAccountName() + "\" to group \"" + groupAccount.getAccountName() + "\""
                              );
                              handler.error(msg);
                              this.filter.severe("failed to add \"" + memberAccount.getAccountName() + "\" to group \"" + groupAccount.getAccountName() + "\"");
                              return 500;
                           }

                           this.filter
                              .info(
                                 "user \""
                                    + username
                                    + "\" added user \""
                                    + memberAccount.getAccountName()
                                    + "\" to group \""
                                    + groupAccount.getAccountName()
                                    + "\""
                              );
                           return 200;
                        }
                     } else if (!this.platformProvider.isGroupMember(memberAccount.getPlatformIdentifier(), groupAccount.getPlatformIdentifier())) {
                        MessageBundle msg = new MessageBundle("User is already a member of group");
                        handler.error(msg);
                        return 400;
                     } else if (groupAccount.equals(this.platformProvider.getDefaultAdminGroup())
                        && memberAccount.getAccountName().equals(this.getServer().getAuthenticator().getRequestUserName(request))) {
                        MessageBundle msg = new MessageBundle("Invalid user argument");
                        handler.error(msg);
                        return 400;
                     } else {
                        if (!this.platformProvider.removeUserFromGroup(memberAccount.getPlatformIdentifier(), groupAccount.getPlatformIdentifier())) {
                           MessageBundle msg = new MessageBundle(
                              "Failed to remove \"" + memberAccount.getAccountName() + "\" from group \"" + groupAccount.getAccountName() + "\""
                           );
                           handler.error(msg);
                           this.filter
                              .severe("failed to remove \"" + memberAccount.getAccountName() + "\" from group \"" + groupAccount.getAccountName() + "\"");
                           return 500;
                        }

                        this.filter
                           .info(
                              "user \""
                                 + username
                                 + "\" removed user \""
                                 + memberAccount.getAccountName()
                                 + "\" from group \""
                                 + groupAccount.getAccountName()
                                 + "\""
                           );
                        return 200;
                     }
                  }
               }
            case "changepassword":
               String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
               SharedSecretKey sharedSecretKey = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeName);
               String userArg = query.get("user", null);
               String userIdArg = query.get("userid", null);
               String encodedOldPassword = query.get("oldpassword", null);
               String encodedNewPassword = query.get("newpassword", null);
               if (sharedSecretKey == null) {
                  MessageBundle msg = new MessageBundle("Missing shared key argument");
                  handler.error(msg);
                  return 400;
               } else if (userArg == null && userIdArg == null) {
                  MessageBundle msg = new MessageBundle("Missing user/userid argument");
                  handler.error(msg);
                  return 400;
               } else {
                  try {
                     userArg = userArg != null ? this.decodeMessage(userArg, sharedSecretKey) : null;
                     userIdArg = userIdArg != null ? this.decodeMessage(userIdArg, sharedSecretKey) : null;
                  } catch (Exception e) {
                     MessageBundle msg = new MessageBundle("Could not decrypt user/userid");
                     handler.error(msg);
                     return 400;
                  }

                  UserAccount userAccount = userIdArg != null
                     ? (UserAccount)this.platformProvider.getAccountFromId(userIdArg, true)
                     : (UserAccount)this.platformProvider.getAccountFromName(userArg, defaultRealm, true);
                  if (userAccount == null) {
                     MessageBundle msg = new MessageBundle("User not found");
                     handler.error(msg);
                     return 400;
                  } else if (NativeAccount.RESERVED_NAMES.contains(userAccount.getAccountName())) {
                     MessageBundle msg = new MessageBundle("Invalid user argument");
                     handler.error(msg);
                     return 400;
                  } else if (encodedOldPassword == null) {
                     MessageBundle msg = new MessageBundle("Missing oldpassword argument");
                     handler.error(msg);
                     return 400;
                  } else {
                     String decodedOldPassword;
                     try {
                        decodedOldPassword = LegacyStorageUtil.decode(this.decodeMessage(encodedOldPassword, sharedSecretKey));
                     } catch (Exception e) {
                        MessageBundle msg = new MessageBundle("Failed to change password for user \"" + userAccount.getAccountName() + "\"");
                        handler.error(msg);
                        return 400;
                     }

                     if (!userAccount.isPasswordValid(this.platformProvider, decodedOldPassword)) {
                        MessageBundle msg = new MessageBundle("Failed to change password for user \"" + userAccount.getAccountName() + "\"");
                        handler.error(msg);
                        this.filter.warning("password change by user \"" + username + "\" for user \"" + userAccount.getAccountName() + "\" failed");
                        return 400;
                     } else if (encodedNewPassword == null) {
                        MessageBundle msg = new MessageBundle("Missing newpassword argument");
                        handler.error(msg);
                        return 400;
                     } else {
                        String decodedNewPassword;
                        try {
                           decodedNewPassword = LegacyStorageUtil.decode(this.decodeMessage(encodedNewPassword, sharedSecretKey));
                        } catch (Exception ignored) {
                           MessageBundle msg = new MessageBundle("Failed to change password for user \"" + userAccount.getAccountName() + "\"");
                           handler.error(msg);
                           return 400;
                        }

                        if (decodedNewPassword.equals(this.platformProvider.getDefaultPassword())) {
                           MessageBundle msg = new MessageBundle("Invalid password argument");
                           handler.error(msg);
                           return 400;
                        } else {
                           if (!this.platformProvider.changeUserPassword(userAccount.getPlatformIdentifier(), decodedOldPassword, decodedNewPassword)) {
                              MessageBundle msg = new MessageBundle("Failed to change password for user \"" + userAccount.getAccountName() + "\"");
                              handler.error(msg);
                              this.filter.severe("failed to change password for user \"" + userAccount.getAccountName() + "\"");
                              return 500;
                           }

                           this.filter.info("password change by user \"" + username + "\" for user \"" + userAccount.getAccountName() + "\" successful");
                           return 200;
                        }
                     }
                  }
               }
            default:
               return 400;
         }
      } else if (query.containsKey("check")) {
         if (this.platformProvider.isAuthenticationReadonly()) {
            MessageBundle msg = new MessageBundle("authentication settings are readonly");
            handler.error(msg);
            this.filter.severe("authentication settings are readonly");
            return 400;
         }

         String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
         SharedSecretKey sharedSecretKey = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeName);
         String encodedUser = query.get("user", null);
         String encodedPassword = query.get("password", null);
         if (sharedSecretKey == null) {
            MessageBundle msg = new MessageBundle("Missing shared key argument");
            handler.error(msg);
            return 400;
         }

         if (encodedUser == null) {
            MessageBundle msg = new MessageBundle("Missing user argument");
            handler.error(msg);
            return 400;
         }

         String decodedUser;
         try {
            decodedUser = this.decodeMessage(encodedUser, sharedSecretKey);
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("Invalid user argument");
            handler.error(msg);
            return 400;
         }

         if (encodedPassword == null) {
            MessageBundle msg = new MessageBundle("Missing password argument");
            handler.error(msg);
            return 400;
         }

         String decodedPassword;
         try {
            decodedPassword = this.decodeMessage(encodedPassword, sharedSecretKey);
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("Invalid password argument");
            handler.error(msg);
            return 400;
         }

         if (NativeAccount.fullyQualifiedToUsername(decodedUser).equals(this.platformProvider.getDefaultUsername())) {
            MessageBundle msg = new MessageBundle("Invalid user argument");
            handler.error(msg);
            return 400;
         } else if (NativeAccount.RESERVED_NAMES.contains(NativeAccount.fullyQualifiedToUsername(decodedUser))) {
            MessageBundle msg = new MessageBundle("Invalid user argument");
            handler.error(msg);
            return 400;
         } else if (decodedPassword.equals(this.platformProvider.getDefaultPassword())) {
            MessageBundle msg = new MessageBundle("Invalid password argument");
            handler.error(msg);
            return 400;
         } else {
            return 200;
         }
      } else {
         String adminGroupXmlString = this.platformProvider.getAccountXml(this.platformProvider.getDefaultAdminGroup().getPlatformIdentifier(), false);
         if (query.containsKey("sendHash")) {
            XElem adminGroupXml;
            try {
               adminGroupXml = XParser.make(adminGroupXmlString).parse();
            } catch (Exception e) {
               MessageBundle msg = new MessageBundle("Failed to retrieve account xml (" + e + ")");
               handler.error(msg);
               this.filter.log(Level.SEVERE, "failed to retrieve account xml", e);
               return 400;
            }

            XElem[] adminUserElems = adminGroupXml.elems("user");

            for (XElem adminUserElem : adminUserElems) {
               String accountName = NativeAccount.fullyQualifiedToUsername(adminUserElem.get("name"));
               adminUserElem.addAttr("passwordHash", this.platformProvider.getPasswordHash(accountName));
            }

            ByteBuffer buffer = new ByteBuffer();
            XWriter out = new XWriter();
            out.setOutputStream(buffer.getOutputStream());
            out.prolog();
            adminGroupXml.write(out);
            out.flush();
            out.close();
            adminGroupXmlString = new String(buffer.getBytes(), 0, buffer.getLength());
         }

         content.w("<accountInfo>\n");
         if (adminGroupXmlString != null) {
            content.write(adminGroupXmlString);
         }

         content.w("</accountInfo>\n");
         return 200;
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      boolean requireAdmin = false;
      String queryString = req.getQueryString();
      if (queryString != null) {
         KeyedList query = Http.getGetForm(queryString);
         requireAdmin = query.containsKey("update");
      }

      return requireAdmin
         ? DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp)
         : DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), req, resp);
   }

   private String decodeMessage(String message, SharedSecretKey sharedSecretKey) throws Exception {
      return sharedSecretKey.decrypt(Base64.getDecoder().decode(message)).asString(true, StandardCharsets.UTF_8);
   }
}
