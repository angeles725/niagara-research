package com.tridium.nre.security;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;

public final class KeyStorePermission extends Permission {
   private static final int READ = 1;
   private static final int WRITE = 2;
   private static final String READ_ACTION = "read";
   private static final String WRITE_ACTION = "write";
   private static final String ALL_ACTION = "all";
   private static final String WILDCARD = "*";
   private final int mask;
   private final String canonicalActions;

   public KeyStorePermission(String name, String actions) {
      super(name);
      this.mask = getMask(actions);
      this.canonicalActions = canonicalizeActions(this.mask);
   }

   private static int getMask(String actions) {
      int mask = 0;
      if (actions == null) {
         return mask;
      }

      StringTokenizer tokenizer = new StringTokenizer(actions, ",");

      while (tokenizer.hasMoreTokens()) {
         String action = tokenizer.nextToken().trim();
         if (action.equalsIgnoreCase("read")) {
            mask |= 1;
         } else if (action.equalsIgnoreCase("write")) {
            mask |= 2;
         } else {
            if (!action.equalsIgnoreCase("all")) {
               throw new IllegalArgumentException("Invalid action: " + action);
            }

            mask = 3;
         }
      }

      return mask;
   }

   private static String canonicalizeActions(int mask) {
      StringJoiner joiner = new StringJoiner(",");
      if ((mask & 1) != 0) {
         joiner.add("read");
      }

      if ((mask & 2) != 0) {
         joiner.add("write");
      }

      return joiner.toString();
   }

   private boolean impliesIgnoreMask(Permission permission) {
      String thisName = this.getName();
      String otherName = permission.getName();
      return permission instanceof KeyStorePermission && (thisName.equals(otherName) || thisName.equals("*"));
   }

   @Override
   public boolean implies(Permission permission) {
      if (this.impliesIgnoreMask(permission)) {
         int desired = ((KeyStorePermission)permission).mask;
         return (this.mask & desired) == desired;
      } else {
         return false;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }

      if (!(obj instanceof KeyStorePermission)) {
         return false;
      }

      KeyStorePermission other = (KeyStorePermission)obj;
      return this.mask == other.mask && this.getName().equals(other.getName());
   }

   @Override
   public int hashCode() {
      return this.getName().hashCode() * this.mask;
   }

   @Override
   public String getActions() {
      return this.canonicalActions;
   }

   @Override
   public PermissionCollection newPermissionCollection() {
      return new KeyStorePermission.KeyStorePermissionCollection();
   }

   public static void checkRead(String storeName) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new KeyStorePermission(storeName, "read"));
      }
   }

   public static void checkWrite(String storeName) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new KeyStorePermission(storeName, "write"));
      }
   }

   private static final class KeyStorePermissionCollection extends PermissionCollection {
      private final List<Permission> permissions = new ArrayList<>();

      private KeyStorePermissionCollection() {
      }

      @Override
      public void add(Permission permission) {
         if (!(permission instanceof KeyStorePermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
         }

         synchronized (this) {
            this.permissions.add(permission);
         }
      }

      @Override
      public boolean implies(Permission permission) {
         if (!(permission instanceof KeyStorePermission)) {
            return false;
         }

         KeyStorePermission kp = (KeyStorePermission)permission;
         int effective = 0;
         synchronized (this) {
            for (Permission perm : this.permissions) {
               if (((KeyStorePermission)perm).impliesIgnoreMask(kp)) {
                  effective |= ((KeyStorePermission)perm).mask;
               }
            }
         }

         int desired = kp.mask;
         return (effective & desired) == desired;
      }

      @Override
      public Enumeration<Permission> elements() {
         return Collections.enumeration(this.permissions);
      }
   }
}
