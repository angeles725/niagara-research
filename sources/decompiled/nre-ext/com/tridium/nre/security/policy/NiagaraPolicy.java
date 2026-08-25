package com.tridium.nre.security.policy;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.CryptoStoreId;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.KeyRingPermission;
import com.tridium.nre.security.KeyStorePermission;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.util.CacheMap;
import com.tridium.nre.util.NiagaraFiles;
import com.tridium.nre.util.tuple.Pair;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.StringJoiner;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.security.auth.AuthPermission;

public final class NiagaraPolicy extends Policy {
   private AbstractPermissionGroupStore permissionGroupStore;
   private final Logger logger = Logger.getLogger("security.niagaraPolicy");
   private final ProtectionDomain policyProtectionDomain = NiagaraPolicy.class.getProtectionDomain();
   private NiagaraPolicy.PolicyType policyType;
   private Optional<Map<String, Map<NiagaraPolicy.PolicyType, Set<Permission>>>> niagaraPolicy = Optional.empty();
   private final List<PermissionMapper> permissionMappers = new LinkedList<>();
   private final Map<CodeSource, PermissionsCache> permissionsCacheMap = new ConcurrentHashMap<>();
   private static final Set<Permission> ALL_PERMISSION_SET = Collections.unmodifiableSet(Collections.singleton(new AllPermission()));
   private static final PermissionCollection EMPTY_PERMISSIONS_COLLECTION = new NiagaraPolicy.EmptyPermissionsCollection();
   private final Map<Pair<CodeSource, Permission>, Boolean> resultCache = new CacheMap<>(64);
   private final boolean isDaemon;

   public NiagaraPolicy() {
      this(false);
   }

   public NiagaraPolicy(boolean isDaemon) {
      this.isDaemon = isDaemon;
      Set<Permission> grantedToAll = new HashSet<>();
      grantedToAll.add(new PropertyPermission("*", "read"));
      grantedToAll.add(new AuthPermission("getSubject"));
      grantedToAll.add(new RuntimePermission("stopThread"));
      grantedToAll.add(new KeyRingPermission("javax.baja.security.BAes256PasswordEncoder.key"));
      grantedToAll.add(new KeyStorePermission(CryptoStoreId.SYSTEM_TRUST_STORE.getValue(), "read"));
      grantedToAll.add(new KeyStorePermission(CryptoStoreId.USER_TRUST_STORE.getValue(), "read"));
      this.permissionMappers.add(new WildcardPermissionMapper("*", grantedToAll));
      String niagaraHome = AccessController.doPrivileged(() -> System.getProperty("niagara.home")).replace(File.separatorChar, '/');
      String javaHome = AccessController.doPrivileged(() -> System.getProperty("java.home")).replace(File.separatorChar, '/');
      List<Path> extDirs = new LinkedList<>();
      if (isDaemon) {
         if (NiagaraPolicy.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.isEmbedded()) {
            extDirs.add(Paths.get(niagaraHome, "bin", "ext"));
         } else {
            try {
               Set<Permission> niagarad = new HashSet<>();
               niagarad.add(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
               niagarad.add(new FilePermission(NiagaraPolicy.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getHostFileName(), "read"));
               String binExtUrl = NiagaraPolicyUtil.canonicalizeCodeSource(Paths.get(niagaraHome, "bin", "ext"));
               this.permissionMappers.add(new WildcardPermissionMapper(binExtUrl, '*', niagarad));
               String nreUrl = NiagaraPolicyUtil.canonicalizeCodeSource(Paths.get(niagaraHome, "bin", "ext", "nre.jar"));
               this.permissionMappers.add(new NiagaraPolicy.CachedPermissionMapper(nreUrl, ALL_PERMISSION_SET));
            } catch (IOException e) {
               System.err.println("SEVERE [" + new Date() + "][security.niagaraPolicy] error initializing NiagaraPolicy (" + e + ")");
               e.printStackTrace();
            }
         }
      } else {
         extDirs.add(Paths.get(niagaraHome, "bin", "ext"));
      }

      extDirs.add(Paths.get(niagaraHome, "bin", "ext", "bcfips"));
      extDirs.add(Paths.get(niagaraHome, "bin", "ext", "bcstd"));
      extDirs.add(Paths.get(javaHome, "lib", "ext"));
      extDirs.stream().filter(x$0 -> Files.exists(x$0)).flatMap(path -> {
         try {
            return Stream.of(NiagaraPolicyUtil.canonicalizeCodeSource(path));
         } catch (IOException e) {
            return Stream.empty();
         }
      }).map(url -> new WildcardPermissionMapper(url, '*', ALL_PERMISSION_SET)).forEach(this.permissionMappers::add);
   }

   public void bootstrap() {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
      }

      AccessController.doPrivileged(() -> {
         this.readPermissionXml();
         this.updateFromXmlPolicy(NiagaraPolicy.PolicyType.ALL);
         this.refresh();
         return null;
      });
   }

