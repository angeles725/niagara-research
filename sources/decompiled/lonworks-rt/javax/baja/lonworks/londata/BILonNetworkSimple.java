package javax.baja.lonworks.londata;

import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BILonNetworkSimple extends BInterface {
   Type TYPE = Sys.loadType(BILonNetworkSimple.class);

   void toOutputStream(LonOutputStream var1);

   BILonNetworkSimple fromInputStream(LonInputStream var1);

   int getNetLength();
}
