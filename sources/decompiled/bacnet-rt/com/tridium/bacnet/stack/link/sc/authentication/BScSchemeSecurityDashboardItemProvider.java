package com.tridium.bacnet.stack.link.sc.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.baja.agent.BIAgent;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.role.BRoleService;
import javax.baja.security.BPermissionsMap;
import javax.baja.security.dashboard.BISecurityDashboardItemProvider;
import javax.baja.security.dashboard.SecurityDashboardItem;
import javax.baja.security.dashboard.SecurityDashboardItemBuilder;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:AuthenticationServiceSecurityDashboardProviderAgent"}
   )}
)
public class BScSchemeSecurityDashboardItemProvider extends BObject implements BISecurityDashboardItemProvider, BIAgent {
   public static final Type TYPE = Sys.loadType(BScSchemeSecurityDashboardItemProvider.class);
   private static final int VERSION = 1;
   private static final String SC_USERS_DESCRIPTION = "securityDashboard.scAuthScheme.users.description";
   private static final SecurityDashboardItemBuilder builder = new SecurityDashboardItemBuilder(TYPE);

   public Type getType() {
      return TYPE;
   }

   public int getSecurityDashboardItemsVersion() {
      return 1;
   }

   public List<SecurityDashboardItem> getSecurityDashboardItems(Context cx) {
      List<SecurityDashboardItem> items = new ArrayList<>();
      List<BUser> scUsers = getScUsers();
      if (scUsers.isEmpty()) {
         return Collections.singletonList(
            builder.makeInfo("securityDashboard.scAuthScheme.noUsers.summary", "securityDashboard.scAuthScheme.noUsers.description")
         );
      } else {
         addPermissionItems(items, scUsers);
         return items;
      }
   }

   private static void addPermissionItems(List<SecurityDashboardItem> items, List<BUser> scUsers) {
      BRoleService roleService = BRoleService.getService();
      List<String> scUsersWithPermissions = new ArrayList<>(scUsers.size());
      List<String> scUsersWithNoPermissions = new ArrayList<>(scUsers.size());

      for (BUser user : scUsers) {
         if (BPermissionsMap.DEFAULT.equals(user.getPermissions(roleService))) {
            scUsersWithNoPermissions.add(user.getName());
         } else {
            scUsersWithPermissions.add(user.getName());
         }
      }

      if (!scUsersWithNoPermissions.isEmpty()) {
         items.add(
            builder.makeOk()
               .withSummary("securityDashboard.scAuthScheme.users.noPermissions.summary", new Object[]{String.join(", ", scUsersWithNoPermissions)})
               .withDescription("securityDashboard.scAuthScheme.users.description", new Object[0])
         );
      }

      if (!scUsersWithPermissions.isEmpty()) {
         items.add(
            builder.makeAlert()
               .withSummary("securityDashboard.scAuthScheme.users.withPermissions.summary", new Object[]{String.join(", ", scUsersWithPermissions)})
               .withDescription("securityDashboard.scAuthScheme.users.description", new Object[0])
         );
      }
   }

   private static List<BUser> getScUsers() {
      BUserService userService = BUserService.getService();
      if (userService == null) {
         return Collections.emptyList();
      } else {
         List<BUser> scUsers = new ArrayList<>();

         for (BUser user : userService.getUsers()) {
            if (user.getAuthenticator() instanceof BBacnetScAuthenticator) {
               scUsers.add(user);
            }
         }

         return scUsers;
      }
   }
}
