package com.tridium.excel.impl;

import com.tridium.excel.Row;
import com.tridium.excel.Sheet;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Cell;

public class RowImpl implements Row {
   final org.apache.poi.ss.usermodel.Row row;

   RowImpl(org.apache.poi.ss.usermodel.Row row) {
      this.row = row;
   }

   public Sheet getSheet() {
      org.apache.poi.ss.usermodel.Sheet sheet = this.row.getSheet();
      return sheet == null ? null : new SheetImpl(sheet);
   }

   public int getRowNum() {
      return this.row.getRowNum();
   }

   public Iterator<com.tridium.excel.Cell> cellIterator() {
      return new RowImpl.CellIterator(this.row.cellIterator());
   }

   public com.tridium.excel.Cell createCell(int column) {
      Cell cell = this.row.createCell(column);
      return cell == null ? null : new CellImpl(cell);
   }

   public com.tridium.excel.Cell getCell(int cellNum) {
      Cell cell = this.row.getCell(cellNum);
      return cell == null ? null : new CellImpl(cell);
   }

   @Override
   public String toString() {
      return this.row.toString();
   }

   static class CellIterator implements Iterator<com.tridium.excel.Cell> {
      final Iterator<Cell> it;

      CellIterator(Iterator<Cell> it) {
         this.it = it;
      }

      @Override
      public boolean hasNext() {
         return this.it.hasNext();
      }

      public com.tridium.excel.Cell next() {
         return new CellImpl(this.it.next());
      }
   }
}
