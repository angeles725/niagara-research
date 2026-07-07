package com.tridium.template.file;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.file.types.bog.BBogSpace;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.api.TemplateType;
import com.tridium.template.manifest.ManifestXMLWriter;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.util.PasswordUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.baja.driver.BDevice;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIDirectory;
import javax.baja.file.BIFile;
import javax.baja.file.BIFileSpace;
import javax.baja.file.BIFileStore;
import javax.baja.file.BMemoryFileStore;
import javax.baja.file.FilePath;
import javax.baja.file.types.image.BImageFile;
import javax.baja.file.zip.BZipFileEntry;
import javax.baja.file.zip.BZipSpace;
import javax.baja.io.ValueDocEncoder;
import javax.baja.naming.BOrd;
import javax.baja.naming.SyntaxException;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.util.FileUtil;
import javax.baja.space.BComponentSpace;
import javax.baja.space.BSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BUnrestrictedFolder;

public final class NtplUtil {
   public static final Logger log = Logger.getLogger("ntpl");
   public static final String COMPONENT_TEMPLATE_DIR = "templates";
   public static final String STATION_TEMPLATE_DIR = "stationTemplates";
   public static final String APPLICATION_TEMPLATE_DIR = "applicationTemplates";
   public static final String TEMPLATE_MANIFEST = "template-manifest.xml";
   private static final BIFile[] BI_FILES = new BIFile[0];

   private NtplUtil() {
   }

   public static String templateNameFromFilePath(FilePath templateFilePath, TemplateType templateType) {
      String templateFileName = templateFilePath.getName();
      String templateFileExt = templateType == TemplateType.APPLICATION ? ".napl" : ".ntpl";
      return templateFileName.endsWith(templateFileExt)
         ? templateFileName.substring(0, templateFileName.length() - templateFileExt.length())
         : templateFileName;
   }

   public static String templateFolderFromFilePath(FilePath templateFilePath, TemplateType templateType) {
      if (templateFilePath.getParent() == null) {
         return "";
      } else {
         String folder = templateFilePath.getParent().getBody();
         String baseFolder = getTemplateDirectoryPath(templateType).getBody();
         if (folder.equals(baseFolder)) {
            return "";
         } else {
            return folder.startsWith(baseFolder) ? folder.substring(baseFolder.length() + 1) : folder;
         }
      }
   }

   public static FilePath getTemplateDirectoryPath(TemplateType templateType) {
      switch (templateType) {
         case APPLICATION:
            return getApplicationDirectoryPath();
         case STATION:
            return getStationTemplateDirectoryPath();
         default:
            return getTemplateDirectoryPath();
      }
   }

   public static FilePath getTemplateDirectoryPath() {
      return new FilePath("~templates");
   }

   public static FilePath getApplicationDirectoryPath() {
      return new FilePath("~applicationTemplates");
   }

   public static FilePath getStationTemplateDirectoryPath() {
      return new FilePath("~stationTemplates");
   }

   public static BDirectory getTemplateDirectory() {
      try {
         return (BDirectory)getTemplateDirectoryOrd().resolve().get();
      } catch (Exception var1) {
         return null;
      }
   }

   public static BIFile[] getTemplates(BDirectory templateDirectory) {
      List<BIFile> templateFiles = new ArrayList<>();
      getTemplatesFiles(templateFiles, templateDirectory);
      return templateFiles.toArray(BI_FILES);
   }

   private static void getTemplatesFiles(List<BIFile> templateFilesList, BDirectory templateDirectory) {
      if (templateDirectory != null) {
         try {
            BIFile[] children = templateDirectory.listFiles();
            if (children == null || children.length <= 0) {
               return;
            }

            for (BIFile child : children) {
               if (child instanceof BDirectory) {
                  getTemplatesFiles(templateFilesList, (BDirectory)child);
               } else {
                  templateFilesList.add(child);
               }
            }
         } catch (Exception var7) {
            log.log(Level.WARNING, "Exception occurred while listing templates files : " + var7);
         }
      }
   }

   public static BDirectory getApplicationDirectory() {
      try {
         return (BDirectory)getApplicationDirectoryOrd().resolve().get();
      } catch (Exception var1) {
         return null;
      }
   }

   public static BOrd makeTemplateDirectoryOrd(TemplateType templateType) {
      switch (templateType) {
         case APPLICATION:
            return makeApplicationDirectoryOrd();
         case STATION:
            return makeStationTemplateDirectoryOrd();
         default:
            return makeTemplateDirectoryOrd();
      }
   }

   public static BOrd makeTemplateDirectoryOrd() {
      return makeDirectoryOrd("templates");
   }

   public static BOrd makeApplicationDirectoryOrd() {
      return makeDirectoryOrd("applicationTemplates");
   }

