package com.tridium.niagarad.security;

public class SimpleAuthenticationInfo extends AuthenticationInfo {
   protected String username;
   private boolean hostAdminAccess;
   protected boolean extra;

   public SimpleAuthenticationInfo(String pUsername, boolean pHostAdminAccess, boolean pExtra) {
      this.username = pUsername;
      this.hostAdminAccess = pHostAdminAccess;
      this.extra = pExtra;
   }

   public boolean hasHostAdminAccess() {
      return this.hostAdminAccess;
   }

   @Override
   public String getUsername() {
      return this.username;
   }

   public boolean isExtra() {
      return this.extra;
   }
}
