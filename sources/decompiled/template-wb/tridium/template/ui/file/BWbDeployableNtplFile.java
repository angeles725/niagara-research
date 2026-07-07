package com.tridium.template.ui.file;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.file.types.bog.BBogSpace;
import com.tridium.fox.sys.BFoxClientConnection;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.install.BVersion;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import com.tridium.sys.transfer.DeployToComp.DeployTransferResult;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateService;
import com.tridium.template.BTemplateValues;
import com.tridium.template.api.TemplateType;
import com.tridium.template.file.BMemoryFileSpace;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.file.NtplUtil;
import com.tridium.template.file.PxFileRef;
import com.tridium.template.file.BNtplFile.FileTransferSpec;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.manifest.TemplateManifest.Resource;
import com.tridium.template.manifest.TemplateManifest.Subtemplate;
import com.tridium.template.ui.BTemplateManager;
import com.tridium.util.CompUtil;
import com.tridium.workbench.transfer.TransferUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.baja.agent.BAbstractPxView;
import javax.baja.agent.BPxView;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BIDeployable;
import javax.baja.file.BMemoryFileStore;
import javax.baja.file.FilePath;
import javax.baja.file.BIDeployable.Step;
import javax.baja.file.types.image.BImageFile;
import javax.baja.file.types.text.BPxFile;
import javax.baja.file.zip.BZipSpace;
import javax.baja.file.zip.ZipPath;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.Path;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.naming.ViewQuery;
import javax.baja.nre.annotations.FileExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.space.BSpace;
import javax.baja.space.Mark;
import javax.baja.sys.BAbstractService;
import javax.baja.sys.BComponent;
import javax.baja.sys.BMarker;
import javax.baja.sys.BStation;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.px.BPxInclude;
import javax.baja.ui.px.BPxTemplateInfo;
import javax.baja.ui.px.PxDecoder;
import javax.baja.ui.px.PxEncoder;
import javax.baja.ui.px.PxLayer;
import javax.baja.ui.px.PxProperty;
import javax.baja.util.BFormat;
import javax.baja.util.BUuid;
import javax.baja.util.Version;

@NiagaraType(
   ext = {@FileExt(
      name = "ntpl"
   ), @FileExt(
      name = "napl"
   )}
)
public class BWbDeployableNtplFile extends BNtplFile implements BIDeployable {
   public static final Type TYPE = Sys.loadType(BWbDeployableNtplFile.class);
   private Object owner = null;
   private boolean isModule = false;
   private FilePath stationDir = null;
   private String missingModules = null;
   private String missingPxModules = null;

   public Type getType() {
      return TYPE;
   }

   public BWbDeployableNtplFile() {
   }

   public BWbDeployableNtplFile(BMemoryFileStore fs, TemplateManifest manifest, BBogFile bog, PxFileRef[] pxRefs, BImageFile[] imageRefs) {
      super(fs, manifest, bog, pxRefs, imageRefs, null);
   }

   public BWbDeployableNtplFile(
      BMemoryFileStore fs, TemplateManifest manifest, BBogFile bog, PxFileRef[] pxRefs, BImageFile[] imageRefs, BDataFile[] stationFiles
   ) {
      super(fs, manifest, bog, pxRefs, imageRefs, stationFiles);
   }

   public BWbDeployableNtplFile(BNtplFile f, BMemoryFileStore fs) {
      super(fs, f.getTemplateManifest(), f.getBog(), f.getPxFiles(), f.getPxImageFiles(), f.getStationFiles());
   }

   public static BWbDeployableNtplFile make(BNtplFile f) {
      if (f instanceof BWbDeployableNtplFile) {
         return (BWbDeployableNtplFile)f;
      } else {
         BMemoryFileStore fs = BMemoryFileSpace.INSTANCE.makeMemoryStore(f.getFileName());
         BWbDeployableNtplFile dNtplFile = new BWbDeployableNtplFile(f, fs);
         fs.setFile(dNtplFile);
         return dNtplFile;
      }
   }

