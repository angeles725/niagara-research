package com.tridium.template.ui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.baja.sys.BComponent;
import javax.baja.ui.BProgressDialog.Worker;

public class TemplateDeployWorker extends Worker {
   private final BulkDeployWorkbook workbook;
   private final BComponent root;
   private boolean isRunning;
   private boolean canceled;
   private final BulkDeployUtil deployUtil;
   private Map<String, List<String>> deployMessages;

   public TemplateDeployWorker(BulkDeployWorkbook workbook, BComponent root, BulkDeployUtil deployUtil) {
      this.workbook = workbook;
      this.root = root;
      this.deployUtil = deployUtil;
   }

   public void doRun() {
      this.isRunning = true;
      this.canceled = false;
      this.deployUtil.deployTemplatesFromExcel(this.workbook, this.root, this);
      this.isRunning = false;
   }

   public void doCancel() {
      this.isRunning = false;
      this.canceled = true;
   }

   public String getMaxMessage() {
      Map<String, String> templateTitles = BulkDeployUtil.getTemplateTitlesFromExcel(this.workbook);
      AtomicReference<String> message = new AtomicReference<>();
      message.set("");
      templateTitles.forEach((title, deployName) -> {
         String progressMessage = BulkDeployUtil.lex.getText("bulkDeploy.progress.update.config", new Object[]{title, deployName});
         if (progressMessage.length() > message.get().length()) {
            message.set(progressMessage);
         }
      });
      return message.get();
   }

   public void setDialog(BTemplateDeployProgressDialog dialog) {
      this.dialog = dialog;
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   public void setDeployMessages(Map<String, List<String>> messages) {
      if (messages != null) {
         this.deployMessages = messages;
      }
   }

   public Map<String, List<String>> getDeployMessages() {
      return this.deployMessages == null ? Collections.emptyMap() : Collections.unmodifiableMap(this.deployMessages);
   }
}
