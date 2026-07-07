package com.tridium.template.file;

import com.tridium.install.BDependency;
import com.tridium.install.BVersion;
import com.tridium.install.part.BModulePart;
import com.tridium.neql.component.ComponentTreeIterator;
import com.tridium.sys.Nre;
import com.tridium.sys.module.NModule;
import com.tridium.util.ObjectUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.baja.file.types.text.BPxFile;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.platform.install.BVersionRelation;
import javax.baja.registry.ModuleInfo;
import javax.baja.registry.Registry;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.tag.Entity;
import javax.baja.util.Version;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class DependencyUtil {
   public static Hashtable<String, BDependency> getTemplatePxDependencies(
      PxFileRef[] pxFileRefs, Hashtable<String, BDependency> pxDependencies, boolean useMinor
   ) {
      for (PxFileRef pxFileRef : pxFileRefs) {
         getPxFileDependencies(pxFileRef.getPxFile(), pxDependencies, useMinor);
      }

      return pxDependencies;
   }

   public static void getPxFileDependencies(BPxFile pxFile, Hashtable<String, BDependency> pxDependencies, boolean useMinor) {
      HashSet<String> modules = new HashSet<>();
      Registry registry = Sys.getRegistry();

      try (InputStream in = pxFile.getInputStream()) {
         XParser xParser = XParser.make(in);
         XElem root = xParser.parse();
         XElem elem = root.elem("import");
         if (elem != null) {
            XElem[] moduleElems = elem.elems("module");

            for (XElem moduleElem : moduleElems) {
               modules.add(moduleElem.get("name"));
            }
         }

         xParser.close();

         try (InputStream inx = pxFile.getInputStream()) {
            for (String line = readLine(inx); line != null; line = readLine(inx)) {
               int mIndex = line.indexOf("module://") + 9;
               if (mIndex > 9) {
                  int mEnd = line.indexOf(47, mIndex);
                  String moduleName = line.substring(mIndex, mEnd);
                  modules.add(moduleName);
               }
            }
         }

         for (String module : modules) {
            try {
               for (ModuleInfo moduleInfo : registry.getModules(module)) {
                  if (moduleInfo.getRuntimeProfile() != RuntimeProfile.doc && moduleInfo.getRuntimeProfile() != RuntimeProfile.se) {
                     String modulePartName = moduleInfo.getModulePartName();
                     if (!pxDependencies.containsKey(modulePartName)) {
                        String vendor = moduleInfo.getVendor();
                        Version vendorVersion = moduleInfo.getVendorVersion();
                        String dependencyVersion = useMinor ? vendorVersion.toMinorVersion().toString() : vendorVersion.toString();
                        BVersion v = new BVersion(vendor, dependencyVersion);
                        BDependency dependency = BDependency.forModule(modulePartName);
                        dependency.setVersion(v);
                        if (NtplUtil.log.isLoggable(Level.FINE)) {
                           NtplUtil.log
                              .log(Level.FINE, "Adding module dependency '" + modulePartName + "' -> '" + dependency + "' for PX file '" + pxFile + "'");
                        }

                        pxDependencies.put(modulePartName, dependency);
                     }
                  } else if (NtplUtil.log.isLoggable(Level.FINE)) {
                     NtplUtil.log.log(Level.FINE, "Skipping doc/se module dependency '" + moduleInfo + "' for PX file '" + pxFile + "'");
                  }
               }
            } catch (Exception var40) {
               NtplUtil.log.log(Level.WARNING, "Missing module '" + module + "' for PX file '" + pxFile + "', cannot add dependency", (Throwable)var40);
            }
         }
      } catch (Exception var45) {
         NtplUtil.log.log(Level.WARNING, "Error resolving PX file dependencies for PX file '" + pxFile + "'", (Throwable)var45);
      }
   }

   private static String readLine(InputStream in) {
      StringBuilder sb = new StringBuilder();

      try {
         int value;
         for (value = in.read(); value >= 0 && value != 13 && value != 10; value = in.read()) {
            sb.append((char)(value & 0xFF));
         }

         return sb.length() == 0 && value < 0 ? null : sb.toString();
      } catch (IOException var3) {
         NtplUtil.log.log(Level.WARNING, "Error reading input stream:" + var3.getLocalizedMessage(), (Throwable)var3);
         return sb.toString();
      }
   }

   public static void getBogComponentDependencies(BComponent templateRoot, Hashtable<String, BDependency> dependencies, boolean useMinor) {
      Set<NModule> moduleSet = new HashSet<>();
      NModule niagaraModule = Nre.getModuleManager().getModuleForClass(templateRoot.getClass());
      moduleSet.add(niagaraModule);
      getFacetModuleDependenciesForComponent(templateRoot, dependencies, useMinor);
      ComponentTreeIterator iterator = new ComponentTreeIterator(templateRoot);

      while (iterator.hasNext()) {
         Entity entity = iterator.next();
         if (entity instanceof BComponent) {
            BComponent templateComponent = (BComponent)entity;
            niagaraModule = Nre.getModuleManager().getModuleForClass(templateComponent.getClass());
            moduleSet.add(niagaraModule);
            getFacetModuleDependenciesForComponent(templateComponent, dependencies, useMinor);
         }
      }

      moduleSet.forEach(
         module -> {
            Version version = module.getVendorVersion();
            String dependencyVersion = useMinor ? version.toMinorVersion().toString() : version.toString();
            dependencies.put(
               module.getModulePartName(),
               new BDependency(
                  module.getModulePartName(),
                  new BVersion(module.getVendor(), dependencyVersion),
                  BVersionRelation.minimum,
                  BModulePart.TYPE.getTypeSpec(),
                  "*"
               )
            );
         }
      );
   }

   private static void getFacetModuleDependenciesForComponent(BComponent component, Hashtable<String, BDependency> dependencies, boolean useMinor) {
      Property myProperty = component.getPropertyInParent();
      if (myProperty != null) {
         BFacets facetsValue = component.getParent().getSlotFacets(myProperty);
         ObjectUtil.getModuleDependenciesFromFacets(facetsValue).forEach(new DependencyUtil.ModuleAddConsumer(dependencies, useMinor));
      }

      SlotCursor<Property> propertySlotCursor = component.getProperties();

      while (propertySlotCursor.next(BFacets.class)) {
         BFacets facetsValue = (BFacets)propertySlotCursor.get();
         ObjectUtil.getModuleDependenciesFromFacets(facetsValue).forEach(new DependencyUtil.ModuleAddConsumer(dependencies, useMinor));
      }
   }

   private static class ModuleAddConsumer implements Consumer<String> {
      private final Hashtable<String, BDependency> dependencies;
      private final boolean useMinor;

      ModuleAddConsumer(Hashtable<String, BDependency> dependencies, boolean useMinor) {
         this.dependencies = dependencies;
         this.useMinor = useMinor;
      }

      public void accept(String moduleOrPartName) {
         String moduleName = moduleOrPartName;
         RuntimeProfile profile = null;
         int dashIndex = moduleOrPartName.lastIndexOf(45);
         if (dashIndex > 0) {
            profile = RuntimeProfile.valueOf(moduleOrPartName.substring(dashIndex + 1), null);
            if (profile != null) {
               moduleName = moduleOrPartName.substring(0, dashIndex);
            }
         }

         ModuleInfo[] moduleInfos;
         if (profile == null) {
            moduleInfos = Sys.getRegistry().getModules(moduleName);
         } else {
            moduleInfos = new ModuleInfo[1];
            moduleInfos[0] = Sys.getRegistry().getModule(moduleName, profile);
         }

         if (moduleInfos != null) {
            for (ModuleInfo moduleInfo : moduleInfos) {
               String partName = moduleInfo.getModulePartName();
               if (!this.dependencies.containsKey(partName)) {
                  Version vendorVersion = this.useMinor ? moduleInfo.getVendorVersion().toMinorVersion() : moduleInfo.getVendorVersion();
                  BVersion v = new BVersion(moduleInfo.getVendor(), vendorVersion.toString());
                  BDependency dependency = new BDependency(partName, v, BModulePart.TYPE.getTypeSpec());
                  if (NtplUtil.log.isLoggable(Level.FINE)) {
                     NtplUtil.log.log(Level.FINE, "Adding module dependency '" + partName + "' -> '" + dependency + "' in ModuleAddConsumer");
                  }

                  this.dependencies.put(partName, dependency);
               }
            }
         }
      }
   }
}
