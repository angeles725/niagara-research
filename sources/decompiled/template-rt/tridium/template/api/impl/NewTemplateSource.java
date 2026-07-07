package com.tridium.template.api.impl;

import com.tridium.install.BDependency;
import com.tridium.install.BVersion;
import com.tridium.sys.transfer.FileToFile;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.TemplateConst;
import com.tridium.template.UpgradeUtil;
import com.tridium.template.api.NiagaraTemplate;
import com.tridium.template.api.TemplateSourceType;
import com.tridium.template.api.TemplateType;
import com.tridium.template.api.TemplateValueSource;
import com.tridium.template.file.DependencyUtil;
import com.tridium.template.manifest.ManifestXMLWriter;
import com.tridium.template.manifest.TemplateFileSpec;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.util.CompUtil;
import com.tridium.util.PasswordUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.file.types.text.BPxFile;
import javax.baja.io.ValueDocEncoder;
import javax.baja.naming.BOrd;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.space.BComponentSpace;
import javax.baja.space.Mark;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BMarker;
import javax.baja.sys.BModule;
import javax.baja.sys.BObject;
import javax.baja.sys.BStation;
import javax.baja.sys.BString;
import javax.baja.sys.Clock;
import javax.baja.sys.Sys;
import javax.baja.util.BUnrestrictedFolder;
import javax.baja.util.BUuid;
import javax.baja.util.Version;

public abstract class NewTemplateSource extends TemplateSourceWithBase {
   private final boolean sourceOffline;
   private BComponent sourceRoot;
   private BIDirectory sourceHomeDir;
   private BIDirectory sourceProtectedHomeDir;
   private boolean sourceDirectoriesAreResolved = false;
   private BComponentSpace componentSpace = null;
   private BComponent deployRoot = null;

   protected NewTemplateSource(BComponent sourceComponent, BIDirectory sourceHomeDir, BIDirectory sourceProtectedHomeDir) {
      this.sourceOffline = !sourceComponent.isRunning();
      this.sourceRoot = sourceComponent;
      this.sourceHomeDir = sourceHomeDir;
      this.sourceProtectedHomeDir = sourceProtectedHomeDir;
   }

   @Override
   protected TemplateValueSource getValueSource() {
      return TemplateValueSource.DEFAULT_VALUE;
   }

   @Override
   protected BComponent getBase() {
      if (this.deployRoot == null && this.sourceRoot != null) {
         this.createRootAndReleaseSource();
      }

      return this.deployRoot;
   }

   protected abstract List<BComponent> getInstallRoots();

   @Override
   public void save(OutputStream out) throws IOException {
      long start = Clock.nanoTicks();
      log.fine(() -> String.format("Starting save to stream of %s template [%s].", this.getTemplateType(), this.getBaseName()));
      TemplateManifest manifest = new TemplateManifest();
      BTemplateConfig config = BTemplateConfig.getOrCreateConfigForRoot(this.getBase(), this.getTemplateType() == TemplateType.APPLICATION);
      this.collectTemplateInfo(manifest, config);
      Set<TemplateFileSpec> filesToStore = this.addFilesToManifest(manifest);
      this.addDependenciesToManifest(filesToStore, manifest);
      this.buildTemplateFile(out, manifest, filesToStore);
      log.fine(() -> String.format("Finished save to stream of %s template [%s].", this.getTemplateType(), this.getBaseName()));
      log.finest(
         () -> String.format("Template save to stream completed in %d ms.", TimeUnit.MILLISECONDS.convert(Clock.nanoTicks() - start, TimeUnit.NANOSECONDS))
      );
   }

   @Override
   public BOrd save(FilePath filePath) throws IOException {
      long start = Clock.nanoTicks();
      log.fine(() -> String.format("Starting template save to: %s", filePath));
      BIFile file = BFileSystem.INSTANCE.makeFile(filePath);

      try (OutputStream fileOut = file.getOutputStream()) {
         this.save(fileOut);
      }

      log.fine(() -> String.format("Finished template save to : %s", filePath));
      log.finest(() -> String.format("Template save %d ms.", TimeUnit.MILLISECONDS.convert(Clock.nanoTicks() - start, TimeUnit.NANOSECONDS)));
      return file.getOrdInSession();
   }

   @Override
   public BOrd save() throws IOException {
      FilePath targetDirPath = new FilePath((this.sourceOffline ? "~" : "^") + this.getDefaultStationDirectoryName());
      BDirectory targetDir = BFileSystem.INSTANCE.makeDir(targetDirPath);
      String filename = FileToFile.getUniqueFilename(targetDir, this.getBaseName() + "." + this.getFileExtension());
      return this.save(targetDirPath.merge(filename));
   }

   @Override
   public String getTitle() {
      return this.getBaseName();
   }

