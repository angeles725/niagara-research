package com.tridium.template.file;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.install.BVersion;
import com.tridium.sys.registry.NAgentList;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.TemplateConst;
import com.tridium.template.manifest.ManifestXMLReader;
import com.tridium.template.manifest.TemplateManifest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BIDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.BIFileStore;
import javax.baja.file.BLocalFileStore;
import javax.baja.file.BMemoryFileStore;
import javax.baja.file.FilePath;
import javax.baja.file.types.image.BImageFile;
import javax.baja.file.types.image.BPngFile;
import javax.baja.file.types.text.BPxFile;
import javax.baja.file.zip.BZipFile;
import javax.baja.file.zip.BZipSpace;
import javax.baja.file.zip.BZipZipSpace;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.space.BComponentSpace;
import javax.baja.space.BSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BStation;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BUuid;
import javax.baja.util.Lexicon;

@NiagaraType
public abstract class BNtplFile extends BZipFile implements BINtplFile, TemplateConst {
   public static final Type TYPE = Sys.loadType(BNtplFile.class);
   private BImageFile pngFile;
   private static final BIcon icon = BIcon.make("module://icons/x16/files/ntpl.png");
   private static final String[] stationFileDirectoryNames = new String[]{"ace", "shared"};
   protected static Lexicon lex = Lexicon.make("template");
   protected static final Logger log = Logger.getLogger("template.ntpl");
   private TemplateManifest manifest;
   private BBogFile bogFile;
   private PxFileRef[] pxRefs;
   private BImageFile[] imageFiles;
   private BDataFile[] stationFiles;

   public Type getType() {
      return TYPE;
   }

   public BNtplFile() {
   }

   public BNtplFile(BMemoryFileStore fileStore, TemplateManifest manifest, BBogFile bogFile, PxFileRef[] pxRefs, BImageFile[] imageFiles) {
      this(fileStore, manifest, bogFile, pxRefs, imageFiles, null);
   }

   public BNtplFile(
      BMemoryFileStore fileStore, TemplateManifest manifest, BBogFile bogFile, PxFileRef[] pxRefs, BImageFile[] imageFiles, BDataFile[] stationFiles
   ) {
      super(fileStore);
      this.manifest = manifest;
      this.bogFile = bogFile;
      this.pxRefs = pxRefs;
      this.imageFiles = imageFiles;
      this.stationFiles = stationFiles;
   }

   public BComponent getBaseComponent() {
      BBogFile tempBogFile = this.loadBog();
      if (tempBogFile == null) {
         return null;
      } else {
         BComponent rootComponent = ((BComponentSpace)tempBogFile.open()).getRootComponent();
         BComponent[] childComponents = rootComponent.getChildComponents();
         return childComponents.length == 0 ? null : childComponents[0];
      }
   }

   protected BTemplateConfig getTemplateConfig() {
      return BTemplateConfig.getConfigForRoot(this.getBaseComponent());
   }

   protected boolean isBaseAStation() {
      BComponent base = this.getBaseComponent();
      return base != null && base.getType().is(BStation.TYPE);
   }

   public String getVendor() {
      return this.getTemplateManifest().vendor;
   }

   public String getVersion() {
      return this.getTemplateManifest().version;
   }

   public String getDescription() {
      return this.getTemplateManifest().description;
   }

   public BUuid getUID() {
      return this.getTemplateManifest().uID;
   }

   public String getTitle() {
      return this.getTemplateManifest().title;
   }

   @Override
   public boolean isOpen() {
      return this.zipSpace != null || this.getSpace() instanceof BMemoryFileSpace;
   }

   public BSpace open() {
      return this.doOpen();
   }

   public void save() throws Exception {
   }