   public static BOrd makeStationTemplateDirectoryOrd() {
      return makeDirectoryOrd("stationTemplates");
   }

   private static BOrd makeDirectoryOrd(String directory) {
      return BOrd.make("local:|file:~" + directory);
   }

   public static BOrd getTemplateDirectoryOrd() throws Exception {
      return getDirectoryOrd("templates");
   }

   public static BOrd getApplicationDirectoryOrd() throws Exception {
      return getDirectoryOrd("applicationTemplates");
   }

   public static BOrd getStationTemplateDirectoryOrd() throws Exception {
      return getDirectoryOrd("stationTemplates");
   }

   private static BOrd getDirectoryOrd(String directory) throws Exception {
      BOrd directoryOrd = makeDirectoryOrd(directory);

      try {
         directoryOrd.resolve();
      } catch (UnresolvedException var4) {
         BDirectory userHome = BFileSystem.INSTANCE.getUserHome();
         userHome.getFileSpace().makeDir(userHome.getFilePath().merge(directory));
      }

      return directoryOrd;
   }

   public static FilePath makeNtpl(
      String templateName,
      String templateFolder,
      TemplateType templateType,
      BComponent root,
      BTemplateConfig config,
      PxFileRef[] pxFiles,
      BImageFile image,
      BINtplFile ntplFile,
      boolean creatingNew
   ) {
      try {
         BImageFile[] pxImages = null;
         BIFile ntplFileHandle = ntplFile instanceof BIFile ? (BIFile)ntplFile : null;
         FilePath outFilePath = null;
         if (ntplFile != null) {
            pxImages = ntplFile.getPxImageFiles();
            outFilePath = ntplFile.getFilePath();
         }

         if (creatingNew) {
            outFilePath = buildTemplateFilePath(templateName, templateFolder, templateType);
            ntplFileHandle = BFileSystem.INSTANCE.makeFile(outFilePath);
         }

         Objects.requireNonNull(ntplFileHandle, "Invalid type for ntplFile");

         try (ZipOutputStream zipOutputStream = new ZipOutputStream(ntplFileHandle.getOutputStream())) {
            ZipEntry zipManifest = new ZipEntry("template-manifest.xml");
            zipManifest.setComment("Template Manifest");
            zipOutputStream.putNextEntry(zipManifest);
            TemplateManifest manifest = config.getManifest();
            ManifestXMLWriter manifestWriter = new ManifestXMLWriter(zipOutputStream);
            manifestWriter.encode(manifest);
            zipOutputStream.closeEntry();
            ZipEntry zipBog = new ZipEntry("template.bog");
            zipBog.setComment("BOG snippet");
            zipOutputStream.putNextEntry(zipBog);
            PasswordUtil.forceClearReversiblePasswords(root);
            ValueDocEncoder encoder = new ValueDocEncoder(zipOutputStream);
            encoder.encodeDocument(root);
            zipOutputStream.closeEntry();
            Map<String, BDataFile> filesToZip = new HashMap<>();

            for (PxFileRef pxFile1 : pxFiles) {
               String memoryOrd = pxFile1.getPxFile().getAbsoluteOrd().encodeToString();
               memoryOrd = memoryOrd.substring(memoryOrd.lastIndexOf(58) + 1);
               filesToZip.putIfAbsent(memoryOrd, pxFile1.getPxFile());
            }

            if (ntplFile != null && (templateType == TemplateType.STATION || templateType == TemplateType.APPLICATION)) {
               BDataFile[] stationFiles = ntplFile.getStationFiles();
               if (stationFiles != null) {
                  for (BDataFile stationFile : stationFiles) {
                     String relativePath = stationFile.getOrdInSpace().encodeToString();
                     relativePath = relativePath.substring(relativePath.indexOf(58) + 1);
                     filesToZip.putIfAbsent(relativePath, stationFile);
                  }
               }
            }

            if (image != null) {
               filesToZip.putIfAbsent("image.png", image);
            }

            if (pxImages != null && (templateType == TemplateType.COMPONENT || templateType == TemplateType.DEVICE)) {
               for (BImageFile pxImage : pxImages) {
                  if (pxImage != null) {
                     filesToZip.putIfAbsent(pxImage.getFilePath().getBody(), pxImage);
                  }
               }
            }

            for (Entry<String, BDataFile> fileEntry : filesToZip.entrySet()) {
               String zipPath = fileEntry.getKey();
               BDataFile fileToZip = fileEntry.getValue();
               ZipEntry zipEntry = new ZipEntry(zipPath);
               zipEntry.setTime(fileToZip.getLastModified().getMillis());
               zipOutputStream.putNextEntry(zipEntry);

               try (InputStream inputStream = fileToZip.getInputStream()) {
                  FileUtil.pipe(inputStream, zipOutputStream);
               }

               zipOutputStream.closeEntry();
            }
         }

         log.info("Niagara Template created: " + outFilePath.toString());
         return outFilePath;
      } catch (Exception var55) {
         log.log(Level.WARNING, "Cannot create template:" + var55.getLocalizedMessage(), (Throwable)var55);
         return null;
      }
   }

