package javax.baja.webchart.ux;

import com.tridium.history.ux.BHistoryJsBuild;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.BJsBuild;
import javax.baja.webeditors.ux.BWebEditorsJsBuild;

@NiagaraType
@NiagaraSingleton
public final class BWebChartJsBuild extends BJsBuild {
   public static final BWebChartJsBuild INSTANCE = new BWebChartJsBuild();
   public static final Type TYPE = Sys.loadType(BWebChartJsBuild.class);

   public Type getType() {
      return TYPE;
   }

   private BWebChartJsBuild() {
      super(
         "webChart",
         BOrd.make("module://webChart/rc/webChart.built.min.js"),
         new Type[]{BWebEditorsJsBuild.TYPE, BHistoryJsBuild.TYPE, BWebChartCssResource.TYPE}
      );
   }
}
