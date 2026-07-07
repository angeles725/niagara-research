package com.tridium.excel;

public interface Sheet extends ExcelFileObject {
   Workbook getWorkbook();

   String getSheetName();

   void setDefaultColumnStyle(int var1, CellStyle var2);

   Row createRow(int var1);

   int getFirstRowNum();

   int getLastRowNum();

   int getPhysicalNumberOfRows();

   Row getRow(int var1);

   int addMergedRegion(CellRangeAddress var1);

   void autoSizeColumn(int var1);

   void setColumnWidth(int var1, int var2);

   void createFreezePane(int var1, int var2);

   Drawing createDrawingPatriarch();

   void lockSelectLockedCells(boolean var1);

   void lockSelectUnlockedCells(boolean var1);

   void lockFormatColumns(boolean var1);

   void lockFormatRows(boolean var1);

   void lockFormatCells(boolean var1);

   void lockInsertColumns(boolean var1);

   void lockInsertRows(boolean var1);

   void lockInsertHyperlinks(boolean var1);

   void lockDeleteColumns(boolean var1);

   void lockDeleteRows(boolean var1);

   void lockSort(boolean var1);

   void lockAutoFilter(boolean var1);

   void lockPivotTables(boolean var1);

   void lockObjects(boolean var1);

   void lockScenarios(boolean var1);

   void enableLocking();

   CTCol addCTCol();
}
