package com.tridium.excel.impl;

import com.tridium.excel.CellRangeAddress;
import com.tridium.excel.DataFormatter;
import com.tridium.excel.EncryptedDocumentException;
import com.tridium.excel.EncryptionInfo;
import com.tridium.excel.ExcelFileSystem;
import com.tridium.excel.Factory;
import com.tridium.excel.IndexedColors;
import com.tridium.excel.Workbook;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFWorkbookFactory;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;

public class FactoryImpl implements Factory {
   private static final FactoryImpl INSTANCE = new FactoryImpl();

   public static Factory instance() {
      return INSTANCE;
   }

   public Workbook createWorkbook(boolean xmlFormat) throws IOException {
      org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(xmlFormat);
      return workbook == null ? null : new WorkbookImpl(workbook);
   }

   public Workbook createWorkbook(File file, String password) throws EncryptedDocumentException, IOException {
      try {
         org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(file, password);
         return workbook == null ? null : new WorkbookImpl(workbook);
      } catch (org.apache.poi.EncryptedDocumentException var4) {
         throw new EncryptedDocumentException(var4);
      }
   }

   public Workbook createWorkbook(InputStream inp, String password) throws EncryptedDocumentException, IOException {
      try {
         org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(inp, password);
         return workbook == null ? null : new WorkbookImpl(workbook);
      } catch (org.apache.poi.EncryptedDocumentException var4) {
         throw new EncryptedDocumentException(var4);
      }
   }

   public ExcelFileSystem makeFileSystem() {
      return new ExcelFileSystemImpl(new POIFSFileSystem());
   }

   public void setCurrentUserPassword(String password) {
      Biff8EncryptionKey.setCurrentUserPassword(password);
   }

   public EncryptionInfo makeEncryptionInfo() {
      return new EncryptionInfoImpl(new org.apache.poi.poifs.crypt.EncryptionInfo(EncryptionMode.agile));
   }

   public CellRangeAddress makeCellRangeAddress(int firstRow, int lastRow, int firstCol, int lastCol) {
      return new CellRangeAddressImpl(new org.apache.poi.ss.util.CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
   }

   public DataFormatter makeDataFormatter() {
      return new DataFormatterImpl(new org.apache.poi.ss.usermodel.DataFormatter());
   }

   public short getColorIndex(IndexedColors color) {
      switch (color) {
         case DARK_BLUE:
            return org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex();
         case GREY_25_PERCENT:
            return org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex();
         case GREY_40_PERCENT:
            return org.apache.poi.ss.usermodel.IndexedColors.GREY_40_PERCENT.getIndex();
         case PALE_BLUE:
            return org.apache.poi.ss.usermodel.IndexedColors.PALE_BLUE.getIndex();
         case SKY_BLUE:
            return org.apache.poi.ss.usermodel.IndexedColors.SKY_BLUE.getIndex();
         case LIGHT_GREEN:
            return org.apache.poi.ss.usermodel.IndexedColors.LIGHT_GREEN.getIndex();
         case LIGHT_YELLOW:
            return org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.getIndex();
         case GOLD:
            return org.apache.poi.ss.usermodel.IndexedColors.GOLD.getIndex();
         case LEMON_CHIFFON:
            return org.apache.poi.ss.usermodel.IndexedColors.LEMON_CHIFFON.getIndex();
         default:
            return 0;
      }
   }

   public short getBuiltinFormat(String pFmt) {
      return (short)BuiltinFormats.getBuiltinFormat(pFmt);
   }

   static {
      WorkbookFactory.addProvider(new HSSFWorkbookFactory());
      WorkbookFactory.addProvider(new XSSFWorkbookFactory());
   }
}
