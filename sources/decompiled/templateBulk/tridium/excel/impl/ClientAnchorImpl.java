package com.tridium.excel.impl;

import com.tridium.excel.ClientAnchor;

public class ClientAnchorImpl implements ClientAnchor {
   final org.apache.poi.ss.usermodel.ClientAnchor anchor;

   ClientAnchorImpl(org.apache.poi.ss.usermodel.ClientAnchor anchor) {
      this.anchor = anchor;
   }

   public void setCol1(int col1) {
      this.anchor.setCol1(col1);
   }

   public void setCol2(int col2) {
      this.anchor.setCol2(col2);
   }

   public void setRow1(int row1) {
      this.anchor.setRow1(row1);
   }

   public void setRow2(int row2) {
      this.anchor.setRow2(row2);
   }

   @Override
   public String toString() {
      return this.anchor.toString();
   }
}
