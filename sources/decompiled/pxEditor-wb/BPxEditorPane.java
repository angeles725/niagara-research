package com.tridium.px.editor;

import com.tridium.px.editor.sidebars.binding.BBoundOrds;
import com.tridium.px.editor.sidebars.cellsheet.BPxCellSheet;
import com.tridium.px.editor.sidebars.layersheet.BPxLayerSheet;
import com.tridium.px.editor.sidebars.propsheet.BPxPropSheet;
import com.tridium.px.editor.sidebars.tree.BPxTreePane;
import com.tridium.px.editor.studio.BStudio;
import com.tridium.px.editor.studio.CommandStudio;
import com.tridium.px.editor.studio.PainterStudio;
import com.tridium.px.editor.studio.RootStudio;
import com.tridium.px.editor.studio.TrackerStudio;
import com.tridium.px.editor.studio.TreeStudio;
import com.tridium.px.editor.studio.commands.Align;
import com.tridium.px.editor.studio.commands.Reorg;
import com.tridium.px.editor.studio.painters.DefaultPainter;
import com.tridium.px.editor.studio.trackers.AddPathTracker;
import com.tridium.px.editor.studio.trackers.AddPointTracker;
import com.tridium.px.editor.studio.trackers.AddPolygonTracker;
import com.tridium.px.editor.studio.trackers.DeletePointTracker;
import com.tridium.px.editor.studio.trackers.UnpressedTracker;
import com.tridium.px.editor.util.LayerManager;
import com.tridium.px.editor.util.Reflector;
import com.tridium.px.editor.util.SelectedWidgets;
import com.tridium.ui.BZoomPane;
import com.tridium.ui.BZoomPane.ZoomInCommand;
import com.tridium.ui.BZoomPane.ZoomOutCommand;
import com.tridium.ui.BZoomPane.ZoomResetCommand;
import com.tridium.ui.px.PxPropertyComponentArray;
import com.tridium.ui.px.PxUtil;
import com.tridium.ui.theme.Theme;
import com.tridium.workbench.util.PropertyManager;
import com.tridium.workbench.util.TypeInfoSpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.baja.agent.BAbstractPxView;
import javax.baja.agent.BPxView;
import javax.baja.file.BIFile;
import javax.baja.file.types.text.BPxFile;
import javax.baja.gx.BBrush;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.gx.BSize;
import javax.baja.gx.RectGeom;
import javax.baja.gx.BBrush.Image;
import javax.baja.gx.BBrush.Paint;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.px.editor.BDrawingTool;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.BPxSideBar;
import javax.baja.px.editor.event.PxEditorEvent;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BBorder;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLayout;
import javax.baja.ui.BMenu;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BTextField;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.list.BList;
import javax.baja.ui.naming.BRootContainer;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BLabelPane;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.pane.BToolPane;
import javax.baja.ui.pane.BToolPane.MenuController;
import javax.baja.ui.px.BPxInclude;
import javax.baja.ui.px.BPxMedia;
import javax.baja.ui.px.PxEncoder;
import javax.baja.ui.px.PxProperty;
import javax.baja.ui.shape.BShape;
import javax.baja.ui.transfer.BTransferWidget;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.Lexicon;
import javax.baja.workbench.BWbShell;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "optionChanged",
      parameterType = "BString",
      defaultValue = "BString.DEFAULT"
   ), @NiagaraAction(
      name = "zoomChanged",
      parameterType = "BDouble",
      defaultValue = "BDouble.DEFAULT"
   )})
public class BPxEditorPane extends BEdgePane {
   public static final Action optionChanged = newAction(0, BString.DEFAULT, null);
   public static final Action zoomChanged = newAction(0, BDouble.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BPxEditorPane.class);
   private static PropertyManager propMgr = new PropertyManager(new File(Sys.getNiagaraUserHome(), "pxEditor.properties"), "PxEditor Properties");
   private static Lexicon lexicon = Lexicon.make("pxEditor");
   private static Lexicon wbLexicon = Lexicon.make("workbench");
   private BPxEditor editor;
   private BBoundOrds boundOrds;
   private BPxTreePane treePane;
   private BPxPropSheet propSheet;
   private BPxLayerSheet layerSheet;
   private BPxCellSheet cellSheet;
   private BSplitPane mainSplit;
   private BToolPane sideBarPane;
   private BTextField xyStatus;
   private BTextField zoomStatus;
   private DecimalFormat zoomStatusFormat = new DecimalFormat("0.0");
   private BGridPane statusPane;
   private PxPropertyComponentArray propComps;
   private LayerManager layerMgr;
   private SelectedWidgets selected;
   private BStudio studio;
   private BZoomPane zoomPane;
   private BScrollPane zoomScrollPane;
   private Context cx;
   private BRootContainer rootContainer;
   private RootShellManager rootShellMgr;
   private BPxEditorPane.OptionToggle showGrid;
   private BPxEditorPane.OptionToggle useSnap;
   private BPxEditorPane.OptionToggle showHatch;
   private BPxEditorPane.SetTargetMedia setTargetMedia;
   private BPxEditorPane.ValidateMedia validateMedia;
   private ZoomInCommand zoomIn;
   private ZoomOutCommand zoomOut;
   private ZoomResetCommand zoomReset;
   private Align alignCenter;
   private Align alignMiddle;
   private Align alignLft;
   private Align alignRgt;
   private Align alignTop;
   private Align alignBot;
   private Reorg bringToFront;
   private Reorg sendToBack;
   private BPxEditorPane.NormalTool normalTool;
   private BPxEditorPane.GeometryTool addPolygonTool;
   private BPxEditorPane.GeometryTool addPathTool;
   private BPxEditorPane.GeometryTool addPointTool;
   private BPxEditorPane.GeometryTool deletePointTool;
   private BDrawingTool tool;
   private BPxEditorPane.ToggleToolPane toggleToolPane;
   private BPxSideBar[] sideBars;
   private Map<BPxSideBar, Boolean> sideBarVisibility;
   private boolean loaded = false;
   private static final Map<BOrd, Map<String, Object>> pxEditorStates = new HashMap<>();
   private BComponent pxRootComponent;
   private static final boolean STATE_ENABLED = AccessController.doPrivileged(
         (PrivilegedAction<String>)(() -> System.getProperty("niagara.pxEditor.stateEnabled", "true"))
      )
      .equals("true");

