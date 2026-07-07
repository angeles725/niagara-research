package com.tridium.excel.impl;

import com.tridium.excel.Name;

public class NameImpl implements Name {
   final org.apache.poi.ss.usermodel.Name name;

   NameImpl(org.apache.poi.ss.usermodel.Name name) {
      this.name = name;
   }

   public void setSheetIndex(int sheetId) {
      this.name.setSheetIndex(sheetId);
   }

   public int getSheetIndex() {
      return this.name.getSheetIndex();
   }

   public void setNameName(String s) {
      this.name.setNameName(s);
   }

   public void setRefersToFormula(String formulaText) {
      this.name.setRefersToFormula(formulaText);
   }

   public String getRefersToFormula() {
      return this.name.getRefersToFormula();
   }

   @Override
   public String toString() {
      return this.name.toString();
   }
}