   void init(AbstractPermissionGroupStore permissionGroupStore, NiagaraPolicy.PolicyType type) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
      }

      this.policyType = type;
      this.permissionGroupStore = permissionGroupStore;
      this.updateFromXmlPolicy(this.policyType);
      permissionGroupStore.open();
      this.permissionMappers.add(permissionGroupStore);
      this.refresh();
      this.niagaraPolicy.ifPresent(map -> {
         for (Entry<String, Map<NiagaraPolicy.PolicyType, Set<Permission>>> entry : map.entrySet()) {
            Map<NiagaraPolicy.PolicyType, Set<Permission>> innerMap = entry.getValue();
            innerMap.values().removeAll(innerMap.values());
         }

         map.values().removeAll(map.values());
      });
   }

   void grantPermissionGroup(String moduleUrl, NiagaraPermissionGroup permissionGroup) {
      this.permissionGroupStore.grantPermissionGroup(moduleUrl, permissionGroup);
   }

   void revokePermissionGroup(String moduleUrl, NiagaraPermissionGroup permissionGroup) {
      this.permissionGroupStore.revokePermissionGroup(moduleUrl, permissionGroup);
      this.refresh();
   }

   Set<NiagaraPermissionGroup> getAllPermissionGroups(String moduleUrl) {
      return this.permissionGroupStore.getAllPermissionGroups(moduleUrl);
   }

   NiagaraPolicy.PolicyType getPolicyType() {
      return this.policyType;
   }

   void requestPermissionGroup(String moduleUrl, NiagaraPermissionGroup permissionGroup) {
      this.requestPermissionGroups(moduleUrl, Collections.singleton(permissionGroup));
   }

   void requestPermissionGroups(String moduleUrl, Set<NiagaraPermissionGroup> permissionGroups) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
      }

      this.permissionGroupStore.grantPermissionGroups(moduleUrl, permissionGroups);
      this.refresh();
   }

   @Override
   public synchronized void refresh() {
      this.permissionsCacheMap.clear();
      this.resultCache.clear();
   }

   @Override
   public boolean implies(ProtectionDomain pd, Permission permission) {
      if (pd.equals(this.policyProtectionDomain)) {
         return true;
      }

      Pair<CodeSource, Permission> pair = new Pair<>(pd.getCodeSource(), permission);
      synchronized (this.resultCache) {
         if (this.resultCache.containsKey(pair)) {
            return this.resultCache.get(pair);
         }
      }

      boolean implied = AccessController.doPrivileged(new NiagaraPolicy.ImpliesAction(pd, permission));
      synchronized (this.resultCache) {
         this.resultCache.put(pair, implied);
         return implied;
      }
   }

   private void warnOnUnsupportedCallToGetPermissions() {
      if (this.logger.isLoggable(Level.FINE)) {
         this.logger.warning("Unsupported use of getPermissions()");
         StringJoiner joiner = new StringJoiner(System.lineSeparator() + "\t");
         joiner.add("Stack Trace:");
         LinkedList<StackTraceElement> elements = new LinkedList<>(Arrays.asList(Thread.currentThread().getStackTrace()));
         elements.removeFirst();
         elements.removeFirst();

         for (StackTraceElement element : elements) {
            joiner.add(element.toString());
         }

         this.logger.fine(joiner.toString());
      }
   }

   @Override
   public PermissionCollection getPermissions(ProtectionDomain domain) {
      this.warnOnUnsupportedCallToGetPermissions();
      return new Permissions();
   }

   @Override
   public PermissionCollection getPermissions(CodeSource codesource) {
      this.warnOnUnsupportedCallToGetPermissions();
      return new Permissions();
   }

   @Override
   public String getType() {
      return super.getType();
   }

   private void readPermissionXml() {
      if (!this.niagaraPolicy.isPresent()) {
         Path jarPath;
         if (this.isDaemon) {
            jarPath = Paths.get(AccessController.doPrivileged(() -> System.getProperty("niagara.home")), "bin", "ext", "niagarad.jar");
         } else {
            jarPath = new File(NiagaraFiles.getModulesPath(), "baja.jar").toPath();
         }

         if (Files.exists(jarPath)) {
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
               Map<String, Map<NiagaraPolicy.PolicyType, Set<Permission>>> policy = new HashMap<>();
               CoreCryptoManager mgr = CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
               JarEntry je = jarFile.getJarEntry("rc/niagara-policy.xml");
               if (je != null) {
                  XElem root = parseReadingFully(jarFile, je);
                  mgr.validateCertChain(je, true);
                  Map<String, Map<NiagaraPolicy.PolicyType, Set<Permission>>> policyPermissions = JavaPermissionsFactory.parseAll(root);
                  policyPermissions.forEach(
                     (k, v) -> policy.computeIfAbsent(k, ik -> new EnumMap<>(NiagaraPolicy.PolicyType.class))
                        .putAll((Map<? extends NiagaraPolicy.PolicyType, ? extends Set<Permission>>)v)
                  );
               }

               je = jarFile.getJarEntry("META-INF/module.xml");
               if (je != null) {
                  XElem root = parseReadingFully(jarFile, je);
                  mgr.validateCertChain(je, true);
                  XElem permissionsBlock = root.elem("permissions");
                  if (permissionsBlock != null) {
                     String codeSourceUrl = NiagaraPolicyUtil.canonicalizeCodeSource(jarPath);
                     ICodeSourceInfo codeSource = new DefaultCodeSource(codeSourceUrl, codeSourceUrl, je.getCodeSigners() != null);
                     Map<NiagaraPolicy.PolicyType, Set<Permission>> map = JavaPermissionsFactory.parseAll(codeSource, permissionsBlock);
                     policy.computeIfAbsent(codeSourceUrl, k -> new EnumMap<>(NiagaraPolicy.PolicyType.class)).putAll(map);
                  }
               }

               this.niagaraPolicy = Optional.of(policy);
            } catch (Exception e) {
               if (this.isDaemon) {
                  System.err
                     .println("SEVERE [" + new Date() + "][security.niagaraPolicy] Failed to load the niagarad niagara policy file, can not start (" + e + ")");
                  e.printStackTrace();
               } else {
                  this.logger.log(Level.SEVERE, "Failed to load the baja module niagara policy file, can not start", e);
               }

               System.exit(-6);
            }
         }
      }
   }

   private static XElem parseReadingFully(JarFile file, JarEntry entry) throws Exception {
      XElem root;
      try (InputStream is = file.getInputStream(entry)) {
         root = XParser.make(is).parse(false);

         while (is.read() != -1) {
         }
      }

      return root;
   }

   private void updateFromXmlPolicy(NiagaraPolicy.PolicyType type) {
      this.niagaraPolicy.ifPresent(map -> map.entrySet().stream().filter(entry -> entry.getValue().containsKey(type)).forEach(entry -> {
         String url = entry.getKey();
         Set<Permission> permissions = entry.getValue().get(type);
         if (WildcardPermissionMapper.isWildcardedUrl(url)) {
            this.permissionMappers.add(new WildcardPermissionMapper(url, permissions));
         } else {
            this.permissionMappers.add(new NiagaraPolicy.CachedPermissionMapper(url, permissions));
         }
      }));
   }

   private PermissionsCache getPermissionsCache(ProtectionDomain pd) throws IOException {
      String url = NiagaraPolicyUtil.canonicalizeCodeSource(pd.getCodeSource());
      Set<Permission> permissions = this.permissionMappers
         .stream()
         .filter(s -> s.appliesTo(url))
         .map(s -> s.get(url))
         .collect(HashSet::new, Set::addAll, Set::addAll);
      PermissionsCache pc = new PermissionsCache();
      permissions.forEach(pc::add);
      PermissionCollection staticPerms = pd.getPermissions();
      if (staticPerms != null) {
         Enumeration<Permission> enumeration = staticPerms.elements();

         while (enumeration.hasMoreElements()) {
            pc.add(enumeration.nextElement());
         }
      }

      return pc;
   }

   private static class CachedPermissionMapper implements PermissionMapper {
      private final String url;
      private final Set<Permission> permissions;

      public CachedPermissionMapper(String url, Set<Permission> permissions) {
         this.url = url;
         this.permissions = new HashSet<>(permissions);
      }

      @Override
      public boolean appliesTo(String otherUrl) {
         return this.url.equals(otherUrl);
      }

      @Override
      public Set<Permission> get(String url) {
         return this.url.equals(url) ? this.permissions : Collections.emptySet();
      }

      @Override
      public String toString() {
         return this.url;
      }
   }

   private static final class EmptyPermissionsCollection extends PermissionCollection {
      private EmptyPermissionsCollection() {
      }

      @Override
      public void add(Permission permission) {
      }

      @Override
      public boolean implies(Permission permission) {
         return false;
      }

      @Override
      public Enumeration<Permission> elements() {
         return Collections.emptyEnumeration();
      }
   }

   private class ImpliesAction implements PrivilegedAction<Boolean> {
      private final ProtectionDomain pd;
      private final Permission permission;

      public ImpliesAction(ProtectionDomain pd, Permission permission) {
         this.pd = pd;
         this.permission = permission;
      }

      public Boolean run() {
         if (!NiagaraPolicy.this.permissionsCacheMap.containsKey(this.pd.getCodeSource())) {
            try {
               NiagaraPolicy.this.permissionsCacheMap.put(this.pd.getCodeSource(), NiagaraPolicy.this.getPermissionsCache(this.pd));
            } catch (IOException e) {
               if (NiagaraPolicy.this.logger.isLoggable(Level.FINE)) {
                  NiagaraPolicy.this.logger.fine("Could not find code source " + this.pd.getCodeSource() + ", caused by " + e.getLocalizedMessage());
               }

               if (NiagaraPolicy.this.logger.isLoggable(Level.FINER)) {
                  NiagaraPolicy.this.logger.log(Level.FINER, "Caused by", e);
               }

               return false;
            }
         }

         String url = this.pd.getCodeSource().getLocation().toString();
         PermissionCollection pc = NiagaraPolicy.this.permissionsCacheMap.get(this.pd.getCodeSource());
         if (pc.implies(this.permission)) {
            if (NiagaraPolicy.this.logger.isLoggable(Level.FINEST)) {
               NiagaraPolicy.this.logger.finest("Access granted for " + url + ":<" + this.permission + "> by NiagaraPolicy");
            }

            return true;
         } else {
            NiagaraPolicy.this.logger.log(Level.FINE, () -> {
               StringJoiner logMessage = new StringJoiner(System.lineSeparator());
               logMessage.add("Access denied");
               logMessage.add("\tPermission that failed: " + this.permission);
               logMessage.add("\tProtectionDomain that failed: " + url);
               LinkedList<StackTraceElement> elements = new LinkedList<>(Arrays.asList(Thread.currentThread().getStackTrace()));
               elements.removeFirst();
               elements.removeFirst();
               elements.removeFirst();
               Iterator<StackTraceElement> iterator = elements.iterator();

               while (iterator.hasNext()) {
                  StackTraceElement e = iterator.next();
                  String className = e.getClassName();
                  if (className.startsWith("java.security") || className.startsWith("com.tridium.nre.security.policy")) {
                     iterator.remove();
                  }
               }

               if (!elements.isEmpty()) {
                  logMessage.add("\tStack trace causing the failure:");

                  for (StackTraceElement e : elements) {
                     logMessage.add("\t\t" + e.toString());
                  }
               }

               return logMessage.toString();
            });
            NiagaraPolicy.this.logger.log(Level.FINER, () -> {
               if (pc == null) {
                  return "\tDomain has no granted permissions";
               }

               StringJoiner logMessage = new StringJoiner(System.lineSeparator());
               logMessage.add("\tPermissions granted to that domain:");
               Enumeration<Permission> e = pc.elements();

               while (e.hasMoreElements()) {
                  Permission p = e.nextElement();
                  logMessage.add("\t\t" + p);
               }

               logMessage.add("\tSuppliers that should have provided permissions:");
               List<PermissionMapper> suppliers = NiagaraPolicy.this.permissionMappers.stream().filter(sx -> sx.appliesTo(url)).collect(Collectors.toList());

               for (PermissionMapper s : suppliers) {
                  logMessage.add("\t\t" + s);
               }

               logMessage.add("\tPermissions that should have been supplied:");

               for (Permission p : (Set)suppliers.stream().map(sx -> sx.get(url)).collect(HashSet::new, Set::addAll, Set::addAll)) {
                  logMessage.add("\t\t" + p);
               }

               return logMessage.toString();
            });
            return false;
         }
      }
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }

   public enum PolicyType {
      ALL,
      WORKBENCH,
      STATION,
      DAEMON;
   }
}