   public static TemplateType getTemplateType(BComponent rootComponent, BTemplateConfig templateConfig) {
      if (rootComponent instanceof BStation) {
         return templateConfig.getPropertyInParent().isFrozen() ? TemplateType.APPLICATION : TemplateType.STATION;
      } else {
         return rootComponent instanceof BDevice ? TemplateType.DEVICE : TemplateType.COMPONENT;
      }
   }

   public static String buildTemplateFileName(String templateName, TemplateType templateType) {
      return templateName + "." + (templateType == TemplateType.APPLICATION ? "napl" : "ntpl");
   }

   public static FilePath buildTemplateFolderPath(String templateFolder, TemplateType templateType) {
      if (templateFolder.startsWith(".")) {
         throw new SyntaxException(templateFolder);
      } else {
         FilePath filePath = new FilePath(templateFolder);
         if (filePath.isAbsolute()) {
            return filePath;
         } else {
            StringBuilder path = new StringBuilder();
            path.append('~');
            switch (templateType) {
               case APPLICATION:
                  path.append("applicationTemplates");
                  break;
               case STATION:
                  path.append("stationTemplates");
                  break;
               default:
                  path.append("templates");
            }

            if (!templateFolder.isEmpty()) {
               path.append('/');
               path.append(templateFolder);
            }

            return new FilePath(path.toString());
         }
      }
   }

   public static FilePath buildTemplateFilePath(String templateName, String templateFolder, TemplateType templateType) {
      FilePath folderPath = buildTemplateFolderPath(templateFolder, templateType);
      String fileName = buildTemplateFileName(templateName, templateType);
      return new FilePath(folderPath.getBody() + "/" + fileName);
   }

   public static FilePath buildTemplateFilePath(String templatePathAndName, TemplateType templateType) {
      int lastSlash = templatePathAndName.lastIndexOf(47);
      String templateName = templatePathAndName.substring(lastSlash + 1);
      String templateFolder = lastSlash < 1 ? "" : templatePathAndName.substring(0, lastSlash);
      return buildTemplateFilePath(templateName, templateFolder, templateType);
   }

   public static FilePath getTemplateRelativePath(FilePath filePath, TemplateType templateType) {
      String body = filePath.getBody();
      String prefix = getTemplateDirectoryPath(templateType).getBody() + "/";
      return body.startsWith(prefix) ? new FilePath(body.substring(prefix.length())) : filePath;
   }

   public static void copyFile(BIFile fromFile, BIFile toFile) {
      try (
         InputStream in = fromFile.getInputStream();
         OutputStream out = toFile.getOutputStream();
      ) {
         FileUtil.pipe(in, out);
      } catch (Exception var34) {
         log.log(Level.WARNING, "Cannot copy file:" + var34.getLocalizedMessage(), (Throwable)var34);
      }
   }

   public static boolean isTemplate(BComponentSpace space) {
      boolean isTemplate = false;
      if (space instanceof BBogSpace) {
         boolean mightBeTemplate = false;
         BBogFile bogFile = ((BBogSpace)space).getBogFile();
         if (bogFile.getType() == BBogFile.TYPE) {
            BIFileStore fileStore = bogFile.getStore();
            if (fileStore instanceof BZipFileEntry) {
               BIFileSpace zipSpace = fileStore.getFileSpace();
               if (zipSpace instanceof BZipSpace) {
                  BIFile manifestFile = zipSpace.findFile(new FilePath("template-manifest.xml"));
                  if (manifestFile != null) {
                     mightBeTemplate = true;
                  }
               }
            } else if (fileStore instanceof BMemoryFileStore) {
               mightBeTemplate = true;
            }

            if (mightBeTemplate) {
               BComponent root = space.getRootComponent();
               if (root instanceof BUnrestrictedFolder) {
                  BComponent[] children = root.getChildComponents();
                  if (children != null && (children.length == 1 || children.length == 2)) {
                     isTemplate = true;
                  }
               }
            }
         }
      }

      return isTemplate;
   }

   public static void listAllEntries(BNtplFile ntplFile, List<BIFile> entryList) {
      listEntries(ntplFile, entryList, true, true, null, null, true);
   }

   public static void listAllDirectories(BNtplFile ntplFile, List<BIFile> directoryList) {
      listEntries(ntplFile, directoryList, true, false, null, null, true);
   }

