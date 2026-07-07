package com.tridium.excel;

public interface CellStyle extends ExcelFileObject {
   int getIndex();

   void setFont(Font var1);

   void setLocked(boolean var1);

   void setQuotePrefixed(boolean var1);

   void setDataFormat(short var1);

   void setHidden(boolean var1);

   void setFillForegroundColor(short var1);

   void setFillPattern(FillPatternType var1);
}
