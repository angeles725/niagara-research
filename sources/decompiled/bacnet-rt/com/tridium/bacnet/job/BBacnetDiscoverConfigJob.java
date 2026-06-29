package com.tridium.bacnet.job;

import com.tridium.driver.util.StringUtil;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.config.BBacnetConfigDeviceExt;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetDiscoverConfigJob extends BBacnetDiscoverJob {
   public static final Type TYPE = Sys.loadType(BBacnetDiscoverConfigJob.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetDiscoverConfigJob() {
   }

   public BBacnetDiscoverConfigJob(BBacnetConfigDeviceExt deviceExt) {
      super(deviceExt);
   }

   @Override
   protected boolean doForId(BBacnetObjectIdentifier objectId) {
      return true;
   }

   @Override
   void addDiscoveryChild(BBacnetDiscoverJob.IdVals iv) {
      BDiscoveryConfig dc = new BDiscoveryConfig(iv.name, iv.id);
      BBacnetDiscoverJob.PropVal primary = iv.primary();
      if (primary != null) {
         dc.setValue(primary.toString());
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine("No primary value for discovered object " + iv);
      }

      BBacnetDiscoverJob.PropVal desc = iv.get(28);
      if (desc != null) {
         dc.setDescription(desc.toString());
      }

      TypeInfo[] tis = BBacnetObject.getTypeInfos(iv.id);
      String[] infos = new String[tis.length];

      for (int i = 0; i < infos.length; i++) {
         infos[i] = tis[i].toString();
      }

      dc.setTypeSpecs(StringUtil.toString(infos, ";"));
      this.add(null, dc);
   }
}
