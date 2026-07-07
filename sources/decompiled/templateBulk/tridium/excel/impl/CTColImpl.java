package com.tridium.excel.impl;

import com.tridium.excel.CTCol;

public class CTColImpl implements CTCol {
   final org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol ctCol;

   CTColImpl(org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol ctCol) {
      this.ctCol = ctCol;
   }

   public void setMin(long min) {
      this.ctCol.setMin(min);
   }

   public void setMax(long max) {
      this.ctCol.setMax(max);
   }

   public void setStyle(long style) {
      this.ctCol.setStyle(style);
   }

   @Override
   public String toString() {
      return this.ctCol.toString();
   }
}
