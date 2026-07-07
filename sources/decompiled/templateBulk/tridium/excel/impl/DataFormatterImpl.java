package com.tridium.excel.impl;

import com.tridium.excel.Cell;
import com.tridium.excel.DataFormatter;

public class DataFormatterImpl implements DataFormatter {
   final org.apache.poi.ss.usermodel.DataFormatter dataFormatter;

   DataFormatterImpl(org.apache.poi.ss.usermodel.DataFormatter dataFormatter) {
      this.dataFormatter = dataFormatter;
   }

   public String formatCellValue(Cell cell) {
      return this.dataFormatter.formatCellValue(((CellImpl)cell).cell);
   }

   @Override
   public String toString() {
      return this.dataFormatter.toString();
   }
}
