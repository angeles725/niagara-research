package com.tridium.kitpx;

import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.commands.InvokeActionCommand;
import javax.baja.ui.event.BMouseEvent;

@NiagaraType(
   agent = {@AgentOn(
      types = {"bajaui:Widget"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "widgetEvent",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "actionArg",
      type = "BValue",
      defaultValue = "BString.DEFAULT"
   )})
public class BActionBinding extends BBinding {
   public static final Property widgetEvent = newProperty(0, "", null);
   public static final Property actionArg = newProperty(0, BString.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BActionBinding.class);
   boolean firedStarted = false;
   private volatile boolean runningAction = false;
   private final Object actionMonitor = new Object();

   public String getWidgetEvent() {
      return this.getString(widgetEvent);
   }

   public void setWidgetEvent(String v) {
      this.setString(widgetEvent, v, null);
   }

   public BValue getActionArg() {
      return this.get(actionArg);
   }

   public void setActionArg(BValue v) {
      this.set(actionArg, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean invokedOnWidget(Action action, BValue value, Context context) {
      if (action.getName().equals(this.getWidgetEvent())) {
         this.invokeActionOnTarget();
         return true;
      } else {
         return false;
      }
   }

   public boolean firedOnWidget(Topic topic, BValue event, Context context) {
      if (topic.getName().equals(this.getWidgetEvent())) {
         if (event instanceof BMouseEvent) {
            BMouseEvent mouseEvent = (BMouseEvent)event;
            if (mouseEvent.getId() == 505) {
               return false;
            }
         }

         this.invokeActionOnTarget();
         return true;
      } else {
         return false;
      }
   }

   public boolean isDegraded() {
      return !this.isBound() || !this.getTarget().canInvoke();
   }

   public void targetChanged() {
      if (!this.firedStarted && this.isBound() && this.getWidgetEvent().equals("started")) {
         this.firedStarted = true;
         this.invokeActionOnTarget();
      }

      super.targetChanged();
   }

   public void invokeActionOnTarget() {
      BOrd ord = this.getOrd();
      if (!ord.isNull()) {
         if (!this.isBound()) {
            LOGGER.severe("ActionBinding not bound " + ord);
         } else {
            OrdTarget target = this.getTarget();
            BComponent comp = target.getComponent();
            if (comp == null) {
               LOGGER.severe("ActionBinding not bound to component " + ord);
            } else {
               Action action = null;
               if (target.getSlotInComponent() instanceof Action) {
                  action = (Action)target.getSlotInComponent();
               }

               if (action == null) {
                  LOGGER.severe("ActionBinding not bound to action " + ord);
               } else {
                  BValue actionArg = null;
                  BValue arg = this.getActionArg();
                  Type paramType = action.getParameterType();
                  if (!arg.equals(BString.DEFAULT) && paramType != null) {
                     if (arg.getType().is(paramType)) {
                        actionArg = arg;
                     } else if (arg instanceof BString && paramType.is(BSimple.TYPE)) {
                        try {
                           BSimple simpleParam = (BSimple)paramType.getInstance();
                           actionArg = (BValue)simpleParam.decodeFromString(arg.toString());
                        } catch (Exception var16) {
                           LOGGER.severe("ActionBinding parsing arg \"" + arg + "\" as \"" + paramType + "\"");
                        }
                     } else {
                        LOGGER.severe("ActionBinding arg type mismatch " + arg.getType() + " != " + paramType);
                     }
                  }

                  if (!this.runningAction) {
                     synchronized (this.actionMonitor) {
                        if (!this.runningAction) {
                           try {
                              this.runningAction = true;
                              new InvokeActionCommand(this.getWidget(), comp, action, actionArg).invoke();
                           } finally {
                              this.runningAction = false;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
