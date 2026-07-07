package com.tridium.template.ui;

import com.tridium.excel.CTCol;
import com.tridium.excel.Cell;
import com.tridium.excel.CellRangeAddress;
import com.tridium.excel.CellStyle;
import com.tridium.excel.CellType;
import com.tridium.excel.ClientAnchor;
import com.tridium.excel.Comment;
import com.tridium.excel.CreationHelper;
import com.tridium.excel.DataFormatter;
import com.tridium.excel.Drawing;
import com.tridium.excel.EncryptionInfo;
import com.tridium.excel.Encryptor;
import com.tridium.excel.ExcelFileSystem;
import com.tridium.excel.ExcelUtils;
import com.tridium.excel.FillPatternType;
import com.tridium.excel.Font;
import com.tridium.excel.IndexedColors;
import com.tridium.excel.Name;
import com.tridium.excel.RichTextString;
import com.tridium.excel.Row;
import com.tridium.excel.Sheet;
import com.tridium.excel.Workbook;
import com.tridium.neql.component.ComponentTreeIterator;
import com.tridium.sys.tag.ComponentTags;
import com.tridium.sys.transfer.DeployToComp;
import com.tridium.template.BConfigBinding;
import com.tridium.template.BRelationInfo;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateService;
import com.tridium.template.api.NiagaraTemplate;
import com.tridium.template.api.OptionalComponent;
import com.tridium.template.api.TemplateElement;
import com.tridium.template.api.TemplateProperty;
import com.tridium.template.api.TemplateValue;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.file.TemplateManager;
import com.tridium.template.file.TemplateManager.TemplateInfo;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.util.CompUtil;
import com.tridium.util.ThrowableUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.data.BIDataValue;
import javax.baja.driver.BDevice;
import javax.baja.driver.BDriverContainer;
import javax.baja.driver.BIDeviceFolder;
import javax.baja.driver.history.BIArchiveFolder;
import javax.baja.driver.point.BIPointFolder;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.io.BIEncodable;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.nre.util.Array;
import javax.baja.registry.TypeInfo;
import javax.baja.search.BSearchService;
import javax.baja.security.BPassword;
import javax.baja.space.BComponentSpace;
import javax.baja.space.Mark;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BIService;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLink;
import javax.baja.sys.BLong;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.LinkCheck;
import javax.baja.sys.ModuleNotFoundException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.Id;
import javax.baja.tag.Tag;
import javax.baja.tag.Tags;
import javax.baja.util.BFolder;
import javax.baja.util.BFormat;
import javax.baja.util.BNameMap;
import javax.baja.util.BServiceContainer;
import javax.baja.util.BUuid;
import javax.baja.util.BWsAnnotation;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;

public class BulkDeployUtil {
   public static final Level IMPORT_LOG_LEVEL = Level.FINE;
   public static final Lexicon lex = Lexicon.make("template");
   public static final Logger log = Logger.getLogger("template.bulkDeploy");
   protected static final String SLOT_NAME_SEPARATOR = ".";
   private static final String SLOT_NAME_SEPARATOR_REGEX = "\\.";
   public static final int EXCEL_HEADER_ROWS = 6;
   public static final int EXCEL_HEADER_ROWS_WITH_SLOT_PATH_SCOPE = 7;
   public static final int EXCEL_APPLICATION_HEADER_ROWS = 6;
   public static final int EXCEL_COLUMN_FREEZE = 2;
   public static final int EXCEL_APP_COLUMN_FREEZE = 2;
   private BTemplateService templateService;
   private BSearchService searchService;
   private BulkDeployUtil.StyleVault styleVault;
   public static final int DEVICE_INFO_COLUMN_COUNT = 5;
   public static final int COMPONENT_INFO_COLUMN_COUNT = 5;
   public static final int APP_INFO_COLUMN_COUNT = 2;
   public static final int CELL_COMMENT_OFFSET_COLUMN = 5;
   public static final int CELL_COMMENT_OFFSET_ROW = 5;
   public static final int CELL_COMMENT_MAX_STRING_LENGTH = 60;
   protected static final int PARENT_SLOTPATH = 0;
   protected static final int DEPLOYED_NAME = 1;
   protected static final int DISPLAY_NAME = 2;
   protected static final int WS_POSITION = 3;
   protected static final int UNIQUE_DEVICE = 4;
   protected static final int HISTORY_EXT = 5;
   protected static final int APP_UNIQUE_DEVICE = 1;
   protected static final int TEMPLATE_HEADER_LABEL_COLUMN = 0;
   protected static final int TEMPLATE_HEADER_TEXT_COLUMN = 1;
   protected static final int TEMPLATE_TITLE_LABEL_COLUMN = 0;
   protected static final int TEMPLATE_TITLE_INFO_COLUMN = 1;
   protected static final int TEMPLATE_VERSION_LABEL_COLUMN = 0;
   protected static final int TEMPLATE_VERSION_INFO_COLUMN = 1;
   protected static final int INPUT_DEFS_INDEX = 0;
   protected static final int OUTPUT_DEFS_INDEX = 1;
   protected static final int RELATION_DEFS_INDEX = 2;
   protected static final int CONFIG_DEFS_INDEX = 3;
   protected static final int OPTIONAL_DEFS_INDEX = 4;
   protected static final int OPTIONAL_CONFIG_DEFS_INDEX = 5;
   protected static final int TAG_DEFS_INDEX = 6;
   protected static final String TEMPLATE_EXPORT_VERSION = "1.3";
   protected static final String TEMPLATE_EXPORT_VERSION_NAME = "version";
   protected static final String TEMPLATE_VENDOR_NAME = "templateVendor";
   protected static final String TEMPLATE_FILE_NAME = "templateFile";
   protected static final String TEMPLATE_TITLE_NAME = "templateTitle";
   protected static final String TEMPLATE_VERSION_NAME = "templateVersion";
   protected static final String TEMPLATE_UID_NAME = "templateUID";
   protected static final String TEMPLATE_TYPE_NAME = "templateType";
   protected static final String TEMPLATE_SHEET_INFO_COLUMNS_NAME = "infoColumns";
   protected static final String TEMPLATE_INPUTS_COUNT_NAME = "inputsCount";
   protected static final String TEMPLATE_OUTPUTS_COUNT_NAME = "outputsCount";
   protected static final String TEMPLATE_RELATIONS_COUNT_NAME = "relationsCount";
   protected static final String TEMPLATE_CONFIGS_COUNT_NAME = "configsCount";
   protected static final String TEMPLATE_OPTIONALS_COUNT_NAME = "optionalsCount";
   protected static final String TEMPLATE_OPTIONAL_CONFIGS_COUNT_NAME = "optionalConfigsCount";
   protected static final String TEMPLATE_TAG_COUNT_NAME = "tagCount";
   protected static final String KEEP_PRIVATE_NAME = "keepPrivate";
   protected static final String DEFAULT_TARGET_SLOT = "in10";
   protected static final String DEFAULT_SOURCE_SLOT = "out";
   protected static final String DEFAULT_SLOT_PATH_SCOPE = "";
   protected static final String SOURCE_SLOT_MIN_VERSION = "1.1";
   protected static final String SLOT_PATH_SCOPE_MIN_VERSION = "1.3";
   public static final String TEMPLATE_TYPE_COMPONENT = "Component";
   public static final String TEMPLATE_TYPE_DEVICE = "Device";
   public static final String TEMPLATE_TYPE_APPLICATION = "Application";
   protected static final BHistoryExt[] HISTORY_EXTS = new BHistoryExt[0];
   protected static final String[] STRINGS = new String[0];
   protected static final BOrd[] BORDS = new BOrd[0];

   public HashSet<Tag> getStringTagsInTemplate(BWbDeployableNtplFile ntplFile) {
      BComponent templateBase = ntplFile.getBaseComponent();
      HashSet<Tag> tags = new HashSet<>();
      ComponentTreeIterator iterator = new ComponentTreeIterator(templateBase);

      while (iterator.hasNext()) {
         Entity entity = iterator.next();
         if (entity.tags() instanceof ComponentTags) {
            ComponentTags componentTags = (ComponentTags)entity.tags();
            if (!componentTags.isEmpty()) {
               componentTags.forEach(tag -> {
                  if (tag.getValue().getType().is(BString.TYPE)) {
                     tags.add(tag);
                  }
               });
            }
         }
      }

      return tags;
   }

   public File exportTemplateToExcel(
      BIFile exportPath,
      BPassword encryptPassword,
      BWbDeployableNtplFile[] ntplFiles,
      Map<BWbDeployableNtplFile, HashSet<Tag>> tagMap,
      StringBuilder replyMessage
   ) {
      File exportBase = BFileSystem.INSTANCE.pathToLocalFile(exportPath.getFilePath());
      File exportFile = new File(exportBase.getParent(), exportBase.getName());
      boolean useOldExcelFormat = exportPath.getExtension().equalsIgnoreCase("xls");

      try (FileOutputStream excelExport = new FileOutputStream(exportFile)) {
         Workbook wb = ExcelUtils.createWorkbook(!useOldExcelFormat);
         Throwable var12 = null;

         try {
            setTemplateExportVersion(wb);
            this.styleVault = new BulkDeployUtil.StyleVault(wb);

            for (BWbDeployableNtplFile ntplFile : ntplFiles) {
               String templateFileName = ntplFile.getFileName();
               TemplateManifest manifest = ntplFile.getTemplateManifest();
               String templateName = manifest.vendor + '-' + templateFileName;
               Sheet templateSheet = wb.createSheet(templateName);
               this.setDefaultCellStyle(templateSheet);
               setTemplateFile(templateSheet, templateFileName);

               BComponent templateBase;
               BTemplateConfig templateConfig;
               try {
                  templateBase = ntplFile.getBaseComponent();
                  templateConfig = BTemplateConfig.getConfigForRoot(templateBase);
                  if (templateConfig == null) {
                     log.log(Level.WARNING, lex.getText("bulkDeploy.excelExport.templateConfigNotFound", new Object[]{templateFileName}));
                     return null;
                  }
               } catch (Exception var90) {
                  String message = lex.getText("bulkDeploy.excelExport.exportFileError", new Object[]{exportFile.toString()});
                  if (isModuleNotFoundException(var90)) {
                     message = lex.getText("bulkDeploy.excelExport.moduleNotFoundError", new Object[]{ntplFile.getTitle()});
                  }

                  log.log(Level.WARNING, message, (Throwable)var90);
                  replyMessage.append(message);
                  return null;
               }

               try {
                  setTemplateVendor(templateSheet, manifest.vendor);
                  setTemplateTitle(templateSheet, manifest.title);
                  setTemplateVersion(templateSheet, manifest.version);
                  setTemplateUID(templateSheet, templateConfig.getUID().encodeToString());
                  setTemplateTypeName(templateSheet, this.buildTemplateTypeName(manifest));
               } catch (IOException var89) {
                  log.log(Level.WARNING, lex.getText("bulkDeploy.excelExport.templateIdError", new Object[]{templateFileName}), (Throwable)var89);
                  return null;
               }

               HashSet<Tag> tags = tagMap.get(ntplFile);
               this.getTemplateConfigContents(templateBase, templateConfig, manifest, tags, templateSheet);
               if (ExcelUtils.isXmlFormat(templateSheet)) {
                  templateSheet.lockSelectLockedCells(false);
                  templateSheet.lockSelectUnlockedCells(false);
                  templateSheet.lockFormatColumns(false);
                  templateSheet.lockFormatRows(false);
                  templateSheet.lockFormatCells(true);
                  templateSheet.lockInsertColumns(true);
                  templateSheet.lockInsertRows(true);
                  templateSheet.lockInsertHyperlinks(true);
                  templateSheet.lockDeleteColumns(true);
                  templateSheet.lockDeleteRows(true);
                  templateSheet.lockSort(true);
                  templateSheet.lockAutoFilter(true);
                  templateSheet.lockPivotTables(true);
                  templateSheet.lockObjects(true);
                  templateSheet.lockScenarios(true);
                  templateSheet.enableLocking();
               }

               ntplFile.closeIfOpen();
            }

            if (encryptPassword != null && !useOldExcelFormat || this.templatesHaveConfigPasswords(ntplFiles)) {
               setKeepPrivateFlag(wb);
               Row noteRow = wb.getSheetAt(wb.getActiveSheetIndex()).getRow(1);
               generateCell(noteRow, 0, lex.getText("templateSideBar.privacyNote"), this.styleVault.getInfoCellStyle());
               generateCell(noteRow, 1, lex.getText("templateSideBar.privacyMessage"), this.styleVault.getStringCellStyle());
            }

            if (encryptPassword == null) {
               wb.write(excelExport);
               return exportFile;
            } else if (useOldExcelFormat) {
               ExcelUtils.setCurrentUserPassword(encryptPassword.getValue());
               wb.write(excelExport);
               ExcelUtils.setCurrentUserPassword(null);
            } else {
               ExcelFileSystem fs = ExcelUtils.makeFileSystem();
               EncryptionInfo info = ExcelUtils.makeEncryptionInfo();
               Encryptor enc = info.getEncryptor();
               enc.confirmPassword(encryptPassword.getValue());

               try (OutputStream os = enc.getDataStream(fs)) {
                  wb.write(os);
               }

               fs.writeFilesystem(excelExport);
            }
         } catch (Throwable var91) {
            var12 = var91;
            throw var91;
         } finally {
            if (wb != null) {
               if (var12 != null) {
                  try {
                     wb.close();
                  } catch (Throwable var85) {
                     var12.addSuppressed(var85);
                  }
               } else {
                  wb.close();
               }
            }
         }

         return exportFile;
      } catch (Exception var95) {
         String message = lex.getText("bulkDeploy.excelExport.exportFileError", new Object[]{exportFile.toString()});
         log.log(Level.WARNING, message, (Throwable)var95);
         replyMessage.append(message);
         return null;
      }
   }

   public File exportApplicationTemplateToExcel(BIFile exportPath, BPassword encryptPassword, NiagaraTemplate template, StringBuilder replyMessage) {
      List<NiagaraTemplate> templates = new ArrayList<>();
      templates.add(template);
      return this.exportApplicationTemplateToExcel(exportPath, encryptPassword, templates, replyMessage);
   }

   public File exportApplicationTemplateToExcel(BIFile exportPath, BPassword encryptPassword, List<NiagaraTemplate> templates, StringBuilder replyMessage) {
      File exportBase = BFileSystem.INSTANCE.pathToLocalFile(exportPath.getFilePath());
      File exportFile = new File(exportBase.getParent(), exportBase.getName());
      boolean useOldExcelFormat = exportPath.getExtension().equalsIgnoreCase("xls");

      try (FileOutputStream excelExport = new FileOutputStream(exportFile)) {
         this.exportApplicationTemplateToExcel(excelExport, encryptPassword, useOldExcelFormat, templates);
         return exportFile;
      } catch (Exception var21) {
         String message = lex.getText("bulkDeploy.excelExport.exportFileError", new Object[]{exportFile.toString()});
         log.log(Level.WARNING, message, (Throwable)var21);
         replyMessage.append(message);
         return null;
      }
   }

   public void exportApplicationTemplateToExcel(
      OutputStream exportStream, BPassword encryptPassword, boolean useOldExcelFormat, List<NiagaraTemplate> templates
   ) throws IOException, GeneralSecurityException {
      Workbook wb = ExcelUtils.createWorkbook(!useOldExcelFormat);
      Throwable var6 = null;

      try {
         setTemplateExportVersion(wb);
         this.styleVault = new BulkDeployUtil.StyleVault(wb);
         boolean hasSensitiveDataConfig = false;

         for (NiagaraTemplate template : templates) {
            hasSensitiveDataConfig |= template.hasSensitiveDataConfig();
         }

         List<List<NiagaraTemplate>> templateGroups = new ArrayList<>();

         for (NiagaraTemplate template : templates) {
            boolean groupFound = false;

            for (int i = 0; i < templateGroups.size(); i++) {
               List<NiagaraTemplate> templateGroup = templateGroups.get(i);
               if (templateGroup.get(0).getUid().equals(template.getUid())) {
                  if (template.isCompatibleWith(templateGroup.get(0))) {
                     templateGroup.add(template);
                     groupFound = true;
                     break;
                  }

                  if (templateGroup.get(0).isCompatibleWith(template)) {
                     templateGroup.add(0, template);

                     for (int j = i + 1; j < templateGroups.size(); j++) {
                        if (templateGroups.get(j).get(0).isCompatibleWith(template)) {
                           List<NiagaraTemplate> otherGroup = templateGroups.get(j);
                           templateGroups.remove(j);
                           templateGroup.addAll(otherGroup);
                           j--;
                        }
                     }

                     groupFound = true;
                     break;
                  }
               }
            }

            if (!groupFound) {
               List<NiagaraTemplate> newGroup = new ArrayList<>();
               newGroup.add(template);
               templateGroups.add(newGroup);
            }
         }

         for (List<NiagaraTemplate> group : templateGroups) {
            NiagaraTemplate mainTemplate = group.get(0);
            String templateFileName = mainTemplate.getFileName();
            String vendor = mainTemplate.getVendor();
            String templateName = (vendor.isEmpty() ? "" : mainTemplate.getVendor() + '-') + templateFileName;
            Sheet templateSheet = wb.createSheet(templateName);
            this.setDefaultCellStyle(templateSheet);
            setTemplateFile(templateSheet, templateFileName);
            setTemplateVendor(templateSheet, vendor);
            setTemplateTitle(templateSheet, mainTemplate.getTitle());
            setTemplateVersion(templateSheet, mainTemplate.getVersion());
            setTemplateUID(templateSheet, mainTemplate.getUid());
            setTemplateTypeName(templateSheet, mainTemplate.getTemplateType().friendlyName());
            this.getApplicationTemplateConfigContents(mainTemplate, group, templateSheet);
            if (ExcelUtils.isXmlFormat(templateSheet)) {
               templateSheet.lockSelectLockedCells(false);
               templateSheet.lockSelectUnlockedCells(false);
               templateSheet.lockFormatColumns(false);
               templateSheet.lockFormatRows(false);
               templateSheet.lockFormatCells(true);
               templateSheet.lockInsertColumns(true);
               templateSheet.lockInsertRows(true);
               templateSheet.lockInsertHyperlinks(true);
               templateSheet.lockDeleteColumns(true);
               templateSheet.lockDeleteRows(true);
               templateSheet.lockSort(true);
               templateSheet.lockAutoFilter(true);
               templateSheet.lockPivotTables(true);
               templateSheet.lockObjects(true);
               templateSheet.lockScenarios(true);
               templateSheet.enableLocking();
            }
         }

         if (encryptPassword != null && !useOldExcelFormat || hasSensitiveDataConfig) {
            setKeepPrivateFlag(wb);
            Sheet firstSheet;
            if (wb.getNumberOfSheets() < 1) {
               firstSheet = wb.createSheet();
            } else {
               firstSheet = wb.getSheetAt(0);
            }

            Row noteRow = firstSheet.getRow(1);
            if (noteRow == null) {
               noteRow = firstSheet.createRow(1);
            }

            generateCell(noteRow, 0, lex.getText("templateSideBar.privacyNote"), this.styleVault.getInfoCellStyle());
            generateCell(noteRow, 1, lex.getText("templateSideBar.privacyMessage"), this.styleVault.getStringCellStyle());
         }

         if (encryptPassword == null) {
            wb.write(exportStream);
         } else if (useOldExcelFormat) {
            ExcelUtils.setCurrentUserPassword(encryptPassword.getValue());
            wb.write(exportStream);
            ExcelUtils.setCurrentUserPassword(null);
         } else {
            ExcelFileSystem fs = ExcelUtils.makeFileSystem();
            EncryptionInfo info = ExcelUtils.makeEncryptionInfo();
            Encryptor enc = info.getEncryptor();
            enc.confirmPassword(encryptPassword.getValue());

            try (OutputStream os = enc.getDataStream(fs)) {
               wb.write(os);
            }

            fs.writeFilesystem(exportStream);
         }
      } catch (Throwable var39) {
         var6 = var39;
         throw var39;
      } finally {
         if (wb != null) {
            if (var6 != null) {
               try {
                  wb.close();
               } catch (Throwable var35) {
                  var6.addSuppressed(var35);
               }
            } else {
               wb.close();
            }
         }
      }
   }

