package com.tridium.template.application;

import com.tridium.install.BVersion;
import com.tridium.sys.schema.ComponentSlotMap;
import com.tridium.sys.transfer.ReplacingContext;
import com.tridium.template.BApplicationService;
import com.tridium.template.BConfigBinding;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.file.BNtplFile;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import javax.baja.agent.BPxView;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSpace;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.util.Array;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BLink;
import javax.baja.sys.BRelation;
import javax.baja.sys.BStation;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.CopyHints;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.util.BFormat;
import javax.baja.util.BUnrestrictedFolder;
import javax.baja.util.BWsAnnotation;

public class ApplicationTemplateInstaller implements AutoCloseable {
   private final BNtplFile applicationFile;
   private final NameTree componentsToBeRemoved;
   private final ProgressTracker progress;

   public ApplicationTemplateInstaller() {
      this(null, null, new ProgressTracker());
   }

   public ApplicationTemplateInstaller(BOrd templateFileOrd, BOrdList componentsToBeRemoved, ProgressTracker progress) {
      this.applicationFile = templateFileOrd == null ? null : (BNtplFile)templateFileOrd.resolve().get();
      this.componentsToBeRemoved = componentsToBeRemoved == null ? null : ApplicationTemplateUtil.makeNameTree(componentsToBeRemoved);
      this.progress = progress;
   }

   public boolean checkForCompatibleModules() {
      return this.checkStationForCompatibleModules(Sys.getStation());
   }

   private boolean checkStationForCompatibleModules(BStation target) {
      if (this.applicationFile == null) {
         throw new IllegalStateException();
      } else {
         this.progress.message("application.compatibility.starting");
         List<Map<String, BVersion>> missingDependencies = this.applicationFile.checkRemoteModuleDependencies(target);
         Map<String, BVersion> missingModules = missingDependencies.get(0);
         Map<String, BVersion> mismatchedModules = missingDependencies.get(1);
         Map<String, BVersion> missingPxModules = missingDependencies.get(2);
         boolean incompatible = missingModules.size() > 0;
         boolean compatibilityWarnings = mismatchedModules.size() > 0 || missingPxModules.size() > 0;

         for (String module : missingModules.keySet()) {
            this.progress.message("application.compatibility.missingModule", module);
         }

         for (Entry<String, BVersion> moduleSpec : mismatchedModules.entrySet()) {
            this.progress.message("application.compatibility.mismatchedModule", moduleSpec.getKey(), moduleSpec.getValue().toString());
         }

         for (String module : missingPxModules.keySet()) {
            this.progress.message("application.compatibility.missingPxModule", module);
         }

         if (incompatible) {
            this.progress.message("application.compatibility.failed");
         } else if (compatibilityWarnings) {
            this.progress.message("application.compatibility.warnings");
         } else {
            this.progress.message("application.compatibility.passed");
         }

         return !incompatible;
      }
   }

   public void install(Context cx) throws IOException {
      this.installToStation(Sys.getStation(), ApplicationTemplateUtil.describeDefaultStation(cx));
   }

   public void upgrade(Context cx) throws IOException {
      this.upgradeStation(Sys.getStation(), ApplicationTemplateUtil.describeDefaultStation(cx));
   }

   private void installToStation(BStation target, NameTree keepInTarget) throws IOException {
      if (this.applicationFile == null) {
         throw new IllegalStateException();
      } else {
         try {
            this.basicInstallToStation(target, keepInTarget, this.componentsToBeRemoved);
         } catch (Exception var4) {
            this.progress.endFailed(var4);
            throw var4;
         }

         this.progressMessage("application.install.finished");
         this.progress.message("application.install.passwordNote");
      }
   }