   @Override
   public void close() {
      this.doClose();
      this.manifest = null;
      if (this.pxRefs != null) {
         for (PxFileRef px : this.pxRefs) {
            if (px != null) {
               deleteInMemory(px.getPxFile().getStore());
            }
         }
      }

      this.pxRefs = null;
      if (this.imageFiles != null) {
         for (BImageFile imageFile : this.imageFiles) {
            if (imageFile != null) {
               deleteInMemory(imageFile.getStore());
            }
         }
      }

      this.imageFiles = null;
      if (this.stationFiles != null) {
         for (BDataFile df : this.stationFiles) {
            if (df != null) {
               deleteInMemory(df.getStore());
            }
         }
      }

      this.stationFiles = null;
      if (this.bogFile != null) {
         if (this.bogFile.isOpen()) {
            this.bogFile.close();
         }

         deleteInMemory(this.bogFile.getStore());
      }

      this.bogFile = null;
      deleteInMemory(this.getStore());
   }

   protected BSpace doOpen() {
      if (this.zipSpace == null) {
         if (this.getStore() instanceof BLocalFileStore) {
            this.zipSpace = new BZipSpace(this);
         } else {
            if (this.getStore() instanceof BMemoryFileStore) {
               return null;
            }

            this.zipSpace = new BZipZipSpace(this);
         }
      }

      return this.zipSpace;
   }

   private static void deleteInMemory(BIFileStore fileStore) {
      try {
         if (fileStore instanceof BMemoryFileStore) {
            BMemoryFileSpace.INSTANCE.delete(fileStore.getFilePath());
         }
      } catch (Exception var2) {
         log.log(Level.WARNING, "Error resolving external knobs: " + var2.getLocalizedMessage(), (Throwable)var2);
      }
   }

   @Override
   public TemplateManifest getTemplateManifest() {
      if (this.manifest != null) {
         return this.manifest;
      } else {
         log.fine("getTemplateManifest: " + this.getFileName());
         BZipSpace zipSpace = (BZipSpace)this.doOpen();
         if (zipSpace != null) {
            BIFile manifestXml = zipSpace.getChild(zipSpace.getDirectory(), "template-manifest.xml");
            this.manifest = ManifestXMLReader.decode(manifestXml);
            return this.manifest;
         } else {
            throw new IllegalStateException("no backing file or source component");
         }
      }
   }

   public BBogFile getBog() {
      return this.bogFile;
   }

   private BBogFile loadBog() {
      if (this.bogFile != null) {
         return this.bogFile;
      } else {
         BSpace space = this.doOpen();
         if (space != null) {
            BIFile[] entriesInZipRoot = ((BIDirectory)space).listFiles();

            for (BIFile entry : entriesInZipRoot) {
               if (entry instanceof BBogFile) {
                  return (BBogFile)entry;
               }
            }
         }

         return null;
      }
   }

   @Override
   public PxFileRef[] getPxFiles() {
      if (this.pxRefs != null) {
         return this.pxRefs;
      } else {
         List<BIFile> pxFiles = new ArrayList<>();
         boolean foundInSharedFiles = true;
         BSpace space = this.doOpen();
         if (space != null) {
            NtplUtil.listAllFilesOfTypeInDirectory(this, pxFiles, "/shared", BPxFile.TYPE);
            foundInSharedFiles = !pxFiles.isEmpty();
            if (!foundInSharedFiles) {
               NtplUtil.listAllFilesOfTypeInDirectory(this, pxFiles, "/px", BPxFile.TYPE);
            }
         } else if (this.stationFiles != null) {
            for (BDataFile stationFile : this.stationFiles) {
               String filepath = stationFile.getFilePath().getBody();
               if (filepath.startsWith("shared/")) {
                  TypeInfo fileType = Sys.getRegistry().getFileTypeForExtension(stationFile.getExtension());
                  if (fileType.is(BPxFile.TYPE)) {
                     pxFiles.add(stationFile);
                  }
               }
            }
         }

         List<PxFileRef> pxRefsList = new ArrayList<>();

         for (BIFile file : pxFiles) {
            FilePath filePath = file.getFilePath();
            BMemoryFileStore fileStore = BMemoryFileSpace.INSTANCE.makeMemoryStore(filePath);
            BPxFile pxFileCopy = new BPxFile(fileStore);
            fileStore.setFile(pxFileCopy);
            NtplUtil.copyFile(file, pxFileCopy);
            if (foundInSharedFiles) {
               String relativePath = filePath.getBody();
               if (relativePath.startsWith("/")) {
                  relativePath = relativePath.substring(1);
               }

               if (relativePath.startsWith("shared/")) {
                  relativePath = relativePath.substring("shared/".length());
               }

               pxRefsList.add(new MemoryPxFileRef(pxFileCopy, BOrd.make("file:^" + relativePath), pxFileCopy.getAbsoluteOrd(), pxFileCopy.getFileName()));
            } else {
               TemplateManifest.Resource pxResource = this.getTemplateManifest().getResource(file.getFileName(), "px");
               if (pxResource != null) {
                  pxRefsList.add(new MemoryPxFileRef(pxFileCopy, BOrd.make(pxResource.sourceOrd), pxFileCopy.getAbsoluteOrd(), pxResource.name));
               }
            }
         }

         return pxRefsList.toArray(new PxFileRef[0]);
      }
   }