   @Override
   public String getVendor() {
      return Sys.getBajaModule().getVendor(RuntimeProfile.rt);
   }

   private BComponent getTemplateRoot() {
      if (this.componentSpace == null && this.sourceRoot != null) {
         this.createRootAndReleaseSource();
      }

      return this.componentSpace == null ? null : this.componentSpace.getRootComponent();
   }

   @Override
   public TemplateSourceType getSourceType() {
      return TemplateSourceType.CREATE;
   }

   protected BComponent getTransferStrategyParams() {
      return null;
   }

   protected void doPostCopyCleanup(BComponent source, BComponent copy) {
   }

   protected abstract String getDefaultStationDirectoryName();

   protected SortedSet<TemplateFileSpec> getFilesToStore() {
      return Collections.emptySortedSet();
   }

   final BIDirectory getSourceHomeDir() {
      this.resolveSourceDirectories();
      return this.sourceHomeDir;
   }

   final BIDirectory getSourceProtectedHomeDir() {
      this.resolveSourceDirectories();
      return this.sourceProtectedHomeDir;
   }

   private void createRootAndReleaseSource() {
      Objects.requireNonNull(this.sourceRoot);
      log.finer("Copying template root component...");
      BComponentSpace space = new BComponentSpace(null, null, null);
      BComponent templateRoot = new BUnrestrictedFolder();
      space.setRootComponent(templateRoot);
      String name = this.sourceRoot instanceof BStation ? ((BStation)this.sourceRoot).getStationName() : this.sourceRoot.getName();
      Mark sourceMark = name != null && !name.isEmpty() ? new Mark(this.sourceRoot, name) : new Mark(this.sourceRoot);
      TransferStrategy transferStrategy = TransferStrategy.make(16, sourceMark, templateRoot, this.getTransferStrategyParams(), null);
      if (transferStrategy == null) {
         log.severe("Failed to copy the source component into the template; failed to create component copier.");
      } else {
         TransferResult transferResult;
         try {
            transferResult = transferStrategy.transfer();
         } catch (Exception var8) {
            log.severe(() -> String.format("Failed to copy the source component into the template; exception during transfer: %s", var8));
            return;
         }

         if (transferResult == null) {
            log.severe("Failed to copy the source component into the template; transfer produced empty results.");
         } else {
            BComponent deployRoot = templateRoot.get(transferResult.getInsertNames()[0]).asComponent();
            log.finer("Clearing passwords from new template root...");
            PasswordUtil.forceClearReversiblePasswords(templateRoot);
            this.doPostCopyCleanup(this.sourceRoot, deployRoot);
            this.componentSpace = space;
            this.deployRoot = deployRoot;
            this.resolveSourceDirectories();
            this.sourceRoot = null;
         }
      }
   }

   private void resolveSourceDirectories() {
      if (!this.sourceDirectoriesAreResolved) {
         if (this.sourceRoot != null && (this.sourceHomeDir == null || this.sourceProtectedHomeDir == null)) {
            BOrd ordInHost = this.sourceRoot.getOrdInHost();
            if (ordInHost != null) {
               if (this.sourceHomeDir == null) {
                  this.sourceHomeDir = confirmAsDirectory(BOrd.make(ordInHost + "|file:^"), this.sourceRoot);
               }

               if (this.sourceProtectedHomeDir == null) {
                  this.sourceProtectedHomeDir = confirmAsDirectory(BOrd.make(ordInHost + "|file:^^"), this.sourceRoot);
               }
            }
         }

         this.sourceDirectoriesAreResolved = true;
      }
   }

   private static BIDirectory confirmAsDirectory(BOrd ord, BObject base) {
      try {
         return (BIDirectory)ord.resolve(base).get();
      } catch (Exception var3) {
         return null;
      }
   }

   private static String getBuildVersion() {
      BModule module = Sys.getModuleForClass(NiagaraTemplate.class);
      if (module == null) {
         return "";
      } else {
         Version vendorVersion = module.getVendorVersion(RuntimeProfile.rt);
         return vendorVersion == null ? "" : vendorVersion.toString();
      }
   }

   private Set<TemplateFileSpec> addFilesToManifest(TemplateManifest manifest) {
      log.finer("Adding files to template manifest...");
      Set<TemplateFileSpec> filesToStore = this.getFilesToStore();

      for (TemplateFileSpec fileSpec : filesToStore) {
         log.finest(() -> String.format("Adding file to template manifest: %s, %s, %s", fileSpec.getName(), fileSpec.getType(), fileSpec.getSourceOrd()));
         manifest.addResource(fileSpec.getName(), fileSpec.getType(), fileSpec.getSourceOrd());
      }

      return filesToStore;
   }