   public String getDeployName() {
      return SlotPath.escape(this.getFileBase(this.getFileName()));
   }

   public boolean isDeployable(BComponent target) {
      BComponent root = this.getBaseComponent();
      if (root != null) {
         boolean isStationDeploy = root.getType().is(BStation.TYPE);
         if (isStationDeploy) {
            this.notifyError(lex.getText("deploy.noStationDeploy"));
            return false;
         }
      }

      BISession session = target.getSession();
      if (session == null) {
         return false;
      } else {
         BOrd templateServiceOrd = BOrd.make(session.getAbsoluteOrd(), "service:template:TemplateService");

         OrdTarget templateTarget;
         try {
            templateTarget = templateServiceOrd.resolve();
         } catch (UnresolvedException var8) {
            boolean isOffLine = target.getSpace().getType().is(BBogSpace.TYPE);
            if (isOffLine) {
               return true;
            }

            this.notifyError(lex.getText("deploy.templateServiceNotFound.title"), lex.getText("deploy.templateServiceNotFound.msg"), var8);
            return false;
         }

         BPermissions permissions = templateTarget.getPermissionsForTarget();
         if (permissions.hasOperatorRead() && permissions.hasOperatorInvoke()) {
            BAbstractService templateService = (BAbstractService)templateTarget.get();
            templateService.lease();
            if (!templateService.isOperational()) {
               if (templateService.isFault()) {
                  this.notifyError(templateService.getFaultCause());
               } else if (templateService.isDisabled()) {
                  this.notifyError(lex.getText("deploy.disabled"));
               } else {
                  this.notifyError(lex.getText("deploy.templateServiceNotFound.title"), lex.getText("deploy.templateServiceNotFound.msg"));
               }

               return false;
            } else {
               return true;
            }
         } else {
            this.notifyError(lex.getText("deploy.permissionsError"));
            return false;
         }
      }
   }

