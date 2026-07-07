package com.tridium.template.job;

import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateInfo;
import com.tridium.template.BTemplateService;
import com.tridium.template.UpgradeUtil;
import com.tridium.template.application.JobProgressTracker;
import java.util.ArrayList;
import java.util.List;
import javax.baja.job.BSimpleJob;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BUpgradeTemplateJob extends BSimpleJob {
   public static final Type TYPE = Sys.loadType(BUpgradeTemplateJob.class);
   BVector tmplConfigs = null;
   BTemplateService tmplService = null;

   public Type getType() {
      return TYPE;
   }

   public BUpgradeTemplateJob() {
      this.tmplConfigs = null;
   }

   public BUpgradeTemplateJob(BVector tmplConfigs, BTemplateService tmplService) {
      this.tmplConfigs = tmplConfigs;
      this.tmplService = tmplService;
   }

   public void run(Context cx) throws Exception {
      BTemplateService.logger.fine("BUpgradeTemplateJob.run() start");
      Property[] propertiesArray = this.tmplConfigs.getPropertiesArray();
      if (propertiesArray.length > 0) {
         this.log().message("starting");
         List<BTemplateConfig> configs = new ArrayList<>();
         List<Boolean> redeployValues = new ArrayList<>();

         for (Property property : propertiesArray) {
            BValue bValue = this.tmplConfigs.get(property);
            boolean isRedeploy = true;
            BTemplateConfig templateConfig = null;

            try {
               if (bValue instanceof BOrd) {
                  OrdTarget ordTarget = ((BOrd)bValue).resolve(this.tmplService);
                  if (ordTarget != null) {
                     BObject bObject = ordTarget.get();
                     if (bObject instanceof BTemplateConfig) {
                        templateConfig = (BTemplateConfig)bObject;
                     } else if (bObject instanceof BTemplateInfo) {
                        BTemplateInfo tmplInfo = (BTemplateInfo)bObject;
                        templateConfig = tmplInfo.getTemplateConfig();
                        isRedeploy = tmplInfo.isRedeploy();
                     }
                  }
               }

               if (templateConfig != null) {
                  configs.add(templateConfig);
                  redeployValues.add(isRedeploy);
               }
            } catch (UnresolvedException var15) {
            }
         }

         UpgradeUtil.upgradeTemplates(configs, redeployValues, new JobProgressTracker(this));
         this.tmplService.updateSignatures();
      }
   }
}