   private void upgradeStation(BStation target, NameTree keepInTarget) throws IOException {
      if (this.applicationFile == null) {
         throw new IllegalStateException();
      } else {
         try {
            this.progressMessage("application.upgrade.saveConfig");
            this.progress.constrain(5);
            Map<String, BValue> rootProps = getApplicationProperties(target);
            this.progress.fulfill();
            this.progress.constrain(10);
            NameTree componentsToBeRemoved = getMissingOptionalComponents(target, this.applicationFile.getTemplateManifest().optional);
            this.progress.fulfill();
            this.progress.constrain(90);
            this.basicInstallToStation(target, keepInTarget, componentsToBeRemoved);
            this.progress.fulfill();
            this.progressMessage("application.upgrade.restoreConfig");
            this.progress.constrain(100);
            setApplicationProperties(target, rootProps);
            this.progress.fulfill();
         } catch (Exception var5) {
            this.progress.endFailed(var5);
            throw var5;
         }

         this.progressMessage("application.upgrade.finished");
      }
   }

   private void basicInstallToStation(BStation target, NameTree keepInTarget, NameTree componentsToBeRemoved) throws IOException {
      this.progress.constrain(5);
      BStation source = (BStation)this.applicationFile.getBaseComponent();
      this.progress.fulfill();
      this.progressMessage("application.install.loaded");
      this.progressMessage("application.clear.starting");
      Context cx = new ReplacingContext(Context.NULL, BFacets.make("niagaraAutoStart", BBoolean.FALSE));
      this.progress.constrain(20);
      this.clearApplicationComponents(target, keepInTarget, cx);
      this.progress.fulfill();
      this.progress.constrain(30);
      String[] fileKeepers = new String[]{this.applicationFile.getFilePath().toString()};
      this.clearApplicationFiles(target, fileKeepers);
      this.progress.fulfill();
      this.progressMessage("application.clear.finished");
      this.progressMessage("application.install.starting");
      this.progress.constrain(50);
      this.installApplicationFiles(this.applicationFile, target);
      this.progress.fulfill();
      this.progress.constrain(100);
      this.installApplicationComponents(source, componentsToBeRemoved, target, keepInTarget, cx);
      this.progress.fulfill();
   }

   public void installApplicationComponents(BStation source, BStation target, NameTree keepInTarget, Context cx) {
      this.installApplicationComponents(source, null, target, keepInTarget, cx);
   }