   public static void listAllMatchingDirectories(BNtplFile ntplFile, List<BIFile> directoryList, String nameContains) {
      listEntries(ntplFile, directoryList, true, false, null, f -> f.getFileName().contains(nameContains), true);
   }

   public static void listSubdirectories(BNtplFile ntplFile, List<BIFile> directoryList, String root) {
      listEntries(ntplFile, directoryList, true, false, root, null, false);
   }

   public static void listMatchingSubdirectories(BNtplFile ntplFile, List<BIFile> directoryList, String root, String nameContains) {
      listEntries(ntplFile, directoryList, true, false, root, f -> f.getFileName().contains(nameContains), false);
   }

   public static void listAllSubdirectories(BNtplFile ntplFile, List<BIFile> directoryList, String root) {
      listEntries(ntplFile, directoryList, true, false, root, null, true);
   }

   public static void listAllMatchingSubdirectories(BNtplFile ntplFile, List<BIFile> directoryList, String root, String nameContains) {
      listEntries(ntplFile, directoryList, true, false, root, f -> f.getFileName().contains(nameContains), true);
   }

   public static void listAllFiles(BNtplFile ntplFile, List<BIFile> fileList) {
      listEntries(ntplFile, fileList, false, true, null, null, true);
   }

   public static void listAllMatchingFiles(BNtplFile ntplFile, List<BIFile> fileList, String nameContains) {
      listEntries(ntplFile, fileList, false, true, null, f -> f.getFileName().contains(nameContains), true);
   }

   public static void listFilesInDirectory(BNtplFile ntplFile, List<BIFile> fileList, String directory) {
      listEntries(ntplFile, fileList, false, true, directory, null, false);
   }

   public static void listAllFilesInDirectory(BNtplFile ntplFile, List<BIFile> fileList, String directory) {
      listEntries(ntplFile, fileList, false, true, directory, null, true);
   }

   public static void listMatchingFilesInDirectory(BNtplFile ntplFile, List<BIFile> fileList, String directory, String nameContains) {
      listEntries(ntplFile, fileList, false, true, directory, f -> f.getFileName().contains(nameContains), false);
   }

   public static void listAllMatchingFilesInDirectory(BNtplFile ntplFile, List<BIFile> fileList, String directory, String nameContains) {
      listEntries(ntplFile, fileList, false, true, directory, f -> f.getFileName().contains(nameContains), true);
   }

   public static void listAllFilesOfTypeInDirectory(BNtplFile ntplFile, List<BIFile> fileList, String directory, String fileTypeExtension) {
      listEntries(ntplFile, fileList, false, true, directory, f -> f.getFileName().endsWith("." + fileTypeExtension), true);
   }

   public static void listAllFilesOfTypeInDirectory(BNtplFile ntplFile, List<BIFile> fileList, String directory, Type fileType) {
      listEntries(ntplFile, fileList, false, true, directory, f -> Sys.getRegistry().getFileTypeForExtension(f.getExtension()).is(fileType), true);
   }

   private static void listEntries(
      BNtplFile ntplFile,
      List<BIFile> fileList,
      boolean includeDirectories,
      boolean includeFiles,
      String rootDirectory,
      Predicate<BIFile> fileFilter,
      boolean recursive
   ) {
      if (fileList == null) {
         fileList = new ArrayList<>();
      }

      BSpace space = ntplFile.doOpen();
      if (space != null) {
         BIFile[] entries;
         if (rootDirectory != null) {
            if (!rootDirectory.startsWith("/")) {
               rootDirectory = '/' + rootDirectory;
            }

            BIFile root = ((BIFileSpace)space).findFile(new FilePath(rootDirectory));
            if (!(root instanceof BIDirectory)) {
               return;
            }

            entries = ((BIDirectory)root).listFiles();
         } else {
            entries = ((BIDirectory)space).listFiles();
         }

         if (recursive) {
            listEntries(entries, fileList, includeDirectories, includeFiles, fileFilter, recursive);
         }
      }
   }

   private static void listEntries(
      BIFile[] entries, List<BIFile> fileList, boolean includeDirectories, boolean includeFiles, Predicate<BIFile> fileFilter, boolean recursive
   ) {
      for (BIFile entry : entries) {
         boolean isDirectory = entry.isDirectory();
         if (includeDirectories && isDirectory || !isDirectory && includeFiles && (fileFilter == null || fileFilter.test(entry))) {
            fileList.add(entry);
         }

         if (isDirectory && recursive) {
            listEntries(((BIDirectory)entry).listFiles(), fileList, includeDirectories, includeFiles, fileFilter, recursive);
         }
      }
   }
}
