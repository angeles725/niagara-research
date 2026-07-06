package com.tridium.px.editor.make;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import com.tridium.ui.px.PxPropertyComponentArray;
import com.tridium.util.ClassUtil;
import com.tridium.workbench.ord.BComponentChooser;
import com.tridium.workbench.util.PropertyManager;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.gx.BBrush;
import javax.baja.gx.BImage;
import javax.baja.naming.BHost;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.nav.BIIndirectNavNode;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.factory.WidgetInserter;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BRadioButton;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.list.BList;
import javax.baja.ui.list.ListController;
import javax.baja.ui.list.ListRenderer;
import javax.baja.ui.list.ListRenderer.Item;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.wizard.BWizardHeader;
import javax.baja.util.BTypeSpec;
import javax.baja.virtual.BVirtualGateway;

@NiagaraType
public class BMakeWidget extends BEdgePane {
   public static final Type TYPE = Sys.loadType(BMakeWidget.class);
   private final PropertyManager propMgr = BPxEditorPane.propMgr();
   private static final BImage headerIcon = BPxEditorPane.icon("makeWidget.icon");
   public static final BOrd placeholder = BOrd.make("<ord>");
   public static final BWidget[] EMPTY_WIDGET_ARR = new BWidget[0];
   private final BPxEditor editor;
   private final BPxEditorPane editorPane;
   private final BObject[] drawingObjects;
   private final BOrd[] ords;
   private BWidget workingWidget;
   private boolean constructing;
   private final BList ordList = new BList();
   private final Set<BMwConfig> mwConfigs = new LinkedHashSet<>();
   private final Map<String, BRadioButton> configButtons = new LinkedHashMap<>();
   private final BConstrainedPane leftPane = new BConstrainedPane();
   private final BConstrainedPane rightPane = new BConstrainedPane();
   private final MakeWidgetContext context;
   private final BPxCellSheet sheet;
   private WidgetInserter inserter;

   public Type getType() {
      return TYPE;
   }

   public BMakeWidget(BPxEditor editor, BObject[] objects) {
      this.constructing = true;
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
      this.drawingObjects = objects;
      this.context = new MakeWidgetContext(this);
      BHost curHost = editor.getCurrentValueHost();
      BISession curSession = editor.getCurrentValueSession();
      this.ords = new BOrd[objects.length];
      Set<AgentInfo> configAgents = new LinkedHashSet<>();
      configAgents.add(BMwBoundLabel.TYPE.getTypeInfo().getAgentInfo());
      configAgents.add(BMwPxInclude.TYPE.getTypeInfo().getAgentInfo());
      configAgents.add(BMwFromPalette.TYPE.getTypeInfo().getAgentInfo());
      configAgents.add(BMwWorkbenchView.TYPE.getTypeInfo().getAgentInfo());
      configAgents.add(BMwPropertyBatch.TYPE.getTypeInfo().getAgentInfo());
      configAgents.add(BMwActionBatch.TYPE.getTypeInfo().getAgentInfo());
      configAgents.add(BMwTimePlot.TYPE.getTypeInfo().getAgentInfo());
      configAgents.add(BMwChart.TYPE.getTypeInfo().getAgentInfo());

      for (int i = 0; i < objects.length; i++) {
         this.ords[i] = makeOrd(objects[i], curHost, curSession);
         this.ordList.addItem(this.ords[i]);
         AgentList filter = objects[i].getAgents().filter(AgentFilter.is(BMwConfig.TYPE));
         AgentInfo[] list = filter.list();

         for (AgentInfo agent : list) {
            if (!configAgents.contains(agent)) {
               configAgents.add(agent);
            }
         }

         if (objects[i] instanceof BVirtualGateway) {
            try {
               objects[i] = ((BVirtualGateway)objects[i]).getVirtualSpace().getRootComponent();
            } catch (Exception var14) {
               var14.printStackTrace();
            }
         }

         if (objects[i].isComponent()) {
            objects[i].asComponent().loadSlots();
         }
      }

      this.ordList.setController(new BMakeWidget.Controller());
      this.ordList.setRenderer(new BMakeWidget.Renderer());
      BConstrainedPane consOrdList = new BConstrainedPane(this.ordList);
      consOrdList.setMaxHeight(this.ordList.getRenderer().getItemHeight() * 5.0);
      this.sheet = new BPxCellSheet(editor, this.context);
      this.sheet.enableBinding(false);
      this.rightPane.setMinWidth(250.0);
      this.rightPane.setMaxWidth(250.0);
      this.rightPane.setMinHeight(this.ordList.getRenderer().getItemHeight() * 21.0);
      Map<BTypeSpec, BMwConfig> agents = new LinkedHashMap<>();

      for (AgentInfo ai : configAgents) {
         if (!ai.getAgentType().isAbstract()) {
            BMwConfig config = (BMwConfig)ai.getInstance();
            config.setMakeWidget(this);
            config.load();
            BTypeSpec configType = config.getSuperceedingType();
            if (null == configType) {
               configType = config.getType().getTypeSpec();
            }

            Object object = agents.get(configType);
            if (null == object) {
               agents.put(configType, config);
            } else {
               BMwConfig original = (BMwConfig)object;
               if (null == original.getSuperceedingType()) {
                  agents.put(configType, config);
               }
            }
         }
      }

      this.mwConfigs.addAll(agents.values());
      BWidget[] widgets = this.mwConfigs.toArray(EMPTY_WIDGET_ARR);
      constrainMin(this.leftPane, 300.0, widgets);
      ToggleCommandGroup<ToggleCommand> grp = new ToggleCommandGroup();

      for (BMwConfig configx : this.mwConfigs) {
         ToggleCommand cmd = configx.getCommand();
         if (cmd.isEnabled()) {
            grp.add(cmd);
            this.configButtons.put(configx.getType().getTypeName(), new BRadioButton(cmd));
         }
      }

      BGridPane buttons = new BGridPane(1);
      buttons.setValign(BValign.top);
      buttons.setHalign(BHalign.left);

      for (BRadioButton radioButton : this.configButtons.values()) {
         buttons.add(null, radioButton);
      }

      BEdgePane edge2 = new BEdgePane();
      edge2.setTop(buttons);
      edge2.setCenter(new BBorderPane(this.leftPane, 10.0, 0.0, 0.0, 0.0));
      BEdgePane edge = new BEdgePane();
      edge.setTop(consOrdList);
      edge.setLeft(new BBorderPane(edge2, 10.0, 10.0, 0.0, 0.0));
      edge.setCenter(new BBorderPane(this.rightPane, 10.0, 0.0, 0.0, 0.0));
      this.setTop(new BWizardHeader(headerIcon, this.getTitle(), this.getDescription()));
      this.setCenter(new BBorderPane(edge));
      this.unpickle();
      this.constructing = false;
   }

