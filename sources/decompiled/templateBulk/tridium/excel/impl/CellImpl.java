package com.tridium.excel.impl;

import com.tridium.excel.Cell;
import com.tridium.excel.CellStyle;
import com.tridium.excel.CellType;
import com.tridium.excel.Comment;
import com.tridium.excel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;

public class CellImpl implements Cell {
   final org.apache.poi.ss.usermodel.Cell cell;
   final XSSFCell xmlCell;

   CellImpl(org.apache.poi.ss.usermodel.Cell cell) {
      this.cell = cell;
      this.xmlCell = cell instanceof XSSFCell ? (XSSFCell)cell : null;
   }

   public int getColumnIndex() {
      return this.cell.getColumnIndex();
   }

   public Row getRow() {
      org.apache.poi.ss.usermodel.Row row = this.cell.getRow();
      return row == null ? null : new RowImpl(row);
   }

   public CellType getCellType() {
      switch (this.cell.getCellType()) {
         case BLANK:
            return CellType.BLANK;
         case STRING:
            return CellType.STRING;
         case BOOLEAN:
            return CellType.BOOLEAN;
         case NUMERIC:
         default:
            return CellType.NUMERIC;
      }
   }

   public void setBlank() {
      this.cell.setBlank();
   }

   public void setCellValue(double value) {
      this.cell.setCellValue(value);
   }

   public void setCellValue(String value) {
      this.cell.setCellValue(value);
   }

   public void setCellStyle(CellStyle style) {
      this.cell.setCellStyle(((CellStyleImpl)style).cellStyle);
   }

   public void setCellComment(Comment comment) {
      this.cell.setCellComment(((CommentImpl)comment).comment);
   }

   public String getStringCellValue() {
      return this.cell.getStringCellValue();
   }

   public double getNumericCellValue() {
      return this.cell.getNumericCellValue();
   }

   public boolean getBooleanCellValue() {
      return this.cell.getBooleanCellValue();
   }

   public boolean isXmlFormat() {
      return this.xmlCell != null;
   }

   @Override
   public String toString() {
      return this.cell.toString();
   }
}
