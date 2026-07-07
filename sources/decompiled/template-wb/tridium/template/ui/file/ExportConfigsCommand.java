package com.tridium.template.ui.file;

import com.tridium.excel.ui.ExcelUiUtils;
import com.tridium.fox.sys.file.BFoxFileSpace;
import com.tridium.raster.viewer.BPictureGrid;
import com.tridium.template.file.NtplUtil;
import com.tridium.template.ui.BulkDeployUtil;
import com.tridium.template.ui.tag.BTemplateTagChooser;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.zip.BZipSpace;
import javax.baja.naming.BOrd;
import javax.baja.security.BPassword;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Localizable;
import javax.baja.tag.Tag;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.Subject;
import javax.baja.ui.file.BFileChooser;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.text.BTextEditor;
import javax.baja.user.BPasswordStrength;
import javax.baja.util.Lexicon;
import javax.baja.workbench.nav.tree.BNavTree;

public class ExportConfigsCommand extends Command {
   private static final Lexicon lex = Lexicon.make("template");
   private static final Logger log = Logger.getLogger("ntpl");
   private static BOrd exportFileOrd;

   public ExportConfigsCommand(BWidget owner) {
      super(owner, lex, "commands.exportConfigs");
   }

   protected Command doMerge(Command c) {
      return this;
   }

