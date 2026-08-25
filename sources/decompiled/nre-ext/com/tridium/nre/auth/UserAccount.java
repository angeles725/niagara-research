package com.tridium.nre.auth;

import com.tridium.nre.platform.IPlatformProvider;

public class UserAccount extends NativeAccount {
   private String comment = "";
   private String password = null;
   private String oldPassword = null;

   public UserAccount(String fullyQualifiedName, String platformIdentifier) {
      super(fullyQualifiedName, platformIdentifier);
   }

   public UserAccount(String fullyQualifiedName, String platformIdentifier, String comment, String password) {
      super(fullyQualifiedName, platformIdentifier);
      this.comment = comment;
      this.password = password;
   }

   public boolean isPasswordValid(IPlatformProvider platformProvider, String password) {
      return platformProvider.isPasswordValid(this.getPlatformIdentifier(), password);
   }

   public String getComment() {
      return this.comment;
   }

   public void setComment(String comment) {
      this.comment = comment;
   }

   public String getPassword() {
      return this.password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getOldPassword() {
      return this.oldPassword;
   }

   public void setOldPassword(String oldPassword) {
      this.oldPassword = oldPassword;
   }
}
