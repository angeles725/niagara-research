package javax.baja.bajaux;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.BCssResource;

@NiagaraType
@NiagaraSingleton
public class BBajauxCssResource extends BCssResource {
   public static final BBajauxCssResource INSTANCE = new BBajauxCssResource();
   public static final Type TYPE = Sys.loadType(BBajauxCssResource.class);

   public Type getType() {
      return TYPE;
   }

   private BBajauxCssResource() {
      super(new BOrd[]{BOrd.make("module://bajaux/rc/container/container.css")});
   }
}
