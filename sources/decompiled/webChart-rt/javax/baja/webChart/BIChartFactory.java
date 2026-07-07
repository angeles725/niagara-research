package javax.baja.webChart;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.BIJavaScript;

@NiagaraType
public interface BIChartFactory extends BIJavaScript {
   Type TYPE = Sys.loadType(BIChartFactory.class);
}
