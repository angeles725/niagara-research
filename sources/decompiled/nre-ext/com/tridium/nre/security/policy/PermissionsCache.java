package com.tridium.nre.security.policy;

import java.lang.reflect.Constructor;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.UnresolvedPermission;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PermissionsCache extends PermissionCollection {
   private Logger LOG = Logger.getLogger("security.niagaraPolicy.permissions");
   private final Map<Class<? extends Permission>, PermissionsCache.PermissionCollectionSet> cache = new HashMap<>();
   private final Map<String, Set<UnresolvedPermission>> unresolvedPermissions = new HashMap<>();
   private boolean allPermission = false;

   @Override
   public boolean implies(Permission p) {
      if (this.allPermission) {
         return true;
      }

      synchronized (this) {
         PermissionCollection pc = this.getPermissionsCache(p);
         return pc.implies(p);
      }
   }

   @Override
   public void add(Permission p) {
      if (p instanceof AllPermission) {
         this.allPermission = true;
      }

      synchronized (this) {
         if (p instanceof NiagaraPermission) {
            NiagaraPermission np = (NiagaraPermission)p;
            np.getImpliedPermissions().forEach(this::add);
            Set<Class<? extends Permission>> classes = np.getSupportedClasses();
            if (!classes.isEmpty()) {
               PermissionCollection pc = np.newPermissionCollection();
               pc.add(p);
               classes.forEach(
                  c -> this.cache.computeIfAbsent((Class<? extends Permission>)c, k -> new PermissionsCache.PermissionCollectionSet(c)).addCollection(pc)
               );
            }
         } else if (p instanceof UnresolvedPermission) {
            this.unresolvedPermissions.computeIfAbsent(p.getName(), k -> new HashSet<>()).add((UnresolvedPermission)p);
         } else {
            this.cache.computeIfAbsent((Class<? extends Permission>)p.getClass(), k -> new PermissionsCache.PermissionCollectionSet(p)).add(p);
         }
      }
   }

   @Override
   public synchronized Enumeration<Permission> elements() {
      return new PermissionsCache.PermissionEnumeration(this.cache.values().iterator());
   }

   private PermissionCollection getPermissionsCache(Permission permission) {
      PermissionCollection pc = this.cache
         .computeIfAbsent((Class<? extends Permission>)permission.getClass(), k -> new PermissionsCache.PermissionCollectionSet(permission));
      synchronized (this) {
         if (this.unresolvedPermissions.containsKey(permission.getClass().getName())) {
            Iterator<UnresolvedPermission> iterator = this.unresolvedPermissions.get(permission.getClass().getName()).iterator();

            while (iterator.hasNext()) {
               UnresolvedPermission unresolvedPermission = iterator.next();
               Permission resolvedPermission = this.resolvePermission(unresolvedPermission, permission);
               if (resolvedPermission != null) {
                  if (this.LOG.isLoggable(Level.FINE)) {
                     this.LOG.fine("Resolved permission: " + resolvedPermission.toString());
                  }

                  pc.add(resolvedPermission);
                  iterator.remove();
               }
            }
         }

         return pc;
      }
   }

   private Permission resolvePermission(UnresolvedPermission unresolved, Permission permission) {
      Certificate[] unresolvedCerts = unresolved.getUnresolvedCerts();
      if (unresolvedCerts != null) {
         Object[] permissionCerts = permission.getClass().getSigners();
         if (permissionCerts == null) {
            if (this.LOG.isLoggable(Level.FINER)) {
               this.LOG
                  .finer("Did not resolve " + unresolved.toString() + ". Provided permission class <" + permission.getClass().getName() + "> has no signers");
            }

            return null;
         }

         List<Certificate> unresolvedCertList = Arrays.asList(unresolvedCerts);
         List<Object> permissionCertList = Arrays.asList(permissionCerts);
         if (!permissionCertList.containsAll(unresolvedCertList)) {
            if (this.LOG.isLoggable(Level.FINER)) {
               this.LOG
                  .finer(
                     "Did not resolve " + unresolved.toString() + ". Provided permission class <" + permission.getClass().getName() + "> signers do not match"
                  );
            }

            return null;
         }
      }

      Class<?> permissionClass = permission.getClass();

      try {
         String name = unresolved.getUnresolvedName();
         String actions = unresolved.getUnresolvedActions();
         if (name == null && actions == null) {
            try {
               Constructor<?> noArg = permissionClass.getConstructor();
               return (Permission)noArg.newInstance();
            } catch (NoSuchMethodException e1) {
               try {
                  Constructor<?> oneArg = permissionClass.getConstructor(String.class);
                  return (Permission)oneArg.newInstance(name);
               } catch (NoSuchMethodException e2) {
                  Constructor<?> twoArgs = permissionClass.getConstructor(String.class, String.class);
                  return (Permission)twoArgs.newInstance(name, actions);
               }
            }
         }

         if (name != null && actions == null) {
            try {
               Constructor<?> oneArg = permissionClass.getConstructor(String.class);
               return (Permission)oneArg.newInstance(name);
            } catch (NoSuchMethodException e2) {
               Constructor<?> twoArgs = permissionClass.getConstructor(String.class, String.class);
               return (Permission)twoArgs.newInstance(name, actions);
            }
         }

         Constructor<?> twoArgs = permissionClass.getConstructor(String.class, String.class);
         return (Permission)twoArgs.newInstance(name, actions);
      } catch (NoSuchMethodException e) {
         if (this.LOG.isLoggable(Level.FINE)) {
            this.LOG.log(Level.WARNING, "Could not find constructor for permission class " + unresolved.getUnresolvedType(), e);
         } else {
            this.LOG.log(Level.WARNING, "Could not find constructor for permission class " + unresolved.getUnresolvedType());
         }
      } catch (Exception e) {
         if (this.LOG.isLoggable(Level.FINE)) {
            this.LOG.log(Level.WARNING, "Could not instantiate " + unresolved.getUnresolvedType(), e);
         } else {
            this.LOG.log(Level.WARNING, "Could not instantiate " + unresolved.getUnresolvedType());
         }
      }

      return null;
   }

   private static class PermissionCollectionSet extends PermissionCollection {
      private final Class<? extends Permission> cls;
      private Optional<PermissionCollection> classCollection = Optional.empty();
      private final Set<PermissionCollection> otherCollections = new HashSet<>();

      public PermissionCollectionSet(Class<? extends Permission> cls) {
         this.cls = cls;
      }

      public PermissionCollectionSet(Permission p) {
         this.cls = (Class<? extends Permission>)p.getClass();
         this.classCollection = Optional.ofNullable(this.getClassCollection(p));
      }

      @Override
      public void add(Permission p) {
         if (p.getClass() != this.cls) {
            throw new IllegalArgumentException("Cannot add class");
         }

         synchronized (this) {
            if (!this.classCollection.isPresent()) {
               this.classCollection = Optional.ofNullable(this.getClassCollection(p));
            }

            this.classCollection.ifPresent(pc -> pc.add(p));
         }
      }

      @Override
      public synchronized boolean implies(Permission p) {
         return this.classCollection.filter(pc -> pc.implies(p)).isPresent()
            ? true
            : this.otherCollections.stream().filter(pc -> pc.implies(p)).findAny().isPresent();
      }

      @Override
      public synchronized Enumeration<Permission> elements() {
         LinkedList<PermissionCollection> pcs = new LinkedList<>(this.otherCollections);
         this.classCollection.ifPresent(pcs::add);
         return new PermissionsCache.PermissionEnumeration(pcs.iterator());
      }

      public synchronized void addCollection(PermissionCollection pc) {
         for (PermissionCollection otherCollection : this.otherCollections) {
            if (otherCollection.getClass().equals(pc.getClass())) {
               Enumeration<Permission> perms = pc.elements();

               while (perms.hasMoreElements()) {
                  otherCollection.add(perms.nextElement());
               }

               return;
            }
         }

         this.otherCollections.add(pc);
      }

      private PermissionCollection getClassCollection(Permission p) {
         PermissionCollection pc = p.newPermissionCollection();
         if (pc == null) {
            pc = new SimplePermissionCollection(this.cls);
         }

         return pc;
      }
   }

   private static class PermissionEnumeration implements Enumeration<Permission> {
      private final Iterator<? extends PermissionCollection> iterator;
      private Enumeration<Permission> current;

      public PermissionEnumeration(Iterator<? extends PermissionCollection> iterator) {
         this.iterator = iterator;
         this.current = this.nextPermissionCollection();
      }

      @Override
      public boolean hasMoreElements() {
         if (this.current == null) {
            return false;
         }

         if (this.current.hasMoreElements()) {
            return true;
         }

         this.current = this.nextPermissionCollection();
         return this.current != null;
      }

      public Permission nextElement() {
         if (this.hasMoreElements()) {
            return this.current.nextElement();
         } else {
            throw new NoSuchElementException("No more permissions");
         }
      }

      private Enumeration<Permission> nextPermissionCollection() {
         while (this.iterator.hasNext()) {
            PermissionCollection pc = this.iterator.next();
            if (pc.elements().hasMoreElements()) {
               return pc.elements();
            }
         }

         return null;
      }
   }
}