   public void installApplicationComponents(BStation source, NameTree dropFromSource, BStation target, NameTree keepInTarget, Context cx) {
      this.progressMessage("application.deploy.building");
      this.progress.constrain(15);
      CopyHints hints = new CopyHints();
      hints.defaultOnClone = false;
      hints.swizzleHandles = true;
      BStation newSource = (BStation)source.newCopy(hints);
      this.progress.fulfill();
      this.progress.constrain(20);
      if (dropFromSource != null && !dropFromSource.isEmpty()) {
         ApplicationTemplateUtil.deleteComponents(newSource, dropFromSource);
      }

      this.progress.fulfill();
      Map<Object, Object> handleMap = new HashMap<>();
      Map<Object, BComponent> needHandles = new HashMap<>();
      this.progress.constrain(40);
      this.progressMessage("application.deploy.annotate");
      this.transferAnnotations(newSource, target, handleMap, needHandles);
      this.progress.fulfill();
      this.progress.constrain(50);
      BComponent[] roots = ApplicationTemplateUtil.findApplicationRoots(newSource, keepInTarget);
      this.progress.fulfill();
      this.progressMessage("application.deploy.relink");
      this.progress.constrain(55);
      this.progress.divide(roots.length);

      for (BComponent root : roots) {
         this.collectComponentsThatNeedHandles(root, needHandles);
         this.progress.cycle();
      }

      this.progress.conquer();
      this.progress.fulfill();
      this.progress.constrain(60);
      if (cx instanceof ReplacingContext) {
         ReplacingContext replacingContext = (ReplacingContext)cx;
         this.progress.divide(needHandles.size());

         for (Iterator<Entry<Object, BComponent>> it = needHandles.entrySet().iterator(); it.hasNext(); this.progress.cycle()) {
            Entry<Object, BComponent> entry = it.next();
            BComponent sourceComponent = entry.getValue();
            Object reusedHandle = replacingContext.removeHandle(this.buildTargetSlotPathOrd(sourceComponent));
            if (reusedHandle != null) {
               setHandle(sourceComponent, reusedHandle);
               handleMap.put(entry.getKey(), reusedHandle);
               it.remove();
            }
         }

         this.progress.conquer();
      }

      this.progress.fulfill();
      this.progress.constrain(65);
      Object[] newHandles = this.generateHandles(target.getComponentSpace(), needHandles.size());
      this.progress.fulfill();
      this.progress.constrain(70);
      this.progress.divide(needHandles.size());
      int i = 0;

      for (Entry<Object, BComponent> entry : needHandles.entrySet()) {
         Object newHandle = newHandles[i++];
         setHandle(entry.getValue(), newHandle);
         handleMap.put(entry.getKey(), newHandle);
         this.progress.cycle();
      }

      this.progress.conquer();
      this.progress.fulfill();
      this.progress.constrain(75);
      ApplicationTemplateUtil.purgeBrokenConfigProperties(BTemplateConfig.getConfigForApplication(newSource), handleMap.keySet());
      this.progress.fulfill();
      this.progress.constrain(80);
      this.relink(roots, handleMap);
      this.progress.fulfill();
      this.progressMessage("application.deploy.copying");
      this.progress.constrain(85);
      this.progress.divide(roots.length);
      Set<BComponent> parentsOfRoots = new HashSet<>();

      for (BComponent root : roots) {
         BComponent newParent = this.moveComponent(root, newSource, target, cx);
         if (newParent != null) {
            parentsOfRoots.add(newParent);
         }

         this.progress.cycle();
      }

      this.progress.conquer();
      this.progress.fulfill();
      this.progressMessage("application.deploy.sorting");
      this.progress.constrain(90);
      this.progress.divide(parentsOfRoots.size());

      for (BComponent parent : parentsOfRoots) {
         this.reorderChildrenToMatchSource(parent, target, source);
         this.progress.cycle();
      }

      this.progress.conquer();
      this.progress.fulfill();
      this.progress.divide(roots.length);

      for (BComponent root : roots) {
         root.start();
         this.progress.cycle();
      }

      this.progress.conquer();
   }

   private BOrd buildTargetSlotPathOrd(BComponent component) {
      Deque<String> names = new ArrayDeque<>();

      for (String name = component.getName(); name != null; name = component == null ? null : component.getName()) {
         names.addFirst(name);
         BComplex parent = component.getParent();
         component = parent instanceof BComponent ? (BComponent)parent : null;
      }

      return BOrd.make(new SlotPath("slot", names.toArray(new String[0])));
   }

   public void clearApplicationComponents(BStation target, NameTree keepInTarget, Context cx) {
      this.progress.constrain(20);
      this.progressMessage("application.clear.finding");
      BComponent[] roots = ApplicationTemplateUtil.findApplicationRoots(target, keepInTarget);
      this.progress.fulfill();
      this.progress.divide(roots.length);

      for (BComponent root : roots) {
         assert root.getPropertyInParent().isDynamic();

         BComponent parent = root.getParent().asComponent();
         Property property = root.getPropertyInParent();
         this.progressMessage("application.clear.removingComponent", root.toPathString());
         parent.setDisplayName(property, null, null);
         parent.remove(property, cx);
         this.progress.cycle();
      }

      this.progress.conquer();
   }

