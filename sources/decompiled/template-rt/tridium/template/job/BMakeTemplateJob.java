package com.tridium.template.job;

import com.tridium.sys.transfer.FileToFile;
import com.tridium.template.api.NiagaraTemplate;
import java.io.IOException;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.FilePath;
import javax.baja.job.BSimpleJob;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
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
      name = "templateName",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
public abstract class BMakeTemplateJob extends BSimpleJob {
   public static final Property templateFileOrd = newProperty(1, BOrd.DEFAULT, null);
   public static final Property templateName = newProperty(1, "", null);
   public static final Type TYPE = Sys.loadType(BMakeTemplateJob.class);
   private final boolean useMinorVersionOnDeployment;

   public BOrd getTemplateFileOrd() {
      return (BOrd)this.get(templateFileOrd);
   }

   public void setTemplateFileOrd(BOrd v) {
      this.set(templateFileOrd, v, null);
   }

   public String getTemplateName() {
      return this.getString(templateName);
   }

   public void setTemplateName(String v) {
      this.setString(templateName, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BMakeTemplateJob(boolean useMinorVersionOnDeployment) {
      this.useMinorVersionOnDeployment = useMinorVersionOnDeployment;
   }

   protected void saveTemplateToTemporaryFile(NiagaraTemplate template) throws IOException {
      template.setUseMinorVersionOnDeployment(this.useMinorVersionOnDeployment);
      this.setTemplateName(template.getTitle());
      FilePath destPath = new FilePath("^temp");
      BDirectory destDirectory = BFileSystem.INSTANCE.makeDir(destPath);
      String destName = FileToFile.getUniqueFilename(destDirectory, template.getFileName());
      this.setTemplateFileOrd(template.save(destPath.merge(destName)));
   }
}
