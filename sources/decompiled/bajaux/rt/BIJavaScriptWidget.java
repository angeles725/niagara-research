package javax.baja.bajaux;

import javax.baja.agent.AgentList;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.BIJavaScript;

@NiagaraType
public interface BIJavaScriptWidget extends BIJavaScript {
   Type TYPE = Sys.loadType(BIJavaScriptWidget.class);

   static BIJavaScriptWidget forType(Type type, Context cx) {
      AgentList agents = Sys.getRegistry().getAgents(type.getTypeInfo()).filter(i -> i.getAgentType().is(TYPE));
      return agents.size() > 0 ? (BIJavaScriptWidget)agents.get(0).getInstance() : null;
   }

   default String validateWidget(BComponent widget, Context cx) {
      return null;
   }

   default boolean isChildWidgetValidationRequired(BComponent widget, BComponent childWidget, Context cx) {
      return true;
   }

   default boolean isBindingValidationRequired(BComponent widget, BComponent binding, Context cx) {
      return true;
   }
}