   private static BOrd makeOrd(BObject object, BHost curHost, BISession curSession) {
      BOrd ord = object instanceof BIIndirectNavNode ? ((BIIndirectNavNode)object).getTargetOrNavOrd() : ((BINavNode)object).getNavOrd();
      BHost host = BOrd.toHost(object);
      if (host != null && host.equals(curHost)) {
         return BOrd.toSession(object).equals(curSession) ? ord.relativizeToSession() : ord.relativizeToHost();
      } else {
         return ord;
      }
   }

   private static void constrainMin(BConstrainedPane c, double throttleHeight, BWidget[] widgets) {
      double w = 0.0;
      double h = 0.0;

      for (BWidget widget : widgets) {
         widget.computePreferredSize();
         w = Math.max(w, widget.getPreferredWidth());
         h = Math.max(h, widget.getPreferredHeight());
      }

      c.setMinWidth(w);
      c.setMinHeight(Math.min(h, throttleHeight));
   }

   private void unpickle() {
      String configType = this.propMgr.get("mwRadio", BMwFromPalette.TYPE.getTypeName());
      BMwConfig defaultConfig = null;

      for (BMwConfig config : this.mwConfigs) {
         if (config.getType().getTypeName().equals(configType) && config.getCommand().isEnabled()) {
            ToggleCommand cmd = config.getCommand();
            if (cmd.isSelected()) {
               try {
                  cmd.doInvoke();
               } catch (Exception var8) {
                  var8.printStackTrace();
               }
            } else {
               cmd.setSelected(true);

               try {
                  cmd.doInvoke();
               } catch (Exception var7) {
                  var7.printStackTrace();
               }
            }

            config.unpickle(this.propMgr);
            if (config instanceof BMwBoundLabel || config instanceof BMwPxInclude || config instanceof BMwFromPalette || config instanceof BMwWorkbenchView) {
               this.constructing = false;
               config.setWorkingWidget();
               this.editWorkingWidget();
            }

            return;
         }

         if (config instanceof BMwFromPalette) {
            defaultConfig = config;
         }
      }

      if (null != defaultConfig) {
         defaultConfig.getCommand().setSelected(true);
         defaultConfig.unpickle(this.propMgr);
         this.constructing = false;
         defaultConfig.setWorkingWidget();
         this.editWorkingWidget();
      }
   }

   private void pickle() {
      for (BMwConfig config : this.mwConfigs) {
         if (config.getCommand().isSelected()) {
            this.propMgr.set("mwRadio", config.getType().getTypeName());
            config.pickle(this.propMgr);
            return;
         }
      }

      throw new IllegalStateException();
   }

