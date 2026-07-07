package javax.baja.bajaux;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.BJsBuild;

@NiagaraType
@NiagaraSingleton
public final class BBajauxJsBuild extends BJsBuild {
   public static final BBajauxJsBuild INSTANCE = new BBajauxJsBuild();
   public static final Type TYPE = Sys.loadType(BBajauxJsBuild.class);

   public Type getType() {
      return TYPE;
   }

   private BBajauxJsBuild() {
      super("bajaux", BOrd.make("module://bajaux/rc/bajaux.built.min.js"), new Type[]{BBajauxCssResource.TYPE});
   }
}
