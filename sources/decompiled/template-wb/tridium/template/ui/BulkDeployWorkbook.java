package com.tridium.template.ui;

import com.tridium.excel.EncryptedDocumentException;
import com.tridium.excel.ExcelUtils;
import com.tridium.excel.Name;
import com.tridium.excel.Workbook;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;

public class BulkDeployWorkbook implements Closeable {
   private static final String TEMPLATE_EXPORT_VERSION_NAME = "version";
   private static final String TEMPLATE_TYPE_NAME = "templateType";
   private static final String KEEP_PRIVATE_NAME = "keepPrivate";
   private static final String TEMPLATE_TYPE_COMPONENT = "Component";
   private static final String TEMPLATE_TYPE_DEVICE = "Device";
   private static final String TEMPLATE_TYPE_APPLICATION = "Application";
   private final String name;
   private final boolean isEncrypted;
   private final boolean wrongPassword;
   private final boolean usesOldExcelFormat;
   private final String templateExportVersion;
   private final boolean keepPrivate;
   private Workbook workbook;
   private InputStream workbookStream;

   public static BulkDeployWorkbook load(File workbookFile, String password) throws IOException {
      try {
         return AccessController.doPrivileged(
            (PrivilegedExceptionAction<BulkDeployWorkbook>)(() -> new BulkDeployWorkbook(workbookFile.getName(), workbookFile, password))
         );
      } catch (PrivilegedActionException var3) {
         throw (IOException)var3.getException();
      }
   }

   public static BulkDeployWorkbook load(File workbookFile) throws IOException {
      return load(workbookFile, null);
   }

   public static BulkDeployWorkbook load(String name, InputStream inputStream) throws IOException {
      return load(name, inputStream, null);
   }

   public static BulkDeployWorkbook load(String name, InputStream inputStream, String password) throws IOException {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<BulkDeployWorkbook>)(() -> new BulkDeployWorkbook(name, inputStream, password)));
      } catch (PrivilegedActionException var4) {
         throw (IOException)var4.getException();
      }
   }

   @Override
   public void close() throws IOException {
      IOException exception = null;
      if (this.workbook != null) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               this.workbook.close();
               return null;
            }));
         } catch (PrivilegedActionException var3) {
            exception = (IOException)var3.getException();
         }

         this.workbook = null;
      }

      if (this.workbookStream != null) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               this.workbookStream.close();
               return null;
            }));
         } catch (PrivilegedActionException var4) {
            if (exception == null) {
               exception = (IOException)var4.getException();
            } else {
               var4.addSuppressed(exception);
               exception = (IOException)var4.getException();
            }
         }

         this.workbookStream = null;
      }

      if (exception != null) {
         throw exception;
      }
   }

   public String getName() {
      return this.name;
   }

   public boolean isEncrypted() {
      return this.isEncrypted;
   }

   public boolean wrongPassword() {
      return this.wrongPassword;
   }

   public boolean usesOldExcelFormat() {
      return this.usesOldExcelFormat;
   }

   public boolean isValid() {
      return this.workbook != null;
   }

   public String getTemplateExportVersion() {
      return this.templateExportVersion;
   }

   public boolean hasApplicationTemplate() {
      return this.hasNamedRangeValue("templateType", "Application");
   }

   public boolean hasComponentTemplate() {
      return this.hasNamedRangeValue("templateType", "Component");
   }

   public boolean hasDeviceTemplate() {
      return this.hasNamedRangeValue("templateType", "Device");
   }

   public boolean hasPrivateData() {
      return this.keepPrivate;
   }

   public Workbook getWorkbook() {
      return this.workbook;
   }

   private BulkDeployWorkbook(String name, Object workbookSource, String password) throws IOException {
      this.name = name;
      this.workbook = null;
      this.workbookStream = null;
      boolean usePassword = false;
      boolean failed = false;
      boolean encrypted = false;

      do {
         try {
            if (workbookSource instanceof File) {
               this.workbook = ExcelUtils.createWorkbook((File)workbookSource, usePassword ? password : null);
            } else if (workbookSource instanceof InputStream) {
               if (this.workbookStream == null) {
                  InputStream is = (InputStream)workbookSource;
                  ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                  for (int b = is.read(); b != -1; b = is.read()) {
                     byteArrayOutputStream.write(b);
                  }

                  this.workbookStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                  this.workbookStream.mark(this.workbookStream.available());
               } else {
                  this.workbookStream.reset();
               }

               this.workbook = ExcelUtils.createWorkbook(this.workbookStream, usePassword ? password : null);
            }
         } catch (EncryptedDocumentException var10) {
            this.workbook = null;
            if (usePassword) {
               failed = true;
            } else {
               encrypted = true;
            }
         }

         usePassword = true;
      } while (this.workbook == null && password != null && !failed);

      this.isEncrypted = encrypted;
      this.wrongPassword = failed;
      this.usesOldExcelFormat = !ExcelUtils.isXmlFormat(this.workbook);
      this.templateExportVersion = this.loadTemplateExportVersion();
      if (this.templateExportVersion == null) {
         this.close();
         System.gc();
         this.workbook = null;
      }

      this.keepPrivate = this.loadKeepPrivateFlag();
   }

   private String loadTemplateExportVersion() {
      return this.getWorkbookConstant("version");
   }

   private boolean loadKeepPrivateFlag() {
      return Boolean.parseBoolean(this.getWorkbookConstant("keepPrivate"));
   }

   private String getWorkbookConstant(String name) {
      return this.getSheetOrWorkbookConstant(-1, name);
   }

   private String getSheetOrWorkbookConstant(int sheetIndex, String name) {
      Name constantName = this.getNamedRange(sheetIndex, name);
      return constantName == null ? null : extractStringValue(constantName.getRefersToFormula());
   }

   private Name getNamedRange(int sheetIndex, String nameName) {
      Name foundName = null;
      if (this.workbook != null) {
         for (Name name : this.workbook.getNames(nameName)) {
            if (name.getSheetIndex() == sheetIndex) {
               foundName = name;
               break;
            }
         }
      }

      return foundName;
   }

   private boolean hasNamedRangeValue(String nameName, String nameValue) {
      if (this.workbook == null) {
         return false;
      } else {
         for (Name name : this.workbook.getNames(nameName)) {
            if (Objects.equals(nameValue, extractStringValue(name.getRefersToFormula()))) {
               return true;
            }
         }

         return false;
      }
   }

   private static String extractStringValue(String formula) {
      return formula != null && formula.length() >= 2 && formula.startsWith("\"") && formula.endsWith("\"") ? formula.substring(1, formula.length() - 1) : null;
   }
}
