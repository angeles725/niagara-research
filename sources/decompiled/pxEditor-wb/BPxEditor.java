package javax.baja.px.editor;

import com.tridium.px.editor.BPxEditorOptions;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.binding.BBoundOrds;
import com.tridium.px.editor.util.EventUtil;
import com.tridium.ui.Binder;
import com.tridium.ui.PxIncludeManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentList;
import javax.baja.agent.BAbstractPxView;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.event.PxComponentEvent;
import javax.baja.px.editor.event.PxEditorEvent;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxListener;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BMenu;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.px.BPxInclude;
import javax.baja.ui.px.PxDecoder;
import javax.baja.ui.px.PxEncoder;
import javax.baja.ui.px.PxLayer;
import javax.baja.ui.px.PxProperty;
import javax.baja.workbench.BWbProfile;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.px.BWbPxView;
import javax.baja.workbench.px.BWbPxView.ToggleMode;
import javax.baja.workbench.view.BWbView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Component", "file:PxFile"},
      requiredPermissions = "r"
   )}
)
public class BPxEditor extends BWbPxView {
   public static final Type TYPE = Sys.loadType(BPxEditor.class);
   private static final Logger eventLogger = Logger.getLogger("px.event");
   private final ToggleCommand cmdToggleMode;
   private BPxEditorPane editorPane = null;
   private BMenu[] viewMenus = null;
   private BToolBar viewToolBar = null;
   private BWidget viewStatus = null;
   private final List<PxListener> listeners = new ArrayList<>();
   private BPxProfile pxProfile;
   private PxEditorController controller = new PxEditorController(this);
   private Binder binder;

   public Type getType() {
      return TYPE;
   }

   public BPxEditor() {
      this(null);
   }

   public BPxEditor(BAbstractPxView agent) {
      super(agent);
      this.cmdToggleMode = new ToggleMode(this, this);
      this.cmdToggleMode.setSelected(true);
   }

   public void doLoadValue(BObject value, Context cx) {
      BWidget loadWidget = this.loadPx(value, cx);
      if (this.getWidget() == null) {
         this.setContent(loadWidget);
      } else {
         this.setReadonly(!this.isEditable());
         this.setContent(this.editorPane = new BPxEditorPane(this));
         this.editorPane.initSideBarPane();
         this.viewMenus = this.getPxProfile().getViewMenus(this.editorPane.getViewMenus());
         this.viewToolBar = this.getPxProfile().getViewToolBar(this.editorPane.getViewToolBar());
         this.viewStatus = this.getPxProfile().getViewStatusBarSupplement(this.editorPane.getViewStatusBarSupplement());
         this.editorPane.load(cx);
      }

      this.firePxEvent(new PxEditorEvent(0));
   }

   public BObject doSaveValue(BObject value, Context cx) {
      BObject obj = null;
      if (this.isDynamic()) {
         try {
            obj = ((BWbView)this.getContent()).saveValue(value, cx);
         } catch (Exception var5) {
            throw new BajaRuntimeException(var5);
         }
      } else {
         obj = this.editorPane.save(value, cx);
      }

      PxIncludeManager.trimAll();
      this.firePxEvent(new PxEditorEvent(1));
      return obj;
   }

   public BMenu[] getViewMenus() {
      return this.viewMenus;
   }

   public BToolBar getViewToolBar() {
      return this.viewToolBar;
   }

   public BWidget getViewStatusBarSupplement() {
      return this.viewStatus;
   }

   public void deactivated() {
      super.deactivated();
      if (this.editorPane != null) {
         this.editorPane.deactivated();
      }
   }

   public boolean isEditor() {
      return true;
   }

   public ToggleCommand getToggleMode() {
      return this.cmdToggleMode;
   }

   public PxListener[] getPxListeners() {
      return this.listeners.toArray(new PxListener[0]);
   }

   public void addPxListener(PxListener listener) {
      if (!this.listeners.contains(listener)) {
         this.listeners.add(listener);
      }
   }

   public void removePxListener(PxListener listener) {
      this.listeners.remove(listener);
   }