   private void transferAnnotations(BComponent source, BComponent target, Map<Object, Object> handleMap, Map<Object, BComponent> needHandles) {
      for (Property targetProperty : target.getPropertiesArray()) {
         String name = targetProperty.getName();
         Property sourceProperty = source.getProperty(name);
         if (sourceProperty != null) {
            BFormat displayName = source.getDisplayNameFormat(sourceProperty);
            BFormat otherDisplayName = target.getDisplayNameFormat(targetProperty);
            if (!Objects.equals(displayName, otherDisplayName)) {
               target.setDisplayName(targetProperty, displayName, null);
            }

            BValue targetValue = target.get(targetProperty);
            BValue sourceValue = source.get(sourceProperty);
            if (targetValue.isComponent() && sourceValue.isComponent()) {
               BComponent sourceComponent = sourceValue.asComponent();
               BComponent targetComponent = targetValue.asComponent();
               BValue sourceAnnotation = sourceComponent.get("wsAnnotation");
               if (sourceAnnotation instanceof BWsAnnotation) {
                  BValue targetAnnotation = targetComponent.get("wsAnnotation");
                  if (targetAnnotation == null) {
                     targetComponent.add("wsAnnotation", sourceAnnotation);
                  } else {
                     targetComponent.set("wsAnnotation", sourceAnnotation);
                  }
               }

               Object sourceHandle = sourceComponent.getHandle();
               if (sourceHandle != null) {
                  Object targetHandle = targetComponent.getHandle();
                  if (targetHandle == null) {
                     needHandles.put(sourceHandle, targetComponent);
                  } else {
                     handleMap.put(sourceHandle, targetHandle);
                  }
               }

               this.transferAnnotations(sourceComponent, targetComponent, handleMap, needHandles);
            }
         }
      }
   }

   private Object[] generateHandles(BComponentSpace space, int count) {
      return space != null && count != 0 ? (Object[])space.fw(103, count, null, null, null) : new Object[0];
   }

   private void collectComponentsThatNeedHandles(BComponent component, Map<Object, BComponent> needHandles) {
      Object handle = component.getHandle();
      if (handle != null) {
         needHandles.put(handle, component);
      }

      SlotCursor<Property> cursor = component.getProperties();

      while (cursor.nextComponent()) {
         this.collectComponentsThatNeedHandles(cursor.get().asComponent(), needHandles);
      }
   }

   private BComponent moveComponent(BComponent component, BStation sourceStation, BStation targetStation, Context cx) {
      assert component.getPropertyInParent().isDynamic();

      BComponent newParent = null;
      String name = component.getName();
      BComponent sourceParent = component.getParent().asComponent();
      Stack<String> lineage = getLineage(sourceParent, sourceStation);
      BComponent targetParent = resolveNamedDescendant(targetStation, lineage);
      if (targetParent != null && targetParent.get(name) == null) {
         Property sourceProperty = component.getPropertyInParent();
         BFormat displayName = sourceParent.getDisplayNameFormat(sourceProperty);
         sourceParent.remove(sourceProperty, Context.skipRemoveCheck);
         this.progress.step(50);
         if (cx == null) {
            cx = new BasicContext(Context.NULL, BFacets.make("niagaraAutoStart", BBoolean.FALSE));
         }

         if (!BBoolean.FALSE.equals(cx.getFacet("niagaraAutoStart"))) {
            throw new IllegalStateException("Context must include facet: niagaraAutoStart=FALSE");
         }

         Property targetProperty = targetParent.add(name, component, cx);
         targetParent.setDisplayName(targetProperty, displayName, null);
         newParent = targetParent;
         this.progressMessage("application.deploy.added", component.toPathString());
      } else {
         this.progressMessage("application.deploy.notAdded", component.toPathString());
      }

      this.progress.step(100);
      return newParent;
   }

   private void reorderChildrenToMatchSource(BComponent parent, BStation station, BStation sourceStation) {
      if (parent != null) {
         Stack<String> lineage = getLineage(parent, station);
         BComponent sourceParent = resolveNamedDescendant(sourceStation, lineage);
         if (sourceParent != null) {
            Property[] propertiesInOrder = parent.getDynamicPropertiesArray();
            Set<Property> propertiesToReorder = new HashSet<>(Arrays.asList(propertiesInOrder));
            List<Property> propertiesInNewOrder = new ArrayList<>();

            for (Property sourceProperty : sourceParent.getDynamicPropertiesArray()) {
               String nextPropertyName = sourceProperty.getName();
               Property nextProperty = parent.getProperty(nextPropertyName);
               if (propertiesToReorder.remove(nextProperty)) {
                  propertiesInNewOrder.add(nextProperty);
               }
            }

            for (Property nextProperty : propertiesInOrder) {
               if (propertiesToReorder.remove(nextProperty)) {
                  propertiesInNewOrder.add(nextProperty);
               }
            }

            parent.reorder(propertiesInNewOrder.toArray(new Property[0]));
         }
      }
   }

