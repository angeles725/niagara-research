package com.tridium.excel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface Factory {
   Workbook createWorkbook(boolean var1) throws IOException;

   Workbook createWorkbook(File var1, String var2) throws EncryptedDocumentException, IOException;

   Workbook createWorkbook(InputStream var1, String var2) throws EncryptedDocumentException, IOException;

   ExcelFileSystem makeFileSystem();

   void setCurrentUserPassword(String var1);

   EncryptionInfo makeEncryptionInfo();

   CellRangeAddress makeCellRangeAddress(int var1, int var2, int var3, int var4);

   DataFormatter makeDataFormatter();

   short getColorIndex(IndexedColors var1);

   short getBuiltinFormat(String var1);
}
