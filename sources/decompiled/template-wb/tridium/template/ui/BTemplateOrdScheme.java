package com.tridium.template.ui;

import com.tridium.sys.transfer.FileToFile;
import com.tridium.template.BTemplateService;
import com.tridium.template.api.NiagaraTemplate;
import com.tridium.template.file.BNewNtplFromTemporary;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.job.BMakeTemplateJob;
import com.tridium.template.ui.file.TmplUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.job.BJobState;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdScheme;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SyntaxException;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   ordScheme = "template"
)
@NiagaraSingleton
public class BTemplateOrdScheme extends BOrdScheme {
   public static final BTemplateOrdScheme INSTANCE = new BTemplateOrdScheme();
   public static final Type TYPE = Sys.loadType(BTemplateOrdScheme.class);
   private static final int JOB_CHECK_SLEEP_TIME = 200;

   public Type getType() {
      return TYPE;
   }

   private BTemplateOrdScheme() {
      super("template");
   }

   public OrdTarget resolve(OrdTarget base, OrdQuery query) throws SyntaxException, UnresolvedException {
      String queryBody = query.getBody();
      boolean createApp = "createApp".equals(queryBody);
      if (createApp || "create".equals(queryBody)) {
         BComponent target = base.getComponent();
         if (target != null) {
            return this.buildOrdTarget(base, target, createApp);
         }
      }

      return null;
   }

   private OrdTarget buildOrdTarget(OrdTarget base, BComponent target, boolean createApp) {
      if (target instanceof BStation) {
         BStation station = (BStation)target;
         boolean preferOnStationBuilds = AccessController.doPrivileged(
            (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.template.preferBuildingOnStation"))
         );
         if (preferOnStationBuilds || TmplUtil.stationHasAce(station)) {
            return this.buildTemporaryFile(base, station, createApp);
         }
      }

      return this.buildInMemoryFile(base, target, createApp);
   }

   private OrdTarget buildInMemoryFile(OrdTarget base, BComponent target, boolean createApp) {
      BComponentSpace sp = target.getComponentSpace();
      if (sp != null) {
         sp.update(target, Integer.MAX_VALUE);
      }

      try {
         OrdTarget result;
         if (createApp) {
            result = new OrdTarget(base, TmplUtil.createInMemoryApp(target));
         } else {
            result = new OrdTarget(base, TmplUtil.createInMemoryNtpl(target));
         }

         return result;
      } catch (Exception var7) {
         throw new UnresolvedException("Unable to create in memory template", var7);
      }
   }

   private OrdTarget buildTemporaryFile(OrdTarget base, BStation station, boolean createApp) {
      try {
         BComponentSpace space = station.getComponentSpace();
         boolean isOnline = space != null && space.isProxyComponentSpace();
         BTemplateService templateService = TemplateUiUtil.resolveTemplateService(station);
         boolean templateServiceIsCapable = templateService != null
            && templateService.getAction(createApp ? "makeApplicationTemplate" : "makeStationTemplate") != null;
         BNtplFile destFile;
         if (isOnline && templateServiceIsCapable && TemplateUiUtil.isSuperUser(station)) {
            BMakeTemplateJob job = this.startMakingTemplateInService(station, templateService, createApp);

            while (!job.getJobState().isComplete()) {
               try {
                  Thread.sleep(200L);
               } catch (InterruptedException var16) {
                  if (job.getJobState().isComplete()) {
                     break;
                  }

                  throw new RuntimeException(var16);
               }
            }

            if (!BJobState.success.equals(job.getJobState())) {
               throw new UnresolvedException("Failed to create template");
            }

            BOrd fileOrd = job.getTemplateFileOrd();
            BIFile sourceFile = (BIFile)fileOrd.get(station);
            String templateName = job.getTemplateName();
            FilePath destDirectoryPath = new FilePath("~temp");
            BDirectory destDirectory = BFileSystem.INSTANCE.makeDir(destDirectoryPath);
            String destFilename = FileToFile.getUniqueFilename(destDirectory, templateName + "." + (createApp ? "napl" : "ntpl"));
            destFile = (BNtplFile)BFileSystem.INSTANCE.makeFile(destDirectoryPath.merge(destFilename));
            BajaFileUtil.pipe(sourceFile, destFile);
            sourceFile.delete();
         } else {
            NiagaraTemplate template = createApp ? NiagaraTemplate.createApplicationFrom(station) : NiagaraTemplate.createStationTemplateFrom(station);
            FilePath destPath = new FilePath("~temp");
            BDirectory destDirectory = BFileSystem.INSTANCE.makeDir(destPath);
            String destName = FileToFile.getUniqueFilename(destDirectory, template.getFileName());
            destFile = (BNtplFile)template.save(destPath.merge(destName)).get();
         }

         return destFile != null ? new OrdTarget(base, new BNewNtplFromTemporary(destFile)) : null;
      } catch (Exception var17) {
         throw new UnresolvedException("Unable to create template", var17);
      }
   }

   private BMakeTemplateJob startMakingTemplateInService(BStation station, BTemplateService templateService, boolean createApp) throws Exception {
      BOrd makeTemplateJobOrd = createApp
         ? templateService.makeApplicationTemplate(BBoolean.make(BTemplateOptions.get().getUseMinorVersionOnDeployment()))
         : templateService.makeStationTemplate(BBoolean.make(BTemplateOptions.get().getUseMinorVersionOnDeployment()));
      BComponentSpace componentSpace = Objects.requireNonNull(station.getComponentSpace());
      componentSpace.sync();
      return (BMakeTemplateJob)makeTemplateJobOrd.relativizeToSession().get(station);
   }
}
