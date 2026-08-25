package com.tridium.nre.security.policy;

import com.tridium.nre.security.NiagaraBasicPermission;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Policy;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class NiagaraPolicyUtil {
   private static final Pattern WINDOWS_DRIVE_LETTERS = Pattern.compile("^/[a-zA-Z]:[\\\\/].*");

   private NiagaraPolicyUtil() {
   }

   public static void init(AbstractPermissionGroupStore permissionGroupStore, NiagaraPolicy.PolicyType type) {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null && !(Policy.getPolicy() instanceof NiagaraPolicy)) {
         Policy.setPolicy(new NiagaraPolicy());
      }

      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
      }

      AccessController.doPrivileged(() -> {
         NiagaraPolicy niagaraPolicy = (NiagaraPolicy)Policy.getPolicy();
         niagaraPolicy.init(permissionGroupStore, type);
         return null;
      });
   }

   public static NiagaraPolicy.PolicyType getPolicyType() {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("VIEW_NIAGARA_POLICY"));
      }

      return AccessController.doPrivileged(() -> {
         NiagaraPolicy niagaraPolicy = (NiagaraPolicy)Policy.getPolicy();
         return niagaraPolicy.getPolicyType();
      });
   }

   public static void grantPermissionGroup(String moduleUrl, NiagaraPermissionGroup permissionGroup) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
      }

      AccessController.doPrivileged(() -> {
         NiagaraPolicy niagaraPolicy = (NiagaraPolicy)Policy.getPolicy();
         niagaraPolicy.grantPermissionGroup(moduleUrl, permissionGroup);
         return null;
      });
   }

   public static void revokePermissionGroup(String moduleUrl, NiagaraPermissionGroup permissionGroup) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
      }

      AccessController.doPrivileged(() -> {
         NiagaraPolicy niagaraPolicy = (NiagaraPolicy)Policy.getPolicy();
         niagaraPolicy.revokePermissionGroup(moduleUrl, permissionGroup);
         return null;
      });
   }

   public static Set<NiagaraPermissionGroup> getAllPermissionGroups(String moduleUrl) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("VIEW_NIAGARA_POLICY"));
      }

      return AccessController.doPrivileged(() -> {
         NiagaraPolicy niagaraPolicy = (NiagaraPolicy)Policy.getPolicy();
         return niagaraPolicy.getAllPermissionGroups(moduleUrl).stream().map(NiagaraPermissionGroup::copy).collect(Collectors.toSet());
      });
   }

   public static void requestPermissions(String moduleUrl, Set<NiagaraPermissionGroup> permissionGroups) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(new NiagaraBasicPermission("MANAGE_NIAGARA_POLICY"));
      }

      AccessController.doPrivileged(() -> {
         NiagaraPolicy policy = (NiagaraPolicy)Policy.getPolicy();
         policy.requestPermissionGroups(moduleUrl, permissionGroups);
         return null;
      });
   }

   public static String canonicalizeCodeSource(String srcPath) throws IOException {
      if (srcPath.isEmpty()) {
         return srcPath;
      }

      String prefix = "file:";
      if (srcPath.startsWith(prefix)) {
         srcPath = srcPath.substring(prefix.length());
      }

      if (File.separatorChar == '\\' && srcPath.charAt(0) == '/' && WINDOWS_DRIVE_LETTERS.matcher(srcPath).matches()) {
         srcPath = srcPath.substring(1);
      }

      char c = srcPath.charAt(srcPath.length() - 1);
      if (c == '-' || c == '*') {
         srcPath = srcPath.substring(0, srcPath.length() - 1);
      }

      Path p = Paths.get(srcPath).toRealPath();
      String path = p.toString().replace(File.separatorChar, '/');
      if (Files.isDirectory(p) && !path.endsWith("/")) {
         path = path + '/';
      }

      if (!path.startsWith("/")) {
         path = '/' + path;
      }

      if (c == '-' || c == '*') {
         path = path + c;
      }

      return prefix + path;
   }

   public static String canonicalizeCodeSource(Path path) throws IOException {
      return URLDecoder.decode(path.toRealPath().toUri().toURL().toString(), "UTF-8");
   }

   public static String canonicalizeCodeSource(CodeSource cs) throws IOException {
      return canonicalizeCodeSource(cs.getLocation());
   }

   public static String canonicalizeCodeSource(URL url) throws IOException {
      return "file".equals(url.getProtocol()) ? canonicalizeCodeSource(URLDecoder.decode(url.toString(), "UTF-8")) : url.toString();
   }

   public static String createRegexPatternFromName(String name) {
      return name.replace("\\", "\\\\")
         .replace("/", "\\/")
         .replace(".", "\\.")
         .replace("*", ".*")
         .replace("^", "\\^")
         .replace("$", "\\$")
         .replace("+", "\\+")
         .replace("|", "\\|")
         .replace("?", "\\?")
         .replace("[", "\\[")
         .replace("]", "\\]")
         .replace("(", "\\(")
         .replace(")", "\\)");
   }
}