   private static BComponent resolveNamedDescendant(BStation station, Stack<String> lineage) {
      BComplex descendant = station;

      while (!lineage.empty()) {
         String propertyName = lineage.pop();
         Property property = descendant.getProperty(propertyName);
         if (property == null && descendant.isComponent()) {
            property = descendant.asComponent().add(propertyName, new BUnrestrictedFolder());
         }

         if (property == null) {
            return null;
         }

         BValue candidate = descendant.get(property);
         if (!candidate.isComponent()) {
            return null;
         }

         descendant = candidate.asComponent();
      }

      return !descendant.isComponent() ? null : descendant.asComponent();
   }

   private static Stack<String> getLineage(BComponent component, BStation station) {
      Stack<String> names = new Stack<>();
      BComplex next = component;

      for (String nextName = component.getName(); next != station && nextName != null; nextName = next == null ? null : next.getName()) {
         names.push(nextName);
         next = next.getParent();
      }

      return names;
   }

   private void progressMessage(String lexKey, String... args) {
      String[] newArgs = Arrays.copyOf(args, args.length + 1);
      newArgs[args.length] = String.format("%3d", this.progress.get());
      this.progress.message(lexKey, newArgs);
   }

   private static void setHandle(BComponent component, Object handle) {
      ((ComponentSlotMap)component.fw(1)).setHandle(handle);
   }

   private void relink(BComponent[] components, Map<Object, Object> handleMap) {
      for (BComponent component : components) {
         this.relink(component, handleMap);
      }
   }

   private void relink(BComplex obj, Map<Object, Object> handleMap) {
      for (Property prop : obj.getPropertiesArray()) {
         if (prop.getTypeAccess() == 7) {
            BValue kid = obj.get(prop);
            if (kid instanceof BLink) {
               this.relink((BLink)kid, handleMap);
            } else if (kid instanceof BRelation) {
               this.relink((BRelation)kid, handleMap);
            } else if (kid instanceof BOrd) {
               BOrd newOrd = this.relink((BOrd)kid, handleMap);
               if (newOrd != null) {
                  obj.set(prop, newOrd);
               }
            } else if (kid instanceof BComplex) {
               this.relink((BComplex)kid, handleMap);
            }
         }
      }
   }

   private BOrd relink(BOrd ord, Map<Object, Object> handleMap) {
      String ordStr = ord.toString();
      if (!ordStr.startsWith("h:")) {
         return null;
      } else {
         String oldHandle = ordStr.substring(2).trim();
         Object newHandle = handleMap.get(oldHandle);
         BOrd newOrd;
         if (newHandle == null) {
            newOrd = BOrd.make("h:" + ComponentSlotMap.unswizzle(oldHandle));
         } else {
            newOrd = BOrd.make("h:" + newHandle);
         }

         return newOrd;
      }
   }

   private void relink(BLink link, Map<Object, Object> handleMap) {
      String ord = link.getSourceOrd().toString();
      if (ord.startsWith("h:")) {
         String oldHandle = ord.substring(2).trim();
         Object newHandle = handleMap.get(oldHandle);
         if (newHandle == null) {
            BComponent parent = (BComponent)link.getParent();
            parent.remove(link.getPropertyInParent());
            String targetSlotName = link.getTargetSlotName();
            Slot targetSlot = parent.getSlot(targetSlotName);
            if (targetSlot instanceof Property) {
               Property targetProp = (Property)targetSlot;
               parent.set(targetProp, targetProp.getDefaultValue().newCopy());
            }
         } else {
            BOrd newOrd = BOrd.make("h:" + newHandle);
            link.setSourceOrd(newOrd);
         }
      }
   }

   private void relink(BRelation relation, Map<Object, Object> handleMap) {
      String ord = relation.getSourceOrd().toString();
      if (ord.startsWith("h:")) {
         String oldHandle = ord.substring(2).trim();
         Object newHandle = handleMap.get(oldHandle);
         if (newHandle == null) {
            BComponent parent = (BComponent)relation.getParent();
            parent.remove(relation.getPropertyInParent());
         } else {
            BOrd newOrd = BOrd.make("h:" + newHandle);
            relation.setSourceOrd(newOrd);
         }
      }
   }

