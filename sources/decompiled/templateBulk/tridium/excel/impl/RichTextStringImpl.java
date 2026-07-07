package com.tridium.excel.impl;

import com.tridium.excel.RichTextString;

public class RichTextStringImpl implements RichTextString {
   final org.apache.poi.ss.usermodel.RichTextString string;

   RichTextStringImpl(org.apache.poi.ss.usermodel.RichTextString string) {
      this.string = string;
   }

   @Override
   public String toString() {
      return this.string.toString();
   }
}
