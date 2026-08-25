package com.tridium.nre.security;

import com.tridium.hsm.provider.TridiumHsmProvider;
import java.security.AccessController;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.HsmManager;

public final class HsmManagerImpl implements HsmManager {
   private static final Logger logger = Logger.getLogger("nre.hsm");
   private final ClassLoader classLoader;
   private String hsmType = "none";
   private String hsmEngineClassName = null;
   private Class<?> hsmEngineClass = null;

   public static HsmManagerImpl make(ClassLoader classLoader) {
      return new HsmManagerImpl(classLoader);
   }

   private HsmManagerImpl(ClassLoader classLoader) {
      this.classLoader = classLoader;
      AccessController.doPrivileged(() -> {
         try {
            this.hsmType = System.getProperty("niagara.hsm.type", "none");
            this.hsmEngineClassName = System.getProperty("niagara.hsm.engine");
            if (!Objects.isNull(this.hsmEngineClassName)) {
               this.hsmEngineClass = Class.forName(this.hsmEngineClassName, true, this.classLoader);
               Security.setProperty("niagara.hsm.engine", this.hsmEngineClassName);
            } else {
               logger.fine("hsm engine not specified");
            }
         } catch (Throwable e) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.WARNING, "error processing hsm engine", e);
            } else {
               logger.log(Level.WARNING, "error processing hsm engine: " + e.getLocalizedMessage());
            }
         }

         return null;
      });
   }

   @Override
   public String getHsmType() {
      return this.hsmType;
   }

   @Override
   public boolean hasHsmEngine() {
      return this.hsmEngineClass != null;
   }

   @Override
   public String getHsmEngineClassName() {
      return this.hsmEngineClassName;
   }

   public void registerProvider() {
      if (this.hsmEngineClass != null) {
         try {
            Security.addProvider((Provider)Class.forName("com.tridium.hsm.provider.TridiumHsmProvider").getDeclaredConstructor().newInstance());
         } catch (Throwable e) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.WARNING, "error creating hsm provider", e);
            } else {
               logger.log(Level.WARNING, "error creating hsm provider: " + e.getLocalizedMessage());
            }
         }
      }
   }

   @Override
   public Map<String, String> getProperties() {
      return this.hsmEngineClass == null ? null : Collections.unmodifiableMap(TridiumHsmProvider.getHsmEngine().getProperties());
   }
}