   private void buildTemplateFile(OutputStream out, TemplateManifest manifest, Set<TemplateFileSpec> filesToStore) throws IOException {
      log.finer("Zipping template files...");

      try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
         log.finer("  Zipping the manifest file...");
         ZipEntry manifestEntry = new ZipEntry("template-manifest.xml");
         manifestEntry.setComment("Template Manifest");
         zipOut.putNextEntry(manifestEntry);
         ManifestXMLWriter manifestWriter = new ManifestXMLWriter(zipOut);
         manifestWriter.encode(manifest);
         zipOut.closeEntry();
         log.finer("  Zipping the BOG file...");
         ZipEntry bogEntry = new ZipEntry("template.bog");
         bogEntry.setComment("BOG snippet");
         zipOut.putNextEntry(bogEntry);
         ValueDocEncoder encoder = new ValueDocEncoder(zipOut);
         encoder.encodeDocument(this.getTemplateRoot());
         zipOut.closeEntry();
         if (!filesToStore.isEmpty()) {
            log.finer("  Zipping the supporting files...");
         }

         for (TemplateFileSpec fileSpec : filesToStore) {
            log.finest(() -> String.format("    Zipping file %s...", fileSpec.getName()));
            ZipEntry fileEntry = new ZipEntry(fileSpec.getName());
            zipOut.putNextEntry(fileEntry);

            try (InputStream fileIn = fileSpec.getFile().getInputStream()) {
               BajaFileUtil.pipe(fileIn, zipOut);
            }

            zipOut.closeEntry();
         }

         zipOut.finish();
      }
   }

   private void addDependenciesToManifest(Set<TemplateFileSpec> filesToStore, TemplateManifest manifest) {
      Hashtable<String, BDependency> hashtable = new Hashtable<>();
      List<BComponent> installRoots = this.getInstallRoots();
      if (!installRoots.isEmpty()) {
         log.finer("Adding component dependencies to template manifest...");
      }

      for (BComponent root : this.getInstallRoots()) {
         log.finest(() -> String.format("Adding dependencies for root %s...", root.getSlotPath()));
         DependencyUtil.getBogComponentDependencies(root, hashtable, this.getUseMinorVersionOnDeployment());
      }

      if (!filesToStore.isEmpty()) {
         log.finer("Adding PX file dependencies to template manifest...");
      }

      for (TemplateFileSpec fileSpec : filesToStore) {
         if (fileSpec.getFile() instanceof BPxFile) {
            log.finest(() -> String.format("Adding dependencies for px file %s...", fileSpec.getName()));
            DependencyUtil.getPxFileDependencies((BPxFile)fileSpec.getFile(), hashtable, this.getUseMinorVersionOnDeployment());
         }
      }

      BDependency[] dependencies = new BDependency[hashtable.size()];
      manifest.addDependencies(hashtable.values().toArray(dependencies));
   }

   private void collectTemplateInfo(TemplateManifest manifest, BTemplateConfig config) {
      BComponent deployRoot = this.getBase();
      TemplateType templateType = this.getTemplateType();
      log.finer("Collecting template information...");
      CompUtil.setOrAdd(deployRoot, TemplateConst.ROOT_TAG_NAME, BMarker.MARKER, 16389, null, null);
      manifest.uID = BUuid.make();
      config.setUID(manifest.uID);
      CompUtil.setOrAdd(deployRoot, TemplateConst.UID_TAG_NAME, BString.make(manifest.uID.toString()), 16389, null, null);
      manifest.vendor = this.getVendor();
      BVersion version = new BVersion(manifest.vendor, manifest.version);
      version.setBajaVersionString(Sys.getBajaVersion().toString());
      config.setVersion(version);
      config.setVersionDate(BAbsTime.now());
      CompUtil.setOrAdd(deployRoot, TemplateConst.VENDOR_TAG_NAME, BString.make(manifest.vendor), 16389, null, null);
      CompUtil.setOrAdd(deployRoot, TemplateConst.VERSION_TAG_NAME, BString.make(manifest.version), 16389, null, null);
      manifest.title = deployRoot.getName();
      config.setTemplateName(manifest.title);
      CompUtil.setOrAdd(deployRoot, TemplateConst.TITLE_TAG_NAME, BString.make(manifest.title), 16389, null, null);
      CompUtil.setOrAdd(deployRoot, TemplateConst.NTPL_FILE_TAG_NAME, BString.make(manifest.title + "." + this.getFileExtension()), 16389, null, null);
      CompUtil.setOrAdd(deployRoot, TemplateConst.INFO_TAG_NAME, BString.make(manifest.description), 16389, null, null);
      manifest.buildVersion = getBuildVersion();
      long signature = UpgradeUtil.getTemplateSignature(this.getTemplateRoot());
      manifest.bogSignature = Long.toHexString(signature);
      manifest.isApplication = templateType == TemplateType.APPLICATION;
      manifest.isStation = templateType == TemplateType.STATION;
   }
}
