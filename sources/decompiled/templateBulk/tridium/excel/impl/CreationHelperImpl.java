package com.tridium.excel.impl;

import com.tridium.excel.ClientAnchor;
import com.tridium.excel.CreationHelper;
import com.tridium.excel.DataFormat;
import com.tridium.excel.RichTextString;

public class CreationHelperImpl implements CreationHelper {
   final org.apache.poi.ss.usermodel.CreationHelper creationhelper;

   public CreationHelperImpl(org.apache.poi.ss.usermodel.CreationHelper creationHelper) {
      this.creationhelper = creationHelper;
   }

   public ClientAnchor createClientAnchor() {
      org.apache.poi.ss.usermodel.ClientAnchor anchor = this.creationhelper.createClientAnchor();
      return anchor == null ? null : new ClientAnchorImpl(anchor);
   }

   public RichTextString createRichTextString(String text) {
      org.apache.poi.ss.usermodel.RichTextString rts = this.creationhelper.createRichTextString(text);
      return rts == null ? null : new RichTextStringImpl(rts);
   }

   public DataFormat createDataFormat() {
      org.apache.poi.ss.usermodel.DataFormat dataFormat = this.creationhelper.createDataFormat();
      return dataFormat == null ? null : new DataFormatImpl(dataFormat);
   }

   @Override
   public String toString() {
      return this.creationhelper.toString();
   }
}