   public Step[] getSteps(Object owner, BComponent target, String name) throws Exception {
      this.owner = owner;
      Version remoteVersion = (Version)target.fw(404, "template", null, null, null);
      boolean isRemotePre4_3 = remoteVersion.major() == 4 && remoteVersion.minor() < 3;
      if (isRemotePre4_3) {
         TemplateManifest manifest = this.getTemplateManifest();
         BTemplateConfig templateConfig = this.getTemplateConfig();
         BUuid templateUid = BUuid.DEFAULT;
         if (templateConfig != null) {
            templateUid = templateConfig.getUID();
         }

         if (!templateUid.equals(BUuid.DEFAULT)) {
            this.notifyError(lex.get("deploy.4_2_station"));
            return null;
         }
      }

      ArrayList<Step> al = new ArrayList<>();

      try {
         BSpace space = this.getSpace();
         this.isModule = space instanceof BZipSpace;
         BComponent root = this.getBaseComponent();
         TemplateManifest manifestx = this.getTemplateManifest();
         if (root != null) {
            BTemplateConfig tc = this.getTemplateConfig();
            tc.checkForValidTemplate();
            Mark mark = new Mark(root, name);
            boolean hasMissing = !TransferUtil.checkForMissingModules(owner, mark, target, false);
            if (hasMissing) {
               log.finer("BWbDeployableNtplFile.getSteps has missing Modules");
               return null;
            }

            al.add(new Step(mark, target.getAbsoluteOrd()));
         }

         BSpace targetSpace = target.getSpace();
         boolean isTargetTemplate = false;
         boolean isOffLine = targetSpace.getType().is(BBogSpace.TYPE);
         BOrd dplDirOrd = null;
         if (isOffLine) {
            BBogFile targetBogFile = ((BBogSpace)targetSpace).getBogFile();
            FilePath filePath = targetBogFile.getFilePath();
            isTargetTemplate = filePath instanceof ZipPath;
            this.stationDir = filePath.getParent();
         } else {
            this.stationDir = null;
         }

         if (!isTargetTemplate) {
            if (owner != null) {
               Collections.addAll(al, this.makeNtplTransferStep(target));
            }

            PxFileRef[] pxFileRefs = this.getPxFiles();
            if (pxFileRefs != null) {
               FilePath pxDeployDir = this.pxDeployDir();
               dplDirOrd = this.verifyDir(pxDeployDir, target);
               BPxView[] pxViews = null;
               if (root != null) {
                  pxViews = (BPxView[])CompUtil.getDescendants(root, BPxView.class);
               }

               ArrayList<BPxFile> pxl = new ArrayList<>();
               ArrayList<String> nml = new ArrayList<>();

               for (PxFileRef pxFileRef : pxFileRefs) {
                  BPxFile pxf = pxFileRef.getPxFile();
                  String pxNam = pxFileRef.getPxName();
                  Resource res = manifestx.getResource(pxNam, "px");
                  if (res != null && pxViews != null) {
                     for (int n = 0; n < pxViews.length; n++) {
                        BPxView pv = pxViews[n];
                        if (pv != null && pv.getPxFile().toString(null).equals(res.sourceOrd)) {
                           pv.setPxFile(BOrd.make(this.pxDeployPath(pxNam)));
                           pxViews[n] = null;
                        }
                     }
                  }

                  PxDecoder decoder = new PxDecoder(pxf);
                  BWidget wroot = decoder.decodeDocument();
                  BPxInclude[] pxIncludes = (BPxInclude[])CompUtil.getDescendants(wroot, BPxInclude.class);
                  if (pxIncludes != null) {
                     for (BPxInclude pv : pxIncludes) {
                        String pxNam1 = pv.getOrd().toString(null);
                        pxNam1 = pxNam1.substring(pxNam1.lastIndexOf(47) + 1);
                        res = manifestx.getResource(pxNam, "px");
                        if (res == null) {
                           log.log(Level.WARNING, lex.getText("deploy.templatePxResourceResolved"), pxNam);
                        } else {
                           BOrd ord = BOrd.make(this.pxDeployPath(pxNam1));
                           pv.setOrd(ord);
                        }
                     }
                  }

                  BPxTemplateInfo tmplInfo = new BPxTemplateInfo(this.getDeployName(), manifestx.vendor, manifestx.version);
                  CompUtil.setOrAdd(wroot, "templateInfo", tmplInfo, null);
                  PxEncoder pxEncoder = new PxEncoder(pxf.getOutputStream());
                  PxProperty[] propA = decoder.getPxProperties();
                  PxLayer[] layA = decoder.getPxLayers();
                  boolean hasProperties = propA.length > 0;
                  pxEncoder.setPreserveIdentities(hasProperties);
                  pxEncoder.encodeDocument(wroot, decoder.getPxProperties(), decoder.getPxLayers(), (BAbstractPxView)null);
                  String newNam = this.getPxName(target, pxNam);
                  if (newNam != null) {
                     pxl.add(pxf);
                     nml.add(newNam);
                  }
               }

               if (pxl.size() > 0) {
                  BPxFile[] pxa = pxl.toArray(new BPxFile[0]);
                  String[] nma = nml.toArray(new String[0]);
                  al.add(new Step(new Mark(pxa, nma), dplDirOrd));
               }

               BImageFile[] imageFiles = this.getPxImageFiles();
               if (imageFiles != null) {
                  for (BImageFile imageFile : imageFiles) {
                     if (imageFile != null) {
                        Resource resx = manifestx.getResource(imageFile.getFileName(), "image");
                        if (resx != null) {
                           BOrd targetOrd = BOrd.make(this.imageDeployDir(resx.sourceOrd));

                           try {
                              targetOrd.resolve(target);
                              log.fine("Using existing image: " + resx.sourceOrd);
                           } catch (Exception var39) {
                              BOrd imageDeployDir = targetOrd.getParent();
                              String imageDeployName = this.getFileNameFromOrd(targetOrd);
                              al.add(new Step(imageFile, imageDeployName, imageDeployDir));
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var40) {
         this.notifyError(var40.getLocalizedMessage());
      } finally {
         if (!this.isModule && this.isOpen()) {
            this.close();
         }
      }

      Step[] steps = al.toArray(new Step[0]);
      log.finer("BWbDeployableNtplFile.getSteps steps length = " + steps.length);
      return steps;
   }

   private String getFileNameFromOrd(BOrd ord) {
      OrdQuery[] q = ord.parse();
      int lastIndex = q.length - 1;
      if (lastIndex < q.length && lastIndex >= 0) {
         OrdQuery lastQuery = q[lastIndex];
         if (lastQuery instanceof ViewQuery) {
            lastIndex--;
            if (lastIndex >= q.length || lastIndex < 0) {
               return null;
            }

            lastQuery = q[lastIndex];
         }

         if (!(lastQuery instanceof Path)) {
            return null;
         } else {
            Path path = (Path)lastQuery;
            String[] names = path.getNames();
            return names.length == 0 ? null : names[names.length - 1];
         }
      } else {
         return null;
      }
   }

   private void notifyInfo(String message) {
      if (this.owner instanceof BWidget) {
         BDialog.info((BWidget)this.owner, message);
      } else {
         log.log(Level.INFO, message);
      }
   }

   private void notifyError(String message) {
      if (this.owner instanceof BWidget) {
         BDialog.error((BWidget)this.owner, message);
      } else {
         log.log(Level.WARNING, message);
      }
   }

   private void notifyError(String title, String message) {
      if (this.owner instanceof BWidget) {
         BDialog.error((BWidget)this.owner, title, message);
      } else {
         log.log(Level.WARNING, message);
      }
   }

   private void notifyError(String title, String message, Exception e) {
      if (this.owner instanceof BWidget) {
         BDialog.error((BWidget)this.owner, title, message, e);
      } else {
         log.log(Level.WARNING, message, (Throwable)e);
      }
   }

   public void postDeploy(TransferResult[] tres, TransferStrategy strategy, Context cx) {
      BComponent target = null;
      List<String> insertNames = Collections.emptyList();
      if (tres != null && tres.length > 0 && tres[0] != null) {
         DeployTransferResult result = (DeployTransferResult)tres[0];
         target = result.target;
         insertNames = Arrays.asList(result.getInsertNames());
      }

      doPostDeploy(this, (BWidget)this.owner, target, insertNames, statusMessage -> {
         if (strategy != null) {
            strategy.updateStatus(statusMessage);
         }
      }, cx);
   }

   public static void doPostDeploy(
      BWbDeployableNtplFile file, BWidget owner, BComponent target, List<String> insertNames, Consumer<String> statusListener, Context cx
   ) {
      TemplateManifest manifest;
      try {
         manifest = file.getTemplateManifest();
      } catch (Exception var12) {
         manifest = null;
      }

      if (!insertNames.isEmpty()) {
         Version remoteVersion = (Version)target.fw(404, "template", null, null, null);
         if (insertNames.size() > 1 && target.getType().is(BStation.TYPE)) {
            target.lease(Integer.MAX_VALUE);
            BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(target);
            templateConfig.setVersionDate(file.getLastModified());
            templateConfig.propagateConfiguration();
         } else {
            BComponent[] deployedRoots = new BComponent[insertNames.size()];

            for (int i = 0; i < insertNames.size(); i++) {
               BValue deployedRootsValue;
               if (insertNames.size() == 1 && insertNames.get(0).contains(";")) {
                  deployedRootsValue = target;
               } else {
                  deployedRootsValue = target.get(insertNames.get(i));
               }

               deployedRoots[i] = deployedRootsValue.asComponent();
               statusListener.accept("initializing " + deployedRoots[i].getSlotPath());
               if (deployedRootsValue != target) {
                  target.setDisplayName(deployedRoots[i].getPropertyInParent(), BFormat.DEFAULT, null);
               }

               CompUtil.setOrAdd(deployedRoots[i], ROOT_TAG_NAME, BMarker.MARKER, 16389, null, null);
               CompUtil.setOrAdd(deployedRoots[i], NTPL_FILE_TAG_NAME, BString.make(file.getFileName()), 16389, null, null);
               if (manifest != null) {
                  CompUtil.setOrAdd(deployedRoots[i], VENDOR_TAG_NAME, BString.make(manifest.vendor), 16389, null, null);
                  CompUtil.setOrAdd(deployedRoots[i], VERSION_TAG_NAME, BString.make(manifest.version), 16389, null, null);
                  CompUtil.setOrAdd(deployedRoots[i], INFO_TAG_NAME, BString.make(manifest.description), 16389, null, null);
               }

               deployedRoots[i].lease(Integer.MAX_VALUE);
               BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(deployedRoots[i]);
               if (!templateConfig.isRunning()) {
                  templateConfig.propagateConfiguration();
               }

               if (remoteVersion.major() == 4 && remoteVersion.minor() == 3) {
                  if (templateConfig.getVersion().getVendor().isEmpty() && manifest != null) {
                     templateConfig.setVersion(new BVersion(manifest.vendor, manifest.version));
                  }

                  templateConfig.setVersionDate(file.getLastModified());
               }

               deployedRoots[i].loadSlots();
               templateConfig.lease();
            }

            BTemplateManager.processPostDeploy(owner, deployedRoots, statusListener, cx);
         }

         statusListener.accept("postDeployComplete");
      }

      file.closeIfOpen();
   }

   public void closeIfOpen() {
      if (this.isOpen()) {
         try {
            this.close();
         } catch (Exception var2) {
         }
      }
   }

   public Step[] makeNtplTransferStep(BComponent target) throws Exception {
      List<Map<String, BVersion>> missingDependencies = this.checkRemoteModuleDependencies(target);
      Map<String, BVersion> missingElements = missingDependencies != null && missingDependencies.size() >= 1 ? missingDependencies.get(0) : null;
      if (missingElements != null && missingElements.keySet().size() > 0) {
         this.logMissingModules(missingElements, "transfer.missingModuleInfo", Level.WARNING);
         return new Step[0];
      } else {
         missingElements = missingDependencies != null && missingDependencies.size() >= 2 ? missingDependencies.get(1) : null;
         if (missingElements != null && missingElements.keySet().size() > 0) {
            this.logMissingModules(missingElements, "transfer.missingModuleInfo", Level.FINE);
         }

         missingElements = missingDependencies != null && missingDependencies.size() >= 3 ? missingDependencies.get(2) : null;
         if (missingElements != null && missingElements.keySet().size() > 0) {
            this.logMissingModules(missingElements, "transfer.missingPxInfo", Level.FINE);
         }

         List<FileTransferSpec> fileTransferSpecs = new ArrayList<>();
         this.listFilesToBeTransferred(target, fileTransferSpecs);
         Step[] steps = new Step[fileTransferSpecs.size()];

         for (int i = 0; i < fileTransferSpecs.size(); i++) {
            FileTransferSpec fileTransferSpec = fileTransferSpecs.get(i);
            Mark mark = new Mark(fileTransferSpec.getFileToTransfer(), fileTransferSpec.getNewFileName());
            Step step = new Step(mark, BOrd.make(fileTransferSpec.getTargetPath()));
            steps[i] = step;
         }

         log.finer("BWbDeployableNtplFile.makeNtplTransferStep steps length = " + steps.length);
         return steps;
      }
   }

   public FilePath listFilesToBeTransferred(BComponent target, List<FileTransferSpec> fileTransferSpecs) throws Exception {
      assert target != null;

      assert fileTransferSpecs != null;

      TemplateManifest manifest = this.getTemplateManifest();
      BTemplateConfig config = this.getTemplateConfig();
      TemplateType templateType = NtplUtil.getTemplateType(this.getBaseComponent(), config);
      String templateName = config.getTemplateName();
      String thisFileName;
      if (templateName != null && !templateName.isEmpty()) {
         thisFileName = NtplUtil.buildTemplateFileName(templateName, templateType);
      } else {
         thisFileName = this.getFileName();
      }

      FilePath targetDirectory = this.deployDir();
      boolean exist = this.alreadyExist(target, thisFileName);
      log.finer("BWbDeployableNtplFile.makeNtplTransferStep exist = " + exist);
      if (!exist) {
         this.verifyDir(targetDirectory, target);
         fileTransferSpecs.add(new FileTransferSpec(this, this.getFileBase(thisFileName) + '.' + this.getFileExtension(thisFileName), targetDirectory));
      }

      HashSet<String> addedSet = new HashSet<>();

      for (Object subtemplate : manifest.subtemplates) {
         if (subtemplate instanceof Subtemplate) {
            Subtemplate subTmpl = (Subtemplate)subtemplate;
            String ntplFileOrd = subTmpl.ntplFileOrd;
            if (!addedSet.contains(ntplFileOrd)) {
               BNtplFile stFile = null;

               try {
                  stFile = (BNtplFile)BOrd.make(ntplFileOrd).resolve().get();
               } catch (Exception var17) {
                  log.log(Level.WARNING, "Error resolving file ORD:" + var17.getLocalizedMessage(), (Throwable)var17);
               }

               if (stFile != null) {
                  exist = this.alreadyExist(target, stFile.getFileName());
                  if (!exist) {
                     FilePath directory = stFile.deployDir();
                     this.verifyDir(directory, target);
                     fileTransferSpecs.add(
                        new FileTransferSpec(stFile, this.getFileBase(stFile.getFileName()) + '.' + this.getFileExtension(stFile.getFileName()), directory)
                     );
                     addedSet.add(ntplFileOrd);
                  }

                  stFile.close();
               }
            }
         }
      }

      if (!this.isModule && this.isOpen()) {
         this.close();
      }

      return targetDirectory.merge(thisFileName);
   }

   private void logMissingModules(Map<String, BVersion> missingElements, String lexKey, Level logLevel) {
      StringBuilder modules = new StringBuilder().append("\n");
      modules.append(" Template: ").append(this.getTemplateManifest().title).append("\n");

      for (String key : missingElements.keySet()) {
         modules.append(" Module: ").append(key);
         BVersion version = missingElements.get(key);
         if (version != BVersion.ZERO) {
            modules.append("    Version: ").append(version.toString());
         }

         modules.append("\n");
      }

      if (logLevel.equals(Level.WARNING) || logLevel.equals(Level.SEVERE)) {
         this.notifyError(lex.getText(lexKey, new Object[]{modules.toString()}));
      } else if (logLevel.equals(Level.INFO)) {
         this.notifyInfo(lex.getText(lexKey, new Object[]{modules.toString()}));
      } else {
         log.log(logLevel, lex.getText(lexKey, new Object[]{modules.toString()}));
      }
   }

   private FilePath imageDeployDir(String imageOrd) {
      if (this.stationDir != null) {
         String[] split = imageOrd.split("\\^");
         return split.length > 1 ? this.stationDir.merge("shared/" + split[1]) : new FilePath(imageOrd);
      } else {
         String[] split = imageOrd.split("file:");
         return split.length > 1 ? new FilePath(split[1]) : new FilePath(imageOrd);
      }
   }

   private BOrd verifyDir(FilePath dPath, BComponent target) throws Exception {
      BOrd dirOrd = BOrd.make(dPath);

      try {
         dirOrd.resolve(target);
      } catch (UnresolvedException var7) {
         try {
            if (this.stationDir == null) {
               BDirectory rootDir = (BDirectory)BOrd.make("file:^").resolve(target).get();
               rootDir.getFileSpace().makeDir(dPath);
            } else {
               BDirectory rootDir = (BDirectory)BOrd.make(this.stationDir).resolve().get();
               rootDir.getFileSpace().makeDir(dPath);
            }
         } catch (Exception var6) {
            log.log(Level.WARNING, "Error resolving path:" + dPath.toString(), (Throwable)var6);
            throw var6;
         }
      }

      return dirOrd;
   }

   public List<Map<String, BVersion>> checkRemoteModuleDependencies(BComponent target) {
      try {
         BISession session = target.getSession();
         BOrd templateServiceOrd = BOrd.make(session.getAbsoluteOrd(), "service:template:TemplateService");
         BTemplateService templateService = null;

         try {
            OrdTarget templateTarget = templateServiceOrd.resolve();
            templateService = (BTemplateService)templateTarget.get();
         } catch (Exception var15) {
         }

         if (templateService != null) {
            templateService.lease();
            BVector resultVector = templateService.getRemoteModules();
            Property[] propertiesArray = resultVector.getPropertiesArray();
            HashMap<String, BVersion> nreModules = new HashMap<>();
            if (propertiesArray.length > 0) {
               for (Property property : propertiesArray) {
                  BValue bValue = resultVector.get(property);
                  if (bValue instanceof BTemplateValues) {
                     BTemplateValues value = (BTemplateValues)bValue;
                     nreModules.put(value.getModulePartName(), value.getModuleVersion());
                  }
               }

               return TmplUtil.checkModuleDependencies(this, nreModules, true, true);
            }
         }
      } catch (Exception var16) {
         log.log(Level.WARNING, "Error checking module dependencies:" + var16.getLocalizedMessage(), (Throwable)var16);
      }

      return null;
   }

   private boolean alreadyExist(BComponent target, String templateName) {
      try {
         FilePath ntplFilePath = this.deployDir().merge(templateName);
         OrdTarget f = BOrd.make(ntplFilePath).resolve(target);
         if (target.getType().is(BFoxClientConnection.TYPE)) {
            BFoxSession session = ((BFoxClientConnection)target).getFoxSession();
            f = BOrd.make(session.getAbsoluteOrd(), ntplFilePath).resolve();
         }

         if (f != null) {
            BNtplFile file = (BNtplFile)f.get();
            if (file.getCrc() != this.getCrc()) {
               file.close();
               file.delete();
               return false;
            }

            return true;
         }
      } catch (UnresolvedException var6) {
      } catch (Exception var7) {
         log.log(Level.WARNING, "Error checking if template " + templateName + " exists", (Throwable)var7);
      }

      return false;
   }

   private String getFileExtension(String fileName) {
      return fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0 ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";
   }

   private String getFileBase(String fileName) {
      String version = '-' + this.getTemplateManifest().version;
      int findVersion = fileName.lastIndexOf(version);
      if (findVersion != -1 && findVersion != 0) {
         return fileName.substring(0, findVersion);
      } else {
         return fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0 ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
      }
   }

   private String getPxName(BComponent target, String nam) {
      try {
         FilePath pxFilePath = this.pxDeployPath(nam);
         OrdTarget f = BOrd.make(pxFilePath).resolve(target);
         if (f != null) {
            BPxFile file = (BPxFile)f.get();
            file.delete();
         }
      } catch (UnresolvedException var6) {
      } catch (Exception var7) {
         log.log(Level.WARNING, "Error resolving PX file name " + nam, (Throwable)var7);
      }

      return nam;
   }

   private FilePath pxDeployPath(String nam) {
      return this.pxDeployDir().merge(nam);
   }

   private FilePath pxDeployDir() {
      return this.stationDir != null
         ? this.stationDir.merge("shared/px/deploy/" + this.getFileName().substring(0, this.getFileName().indexOf(".")).toLowerCase())
         : new FilePath("^px/deploy/" + this.getFileName().substring(0, this.getFileName().indexOf(".")).toLowerCase());
   }

   public FilePath deployDir() {
      boolean isApplication = this.getTemplateManifest().isApplication;
      String basePath = isApplication ? "^applicationTemplate/" : "^template/";
      String sharedPath = isApplication ? "shared/applicationTemplate/" : "shared/template/";
      return this.deployDir(basePath, sharedPath, this.getTemplateManifest().vendor);
   }

   private FilePath deployDir(String basePath, String sharedPath, String vendor) {
      return this.stationDir != null ? this.stationDir.merge(sharedPath + vendor) : new FilePath(basePath + vendor);
   }
}
