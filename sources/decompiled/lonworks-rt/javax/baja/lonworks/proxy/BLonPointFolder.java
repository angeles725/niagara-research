package javax.baja.lonworks.proxy;

import javax.baja.driver.point.BPointFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonPointFolder extends BPointFolder {
   public static final Type TYPE = Sys.loadType(BLonPointFolder.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent.getType().is(BLonPointDeviceExt.TYPE) || parent.getType().is(TYPE);
   }

   public boolean isChildLegal(BComponent child) {
      return !child.getType().is(BPointFolder.TYPE) || child.getType().is(TYPE);
   }
}
