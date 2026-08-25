package com.tridium.nre.bootstrap;

import com.tridium.nre.di.NreInstantiator;
import com.tridium.nre.di.SingletonSupplier;
import com.tridium.nre.di.TypeSupplier;
import com.tridium.nre.security.DefaultSecurityInitializerConfig;
import com.tridium.nre.security.ISecurityInitializer;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.policy.NiagaraPolicy;
import com.tridium.nre.syslog.SyslogManager;
import com.tridium.nre.util.NiagaraFiles;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.Policy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public final class Bootstrap {
   private static BootstrapClassLoader loader;
   private static final NreInstantiator instantiator = new NreInstantiator();
   private static boolean nonFipsOverride = false;
   private static boolean isStation;
   public static final long bootTime = System.currentTimeMillis();
   public static final String bootstrapLogFormat = "%4$s [%1$tH:%1$tM:%1$tS %1$td-%1$tb-%1$ty %1$tZ][%3$s] %5$s%6$s%n";

   private Bootstrap() {
   }

   public static boolean overrideFipsModeOnStart(String userHome) {
      try (Scanner scanner = new Scanner(
            new File(userHome + File.separator + "etc" + File.separator + "options" + File.separator + "bajaui-FipsOptions.options")
         )) {
         while (scanner.hasNextLine()) {
            String next = scanner.nextLine();
            if (next.contains("<p n=\"startWorkbenchInFipsMode\" v=\"false\"/>")) {
               return true;
            }
         }
      } catch (FileNotFoundException var17) {
      }

      return false;
   }

   public static void Main(String[] args) throws Exception {
      System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s [%1$tH:%1$tM:%1$tS %1$td-%1$tb-%1$ty %1$tZ][%3$s] %5$s%6$s%n");
      NiagaraPolicy niagaraPolicy = new NiagaraPolicy();
      Policy.setPolicy(niagaraPolicy);
      String niagaraUserHome = AccessController.doPrivileged(() -> System.getProperty("niagara.user.home"));
      boolean overrideFipsMode = overrideFipsModeOnStart(niagaraUserHome);
      nonFipsOverride = (hasFlag("-fips=false", args) || overrideFipsMode) && !hasFlag("-fips=true", args);
      isStation = args.length > 0 && args[0].equals("com.tridium.sys.station.Station");
      ISecurityInitializer initializer = instantiator.instance(ISecurityInitializer.class);
      SyslogManager syslogManager = SyslogManager.getInstance();
      if (syslogManager.isSyslogLicensed()) {
         if (isStation) {
            syslogManager.setEnvironmentTag("station_" + args[1]);
         } else if (args.length > 0 && args[0].contains("WbMain")) {
            syslogManager.setEnvironmentTag("workbench");
         } else {
            syslogManager.setEnvironmentTag("niagara");
         }
      }

      if (syslogManager.getEnabled()) {
         syslogManager.start();
      }

      Logger.getLogger("nre").info("Booting");
      List<URL> urls = new ArrayList<>();
      urls.add(new File(NiagaraFiles.getModulesPath(), "baja.jar").toURI().toURL());
      loader = new BootstrapClassLoader(urls.toArray(new URL[0]), initializer.getSecurityInfoProvider());
      niagaraPolicy.bootstrap();
      Class<?> bootstrapClass = loader.loadClass("com.tridium.sys.Nre");
      Method bootstrap = bootstrapClass.getMethod("bootstrap", String[].class);
      bootstrap.invoke(null, args);
   }

   public static BootstrapClassLoader getBootstrapClassLoader() {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         NiagaraBasicPermission classLoaderPermission = new NiagaraBasicPermission("GET_BOOTSTRAP_CLASS_LOADER");
         sm.checkPermission(classLoaderPermission);
      }

      return loader;
   }

   public static NreInstantiator getInstantiator() {
      return instantiator;
   }

   public static boolean nonFipsOverride() {
      return nonFipsOverride;
   }

   public static boolean isStation() {
      return isStation;
   }

   private static boolean hasFlag(String flag, String[] args) {
      return Arrays.asList(args).contains(flag);
   }

   static {
      TypeSupplier<ISecurityInitializer> secIntSupplier = new SingletonSupplier<>(
         ISecurityInitializer.class, SecurityInitializer.class, new DefaultSecurityInitializerConfig()
      );
      instantiator.addSupplier(secIntSupplier);
   }
}
