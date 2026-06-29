package javax.baja.bacnet.config;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetConfigFolder extends BInterface {
   Type TYPE = Sys.loadType(BIBacnetConfigFolder.class);

   BBacnetConfigDeviceExt getConfig();
}
