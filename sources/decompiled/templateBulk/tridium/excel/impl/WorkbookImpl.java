package com.tridium.excel.impl;

import com.tridium.excel.CellStyle;
import com.tridium.excel.CreationHelper;
import com.tridium.excel.Font;
import com.tridium.excel.Workbook;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class WorkbookImpl implements Workbook {
   final org.apache.poi.ss.usermodel.Workbook workbook;
   final XSSFWorkbook xmlWorkbook;

   WorkbookImpl(org.apache.poi.ss.usermodel.Workbook workbook) {
      this.workbook = workbook;
      this.xmlWorkbook = workbook instanceof XSSFWorkbook ? (XSSFWorkbook)workbook : null;
   }

   public void close() throws IOException {
      this.workbook.close();
   }

   public com.tridium.excel.Sheet createSheet() {
      Sheet sheet = this.workbook.createSheet();
      return sheet == null ? null : new SheetImpl(sheet);
   }

   public com.tridium.excel.Sheet createSheet(String sheetName) {
      Sheet sheet = this.workbook.createSheet(sheetName);
      return sheet == null ? null : new SheetImpl(sheet);
   }

   public int getNumberOfSheets() {
      return this.workbook.getNumberOfSheets();
   }

   public int getActiveSheetIndex() {
      return this.workbook.getActiveSheetIndex();
   }

   public int getSheetIndex(String sheetName) {
      return this.workbook.getSheetIndex(sheetName);
   }

   public com.tridium.excel.Sheet getSheetAt(int index) {
      Sheet sheet = this.workbook.getSheetAt(index);
      return sheet == null ? null : new SheetImpl(sheet);
   }

   public Iterator<com.tridium.excel.Sheet> sheetIterator() {
      return new WorkbookImpl.SheetIterator(this.workbook.sheetIterator());
   }

   public com.tridium.excel.Name createName() {
      Name name = this.workbook.createName();
      return name == null ? null : new NameImpl(name);
   }

   public List<? extends com.tridium.excel.Name> getNames(String name) {
      return new WorkbookImpl.NameList(this.workbook.getNames(name));
   }

   public Font createFont() {
      org.apache.poi.ss.usermodel.Font font = this.workbook.createFont();
      return font == null ? null : new FontImpl(font);
   }

   public CreationHelper getCreationHelper() {
      org.apache.poi.ss.usermodel.CreationHelper helper = this.workbook.getCreationHelper();
      return helper == null ? null : new CreationHelperImpl(helper);
   }

   public CellStyle createCellStyle() {
      org.apache.poi.ss.usermodel.CellStyle cellStyle = this.workbook.createCellStyle();
      return cellStyle == null ? null : new CellStyleImpl(cellStyle);
   }

   public void write(OutputStream stream) throws IOException {
      this.workbook.write(stream);
   }

   public boolean isXmlFormat() {
      return this.xmlWorkbook != null;
   }

   @Override
   public String toString() {
      return this.workbook.toString();
   }

   public org.apache.poi.ss.usermodel.Workbook getPoiWorkbook() {
      return this.workbook;
   }

   static class NameList extends AbstractSequentialList<com.tridium.excel.Name> {
      final List<? extends Name> names;

      NameList(List<? extends Name> names) {
         this.names = names;
      }

      @Override
      public ListIterator<com.tridium.excel.Name> listIterator(int index) {
         return new WorkbookImpl.NameListIterator(this.names.listIterator());
      }

      @Override
      public int size() {
         return this.names.size();
      }
   }

   static class NameListIterator implements ListIterator<com.tridium.excel.Name> {
      final ListIterator<? extends Name> it;

      NameListIterator(ListIterator<? extends Name> it) {
         this.it = it;
      }

      @Override
      public boolean hasNext() {
         return this.it.hasNext();
      }

      public com.tridium.excel.Name next() {
         return new NameImpl(this.it.next());
      }

      @Override
      public boolean hasPrevious() {
         return this.it.hasPrevious();
      }

      public com.tridium.excel.Name previous() {
         return new NameImpl(this.it.previous());
      }

      @Override
      public int nextIndex() {
         return this.it.nextIndex();
      }

      @Override
      public int previousIndex() {
         return this.it.previousIndex();
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }

      public void set(com.tridium.excel.Name name) {
         throw new UnsupportedOperationException();
      }

      public void add(com.tridium.excel.Name name) {
         throw new UnsupportedOperationException();
      }
   }

   static class SheetIterator implements Iterator<com.tridium.excel.Sheet> {
      final Iterator<Sheet> it;

      SheetIterator(Iterator<Sheet> it) {
         this.it = it;
      }

      @Override
      public boolean hasNext() {
         return this.it.hasNext();
      }

      public com.tridium.excel.Sheet next() {
         return new SheetImpl(this.it.next());
      }
   }
}
