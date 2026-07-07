package com.tridium.template.ui.file;

import com.tridium.excel.ui.ExcelUiUtils;
import com.tridium.template.api.NiagaraTemplate;
import com.tridium.template.file.NtplUtil;
import com.tridium.template.ui.BulkDeployUtil;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.security.BPassword;
import javax.baja.sys.BStation;
import javax.baja.sys.BString;
import javax.baja.sys.Localizable;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.file.BFileChooser;
import javax.baja.ui.util.UiLexicon;
import javax.baja.user.BPasswordStrength;
import javax.baja.util.Lexicon;

public class ExportApplicationCommand extends Command {
   private final BStation station;
   private static final Lexicon lex = Lexicon.make("template");
   private static final Logger log = Logger.getLogger("ntpl");
   private static BOrd exportDirectoryOrd;

   public ExportApplicationCommand(BWidget owner, BStation station) {
      super(owner, UiLexicon.bajaui(), "commands.exportApplication");
      this.station = station;
   }

   public CommandArtifact doInvoke() throws Exception {
      if (ExcelUiUtils.informIfNoExcelSupportIsInstalledLocally(this.getOwner())) {
         return null;
      } else {
         try {
            NiagaraTemplate template = NiagaraTemplate.open(this.station);
            Throwable var2 = null;

            try {
               if (template == null) {
                  return null;
               } else {
                  if (exportDirectoryOrd == null) {
                     BDirectory applicationDirectory = NtplUtil.getApplicationDirectory();
                     if (applicationDirectory != null) {
                        exportDirectoryOrd = NtplUtil.getApplicationDirectory().getAbsoluteOrd();
                     }
                  }

                  BFileChooser chooser = BFileChooser.makeSave(this.getOwner());
                  chooser.setDefaultFileName(this.station.getStationName() + ".xlsx");
                  chooser.setCurrentDirectory(exportDirectoryOrd);
                  BulkDeployUtil deployUtil = new BulkDeployUtil();
                  boolean passwordsInTemplates = template.hasSensitiveDataConfig();
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
                        } catch (Exception var30) {
                        }

                        BDialog.warning(
                           this.getOwner(),
                           lex.getText("bulkDeploy.excelExport.insecureFile.title"),
                           lex.getText("bulkDeploy.excelExport.insecureFile.message")
                        );
                        chooser.setDefaultFileName(filename);
                        chooser.setCurrentDirectory(ord.getParent());
                     } else {
                        exportDirectoryOrd = ord;
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
                        } catch (Exception var29) {
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

                  StringBuilder replyMessage = new StringBuilder();
                  File exportFile = deployUtil.exportApplicationTemplateToExcel(exportPath, encryptPassword, template, replyMessage);
                  if (exportFile != null) {
                     BDialog.info(
                        this.getOwner(),
                        lex.getText("templateSideBar.exportConfigs"),
                        BString.make(lex.getText("templateSideBar.exportConfigs.fileExported", new Object[]{exportFile.getName()}))
                     );
                     return null;
                  } else {
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
                     } else {
                        exportFile = new File(exportBase.getParent(), exportBase.getName());
                        if (exportFile.length() == 0L) {
                           exportFile.delete();
                        }

                        return null;
                     }
                  }
               }
            } catch (Throwable var31) {
               var2 = var31;
               throw var31;
            } finally {
               if (template != null) {
                  if (var2 != null) {
                     try {
                        template.close();
                     } catch (Throwable var28) {
                        var2.addSuppressed(var28);
                     }
                  } else {
                     template.close();
                  }
               }
            }
         } catch (Exception var33) {
            log.log(Level.WARNING, lex.getText("bulkDeploy.excelExport.exportError"), (Throwable)var33);
            return null;
         }
      }
   }
}