   @Override
   public BImageFile[] getPxImageFiles() {
      if (this.imageFiles != null) {
         return this.imageFiles;
      } else {
         List<BIFile> pxImageFiles = new ArrayList<>();
         BSpace space = this.doOpen();
         if (space != null) {
            NtplUtil.listAllFilesOfTypeInDirectory(this, pxImageFiles, "/shared", BImageFile.TYPE);
            if (pxImageFiles.isEmpty()) {
               NtplUtil.listAllFilesInDirectory(this, pxImageFiles, "/images");
            }
         } else if (this.stationFiles != null) {
            for (BDataFile stationFile : this.stationFiles) {
               String filepath = stationFile.getFilePath().getBody();
               TypeInfo fileType = Sys.getRegistry().getFileTypeForExtension(stationFile.getExtension());
               if (filepath.startsWith("shared/") && fileType.is(BImageFile.TYPE)) {
                  pxImageFiles.add(stationFile);
               }
            }
         }

         this.imageFiles = new BImageFile[pxImageFiles.size()];

         for (int i = 0; i < pxImageFiles.size(); i++) {
            BIFile entry = pxImageFiles.get(i);
            FilePath filePath = entry.getFilePath();
            TemplateManifest.Resource imageResource = this.manifest.getResource(filePath.getName(), "image");
            FilePath alias = null;
            if (imageResource != null) {
               BOrd sourceOrd = BOrd.make(imageResource.sourceOrd);
               OrdQuery[] queries = sourceOrd.parse();
               if (queries.length == 1 && queries[0] instanceof FilePath) {
                  alias = (FilePath)queries[0];
               }
            }

            BMemoryFileStore fileStore = BMemoryFileSpace.INSTANCE.makeMemoryStore(filePath, alias);
            BImageFile imageFileCopy = new BImageFile(fileStore);
            fileStore.setFile(imageFileCopy);
            NtplUtil.copyFile(entry, imageFileCopy);
            this.imageFiles[i] = imageFileCopy;
         }

         return this.imageFiles;
      }
   }

   @Override
   public BImageFile getImageFile() {
      if (this.pngFile != null) {
         return this.pngFile;
      } else {
         BSpace space = this.doOpen();
         if (space != null) {
            BIFile[] entriesInZipRoot = ((BIDirectory)space).listFiles();

            for (BIFile entry : entriesInZipRoot) {
               if (entry instanceof BPngFile) {
                  BMemoryFileStore fs = BMemoryFileSpace.INSTANCE.makeMemoryStore("image.png");
                  this.pngFile = new BPngFile(fs);
                  fs.setFile(this.pngFile);
                  NtplUtil.copyFile(entry, this.pngFile);
                  break;
               }
            }
         }

         return this.pngFile;
      }
   }

   public abstract FilePath listFilesToBeTransferred(BComponent var1, List<BNtplFile.FileTransferSpec> var2) throws Exception;