   public void firePxEvent(PxEvent event) {
      label40:
      switch (event.getEventType()) {
         case 0:
            switch (((PxEditorEvent)event).getEventId()) {
               case 3:
                  this.editorPane.updateToolbar();
                  this.editorPane.repaint();
               default:
                  break label40;
            }
         case 1:
            switch (((PxPropertyEvent)event).getEventId()) {
               case 0:
               case 3:
                  this.doUpdate();
                  break label40;
               case 1:
               case 2:
                  this.editorPane.getSelectedWidgets().resetHandles();
                  this.editorPane.forceRootLayout();
                  this.doUpdate();
               default:
                  break label40;
            }
         case 2:
            this.editorPane.updateToolbar();
            this.editorPane.repaint();
            break;
         case 3:
         case 4:
            switch (EventUtil.getEventType((PxComponentEvent)event)) {
               case 1:
               case 2:
               case 3:
               case 5:
               case 6:
                  this.editorPane.getSelectedWidgets().resetHandles();
                  this.editorPane.forceRootLayout();
                  this.doUpdate();
                  break label40;
               case 4:
               case 7:
               case 8:
               case 9:
               case 10:
               case 11:
               case 12:
                  this.doUpdate();
               default:
                  break label40;
            }
         case 5:
         case 6:
            this.editorPane.getSelectedWidgets().resetHandles();
            this.editorPane.forceRootLayout();
            this.doUpdate();
         case 7:
         default:
            break;
         case 8:
            this.doUpdate();
      }

      if (eventLogger.isLoggable(Level.FINE)) {
         eventLogger.fine(event.toString());
      }

      PxListener[] x = this.getPxListeners();

      for (int i = 0; i < x.length; i++) {
         try {
            x[i].pxEvent(event);
         } catch (Throwable var5) {
            var5.printStackTrace();
         }
      }
   }

   private void doUpdate() {
      this.editorPane.getCommandStudio().getPainter().reset();
      this.editorPane.updateToolbar();
      this.setModified();
      this.editorPane.repaint();
   }

   public Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 302:
            return this.binder;
         case 401:
            return null;
         case 402:
            if (null != this.binder) {
               this.binder.stop();
            }

            this.binder = null;
            if (this.editorPane != null) {
               this.editorPane.saveState();
            }

            return null;
         case 403:
            return this.binder = new BPxEditor.PxEditorBinder(this);
         default:
            return super.fw(x, a, b, c, d);
      }
   }

   public BBoundOrds getBoundOrds() {
      return this.editorPane == null ? null : this.editorPane.getBoundOrds();
   }

   public BWidget cloneWidget(BWidget oldWidget) {
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         PxEncoder enc = new PxEncoder(out);
         enc.encodeDocument(oldWidget);
         enc.close();
         BWbShell wbShell = this.getWbShell();
         BWidget newWidget = new PxDecoder(wbShell != null ? wbShell.getActiveOrd() : BOrd.NULL, new ByteArrayInputStream(out.toByteArray())).decodeDocument();
         newWidget.setLocation(oldWidget.getX(), oldWidget.getY());
         newWidget.setSize(oldWidget.getWidth(), oldWidget.getHeight());
         this.forcePxIncludeLoad(newWidget);
         BPxEditorPane.addDynamicPxIncludeProperties(newWidget);
         return newWidget;
      } catch (Exception var6) {
         throw new BajaRuntimeException(var6);
      }
   }

   private void forcePxIncludeLoad(BWidget widget) {
      if (widget instanceof BPxInclude) {
         widget.computePreferredSize();
      }

      BWidget[] kids = widget.getChildWidgets();

      for (int i = 0; i < kids.length; i++) {
         this.forcePxIncludeLoad(kids[i]);
      }
   }

   public BWidget[] cloneWidgets(BWidget[] widgets) {
      BWidget[] arr = new BWidget[widgets.length];

      for (int i = 0; i < widgets.length; i++) {
         arr[i] = this.cloneWidget(widgets[i]);
      }

      return arr;
   }

   public BPxProfile getPxProfile() {
      if (this.pxProfile == null) {
         BWbProfile wbProfile = this.getWbShell().getProfile();
         AgentList agents = wbProfile.getAgents().filter(AgentFilter.is(BPxProfile.TYPE));
         Class<?> cls = agents.getDefault().getAgentType().getTypeSpec().getResolvedType().getTypeClass();

         try {
            Constructor<?> cons = cls.getConstructor(BPxEditor.class);
            this.pxProfile = (BPxProfile)cons.newInstance(this);
         } catch (Exception var5) {
            throw new BajaRuntimeException(var5);
         }
      }

      return this.pxProfile;
   }

   public PxEditorSelection getSelection() {
      return this.editorPane.getSelectedWidgets();
   }

   public void setPxProperties(PxProperty[] pxProperties) {
      this.pxProperties = pxProperties;
   }

   public void setPxLayers(PxLayer[] pxLayers) {
      this.pxLayers = pxLayers;
   }

   public PxEditorController getController() {
      return this.controller;
   }

   public void setController(PxEditorController controller) {
      this.controller = controller;
   }

   class PxEditorBinder extends Binder {
      PxEditorBinder(BWidget owner) {
         super(owner);
      }

      protected void updateBindings(List<BBinding> bindings, OrdTarget target) {
         if (BPxEditor.this.editorPane != null && BPxEditorOptions.make().getAnimateBindings()) {
            for (int i = 0; i < bindings.size(); i++) {
               updateBinding(bindings.get(i), target);
            }

            BPxEditor.this.editorPane.repaint();
         }
      }
   }
}