   public void clearApplicationFiles(BStation target, String[] fileKeepers) throws IOException {
      BDirectory stationHomeDir = this.getStationHomeDir(target);
      BDirectory stationAceDir = this.getStationDir(target, new FilePath("^^ace"));
      if (stationHomeDir != null) {
         if (stationAceDir != null) {
            this.progress.constrain(75);
         }

         HashSet<String> keepTheseFiles = new HashSet<>(Arrays.asList(fileKeepers));
         this.clearDirectory(stationHomeDir, keepTheseFiles);
         if (stationAceDir != null) {
            this.progress.fulfill();
         }
      }

      if (stationAceDir != null) {
         this.clearDirectory(stationAceDir);
      }
   }

   public void installApplicationFiles(BNtplFile applicationFile, BStation target) throws IOException {
      BDirectory[] sourceDirectories = applicationFile.getStationFileDirectories();
      BFileSpace targetFileSpace = this.getStationFileSpace(target);
      if (targetFileSpace == null) {
         this.progressMessage("application.files.noTarget");
      } else {
         this.copyApplicationFiles(sourceDirectories, targetFileSpace, new FilePath("^^"));
      }
   }

   private void copyApplicationFiles(BDirectory[] sourceDirectories, BFileSpace targetSpace, FilePath targetPath) {
      if (sourceDirectories.length != 0) {
         this.progress.divide(sourceDirectories.length);

         for (BDirectory sourceDirectory : sourceDirectories) {
            FilePath nextPath = targetPath.merge(sourceDirectory.getFileName());
            if (nextPath.equals(new FilePath("^^shared"))) {
               nextPath = new FilePath("^");
            }

            this.copyApplicationFiles(sourceDirectory, targetSpace, nextPath);
            this.progress.cycle();
         }

         this.progress.conquer();
      }
   }

   private void copyApplicationFiles(BDirectory sourceDirectory, BFileSpace targetSpace, FilePath targetPath) {
      BIFile[] files = sourceDirectory.listFiles();
      if (files.length != 0) {
         this.progress.divide(files.length);

         for (BIFile file : files) {
            FilePath filePath = targetPath.merge(file.getFileName());
            if (file instanceof BDirectory) {
               this.copyApplicationFiles((BDirectory)file, targetSpace, filePath);
            } else {
               boolean copyFailed = false;

               try {
                  BIFile newFile = targetSpace.makeFile(targetPath.merge(file.getFileName()));
                  BajaFileUtil.pipe(file, newFile);
               } catch (IOException var12) {
                  copyFailed = true;
                  this.progressMessage("application.files.copyFailed", filePath.toString(), var12.toString());
               }

               if (!copyFailed) {
                  this.progressMessage("application.files.copied", filePath.toString());
               }
            }

            this.progress.cycle();
         }

         this.progress.conquer();
      }
   }

   private BFileSpace getStationFileSpace(BStation target) {
      BDirectory stationHomeDir = this.getStationHomeDir(target);
      return stationHomeDir == null ? null : stationHomeDir.getFileSpace();
   }

   private BDirectory getStationHomeDir(BStation target) {
      return this.getStationDir(target, new FilePath("^"));
   }

   private BDirectory getStationDir(BStation target, FilePath path) {
      BOrd directoryOrd = BOrd.make(path);
      BDirectory directory = null;

      try {
         directory = (BDirectory)directoryOrd.get(target);
      } catch (UnresolvedException var6) {
      }

      return directory;
   }

   private void clearDirectory(BDirectory directory) {
      this.clearDirectory(directory, null);
   }

