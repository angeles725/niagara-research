package com.tridium.template;

import com.tridium.excel.ExcelUtils;
import com.tridium.fox.sys.BFoxChannelRegistry;
import com.tridium.install.BVersion;
import com.tridium.sys.Nre;
import com.tridium.sys.module.ModuleManager;
import com.tridium.sys.module.NModule;
import com.tridium.template.application.BApplicationInstallSpecs;
import com.tridium.template.file.BINtplFile;
import com.tridium.template.job.BInstallApplicationTemplateJob;
import com.tridium.template.job.BMakeApplicationTemplateJob;
import com.tridium.template.job.BMakeStationTemplateJob;
import com.tridium.template.job.BUpgradeTemplateJob;
import com.tridium.util.CompUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.job.BJob;
import javax.baja.license.Feature;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.ModuleInfo;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbstractService;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BRelation;
import javax.baja.sys.BStation;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BIRestrictedComponent;
import javax.baja.util.BUuid;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "uID",
      type = "BUuid",
      defaultValue = "BUuid.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "isBulkOperationSupported",
      type = "boolean",
      defaultValue = "false",
      flags = 7
   )})
@NiagaraActions({@NiagaraAction(
      name = "submitUpgradeJob",
      parameterType = "BVector",
      defaultValue = "new BVector()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "makeStationTemplate",
      parameterType = "BBoolean",
      defaultValue = "BBoolean.make(true)",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "makeApplicationTemplate",
      parameterType = "BBoolean",
      defaultValue = "BBoolean.make(true)",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "installApplication",
      parameterType = "BApplicationInstallSpecs",
      defaultValue = "BApplicationInstallSpecs.make()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "installApplicationTemplate",
      parameterType = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      returnType = "BOrd",
      flags = 4,
      deprecated = true
   ), @NiagaraAction(
      name = "checkAndInstallApplicationTemplate",
      parameterType = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      returnType = "BOrd",
      flags = 4,
      deprecated = true
   ), @NiagaraAction(
      name = "getRemoteModules",
      returnType = "BVector",
      flags = 4
   ), @NiagaraAction(
      name = "updateSignatures",
      flags = 16
   )})
