package com.tridium.excel.impl;

import com.tridium.excel.CellStyle;
import com.tridium.excel.FillPatternType;
import com.tridium.excel.Font;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

public class CellStyleImpl implements CellStyle {
   final org.apache.poi.ss.usermodel.CellStyle cellStyle;
   final XSSFCellStyle xmlCellStyle;

   CellStyleImpl(org.apache.poi.ss.usermodel.CellStyle cellStyle) {
      this.cellStyle = cellStyle;
      this.xmlCellStyle = cellStyle instanceof XSSFCellStyle ? (XSSFCellStyle)cellStyle : null;
   }

   public int getIndex() {
      return this.cellStyle.getIndex();
   }

   public void setFont(Font font) {
      this.cellStyle.setFont(((FontImpl)font).font);
   }

   public void setLocked(boolean locked) {
      this.cellStyle.setLocked(locked);
   }

   public void setQuotePrefixed(boolean quotePrefix) {
      this.cellStyle.setQuotePrefixed(quotePrefix);
   }

   public void setDataFormat(short fmt) {
      this.cellStyle.setDataFormat(fmt);
   }

   public void setHidden(boolean hidden) {
      this.cellStyle.setHidden(hidden);
   }

   public void setFillForegroundColor(short color) {
      this.cellStyle.setFillForegroundColor(color);
   }

   public void setFillPattern(FillPatternType fp) {
      if (fp == FillPatternType.SOLID_FOREGROUND) {
         this.cellStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
      }
   }

   public boolean isXmlFormat() {
      return this.xmlCellStyle != null;
   }

   @Override
   public String toString() {
      return this.cellStyle.toString();
   }
}
