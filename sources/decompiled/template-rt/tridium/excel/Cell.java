package com.tridium.excel;

public interface Cell extends ExcelFileObject {
   int getColumnIndex();

   Row getRow();

   CellType getCellType();

   void setBlank();

   void setCellValue(double var1);

   void setCellValue(String var1);

   void setCellStyle(CellStyle var1);

   void setCellComment(Comment var1);

   String getStringCellValue();

   double getNumericCellValue();

   boolean getBooleanCellValue();
}
