package javax.baja.lonworks;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInterface;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BILonLoadable extends BInterface {
   Type TYPE = Sys.loadType(BILonLoadable.class);

   void beginUpload();

   void endUpload();

   void beginDownload();

   void endDownload();

   BLonDevice getLonDevice();

   BComponent asComponent();

   String getName();

   String getDisplayName(Context var1);
}
