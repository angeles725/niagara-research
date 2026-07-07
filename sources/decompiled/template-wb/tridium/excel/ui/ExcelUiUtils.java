package com.tridium.excel.ui;

import com.tridium.excel.ExcelUtils;
import com.tridium.template.BTemplateService;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.util.Lexicon;

public class ExcelUiUtils {
   private static final Lexicon lex = Lexicon.make("template");

   public static boolean informIfNoExcelSupportIsInstalledLocally(BWidget parent) {
      if (!ExcelUtils.isExcelSupportInstalled()) {
         BDialog.info(parent, lex.getText("excelSupport.missingTitle"), lex.getText("excelSupport.missingWb"));
         return true;
      } else {
         return false;
      }
   }

   public static boolean informIfNoExcelSupportIsInstalledInStation(BWidget parent, BTemplateService templateService) {
      if (!templateService.getIsBulkOperationSupported()) {
         BDialog.info(parent, lex.getText("excelSupport.missingTitle"), lex.getText("excelSupport.missingStation"));
         return true;
      } else {
         return false;
      }
   }

   private ExcelUiUtils() {
   }
}
