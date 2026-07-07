package com.tridium.excel.impl;

import com.tridium.excel.CTCol;
import com.tridium.excel.CellRangeAddress;
import com.tridium.excel.CellStyle;
import com.tridium.excel.Row;
import com.tridium.excel.Sheet;
import com.tridium.excel.Workbook;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class SheetImpl implements Sheet {
   final org.apache.poi.ss.usermodel.Sheet sheet;
   final XSSFSheet xmlSheet;

   SheetImpl(org.apache.poi.ss.usermodel.Sheet sheet) {
      this.sheet = sheet;
      this.xmlSheet = sheet instanceof XSSFSheet ? (XSSFSheet)sheet : null;
   }

   public Workbook getWorkbook() {
      org.apache.poi.ss.usermodel.Workbook workbook = this.sheet.getWorkbook();
      return workbook == null ? null : new WorkbookImpl(workbook);
   }

   public String getSheetName() {
      return this.sheet.getSheetName();
   }

   public void setDefaultColumnStyle(int column, CellStyle style) {
      this.sheet.setDefaultColumnStyle(column, ((CellStyleImpl)style).cellStyle);
   }

   public Row createRow(int rowNum) {
      org.apache.poi.ss.usermodel.Row row = this.sheet.createRow(rowNum);
      return row == null ? null : new RowImpl(row);
   }

   public int getFirstRowNum() {
      return this.sheet.getFirstRowNum();
   }

   public int getLastRowNum() {
      return this.sheet.getLastRowNum();
   }

   public int getPhysicalNumberOfRows() {
      return this.sheet.getPhysicalNumberOfRows();
   }

   public Row getRow(int rowNum) {
      org.apache.poi.ss.usermodel.Row row = this.sheet.getRow(rowNum);
      return row == null ? null : new RowImpl(row);
   }

   public int addMergedRegion(CellRangeAddress region) {
      return this.sheet.addMergedRegion(((CellRangeAddressImpl)region).cellRangeAddress);
   }

   public void autoSizeColumn(int column) {
      this.sheet.autoSizeColumn(column);
   }

   public void setColumnWidth(int column, int width) {
      this.sheet.setColumnWidth(column, width);
   }

   public void createFreezePane(int colSplit, int rowSplit) {
      this.sheet.createFreezePane(colSplit, rowSplit);
   }

   public com.tridium.excel.Drawing createDrawingPatriarch() {
      Drawing<?> drawing = this.sheet.createDrawingPatriarch();
      return drawing == null ? null : new DrawingImpl(drawing);
   }

   public void lockSelectLockedCells(boolean enabled) {
      this.xmlSheet.lockSelectLockedCells(enabled);
   }

   public void lockSelectUnlockedCells(boolean enabled) {
      this.xmlSheet.lockSelectUnlockedCells(enabled);
   }

   public void lockFormatColumns(boolean enabled) {
      this.xmlSheet.lockFormatColumns(enabled);
   }

   public void lockFormatRows(boolean enabled) {
      this.xmlSheet.lockFormatRows(enabled);
   }

   public void lockFormatCells(boolean enabled) {
      this.xmlSheet.lockFormatCells(enabled);
   }

   public void lockInsertColumns(boolean enabled) {
      this.xmlSheet.lockInsertColumns(enabled);
   }

   public void lockInsertRows(boolean enabled) {
      this.xmlSheet.lockInsertRows(enabled);
   }

   public void lockInsertHyperlinks(boolean enabled) {
      this.xmlSheet.lockInsertHyperlinks(enabled);
   }

   public void lockDeleteColumns(boolean enabled) {
      this.xmlSheet.lockDeleteColumns(enabled);
   }

   public void lockDeleteRows(boolean enabled) {
      this.xmlSheet.lockDeleteRows(enabled);
   }

   public void lockSort(boolean enabled) {
      this.xmlSheet.lockSort(enabled);
   }

   public void lockAutoFilter(boolean enabled) {
      this.xmlSheet.lockAutoFilter(enabled);
   }

   public void lockPivotTables(boolean enabled) {
      this.xmlSheet.lockPivotTables(enabled);
   }

   public void lockObjects(boolean enabled) {
      this.xmlSheet.lockObjects(enabled);
   }

   public void lockScenarios(boolean enabled) {
      this.xmlSheet.lockScenarios(enabled);
   }

   public void enableLocking() {
      this.xmlSheet.enableLocking();
   }

   public CTCol addCTCol() {
      org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol ctCol = this.xmlSheet.getCTWorksheet().getColsArray(0).addNewCol();
      return ctCol == null ? null : new CTColImpl(ctCol);
   }

   public boolean isXmlFormat() {
      return this.xmlSheet != null;
   }

   @Override
   public String toString() {
      return this.sheet.toString();
   }
}
