package com.tridium.excel;

import java.util.Iterator;

public interface Row {
   Sheet getSheet();

   int getRowNum();

   Iterator<Cell> cellIterator();

   Cell createCell(int var1);

   Cell getCell(int var1);
}