   public CommandArtifact doInvoke() {
      if (ExcelUiUtils.informIfNoExcelSupportIsInstalledLocally(this.getOwner())) {
         return null;
      } else {
         BObject[] objs = this.getSelectedObjects(this.getOwner());
         if (objs == null) {
            return null;
         } else {
            List<BWbDeployableNtplFile> fileList = new ArrayList<>();

            for (BObject obj : objs) {
               if (obj instanceof BWbDeployableNtplFile) {
                  BWbDeployableNtplFile file = (BWbDeployableNtplFile)obj;
                  if (file.getSpace() instanceof BFoxFileSpace) {
                     log.warning(lex.getText("bulkDeploy.excelExport.remoteFile", new Object[]{file.getFilePath()}));
                  } else {
                     fileList.add((BWbDeployableNtplFile)obj);
                  }
               }
            }

            if (fileList.size() == 0) {
               BDialog.error(
                  this.getOwner(), lex.getText("bulkDeploy.excelExport.noValidFile.title"), lex.getText("bulkDeploy.excelExport.noValidFile.message")
               );
               return null;
            } else {
               try {
                  BWbDeployableNtplFile[] files = fileList.toArray(new BWbDeployableNtplFile[0]);

                  for (int i = 0; i < fileList.size(); i++) {
                     BWbDeployableNtplFile templateFile = (BWbDeployableNtplFile)fileList.get(i).getAbsoluteOrd().resolve().get();
                     files[i] = templateFile;
                  }

                  if (exportFileOrd == null) {
                     exportFileOrd = files[0].getAbsoluteOrd().getParent();
                     boolean isZip = files[0].getSpace() instanceof BZipSpace;
                     if (isZip || exportFileOrd == null) {
                        exportFileOrd = NtplUtil.getTemplateDirectory().getAbsoluteOrd();
                     }
                  }

                  BFileChooser chooser = BFileChooser.makeSave(this.getOwner());
                  String defaultFileName = files[0].getFileName();
                  if (defaultFileName.length() > 5) {
                     defaultFileName = defaultFileName.substring(0, defaultFileName.length() - 5);
                  }

                  defaultFileName = defaultFileName + ".xlsx";
                  chooser.setDefaultFileName(defaultFileName);
                  chooser.setCurrentDirectory(exportFileOrd);
                  BulkDeployUtil deployUtil = new BulkDeployUtil();
                  boolean passwordsInTemplates = deployUtil.templatesHaveConfigPasswords(files);
                  BIFile exportPath = null;

                  while (exportPath == null) {
                     BOrd ord = chooser.show();
                     if (ord == null) {
                        return null;
                     }

                     BIFile candidateExportPath = (BIFile)ord.resolve().get();
                     if (passwordsInTemplates && candidateExportPath.getExtension().equalsIgnoreCase("xls")) {
                        String filename = candidateExportPath.getFileName();
                        filename = filename.substring(0, filename.length() - "xls".length()) + "xlsx";

                        try {
                           candidateExportPath.delete();
                        } catch (Exception var21) {
                        }

                        BDialog.warning(
                           this.getOwner(),
                           lex.getText("bulkDeploy.excelExport.insecureFile.title"),
                           lex.getText("bulkDeploy.excelExport.insecureFile.message")
                        );
                        chooser.setDefaultFileName(filename);
                        chooser.setCurrentDirectory(ord.getParent());
                     } else {
                        exportFileOrd = ord;
                        exportPath = candidateExportPath;
                     }
                  }

                  BPassword encryptPassword = null;
                  boolean strongPassword = false;

                  do {
                     encryptPassword = BExportConfigsPasswordPrompt.getPassword(this.getOwner(), passwordsInTemplates, encryptPassword != null);
                     if (encryptPassword == BExportConfigsPasswordPrompt.DLG_CANCELLED) {
                        try {
                           exportPath.delete();
                        } catch (Exception var20) {
                        }

                        return null;
                     }

                     if (encryptPassword != null) {
                        strongPassword = !encryptPassword.isDefault();
                        if (!strongPassword) {
                           BDialog.error(
                              this.getOwner(),
                              lex.getText("bulkDeploy.excelExport.invalidPassword.title"),
                              lex.getText("bulkDeploy.excelExport.invalidPassword.message")
                           );
                        } else {
                           AtomicReference<Localizable> messageReference = new AtomicReference<>();
                           strongPassword = BPasswordStrength.DEFAULT.isPasswordValid(encryptPassword.getValue().toCharArray(), messageReference::set);
                           if (!strongPassword) {
                              BDialog.error(this.getOwner(), lex.getText("bulkDeploy.excelExport.invalidPassword.title"), messageReference.get().toString(null));
                           }
                        }
                     }
                  } while (encryptPassword != null && !strongPassword);

                  BTextEditor msg = new BTextEditor(lex.getText("bulkDeploy.excelExport.addTagsToConfig.details"), false);
                  BTextEditorPane pane = new BTextEditorPane(msg, 4, 80);
                  Map<BWbDeployableNtplFile, HashSet<Tag>> selectedTagMap = new HashMap<>();
                  if (4 == BDialog.open(this.getOwner(), lex.getText("bulkDeploy.excelExport.addTagsToConfig.title"), pane, 12)) {
                     Map<BWbDeployableNtplFile, HashSet<Tag>> tagMap = new HashMap<>();

                     for (BWbDeployableNtplFile file : files) {
                        HashSet<Tag> fileTags = deployUtil.getStringTagsInTemplate(file);
                        if (!fileTags.isEmpty()) {
                           tagMap.put(file, fileTags);
                        }
                     }

                     if (!tagMap.isEmpty()) {
                        BTemplateTagChooser tagChooser = new BTemplateTagChooser(tagMap);
                        if (1 == BDialog.open(this.getOwner(), lex.getText("bulkDeploy.excelExport.addTagsToConfig.title"), tagChooser, 3)) {
                           selectedTagMap = tagChooser.getResult();
                        }
                     }
                  }

                  StringBuilder replyMessage = new StringBuilder();
                  File exportFile = deployUtil.exportTemplateToExcel(exportPath, encryptPassword, files, selectedTagMap, replyMessage);
                  if (exportFile == null) {
                     log.log(Level.WARNING, lex.getText("bulkDeploy.excelExport.exportError"));
                     BDialog.error(
                        this.getOwner(),
                        lex.getText("templateSideBar.exportConfigsFailure"),
                        BString.make(
                           lex.getText("templateSideBar.exportConfigsFailure.fileExported", new Object[]{exportPath.getFileName()})
                              + "\n"
                              + replyMessage.toString()
                        )
                     );
                     File exportBase = BFileSystem.INSTANCE.pathToLocalFile(exportPath.getFilePath());
                     if (exportBase == null) {
                        return null;
                     }

                     exportFile = new File(exportBase.getParent(), exportBase.getName());
                     if (exportFile.length() == 0L) {
                        exportFile.delete();
                     }

                     return null;
                  }

                  BDialog.info(
                     this.getOwner(),
                     lex.getText("templateSideBar.exportConfigs"),
                     BString.make(lex.getText("templateSideBar.exportConfigs.fileExported", new Object[]{exportFile.getName()}))
                  );
               } catch (Exception var22) {
                  log.log(Level.WARNING, lex.getText("bulkDeploy.excelExport.exportError"), (Throwable)var22);
               }

               return null;
            }
         }
      }
   }

   private BObject[] getSelectedObjects(BWidget owner) {
      BObject[] objs = null;
      if (owner instanceof BNavTree) {
         objs = ((BNavTree)owner).getSelectedObjects();
      } else {
         Subject subject = null;
         if (owner instanceof BTable) {
            subject = ((BTable)owner).getSelection().getSubject();
         } else if (owner instanceof BPictureGrid) {
            subject = ((BPictureGrid)owner).getSubject(null);
         }

         if (subject != null) {
            List<BObject> listOfObjects = new ArrayList<>();

            for (int i = 0; i < subject.size(); i++) {
               Object x = subject.get(i);
               if (x instanceof BObject) {
                  listOfObjects.add((BObject)x);
               }
            }

            if (listOfObjects.size() > 0) {
               objs = listOfObjects.toArray(new BObject[0]);
            }
         }
      }

      return objs;
   }
}