   private BMwConfig currentConfig() {
      for (BMwConfig config : this.mwConfigs) {
         if (config.getCommand().isSelected()) {
            return config;
         }
      }

      throw new IllegalStateException();
   }

   public void editWorkingWidget() {
      if (!this.constructing) {
         this.sheet.editWidgets(new BWidget[]{this.workingWidget});
         this.sheet.enableBinding(false);
         this.sheet.repaint();
      }
   }

   BPxCellSheet sheet() {
      return this.sheet;
   }

   public BMwConfig getConfig(Type mwType) {
      for (BMwConfig config : this.mwConfigs) {
         if (config.getType() == mwType) {
            return config;
         }
      }

      return null;
   }

   public String getTitle() {
      return this.drawingObjects.length == 1 ? text("title") : text("titles");
   }

   public String getDescription() {
      return this.drawingObjects.length == 1 ? text("description") : text("descriptions");
   }

   public WidgetInserter getWidgetInserter() {
      if (this.inserter == null) {
         this.pickle();
         WidgetCopier wc = new WidgetCopier(this.editorPane);
         BMwConfig cfg = this.currentConfig();
         this.inserter = new WidgetInserter(
            cfg.makePxWidgets(wc),
            new BMakeWidget.LinkWidgets(this.editorPane.getPxPropertyComponents(), wc.makePxPropertyComponents(this.context.workingGc)),
            cfg.columnCount()
         );
      }

      return this.inserter;
   }

   public BWidget getWorkingWidget() {
      return this.workingWidget;
   }

   public void setWorkingWidget(BWidget widget) {
      this.workingWidget = widget;
   }

   public BConstrainedPane getRightPane() {
      return this.rightPane;
   }

   public BConstrainedPane getLeftPane() {
      return this.leftPane;
   }

   public BObject[] getDrawingObjects() {
      return this.drawingObjects;
   }

   public BOrd[] getOrds() {
      return this.ords;
   }

   public boolean isConstructing() {
      return this.constructing;
   }

   public BPxEditorPane getEditorPane() {
      return this.editorPane;
   }

   public BPxEditor getEditor() {
      return this.editor;
   }

   public BBinding workingBinding() {
      return ((BBinding[])this.workingWidget.getChildren(BBinding.class))[0];
   }

   static String text(String s) {
      return BPxEditorPane.text("makeWidget." + s);
   }

   private class Controller extends ListController {
      private Controller() {
      }

      protected void itemDoubleClicked(BMouseEvent event, int index) {
         BObject base = BMakeWidget.this.editor.getWbShell().getActiveOrdTarget().get();
         BComponentChooser chooser = new BComponentChooser();
         BOrd ord = chooser.openChooser(BMakeWidget.this, base, BMakeWidget.this.ords[index], null);
         if (ord != null) {
            ord = ord.relativizeToSession();
            BMakeWidget.this.ords[index] = ord;
            BMakeWidget.this.drawingObjects[index] = ord.get(base);

            for (BMwConfig mwConfig : BMakeWidget.this.mwConfigs) {
               mwConfig.load();
            }

            if (ClassUtil.anyNull(BMakeWidget.this.drawingObjects)) {
               for (BMwConfig config : BMakeWidget.this.mwConfigs) {
                  if (config instanceof BMwFromPalette) {
                     BMwFromPalette fromPalette = (BMwFromPalette)config;
                     fromPalette.getCommand().setSelected(true);
                     break;
                  }
               }
            }

            BMakeWidget.this.ordList.removeAllItems();

            for (BOrd ord1 : BMakeWidget.this.ords) {
               BMakeWidget.this.ordList.addItem(ord1);
            }

            BMakeWidget.this.ordList.setSelectedIndex(index);
            BMakeWidget.this.relayout();
         }
      }
   }

   private class LinkWidgets extends Command {
      private final PxPropertyComponentArray oldGc;
      private final PxPropertyComponentArray newGc;

      private LinkWidgets(PxPropertyComponentArray oldGc, PxPropertyComponentArray newGc) {
         super(BMakeWidget.this.editor, "linkWidgets");
         this.oldGc = oldGc;
         this.newGc = newGc;
      }

      public CommandArtifact doInvoke() throws Exception {
         CommandArtifact a = new CommandArtifact() {
            public void redo() throws Exception {
               BMakeWidget.this.editorPane.setPxPropertyComponents(LinkWidgets.this.newGc);
            }

            public void undo() throws Exception {
               BMakeWidget.this.editorPane.setPxPropertyComponents(LinkWidgets.this.oldGc);
            }
         };
         a.redo();
         return a;
      }
   }

   private static class Renderer extends ListRenderer {
      private Renderer() {
      }

      public BBrush getSelectionForeground(Item item) {
         return this.getForeground(item);
      }

      public BBrush getSelectionBackground(Item item) {
         return this.getBackground(item);
      }
   }
}
