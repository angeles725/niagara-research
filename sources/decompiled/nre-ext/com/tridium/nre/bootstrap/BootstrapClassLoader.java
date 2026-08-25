package com.tridium.nre.bootstrap;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.nre.security.ISecurityInfoProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;

public class BootstrapClassLoader extends URLClassLoader {
   static final boolean parallelCapableClassLoader = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.classLoader.parallelCapable"));
   private ISecurityInfoProvider secInfoProvider;
   private CoreCryptoManager coreCryptoManager;

   public BootstrapClassLoader(URL[] urls, ISecurityInfoProvider secInfProvider) throws Exception {
      super(urls);
      this.secInfoProvider = secInfProvider;
      this.coreCryptoManager = CoreCryptoManager.get(this.secInfoProvider);
   }

   @Override
   public synchronized Class<?> loadClass(String name) throws ClassNotFoundException {
      ClassLoader parent = this.getParent();
      if (parent != null) {
         try {
            return parent.loadClass(name);
         } catch (ClassNotFoundException var6) {
         }
      }

      Class<?> clazz = super.loadClass(name);

      try {
         this.coreCryptoManager.validateCertChain(clazz, true);
      } catch (Exception e) {
         System.err.println("SEVERE [nre] Unable to load " + name + ": " + e.getLocalizedMessage());
         System.exit(-6);
      }

      return clazz;
   }

   static {
      if (parallelCapableClassLoader) {
         ClassLoader.registerAsParallelCapable();
      }
   }
}
