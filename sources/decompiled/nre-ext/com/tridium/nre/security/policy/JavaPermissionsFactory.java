package com.tridium.nre.security.policy;

import com.tridium.nre.bootstrap.Bootstrap;
import com.tridium.nre.util.TextExpander;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.security.AccessController;
import java.security.Permission;
import java.security.UnresolvedPermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.xml.XElem;

public final class JavaPermissionsFactory extends XmlPolicyParser<Permission> {
   private static final JavaPermissionsFactory INSTANCE = new JavaPermissionsFactory();
   private static final Set<String> ignoredMissingKeys = Collections.unmodifiableSet(
      new HashSet<>(
         Arrays.asList(
            "protected.station.home",
            "niagara.jxbrowser.version",
            "niagara.jxbrowser.chromium.version",
            "niagara.alternative.database.path",
            "niagara.alternative.archive.path",
            "niagara.alternative.archive.zip.path",
            "niagara.platDataRecovery.mountPath",
            "niagara.platDataRecovery.geomPath",
            "niagara.platDataRecovery.chunkfsStatsPath",
            "niagara.platDataRecovery.chunkfsSizesPath",
            "niagara.platDataRecovery.activeDirectoryPath",
            "niagara.platDataRecovery.persistentDirectoryPath",
            "niagara.platNtp.configurationFilePath",
            "niagara.platNtp.driftFilePath",
            "niagara.platNtp.statisticsDirectoryPath"
         )
      )
   );
   private static final Logger log = Logger.getLogger("security.niagaraPolicy");
   private static final TextExpander expander = new TextExpander(JavaPermissionsFactory::mapper);
   private static final TextExpander fileExpander = new TextExpander(JavaPermissionsFactory::fileMapper);
   private static final String JAVA_EXT_DIRS = "java.ext.dirs";
   private static final String JAVA_EXT_DIRS_VAR = "${{java.ext.dirs}}";
   private static final String FILE_PERMISSION_NAME = "java.io.FilePermission";
   private static final String LOADLIBRARY_PERMISSION_NAME = "com.tridium.nre.security.NiagaraLoadLibraryPermission";
   public static final String POLICY_ELEM = "policy";
   public static final String CODESOURCE_ATTR = "codesource";
   public static final String JAVA_PERMISSIONS_ELEM = "java-permissions";
   public static final String JAVA_PERMISSION_ELEM = "java-permission";
   public static final String CLASS_ATTR = "class";
   public static final String NAME_ATTR = "name";
   public static final String ACTION_ATTR = "action";

   private JavaPermissionsFactory() {
   }

   public static Map<String, Map<NiagaraPolicy.PolicyType, Set<Permission>>> parseAll(XElem policyBlock) throws ParsingException {
      Map<String, Map<NiagaraPolicy.PolicyType, Set<Permission>>> map = new HashMap<>();
      if (!"policy".equals(policyBlock.name())) {
         throw new ParsingException(String.format("Unexpected XML block %s", policyBlock.name()));
      }

      for (XElem permissions : policyBlock.elems("permissions")) {
         String codeSource = permissions.get("codesource");
         if (codeSource == null) {
            log.warning(String.format("Found permissions block without %1$s attribute; skipping", "codesource"));
         } else if ("*".equals(codeSource)) {
            Map<NiagaraPolicy.PolicyType, Set<Permission>> allPermission = parseAll(DefaultCodeSource.ALL_CODE_SOURCE, permissions);
            map.put(codeSource, allPermission);
         } else {
            List<URL> codeSources = new ArrayList<>();
            int extDirIndex = codeSource.indexOf("${{java.ext.dirs}}");
            if (extDirIndex > 0) {
               String prefix = codeSource.substring(0, extDirIndex);
               String suffix = codeSource.substring(extDirIndex + "${{java.ext.dirs}}".length());
               String[] extDirs = AccessController.doPrivileged(() -> System.getProperty("java.ext.dirs")).split(File.pathSeparator);

               for (String extDir : extDirs) {
                  try {
                     String url = NiagaraPolicyUtil.canonicalizeCodeSource(prefix + extDir + suffix);
                     codeSources.add(new URL(url));
                  } catch (NoSuchFileException var19) {
                  } catch (IOException e) {
                     log.log(Level.WARNING, String.format("Could not parse %s%s%s as a URL", prefix, extDir, suffix));
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Caused by: ", e);
                     }
                  }
               }
            } else {
               try {
                  String url = expander.expand(codeSource).replace(File.separatorChar, '/');
                  codeSources.add(new URL(url));
               } catch (IllegalArgumentException | MalformedURLException e) {
                  log.log(Level.WARNING, String.format("Could not parse %s as a URL", codeSource));
                  if (log.isLoggable(Level.FINE)) {
                     log.log(Level.FINE, "Caused by: ", e);
                  }
                  continue;
               }
            }

            for (URL codeSourceUrl : codeSources) {
               String url;
               try {
                  url = NiagaraPolicyUtil.canonicalizeCodeSource(codeSourceUrl);
               } catch (IOException e) {
                  if (log.isLoggable(Level.FINE)) {
                     log.log(Level.FINE, "Could not expand path " + codeSourceUrl, e);
                  }

                  url = codeSourceUrl.toString();
               }

               ICodeSourceInfo codeSourceInfo = new DefaultCodeSource(url, url, false);
               map.put(url, parseAll(codeSourceInfo, permissions));
            }
         }
      }

