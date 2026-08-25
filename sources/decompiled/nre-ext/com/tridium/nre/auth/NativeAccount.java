package com.tridium.nre.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.baja.nre.util.TextUtil;

public abstract class NativeAccount implements Comparable<NativeAccount> {
   private String accountName;
   private String platformIdentifier;
   private String domain;
   private String fullyQualifiedName;
   public static final Set<String> RESERVED_NAMES = new HashSet<>();

   public NativeAccount() {
   }

   public NativeAccount(String fullyQualifiedName, String platformIdentifier) {
      if (platformIdentifier != null && platformIdentifier.length() != 0) {
         this.platformIdentifier = platformIdentifier;
      } else {
         this.platformIdentifier = null;
      }

      if (fullyQualifiedName == null) {
         throw new RuntimeException("Native account fullyQualifiedName is null");
      }

      if (fullyQualifiedName.contains("@")) {
         String[] tokens = TextUtil.split(fullyQualifiedName, '@');
         if (tokens.length != 2) {
            throw new RuntimeException("Native account \"" + fullyQualifiedName + "\" not fully qualified");
         }

         this.domain = tokens[1];
         this.accountName = tokens[0];
      } else {
         if (!fullyQualifiedName.contains("\\")) {
            throw new RuntimeException("Native account \"" + fullyQualifiedName + "\" not fully qualified");
         }

         String[] tokens = TextUtil.split(fullyQualifiedName, '\\');
         if (tokens.length != 2) {
            throw new RuntimeException("Native account \"" + fullyQualifiedName + "\" not fully qualified");
         }

         this.domain = tokens[0];
         this.accountName = tokens[1];
      }

      if (this.domain.length() != 0 && this.accountName.length() != 0) {
         this.fullyQualifiedName = this.domain + '\\' + this.accountName;
      } else {
         throw new RuntimeException("Native account \"" + fullyQualifiedName + "\" not fully qualified");
      }
   }

   public NativeAccount(String accountName, String accountDomain, String platformIdentifier) {
      this.accountName = accountName;
      this.domain = accountDomain;
      this.fullyQualifiedName = this.domain + '\\' + accountName;
      if (platformIdentifier != null && platformIdentifier.length() != 0) {
         this.platformIdentifier = platformIdentifier;
      } else {
         this.platformIdentifier = null;
      }
   }

   public void setPlatformIdentifier(String platformIdentifier) {
      this.platformIdentifier = platformIdentifier;
   }

   public String getPlatformIdentifier() {
      return this.platformIdentifier;
   }

   public String getAccountName() {
      return this.accountName;
   }

   public String getFullyQualifiedName() {
      return this.fullyQualifiedName;
   }

   public String getDomain() {
      return this.domain;
   }

   public boolean isGroup() {
      return this instanceof GroupAccount;
   }

   public boolean isUser() {
      return this instanceof UserAccount;
   }

   public static boolean usernamesMatch(String user1, String user2) {
      if (user1 == null != (user2 == null)) {
         return false;
      } else if (user1 == null) {
         return true;
      } else {
         return isAccountNameFullyQualified(user1) != isAccountNameFullyQualified(user2)
            ? fullyQualifiedToUsername(user1).equals(fullyQualifiedToUsername(user2))
            : user1.equals(user2);
      }
   }

   public static String fullyQualifiedToUsername(String raw) {
      if (raw.contains("@")) {
         String[] tokens = TextUtil.split(raw, '@');
         return tokens[0];
      } else if (raw.contains("\\")) {
         String[] tokens = TextUtil.split(raw, '\\');
         return tokens[1];
      } else {
         return raw;
      }
   }

   public static String fullyQualifiedToDomain(String raw) {
      if (raw.contains("@")) {
         String[] tokens = TextUtil.split(raw, '@');
         return tokens[1];
      } else if (raw.contains("\\")) {
         String[] tokens = TextUtil.split(raw, '\\');
         return tokens[0];
      } else {
         return null;
      }
   }

   public static boolean isAccountQualifierValid(String accountQualifier) {
      if (accountQualifier == null) {
         return false;
      } else if (accountQualifier.isEmpty()) {
         return false;
      } else {
         int index = -1;
         if ((index = accountQualifier.indexOf(64)) != -1) {
            return accountQualifier.indexOf(92) == -1 && index == accountQualifier.lastIndexOf(64) && index != 0 && index != accountQualifier.length() - 1;
         } else {
            return (index = accountQualifier.indexOf(92)) == -1
               ? true
               : accountQualifier.indexOf(64) == -1 && index == accountQualifier.lastIndexOf(92) && index != 0 && index != accountQualifier.length() - 1;
         }
      }
   }

   public static boolean isAccountNameFullyQualified(String accountName) {
      return !isAccountQualifierValid(accountName) ? false : accountName.contains("@") || accountName.contains("\\");
   }

   public static boolean isReservedName(String name) {
      return RESERVED_NAMES.contains(fullyQualifiedToUsername(name.toLowerCase()));
   }

   public int compareTo(NativeAccount acct) {
      return this.fullyQualifiedName.compareTo(acct.fullyQualifiedName);
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && o instanceof NativeAccount) {
         NativeAccount a = (NativeAccount)o;
         return a.platformIdentifier != null && this.platformIdentifier != null
            ? this.platformIdentifier.equals(a.platformIdentifier)
            : this.fullyQualifiedName.equals(a.fullyQualifiedName);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.fullyQualifiedName.hashCode();
   }

   @Override
   public String toString() {
      return this.fullyQualifiedName;
   }

   static {
      Collections.addAll(RESERVED_NAMES, "root", "sshd", "daemon", "niagarad", "station", "auth", "niagarad_admin", "niagarad_owners", "station_owners", "sshd");
   }
}