   private boolean clearDirectory(BDirectory directory, Set<String> keepTheseFiles) {
      if (directory == null) {
         return true;
      } else {
         BIFile[] files = directory.listFiles();
         if (files.length == 0) {
            return true;
         } else {
            boolean allFilesRemoved = true;
            this.progress.divide(files.length);

            for (BIFile file : files) {
               String filePath = file.getFilePath().toString();
               boolean isDirectory = file instanceof BDirectory;
               boolean allowedToRemove = keepTheseFiles == null || !keepTheseFiles.contains(filePath);
               allFilesRemoved = allFilesRemoved && allowedToRemove;
               if (allowedToRemove && isDirectory) {
                  BDirectory childDir = (BDirectory)file;
                  allowedToRemove = this.clearDirectory(childDir, keepTheseFiles);
                  allFilesRemoved = allFilesRemoved && allowedToRemove;
               }

               if (allowedToRemove) {
                  allowedToRemove = false;

                  try {
                     file.delete();
                     allowedToRemove = true;
                  } catch (IOException var13) {
                  }

                  allFilesRemoved = allFilesRemoved && allowedToRemove;
                  if (!isDirectory) {
                     this.progressMessage(allowedToRemove ? "application.files.removed" : "application.files.notRemoved", filePath);
                  }
               }

               this.progress.cycle();
            }

            this.progress.conquer();
            return allFilesRemoved;
         }
      }
   }

   private static Map<String, BValue> getApplicationProperties(BStation station) {
      Map<String, BValue> rootProps = new HashMap<>();
      BApplicationService[] serviceArray = (BApplicationService[])station.getServices().getChildren(BApplicationService.class);
      if (serviceArray != null && serviceArray.length == 1) {
         BApplicationService applicationService = serviceArray[0];
         BTemplateConfig config = applicationService.getConfiguration();

         for (Property property : config.getProperties()) {
            if ((config.getFlags(property) & 2) == 0 && !property.isFrozen()) {
               BValue curVal = config.get(property);
               if (!(curVal instanceof BPxView) && !(curVal instanceof BLink) && !(curVal instanceof BConfigBinding) && !(curVal instanceof BComponent)) {
                  rootProps.put(property.getName(), curVal);
               }
            }
         }

         return rootProps;
      } else {
         throw new IllegalStateException();
      }
   }

   private static void setApplicationProperties(BStation station, Map<String, BValue> applicationProperties) {
      BApplicationService[] serviceArray = (BApplicationService[])station.getServices().getChildren(BApplicationService.class);
      if (serviceArray != null && serviceArray.length == 1) {
         BApplicationService applicationService = serviceArray[0];
         BTemplateConfig config = applicationService.getConfiguration();

         for (Property property : config.getProperties()) {
            BValue bValue = applicationProperties.get(property.getName());
            if (bValue != null) {
               config.set(property, bValue.newCopy(true));
            }
         }
      } else {
         throw new IllegalStateException();
      }
   }

   private static NameTree getMissingOptionalComponents(BStation station, Array<BOrd> optionalOrds) {
      BOrdList missingOptionalOrds = BOrdList.DEFAULT;

      for (BOrd optionalComponentOrd : optionalOrds) {
         OrdQuery[] queries = optionalComponentOrd.parse();
         SlotPath path = (SlotPath)queries[queries.length - 1];
         String[] names = path.getNames();
         StringBuilder friendlyName = new StringBuilder(names[1]);

         for (int i = 2; i < names.length; i++) {
            friendlyName.append('/');
            friendlyName.append(names[i]);
         }

         BOrd searchForOrd = BOrd.make(new SlotPath(friendlyName.toString()));
         BOrdList searchForList = BOrdList.make(searchForOrd);
         NameTree optionalNameTree = ApplicationTemplateUtil.makeNameTree(searchForList);
         BComponent[] foundComponents = ApplicationTemplateUtil.findComponents(station, optionalNameTree);
         if (foundComponents.length == 0) {
            missingOptionalOrds = BOrdList.add(missingOptionalOrds, searchForOrd);
         }
      }

      return ApplicationTemplateUtil.makeNameTree(missingOptionalOrds);
   }

   @Override
   public void close() {
      if (this.applicationFile != null) {
         this.applicationFile.close();
      }
   }
}