   @Override
   public BDataFile[] getStationFiles() {
      if (this.stationFiles == null) {
         ArrayList<BDataFile> stationFilesInMemory = new ArrayList<>();
         BSpace space = this.doOpen();
         if (space != null) {
            ArrayList<BDataFile> stationFilesInZip = new ArrayList<>();

            for (BIFile aFa : ((BZipSpace)space).listFiles()) {
               if (aFa instanceof BDirectory && 0 <= Arrays.binarySearch(stationFileDirectoryNames, aFa.getFileName(), String::compareToIgnoreCase)) {
                  this.addStationFiles((BDirectory)aFa, stationFilesInZip);
               }
            }

            for (BDataFile fileInZip : stationFilesInZip) {
               String relativePath = fileInZip.getFilePath().toString();
               relativePath = relativePath.substring(relativePath.indexOf("/") + 1);
               BMemoryFileStore fs = BMemoryFileSpace.INSTANCE.makeMemoryStore(relativePath);
               BDataFile newF = new BDataFile(fs);
               fs.setFile(newF);
               NtplUtil.copyFile(fileInZip, newF);
               stationFilesInMemory.add(newF);
            }
         }

         this.stationFiles = stationFilesInMemory.toArray(new BDataFile[0]);
      }

      return this.stationFiles;
   }

   private void addStationFiles(BDirectory directoryToAdd, ArrayList<BDataFile> stationFiles) {
      BINavNode[] children = directoryToAdd.getNavChildren();

      for (BINavNode n : children) {
         if (n instanceof BDirectory) {
            this.addStationFiles((BDirectory)n, stationFiles);
         } else if (n instanceof BDataFile) {
            stationFiles.add((BDataFile)n);
         }
      }
   }

   @Override
   public BDirectory[] getStationFileDirectories() {
      ArrayList<BDirectory> stationFileDirectories = new ArrayList<>();
      BSpace space = this.doOpen();
      if (space instanceof BZipSpace) {
         BIFile[] files = ((BZipSpace)space).listFiles();

         for (BIFile file : files) {
            if (file instanceof BDirectory && 0 <= Arrays.binarySearch(stationFileDirectoryNames, file.getFileName(), String::compareToIgnoreCase)) {
               stationFileDirectories.add((BDirectory)file);
            }
         }
      }

      return stationFileDirectories.toArray(new BDirectory[0]);
   }

   public abstract List<Map<String, BVersion>> checkRemoteModuleDependencies(BComponent var1);

   public String getMimeType() {
      return "ntpl";
   }

   public boolean hasNavChildren() {
      return false;
   }

   @Override
   public boolean isReadOnly() {
      return this.getSpace() instanceof BZipSpace;
   }

   @Override
   public boolean isNew() {
      return this.manifest.newCreation;
   }

   public String getTemplateName() {
      return this.isNew() ? this.getBaseComponent().getPropertyInParent().getName() : this.getTemplateConfig().getTemplateName();
   }

   public BOrd getNavOrd() {
      return this.getStore() instanceof BMemoryFileStore ? BOrd.make("memory:/dummy.ntpl") : this.getAbsoluteOrd();
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      BSpace space = this.getSpace();
      if (!(space instanceof BMemoryFileSpace)) {
         return agents;
      } else {
         AgentList var4 = new NAgentList();
         var4.add("template:TemplateView");
         return var4;
      }
   }

   public abstract FilePath deployDir();

   public BIcon getIcon() {
      return icon;
   }

   public static class FileTransferSpec {
      private final BDataFile fileToTransfer;
      private final String newFileName;
      private final FilePath targetPath;

      public FileTransferSpec(BDataFile fileToTransfer, String newFileName, FilePath targetPath) {
         this.fileToTransfer = fileToTransfer;
         this.newFileName = newFileName;
         this.targetPath = targetPath;
      }

      public BDataFile getFileToTransfer() {
         return this.fileToTransfer;
      }

      public String getNewFileName() {
         return this.newFileName;
      }

      public FilePath getTargetPath() {
         return this.targetPath;
      }
   }
}
