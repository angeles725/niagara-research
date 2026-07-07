package com.tridium.template.job;

import com.tridium.template.api.NiagaraTemplate;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BMakeStationTemplateJob extends BMakeTemplateJob {
   public static final Type TYPE = Sys.loadType(BMakeStationTemplateJob.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMakeStationTemplateJob() {
      this(true);
   }

   public BMakeStationTemplateJob(boolean useMinorVersionOnDeployment) {
      super(useMinorVersionOnDeployment);
   }

   public void run(Context cx) throws Exception {
      NiagaraTemplate template = NiagaraTemplate.createFromLocalStation();
      this.saveTemplateToTemporaryFile(template);
   }
}
