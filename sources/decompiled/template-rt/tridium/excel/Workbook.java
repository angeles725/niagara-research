package com.tridium.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

public interface Workbook extends AutoCloseable, ExcelFileObject {
   @Override
   void close() throws IOException;

   Sheet createSheet();

   Sheet createSheet(String var1);

   int getNumberOfSheets();

   int getActiveSheetIndex();

   int getSheetIndex(String var1);

   Sheet getSheetAt(int var1);

   Iterator<Sheet> sheetIterator();

   Name createName();

   List<? extends Name> getNames(String var1);

   Font createFont();

   CreationHelper getCreationHelper();

   CellStyle createCellStyle();

   void write(OutputStream var1) throws IOException;
}
