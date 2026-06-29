package javax.baja.bacnet;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetObjectContainer extends BInterface {
   Type TYPE = Sys.loadType(BIBacnetObjectContainer.class);
   String POINT = "point";
   String SCHEDULE = "schedule";
   String HISTORY = "history";
   String CONFIG = "config";

   BObject lookupBacnetObject(BBacnetObjectIdentifier var1, int var2, int var3, String var4);
}
