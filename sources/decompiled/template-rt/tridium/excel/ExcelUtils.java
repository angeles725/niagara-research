package com.tridium.excel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.baja.sys.BModule;
import javax.baja.sys.ModuleNotFoundException;
import javax.baja.sys.Sys;

public class ExcelUtils {
   private static boolean initialized = false;
   private static Factory factory = null;
   private static Throwable initializationError = null;
   private static final String EXCEL_SUPPORT_MODULE = "templateBulk";
   private static final String FACTORY_CLASS_NAME = "com.tridium.excel.impl.FactoryImpl";
   private static final String FACTORY_INSTANCE_METHOD_NAME = "instance";

   public static boolean isExcelSupportInstalled() {
      initialize();
      return factory != null;
   }

   public static void confirmExcelSupport() {
      if (!isExcelSupportInstalled()) {
         throw new UnsupportedOperationException("Install module templateBulk for Excel file support.", initializationError);
      }
   }

   public static Workbook createWorkbook(boolean xmlFormat) throws IOException {
      confirmExcelSupport();
      return factory.createWorkbook(xmlFormat);
   }

   public static Workbook createWorkbook(File file, String password) throws IOException {
      confirmExcelSupport();
      return factory.createWorkbook(file, password);
   }

   public static Workbook createWorkbook(InputStream inp, String password) throws IOException {
      confirmExcelSupport();
      return factory.createWorkbook(inp, password);
   }

   public static ExcelFileSystem makeFileSystem() {
      confirmExcelSupport();
      return factory.makeFileSystem();
   }

   public static void setCurrentUserPassword(String password) {
      confirmExcelSupport();
      factory.setCurrentUserPassword(password);
   }

   public static EncryptionInfo makeEncryptionInfo() {
      confirmExcelSupport();
      return factory.makeEncryptionInfo();
   }

   public static CellRangeAddress makeCellRangeAddress(int firstRow, int lastRow, int firstCol, int lastCol) {
      confirmExcelSupport();
      return factory.makeCellRangeAddress(firstRow, lastRow, firstCol, lastCol);
   }

   public static DataFormatter makeDataFormatter() {
      confirmExcelSupport();
      return factory.makeDataFormatter();
   }

   public static short getColorIndex(IndexedColors color) {
      confirmExcelSupport();
      return factory.getColorIndex(color);
   }

   public static short getBuiltinFormat(String pFmt) {
      confirmExcelSupport();
      return factory.getBuiltinFormat(pFmt);
   }

   public static boolean isXmlFormat(ExcelFileObject object) {
      return object != null && object.isXmlFormat();
   }

   private ExcelUtils() {
   }

   private static void initialize() {
      if (!initialized) {
         try {
            BModule excelSupportModule = Sys.loadModule("templateBulk");
            Class<?> factoryClass = excelSupportModule.loadClass("com.tridium.excel.impl.FactoryImpl");
            Method instanceMethod = factoryClass.getMethod("instance");
            factory = (Factory)instanceMethod.invoke(null);
         } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ModuleNotFoundException var6) {
            initializationError = var6;
         } finally {
            initialized = true;
         }
      }
   }
}