      return map;
   }

   public static Map<NiagaraPolicy.PolicyType, Set<Permission>> parseAll(ICodeSourceInfo codeSource, XElem permissionsBlock) throws ParsingException {
      return INSTANCE.doParseAll(codeSource, permissionsBlock);
   }

   public static Set<Permission> parse(ICodeSourceInfo codeSource, XElem permissionsBlock, NiagaraPolicy.PolicyType type) throws ParsingException {
      return INSTANCE.doParse(codeSource, permissionsBlock, type);
   }

   @Override
   protected Set<Permission> doParseElement(ICodeSourceInfo codeSource, XElem element) {
      HashSet<Permission> permissions = new HashSet<>();
      int errorCount = 0;

      for (XElem javaPermission : element.elems("java-permission")) {
         String className = javaPermission.get("class");
         String name = javaPermission.get("name", null);
         if (name != null && name.contains("${") && name.contains("}")) {
            try {
               if (!className.equalsIgnoreCase("java.io.FilePermission")
                  && !className.equalsIgnoreCase("com.tridium.nre.security.NiagaraLoadLibraryPermission")) {
                  name = expander.expand(name);
               } else {
                  name = fileExpander.expand(name);
               }
            } catch (JavaPermissionsFactory.IgnoredNoSuchElementException e) {
               continue;
            } catch (NoSuchElementException | IllegalArgumentException e) {
               errorCount++;
               if (log.isLoggable(Level.FINE)) {
                  log.log(
                     Level.FINE,
                     String.format("%s: %s: %s could not be expanded (does the referenced property exist?)", codeSource.getUrl(), className, name),
                     e
                  );
               }
               continue;
            }
         }

         String action = javaPermission.get("action", null);

         try {
            Permission perm = resolvePermission(codeSource.getUrl(), className, name, action);
            if (perm != null && !permissions.add(perm) && log.isLoggable(Level.FINE)) {
               log.fine(codeSource.getUrl() + ": Duplicate permission " + perm.toString());
            }
         } catch (Exception e) {
            errorCount++;
            if (log.isLoggable(Level.FINE)) {
               log.log(Level.FINE, String.format("%s: %s: %s Could not instantiate permission.", codeSource.getUrl(), className, name), e);
            }
         }
      }

      if (errorCount > 0) {
         log.log(Level.WARNING, String.format("%s: Unable to parse %d policy entries", codeSource.getUrl(), errorCount));
      }

      return permissions;
   }

   @Override
   protected String getPermissionsGroupName() {
      return "java-permissions";
   }

   private static Permission resolvePermission(String moduleName, String className, String name, String action) throws ParsingException {
      Class<?> cls;
      try {
         cls = Class.forName(className);
      } catch (ClassNotFoundException e) {
         try {
            cls = Class.forName(className, true, Bootstrap.getBootstrapClassLoader());
         } catch (ClassNotFoundException e1) {
            if (log.isLoggable(Level.FINE)) {
               log.fine(moduleName + ": Permission " + className + " not found; marking as unresolved");
            }

            return new UnresolvedPermission(className, name, action, null);
         }
      }

      if (!Permission.class.isAssignableFrom(cls)) {
         throw new ParsingException(String.format("%s: Class %s is not a permission", moduleName, className));
      }

      if (name == null && action == null) {
         try {
            Constructor<?> constructor = cls.getConstructor();
            return (Permission)constructor.newInstance();
         } catch (InvocationTargetException e) {
            throw new ParsingException("Could not create permission " + className, e);
         } catch (Exception var14) {
         }
      }

      try {
         Constructor<?> constructor = cls.getConstructor(String.class, String.class);
         return (Permission)constructor.newInstance(name, action);
      } catch (InvocationTargetException e) {
         throw new ParsingException("Could not create permission " + className, e);
      } catch (Exception e) {
         try {
            Constructor<?> constructor = cls.getConstructor(String.class);
            return (Permission)constructor.newInstance(name);
         } catch (Exception e1) {
            throw new ParsingException("Could not create permission " + className, e1);
         }
      }
   }

   private static String mapper(String key) {
      if ("/".equals(key)) {
         return File.separator;
      }

      String propertyValue = AccessController.doPrivileged(() -> System.getProperty(key));
      return Optional.ofNullable(propertyValue)
         .orElseThrow(
            () -> ignoredMissingKeys.contains(key)
               ? new JavaPermissionsFactory.IgnoredNoSuchElementException()
               : new NoSuchElementException(key + " not found")
         );
   }

   private static String fileMapper(String key) {
      if ("/".equals(key)) {
         return File.separator;
      }

      String propertyValue = AccessController.doPrivileged(() -> System.getProperty(key));
      if (propertyValue != null && propertyValue.endsWith(File.separator)) {
         propertyValue = propertyValue.substring(0, propertyValue.length() - 1);
      }

      return Optional.ofNullable(propertyValue)
         .orElseThrow(
            () -> ignoredMissingKeys.contains(key)
               ? new JavaPermissionsFactory.IgnoredNoSuchElementException()
               : new NoSuchElementException(key + " not found")
         );
   }

   private static class IgnoredNoSuchElementException extends NoSuchElementException {
      private IgnoredNoSuchElementException() {
      }
   }
}
