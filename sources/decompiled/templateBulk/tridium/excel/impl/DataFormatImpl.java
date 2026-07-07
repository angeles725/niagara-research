package com.tridium.excel.impl;

import com.tridium.excel.DataFormat;

public class DataFormatImpl implements DataFormat {
   final org.apache.poi.ss.usermodel.DataFormat dataFormat;

   DataFormatImpl(org.apache.poi.ss.usermodel.DataFormat dataFormat) {
      this.dataFormat = dataFormat;
   }

   public short getFormat(String format) {
      return this.dataFormat.getFormat(format);
   }

   @Override
   public String toString() {
      return this.dataFormat.toString();
   }
}
