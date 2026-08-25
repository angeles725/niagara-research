package com.tridium.nre.security.policy;

import java.security.Permission;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class WildcardPermissionMapper implements PermissionMapper {
   private final char wildcardChar;
   private final String url;
   private final Set<Permission> permissions;
   public static final String ALL_URLS = "*";
   public static final char MATCH_ALL = '*';
   public static final char MATCH_ALL_RECURSIVE = '-';

   public WildcardPermissionMapper(String url, char wildcardChar, Set<Permission> permissions) {
      Objects.requireNonNull(url);
      Objects.requireNonNull(permissions);
      if (wildcardChar != '*' && wildcardChar != '-') {
         throw new IllegalArgumentException("Invalid wildcard '" + wildcardChar + "'");
      }

      this.url = url;
      this.wildcardChar = wildcardChar;
      this.permissions = permissions;
   }

   public WildcardPermissionMapper(String url, Set<Permission> permissions) {
      Objects.requireNonNull(url);
      Objects.requireNonNull(permissions);
      if (!isWildcardedUrl(url)) {
         throw new IllegalArgumentException("URL " + url + " is not a wildcard");
      }

      if ("*".equals(url)) {
         this.url = "*";
      } else {
         this.url = url.substring(0, url.length() - 1);
      }

      this.wildcardChar = url.charAt(url.length() - 1);
      if (this.wildcardChar != '*' && this.wildcardChar != '-') {
         throw new IllegalArgumentException("Invalid wildcard '" + this.wildcardChar + "'");
      }

      this.permissions = permissions;
   }

   @Override
   public boolean appliesTo(String otherUrl) {
      if ("*".equals(this.url)) {
         return true;
      } else if (otherUrl.equals(this.url)) {
         return true;
      } else if (this.wildcardChar == '-') {
         return otherUrl.startsWith(this.url);
      } else if (this.wildcardChar == '*' && otherUrl.startsWith(this.url)) {
         String remainingUrl = otherUrl.substring(this.url.length());
         return !remainingUrl.contains("/");
      } else {
         return false;
      }
   }

   @Override
   public Set<Permission> get(String url) {
      return this.appliesTo(url) ? this.permissions : Collections.emptySet();
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.permissions, this.url, this.wildcardChar);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (!(obj instanceof WildcardPermissionMapper)) {
         return false;
      }

      WildcardPermissionMapper that = (WildcardPermissionMapper)obj;
      return that.permissions.equals(this.permissions) && that.url.equals(this.url) && that.wildcardChar == this.wildcardChar;
   }

   @Override
   public String toString() {
      return this.url + this.wildcardChar;
   }

   public static boolean isWildcardedUrl(String url) {
      if ("*".equals(url)) {
         return true;
      }

      char lastChar = url.charAt(url.length() - 1);
      return lastChar == '*' || lastChar == '-';
   }
}
