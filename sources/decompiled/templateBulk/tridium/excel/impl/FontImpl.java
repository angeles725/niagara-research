package com.tridium.excel.impl;

import com.tridium.excel.Font;

public class FontImpl implements Font {
   final org.apache.poi.ss.usermodel.Font font;

   FontImpl(org.apache.poi.ss.usermodel.Font font) {
      this.font = font;
   }

   public void setBold(boolean bold) {
      this.font.setBold(bold);
   }

   public void setColor(short colorIndex) {
      this.font.setColor(colorIndex);
   }

   @Override
   public String toString() {
      return this.font.toString();
   }
}
