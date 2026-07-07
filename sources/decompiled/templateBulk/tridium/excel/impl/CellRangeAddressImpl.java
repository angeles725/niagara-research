package com.tridium.excel.impl;

import com.tridium.excel.CellRangeAddress;

public class CellRangeAddressImpl implements CellRangeAddress {
   final org.apache.poi.ss.util.CellRangeAddress cellRangeAddress;

   CellRangeAddressImpl(org.apache.poi.ss.util.CellRangeAddress cellRangeAddress) {
      this.cellRangeAddress = cellRangeAddress;
   }

   @Override
   public String toString() {
      return this.cellRangeAddress.toString();
   }
}