   private void setDefaultCellStyle(Sheet sheet) {
      if (ExcelUtils.isXmlFormat(sheet)) {
         CTCol cTCol = sheet.addCTCol();
         cTCol.setMin(1L);
         cTCol.setMax(16384L);
         cTCol.setStyle(this.styleVault.getEmptyCellStyle().getIndex());
      }
   }

   private static boolean isModuleNotFoundException(Throwable e) {
      while (e != null) {
         if (e instanceof ModuleNotFoundException) {
            return true;
         }

         e = ThrowableUtil.getCause(e);
      }

      return false;
   }

   private List<Property> getOrderedConfigSlots(BTemplateConfig templateConfig) {
      ArrayList<Property> list = new ArrayList<>();
      SlotCursor<Property> sc = templateConfig.getProperties();
      BConfigBinding[] configBindings = templateConfig.getConfigBindings();

      for (Property p : sc) {
         for (BConfigBinding cb : configBindings) {
            if (p.getName().equals(cb.getSourceSlot())) {
               list.add(p);
               break;
            }
         }
      }

      return list;
   }

   private void getTemplateConfigContents(
      BComponent templateBase, BTemplateConfig templateConfig, TemplateManifest manifest, HashSet<Tag> tags, Sheet templateSheet
   ) {
      Slot[] inputSlots = templateConfig.getInputSlots();
      Slot[] outputSlots = templateConfig.getOutputSlots();
      ArrayList<BRelationInfo> relations = templateConfig.getRelationInfos();
      List<BOrd> optionalComponents = manifest.optional.list();
      Row firstRow = templateSheet.createRow(0);
      Row secondRow = templateSheet.createRow(1);
      Row thirdRow = templateSheet.createRow(2);
      Row fourthRow = templateSheet.createRow(3);
      Row fifthRow = templateSheet.createRow(4);
      Row sixthRow = templateSheet.createRow(5);
      Row seventhRow = templateSheet.createRow(6);
      String description = manifest.description;
      String info = SlotPath.unescape(manifest.info);
      generateCell(firstRow, 0, lex.getText("templateSideBar.description"), this.styleVault.getInfoCellStyle());
      Cell labelCell = generateCell(firstRow, 1, description, this.styleVault.getStringCellStyle());
      if (info == null && !info.isEmpty()) {
         setCellComment(templateSheet, labelCell, info);
      }

      generateCell(thirdRow, 0, lex.getText("excel.label.templateTitle"), this.styleVault.getInfoCellStyle());
      generateCell(thirdRow, 1, manifest.title, this.styleVault.getStringCellStyle());
      generateCell(fourthRow, 0, lex.getText("excel.label.templateVersion"), this.styleVault.getInfoCellStyle());
      generateCell(fourthRow, 1, manifest.version, this.styleVault.getStringCellStyle());
      int uniqueDeviceColumn = 4;
      int columnFreezePane = 2;
      BulkDeployUtil.TemplateType templateType = manifest.isApplication
         ? BulkDeployUtil.TemplateType.APPLICATION
         : (templateBase instanceof BDevice ? BulkDeployUtil.TemplateType.DEVICE : BulkDeployUtil.TemplateType.COMPONENT);
      int excelInstanceInfoColumns;
      switch (templateType) {
         case APPLICATION:
            excelInstanceInfoColumns = 2;
            break;
         case DEVICE:
            excelInstanceInfoColumns = 5;
            break;
         default:
            excelInstanceInfoColumns = 5;
      }

      for (int i = 0; i < excelInstanceInfoColumns; i++) {
         templateSheet.setDefaultColumnStyle(i, this.styleVault.getStringCellStyle());
      }

      byte var72;
      if (templateType == BulkDeployUtil.TemplateType.APPLICATION) {
         var72 = 2;
         labelCell = generateCell(seventhRow, 0, lex.getText("templateSideBar.rowName"), this.styleVault.getInstanceCellStyle());
         setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.rowNameHelp"));
         uniqueDeviceColumn = 1;
         columnFreezePane = 2;
      } else {
         if (templateType == BulkDeployUtil.TemplateType.DEVICE) {
            var72 = 5;
            labelCell = generateCell(seventhRow, 0, lex.getText("templateSideBar.network"), this.styleVault.getInstanceCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.networkHelp"));
            labelCell = generateCell(seventhRow, 1, lex.getText("templateSideBar.device"), this.styleVault.getInstanceCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.deviceHelp"));
         } else {
            var72 = 5;
            labelCell = generateCell(seventhRow, 0, lex.getText("templateSideBar.rootOrd"), this.styleVault.getInstanceCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.rootOrdHelp"));
            labelCell = generateCell(seventhRow, 1, lex.getText("templateSideBar.deployedName"), this.styleVault.getInstanceCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.deployedNameHelp"));
         }

         labelCell = generateCell(seventhRow, 2, lex.getText("templateSideBar.display"), this.styleVault.getOptionalCellStyle());
         setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.displayHelp"));
         labelCell = generateCell(seventhRow, 3, lex.getText("templateSideBar.position"), this.styleVault.getOptionalCellStyle());
         setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.positionHelp"));
      }

      labelCell = generateCell(seventhRow, uniqueDeviceColumn, lex.getText("templateSideBar.uniqueDevice"), this.styleVault.getOptionalCellStyle());
      setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.uniqueDeviceHelp"));
      setTemplateSheetInfoColumns(templateSheet, var72);
      int configColumnOffset = var72;

      try {
         if (inputSlots.length > 0) {
            generateCell(firstRow, configColumnOffset, "", this.styleVault.getInputCellStyle());
            labelCell = generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.slotName"), this.styleVault.getInputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.slotNameInHelp"));
            generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.label"), this.styleVault.getInputCellStyle());
            labelCell = generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.bindHints"), this.styleVault.getInputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.bindHintsHelp"));
            labelCell = generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.targetSlotHints"), this.styleVault.getInputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.targetSlotHintsHelp"));
            labelCell = generateCell(sixthRow, configColumnOffset, lex.getText("templateSideBar.slotPathScope"), this.styleVault.getInputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.slotPathScopeHelp", new Object[]{"third"}));
            labelCell = generateCell(seventhRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getInputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
            templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
            CellRangeAddress inputRange = ExcelUtils.makeCellRangeAddress(0, 0, ++configColumnOffset, configColumnOffset + inputSlots.length * 3 - 1);
            templateSheet.addMergedRegion(inputRange);
            generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.inputs"), this.styleVault.getInputCellStyle());

            for (int i = 0; i < inputSlots.length * 3; i++) {
               templateSheet.setDefaultColumnStyle(configColumnOffset + i, this.styleVault.getStringCellStyle());
            }

            configColumnOffset += inputSlots.length * 3;
         }

         setInputsCount(templateSheet, inputSlots.length * 3);
         if (outputSlots.length > 0) {
            generateCell(firstRow, configColumnOffset, "", this.styleVault.getOutputCellStyle());
            labelCell = generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.slotName"), this.styleVault.getOutputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.slotNameOutHelp"));
            generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.label"), this.styleVault.getOutputCellStyle());
            labelCell = generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.bindHints"), this.styleVault.getOutputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.bindHintsHelp"));
            labelCell = generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.targetSlotHints"), this.styleVault.getOutputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.targetSlotHintsHelp"));
            labelCell = generateCell(sixthRow, configColumnOffset, lex.getText("templateSideBar.slotPathScope"), this.styleVault.getOutputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.slotPathScopeHelp", new Object[]{"third"}));
            labelCell = generateCell(seventhRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getOutputCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
            templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
            CellRangeAddress outputRange = ExcelUtils.makeCellRangeAddress(0, 0, ++configColumnOffset, configColumnOffset + outputSlots.length * 3 - 1);
            templateSheet.addMergedRegion(outputRange);
            generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.outputs"), this.styleVault.getOutputCellStyle());

            for (int i = 0; i < outputSlots.length * 3; i++) {
               templateSheet.setDefaultColumnStyle(configColumnOffset + i, this.styleVault.getStringCellStyle());
            }

            configColumnOffset += outputSlots.length * 3;
         }

         setOutputsCount(templateSheet, outputSlots.length * 3);
         if (!relations.isEmpty()) {
            generateCell(firstRow, configColumnOffset, "", this.styleVault.getRelationCellStyle());
            labelCell = generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.label"), this.styleVault.getRelationCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userTipHelp"));
            generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.relationId"), this.styleVault.getRelationCellStyle());
            labelCell = generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.relateHints"), this.styleVault.getRelationCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.relateHintsHelp"));
            generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.direction"), this.styleVault.getRelationCellStyle());
            labelCell = generateCell(sixthRow, configColumnOffset, lex.getText("templateSideBar.slotPathScope"), this.styleVault.getRelationCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.slotPathScopeHelp", new Object[]{"second"}));
            labelCell = generateCell(seventhRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getRelationCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
            templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
            configColumnOffset++;
            if (relations.size() > 1) {
               CellRangeAddress relationRange = ExcelUtils.makeCellRangeAddress(0, 0, configColumnOffset, configColumnOffset + relations.size() * 2 - 1);
               templateSheet.addMergedRegion(relationRange);
            }

            generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.relations"), this.styleVault.getRelationCellStyle());

            for (int i = 0; i < relations.size() * 2; i++) {
               templateSheet.setDefaultColumnStyle(configColumnOffset + i, this.styleVault.getStringCellStyle());
            }

            configColumnOffset += relations.size() * 2;
         }

         setRelationsCount(templateSheet, relations.size() * 2);
         List<BulkDeployUtil.BindingProperties> normalBindingList = new ArrayList<>();
         List<BulkDeployUtil.OptionalBindingProperties> optionalBindingList = new ArrayList<>();

         for (Property p : this.getOrderedConfigSlots(templateConfig)) {
            Optional<BConfigBinding> configBindingOpt = templateConfig.getConfigBinding(p);
            if (configBindingOpt.isPresent()) {
               BConfigBinding configBinding = configBindingOpt.get();
               if (isOptionalConfiguration(configBinding, optionalComponents, templateBase)) {
                  BulkDeployUtil.OptionalBindingProperties optionalBindingProps = new BulkDeployUtil.OptionalBindingProperties(
                     configBinding, optionalComponents, templateBase, templateConfig
                  );
                  optionalBindingList.add(optionalBindingProps);
               } else {
                  BulkDeployUtil.BindingProperties bindingProps = new BulkDeployUtil.BindingProperties(configBinding, templateConfig);
                  normalBindingList.add(bindingProps);
               }
            }
         }

         int configBindingsColumnCount = getConfigPropertyCount(normalBindingList);
         if (configBindingsColumnCount > 0) {
            generateCell(firstRow, configColumnOffset, "", this.styleVault.getConfigCellStyle());
            generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.slotName"), this.styleVault.getConfigCellStyle());
            generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.label"), this.styleVault.getConfigCellStyle());
            generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.slotType"), this.styleVault.getConfigCellStyle());
            generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.defaultValue"), this.styleVault.getConfigCellStyle());
            generateCell(sixthRow, configColumnOffset, "", this.styleVault.getConfigCellStyle());
            labelCell = generateCell(seventhRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getConfigCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
            templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
            configColumnOffset++;
            if (configBindingsColumnCount > 1) {
               CellRangeAddress configRange = ExcelUtils.makeCellRangeAddress(0, 0, configColumnOffset, configColumnOffset + configBindingsColumnCount - 1);
               templateSheet.addMergedRegion(configRange);
            }

            generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.configs"), this.styleVault.getConfigCellStyle());
            configColumnOffset += configBindingsColumnCount;
         }

         setConfigsCount(templateSheet, configBindingsColumnCount);
         int optionalColumnConfigCount = getConfigPropertyCount(optionalBindingList);
         if (!optionalComponents.isEmpty()) {
            generateCell(firstRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
            generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.optionalSlot"), this.styleVault.getOptionalConfigCellStyle());
            generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.optionalSlotType"), this.styleVault.getOptionalConfigCellStyle());
            generateCell(fourthRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
            generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.installOptional"), this.styleVault.getOptionalConfigCellStyle());
            generateCell(sixthRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
            labelCell = generateCell(
               seventhRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getOptionalConfigCellStyle()
            );
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
            templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
            configColumnOffset++;
            if (optionalComponents.size() > 1) {
               CellRangeAddress optionalRange = ExcelUtils.makeCellRangeAddress(0, 0, configColumnOffset, configColumnOffset + optionalComponents.size() - 1);
               templateSheet.addMergedRegion(optionalRange);
            }

            generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.optional"), this.styleVault.getOptionalConfigCellStyle());
            configColumnOffset += optionalComponents.size();
            if (optionalColumnConfigCount > 0) {
               generateCell(firstRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
               generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.slotName"), this.styleVault.getOptionalConfigCellStyle());
               generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.label"), this.styleVault.getOptionalConfigCellStyle());
               generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.slotType"), this.styleVault.getOptionalConfigCellStyle());
               generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.defaultValue"), this.styleVault.getOptionalConfigCellStyle());
               generateCell(sixthRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
               labelCell = generateCell(
                  seventhRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getOptionalConfigCellStyle()
               );
               setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
               templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
               configColumnOffset++;

               for (BOrd optionalOrd : optionalComponents) {
                  String relativeSlotPath = "";
                  int configsForOptionalComponent = 0;
                  BComponent optionalComponent = optionalOrd.resolve(templateBase).get().asComponent();

                  for (BulkDeployUtil.OptionalBindingProperties optionalBindingProp : optionalBindingList) {
                     Optional<BComponent> parentComponent = getComponentForOptionalConfig(optionalBindingProp.configBinding, optionalComponents, templateBase);
                     if (parentComponent.isPresent() && parentComponent.get() == optionalComponent) {
                        relativeSlotPath = optionalBindingProp.relativeSlotPath;
                        configsForOptionalComponent += optionalBindingProp.properties.size();
                     }
                  }

                  if (configsForOptionalComponent != 0) {
                     if (configsForOptionalComponent > 1) {
                        CellRangeAddress optionalComponentRange = ExcelUtils.makeCellRangeAddress(
                           0, 0, configColumnOffset, configColumnOffset + configsForOptionalComponent - 1
                        );
                        templateSheet.addMergedRegion(optionalComponentRange);
                     }

                     generateCell(
                        firstRow,
                        configColumnOffset,
                        lex.getText("templateSideBar.optionalConfigs", new Object[]{relativeSlotPath}),
                        this.styleVault.getOptionalConfigCellStyle()
                     );
                     configColumnOffset += configsForOptionalComponent;
                  }
               }
            }
         }

         setOptionalCount(templateSheet, optionalComponents.size());
         setOptionalConfigurationCount(templateSheet, optionalColumnConfigCount);
         int tagColumnCount = 0;
         boolean includeTags = tags != null && !tags.isEmpty();
         if (includeTags) {
            generateCell(firstRow, configColumnOffset, "", this.styleVault.getTagCellStyle());
            generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.tagId"), this.styleVault.getTagCellStyle());
            generateCell(thirdRow, configColumnOffset, "", this.styleVault.getTagCellStyle());
            generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.slotType"), this.styleVault.getTagCellStyle());
            generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.defaultValue"), this.styleVault.getTagCellStyle());
            generateCell(sixthRow, configColumnOffset, "", this.styleVault.getTagCellStyle());
            labelCell = generateCell(seventhRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getTagCellStyle());
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
            templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
            configColumnOffset++;
            if (tags.size() > 1) {
               CellRangeAddress tagRange = ExcelUtils.makeCellRangeAddress(0, 0, configColumnOffset, configColumnOffset + tags.size() - 1);
               templateSheet.addMergedRegion(tagRange);
            }

            generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.tags"), this.styleVault.getTagCellStyle());

            for (int i = 0; i < tags.size(); i++) {
               templateSheet.setDefaultColumnStyle(configColumnOffset + i, this.styleVault.getStringCellStyle());
            }

            tagColumnCount += tags.size();
         }

         setTagCount(templateSheet, tagColumnCount);
         int propertyRowPosition = var72 + (inputSlots.length > 0 ? 1 : 0);

         for (Slot input : inputSlots) {
            Tags inputTags = templateConfig.getInputSlotTags(input);
            BIDataValue userTip = BString.make(input.getName());
            BIDataValue bindHints = BString.DEFAULT;
            BIDataValue sourceSlotHint = BString.DEFAULT;
            BIDataValue slotPathScope = BString.DEFAULT;
            if (inputTags != null) {
               userTip = inputTags.get(Id.newId("n:userTip")).filter(biDataValue -> !biDataValue.toString().isEmpty()).orElse(BString.make(input.getName()));
               bindHints = (BIDataValue)inputTags.get(Id.newId("n:bindHints")).orElse(BString.DEFAULT);
               sourceSlotHint = (BIDataValue)inputTags.get(Id.newId("n:targetSlotHint")).orElse(BString.make("out"));
               if (inputTags.contains(BTemplateManager.SLOT_PATH_SCOPE)) {
                  slotPathScope = (BIDataValue)inputTags.get(BTemplateManager.SLOT_PATH_SCOPE).orElse(BString.DEFAULT);
               }
            }

            Cell propertyCell = secondRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(input.getName());
            propertyCell = thirdRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(userTip.toString());
            propertyCell = fourthRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(bindHints.toString());
            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyCell = fifthRow.createCell(++propertyRowPosition);
            propertyCell.setCellValue(sourceSlotHint.toString());
            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyCell = sixthRow.createCell(++propertyRowPosition);
            if (slotPathScope.equals(BString.DEFAULT)) {
               templateSheet.setColumnWidth(propertyCell.getColumnIndex(), 765);
            } else {
               propertyCell.setCellValue(slotPathScope.toString());
            }

            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyRowPosition++;
         }

         propertyRowPosition += outputSlots.length > 0 ? 1 : 0;

         for (Slot output : outputSlots) {
            Tags outputTags = templateConfig.getOutputSlotTags(output);
            BIDataValue userTipx = BString.make(output.getName());
            BIDataValue bindHintsx = BString.DEFAULT;
            BIDataValue targetSlotHint = BString.DEFAULT;
            BIDataValue slotPathScopex = BString.DEFAULT;
            if (outputTags != null) {
               userTipx = outputTags.get(Id.newId("n:userTip")).filter(biDataValue -> !biDataValue.toString().isEmpty()).orElse(BString.make(output.getName()));
               bindHintsx = (BIDataValue)outputTags.get(Id.newId("n:bindHints")).orElse(BString.DEFAULT);
               targetSlotHint = (BIDataValue)outputTags.get(Id.newId("n:targetSlotHint")).orElse(BString.make("in10"));
               slotPathScopex = (BIDataValue)outputTags.get(Id.newId("n:slotPathScope")).orElse(BString.DEFAULT);
               if (outputTags.contains(BTemplateManager.SLOT_PATH_SCOPE)) {
                  slotPathScopex = (BIDataValue)outputTags.get(BTemplateManager.SLOT_PATH_SCOPE).orElse(BString.DEFAULT);
               }
            }

            Cell propertyCell = secondRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(output.getName());
            propertyCell = thirdRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(userTipx.toString());
            propertyCell = fourthRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(bindHintsx.toString());
            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyCell = fifthRow.createCell(++propertyRowPosition);
            propertyCell.setCellValue(targetSlotHint.toString());
            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyCell = sixthRow.createCell(++propertyRowPosition);
            if (slotPathScopex.equals(BString.DEFAULT)) {
               templateSheet.setColumnWidth(propertyCell.getColumnIndex(), 765);
            } else {
               propertyCell.setCellValue(slotPathScopex.toString());
            }

            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyRowPosition++;
         }

         propertyRowPosition += relations.isEmpty() ? 0 : 1;

         for (BRelationInfo relation : relations) {
            Cell propertyCell = secondRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(relation.getUserTip());
            propertyCell = thirdRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(relation.getRelationId());
            propertyCell = fourthRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(relation.getRelateHints());
            propertyCell = fifthRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(relation.getInbound() ? lex.getText("templateRelationEditor.in") : lex.getText("templateRelationEditor.out"));
            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyCell = sixthRow.createCell(++propertyRowPosition);
            if (relation.getSlotPathScope().isEmpty()) {
               templateSheet.setColumnWidth(propertyCell.getColumnIndex(), 765);
            } else {
               propertyCell.setCellValue(relation.getSlotPathScope());
            }

            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyRowPosition++;
         }

         if (configBindingsColumnCount > 0) {
            propertyRowPosition++;

            for (BulkDeployUtil.BindingProperties normalBindingProps : normalBindingList) {
               Row[] rows = new Row[]{secondRow, thirdRow, fourthRow, fifthRow, sixthRow};
               propertyRowPosition += this.addConfigColumn(normalBindingProps, rows, propertyRowPosition);
            }
         }

         Map<String, Boolean> optionalHighlight = new HashMap<>();
         boolean lastHighlight = true;
         propertyRowPosition += !optionalComponents.isEmpty() ? 1 : 0;

         for (BOrd optionalOrd : optionalComponents) {
            String optionalSlot = getRelativeSlotPathForOptionalComponent(optionalOrd, templateBase);
            String optionalSlotType = optionalOrd.resolve(templateBase).get().getType().toString();
            lastHighlight = !lastHighlight;
            optionalHighlight.put(optionalSlot, lastHighlight);
            Cell propertyCell = secondRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(optionalSlot);
            this.setHighlightedCellStyle(lastHighlight, propertyCell);
            propertyCell = thirdRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(optionalSlotType);
            this.setHighlightedCellStyle(lastHighlight, propertyCell);
            propertyCell = fourthRow.createCell(propertyRowPosition);
            this.setHighlightedCellStyle(lastHighlight, propertyCell);
            propertyCell = fifthRow.createCell(propertyRowPosition);
            propertyCell.setCellValue(lex.getText("templateSideBar.true"));
            this.setHighlightedCellStyle(lastHighlight, propertyCell);
            propertyCell = sixthRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyCell = seventhRow.createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.stringCellStyle);
            propertyRowPosition++;
         }

         if (optionalColumnConfigCount > 0) {
            propertyRowPosition++;

            for (BulkDeployUtil.OptionalBindingProperties optionalBindingProps : optionalBindingList) {
               Row[] rows = new Row[]{secondRow, thirdRow, fourthRow, fifthRow, sixthRow};
               int addedPositions = this.addConfigColumn(optionalBindingProps, rows, propertyRowPosition);
               Optional<BComponent> parentComponent = getComponentForOptionalConfig(optionalBindingProps.configBinding, optionalComponents, templateBase);
               if (parentComponent.isPresent()) {
                  boolean highlight = optionalHighlight.get(optionalBindingProps.relativeSlotPath);

                  for (int i = 0; i < addedPositions; i++) {
                     int thisRowPosition = propertyRowPosition + i;

                     for (int n = 0; n < 4; n++) {
                        Cell highlightCell = rows[n].getCell(thisRowPosition);
                        this.setHighlightedCellStyle(highlight, highlightCell);
                     }

                     templateSheet.setDefaultColumnStyle(configColumnOffset + i, this.styleVault.getEmptyCellStyle());
                  }
               }

               propertyRowPosition += addedPositions;
            }
         }

         if (includeTags) {
            propertyRowPosition += tags.isEmpty() ? 0 : 1;
            AtomicInteger lambdaInt = new AtomicInteger(propertyRowPosition);
            tags.forEach(tag -> {
               Cell propertyCell = secondRow.createCell(lambdaInt.get());
               propertyCell.setCellValue(tag.getId().getQName());
               propertyCell = fourthRow.createCell(lambdaInt.get());
               propertyCell.setCellValue(tag.getValue().getType().toString());
               setCellComment(templateSheet, propertyCell, lex.getText("templateSideBar.slotTypeStringHelp"));
               propertyCell = fifthRow.createCell(lambdaInt.get());
               propertyCell.setCellValue(tag.getValue().toString(null));
               propertyCell = sixthRow.createCell(lambdaInt.get());
               propertyCell.setCellStyle(this.styleVault.stringCellStyle);
               propertyCell = seventhRow.createCell(lambdaInt.get());
               propertyCell.setCellStyle(this.styleVault.stringCellStyle);
               lambdaInt.incrementAndGet();
            });
            propertyRowPosition = lambdaInt.get();
         }

         for (int i = 0; i < propertyRowPosition; i++) {
            templateSheet.autoSizeColumn(i);
         }

         templateSheet.createFreezePane(columnFreezePane, 7);
      } catch (Exception var45) {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelExport.columnError"), (Throwable)var45);
      }
   }

   private void getApplicationTemplateConfigContents(NiagaraTemplate mainTemplate, List<NiagaraTemplate> templates, Sheet templateSheet) {
      Row firstRow = templateSheet.createRow(0);
      Row secondRow = templateSheet.createRow(1);
      Row thirdRow = templateSheet.createRow(2);
      Row fourthRow = templateSheet.createRow(3);
      Row fifthRow = templateSheet.createRow(4);
      Row sixthRow = templateSheet.createRow(5);
      Row[] headerRows = new Row[]{secondRow, thirdRow, fourthRow, fifthRow, sixthRow};
      List<Row> templateRows = new ArrayList<>();

      for (int i = 0; i < templates.size(); i++) {
         templateRows.add(templateSheet.createRow(i + 6));
      }

      String description = mainTemplate.getDescription();
      String info = mainTemplate.getInfo();
      generateCell(firstRow, 0, lex.getText("templateSideBar.description"), this.styleVault.getInfoCellStyle());
      Cell labelCell = generateCell(firstRow, 1, description, this.styleVault.getStringCellStyle());
      if (!info.isEmpty()) {
         setCellComment(templateSheet, labelCell, info);
      }

      for (int i = 0; i < 2; i++) {
         templateSheet.setDefaultColumnStyle(i, this.styleVault.getStringCellStyle());
      }

      generateCell(thirdRow, 0, lex.getText("excel.label.templateTitle"), this.styleVault.getInfoCellStyle());
      generateCell(thirdRow, 1, mainTemplate.getTitle(), this.styleVault.getStringCellStyle());
      generateCell(fourthRow, 0, lex.getText("excel.label.templateVersion"), this.styleVault.getInfoCellStyle());
      generateCell(fourthRow, 1, mainTemplate.getVersion(), this.styleVault.getStringCellStyle());
      labelCell = generateCell(sixthRow, 0, lex.getText("templateSideBar.rowName"), this.styleVault.getInstanceCellStyle());
      setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.rowNameHelp"));
      int uniqueDeviceColumn = 1;
      int columnFreezePane = 2;

      for (int i = 0; i < templates.size(); i++) {
         NiagaraTemplate template = templates.get(i);
         Cell nameCell = templateRows.get(i).createCell(0);
         nameCell.setCellValue(template.getBaseName() + '-' + template.getTitle() + '-' + template.getVersion());
         nameCell.setCellStyle(this.styleVault.getStringCellStyle());
      }

      labelCell = generateCell(sixthRow, uniqueDeviceColumn, lex.getText("templateSideBar.uniqueDevice"), this.styleVault.getOptionalCellStyle());
      setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.uniqueDeviceHelp"));

      for (int i = 0; i < templates.size(); i++) {
         Cell uniqueIdCell = templateRows.get(i).createCell(1);
         uniqueIdCell.setCellValue(templates.get(i).getBaseName());
      }

      setTemplateSheetInfoColumns(templateSheet, 2);
      int configColumnOffset = 2;
      setInputsCount(templateSheet, 0);
      setOutputsCount(templateSheet, 0);
      setRelationsCount(templateSheet, 0);
      int configBindingsColumnCount = mainTemplate.requiredPropertyElements().size();
      if (configBindingsColumnCount > 0) {
         generateCell(firstRow, configColumnOffset, "", this.styleVault.getConfigCellStyle());
         generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.slotName"), this.styleVault.getConfigCellStyle());
         generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.label"), this.styleVault.getConfigCellStyle());
         generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.slotType"), this.styleVault.getConfigCellStyle());
         generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.defaultValue"), this.styleVault.getConfigCellStyle());
         labelCell = generateCell(sixthRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getConfigCellStyle());
         setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
         templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
         configColumnOffset++;
         if (configBindingsColumnCount > 1) {
            CellRangeAddress configRange = ExcelUtils.makeCellRangeAddress(0, 0, configColumnOffset, configColumnOffset + configBindingsColumnCount - 1);
            templateSheet.addMergedRegion(configRange);
         }

         generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.configs"), this.styleVault.getConfigCellStyle());
         configColumnOffset += configBindingsColumnCount;
      }

      setConfigsCount(templateSheet, configBindingsColumnCount);
      int optionalColumnConfigCount = 0;
      List<OptionalComponent> optionalComponents = mainTemplate.optionalComponents();

      for (OptionalComponent optionalComponent : optionalComponents) {
         optionalColumnConfigCount += optionalComponent.propertyElements().size();
      }

      if (!optionalComponents.isEmpty()) {
         generateCell(firstRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
         generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.optionalSlot"), this.styleVault.getOptionalConfigCellStyle());
         generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.optionalSlotType"), this.styleVault.getOptionalConfigCellStyle());
         generateCell(fourthRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
         generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.installOptional"), this.styleVault.getOptionalConfigCellStyle());
         labelCell = generateCell(sixthRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getOptionalConfigCellStyle());
         setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
         templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
         configColumnOffset++;
         if (optionalComponents.size() > 1) {
            CellRangeAddress optionalRange = ExcelUtils.makeCellRangeAddress(0, 0, configColumnOffset, configColumnOffset + optionalComponents.size() - 1);
            templateSheet.addMergedRegion(optionalRange);
         }

         generateCell(firstRow, configColumnOffset, lex.getText("templateSideBar.optional"), this.styleVault.getOptionalConfigCellStyle());
         configColumnOffset += optionalComponents.size();
         if (optionalColumnConfigCount > 0) {
            generateCell(firstRow, configColumnOffset, "", this.styleVault.getOptionalConfigCellStyle());
            generateCell(secondRow, configColumnOffset, lex.getText("templateSideBar.slotName"), this.styleVault.getOptionalConfigCellStyle());
            generateCell(thirdRow, configColumnOffset, lex.getText("templateSideBar.label"), this.styleVault.getOptionalConfigCellStyle());
            generateCell(fourthRow, configColumnOffset, lex.getText("templateSideBar.slotType"), this.styleVault.getOptionalConfigCellStyle());
            generateCell(fifthRow, configColumnOffset, lex.getText("templateSideBar.defaultValue"), this.styleVault.getOptionalConfigCellStyle());
            labelCell = generateCell(
               sixthRow, configColumnOffset, lex.getText("templateSideBar.userDescriptions"), this.styleVault.getOptionalConfigCellStyle()
            );
            setCellComment(templateSheet, labelCell, lex.getText("templateSideBar.userDescriptionsHelp"));
            templateSheet.setDefaultColumnStyle(configColumnOffset, this.styleVault.getEmptyCellStyle());
            configColumnOffset++;

            for (OptionalComponent optionalComponent : optionalComponents) {
               if (!optionalComponent.propertyElements().isEmpty()) {
                  if (optionalComponent.propertyElements().size() > 1) {
                     CellRangeAddress optionalComponentRange = ExcelUtils.makeCellRangeAddress(
                        0, 0, configColumnOffset, configColumnOffset + optionalComponent.propertyElements().size() - 1
                     );
                     templateSheet.addMergedRegion(optionalComponentRange);
                  }

                  generateCell(
                     firstRow,
                     configColumnOffset,
                     lex.getText("templateSideBar.optionalConfigs", new Object[]{getOptionalComponentSlot(optionalComponent.getPath())}),
                     this.styleVault.getOptionalConfigCellStyle()
                  );
                  configColumnOffset += optionalComponent.propertyElements().size();
               }
            }
         }
      }

      setOptionalCount(templateSheet, optionalComponents.size());
      setOptionalConfigurationCount(templateSheet, optionalColumnConfigCount);
      setTagCount(templateSheet, 0);
      int propertyRowPosition = 2;
      if (configBindingsColumnCount > 0) {
         propertyRowPosition = this.setValuesForElements(
            mainTemplate.requiredPropertyElements(), templates, headerRows, templateRows, ++propertyRowPosition, false
         );
      }

      propertyRowPosition += optionalComponents.isEmpty() ? 0 : 1;
      boolean highlight = true;

      for (OptionalComponent optionalComponentx : optionalComponents) {
         String optionalPath = optionalComponentx.getPath();
         String optionalSlot = getOptionalComponentSlot(optionalPath);
         String optionalSlotType = optionalComponentx.getNType();
         Cell propertyCell = secondRow.createCell(propertyRowPosition);
         propertyCell.setCellValue(optionalSlot);
         this.setHighlightedCellStyle(highlight, propertyCell);
         propertyCell = thirdRow.createCell(propertyRowPosition);
         propertyCell.setCellValue(optionalSlotType);
         this.setHighlightedCellStyle(highlight, propertyCell);
         propertyCell = fourthRow.createCell(propertyRowPosition);
         this.setHighlightedCellStyle(highlight, propertyCell);
         propertyCell = fifthRow.createCell(propertyRowPosition);
         propertyCell.setCellValue(lex.getText("templateSideBar.true"));
         this.setHighlightedCellStyle(highlight, propertyCell);
         propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
         propertyCell = sixthRow.createCell(propertyRowPosition);
         propertyCell.setCellStyle(this.styleVault.stringCellStyle);

         for (int templateIdx = 0; templateIdx < templates.size(); templateIdx++) {
            if (!templates.get(templateIdx).hasComponent(optionalComponentx.getPath())) {
               propertyCell = templateRows.get(templateIdx).createCell(propertyRowPosition);
               propertyCell.setCellValue(lex.getText("templateSideBar.false"));
               propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            }
         }

         propertyRowPosition++;
         highlight = !highlight;
      }

      if (optionalColumnConfigCount > 0) {
         propertyRowPosition++;
         highlight = true;

         for (OptionalComponent optionalComponentx : optionalComponents) {
            propertyRowPosition = this.setValuesForElements(
               optionalComponentx.propertyElements(), templates, headerRows, templateRows, propertyRowPosition, highlight
            );
            highlight = !highlight;
         }
      }

      for (int i = 0; i < propertyRowPosition; i++) {
         templateSheet.autoSizeColumn(i);
      }

      templateSheet.createFreezePane(columnFreezePane, 6);
   }

   private int setValuesForElements(
      List<TemplateElement> mainElements,
      List<NiagaraTemplate> templates,
      Row[] headerRows,
      List<Row> templateRows,
      int startingColumnNumber,
      boolean highlight
   ) {
      int columnNumber = startingColumnNumber;

      for (TemplateElement mainElement : mainElements) {
         List<TemplateElement> elementsInColumn = new ArrayList<>();

         for (NiagaraTemplate template : templates) {
            TemplateProperty secondaryProperty = template.getProperty(mainElement.property().getName());
            TemplateElement secondaryElement = secondaryProperty == null ? null : secondaryProperty.getElement(mainElement.getName());
            if (secondaryElement != null
               && !secondaryElement.presentValue().isMissing()
               && !secondaryElement.presentValue().isEqualValue(mainElement.defaultValue())) {
               elementsInColumn.add(secondaryElement);
            } else {
               elementsInColumn.add(null);
            }
         }

         this.setCellValueAndFormatColumn(headerRows, templateRows, mainElement, elementsInColumn, columnNumber);

         for (int n = 0; n < 3; n++) {
            Cell highlightCell = headerRows[n].getCell(columnNumber);
            this.setHighlightedCellStyle(highlight, highlightCell);
         }

         columnNumber++;
      }

      return columnNumber;
   }

   private void setHighlightedCellStyle(boolean isHighlighted, Cell cell) {
      if (isHighlighted) {
         cell.setCellStyle(this.styleVault.getHighlightCellStyle());
      }
   }

   private static String getOptionalComponentSlot(String optionalComponentPath) {
      return optionalComponentPath.startsWith("Drivers/") ? optionalComponentPath.substring("Drivers/".length()) : optionalComponentPath;
   }

   private int addConfigColumn(BulkDeployUtil.BindingProperties configBindingProps, Row[] rows, int propertyRowPosition) throws IOException {
      String userTip = configBindingProps.configBinding.getUserTip();

      for (BulkDeployUtil.BindingProperty entry : configBindingProps.properties) {
         this.setCellValueAndFormatColumn(rows, entry.property, entry.value, propertyRowPosition, entry.name, userTip);
         propertyRowPosition++;
      }

      return configBindingProps.properties.size();
   }

   private static String getRelativeSlotPathForOptionalComponent(BOrd optionalOrd, BComponent templateBase) {
      BComponent optionalComponent = optionalOrd.resolve(templateBase).get().asComponent();
      return getRelativeSlotPathForOptionalComponent(optionalComponent, templateBase);
   }

   private static String getRelativeSlotPathForOptionalComponent(BComponent optionalComponent, BComponent templateBase) {
      BDriverContainer[] containers = (BDriverContainer[])CompUtil.getDescendants(templateBase, BDriverContainer.class);
      Optional<SlotPath> relativeSlotPath = CompUtil.slotPathFromAncestor(containers[0], optionalComponent);
      return relativeSlotPath.isPresent() ? relativeSlotPath.get().toDisplayString() : optionalComponent.getSlotPathOrd().toString();
   }

   private static Optional<BComponent> getComponentForOptionalConfig(BConfigBinding configBinding, List<BOrd> optionalComponents, BComponent templateBase) {
      BObject configTargetObject = configBinding.getTargetOrd().resolve(templateBase).get();

      for (BOrd optionalOrd : optionalComponents) {
         BComponent optionalComponent = optionalOrd.resolve(templateBase).get().asComponent();
         if (configTargetObject instanceof BComplex) {
            Optional<SlotPath> slotPath = CompUtil.slotPathFromAncestor(optionalComponent, (BComplex)configTargetObject);
            if (slotPath.isPresent()) {
               return Optional.of(optionalComponent);
            }
         } else {
            BComplex parentObject = configBinding.getTargetOrd().resolve(templateBase).getParent();
            Optional<SlotPath> slotPath = CompUtil.slotPathFromAncestor(optionalComponent, parentObject);
            if (slotPath.isPresent()) {
               return Optional.of(optionalComponent);
            }
         }
      }

      return Optional.empty();
   }

   private static boolean isOptionalConfiguration(BConfigBinding configBinding, List<BOrd> optionalComponents, BComponent templateBase) {
      BObject configTargetObject = configBinding.getTargetOrd().resolve(templateBase).get();

      for (BOrd optionalOrd : optionalComponents) {
         BComponent optionalComponent = optionalOrd.resolve(templateBase).get().asComponent();
         if (configTargetObject instanceof BComplex) {
            Optional<SlotPath> slotPath = CompUtil.slotPathFromAncestor(optionalComponent, (BComplex)configTargetObject);
            if (slotPath.isPresent()) {
               return true;
            }
         } else {
            BComplex parentObject = configBinding.getTargetOrd().resolve(templateBase).getParent();
            Optional<SlotPath> slotPath = CompUtil.slotPathFromAncestor(optionalComponent, parentObject);
            if (slotPath.isPresent()) {
               return true;
            }
         }
      }

      return false;
   }

   private static int getConfigPropertyCount(List<? extends BulkDeployUtil.BindingProperties> bindingList) {
      int propertyCount = 0;

      for (BulkDeployUtil.BindingProperties bindingProperties : bindingList) {
         propertyCount += bindingProperties.properties.size();
      }

      return propertyCount;
   }

   public static BHistoryExt[] unconfigurableHistoryExtensions(BComponent templateBase) {
      BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(templateBase);
      return unconfigurableHistoryExtensions(templateBase, templateConfig);
   }

   private static BHistoryExt[] unconfigurableHistoryExtensions(BComponent templateBase, BTemplateConfig templateConfig) {
      BHistoryExt[] allHistoryExtensions = (BHistoryExt[])CompUtil.getDescendants(templateBase, BHistoryExt.class);
      Set<BHistoryExt> unconfigurableHistoryExtensions = null;
      BConfigBinding[] configBindings = (BConfigBinding[])templateConfig.getChildren(BConfigBinding.class);

      for (BConfigBinding configBinding : configBindings) {
         if (Objects.equals(configBinding.getTargetSlot(), BHistoryExt.enabled.getName())) {
            BObject target = configBinding.getTargetOrd().resolve(templateConfig).get();
            if (target instanceof BHistoryExt) {
               BHistoryExt targetHistoryExt = (BHistoryExt)target;
               if (unconfigurableHistoryExtensions == null) {
                  unconfigurableHistoryExtensions = new HashSet<>(Arrays.asList(allHistoryExtensions));
               }

               unconfigurableHistoryExtensions.remove(targetHistoryExt);
            }
         }
      }

      return unconfigurableHistoryExtensions == null ? allHistoryExtensions : unconfigurableHistoryExtensions.toArray(HISTORY_EXTS);
   }

   private static Cell generateCell(Row row, int column, String value, CellStyle style) {
      Cell newCell = row.createCell(column);
      if (value.isEmpty()) {
         newCell.setBlank();
      } else {
         newCell.setCellValue(value);
      }

      if (style != null) {
         newCell.setCellStyle(style);
      }

      return newCell;
   }

   private void setCellValueAndFormatColumn(
      Row[] rows, Property configProperty, BValue configPropertyValue, int propertyRowPosition, String slotName, String userTip
   ) throws IOException {
      Sheet sheet = rows[0].getSheet();
      Cell propertyCell = rows[0].createCell(propertyRowPosition);
      String cellValue = SlotPath.unescape(slotName);
      propertyCell.setCellValue(cellValue);
      propertyCell = rows[1].createCell(propertyRowPosition);
      propertyCell.setCellValue(userTip);
      Cell slotTypeCell = rows[2].createCell(propertyRowPosition);
      slotTypeCell.setCellValue(configProperty.getType().toString());
      if (configProperty.getType().is(BStatusValue.TYPE)) {
         if (configProperty.getType().is(BStatusNumeric.TYPE)) {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getDataCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellValue(((BStatusNumeric)configPropertyValue).getValue());
            propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeNumberHelp"));
         } else if (configProperty.getType().is(BStatusBoolean.TYPE)) {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getStringCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellValue(((BStatusValue)configPropertyValue).valueToString(null));
            propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeBooleanHelp"));
         } else if (configProperty.getType().is(BStatusEnum.TYPE)) {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getStringCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellValue(((BStatusValue)configPropertyValue).valueToString(null));
            propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            String commentText = getEnumCellComment(((BIEnum)configPropertyValue).getEnum());
            setCellComment(sheet, slotTypeCell, commentText);
         } else {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getStringCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellValue(((BStatusValue)configPropertyValue).valueToString(null));
            propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeStringHelp"));
         }
      } else if (configProperty.getType().is(BSimple.TYPE)) {
         if (configProperty.getType().is(BNumber.TYPE)) {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getDataCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellValue(((BNumber)configPropertyValue).getDouble());
            propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
            if (!configProperty.getType().is(BInteger.TYPE) && !configProperty.getType().is(BLong.TYPE)) {
               setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeNumberHelp"));
            } else {
               setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeIntHelp"));
            }
         } else if (configProperty.getType().is(BBoolean.TYPE)) {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getStringCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellValue(((BIEncodable)configPropertyValue).encodeToString());
            propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeBooleanHelp"));
         } else if (configProperty.getType().is(BEnum.TYPE)) {
            if (configProperty.getType().is(BDynamicEnum.TYPE)) {
               sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getDataCellStyle());
               propertyCell = rows[3].createCell(propertyRowPosition);
               propertyCell.setCellValue(((BEnum)configPropertyValue).getTag());
               propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
            } else {
               sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getStringCellStyle());
               propertyCell = rows[3].createCell(propertyRowPosition);
               propertyCell.setCellValue(((BIEncodable)configPropertyValue).encodeToString());
               propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            }

            String commentText = getEnumCellComment((BEnum)configPropertyValue);
            setCellComment(sheet, slotTypeCell, commentText);
         } else if (configProperty.getType().is(BPassword.TYPE)) {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getPasswordCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellStyle(this.styleVault.getPasswordCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeStringHelp"));
         } else {
            sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getStringCellStyle());
            propertyCell = rows[3].createCell(propertyRowPosition);
            propertyCell.setCellValue(((BIEncodable)configPropertyValue).encodeToString());
            propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeStringHelp"));
         }
      } else {
         sheet.setDefaultColumnStyle(propertyRowPosition, this.styleVault.getStringCellStyle());
         propertyCell = rows[3].createCell(propertyRowPosition);
         propertyCell.setCellValue(configPropertyValue.toString());
         propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
         setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeStringHelp"));
      }

      propertyCell = rows[4].createCell(propertyRowPosition);
      propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
   }

   private void setCellValueAndFormatColumn(
      Row[] rows, List<Row> templateRows, TemplateElement mainElement, List<TemplateElement> elementsInColumn, int columnNumber
   ) {
      Sheet sheet = rows[0].getSheet();
      Cell propertyCell = rows[0].createCell(columnNumber);
      String cellValue = SlotPath.unescape(mainElement.getFullName());
      propertyCell.setCellValue(cellValue);
      propertyCell = rows[1].createCell(columnNumber);
      propertyCell.setCellValue(mainElement.property().getUserTip());
      Cell slotTypeCell = rows[2].createCell(columnNumber);
      TemplateValue presentValue = mainElement.presentValue();
      TemplateValue defaultValue = mainElement.defaultValue();
      slotTypeCell.setCellValue(presentValue.getNType());
      switch (presentValue.getType()) {
         case NUMERIC:
            sheet.setDefaultColumnStyle(columnNumber, this.styleVault.getDataCellStyle());
            propertyCell = rows[3].createCell(columnNumber);
            if (!defaultValue.isMissing()) {
               propertyCell.setCellValue(defaultValue.getNumericValue());
            }

            propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeNumberHelp"));

            for (int ixxxx = 0; ixxxx < templateRows.size(); ixxxx++) {
               if (elementsInColumn.get(ixxxx) != null) {
                  propertyCell = templateRows.get(ixxxx).createCell(columnNumber);
                  propertyCell.setCellValue(elementsInColumn.get(ixxxx).presentValue().getNumericValue());
                  propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
               }
            }
            break;
         case INTEGER:
            sheet.setDefaultColumnStyle(columnNumber, this.styleVault.getDataCellStyle());
            propertyCell = rows[3].createCell(columnNumber);
            if (!defaultValue.isMissing()) {
               propertyCell.setCellValue(defaultValue.getNumericValue());
            }

            propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeIntHelp"));

            for (int ixxx = 0; ixxx < templateRows.size(); ixxx++) {
               if (elementsInColumn.get(ixxx) != null) {
                  propertyCell = templateRows.get(ixxx).createCell(columnNumber);
                  propertyCell.setCellValue(elementsInColumn.get(ixxx).presentValue().getNumericValue());
                  propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
               }
            }
            break;
         case BOOLEAN:
            sheet.setDefaultColumnStyle(columnNumber, this.styleVault.getStringCellStyle());
            propertyCell = rows[3].createCell(columnNumber);
            if (!defaultValue.isMissing()) {
               propertyCell.setCellValue(BBoolean.toString(defaultValue.getBooleanValue(), null));
            }

            propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeBooleanHelp"));

            for (int ixx = 0; ixx < templateRows.size(); ixx++) {
               if (elementsInColumn.get(ixx) != null) {
                  propertyCell = templateRows.get(ixx).createCell(columnNumber);
                  propertyCell.setCellValue(BBoolean.toString(elementsInColumn.get(ixx).presentValue().getBooleanValue(), null));
                  propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
               }
            }
            break;
         case ENUM:
            Map<Integer, String> enumValues = presentValue.getDefinedEnumValues();
            if (enumValues.isEmpty()) {
               sheet.setDefaultColumnStyle(columnNumber, this.styleVault.getDataCellStyle());
            } else {
               sheet.setDefaultColumnStyle(columnNumber, this.styleVault.getStringCellStyle());
            }

            propertyCell = rows[3].createCell(columnNumber);
            if (!defaultValue.isMissing()) {
               int enumValue = (int)defaultValue.getIntegerValue();
               if (enumValues.containsKey(enumValue)) {
                  propertyCell.setCellValue(enumValues.get(enumValue));
               } else {
                  propertyCell.setCellValue(enumValue);
               }
            }

            if (enumValues.isEmpty()) {
               propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
            } else {
               propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            }

            String commentText = getEnumCellComment(enumValues);
            setCellComment(sheet, slotTypeCell, commentText);

            for (int ix = 0; ix < templateRows.size(); ix++) {
               if (elementsInColumn.get(ix) != null) {
                  propertyCell = templateRows.get(ix).createCell(columnNumber);
                  int enumValue = (int)elementsInColumn.get(ix).presentValue().getIntegerValue();
                  if (enumValues.containsKey(enumValue)) {
                     propertyCell.setCellValue(enumValues.get(enumValue));
                  } else {
                     propertyCell.setCellValue(enumValue);
                  }

                  if (enumValues.isEmpty()) {
                     propertyCell.setCellStyle(this.styleVault.getDataCellStyle());
                  } else {
                     propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
                  }
               }
            }
            break;
         case PASSWORD:
            sheet.setDefaultColumnStyle(columnNumber, this.styleVault.getPasswordCellStyle());
            propertyCell = rows[3].createCell(columnNumber);
            propertyCell.setCellStyle(this.styleVault.getPasswordCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeStringHelp"));
            break;
         case STRING:
         default:
            sheet.setDefaultColumnStyle(columnNumber, this.styleVault.getStringCellStyle());
            propertyCell = rows[3].createCell(columnNumber);
            if (!defaultValue.isMissing()) {
               propertyCell.setCellValue(defaultValue.getStringValue());
            }

            propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
            setCellComment(sheet, slotTypeCell, lex.getText("templateSideBar.slotTypeStringHelp"));

            for (int i = 0; i < templateRows.size(); i++) {
               if (elementsInColumn.get(i) != null) {
                  propertyCell = templateRows.get(i).createCell(columnNumber);
                  propertyCell.setCellValue(elementsInColumn.get(i).presentValue().getStringValue());
                  propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
               }
            }
      }

      propertyCell = rows[4].createCell(columnNumber);
      propertyCell.setCellStyle(this.styleVault.getStringCellStyle());
   }

   private static String getEnumCellComment(BEnum tags) {
      BEnumRange range = tags.getRange();
      if (range.isNull()) {
         return lex.getText("templateSideBar.slotTypeEnumEmptyHelp");
      } else {
         StringBuilder sb = new StringBuilder(lex.getText("templateSideBar.slotTypeEnumHelp")).append('\n');
         int currentLineLength = 0;
         boolean first = true;

         for (int i : range.getOrdinals()) {
            if (!first) {
               sb.append(", ");
            }

            first = false;
            if (currentLineLength > 60) {
               sb.append('\n');
               currentLineLength = 0;
            }

            String tag = range.getTag(i);
            sb.append(tag);
            currentLineLength += tag.length() + 2;
         }

         return sb.toString();
      }
   }

   private static String getEnumCellComment(Map<Integer, String> enumValues) {
      if (enumValues.isEmpty()) {
         return lex.getText("templateSideBar.slotTypeEnumEmptyHelp");
      } else {
         StringBuilder sb = new StringBuilder(lex.getText("templateSideBar.slotTypeEnumHelp")).append('\n');
         int currentLineLength = 0;
         boolean first = true;

         for (String enumValue : enumValues.values()) {
            if (!first) {
               sb.append(", ");
            }

            first = false;
            if (currentLineLength > 60) {
               sb.append('\n');
               currentLineLength = 0;
            }

            sb.append(enumValue);
            currentLineLength += enumValue.length() + 2;
         }

         return sb.toString();
      }
   }

   private static void setCellComment(Sheet sheet, Cell cell, String text) {
      CreationHelper factory = sheet.getWorkbook().getCreationHelper();
      ClientAnchor anchor = factory.createClientAnchor();
      anchor.setCol1(cell.getColumnIndex());
      anchor.setCol2(cell.getColumnIndex() + 5);
      anchor.setRow1(cell.getRow().getRowNum());
      anchor.setRow2(cell.getRow().getRowNum() + 5);
      Drawing drawing = sheet.createDrawingPatriarch();
      Comment comment = drawing.createCellComment(anchor);
      comment.setAuthor("Niagara Template Exporter");
      RichTextString rts = factory.createRichTextString(text);
      comment.setString(rts);
      cell.setCellComment(comment);
   }

   public static Map<String, String> getTemplateTitlesFromExcel(BulkDeployWorkbook workbook) {
      List<BulkDeployUtil.DeployedWorksheet> deployedWorksheets = loadDeployedWorksheets(workbook);
      Map<String, String> templateTitles = new HashMap<>();
      if (deployedWorksheets == null || deployedWorksheets.isEmpty()) {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.importFileError", new Object[]{workbook.getName()}));
      }

      for (BulkDeployUtil.DeployedWorksheet deployedWorksheet : deployedWorksheets) {
         for (BulkDeployUtil.DeployedRoot deployedRoot : deployedWorksheet.deployedRoots) {
            templateTitles.put(deployedWorksheet.title, deployedRoot.deployName);
         }
      }

      return templateTitles;
   }

   public void deployTemplatesFromExcel(BulkDeployWorkbook bulkDeployWorkbook, BComponent root, TemplateDeployWorker worker) {
      List<BulkDeployUtil.DeployedWorksheet> deployedWorksheets = loadDeployedWorksheets(bulkDeployWorkbook);
      if (deployedWorksheets != null && !deployedWorksheets.isEmpty()) {
         int templateInstanceCount = deployedWorksheets.stream().mapToInt(deployedWorksheetx -> deployedWorksheetx.deployedRoots.size()).sum();
         int processedInstances = 0;
         int totalProgress = 0;
         HashMap<BComponent, BNameMap> displayNames = new HashMap<>();

         for (BulkDeployUtil.DeployedWorksheet deployedWorksheet : deployedWorksheets) {
            for (BulkDeployUtil.DeployedRoot deployedRoot : deployedWorksheet.deployedRoots) {
               if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                  log.log(
                     IMPORT_LOG_LEVEL,
                     "Copying type "
                        + deployedWorksheet.templateType
                        + " template "
                        + deployedWorksheet.title
                        + " version "
                        + deployedWorksheet.version
                        + " parent component "
                        + deployedRoot.parentComponentSlotPath
                        + " to target component"
                        + deployedRoot.deployName
                  );
               }

               String deployResponses = deployedWorksheet.templateType;
               BComponent deployedRootComponent;
               switch (deployResponses) {
                  case "Application":
                     deployedRootComponent = root;
                     break;
                  case "Device":
                  case "Component":
                  default:
                     deployedRootComponent = this.resolveDeployComponent(deployedRoot, root);
               }

               if (deployedRootComponent == null) {
                  log.log(
                     Level.WARNING, lex.getText("bulkDeploy.excelImport.rootNotResolvedError", new Object[]{deployedRoot.deployName, deployedWorksheet.title})
                  );
               } else {
                  deployedRootComponent.lease();
                  BINavNode templateObject = deployedRootComponent.getNavChild(deployedRoot.deployName);
                  if (templateObject != null) {
                     log.log(
                        Level.WARNING,
                        lex.getText(
                           "bulkDeploy.excelImport.templateComponentExistsError", new Object[]{deployedRoot.deployName, deployedRootComponent.getNavName()}
                        )
                     );
                  } else {
                     BNtplFile ntplFile = this.copyTemplateToStation(deployedWorksheet, deployedRootComponent);
                     if (ntplFile == null) {
                        log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.templateFileCopyError", new Object[]{deployedWorksheet.title}));
                     } else {
                        HashMap<String, BFormat> componentDisplayNames = new HashMap<>();
                        BComponent deployedTemplateComponent = installTemplateToStation(
                           deployedWorksheet, deployedRoot, deployedRootComponent, ntplFile, componentDisplayNames
                        );
                        if (deployedTemplateComponent == null) {
                           log.log(
                              Level.WARNING,
                              lex.getText(
                                 "bulkDeploy.excelImport.templateNotResolvedError", new Object[]{deployedRoot.parentComponentSlotPath, deployedWorksheet.title}
                              )
                           );
                        } else {
                           deployedRoot.deployedTemplate = deployedTemplateComponent;
                           if (!componentDisplayNames.isEmpty()) {
                              BNameMap nameMap = BNameMap.make(componentDisplayNames);
                              if (displayNames.containsKey(deployedRootComponent)) {
                                 BNameMap mergedNameMap = BNameMap.make(displayNames.get(deployedRootComponent), nameMap);
                                 displayNames.put(deployedRootComponent, mergedNameMap);
                              } else {
                                 displayNames.put(deployedRootComponent, nameMap);
                              }
                           }

                           if (worker != null) {
                              processedInstances++;
                              int currentProgress = (int)(processedInstances * 70.0 / templateInstanceCount);
                              if (currentProgress > totalProgress) {
                                 totalProgress = currentProgress;
                                 String progressMessage = lex.getText(
                                    "bulkDeploy.progress.update.deploy", new Object[]{deployedWorksheet.title, deployedRoot.deployName}
                                 );
                                 worker.updateProgress(currentProgress, progressMessage);
                              }

                              if (!worker.isRunning()) {
                                 if (worker.isCanceled()) {
                                    log.log(Level.INFO, lex.getText("bulkDeploy.progress.cancel", new Object[]{bulkDeployWorkbook.getName()}));
                                 } else {
                                    log.log(Level.WARNING, lex.getText("bulkDeploy.progress.cancelInternal", new Object[]{bulkDeployWorkbook.getName()}));
                                 }

                                 return;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (!displayNames.isEmpty()) {
            displayNames.forEach((key, value) -> {
               log.log(Level.FINER, String.format("Component: %s, Display name: %s", key.getName(), value.encodeToString()));
               this.updateTemplateDisplayNames(key, value);
            });
         }

         Map<String, List<String>> accumulatedResponses = new HashMap<>();

         for (BulkDeployUtil.DeployedWorksheet deployedWorksheet : deployedWorksheets) {
            for (BulkDeployUtil.DeployedRoot deployedRoot : deployedWorksheet.deployedRoots) {
               List<String> deployResponses = this.applyConfigurations(deployedWorksheet, deployedRoot);
               if (!deployResponses.isEmpty()) {
                  String key = deployedRoot.parentComponentSlotPath + '/' + deployedRoot.deployName;
                  accumulatedResponses.put(key, deployResponses);
               }

               if (worker != null) {
                  processedInstances++;
                  int currentProgressx = (int)(processedInstances * 30.0 / templateInstanceCount) + 70;
                  if (currentProgressx > totalProgress) {
                     totalProgress = currentProgressx;
                     String progressMessage = lex.getText("bulkDeploy.progress.update.config", new Object[]{deployedWorksheet.title, deployedRoot.deployName});
                     worker.updateProgress(currentProgressx, progressMessage);
                  }

                  if (!worker.isRunning()) {
                     if (worker.isCanceled()) {
                        log.log(Level.INFO, lex.getText("bulkDeploy.progress.cancel", new Object[]{bulkDeployWorkbook.getName()}));
                     } else {
                        log.log(Level.WARNING, lex.getText("bulkDeploy.progress.cancelInternal", new Object[]{bulkDeployWorkbook.getName()}));
                     }

                     return;
                  }
               }
            }
         }

         if (!accumulatedResponses.isEmpty()) {
            worker.setDeployMessages(accumulatedResponses);
         }
      } else {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.importFileError", new Object[]{bulkDeployWorkbook.getName()}));
      }
   }

   public void updateTemplateDisplayNames(BComponent deployRootComponent, BNameMap templateDisplayNames) {
      BNameMap existingDisplayNames = (BNameMap)deployRootComponent.get("displayNames");
      if (existingDisplayNames == null) {
         deployRootComponent.add("displayNames", templateDisplayNames);
      } else if (existingDisplayNames.isNull()) {
         deployRootComponent.remove("displayNames");
         deployRootComponent.add("displayNames", templateDisplayNames);
      } else {
         BNameMap mergedNameMap = BNameMap.make(existingDisplayNames, templateDisplayNames);
         deployRootComponent.remove("displayNames");
         deployRootComponent.add("displayNames", mergedNameMap);
      }
   }

   private static boolean supportInputSlot(BulkDeployUtil.DeployedWorksheet worksheet) {
      Version inputSlotVersion = new Version("1.1");
      Version templateVersion = new Version(worksheet.templateExportVersion);
      return templateVersion.compareTo(inputSlotVersion) >= 0;
   }

   private static boolean supportSlotPathScope(BulkDeployWorkbook bulkDeployWorkbook) {
      Version sourceSlotVersion = new Version("1.3");
      Version templateVersion = new Version(bulkDeployWorkbook.getTemplateExportVersion());
      return templateVersion.compareTo(sourceSlotVersion) >= 0;
   }

   private static int getExcelHeaderRows(Workbook wb, String templateType) {
      if (templateType == null) {
         return 6;
      } else if ("Application".contentEquals(templateType)) {
         return 6;
      } else {
         Version templateVersion = new Version(getTemplateExportVersion(wb));
         Version sourceSlotVersion = new Version("1.3");
         return templateVersion.compareTo(sourceSlotVersion) >= 0 ? 7 : 6;
      }
   }

   public static List<BulkDeployUtil.DeployedWorksheet> loadDeployedWorksheets(BulkDeployWorkbook bulkDeployWorkbook) {
      List<BulkDeployUtil.DeployedWorksheet> deployedWorksheets = new ArrayList<>();
      if (bulkDeployWorkbook != null && bulkDeployWorkbook.isValid()) {
         Workbook wb = bulkDeployWorkbook.getWorkbook();
         String templateExportVersion = getTemplateExportVersion(wb);
         if (templateExportVersion == null) {
            log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.importFileInvalid"));
            return deployedWorksheets;
         } else {
            if (!"1.3".equals(templateExportVersion)) {
               log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.importFileVersionMismatch"), new Object[]{templateExportVersion, "1.3"});
            }

            boolean isSlotPathScopeSupported = supportSlotPathScope(bulkDeployWorkbook);
            Iterator<Sheet> sheetIterator = wb.sheetIterator();

            while (sheetIterator.hasNext()) {
               Sheet sheet = sheetIterator.next();
               int lastRowNum = sheet.getLastRowNum();
               int realLastRow = lastRowNum + 1;
               String templateType = getTemplateTypeName(sheet);
               int headerRowCount = getExcelHeaderRows(wb, templateType);
               if (realLastRow > headerRowCount && !sheet.getSheetName().startsWith("#")) {
                  String templateFile = getTemplateFile(sheet);
                  String vendor = getTemplateVendor(sheet);
                  String title = getTemplateTitle(sheet);
                  String version = getTemplateVersion(sheet);
                  BUuid uid = BUuid.make(getTemplateUID(sheet));
                  int excelInstanceInfoColumns = getTemplateSheetInfoColumns(sheet);
                  if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                     log.log(IMPORT_LOG_LEVEL, "Deploying template " + title + " version " + version);
                  }

                  int[] configCounts = new int[]{0, 0, 0, 0, 0, 0, 0};
                  loadConfigItemCounts(sheet, configCounts);
                  if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                     log.log(
                        IMPORT_LOG_LEVEL,
                        () -> String.format(
                           "Inputs = %d, Outputs = %d, Relations = %d, Configs = %d, Optionals = %d, OptionalConfigs = %d, Tags = %d",
                           configCounts[0],
                           configCounts[1],
                           configCounts[2],
                           configCounts[3],
                           configCounts[4],
                           configCounts[5],
                           configCounts[6]
                        )
                     );
                  }

                  Row secondRow = sheet.getRow(1);
                  Row thirdRow = sheet.getRow(2);
                  Row fourthRow = sheet.getRow(3);
                  Row fifthRow = sheet.getRow(4);
                  Row sixthRow = null;
                  if (isSlotPathScopeSupported) {
                     sixthRow = sheet.getRow(5);
                  }

                  BulkDeployUtil.DeployedWorksheet newWorksheet = new BulkDeployUtil.DeployedWorksheet();
                  newWorksheet.title = title;
                  newWorksheet.vendor = vendor;
                  newWorksheet.version = version;
                  newWorksheet.uid = uid;
                  newWorksheet.templateType = templateType;
                  newWorksheet.templateFile = templateFile;
                  newWorksheet.templateExportVersion = templateExportVersion;
                  newWorksheet.isSlotPathScopeSupported = isSlotPathScopeSupported;
                  int columnOffset = excelInstanceInfoColumns;
                  if (configCounts[0] > 0) {
                     boolean isInputSlotSupported = supportInputSlot(newWorksheet);
                     int columnsPerInput = 1;
                     if (isInputSlotSupported) {
                        columnsPerInput = 2;
                     }

                     if (isSlotPathScopeSupported) {
                        columnsPerInput = 3;
                     }

                     int inputCount = configCounts[0] / columnsPerInput;
                     columnOffset = excelInstanceInfoColumns + 1;

                     for (int n = 0; n < inputCount; n++) {
                        String[] inputs = new String[]{
                           getCell(secondRow, columnOffset), getCell(thirdRow, columnOffset), getCell(fourthRow, columnOffset), null, null, null
                        };
                        if (isInputSlotSupported) {
                           inputs[3] = getCell(fifthRow, ++columnOffset);
                        }

                        if (sixthRow != null) {
                           inputs[4] = getCell(sixthRow, ++columnOffset);
                        }

                        newWorksheet.inputDefs.add(inputs);
                        columnOffset++;
                     }
                  }

                  if (configCounts[1] > 0) {
                     int columnsPerOutput = isSlotPathScopeSupported ? 3 : 2;
                     int outputCount = configCounts[1] / columnsPerOutput;
                     columnOffset++;

                     for (int n = 0; n < outputCount; n++) {
                        String[] outputs = new String[]{
                           getCell(secondRow, columnOffset),
                           getCell(thirdRow, columnOffset),
                           getCell(fourthRow, columnOffset),
                           getCell(fifthRow, ++columnOffset),
                           null,
                           null
                        };
                        if (sixthRow != null) {
                           outputs[4] = getCell(sixthRow, ++columnOffset);
                        }

                        newWorksheet.outputDefs.add(outputs);
                        columnOffset++;
                     }
                  }

                  if (configCounts[2] > 0) {
                     int columnsPerRelation = isSlotPathScopeSupported ? 2 : 1;
                     int relationCount = configCounts[2] / columnsPerRelation;
                     columnOffset++;

                     for (int n = 0; n < relationCount; n++) {
                        String[] relations = new String[]{
                           getCell(secondRow, columnOffset),
                           getCell(thirdRow, columnOffset),
                           getCell(fourthRow, columnOffset),
                           getCell(fifthRow, columnOffset),
                           null,
                           null
                        };
                        if (sixthRow != null) {
                           relations[4] = getCell(sixthRow, ++columnOffset);
                        }

                        newWorksheet.relationDefs.add(relations);
                        columnOffset++;
                     }
                  }

                  if (configCounts[3] > 0) {
                     columnOffset++;

                     for (int n = 0; n < configCounts[3]; n++) {
                        ArrayList<String> configs = new ArrayList<>();
                        configs.add(getCell(secondRow, columnOffset));
                        configs.add(getCell(thirdRow, columnOffset));
                        String valueType = getCell(fourthRow, columnOffset);
                        configs.add(valueType);
                        configs.add(getCellOrNull(fifthRow, columnOffset, isStringType(valueType)));
                        newWorksheet.configDefs.add(configs.toArray(STRINGS));
                        columnOffset++;
                     }
                  }

                  if (configCounts[4] > 0) {
                     columnOffset++;

                     for (int n = 0; n < configCounts[4]; n++) {
                        ArrayList<String> optionals = new ArrayList<>();
                        optionals.add(getCell(secondRow, columnOffset));
                        optionals.add(getCell(thirdRow, columnOffset));
                        optionals.add(getCell(fifthRow, columnOffset));
                        newWorksheet.optionalDefs.add(optionals.toArray(STRINGS));
                        columnOffset++;
                     }
                  }

                  if (configCounts[5] > 0) {
                     columnOffset++;

                     for (int n = 0; n < configCounts[5]; n++) {
                        ArrayList<String> optionalConfigs = new ArrayList<>();
                        optionalConfigs.add(getCell(secondRow, columnOffset));
                        optionalConfigs.add(getCell(thirdRow, columnOffset));
                        String valueType = getCell(fourthRow, columnOffset);
                        optionalConfigs.add(valueType);
                        optionalConfigs.add(getCellOrNull(fifthRow, columnOffset, isStringType(valueType)));
                        newWorksheet.optionalConfigDefs.add(optionalConfigs.toArray(STRINGS));
                        columnOffset++;
                     }
                  }

                  if (configCounts[6] > 0) {
                     columnOffset++;

                     for (int n = 0; n < configCounts[6]; n++) {
                        ArrayList<String> tags = new ArrayList<>();
                        tags.add(getCell(secondRow, columnOffset));
                        tags.add(getCell(fourthRow, columnOffset));
                        tags.add(getCell(fifthRow, columnOffset));
                        newWorksheet.tagDefs.add(tags.toArray(STRINGS));
                        columnOffset++;
                     }
                  }

                  int i = headerRowCount;

                  while (i < realLastRow) {
                     Row primaryRow = sheet.getRow(i++);
                     if (!isCommentRow(primaryRow) && !isSecondaryRow(primaryRow)) {
                        List<Row> rowList = new ArrayList<>();
                        rowList.add(primaryRow);

                        for (Row nextRow = sheet.getRow(i); isSecondaryRow(nextRow); nextRow = sheet.getRow(++i)) {
                           rowList.add(nextRow);
                        }

                        String rootComponentName = getCell(rowList.get(0), 0);
                        String displayName = "";
                        String position = "";
                        String deviceTarget = "";
                        boolean enableHistories = false;
                        String deployName;
                        if ("Application".contentEquals(templateType)) {
                           if (rootComponentName == null || rootComponentName.isEmpty()) {
                              log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.stationEmptyError", new Object[]{title}));
                              continue;
                           }

                           deployName = rootComponentName;
                           if (excelInstanceInfoColumns > 1) {
                              StringBuilder deviceTargetStr = new StringBuilder();

                              for (Row row : rowList) {
                                 String uniqueDevice = getCell(row, 1);
                                 if (uniqueDevice.isEmpty()) {
                                    log.log(Level.INFO, lex.getText("bulkDeploy.excelImport.targetDeviceNotFound", new Object[]{deployName}));
                                 }

                                 deviceTargetStr.append(uniqueDevice).append(',');
                              }

                              deviceTarget = deviceTargetStr.toString();
                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(IMPORT_LOG_LEVEL, "Device target = " + deviceTarget);
                              }
                           }
                        } else {
                           deployName = getCell(rowList.get(0), 1);
                           if (deployName == null || deployName.isEmpty()) {
                              log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.deployEmptyError", new Object[]{title}));
                              continue;
                           }

                           if (excelInstanceInfoColumns > 2) {
                              displayName = getCell(rowList.get(0), 2);
                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(IMPORT_LOG_LEVEL, "Display name = " + displayName);
                              }
                           }

                           if (excelInstanceInfoColumns > 3) {
                              position = getCell(rowList.get(0), 3);
                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(IMPORT_LOG_LEVEL, "Position = " + position);
                              }
                           }

                           if (excelInstanceInfoColumns > 4) {
                              StringBuilder deviceTargetStr = new StringBuilder();

                              for (Row row : rowList) {
                                 String uniqueDevice = getCell(row, 4);
                                 if (uniqueDevice.isEmpty()) {
                                    log.log(Level.INFO, lex.getText("bulkDeploy.excelImport.targetDeviceNotFound", new Object[]{deployName}));
                                 }

                                 deviceTargetStr.append(uniqueDevice).append(',');
                              }

                              deviceTarget = deviceTargetStr.toString();
                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(IMPORT_LOG_LEVEL, "Device target = " + deviceTarget);
                              }
                           }

                           if (excelInstanceInfoColumns > 5) {
                              enableHistories = getCellAsBoolean(rowList.get(0), 5);
                           }
                        }

                        BulkDeployUtil.DeployedRoot newRoot = new BulkDeployUtil.DeployedRoot();
                        newRoot.parentComponentSlotPath = rootComponentName == null ? "" : rootComponentName;
                        newRoot.deployName = deployName;
                        newRoot.displayName = displayName;
                        newRoot.position = position;
                        newRoot.deviceTarget = new ArrayList<>(rowList.size());
                        newRoot.isSlotPathScopeSupported = isSlotPathScopeSupported;

                        for (String deviceTargetValue : deviceTarget.split(",")) {
                           if (!deviceTargetValue.isEmpty()) {
                              newRoot.deviceTarget.add(deviceTargetValue.trim());
                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(IMPORT_LOG_LEVEL, "Device target = " + deviceTargetValue);
                              }
                           }
                        }

                        newRoot.enableHistories = enableHistories;
                        columnOffset = excelInstanceInfoColumns;
                        if (configCounts[0] > 0) {
                           boolean isInputSlotSupportedx = supportInputSlot(newWorksheet);
                           int columnsPerInputx = 1;
                           if (isInputSlotSupportedx) {
                              columnsPerInputx = 2;
                           }

                           if (isSlotPathScopeSupported) {
                              columnsPerInputx = 3;
                           }

                           int inputCount = configCounts[0] / columnsPerInputx;
                           newRoot.inputs = new ArrayList<>();
                           columnOffset = excelInstanceInfoColumns + 1;

                           for (int n = 0; n < inputCount; n++) {
                              BulkDeployUtil.DeployedIOR input = new BulkDeployUtil.DeployedIOR();
                              String[] inputDef = newWorksheet.inputDefs.get(n);
                              if (inputDef != null && inputDef.length != 0) {
                                 String inputName = getCell(rowList.get(0), columnOffset);
                                 input.name = inputName;
                                 String inputSlotName = "out";
                                 String slotPathScope = "";
                                 if (isInputSlotSupportedx) {
                                    inputSlotName = getCell(rowList.get(0), ++columnOffset);
                                    if (inputSlotName == null || inputSlotName.isEmpty()) {
                                       inputSlotName = inputDef[3];
                                       if (inputSlotName.isEmpty()) {
                                          inputSlotName = "out";
                                       }
                                    }

                                    input.slot = inputSlotName;
                                    if (isSlotPathScopeSupported) {
                                       slotPathScope = getCell(rowList.get(0), ++columnOffset);
                                       if (slotPathScope == null || slotPathScope.isEmpty()) {
                                          slotPathScope = inputDef[4];
                                       }
                                    }

                                    input.scope = slotPathScope;
                                 }

                                 newRoot.inputs.add(input);
                                 if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                    log.log(
                                       IMPORT_LOG_LEVEL,
                                       "Input name " + inputName + " slot " + inputSlotName + (isSlotPathScopeSupported ? " scope " + slotPathScope : "")
                                    );
                                 }

                                 columnOffset++;
                              } else {
                                 log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.inputDefinitionError", new Object[]{deployName}));
                              }
                           }
                        }

                        if (configCounts[1] > 0) {
                           int columnsPerOutput = isSlotPathScopeSupported ? 3 : 2;
                           int outputCount = configCounts[1] / columnsPerOutput;
                           newRoot.outputs = new HashMap<>();
                           columnOffset++;

                           for (int outputIndex = 0; outputIndex < outputCount; outputIndex++) {
                              String[] outputDef = newWorksheet.outputDefs.get(outputIndex);
                              if (outputDef != null && outputDef.length != 0) {
                                 List<BulkDeployUtil.DeployedIOR> namedOutputs = new ArrayList<>();
                                 newRoot.outputs.put(outputDef[0], namedOutputs);

                                 for (Row row : rowList) {
                                    BulkDeployUtil.DeployedIOR output = new BulkDeployUtil.DeployedIOR();
                                    String outputName = getCell(row, columnOffset);
                                    if (outputName.isEmpty()) {
                                       log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.outputComponentNotFound", new Object[]{deployName}));
                                    } else {
                                       output.name = outputName;
                                       String outputSlotName = getCell(row, columnOffset + 1);
                                       if (outputSlotName == null || outputSlotName.isEmpty()) {
                                          outputSlotName = outputDef[3];
                                       }

                                       output.slot = outputSlotName;
                                       String slotPathScopex = "";
                                       if (isSlotPathScopeSupported) {
                                          slotPathScopex = getCell(row, columnOffset + 2);
                                          if (slotPathScopex == null || slotPathScopex.isEmpty()) {
                                             slotPathScopex = outputDef[4];
                                          }
                                       }

                                       output.scope = slotPathScopex;
                                       namedOutputs.add(output);
                                       if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                          log.log(
                                             IMPORT_LOG_LEVEL,
                                             "Output name "
                                                + outputName
                                                + " slot "
                                                + outputSlotName
                                                + (isSlotPathScopeSupported ? " scope " + slotPathScopex : "")
                                          );
                                       }
                                    }
                                 }

                                 columnOffset += columnsPerOutput;
                              } else {
                                 log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.outputDefinitionError", new Object[]{deployName}));
                              }
                           }
                        }

                        if (configCounts[2] > 0) {
                           int columnsPerRelation = isSlotPathScopeSupported ? 2 : 1;
                           int relationCount = configCounts[2] / columnsPerRelation;
                           newRoot.relations = new HashMap<>();
                           columnOffset++;

                           for (int relationIndex = 0; relationIndex < relationCount; relationIndex++) {
                              String[] relationDef = newWorksheet.relationDefs.get(relationIndex);
                              if (relationDef != null && relationDef.length != 0) {
                                 String relationId = relationDef[1];
                                 String direction = relationDef[3];
                                 List<BulkDeployUtil.DeployedIOR> namedRelations = new ArrayList<>();
                                 newRoot.relations.put(relationId + direction, namedRelations);

                                 for (Row rowx : rowList) {
                                    BulkDeployUtil.DeployedIOR relation = new BulkDeployUtil.DeployedIOR();
                                    String relationName = getCell(rowx, columnOffset);
                                    if (relationName.isEmpty()) {
                                       log.log(
                                          Level.WARNING, lex.getText("bulkDeploy.excelImport.relationComponentNotFound", new Object[]{deployName, direction})
                                       );
                                    } else {
                                       relation.name = relationName;
                                       String slotPathScopexx = "";
                                       if (isSlotPathScopeSupported) {
                                          slotPathScopexx = getCell(rowx, columnOffset + 1);
                                          if (slotPathScopexx == null || slotPathScopexx.isEmpty()) {
                                             slotPathScopexx = relationDef[4];
                                          }
                                       }

                                       relation.scope = slotPathScopexx;
                                       namedRelations.add(relation);
                                       if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                          log.log(
                                             IMPORT_LOG_LEVEL,
                                             "Relation id "
                                                + relationId
                                                + " direction "
                                                + direction
                                                + " name list "
                                                + relationName
                                                + (isSlotPathScopeSupported ? " scope " + slotPathScopexx : "")
                                          );
                                       }
                                    }
                                 }

                                 columnOffset += columnsPerRelation;
                              } else {
                                 log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.relationDefinitionError", new Object[]{deployName}));
                              }
                           }
                        }

                        if (configCounts[3] > 0) {
                           newRoot.configs = new String[configCounts[3]];
                           columnOffset++;

                           for (int nx = 0; nx < configCounts[3]; nx++) {
                              boolean stringType = false;
                              String[] configDef = newWorksheet.configDefs.get(nx);
                              if (configDef != null && configDef.length != 0) {
                                 if (configDef.length >= 2) {
                                    stringType = isStringType(configDef[2]);
                                 }

                                 String configValue = getCellOrNull(rowList.get(0), columnOffset, stringType);
                                 newRoot.configs[nx] = configValue;
                                 columnOffset++;
                              } else {
                                 log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.configDefinitionError", new Object[]{deployName}));
                              }
                           }
                        }

                        if (configCounts[4] > 0) {
                           newRoot.optionals = new String[configCounts[4]];
                           columnOffset++;

                           for (int nxx = 0; nxx < configCounts[4]; nxx++) {
                              String[] optionalDef = newWorksheet.optionalDefs.get(nxx);
                              if (optionalDef != null && optionalDef.length != 0) {
                                 String optionalName = getCellOrNull(rowList.get(0), columnOffset, true);
                                 if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                    log.log(IMPORT_LOG_LEVEL, "Optional value " + optionalName);
                                 }

                                 newRoot.optionals[nxx] = optionalName;
                                 columnOffset++;
                              } else {
                                 log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.optionalsDefinitionError", new Object[]{deployName}));
                              }
                           }
                        }

                        if (configCounts[5] > 0) {
                           newRoot.optionalConfigs = new String[configCounts[5]];
                           columnOffset++;

                           for (int nxxx = 0; nxxx < configCounts[5]; nxxx++) {
                              boolean stringType = false;
                              String[] optionalConfigDef = newWorksheet.optionalConfigDefs.get(nxxx);
                              if (optionalConfigDef != null && optionalConfigDef.length != 0) {
                                 if (optionalConfigDef.length >= 2) {
                                    Type t = Sys.getType(optionalConfigDef[2]);
                                    stringType = t.is(BString.TYPE)
                                       || t.is(BStatusString.TYPE)
                                       || t.is(BEnum.TYPE)
                                       || t.is(BStatusEnum.TYPE)
                                       || t.is(BFrozenEnum.TYPE);
                                 }

                                 String optionalConfigValue = getCellOrNull(rowList.get(0), columnOffset, stringType);
                                 newRoot.optionalConfigs[nxxx] = optionalConfigValue;
                                 columnOffset++;
                              } else {
                                 log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.optionalsConfigDefinitionError", new Object[]{deployName}));
                              }
                           }
                        }

                        if (configCounts[6] > 0) {
                           newRoot.tags = new String[configCounts[6]];
                           columnOffset++;

                           for (int nxxxx = 0; nxxxx < configCounts[6]; nxxxx++) {
                              String[] tagDef = newWorksheet.tagDefs.get(nxxxx);
                              if (tagDef != null && tagDef.length != 0) {
                                 boolean stringType = true;
                                 String configValue = getCellOrNull(rowList.get(0), columnOffset, stringType);
                                 newRoot.tags[nxxxx] = configValue;
                                 columnOffset++;
                              } else {
                                 log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.tagDefinitionError", new Object[]{deployName}));
                              }
                           }
                        }

                        if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                           log.log(
                              IMPORT_LOG_LEVEL,
                              () -> String.format(
                                 "Root = %s, Deploy Name = %s, Device Target = %s, Enable Histories = %s",
                                 newRoot.parentComponentSlotPath,
                                 newRoot.deployName,
                                 newRoot.deviceTarget,
                                 newRoot.enableHistories
                              )
                           );
                        }

                        newWorksheet.deployedRoots.add(newRoot);
                     } else if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                        log.log(IMPORT_LOG_LEVEL, "Skipping blank, comment, or untethered secondary row #" + i);
                     }
                  }

                  deployedWorksheets.add(newWorksheet);
               }
            }

            return deployedWorksheets;
         }
      } else {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.importFileUndefined"));
         return deployedWorksheets;
      }
   }

   private static boolean isCommentRow(Row row) {
      if (row == null) {
         return true;
      } else {
         boolean isBlank = true;
         boolean isComment = false;
         Iterator<Cell> it = row.cellIterator();

         while (isBlank && it.hasNext()) {
            Cell cell = it.next();
            switch (cell.getCellType()) {
               case BLANK:
                  break;
               case STRING:
                  String value = cell.getStringCellValue().trim();
                  isBlank = value.isEmpty();
                  isComment = cell.getColumnIndex() < 2 && value.startsWith("#");
                  break;
               default:
                  isBlank = false;
            }
         }

         return isBlank || isComment;
      }
   }

   private static boolean isSecondaryRow(Row row) {
      return !isCommentRow(row) && getCell(row, 0).isEmpty() && getCell(row, 1).isEmpty();
   }

   private static boolean isStringType(String valueType) {
      Type t = Sys.getType(valueType);
      return t.is(BString.TYPE) || t.is(BStatusString.TYPE) || t.is(BEnum.TYPE) || t.is(BStatusEnum.TYPE) || t.is(BFrozenEnum.TYPE);
   }

   private static void setTemplateExportVersion(Workbook workbook) {
      addWorkbookConstant(workbook, "version", "1.3");
   }

   private static void setKeepPrivateFlag(Workbook workbook) {
      addWorkbookConstant(workbook, "keepPrivate", BBoolean.TRUE.toString());
   }

   private static void setTemplateFile(Sheet sheet, String templateFile) {
      addSheetConstant(sheet, "templateFile", templateFile);
   }

   private static void setTemplateVendor(Sheet sheet, String templateVendor) {
      addSheetConstant(sheet, "templateVendor", templateVendor);
   }

   private static void setTemplateTitle(Sheet sheet, String templateTitle) {
      addSheetConstant(sheet, "templateTitle", templateTitle);
   }

   private static void setTemplateVersion(Sheet sheet, String templateVersion) {
      addSheetConstant(sheet, "templateVersion", templateVersion);
   }

   private static void setTemplateUID(Sheet sheet, String templateUID) throws IOException {
      addSheetConstant(sheet, "templateUID", templateUID);
   }

   private static void setTemplateTypeName(Sheet sheet, String templateTypeName) {
      addSheetConstant(sheet, "templateType", templateTypeName);
   }

   private static void setTemplateSheetInfoColumns(Sheet sheet, int infoColumns) {
      addSheetConstant(sheet, "infoColumns", Integer.toString(infoColumns));
   }

   private static void setInputsCount(Sheet sheet, int count) {
      addSheetConstant(sheet, "inputsCount", Integer.toString(count));
   }

   private static void setOutputsCount(Sheet sheet, int count) {
      addSheetConstant(sheet, "outputsCount", Integer.toString(count));
   }

   private static void setRelationsCount(Sheet sheet, int count) {
      addSheetConstant(sheet, "relationsCount", Integer.toString(count));
   }

   private static void setConfigsCount(Sheet sheet, int count) {
      addSheetConstant(sheet, "configsCount", Integer.toString(count));
   }

   private static void setOptionalCount(Sheet sheet, int count) {
      addSheetConstant(sheet, "optionalsCount", Integer.toString(count));
   }

   private static void setOptionalConfigurationCount(Sheet sheet, int count) {
      addSheetConstant(sheet, "optionalConfigsCount", Integer.toString(count));
   }

   private static void setTagCount(Sheet sheet, int count) {
      addSheetConstant(sheet, "tagCount", Integer.toString(count));
   }

   private String buildTemplateTypeName(TemplateManifest manifest) {
      if (manifest.isApplication) {
         return "Application";
      } else {
         TemplateManager tm = TemplateManager.INSTANCE;
         TemplateInfo templateInfo = tm.getTemplate(manifest.uID, manifest.vendor);
         return templateInfo.getRootType().is(BDevice.TYPE) ? "Device" : "Component";
      }
   }

   private static void addWorkbookConstant(Workbook workbook, String constantName, String constantValue) {
      addSheetOrWorkbookConstant(workbook, -1, constantName, constantValue);
   }

   private static void addSheetConstant(Sheet sheet, String constantName, String constantValue) {
      Workbook workbook = sheet.getWorkbook();
      int sheetIndex = workbook.getSheetIndex(sheet.getSheetName());
      addSheetOrWorkbookConstant(workbook, sheetIndex, constantName, constantValue);
   }

   private static void addSheetOrWorkbookConstant(Workbook workbook, int sheetIndex, String constantName, String constantValue) {
      Name name = workbook.createName();
      name.setSheetIndex(sheetIndex);
      name.setNameName(constantName);
      name.setRefersToFormula('"' + constantValue + '"');
   }

   private static String getTemplateExportVersion(Workbook workbook) {
      return getWorkbookConstant(workbook, "version");
   }

   private static String getTemplateFile(Sheet sheet) {
      return getSheetConstant(sheet, "templateFile");
   }

   private static String getTemplateVendor(Sheet sheet) {
      return getSheetConstant(sheet, "templateVendor");
   }

   private static String getTemplateTitle(Sheet sheet) {
      return getSheetConstant(sheet, "templateTitle");
   }

   private static String getTemplateVersion(Sheet sheet) {
      return getSheetConstant(sheet, "templateVersion");
   }

   private static String getTemplateUID(Sheet sheet) {
      return getSheetConstant(sheet, "templateUID");
   }

   private static String getTemplateTypeName(Sheet sheet) {
      return getSheetConstant(sheet, "templateType");
   }

   private static int getTemplateSheetInfoColumns(Sheet sheet) {
      String result = getSheetConstant(sheet, "infoColumns");
      return result == null ? 5 : Integer.parseInt(result);
   }

   private static void loadConfigItemCounts(Sheet sheet, int[] configCounts) {
      String inputsCount = getSheetConstant(sheet, "inputsCount");
      String outputsCount = getSheetConstant(sheet, "outputsCount");
      String relationsCount = getSheetConstant(sheet, "relationsCount");
      String configsCount = getSheetConstant(sheet, "configsCount");
      String optionalsCount = getSheetConstant(sheet, "optionalsCount");
      String optionalConfigsCount = getSheetConstant(sheet, "optionalConfigsCount");
      String tagCount = getSheetConstant(sheet, "tagCount");
      if (inputsCount != null) {
         configCounts[0] = Integer.parseInt(inputsCount);
      }

      if (outputsCount != null) {
         configCounts[1] = Integer.parseInt(outputsCount);
      }

      if (relationsCount != null) {
         configCounts[2] = Integer.parseInt(relationsCount);
      }

      if (configsCount != null) {
         configCounts[3] = Integer.parseInt(configsCount);
      }

      if (optionalsCount != null) {
         configCounts[4] = Integer.parseInt(optionalsCount);
      }

      if (optionalConfigsCount != null) {
         configCounts[5] = Integer.parseInt(optionalConfigsCount);
      }

      if (tagCount != null) {
         configCounts[6] = Integer.parseInt(tagCount);
      }
   }

   private static String getWorkbookConstant(Workbook workbook, String name) {
      return getSheetOrWorkbookConstant(workbook, -1, name);
   }

   private static String getSheetConstant(Sheet sheet, String name) {
      Workbook workbook = sheet.getWorkbook();
      int sheetIndex = workbook.getSheetIndex(sheet.getSheetName());
      return getSheetOrWorkbookConstant(workbook, sheetIndex, name);
   }

   private static String getSheetOrWorkbookConstant(Workbook workbook, int sheetIndex, String name) {
      String value = null;
      Name constantName = getNamedRange(workbook, sheetIndex, name);
      if (constantName != null) {
         String formula = constantName.getRefersToFormula();
         if (formula.startsWith("\"") && formula.endsWith("\"") && formula.length() >= 2) {
            value = formula.substring(1, formula.length() - 1);
         }
      }

      return value;
   }

   private static Name getNamedRange(Workbook workbook, int sheetIndex, String nameName) {
      List<? extends Name> names = workbook.getNames(nameName);
      Name foundName = null;

      for (Name name : names) {
         if (name.getSheetIndex() == sheetIndex) {
            foundName = name;
            break;
         }
      }

      return foundName;
   }

   protected BComponent resolveDeployComponent(BulkDeployUtil.DeployedRoot deployedRoot, BComponent root) {
      String escapedParentComponentSlotPath = escapeSlotPathNames(deployedRoot.parentComponentSlotPath);
      BOrd deployRootOrd = BOrd.make(root.getSlotPathOrd() + escapedParentComponentSlotPath);
      if (log.isLoggable(IMPORT_LOG_LEVEL)) {
         log.log(IMPORT_LOG_LEVEL, "deployRootOrd = " + deployRootOrd);
      }

      try {
         deployRootOrd.resolve(root);
      } catch (UnresolvedException var13) {
         SlotPath deployRootSlotPath = new SlotPath("slot", escapedParentComponentSlotPath);
         String[] slotPathElements = deployRootSlotPath.getNames();
         BComponent nextComponent = root;

         for (String element : slotPathElements) {
            nextComponent = this.getNextChild(nextComponent, element);
            if (nextComponent == null) {
               log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.rootTargetError", new Object[]{deployedRoot.parentComponentSlotPath}));
               return null;
            }
         }
      }

      BComponent deployRootComponent = deployRootOrd.resolve(root).getComponent();
      if (deployRootComponent == null) {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.rootTargetError", new Object[]{deployedRoot.parentComponentSlotPath}));
         return null;
      } else if (!deployRootComponent.isMounted()) {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.notMountedError", new Object[]{deployedRoot.parentComponentSlotPath}));
         return null;
      } else {
         return deployRootComponent;
      }
   }

   protected BComponent getNextChild(BComponent parentComponent, String childName) {
      BValue childValue = parentComponent.get(childName);
      if (childValue == null) {
         log.log(Level.FINE, "Creating missing slot path node " + childName);
         Type newFolderType = BFolder.TYPE;
         if (parentComponent instanceof BIPointFolder) {
            newFolderType = ((BIPointFolder)parentComponent).getPointFolderType();
         } else if (parentComponent instanceof BIDeviceFolder) {
            newFolderType = ((BIDeviceFolder)parentComponent).getDeviceFolderType();
         } else if (parentComponent instanceof BIArchiveFolder) {
            newFolderType = ((BIArchiveFolder)parentComponent).getArchiveFolderType();
         }

         BComponent nextChild = newFolderType.getInstance().asComponent();
         parentComponent.add(childName, nextChild);
         return parentComponent.get(childName).asComponent();
      } else {
         return childValue.asComponent();
      }
   }

   private BWbDeployableNtplFile getTemplFileFromWorksheet(BulkDeployUtil.DeployedWorksheet worksheet) {
      TemplateManager tmInstance = new TemplateManager();
      tmInstance.initTemplateMap();
      if (log.isLoggable(IMPORT_LOG_LEVEL)) {
         log.log(IMPORT_LOG_LEVEL, "Locate template file for UID" + worksheet.uid + " and vendor " + worksheet.vendor);
      }

      TemplateInfo deployTemplateInfo = tmInstance.getTemplate(worksheet.uid, worksheet.vendor);
      if (deployTemplateInfo == null) {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.templateFileNotFound", new Object[]{worksheet.title}));
         return null;
      } else {
         BNtplFile ntplFile = deployTemplateInfo.getNtplFile();
         return ntplFile instanceof BWbDeployableNtplFile ? (BWbDeployableNtplFile)ntplFile : BWbDeployableNtplFile.make(ntplFile);
      }
   }

   protected BNtplFile copyTemplateToStation(BulkDeployUtil.DeployedWorksheet worksheet, BComponent deployRootComponent) {
      BWbDeployableNtplFile deployableNtplFile = this.getTemplFileFromWorksheet(worksheet);
      return UpdateUtil.updateNtplFile(deployableNtplFile, deployRootComponent) ? deployableNtplFile : null;
   }

   protected boolean updateNtplFile(BWbDeployableNtplFile ntplFile, BComponent target) {
      return UpdateUtil.updateNtplFile(ntplFile, target);
   }

   protected static BComponent installTemplateToStation(
      BulkDeployUtil.DeployedWorksheet worksheet,
      BulkDeployUtil.DeployedRoot deployedRoot,
      BComponent deployRootComponent,
      BNtplFile ntplFile,
      Map<String, BFormat> componentDisplayNames
   ) {
      try {
         if (!(ntplFile instanceof BWbDeployableNtplFile)) {
            log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.templateFileNotFound", new Object[]{worksheet.title}));
            return null;
         } else {
            String escapedName = SlotPath.escape(deployedRoot.deployName);
            BComponent params = new BComponent();
            params.add("exact", BBoolean.TRUE);
            Mark mark = new Mark(ntplFile, escapedName);
            mark.copyTo(deployRootComponent, params, DeployToComp.NoPostLink);
            BComponent deployedTemplateComponent = deployRootComponent.get(escapedName).asComponent();
            if (deployedTemplateComponent == null) {
               log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.deploymentError", new Object[]{worksheet.title, deployedRoot.deployName}));
               return null;
            } else {
               setComponentPosition(deployedTemplateComponent, deployedRoot.position);
               if (deployedRoot.enableHistories) {
                  BHistoryExt[] unconfigurableHistories = unconfigurableHistoryExtensions(deployedTemplateComponent);

                  for (BHistoryExt historyExt : unconfigurableHistories) {
                     historyExt.setEnabled(true);
                  }
               }

               if (deployedRoot.displayName != null && !deployedRoot.displayName.isEmpty()) {
                  try {
                     componentDisplayNames.put(deployRootComponent.getProperty(escapedName).getName(), BFormat.make(deployedRoot.displayName));
                  } catch (Exception var14) {
                     log.log(
                        Level.WARNING,
                        lex.getText("bulkDeploy.excelImport.displayNameError", new Object[]{deployedRoot.displayName, deployedRoot.deployName}),
                        (Throwable)var14
                     );
                  }
               }

               return deployedTemplateComponent;
            }
         }
      } catch (Exception var15) {
         log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.postDeployError", new Object[]{worksheet.title, deployedRoot.deployName}), (Throwable)var15);
         return null;
      }
   }

   public List<String> applyConfigurations(BulkDeployUtil.DeployedWorksheet deployedWorksheet, BulkDeployUtil.DeployedRoot deployedRoot) {
      return this.setConfigurationsFromWorksheet(deployedWorksheet, deployedRoot, false);
   }

   public List<String> updateConfigurations(BulkDeployUtil.DeployedWorksheet deployedWorksheet, BulkDeployUtil.DeployedRoot deployedRoot) {
      return this.setConfigurationsFromWorksheet(deployedWorksheet, deployedRoot, true);
   }

   private List<String> setConfigurationsFromWorksheet(
      BulkDeployUtil.DeployedWorksheet deployedWorksheet, BulkDeployUtil.DeployedRoot deployedRoot, boolean whenNoValueKeepCurrent
   ) {
      List<String> responseMessages = new ArrayList<>();
      if (deployedWorksheet != null && deployedRoot != null) {
         log.log(IMPORT_LOG_LEVEL, "Bulk deploy inputs/outputs/relations/configs");
         BComponent deployedTemplateComponent = deployedRoot.deployedTemplate;
         if (deployedTemplateComponent == null) {
            return responseMessages;
         } else {
            BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(deployedTemplateComponent);
            if (templateConfig == null) {
               log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.invalidTemplateError", new Object[]{deployedTemplateComponent.getName()}));
               return responseMessages;
            } else {
               if (deployedRoot.inputs != null && !deployedRoot.inputs.isEmpty()) {
                  for (int n = 0; n < deployedRoot.inputs.size(); n++) {
                     String[] inputDef = deployedWorksheet.inputDefs.get(n);
                     if (inputDef != null && inputDef.length != 0) {
                        String slotName = inputDef[0];
                        String bindHints = inputDef[2];
                        String slotPathScope = "";
                        if (deployedRoot.isSlotPathScopeSupported) {
                           slotPathScope = inputDef[4];
                        }

                        Slot inputSlot = deployedTemplateComponent.getSlot(slotName);
                        if (inputSlot == null) {
                           log.log(
                              Level.WARNING,
                              lex.getText("bulkDeploy.excelImport.missingInputSlotError", new Object[]{deployedTemplateComponent.getName(), slotName})
                           );
                        } else {
                           BulkDeployUtil.DeployedIOR input = deployedRoot.inputs.get(n);
                           if (input.scope != null && !input.scope.isEmpty()) {
                              slotPathScope = input.scope;
                           }

                           if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                              log.log(
                                 IMPORT_LOG_LEVEL, "Input slot name = " + slotName + ", bind hints = " + bindHints + ", slot path scope = " + slotPathScope
                              );
                           }

                           Object[] choices;
                           try {
                              choices = TmplUtil.findMatchingObjects(
                                 bindHints,
                                 slotPathScope,
                                 responseMessages,
                                 this.getSearchService(deployedTemplateComponent),
                                 this.getTemplateService(deployedTemplateComponent),
                                 templateConfig,
                                 lex
                              );
                           } catch (Exception var32) {
                              choices = null;
                           }

                           if (choices != null && choices.length != 0) {
                              String sourceName = input.name;
                              String sourceSlotName = "out";
                              if (input.slot != null && !input.slot.isEmpty()) {
                                 sourceSlotName = input.slot;
                              }

                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(IMPORT_LOG_LEVEL, "Input name " + sourceName + " slot " + sourceSlotName);
                              }

                              BComponent sourceComponent = getNamedComponentFromChoices(
                                 choices, escapeSlotPathNames(sourceName), getEscapedNameFromSlotPath(sourceName)
                              );
                              if (sourceComponent == null) {
                                 log.log(
                                    Level.WARNING,
                                    lex.getText("bulkDeploy.excelImport.inputSourceError", new Object[]{sourceName, deployedTemplateComponent.getName()})
                                 );
                              } else {
                                 Property sourceProp = sourceComponent.getProperty(sourceSlotName);
                                 if (sourceProp == null) {
                                    log.log(
                                       Level.WARNING,
                                       lex.getText(
                                          "bulkDeploy.excelImport.sourceSlotError",
                                          new Object[]{sourceComponent.getName(), deployedTemplateComponent.getName()}
                                       )
                                    );
                                 } else {
                                    LinkCheck linkCheck = deployedTemplateComponent.checkLink(sourceComponent, sourceProp, inputSlot, null);
                                    if (!linkCheck.isValid()) {
                                       log.log(
                                          Level.WARNING,
                                          lex.getText(
                                             "bulkDeploy.excelImport.inputLinkError",
                                             new Object[]{sourceComponent.getName(), deployedTemplateComponent.getName(), linkCheck.getInvalidReason()}
                                          )
                                       );
                                    } else {
                                       BLink link = deployedTemplateComponent.makeLink(sourceComponent, sourceProp, inputSlot, null);
                                       deployedTemplateComponent.add(null, link);
                                    }
                                 }
                              }
                           } else {
                              log.log(
                                 Level.WARNING,
                                 lex.getText("bulkDeploy.excelImport.inputBindingError", new Object[]{deployedTemplateComponent.getName(), slotName})
                              );
                           }
                        }
                     } else {
                        log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.inputDefinitionError", new Object[]{deployedTemplateComponent.getName()}));
                     }
                  }
               }

               if (deployedRoot.outputs != null && !deployedRoot.outputs.isEmpty()) {
                  for (int outputDefIndex = 0; outputDefIndex < deployedWorksheet.outputDefs.size(); outputDefIndex++) {
                     String[] outputDef = deployedWorksheet.outputDefs.get(outputDefIndex);
                     String slotNamex = "";
                     String bindHintsx = "";
                     String slotPathScopex = "";
                     if (outputDef != null && outputDef.length != 0) {
                        slotNamex = outputDef[0];
                        bindHintsx = outputDef[2];
                        if (deployedRoot.isSlotPathScopeSupported) {
                           slotPathScopex = outputDef[4];
                        }

                        List<BulkDeployUtil.DeployedIOR> namedOutputs = deployedRoot.outputs.get(slotNamex);
                        if (namedOutputs != null && !namedOutputs.isEmpty()) {
                           for (BulkDeployUtil.DeployedIOR deployedOutput : namedOutputs) {
                              if (deployedOutput.scope != null && !deployedOutput.scope.isEmpty()) {
                                 slotPathScopex = deployedOutput.scope;
                              }

                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(
                                    IMPORT_LOG_LEVEL,
                                    "Output slot name = " + slotNamex + ", bind hints = " + bindHintsx + ", slot path scope = " + slotPathScopex
                                 );
                              }

                              Object[] choicesx;
                              try {
                                 choicesx = TmplUtil.findMatchingObjects(
                                    bindHintsx,
                                    slotPathScopex,
                                    responseMessages,
                                    this.getSearchService(deployedTemplateComponent),
                                    this.getTemplateService(deployedTemplateComponent),
                                    templateConfig,
                                    lex
                                 );
                              } catch (Exception var31) {
                                 choicesx = null;
                              }

                              if (choicesx != null && choicesx.length != 0) {
                                 String outputName = deployedOutput.name;
                                 if (outputName != null && !outputName.isEmpty()) {
                                    String outputSlotName = deployedOutput.slot;
                                    if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                       log.log(IMPORT_LOG_LEVEL, "Output name " + outputName + " slot " + outputSlotName);
                                    }

                                    BComponent targetComponent = getNamedComponentFromChoices(
                                       choicesx, escapeSlotPathNames(outputName), getEscapedNameFromSlotPath(outputName)
                                    );
                                    if (targetComponent == null) {
                                       log.log(
                                          Level.WARNING,
                                          lex.getText("bulkDeploy.excelImport.outputTargetError", new Object[]{outputName, deployedTemplateComponent.getName()})
                                       );
                                    } else {
                                       Slot targetSlot = targetComponent.getSlot(outputSlotName);
                                       if (targetSlot == null) {
                                          log.log(
                                             Level.WARNING,
                                             lex.getText(
                                                "bulkDeploy.excelImport.targetSlotError",
                                                new Object[]{targetComponent.getName(), deployedTemplateComponent.getName()}
                                             )
                                          );
                                       } else {
                                          BLink link = new BLink(deployedTemplateComponent.getHandleOrd(), slotNamex, outputSlotName, true);
                                          targetComponent.add(null, link);
                                          targetComponent.lease(1);
                                       }
                                    }
                                 } else {
                                    log.log(
                                       Level.WARNING,
                                       lex.getText(
                                          "bulkDeploy.excelImport.outputComponentNotFound", new Object[]{outputName, deployedTemplateComponent.getName()}
                                       )
                                    );
                                 }
                              } else {
                                 log.log(
                                    Level.WARNING,
                                    lex.getText("bulkDeploy.excelImport.outputBindingError", new Object[]{deployedTemplateComponent.getName(), slotNamex})
                                 );
                              }
                           }
                        } else {
                           log.log(
                              Level.WARNING, lex.getText("bulkDeploy.excelImport.outputDefinitionError", new Object[]{deployedTemplateComponent.getName()})
                           );
                        }
                     } else {
                        log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.outputDefinitionError", new Object[]{deployedTemplateComponent.getName()}));
                     }
                  }
               }

               if (deployedRoot.relations != null) {
                  for (int relationIndex = 0; relationIndex < deployedRoot.relations.size(); relationIndex++) {
                     String[] relationDef = deployedWorksheet.relationDefs.get(relationIndex);
                     if (relationDef != null && relationDef.length != 0) {
                        String label = relationDef[0];
                        String relationId = relationDef[1];
                        String relateHints = relationDef[2];
                        String direction = relationDef[3];
                        String slotPathScopex = "";
                        if (deployedRoot.isSlotPathScopeSupported) {
                           slotPathScopex = relationDef[4];
                        }

                        List<BulkDeployUtil.DeployedIOR> namedRelations = deployedRoot.relations.get(relationId + direction);
                        if (namedRelations != null && !namedRelations.isEmpty()) {
                           for (BulkDeployUtil.DeployedIOR deployedRelation : namedRelations) {
                              if (deployedRelation.scope != null && !deployedRelation.scope.isEmpty()) {
                                 slotPathScopex = deployedRelation.scope;
                              }

                              if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                 log.log(
                                    IMPORT_LOG_LEVEL,
                                    "Relation name = "
                                       + label
                                       + ", bind hints = "
                                       + relateHints
                                       + ", direction = "
                                       + direction
                                       + ", slot path scope = "
                                       + slotPathScopex
                                 );
                              }

                              Object[] choicesxx;
                              try {
                                 choicesxx = TmplUtil.findMatchingObjects(
                                    relateHints,
                                    slotPathScopex,
                                    responseMessages,
                                    this.getSearchService(deployedTemplateComponent),
                                    this.getTemplateService(deployedTemplateComponent),
                                    templateConfig,
                                    lex
                                 );
                              } catch (Exception var30) {
                                 choicesxx = null;
                              }

                              if (choicesxx != null && choicesxx.length != 0) {
                                 String relationNames = deployedRelation.name;
                                 Set<String> createdRelations = new HashSet<>();

                                 for (String relationName : relationNames.split(",")) {
                                    relationName = relationName.trim();
                                    if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                                       log.log(IMPORT_LOG_LEVEL, "Relation name " + relationName);
                                    }

                                    BComponent externalComponent = getNamedComponentFromChoices(
                                       choicesxx, escapeSlotPathNames(relationName), getEscapedNameFromSlotPath(relationName)
                                    );
                                    if (externalComponent == null) {
                                       log.log(
                                          Level.WARNING,
                                          lex.getText(
                                             "bulkDeploy.excelImport.relationComponentError", new Object[]{deployedTemplateComponent.getName(), relationName}
                                          )
                                       );
                                    } else {
                                       String path = externalComponent.getSlotPath().getBody() + ':' + direction;
                                       if (createdRelations.contains(path)) {
                                          log.log(IMPORT_LOG_LEVEL, lex.getText("bulkDeploy.excelImport.duplicateRelation", new Object[]{path}));
                                       } else {
                                          createdRelations.add(path);
                                          Id id = Id.newId(relationId);
                                          BOrd endPointOrd = direction.equals(lex.getText("templateRelationEditor.out"))
                                             ? externalComponent.getHandleOrd()
                                             : deployedTemplateComponent.getHandleOrd();
                                          BComponent relationOwner = direction.equals(lex.getText("templateRelationEditor.out"))
                                             ? deployedTemplateComponent
                                             : externalComponent;
                                          BRelation newRelation = new BRelation(id, endPointOrd);
                                          relationOwner.relations().add(newRelation);
                                          relationOwner.lease();
                                       }
                                    }
                                 }
                              } else {
                                 log.log(
                                    Level.WARNING,
                                    lex.getText("bulkDeploy.excelImport.relationBindingError", new Object[]{deployedTemplateComponent.getName(), label})
                                 );
                              }
                           }
                        } else {
                           log.log(
                              Level.WARNING, lex.getText("bulkDeploy.excelImport.relationDefinitionError", new Object[]{deployedTemplateComponent.getName()})
                           );
                        }
                     } else {
                        log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.relationDefinitionError", new Object[]{deployedTemplateComponent.getName()}));
                     }
                  }
               }

               templateConfig.subscribed();
               if (deployedRoot.configs != null) {
                  setConfigsFromWorksheet(deployedWorksheet, deployedRoot, deployedTemplateComponent, templateConfig, whenNoValueKeepCurrent);
               }

               if (deployedRoot.optionalConfigs != null) {
                  setOptionalConfigsFromWorksheet(deployedWorksheet, deployedRoot, deployedTemplateComponent, templateConfig, whenNoValueKeepCurrent);
               }

               if (deployedRoot.tags != null) {
                  this.setTagsFromWorksheet(deployedWorksheet, deployedRoot, deployedTemplateComponent);
               }

               return responseMessages;
            }
         }
      } else {
         return responseMessages;
      }
   }

   public static void setConfigsFromWorksheet(
      BulkDeployUtil.DeployedWorksheet deployedWorksheet,
      BulkDeployUtil.DeployedRoot deployedRoot,
      BComponent deployedTemplateComponent,
      BTemplateConfig templateConfig,
      boolean whenNoValueKeepCurrent
   ) {
      setTemplateConfigProperty(deployedRoot.configs, deployedWorksheet.configDefs, deployedTemplateComponent, templateConfig, whenNoValueKeepCurrent);
   }

   public static void setOptionalConfigsFromWorksheet(
      BulkDeployUtil.DeployedWorksheet deployedWorksheet,
      BulkDeployUtil.DeployedRoot deployedRoot,
      BComponent deployedTemplateComponent,
      BTemplateConfig templateConfig,
      boolean whenNoValueKeepCurrent
   ) {
      setTemplateConfigProperty(
         deployedRoot.optionalConfigs, deployedWorksheet.optionalConfigDefs, deployedTemplateComponent, templateConfig, whenNoValueKeepCurrent
      );
   }

   private static void setTemplateConfigProperty(
      String[] rootConfigDefs,
      List<String[]> deployedConfigDefs,
      BComponent deployedTemplateComponent,
      BTemplateConfig templateConfig,
      boolean whenNoValueKeepCurrent
   ) {
      BStruct newInstance = null;
      String previousSlotName = "";

      for (int n = 0; n < rootConfigDefs.length; n++) {
         String[] configDef = deployedConfigDefs.get(n);
         if (configDef != null && configDef.length != 0) {
            String slotName = SlotPath.escape(configDef[0]);
            String slotType = configDef[2];
            String defaultValue = configDef[3];
            if (log.isLoggable(IMPORT_LOG_LEVEL)) {
               log.log(IMPORT_LOG_LEVEL, "Configuration name = " + slotName + ", slot type = " + slotType);
            }

            String[] propertyNames = null;
            Property configProp = templateConfig.getProperty(slotName);
            if (configProp == null) {
               propertyNames = getPropertyNamesForConfigSlot(slotName);
               configProp = propertyNames.length > 0 ? templateConfig.getProperty(propertyNames[0]) : null;
            }

            if (configProp == null) {
               log.log(
                  Level.WARNING,
                  lex.getText("bulkDeploy.excelImport.configComponentError", new Object[]{deployedTemplateComponent.getName(), SlotPath.unescape(slotName)})
               );
            } else {
               try {
                  if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                     log.log(IMPORT_LOG_LEVEL, "  Template Config Property = " + configProp.getName());
                  }

                  String configValue = rootConfigDefs[n];
                  if (configValue == null) {
                     configValue = defaultValue;
                  }

                  if (configProp.getType().is(BStruct.TYPE) && !configProp.getType().is(BStatusValue.TYPE)) {
                     TypeInfo slotTypeInfo = configProp.getType().getTypeInfo();
                     if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                        log.log(IMPORT_LOG_LEVEL, "Slot type info = " + slotTypeInfo);
                     }

                     if (!previousSlotName.contentEquals(configProp.getName())) {
                        if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                           log.log(IMPORT_LOG_LEVEL, "  Create new BStruct instance of " + slotTypeInfo.getTypeName());
                        }

                        previousSlotName = configProp.getName();
                        newInstance = (BStruct)slotTypeInfo.getInstance();
                     }

                     if (propertyNames.length > 1) {
                        Property structProp = ((BComplex)configProp.getDefaultValue()).getProperty(propertyNames[1]);
                        if (structProp != null) {
                           BValue configPropertyValue = null;
                           if (configValue != null) {
                              configPropertyValue = setConfigValue(structProp, configValue, slotType);
                           }

                           if (configPropertyValue == null && !whenNoValueKeepCurrent) {
                              configPropertyValue = structProp.getDefaultValue().newCopy();
                           }

                           if (configPropertyValue != null) {
                              newInstance.set(structProp.getName(), configPropertyValue);
                           }
                        }
                     }

                     templateConfig.set(configProp.getName(), newInstance);
                  } else {
                     BValue configPropertyValuex = null;
                     if (configValue != null) {
                        configPropertyValuex = setConfigValue(configProp, configValue, slotType);
                     }

                     if (configPropertyValuex == null && !whenNoValueKeepCurrent) {
                        configPropertyValuex = configProp.getDefaultValue().newCopy();
                     }

                     if (configPropertyValuex != null) {
                        templateConfig.set(configProp.getName(), configPropertyValuex);
                     }
                  }
               } catch (Exception var18) {
                  log.log(
                     Level.WARNING,
                     lex.getText("bulkDeploy.excelImport.configComponentError", new Object[]{deployedTemplateComponent.getName(), slotName}),
                     (Throwable)var18
                  );
               }
            }
         } else {
            log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.configDefinitionError", new Object[]{deployedTemplateComponent.getName()}));
         }
      }
   }

   protected static String[] getPropertyNamesForConfigSlot(String configSlotName) {
      String[] names = SlotPath.unescape(configSlotName).split("\\.");

      for (int i = 0; i < names.length; i++) {
         names[i] = SlotPath.escape(names[i]);
      }

      return names;
   }

   public void setTagsFromWorksheet(
      BulkDeployUtil.DeployedWorksheet deployedWorksheet, BulkDeployUtil.DeployedRoot deployedRoot, BComponent deployedTemplateComponent
   ) {
      for (int n = 0; n < deployedRoot.tags.length; n++) {
         String[] tagDef = deployedWorksheet.tagDefs.get(n);
         if (tagDef != null && tagDef.length != 0) {
            String tagId = tagDef[0];
            String slotType = tagDef[1];
            String defaultValue = tagDef[2];
            if (log.isLoggable(IMPORT_LOG_LEVEL)) {
               log.log(IMPORT_LOG_LEVEL, "TagId = " + tagId + ", slot type = " + slotType + ", default = " + defaultValue);
            }

            String configValue = deployedRoot.tags[n];
            if (configValue == null) {
               configValue = defaultValue;
            }

            if (log.isLoggable(IMPORT_LOG_LEVEL)) {
               log.log(IMPORT_LOG_LEVEL, "Applying value = " + configValue);
            }

            Tag newTag = Tag.newTag(tagId, configValue);
            if (deployedWorksheet.templateType.equals("Application")) {
               BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(deployedTemplateComponent);
               if (templateConfig == null) {
                  log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.invalidTemplateError", new Object[]{deployedTemplateComponent.getName()}));
                  return;
               }

               templateConfig.lease();

               Object[] choices;
               try {
                  choices = TmplUtil.findMatchingObjects(
                     tagId,
                     "",
                     new ArrayList<>(),
                     this.getSearchService(deployedTemplateComponent),
                     this.getTemplateService(deployedTemplateComponent),
                     templateConfig,
                     lex
                  );
               } catch (Exception var18) {
                  if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                     log.log(IMPORT_LOG_LEVEL, " No components found with tag = " + tagId);
                  }

                  choices = null;
               }

               for (Object choice : choices) {
                  if (choice instanceof BComponent) {
                     BComponent component = (BComponent)choice;
                     component.lease();
                     if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                        log.log(IMPORT_LOG_LEVEL, " TO Entity component at = " + component.getSlotPath());
                     }

                     component.tags().set(newTag);
                  }
               }
            } else {
               ComponentTreeIterator iterator = new ComponentTreeIterator(deployedTemplateComponent);

               while (iterator.hasNext()) {
                  Entity entity = iterator.next();
                  BComponent component = (BComponent)entity;
                  if (entity.tags().contains(newTag.getId())) {
                     component.lease();
                     if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                        log.log(IMPORT_LOG_LEVEL, " TO Entity component at = " + component.getSlotPath());
                     }

                     entity.tags().set(newTag);
                  }
               }
            }
         } else {
            log.log(Level.WARNING, lex.getText("bulkDeploy.excelImport.tagDefinitionError", new Object[]{deployedTemplateComponent.getName()}));
         }
      }
   }

   public boolean templatesHaveConfigPasswords(BWbDeployableNtplFile[] files) {
      boolean hasConfigPasswords = false;

      for (BWbDeployableNtplFile file : files) {
         if (this.templateHasConfigPasswords(file)) {
            hasConfigPasswords = true;
         }

         file.closeIfOpen();
      }

      return hasConfigPasswords;
   }

   public boolean templateHasConfigPasswords(BWbDeployableNtplFile file) {
      BComponent templateBase = file.getBaseComponent();
      if (templateBase == null) {
         return false;
      } else {
         BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(templateBase);
         if (templateConfig == null) {
            return false;
         } else {
            BConfigBinding[] configBindings = templateConfig.getConfigBindings();
            if (configBindings == null) {
               return false;
            } else {
               for (BConfigBinding configBinding : configBindings) {
                  String slotName = configBinding.getSourceSlot();
                  Property configProperty = templateConfig.getProperty(slotName);
                  if (configProperty != null) {
                     if (configProperty.getType().is(BPassword.TYPE)) {
                        return true;
                     }

                     if (configProperty.getType().is(BStruct.TYPE) && !configProperty.getType().is(BStatusValue.TYPE)) {
                        for (Property property : ((BComplex)configProperty.getDefaultValue()).getPropertiesArray()) {
                           if (property.getType().is(BPassword.TYPE)) {
                              return true;
                           }
                        }
                     }
                  }
               }

               return false;
            }
         }
      }
   }

   public static void setComponentPosition(BComponent component, String positionStr) {
      BWsAnnotation position = buildWsPosition(positionStr);
      if (position != null) {
         Slot wsAnnotation = component.getSlot("wsAnnotation");
         if (wsAnnotation == null) {
            component.add("wsAnnotation", position);
         } else if (wsAnnotation.isProperty()) {
            component.set((Property)wsAnnotation, position);
         }
      }
   }

   private static BWsAnnotation buildWsPosition(String positionStr) {
      if (positionStr != null && !positionStr.isEmpty()) {
         String[] values = positionStr.split(",");

         for (int i = 0; i < values.length; i++) {
            values[i] = values[i].trim();
         }

         BWsAnnotation position = null;

         try {
            switch (values.length) {
               case 2:
                  position = BWsAnnotation.make(Integer.parseInt(values[0]), Integer.parseInt(values[1]));
                  break;
               case 3:
                  position = BWsAnnotation.make(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]));
                  break;
               case 4:
                  position = BWsAnnotation.make(
                     Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]), Integer.parseInt(values[3])
                  );
            }
         } catch (NumberFormatException var4) {
         }

         return position;
      } else {
         return null;
      }
   }

   private static BValue setConfigValue(Property configProp, String configValue, String slotType) throws IOException {
      TypeInfo slotTypeInfo = Sys.getRegistry().getType(slotType);
      if (log.isLoggable(IMPORT_LOG_LEVEL)) {
         log.log(IMPORT_LOG_LEVEL, "Slot type info = " + slotTypeInfo);
      }

      BObject slotInstance = slotTypeInfo.getInstance();
      BValue configPropertyValue = null;
      if (configProp.getType().is(BStatusValue.TYPE)) {
         configPropertyValue = (BValue)slotTypeInfo.getInstance();
         if (configProp.getType().is(BStatusNumeric.TYPE)) {
            ((BStatusNumeric)configPropertyValue).setValue(Double.parseDouble(configValue));
         } else if (configProp.getType().is(BStatusBoolean.TYPE)) {
            ((BStatusBoolean)configPropertyValue).setValue(Boolean.parseBoolean(configValue.toLowerCase()));
         } else if (configProp.getType().is(BStatusString.TYPE)) {
            ((BStatusString)configPropertyValue).setValue(configValue);
         } else if (configProp.getType().is(BStatusEnum.TYPE)) {
            BValue defaultValue = configProp.getDefaultValue();
            BEnumRange range = ((BIEnum)defaultValue).getEnum().getRange();
            if (range.isNull()) {
               ((BStatusEnum)configPropertyValue).setValue(BDynamicEnum.make(Double.valueOf(configValue).intValue()));
            } else {
               BEnum selectedValue = range.get(configValue);
               if (log.isLoggable(IMPORT_LOG_LEVEL)) {
                  log.log(IMPORT_LOG_LEVEL, "BStatusEnum value = " + selectedValue.getTag());
               }

               if (selectedValue != null) {
                  ((BStatusEnum)configPropertyValue).setValue(selectedValue);
               }
            }
         }
      } else if (configProp.getType().is(BSimple.TYPE)) {
         if (configProp.getType().is(BInteger.TYPE)) {
            String converted = Integer.toString(Double.valueOf(configValue).intValue());
            configPropertyValue = BInteger.make(converted);
         } else if (configProp.getType().is(BLong.TYPE)) {
            String converted = Long.toString(Double.valueOf(configValue).longValue());
            configPropertyValue = BLong.make(converted);
         } else if (configProp.getType().is(BBoolean.TYPE)) {
            configPropertyValue = BBoolean.make(Boolean.parseBoolean(configValue.toLowerCase()));
         } else if (configProp.getType().is(BFrozenEnum.TYPE)) {
            BEnum selectedValuex = ((BIEnum)slotInstance).getEnum().getRange().get(configValue);
            if (log.isLoggable(IMPORT_LOG_LEVEL)) {
               log.log(IMPORT_LOG_LEVEL, "BFrozenEnum value = " + selectedValuex.getTag());
            }

            configPropertyValue = (BValue)((BIEncodable)slotInstance).decodeFromString(configValue);
         } else if (configProp.getType().is(BDynamicEnum.TYPE)) {
            BValue defaultValue = configProp.getDefaultValue();
            BEnumRange range = ((BIEnum)defaultValue).getEnum().getRange();
            BEnum selectedValuex = range.get(configValue);
            if (log.isLoggable(IMPORT_LOG_LEVEL)) {
               log.log(IMPORT_LOG_LEVEL, "BDynamicEnum value = " + selectedValuex.getTag());
            }

            int selectedOrdinal = selectedValuex.getOrdinal();
            configPropertyValue = BDynamicEnum.make(selectedOrdinal, range);
         } else {
            configPropertyValue = (BValue)((BIEncodable)slotInstance).decodeFromString(configValue);
         }
      }

      return configPropertyValue;
   }

   protected BSearchService getSearchService(BComponent base) {
      if (this.searchService != null) {
         return this.searchService;
      } else {
         BIService service = getService(BSearchService.TYPE, base);
         if (service == null) {
            return null;
         } else {
            this.searchService = (BSearchService)service;
            return this.searchService;
         }
      }
   }

   protected BTemplateService getTemplateService(BComponent base) {
      if (this.templateService != null) {
         return this.templateService;
      } else {
         BIService service = getService(BTemplateService.TYPE, base);
         if (service == null) {
            return null;
         } else {
            this.templateService = (BTemplateService)service;
            return this.templateService;
         }
      }
   }

   private static BIService getService(Type serviceType, BComponent base) {
      BComponentSpace space = base.getComponentSpace();
      if (space == null) {
         return null;
      } else {
         BComponent root = space.getRootComponent();
         BServiceContainer[] serviceContainer = (BServiceContainer[])root.getChildren(BServiceContainer.class);
         if (serviceContainer != null && serviceContainer.length != 0) {
            BIService[] services = (BIService[])serviceContainer[0].getChildren(serviceType.getTypeClass());
            return services != null && services.length != 0 ? services[0] : null;
         } else {
            return null;
         }
      }
   }

   private static String getCell(Row row, int cellNum) {
      String result = getCellOrNull(row, cellNum, false);
      return result == null ? "" : result;
   }

   private static String getCellOrNull(Row row, int cellNum, boolean forceString) {
      Cell cell = row.getCell(cellNum);
      if (cell == null || cell.getCellType() == CellType.BLANK) {
         return null;
      } else if (forceString && cell.getCellType() != CellType.STRING) {
         DataFormatter formatter = ExcelUtils.makeDataFormatter();
         return formatter.formatCellValue(cell);
      } else {
         return cell.toString();
      }
   }

   private static boolean getCellAsBoolean(Row row, int cellNum) {
      Cell cell = row.getCell(cellNum);
      if (cell == null) {
         return false;
      } else {
         switch (cell.getCellType()) {
            case BLANK:
            case BOOLEAN:
               return cell.getBooleanCellValue();
            case STRING:
            default:
               return Boolean.parseBoolean(cell.toString());
            case NUMERIC:
               return cell.getNumericCellValue() != 0.0;
         }
      }
   }

   private static String getEscapedNameFromSlotPath(String name) {
      if (name != null && !name.isEmpty()) {
         String[] names = name.split("/");
         return SlotPath.escape(names[names.length - 1]);
      } else {
         return "";
      }
   }

   private static String escapeSlotPathNames(String name) {
      String[] names = name.split("/");
      StringJoiner sj = new StringJoiner("/");

      for (String node : names) {
         sj.add(SlotPath.escape(node));
      }

      return sj.toString();
   }

   private static BComponent getNamedComponentFromChoices(Object[] choices, String path, String name) {
      for (Object choice : choices) {
         if (choice instanceof BComponent) {
            BComponent component = (BComponent)choice;
            if (component.getSlotPath().toString().contains(path) && component.getName().equals(name)) {
               return component;
            }
         }
      }

      return null;
   }

   public BOrdList getSelectedOptionalsFromWorkbook(
      BWbDeployableNtplFile applicationTemplateFile, BulkDeployUtil.DeployedWorksheet deployedWorksheet, BulkDeployUtil.DeployedRoot deployedRoot
   ) {
      Array<BOrd> optionalComponentOrds = applicationTemplateFile.getTemplateManifest().optional;
      if (optionalComponentOrds.isEmpty()) {
         return BOrdList.DEFAULT;
      } else {
         List<BOrd> optionalOrdList = new ArrayList<>();
         String[] rootOptionals = deployedRoot.getOptionals();
         if (rootOptionals != null) {
            for (int i = 0; i < rootOptionals.length; i++) {
               String optionalInstallValue = rootOptionals[i];
               String[] optionalColumnInfo = deployedWorksheet.optionalDefs.get(i);
               if (optionalInstallValue == null) {
                  optionalInstallValue = optionalColumnInfo[2];
               }

               String optionalSlotPath = optionalColumnInfo[0];

               for (BOrd optionalComponentOrd : optionalComponentOrds) {
                  OrdQuery[] queries = optionalComponentOrd.parse();
                  SlotPath path = (SlotPath)queries[queries.length - 1];
                  if (path.getBody().contains(optionalSlotPath)) {
                     if (Boolean.parseBoolean(optionalInstallValue.toLowerCase())) {
                        break;
                     }

                     String[] names = path.getNames();
                     StringBuilder friendlyName = new StringBuilder(names[1]);

                     for (int j = 2; j < names.length; j++) {
                        friendlyName.append('/').append(names[j]);
                     }

                     optionalOrdList.add(BOrd.make(new SlotPath(friendlyName.toString())));
                     break;
                  }
               }
            }
         }

         return BOrdList.make(optionalOrdList.toArray(BORDS));
      }
   }

   static class BindingProperties {
      BConfigBinding configBinding;
      List<BulkDeployUtil.BindingProperty> properties;

      BindingProperties(BConfigBinding binding, BTemplateConfig templateConfig) {
         this.configBinding = binding;
         this.properties = new ArrayList<>();
         String slotName = this.configBinding.getSourceSlot();
         Property configProperty = templateConfig.getProperty(slotName);
         BValue configPropertyValue = templateConfig.get(slotName);
         if (configPropertyValue != null) {
            if (configProperty.getType().is(BStruct.TYPE) && !configProperty.getType().is(BStatusValue.TYPE)) {
               Property[] structProperties = ((BComplex)configPropertyValue).getPropertiesArray();

               for (Property structProperty : structProperties) {
                  this.properties
                     .add(
                        new BulkDeployUtil.BindingProperty(
                           structProperty, ((BComplex)configPropertyValue).get(structProperty), slotName + "." + structProperty.getName()
                        )
                     );
               }
            } else {
               this.properties.add(new BulkDeployUtil.BindingProperty(configProperty, configPropertyValue, slotName));
            }
         }
      }
   }

   static class BindingProperty {
      Property property;
      BValue value;
      String name;

      BindingProperty(Property property, BValue value, String name) {
         this.property = property;
         this.value = value;
         this.name = name;
      }
   }

   public static class DeployedIOR {
      public String name;
      public String slot;
      public String scope;

      DeployedIOR() {
      }
   }

   public static class DeployedRoot {
      private String parentComponentSlotPath;
      private String deployName;
      private String displayName;
      private String position;
      private List<String> deviceTarget;
      private boolean enableHistories;
      private List<BulkDeployUtil.DeployedIOR> inputs;
      private Map<String, List<BulkDeployUtil.DeployedIOR>> outputs;
      private Map<String, List<BulkDeployUtil.DeployedIOR>> relations;
      private String[] configs;
      private String[] optionals;
      private String[] optionalConfigs;
      private String[] tags;
      private BComponent deployedTemplate;
      private boolean isSlotPathScopeSupported;

      public void setDeployedTemplate(BComponent deployedTemplate) {
         this.deployedTemplate = deployedTemplate;
      }

      public String getParentComponentSlotPath() {
         return this.parentComponentSlotPath;
      }

      public String getDeployName() {
         return this.deployName;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getPosition() {
         return this.position;
      }

      public List<String> getDeviceTarget() {
         return this.deviceTarget;
      }

      public boolean isEnableHistories() {
         return this.enableHistories;
      }

      public List<BulkDeployUtil.DeployedIOR> getInputs() {
         return this.inputs;
      }

      public Map<String, List<BulkDeployUtil.DeployedIOR>> getOutputs() {
         return this.outputs;
      }

      public Map<String, List<BulkDeployUtil.DeployedIOR>> getRelations() {
         return this.relations;
      }

      public String[] getConfigs() {
         return this.configs == null ? null : (String[])this.configs.clone();
      }

      public String[] getOptionals() {
         return this.optionals == null ? null : (String[])this.optionals.clone();
      }

      public String[] getOptionalConfigs() {
         return this.optionalConfigs == null ? null : (String[])this.optionalConfigs.clone();
      }

      public String[] getTags() {
         return this.tags == null ? null : (String[])this.tags.clone();
      }

      public BComponent getDeployedTemplate() {
         return this.deployedTemplate;
      }
   }

   public static class DeployedWorksheet {
      public String title;
      public String vendor;
      public String version;
      public BUuid uid;
      public String templateType;
      public String templateFile;
      public String templateExportVersion;
      public List<BulkDeployUtil.DeployedRoot> deployedRoots = new ArrayList<>();
      public List<String[]> inputDefs = new ArrayList<>();
      public List<String[]> outputDefs = new ArrayList<>();
      public List<String[]> relationDefs = new ArrayList<>();
      public List<String[]> configDefs = new ArrayList<>();
      public List<String[]> optionalDefs = new ArrayList<>();
      public List<String[]> optionalConfigDefs = new ArrayList<>();
      public List<String[]> tagDefs = new ArrayList<>();
      public boolean isSlotPathScopeSupported;
   }

   static class OptionalBindingProperties extends BulkDeployUtil.BindingProperties {
      BComponent parentComponent;
      String relativeSlotPath = "";

      OptionalBindingProperties(BConfigBinding binding, List<BOrd> optionalComponents, BComponent templateBase, BTemplateConfig templateConfig) {
         super(binding, templateConfig);
         Optional<BComponent> parentComponent = BulkDeployUtil.getComponentForOptionalConfig(binding, optionalComponents, templateBase);
         if (parentComponent.isPresent()) {
            this.parentComponent = parentComponent.get();
            this.relativeSlotPath = BulkDeployUtil.getRelativeSlotPathForOptionalComponent(parentComponent.get(), templateBase);
         }
      }
   }

   private static class StyleVault {
      private Font boldFont = null;
      private CellStyle infoCellStyle = null;
      private CellStyle instanceCellStyle = null;
      private CellStyle optionalCellStyle = null;
      private CellStyle inputCellStyle = null;
      private CellStyle outputCellStyle = null;
      private CellStyle relationCellStyle = null;
      private CellStyle configCellStyle = null;
      private CellStyle optionalConfigCellStyle = null;
      private CellStyle highlightCellStyle = null;
      private CellStyle tagCellStyle = null;
      private CellStyle dataCellStyle = null;
      private final CellStyle dataCellStyleRO = null;
      private CellStyle stringCellStyle = null;
      private final CellStyle stringCellStyleRO = null;
      private CellStyle passwordCellStyle = null;
      private final CellStyle passwordCellStyleRO = null;
      private CellStyle emptyCellStyle = null;
      private final Workbook workbook;

      StyleVault(Workbook workbook) {
         this.workbook = workbook;
      }

      CellStyle getInfoCellStyle() {
         if (this.infoCellStyle == null) {
            Font infoFont = this.workbook.createFont();
            infoFont.setBold(true);
            infoFont.setColor(ExcelUtils.getColorIndex(IndexedColors.DARK_BLUE));
            this.infoCellStyle = this.workbook.createCellStyle();
            this.infoCellStyle.setFont(infoFont);
            this.infoCellStyle.setLocked(false);
         }

         return this.infoCellStyle;
      }

      CellStyle getInstanceCellStyle() {
         if (this.instanceCellStyle == null) {
            this.instanceCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.GREY_25_PERCENT));
         }

         return this.instanceCellStyle;
      }

      CellStyle getOptionalCellStyle() {
         if (this.optionalCellStyle == null) {
            this.optionalCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.GREY_40_PERCENT));
         }

         return this.optionalCellStyle;
      }

      CellStyle getInputCellStyle() {
         if (this.inputCellStyle == null) {
            this.inputCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.PALE_BLUE));
         }

         return this.inputCellStyle;
      }

      CellStyle getOutputCellStyle() {
         if (this.outputCellStyle == null) {
            this.outputCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.SKY_BLUE));
         }

         return this.outputCellStyle;
      }

      CellStyle getRelationCellStyle() {
         if (this.relationCellStyle == null) {
            this.relationCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.LIGHT_GREEN));
         }

         return this.relationCellStyle;
      }

      CellStyle getConfigCellStyle() {
         if (this.configCellStyle == null) {
            this.configCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.LIGHT_YELLOW));
         }

         return this.configCellStyle;
      }

      CellStyle getOptionalConfigCellStyle() {
         if (this.optionalConfigCellStyle == null) {
            this.optionalConfigCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.GOLD));
         }

         return this.optionalConfigCellStyle;
      }

      CellStyle getHighlightCellStyle() {
         if (this.highlightCellStyle == null) {
            this.highlightCellStyle = this.generateColorCellStyle(ExcelUtils.getColorIndex(IndexedColors.GREY_25_PERCENT));
         }

         return this.highlightCellStyle;
      }

      CellStyle getTagCellStyle() {
         if (this.tagCellStyle == null) {
            this.tagCellStyle = this.generateHeaderCellStyle(this.getBoldFont(), ExcelUtils.getColorIndex(IndexedColors.LEMON_CHIFFON));
         }

         return this.tagCellStyle;
      }

      CellStyle getDataCellStyle() {
         if (this.dataCellStyle == null) {
            this.dataCellStyle = this.generateDataCellStyle();
         }

         return this.dataCellStyle;
      }

      CellStyle getStringCellStyle() {
         if (this.stringCellStyle == null) {
            this.stringCellStyle = this.generateDataCellStyle();
            this.stringCellStyle.setQuotePrefixed(true);
            this.stringCellStyle.setDataFormat(ExcelUtils.getBuiltinFormat("TEXT"));
         }

         return this.stringCellStyle;
      }

      CellStyle getPasswordCellStyle() {
         if (this.passwordCellStyle == null) {
            this.passwordCellStyle = this.generateDataCellStyle();
            this.passwordCellStyle.setHidden(true);
            this.passwordCellStyle.setQuotePrefixed(true);
            CreationHelper creationHelper = this.workbook.getCreationHelper();
            short formatIndex = creationHelper.createDataFormat().getFormat(";;;**");
            this.passwordCellStyle.setDataFormat(formatIndex);
         }

         return this.passwordCellStyle;
      }

      CellStyle getEmptyCellStyle() {
         if (this.emptyCellStyle == null) {
            this.emptyCellStyle = this.generateCellStyle(null, (short)0, false);
         }

         return this.emptyCellStyle;
      }

      private CellStyle generateDataCellStyle() {
         return this.generateCellStyle(null, (short)0, false);
      }

      private CellStyle generateHeaderCellStyle(Font font, short color) {
         return this.generateCellStyle(font, color, true);
      }

      private CellStyle generateCellStyle(Font font, short color, boolean header) {
         CellStyle newStyle = this.workbook.createCellStyle();
         if (font != null) {
            newStyle.setFont(font);
         }

         newStyle.setHidden(false);
         newStyle.setLocked(header);
         newStyle.setQuotePrefixed(header);
         if (color != 0) {
            newStyle.setFillForegroundColor(color);
            newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
         }

         return newStyle;
      }

      private CellStyle generateColorCellStyle(short color) {
         CellStyle newStyle = this.workbook.createCellStyle();
         newStyle.setFillForegroundColor(color);
         newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
         return newStyle;
      }

      private Font getBoldFont() {
         if (this.boldFont == null) {
            this.boldFont = this.workbook.createFont();
            this.boldFont.setBold(true);
         }

         return this.boldFont;
      }
   }

   private static enum TemplateType {
      COMPONENT,
      DEVICE,
      APPLICATION;
   }
}
