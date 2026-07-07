package com.tridium.template;

import com.tridium.template.file.BINtplFile;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.sys.BAbstractService;
import javax.baja.sys.BStation;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BServiceContainer;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "configuration",
      type = "BTemplateConfig",
      defaultValue = "new BTemplateConfig()"
   ), @NiagaraProperty(
      name = "templateSignature",
      type = "BTemplateSignature",
      defaultValue = "BTemplateSignature.DEFAULT",
      flags = 5
   )})
public class BApplicationService extends BAbstractService {
   public static final Property configuration = newProperty(0, new BTemplateConfig(), null);
   public static final Property templateSignature = newProperty(5, BTemplateSignature.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BApplicationService.class);
   private static final Type[] TYPES = new Type[]{TYPE};
   private static final Logger LOGGER = Logger.getLogger("template");

   public BTemplateConfig getConfiguration() {
      return (BTemplateConfig)this.get(configuration);
   }

   public void setConfiguration(BTemplateConfig v) {
      this.set(configuration, v, null);
   }

   public BTemplateSignature getTemplateSignature() {
      return (BTemplateSignature)this.get(templateSignature);
   }

   public void setTemplateSignature(BTemplateSignature v) {
      this.set(templateSignature, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type[] getServiceTypes() {
      return TYPES;
   }

   public void started() {
      this.updateSignature();
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.fox
      )}
   )
   public static String getApplicationId(Context cx) {
      BTemplateConfig config = getApplicationConfig();
      if (config != null) {
         try {
            return config.getUID().encodeToString();
         } catch (IOException var3) {
         }
      }

      return "";
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.fox
      )}
   )
   public static String getApplicationVersion(Context cx) {
      BTemplateConfig config = getApplicationConfig();
      return config != null ? config.getManifest().version : "";
   }

   private static BTemplateConfig getApplicationConfig() {
      BStation station = Sys.getStation();
      if (station != null) {
         BServiceContainer services = station.getServices();
         if (services != null) {
            BApplicationService[] serviceArray = (BApplicationService[])services.getChildren(BApplicationService.class);
            if (serviceArray != null && serviceArray.length > 0) {
               BApplicationService applicationService = serviceArray[0];
               if (applicationService != null) {
                  return applicationService.getConfiguration();
               }
            }
         }
      }

      return null;
   }

   private void updateSignature() {
      if (this.getTemplateSignature().equals(BTemplateSignature.DEFAULT)) {
         BTemplateConfig config = this.getConfiguration();

         try (BINtplFile file = UpgradeUtil.getDeployedNtplFile(config, "^applicationTemplate/", "napl")) {
            String signatureString = file.getTemplateManifest().bogSignature;
            this.setTemplateSignature((BTemplateSignature)BTemplateSignature.DEFAULT.decodeFromString(signatureString));
         } catch (Exception var15) {
            LOGGER.log(Level.SEVERE, "Could not update template signature.", (Throwable)var15);
         }
      }
   }
}
