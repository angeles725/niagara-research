package com.tridium.px.editor.make;

import com.tridium.util.ClassUtil;
import com.tridium.workbench.util.PropertyManager;
import com.tridium.workbench.web.browser.BWebWidget;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.agent.BAbstractPxView;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Action;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BLayout;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.list.BList;
import javax.baja.web.BIFormFactorMax;
import javax.baja.workbench.WbSys;
import javax.baja.workbench.view.BWbView;
import javax.baja.workbench.view.BWbViewBinding;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Object"}
   )}
)
@NiagaraAction(
   name = "viewChanged"
)
public class BMwWorkbenchView extends BMwConfig {
   public static final Action viewChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BMwWorkbenchView.class);
   protected BList view = new BList();
   protected AgentInfo[] viewAgents = new AgentInfo[0];

   public void viewChanged() {
      this.invoke(viewChanged, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BMwWorkbenchView() {
      this.command = new BMwWorkbenchView.Cmd();
      this.setContent(this.view);
      this.linkTo(this.view, BList.selectionModified, viewChanged);
   }

   public void doViewChanged() {
      if (this.command.isSelected()) {
         this.setWorkingWidget();
         this.mw.editWorkingWidget();
      }
   }

   @Override
   public void load() {
      BObject[] objects = this.mw.getDrawingObjects();
      if (ClassUtil.anyNull(objects)) {
         this.command.setEnabled(false);
      } else {
         this.command.setEnabled(true);

         try {
            Array<AgentInfo> arr = this.agents(objects[0]);

            for (int i = 1; i < objects.length; i++) {
               arr = arr.intersection(this.agents(objects[i]).trim());
            }

            this.viewAgents = (AgentInfo[])arr.trim();
            this.view.removeAllItems();

            for (AgentInfo agent : this.viewAgents) {
               this.view.addItem(agent.getDisplayName(null));
            }

            if (this.viewAgents.length == 0) {
               this.command.setEnabled(false);
               if (this.command.isSelected()) {
                  BMwConfig config = this.mw.getConfig(BMwBoundLabel.TYPE);
                  if (null != config) {
                     config.getCommand().setSelected(true);
                  }
               }
            } else {
               this.view.setSelectedIndex(0);
            }
         } catch (Exception var7) {
            throw new BajaRuntimeException(var7);
         }
      }
   }

   @Override
   public void setWorkingWidget() {
      if (!this.mw.isConstructing()) {
         int n = this.view.getSelectedIndex();
         if (n == -1) {
            n = 0;
         }

         BObject obj = this.viewAgents[n].getInstance();
         BWbView vw;
         if (obj instanceof BIFormFactorMax) {
            vw = new BWebWidget(this.viewAgents[n]);
         } else {
            vw = (BWbView)obj;
         }

         addBinding(vw, new BWbViewBinding(), BMakeWidget.placeholder);
         vw.setLayout(BLayout.make("0,0,100,100"));
         this.mw.setWorkingWidget(vw);
      }
   }

   @Override
   public BWidget[] makePxWidgets(WidgetCopier wc) {
      return this.makeDefaultWidgets(wc);
   }

   @Override
   public void pickle(PropertyManager propMgr) {
   }

   @Override
   public void unpickle(PropertyManager propMgr) {
   }

   private Array<AgentInfo> agents(BObject obj) {
      AgentList list = WbSys.getFilteredViewList(this, obj, agent -> true);
      Array<AgentInfo> arr = new Array(list.list());
      return arr.filter(o -> {
         AgentInfo info = (AgentInfo)o;
         TypeInfo typeInfo = info.getAgentType();
         return !typeInfo.getModuleName().equals("pxEditor") && !typeInfo.is(BAbstractPxView.TYPE);
      });
   }

   private class Cmd extends ToggleCommand {
      Cmd() {
         super(BMwWorkbenchView.this, BMakeWidget.text("workbenchView"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (this.isSelected()) {
            BMwWorkbenchView.this.mw.getRightPane().setContent(BMwWorkbenchView.this.mw.sheet());
            BMwWorkbenchView.this.mw.getLeftPane().setContent(BMwWorkbenchView.this);
            BMwWorkbenchView.this.setWorkingWidget();
            BMwWorkbenchView.this.mw.editWorkingWidget();
         }

         return null;
      }
   }
}