public class BTemplateService extends BAbstractService implements BIRestrictedComponent {
   public static final Property uID = newProperty(1, BUuid.DEFAULT, null);
   public static final Property isBulkOperationSupported = newProperty(7, false, null);
   public static final Action submitUpgradeJob = newAction(4, new BVector(), null);
   public static final Action makeStationTemplate = newAction(4, BBoolean.make(true), null);
   public static final Action makeApplicationTemplate = newAction(4, BBoolean.make(true), null);
   public static final Action installApplication = newAction(4, BApplicationInstallSpecs.make(), null);
   @Deprecated
   public static final Action installApplicationTemplate = newAction(4, BOrd.DEFAULT, null);
   @Deprecated
   public static final Action checkAndInstallApplicationTemplate = newAction(4, BOrd.DEFAULT, null);
   public static final Action getRemoteModules = newAction(4, null);
   public static final Action updateSignatures = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BTemplateService.class);
   private static final Type[] TYPES = new Type[]{TYPE};
   private static final BIcon icon = BIcon.std("files/ntpl.png");
   private static final Lexicon lex = Lexicon.make("template");
   HashMap<Object, BTemplateConfig> templates = new HashMap<>();
   public static final Logger logger = Logger.getLogger("template.service");

   public BUuid getUID() {
      return (BUuid)this.get(uID);
   }

   public void setUID(BUuid v) {
      this.set(uID, v, null);
   }

   public boolean getIsBulkOperationSupported() {
      return this.getBoolean(isBulkOperationSupported);
   }

   public void setIsBulkOperationSupported(boolean v) {
      this.setBoolean(isBulkOperationSupported, v, null);
   }

   public BOrd submitUpgradeJob(BVector parameter) {
      return (BOrd)this.invoke(submitUpgradeJob, parameter, null);
   }

   public BOrd makeStationTemplate(BBoolean parameter) {
      return (BOrd)this.invoke(makeStationTemplate, parameter, null);
   }

   public BOrd makeApplicationTemplate(BBoolean parameter) {
      return (BOrd)this.invoke(makeApplicationTemplate, parameter, null);
   }

   public BOrd installApplication(BApplicationInstallSpecs parameter) {
      return (BOrd)this.invoke(installApplication, parameter, null);
   }

   @Deprecated
   public BOrd installApplicationTemplate(BOrd parameter) {
      return (BOrd)this.invoke(installApplicationTemplate, parameter, null);
   }

   @Deprecated
   public BOrd checkAndInstallApplicationTemplate(BOrd parameter) {
      return (BOrd)this.invoke(checkAndInstallApplicationTemplate, parameter, null);
   }

   public BVector getRemoteModules() {
      return (BVector)this.invoke(getRemoteModules, null, null);
   }

   public void updateSignatures() {
      this.invoke(updateSignatures, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void serviceStarted() {
      this.setIsBulkOperationSupported(ExcelUtils.isExcelSupportInstalled());
      if (this.isOperational()) {
         try {
            BFoxChannelRegistry registry = BFoxChannelRegistry.getPrototype();
            if (registry.get("template") == null) {
               registry.add("template", new BTemplateChannel());
            }
         } catch (Exception var6) {
            logger.severe(() -> "Unable to add BTemplateChannel");
         }

         if (!this.isRunning() || !Sys.atSteadyState()) {
            return;
         }

         this.templates = new HashMap<>();
         BStation station = Sys.getStation();

         for (BTemplateConfig templateConfig : (BTemplateConfig[])CompUtil.getDescendants(station, BTemplateConfig.class)) {
            this.register(templateConfig);
         }
      }
   }

   public void register(BTemplateConfig tmplConfig) {
      if (!this.isOperational()) {
         throw new NotRunningException(lex.get("templateService.notOperational"));
      } else if (!tmplConfig.getPropertyInParent().isFrozen()) {
         this.templates.put(tmplConfig.getHandle(), tmplConfig);
         BTemplateInfo templateInfo = BTemplateInfo.make(tmplConfig);
         Property prop = this.add("t?", templateInfo, 65543);
         BTemplateInfo tInfo = (BTemplateInfo)this.get(prop);
         this.updateSubtemplateRelations(tInfo);
      }
   }

   public void unregister(BTemplateConfig templConfig) {
      if (!this.isOperational()) {
         throw new NotRunningException(lex.get("templateService.notOperational"));
      } else {
         this.templates.remove(templConfig);

         for (BTemplateInfo info : (BTemplateInfo[])this.getChildren(BTemplateInfo.class)) {
            if (info.getLocationOrd().equals(templConfig.getHandleOrd())) {
               this.remove(info);
               break;
            }
         }
      }
   }

   private void updateSubtemplateRelations(BTemplateInfo template) {
      BComponent thisRoot = template.templateConfig.getParent().asComponent();
      SlotPath thisSlotPath = thisRoot.getSlotPath();
      String[] thisPathNames = thisSlotPath.getNames();

      for (BTemplateInfo templateInfo : (BTemplateInfo[])this.getChildren(BTemplateInfo.class)) {
         BComponent tmplRoot = templateInfo.templateConfig.getParent().asComponent();
         if (!tmplRoot.equals(thisRoot) && tmplRoot.isMounted()) {
            String[] tmplSlotPathNames = tmplRoot.getSlotPath().getNames();
            boolean isSubtemplate = true;

            for (int i = 0; i < tmplSlotPathNames.length && i < thisPathNames.length; i++) {
               if (!tmplSlotPathNames[i].equals(thisPathNames[i])) {
                  isSubtemplate = false;
                  break;
               }
            }

            if (isSubtemplate) {
               BRelation relation = new BRelation(TemplateConst.TEMPLATE_SUBTEMPLATE_TAG_ID, template.getHandleOrd());
               templateInfo.add("r?", relation);
            }
         }
      }
   }

   protected final void enabled() {
      this.serviceStarted();
   }

   public Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "template");
   }

   public BOrd doSubmitUpgradeJob(BVector vector) {
      BUpgradeTemplateJob job = new BUpgradeTemplateJob(vector, this);
      return job.submit(null);
   }

   public BOrd doMakeStationTemplate(BBoolean useMinorVersionOnDeployment, Context cx) {
      if (cx.getUser().getPermissions().isSuperUser()) {
         BJob job = new BMakeStationTemplateJob(useMinorVersionOnDeployment.getBoolean());
         return job.submit(cx);
      } else {
         return null;
      }
   }

   public BOrd doMakeApplicationTemplate(BBoolean useMinorVersionOnDeployment, Context cx) {
      if (cx.getUser().getPermissions().isSuperUser()) {
         BJob job = new BMakeApplicationTemplateJob(useMinorVersionOnDeployment.getBoolean());
         return job.submit(cx);
      } else {
         return null;
      }
   }

   public BOrd doInstallApplication(BApplicationInstallSpecs specs, Context cx) {
      BInstallApplicationTemplateJob job = new BInstallApplicationTemplateJob();
      job.setUpgradeMode(specs.getUpgrade());
      job.setCheckModulesBeforeInstall(specs.getCheckModules());
      job.setTemplateFileOrd(specs.getFileOrd());
      job.setComponentsToBeRemoved(specs.getToBeRemoved());
      return job.submit(cx);
   }

   @Deprecated
   public BOrd doInstallApplicationTemplate(BOrd templateFileOrd, Context cx) {
      return this.doInstallApplication(BApplicationInstallSpecs.make(false, false, templateFileOrd, BOrdList.DEFAULT), cx);
   }

   @Deprecated
   public BOrd doCheckAndInstallApplicationTemplate(BOrd templateFileOrd, Context cx) {
      return this.doInstallApplication(BApplicationInstallSpecs.make(false, true, templateFileOrd, BOrdList.DEFAULT), cx);
   }

   public BVector doGetRemoteModules() {
      BVector resultVector = new BVector();
      this.syncStationModules();
      ModuleInfo[] modules = Sys.getRegistry().getModules();

      for (ModuleInfo module : modules) {
         BTemplateValues values = new BTemplateValues();
         values.setModulePartName(module.getModulePartName());
         values.setModuleVersion(new BVersion(module.getVendorVersion().toString()));
         resultVector.add(module.getModulePartName().replace("-", "_"), values);
      }

      return resultVector;
   }

   public void doUpdateSignatures() {
      for (BTemplateInfo tInfo : (BTemplateInfo[])this.getChildren(BTemplateInfo.class)) {
         this.updateSignature(tInfo);
      }
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.fox
      )}
   )
   public static Boolean mustUseServiceToMakeTemplate(Context cx) {
      return CompUtil.hasDescendant(Sys.getStation(), "ace:AceNetwork");
   }

   private void updateSignature(BTemplateInfo tInfo) {
      BComponent root = tInfo.getTemplateRoot();
      BTemplateConfig tc = tInfo.getTemplateConfig();

      try {
         if (tInfo.getNtplSignature().length() <= 1) {
            try (BINtplFile deployedNtplFile = UpgradeUtil.getDeployedNtplFile(tc)) {
               String ts = deployedNtplFile.getTemplateManifest().bogSignature;
               tInfo.setNtplSignature(ts);
            }
         }

         if (root != null) {
            long deployedSignature = UpgradeUtil.getTemplateSignature(root);
            tInfo.setLocalSignature(Long.toHexString(deployedSignature));
         } else {
            logger.severe("TemplateService.updateSignature, template root is null.");
         }
      } catch (Exception var17) {
         logger.log(Level.SEVERE, tc.getTemplateName() + "::" + var17.getLocalizedMessage() + " ntpl file not found.", (Throwable)var17);
      }
   }

   public void subscribed() {
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(this.getName() + " subscribed");
      }

      this.updateSignatures();
   }

   public NModule[] getStationModules() {
      return AccessController.doPrivileged((PrivilegedAction<ModuleManager>)(() -> Nre.getModuleManager())).getModules();
   }

   public void syncStationModules() {
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         Nre.getRegistryManager().syncModules();
         return null;
      }));
   }

   public Type[] getServiceTypes() {
      return TYPES;
   }

   public final void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      BIRestrictedComponent.checkParentForRestrictedComponent(parent, this);
   }

   public BIcon getIcon() {
      return icon;
   }
}
