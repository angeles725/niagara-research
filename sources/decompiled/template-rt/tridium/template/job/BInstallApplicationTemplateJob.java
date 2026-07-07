package com.tridium.template.job;

import com.tridium.template.application.ApplicationTemplateInstaller;
import com.tridium.template.application.ApplicationTemplateUtil;
import com.tridium.template.application.JobProgressTracker;
import javax.baja.job.BSimpleJob;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "templateFileOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "componentsToBeRemoved",
      type = "BOrdList",
      defaultValue = "BOrdList.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "checkModulesBeforeInstall",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "upgradeMode",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   )})
public class BInstallApplicationTemplateJob extends BSimpleJob {
   public static final Property templateFileOrd = newProperty(1, BOrd.DEFAULT, null);
   public static final Property componentsToBeRemoved = newProperty(1, BOrdList.DEFAULT, null);
   public static final Property checkModulesBeforeInstall = newProperty(1, false, null);
   public static final Property upgradeMode = newProperty(1, false, null);
   public static final Type TYPE = Sys.loadType(BInstallApplicationTemplateJob.class);

   public BOrd getTemplateFileOrd() {
      return (BOrd)this.get(templateFileOrd);
   }

   public void setTemplateFileOrd(BOrd v) {
      this.set(templateFileOrd, v, null);
   }

   public BOrdList getComponentsToBeRemoved() {
      return (BOrdList)this.get(componentsToBeRemoved);
   }

   public void setComponentsToBeRemoved(BOrdList v) {
      this.set(componentsToBeRemoved, v, null);
   }

   public boolean getCheckModulesBeforeInstall() {
      return this.getBoolean(checkModulesBeforeInstall);
   }

   public void setCheckModulesBeforeInstall(boolean v) {
      this.setBoolean(checkModulesBeforeInstall, v, null);
   }

   public boolean getUpgradeMode() {
      return this.getBoolean(upgradeMode);
   }

   public void setUpgradeMode(boolean v) {
      this.setBoolean(upgradeMode, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void run(Context cx) throws Exception {
      if (!ApplicationTemplateUtil.isSuperUser(cx)) {
         throw new LocalizableRuntimeException(this.getType().getTypeInfo().getModuleName(), "installApplicationTemplateJob.notASuperUser");
      } else if (this.getTemplateFileOrd().isNull()) {
         throw new IllegalStateException();
      } else {
         this.log().start(this.getType().getTypeInfo().getModuleName(), "installApplicationTemplateJob.start", null);

         try (ApplicationTemplateInstaller installer = new ApplicationTemplateInstaller(
               this.getTemplateFileOrd(), this.getComponentsToBeRemoved(), new JobProgressTracker(this)
            )) {
            if (this.getCheckModulesBeforeInstall() && !installer.checkForCompatibleModules()) {
               throw new LocalizableRuntimeException(this.getType().getTypeInfo().getModuleName(), "installApplicationTemplateJob.compatibility.failed");
            } else {
               if (this.getUpgradeMode()) {
                  installer.upgrade(cx);
               } else {
                  installer.install(cx);
               }
            }
         } catch (Throwable var15) {
            this.log().endFailed(var15);
            throw var15;
         }
      }
   }
}
