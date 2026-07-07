package javax.baja.webchart.ux;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.BCssResource;

@NiagaraType
@NiagaraSingleton
public class BWebChartCssResource extends BCssResource {
   public static final BWebChartCssResource INSTANCE = new BWebChartCssResource();
   public static final Type TYPE = Sys.loadType(BWebChartCssResource.class);

   public Type getType() {
      return TYPE;
   }

   private BWebChartCssResource() {
      super(
         new BOrd[]{
            BOrd.make("module://webChart/rc/ChartWidgetStyle.css"),
            BOrd.make("module://webChart/rc/line/LineStyle.css"),
            BOrd.make("module://webChart/rc/gauge/CircularGaugeWidgetStyle.css"),
            BOrd.make("module://webChart/rc/fe/color/colorChooserStyle.css")
         }
      );
   }
}