   public void optionChanged(BString parameter) {
      this.invoke(optionChanged, parameter, null);
   }

   public void zoomChanged(BDouble parameter) {
      this.invoke(zoomChanged, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BPxEditorPane(BPxEditor editor) {
      this.editor = editor;
      this.selected = new SelectedWidgets(editor, this);
      this.setTargetMedia = new BPxEditorPane.SetTargetMedia();
      this.validateMedia = new BPxEditorPane.ValidateMedia();
      this.studio = new BStudio(editor, this);
      this.zoomPane = new BZoomPane(this.studio);
      this.zoomScrollPane = new BScrollPane(this.zoomPane);
      this.rootContainer = new BRootContainer();
      this.rootShellMgr = new RootShellManager(this, this.studio);
      this.propComps = new PxPropertyComponentArray();
      this.layerMgr = new LayerManager(editor, this);
      BPxEditorOptions options = this.getOptions();
      this.showGrid = new BPxEditorPane.OptionToggle(BPxEditorOptions.showGrid);
      this.useSnap = new BPxEditorPane.OptionToggle(BPxEditorOptions.useSnap);
      this.showHatch = new BPxEditorPane.OptionToggle(BPxEditorOptions.showHatch);
      this.zoomIn = new ZoomInCommand(this, this.zoomPane);
      this.zoomOut = new ZoomOutCommand(this, this.zoomPane);
      this.zoomReset = new ZoomResetCommand(this, this.zoomPane);
      this.alignLft = new Align(this, true, 0);
      this.alignCenter = new Align(this, true, 2);
      this.alignRgt = new Align(this, true, 1);
      this.alignTop = new Align(this, false, 0);
      this.alignMiddle = new Align(this, false, 2);
      this.alignBot = new Align(this, false, 1);
      this.bringToFront = new Reorg(this, true, false);
      this.sendToBack = new Reorg(this, false, false);
      this.normalTool = new BPxEditorPane.NormalTool(text("commands.normalTool"));
      this.addPolygonTool = new BPxEditorPane.GeometryTool(text("commands.addPolygonTool"), BDrawingTool.addPolygon);
      this.addPathTool = new BPxEditorPane.GeometryTool(text("commands.addPathTool"), BDrawingTool.addPath);
      this.addPointTool = new BPxEditorPane.GeometryTool(text("commands.addPointTool"), BDrawingTool.addPoint);
      this.deletePointTool = new BPxEditorPane.GeometryTool(text("commands.deletePointTool"), BDrawingTool.deletePoint);
      this.xyStatus = new BTextField("0,0", 9, false);
      this.zoomStatus = new BTextField("x1.0", 4, false);
      this.statusPane = new BGridPane(2);
      this.statusPane.add(null, this.zoomStatus);
      this.statusPane.add(null, this.xyStatus);
      propMgr.loadProperties();
      this.mainSplit = new BSplitPane();
      double divPos = propMgr.getd("divPos", -1.0);
      if (divPos == -1.0) {
         this.mainSplit.setDividerPosition(75.0);
      } else {
         this.mainSplit.setDividerPosition(divPos);
      }

      this.linkTo(options, BPxEditorOptions.propertyChanged, optionChanged);
      this.linkTo(this.zoomPane, BZoomPane.zoomLevel, zoomChanged);
   }

   public void setPxRootComponent(BComponent root) {
      this.pxRootComponent = root;
   }

   public BComponent getPxRootComponent() {
      return this.pxRootComponent;
   }

   public void doOptionChanged(BString propertyName) {
      this.zoomScrollPane.relayout();
      this.showGrid.frozen = true;
      this.useSnap.frozen = true;
      this.showHatch.frozen = true;
      this.showGrid.setSelected(this.getOptions().getShowGrid());
      this.useSnap.setSelected(this.getOptions().getUseSnap());
      this.showHatch.setSelected(this.getOptions().getShowHatch());
      this.showGrid.frozen = false;
      this.useSnap.frozen = false;
      this.showHatch.frozen = false;
      this.editor.firePxEvent(new PxEditorEvent(2, propertyName.toString()));
   }

   public void doZoomChanged(BDouble zoomLevel) {
      this.zoomStatus.setText("x" + this.zoomStatusFormat.format(zoomLevel.getDouble()));
   }

   public void deactivated() {
      propMgr.setb("sideBar", this.toggleToolPane.isSelected());
      propMgr.setd("divPos", this.mainSplit.getDividerPosition());
      this.persistSideBarPane();
      propMgr.saveProperties();
   }

   public void doLayout(BWidget[] children) {
      super.doLayout(children);
      this.selected.resetHandles();
   }

   public BMenu[] getViewMenus() {
      BMenu menu = UiLexicon.bajaui().buildMenu("PxEditor");
      menu.add(null, this.editor.getToggleMode());
      menu.add(null, new BSeparator());
      menu.add(null, this.editor.getViewSource());
      menu.add(null, this.editor.getGotoSource());
      menu.add(null, new BSeparator());
      menu.add(null, this.showGrid);
      menu.add(null, this.useSnap);
      menu.add(null, this.showHatch);
      menu.add(null, new BSeparator());
      menu.add(null, this.zoomIn);
      menu.add(null, this.zoomOut);
      menu.add(null, this.zoomReset);
      menu.add(null, new BSeparator());
      menu.add(null, this.validateMedia);
      menu.add(null, this.setTargetMedia);
      return new BMenu[]{menu};
   }

   public BToolBar getViewToolBar() {
      BToolBar toolBar = new BToolBar();
      toolBar.add(null, this.editor.getToggleMode());
      toolBar.add(null, new ToggleCommand(this, lexicon, "commands.sideBarMenu")).setMenuController(new BPxEditorPane.SideBarMenuController());
      toolBar.add(null, new BSeparator());
      toolBar.add(null, this.alignLft);
      toolBar.add(null, this.alignCenter);
      toolBar.add(null, this.alignRgt);
      toolBar.add(null, this.alignTop);
      toolBar.add(null, this.alignMiddle);
      toolBar.add(null, this.alignBot);
      toolBar.add(null, this.bringToFront);
      toolBar.add(null, this.sendToBack);
      toolBar.add(null, new BSeparator());
      toolBar.add(null, this.zoomIn);
      toolBar.add(null, this.zoomOut);
      toolBar.add(null, this.zoomReset);
      toolBar.add(null, new BSeparator());
      toolBar.add("normalTool", this.normalTool);
      toolBar.add("addPolygonTool", this.addPolygonTool);
      toolBar.add("addPathTool", this.addPathTool);
      toolBar.add("addPointTool", this.addPointTool);
      toolBar.add("deletePointTool", this.deletePointTool);
      return toolBar;
   }

   public BWidget getViewStatusBarSupplement() {
      return this.statusPane;
   }

   public void load(Context cx) {
      this.cx = cx;
      this.sanityCheck(this.editor.getWidget());
      this.rootContainer.setRoot(this.editor.getWidget());
      BComponent comp = new BComponent();
      comp.add(null, this.rootContainer);
      this.rootContainer.widgetSupport(this.rootShellMgr);
      this.add(null, comp);
      this.addPolygonTool.setEnabled(!this.editor.isReadonly());
      this.addPathTool.setEnabled(!this.editor.isReadonly());
      this.addPointTool.setEnabled(!this.editor.isReadonly());
      this.deletePointTool.setEnabled(!this.editor.isReadonly());
      boolean canModifyTargetMedia = !this.editor.isReadonly() || !this.editor.isFileBased();
      if (this.editor.getPxAgent() != null) {
         BAbstractPxView view = this.editor.getPxAgent();
         if (Flags.isReadonly(view, view.getProperty(BAbstractPxView.media.getName()))) {
            canModifyTargetMedia = false;
         }
      }

      this.setTargetMedia.setEnabled(canModifyTargetMedia);
      this.editor.setTransferWidget(this.studio);
      ToggleCommandGroup<ToggleCommand> group = new ToggleCommandGroup();
      group.add(this.normalTool);
      group.add(this.addPolygonTool);
      group.add(this.addPathTool);
      group.add(this.addPointTool);
      group.add(this.deletePointTool);
      this.tool = BDrawingTool.normal;
      this.updateToolbar();
      this.loadProperties(this.editor.getWidget(), this.editor.getPxProperties());
      this.layerMgr.adjustInvisibleWidgets(false);
      addDynamicPxIncludeProperties(this.editor.getWidget());
      this.forceRootLayout();
      this.loaded = true;
      this.restoreState();
   }

   public static void addDynamicPxIncludeProperties(BWidget widget) {
      if (widget instanceof BPxInclude) {
         BPxInclude inc = (BPxInclude)widget;
         Object[] obj = (Object[])inc.fw(304, null, null, null, null);
         PxProperty[] incProps = (PxProperty[])obj[1];

         for (int i = 0; i < incProps.length; i++) {
            String n = incProps[i].getName();
            BValue v = incProps[i].getValue();
            BValue dyn = inc.get(n);
            if (dyn == null) {
               inc.add(n, v);
            }
         }
      } else {
         BWidget[] kids = widget.getChildWidgets();

         for (int ix = 0; ix < kids.length; ix++) {
            addDynamicPxIncludeProperties(kids[ix]);
         }
      }
   }

   private void loadProperties(BWidget root, PxProperty[] props) {
      PxUtil.loadProperties(root, props, this.propComps);
   }

   private void sanityCheck(BWidget widget) {
      if (widget instanceof BShape) {
         widget.setLayout(BLayout.DEFAULT);
      } else {
         if (widget instanceof BCanvasPane) {
            BCanvasPane cp = (BCanvasPane)widget;
            BSize size = cp.getViewSize();
            double sw = this.ensureBetween(size.width(), 0, 10000);
            double sh = this.ensureBetween(size.height(), 0, 10000);
            cp.setViewSize(BSize.make(sw, sh));
         }

         BWidget editor = widget.getParentWidget();
         if (editor != null && editor instanceof BCanvasPane) {
            BLayout ly = widget.getLayout();
            double x = this.ensureBetween(ly.getX(), -1000, 10000);
            double y = this.ensureBetween(ly.getY(), -1000, 10000);
            double w = this.ensureBetween(ly.getWidth(), -1000, 10000);
            double h = this.ensureBetween(ly.getHeight(), -1000, 10000);
            int xu = ly.getXUnit();
            int yu = ly.getYUnit();
            int wu = ly.getWidthUnit();
            int hu = ly.getHeightUnit();
            widget.setLayout(BLayout.make(x, xu, y, yu, w, wu, h, hu));
         } else {
            widget.setLayout(BLayout.DEFAULT);
         }
      }

      BWidget[] kids = widget.getChildWidgets();

      for (int i = 0; i < kids.length; i++) {
         this.sanityCheck(kids[i]);
      }
   }

   private double ensureBetween(double v, int min, int max) {
      if (v < min) {
         return min;
      } else {
         return v > max ? max : v;
      }
   }

   public BObject save(BObject value, Context cx) {
      if (value instanceof BPxFile) {
         this.saveDirect((BPxFile)value);
      } else if (this.editor.getPxAgent() instanceof BPxView) {
         this.saveViaAgent((BPxView)this.editor.getPxAgent());
      }

      return value;
   }

   private void saveDirect(BPxFile file) {
      BPxView dummyAgent = new BPxView();
      dummyAgent.setMedia(this.editor.getMedia().getType().getTypeSpec());
      this.persist(file, dummyAgent);
   }

   private void saveViaAgent(BPxView agent) {
      BOrd ord = agent.getPxFile();
      if (ord.isNull()) {
         throw new IllegalStateException();
      } else {
         BIFile file = (BIFile)ord.get(this.editor.getCurrentValue());
         this.persist(file, agent);
      }
   }

   private void persist(BIFile file, BPxView agent) {
      this.layerMgr.adjustInvisibleWidgets(true);
      this.editor.setWidget(this.rootContainer.getRoot());
      this.normalizeLayouts(this.editor.getWidget());

      try {
         this.encode(new ByteArrayOutputStream(), agent);
         this.encode(file.getOutputStream(), agent);
      } catch (Exception var4) {
         throw new BajaRuntimeException(var4);
      }

      this.layerMgr.adjustInvisibleWidgets(false);
   }

   private void encode(OutputStream out, BPxView agent) throws Exception {
      PxEncoder encoder = new PxEncoder(out);
      PxProperty[] pxProperties = this.editor.getPxProperties();
      boolean hasProperties = pxProperties != null && pxProperties.length > 0;
      encoder.setPreserveIdentities(this.getOptions().getPreserveIdentities() || hasProperties);
      encoder.encodeDocument(this.editor.getWidget(), this.editor.getPxProperties(), this.editor.getPxLayers(), agent);
      encoder.close();
   }

   private void normalizeLayouts(BWidget widget) {
      if (widget instanceof BShape) {
         widget.setLayout(BLayout.DEFAULT);
      } else {
         BWidget editor = widget.getParentWidget();
         if (editor == null || !(editor instanceof BCanvasPane)) {
            widget.setLayout(BLayout.DEFAULT);
         }
      }

      BWidget[] kids = widget.getChildWidgets();

      for (int i = 0; i < kids.length; i++) {
         this.normalizeLayouts(kids[i]);
      }
   }

   public BBoundOrds getBoundOrds() {
      return this.boundOrds;
   }

   public CommandArtifact relativizeOrds() {
      return this.boundOrds == null ? null : this.boundOrds.relativizeORds();
   }

   public void updateStatus(String text) {
      this.xyStatus.setText(text);
   }

   public BPxEditorOptions getOptions() {
      return BPxEditorOptions.make();
   }

   public void updateToolbar() {
      BTransferWidget trans = this.editor.getTransferWidget();
      if (trans != null) {
         if (this.tool.getOrdinal() == 0) {
            BCanvasPane canvas = this.studio.getCurrentCanvas();
            this.alignLft.setEnabled(this.studio.alignable(canvas));
            this.alignCenter.setEnabled(this.studio.alignable(canvas));
            this.alignRgt.setEnabled(this.studio.alignable(canvas));
            this.alignTop.setEnabled(this.studio.alignable(canvas));
            this.alignMiddle.setEnabled(this.studio.alignable(canvas));
            this.alignBot.setEnabled(this.studio.alignable(canvas));
            BPane freeForm = this.studio.getCurrentFreeForm();
            this.bringToFront.setEnabled(this.studio.reorgable(freeForm));
            this.sendToBack.setEnabled(this.studio.reorgable(freeForm));
            this.treePane.updateEnabledButtons(this.studio.reorgable(freeForm));
            this.updateTransferToolbar(trans);
         } else {
            this.alignLft.setEnabled(false);
            this.alignCenter.setEnabled(false);
            this.alignRgt.setEnabled(false);
            this.alignTop.setEnabled(false);
            this.alignMiddle.setEnabled(false);
            this.alignBot.setEnabled(false);
            this.bringToFront.setEnabled(false);
            this.sendToBack.setEnabled(false);
            trans.setCutEnabled(false);
            trans.setCopyEnabled(false);
            trans.setPasteEnabled(false);
            trans.setDuplicateEnabled(false);
            trans.setDeleteEnabled(false);
         }
      }
   }

   private void updateTransferToolbar(BTransferWidget trans) {
      if (this.selected.size() == 0) {
         trans.setCutEnabled(false);
         trans.setCopyEnabled(false);
         trans.setPasteEnabled(false);
         trans.setDuplicateEnabled(false);
         trans.setDeleteEnabled(false);
      } else if (this.selected.size() == 1) {
         BWidget widget = this.selected.get(0);
         if (widget instanceof BNullWidget) {
            trans.setCutEnabled(false);
            trans.setCopyEnabled(false);
            trans.setPasteEnabled(true);
            trans.setDuplicateEnabled(false);
            trans.setDeleteEnabled(false);
         } else {
            trans.setCutEnabled(true);
            trans.setCopyEnabled(true);
            trans.setPasteEnabled(true);
            trans.setDuplicateEnabled(true);
            trans.setDeleteEnabled(true);
            BWidget parent = widget.getParentWidget();
            if (parent == null || !Reflector.isFreeFormPane(parent)) {
               trans.setDuplicateEnabled(false);
            }

            if (!Reflector.isFreeFormPane(widget)) {
               trans.setPasteEnabled(false);
            }
         }
      } else {
         trans.setCutEnabled(true);
         trans.setCopyEnabled(true);
         trans.setPasteEnabled(false);
         trans.setDuplicateEnabled(true);
         trans.setDeleteEnabled(true);
      }

      if (trans.isCutEnabled()) {
         trans.setCutEnabled(!this.editor.isReadonly());
      }

      if (trans.isPasteEnabled()) {
         trans.setPasteEnabled(!this.editor.isReadonly());
      }

      if (trans.isDuplicateEnabled()) {
         trans.setDuplicateEnabled(!this.editor.isReadonly());
      }

      if (trans.isDeleteEnabled()) {
         trans.setDeleteEnabled(!this.editor.isReadonly());
      }
   }

   protected void setChildWidgets(BWidget center) {
      this.setCenter(center);
   }

   public void forceRootLayout() {
      forceImageLoad(this.rootContainer, this.editor.getWbShell().getActiveOrd());
      this.doForceLayout(this.rootContainer, false);
   }

   private static void forceImageLoad(BComplex comp, BOrd baseOrd) {
      SlotCursor<Property> c = comp.getProperties();

      while (c.nextObject()) {
         BObject obj = c.get();
         if (obj instanceof BImage) {
            BImage img = (BImage)obj;
            img.sync();
            img.setBaseOrd(baseOrd);
         } else if (obj instanceof BBrush) {
            loadBrush((BBrush)obj, baseOrd);
         } else if (obj instanceof BBorder) {
            BBorder b = (BBorder)obj;
            loadBrush(b.topBrush, baseOrd);
            loadBrush(b.leftBrush, baseOrd);
            loadBrush(b.bottomBrush, baseOrd);
            loadBrush(b.rightBrush, baseOrd);
         } else if (obj instanceof BComplex) {
            forceImageLoad((BComplex)obj, baseOrd);
         }
      }
   }

   private static void loadBrush(BBrush brush, BOrd baseOrd) {
      Paint p = brush.getPaint();
      if (p instanceof Image) {
         BImage img = ((Image)p).getImage();
         img.sync();
         img.setBaseOrd(baseOrd);
      }
   }

   private void doForceLayout(BWidget widget, boolean insidePxInclude) {
      if (widget instanceof BCanvasPane) {
         if (insidePxInclude) {
            widget.fw(304, new Object[]{Boolean.FALSE}, new Object[]{Boolean.FALSE}, null, null);
         } else {
            widget.fw(
               304,
               new Object[]{this.getOptions().getShowGrid(), this.getOptions().getGridSize(), this.getOptions().getGridColor()},
               new Object[]{this.getOptions().getShowHatch(), this.getOptions().getHatchColor()},
               null,
               null
            );
         }
      }

      BWidget[] kids = widget.getChildWidgets();
      widget.doLayout(kids);

      for (int i = 0; i < kids.length; i++) {
         this.doForceLayout(kids[i], insidePxInclude || widget instanceof BPxInclude);
      }
   }

   public void initSideBarPane() {
      this.sideBarPane = new BToolPane();
      this.sideBars = this.editor
         .getPxProfile()
         .getSideBars(
            new BPxSideBar[]{
               this.boundOrds = new BBoundOrds(this.editor),
               this.treePane = new BPxTreePane(this),
               this.propSheet = new BPxPropSheet(this.editor),
               this.layerSheet = new BPxLayerSheet(this.editor),
               this.cellSheet = new BPxCellSheet(this.editor)
            }
         );
      int len = this.sideBars.length;
      if (len > 0) {
         Boolean[] vstate = new Boolean[len];

         for (int i = 0; i < len; i++) {
            vstate[i] = Boolean.TRUE;
         }

         String h = Integer.valueOf(10000 / len).toString();
         StringBuilder toolpaneDefaults = new StringBuilder("toolpane=");
         StringBuilder visibleDefaults = new StringBuilder("visible=");

         for (int i = 0; i < len; i++) {
            if (i > 0) {
               toolpaneDefaults.append(";");
               visibleDefaults.append(";");
            }

            toolpaneDefaults.append(h);
            visibleDefaults.append("true");
         }

         String height = propMgr.get("sidePanesHeight", toolpaneDefaults.toString());
         String visible = propMgr.get("sidePanesVisible", visibleDefaults.toString());
         int hlen = TextUtil.split(height.substring("toolpane=".length()), ';').length;
         String[] vtokens = TextUtil.split(visible.substring("visible=".length()), ';');
         if (vtokens.length != len) {
            for (int i = 0; i < this.sideBars.length; i++) {
               this.sideBarPane
                  .addPane(
                     this.sideBars[i].getSideBarDescription(),
                     Theme.javaFx().shouldHideNonessentialIcons() ? BImage.DEFAULT : this.sideBars[i].getSideBarIcon(),
                     this.sideBars[i]
                  );
            }
         } else {
            for (int i = 0; i < len; i++) {
               vstate[i] = Boolean.valueOf(vtokens[i]);
               if (vstate[i]) {
                  this.sideBarPane
                     .addPane(
                        this.sideBars[i].getSideBarDescription(),
                        Theme.javaFx().shouldHideNonessentialIcons() ? BImage.DEFAULT : this.sideBars[i].getSideBarIcon(),
                        this.sideBars[i]
                     );
               }
            }

            this.sideBarPane.unpickle(height);
         }

         this.sideBarVisibility = new LinkedHashMap<>();

         for (int ix = 0; ix < this.sideBars.length; ix++) {
            this.sideBarVisibility.put(this.sideBars[ix], vstate[ix]);
         }
      }

      this.toggleToolPane = new BPxEditorPane.ToggleToolPane();
      if (len > 0) {
         this.toggleToolPane.setSelected(propMgr.getb("sideBar", true));
         this.toggleToolPane.invoke();
      } else {
         this.setChildWidgets(new BBorderPane(this.zoomScrollPane, BBorder.DEFAULT, BInsets.DEFAULT));
      }

      this.sideBarPane.setMenuController(new MenuController() {
         public BMenu getMenu(BToolPane sideBarPane, BWidget content) {
            BMenu menu = new BMenu();
            menu.add(null, BPxEditorPane.this.new CloseSideBar(content));
            return menu;
         }
      });
   }

   private void persistSideBarPane() {
      propMgr.set("sidePanesHeight", this.sideBarPane.pickle());
      StringBuilder sb = new StringBuilder("visible=");
      Iterator<Boolean> it = this.sideBarVisibility.values().iterator();

      for (int n = 0; it.hasNext(); sb.append(it.next())) {
         if (n++ > 0) {
            sb.append(";");
         }
      }

      propMgr.set("sidePanesVisible", sb.toString());
   }

   private void reloadSideBarPane() {
      BLabelPane[] lp = (BLabelPane[])this.sideBarPane.getChildren(BLabelPane.class);

      for (int i = 0; i < lp.length; i++) {
         lp[i].setContent(new BNullWidget());
      }

      this.sideBarPane.removeAll();

      for (int i = 0; i < this.sideBars.length; i++) {
         Boolean state = this.sideBarVisibility.get(this.sideBars[i]);
         if (state) {
            this.sideBarPane
               .addPane(
                  this.sideBars[i].getSideBarDescription(),
                  Theme.javaFx().shouldHideNonessentialIcons() ? BImage.DEFAULT : this.sideBars[i].getSideBarIcon(),
                  this.sideBars[i]
               );
         }
      }

      this.refresh();
   }

   private void restoreState() {
      if (STATE_ENABLED) {
         BWbShell shell = BWbShell.getWbShell(this);
         if (shell != null) {
            Map<String, Object> state = pxEditorStates.get(shell.getActiveOrd());
            if (state != null) {
               double zoomLevel = ((BDouble)state.get("zoomLevel")).getDouble();
               RectGeom viewport = (RectGeom)state.get("scrollViewPort");
               this.zoomPane.setZoomLevel(zoomLevel);
               this.zoomScrollPane.scrollToVisible(viewport);
            }
         }
      }
   }

   public void saveState() {
      if (STATE_ENABLED) {
         BWbShell shell = BWbShell.getWbShell(this);
         if (shell != null) {
            Map<String, Object> state = new HashMap<>();
            state.put("zoomLevel", BDouble.make(this.zoomPane.getZoomLevel()));
            state.put("scrollViewPort", this.zoomScrollPane.getViewport());
            pxEditorStates.put(shell.getActiveOrd(), state);
         }
      }
   }

   private void refresh() {
      BWbShell shell = BWbShell.getWbShell(this);
      if (shell != null) {
         Command refresh = shell.getRefreshCommand();
         if (refresh != null) {
            refresh.invoke();
         }
      }
   }

   public BPxEditor getPxEditor() {
      return this.editor;
   }

   public SelectedWidgets getSelectedWidgets() {
      return this.selected;
   }

   public RootShellManager getRootShellMgr() {
      return this.rootShellMgr;
   }

   public BRootContainer getRootContainer() {
      return this.rootContainer;
   }

   public CommandStudio getCommandStudio() {
      return this.studio;
   }

   public PainterStudio getPainterStudio() {
      return this.studio;
   }

   public RootStudio getRootStudio() {
      return this.studio;
   }

   public TrackerStudio getTrackerStudio() {
      return this.studio;
   }

   public TreeStudio getTreeStudio() {
      return this.studio;
   }

   public BStudio getStudio() {
      return this.studio;
   }

   public BDrawingTool getTool() {
      return this.tool;
   }

   public static PropertyManager propMgr() {
      return propMgr;
   }

   public static Lexicon lexicon() {
      return lexicon;
   }

   public static String text(String s) {
      return lexicon.getText(s);
   }

   public static BImage icon(String s) {
      return BImage.make(lexicon.getText(s));
   }

   public PxPropertyComponentArray getPxPropertyComponents() {
      return this.propComps;
   }

   public void setPxPropertyComponents(PxPropertyComponentArray propComps) {
      this.propComps = propComps;
      propComps.updateTargets(this.editor.getWidget(), this.editor.getPxProperties());
   }

   public LayerManager getLayerManager() {
      return this.layerMgr;
   }

   class CloseSideBar extends Command {
      BWidget sideBar;

      CloseSideBar(BWidget sideBar) {
         super(sideBar, BPxEditorPane.wbLexicon, "sideBar.close");
         this.sideBar = sideBar;
      }

      public CommandArtifact doInvoke() {
         BPxEditorPane.this.sideBarVisibility.put((BPxSideBar)this.sideBar, Boolean.FALSE);
         BPxEditorPane.this.reloadSideBarPane();
         return null;
      }
   }

   class GeometryTool extends ToggleCommand {
      BDrawingTool toolMode;

      GeometryTool(String base, BDrawingTool toolMode) {
         super(BPxEditorPane.this, BPxEditorPane.lexicon, base);
         this.toolMode = toolMode;
         super.setSelected(false);
      }

      public void setSelected(boolean s) {
         super.setSelected(s);
         if (s) {
            BPxEditorPane.this.selected.deselectAll();
            BPxEditorPane.this.tool = this.toolMode;
            switch (BPxEditorPane.this.tool.getOrdinal()) {
               case 1:
                  BPxEditorPane.this.studio.setTracker(new AddPolygonTracker(BPxEditorPane.this, BPxEditorPane.this.studio));
                  break;
               case 2:
                  BPxEditorPane.this.studio.setTracker(new AddPathTracker(BPxEditorPane.this, BPxEditorPane.this.studio));
                  break;
               case 3:
                  BPxEditorPane.this.studio.setTracker(new AddPointTracker(BPxEditorPane.this, BPxEditorPane.this.studio));
                  break;
               case 4:
                  BPxEditorPane.this.studio.setTracker(new DeletePointTracker(BPxEditorPane.this, BPxEditorPane.this.studio));
                  break;
               default:
                  throw new IllegalStateException();
            }

            BPxEditorPane.this.editor.firePxEvent(new PxEditorEvent(3, BPxEditorPane.this.tool));
         }
      }
   }

   class NormalTool extends ToggleCommand {
      NormalTool(String base) {
         super(BPxEditorPane.this, BPxEditorPane.lexicon, base);
         super.setSelected(true);
      }

      public void setSelected(boolean s) {
         super.setSelected(s);
         if (s && BPxEditorPane.this.loaded) {
            BPxEditorPane.this.tool = BDrawingTool.normal;
            BPxEditorPane.this.studio.setTracker(new UnpressedTracker(BPxEditorPane.this, BPxEditorPane.this.studio));
            BPxEditorPane.this.studio.setPainter(new DefaultPainter(BPxEditorPane.this.getPainterStudio()));
            BPxEditorPane.this.selected.deselectAll();
            BPxEditorPane.this.editor.firePxEvent(new PxEditorEvent(3, BPxEditorPane.this.tool));
         }
      }
   }

   class OptionToggle extends ToggleCommand {
      Property prop;
      boolean frozen = false;

      OptionToggle(Property prop) {
         super(BPxEditorPane.this, BPxEditorPane.lexicon, "commands." + prop.getName());
         this.prop = prop;
         super.setSelected(BPxEditorPane.this.getOptions().getBoolean(prop));
      }

      public void setSelected(boolean s) {
         super.setSelected(s);
         if (!this.frozen) {
            BPxEditorPane.this.getOptions().setBoolean(this.prop, s);
            BPxEditorPane.this.studio.getPainter().reset();
            BPxEditorPane.this.refresh();
         }
      }
   }

   class SetTargetMedia extends Command {
      SetTargetMedia() {
         super(BPxEditorPane.this, BPxEditorPane.lexicon, "commands.setTargetMedia");
      }

      public CommandArtifact doInvoke() throws Exception {
         String curMedia = BPxEditorPane.this.editor.getMedia().getType().toString();
         int idx = 0;
         BList list = new BList();
         list.setMultipleSelection(false);
         TypeInfo[] types = Sys.getRegistry().getConcreteTypes(BPxMedia.TYPE.getTypeInfo());

         for (int i = 0; i < types.length; i++) {
            if (types[i].toString().equals(curMedia)) {
               idx = i;
            }

            TypeInfoSpec m = new TypeInfoSpec(types[i]);
            list.addItem(m.icon, m);
         }

         list.setSelectedIndex(idx);
         String t = BPxEditorPane.text("commands.setTargetMedia.label");
         int r = BDialog.open(BPxEditorPane.this, t, list, 3);
         if (r != 1) {
            return null;
         } else {
            idx = list.getSelectedIndex();
            BPxMedia newMedia = (BPxMedia)types[idx].getInstance();
            r = PxMediaValidationUtil.validateMediaDialog(newMedia, null, BPxEditorPane.this.getPxEditor(), BPxEditorPane.this.cx);
            if (r == 8) {
               return null;
            } else {
               BPxEditorPane.SetTargetMedia.Artifact artifact = new BPxEditorPane.SetTargetMedia.Artifact(BPxEditorPane.this.editor.getMedia(), newMedia);
               artifact.redo();
               return artifact;
            }
         }
      }

      class Artifact implements CommandArtifact {
         BPxMedia oldMedia;
         BPxMedia newMedia;

         Artifact(BPxMedia oldMedia, BPxMedia newMedia) {
            this.oldMedia = oldMedia;
            this.newMedia = newMedia;
         }

         public void redo() throws Exception {
            BPxEditorPane.this.editor.setMedia(this.newMedia);
            if (BPxEditorPane.this.editor.isFileBased()) {
               BPxEditorPane.this.editor.setModified();
            }
         }

         public void undo() throws Exception {
            BPxEditorPane.this.editor.setMedia(this.oldMedia);
            if (BPxEditorPane.this.editor.isFileBased()) {
               BPxEditorPane.this.editor.setModified();
            }
         }
      }
   }

   class SideBarMenuController implements javax.baja.ui.BAbstractButton.MenuController {
      public boolean isMenuDistinct() {
         return false;
      }

      public BMenu getMenu(BAbstractButton b) {
         if (BPxEditorPane.this.sideBars.length <= 0) {
            return null;
         } else {
            BMenu menu = UiLexicon.bajaui().buildMenu("menu.sidebar.label");
            menu.add(null, BPxEditorPane.this.toggleToolPane);
            menu.add(null, new BSeparator());

            for (int i = 0; i < BPxEditorPane.this.sideBars.length; i++) {
               menu.add(null, BPxEditorPane.this.new ToggleSideBar(BPxEditorPane.this.sideBars[i]));
            }

            return menu;
         }
      }
   }

   class ToggleSideBar extends ToggleCommand {
      BPxSideBar sideBar;

      ToggleSideBar(BPxSideBar sideBar) {
         super(BPxEditorPane.this, sideBar.getSideBarDescription());
         this.sideBar = sideBar;
         Boolean state = BPxEditorPane.this.sideBarVisibility.get(sideBar);
         this.setSelected(state);
      }

      public CommandArtifact doInvoke() throws Exception {
         Boolean state = BPxEditorPane.this.sideBarVisibility.get(this.sideBar);
         BPxEditorPane.this.sideBarVisibility.put(this.sideBar, state ? Boolean.FALSE : Boolean.TRUE);
         BPxEditorPane.this.reloadSideBarPane();
         return null;
      }
   }

   class ToggleToolPane extends ToggleCommand {
      ToggleToolPane() {
         super(BPxEditorPane.this, BPxEditorPane.lexicon, "commands.showSideBar");
      }

      public CommandArtifact doInvoke() throws Exception {
         BPxEditorPane.this.mainSplit.setWidget1(new BNullWidget());
         BPxEditorPane.this.mainSplit.setWidget2(new BNullWidget());
         BWidget p = BPxEditorPane.this.zoomScrollPane.getParentWidget();
         if (p != null) {
            ((BBorderPane)p).setContent(new BNullWidget());
         }

         if (this.isSelected()) {
            BPxEditorPane.this.mainSplit.setWidget1(new BBorderPane(BPxEditorPane.this.zoomScrollPane, BBorder.none, BInsets.DEFAULT));
            BPxEditorPane.this.mainSplit.setWidget2(BPxEditorPane.this.sideBarPane);
            BPxEditorPane.this.setChildWidgets(BPxEditorPane.this.mainSplit);
         } else {
            BPxEditorPane.this.setChildWidgets(new BBorderPane(BPxEditorPane.this.zoomScrollPane, BBorder.none, BInsets.DEFAULT));
         }

         return null;
      }
   }

   class ValidateMedia extends Command {
      ValidateMedia() {
         super(BPxEditorPane.this, BPxEditorPane.lexicon, "commands.validateMedia");
      }

      public CommandArtifact doInvoke() throws Exception {
         PxMediaValidationUtil.validateMediaDialog(null, null, BPxEditorPane.this.getPxEditor(), BPxEditorPane.this.cx);
         return null;
      }
   }
}
