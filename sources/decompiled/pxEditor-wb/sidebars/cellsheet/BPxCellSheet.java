package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.commands.GotoOrd;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BConverterCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxLayerCE;
import com.tridium.px.editor.sidebars.cellsheet.celleditors.BPxPropertyCE;
import com.tridium.px.editor.sidebars.cellsheet.commands.AddBinding;
import com.tridium.px.editor.sidebars.cellsheet.commands.AddConverter;
import com.tridium.px.editor.sidebars.cellsheet.commands.CopyCell;
import com.tridium.px.editor.sidebars.cellsheet.commands.DeleteConverter;
import com.tridium.px.editor.sidebars.cellsheet.commands.LinkPxProperty;
import com.tridium.px.editor.sidebars.cellsheet.commands.PasteCell;
import com.tridium.px.editor.sidebars.cellsheet.commands.UnlinkPxProperty;
import com.tridium.px.editor.util.EventUtil;
import com.tridium.px.editor.util.Reflector;
import com.tridium.ui.theme.Theme;
import com.tridium.util.AbstractStubGen;
import com.tridium.util.ClassUtil;
import com.tridium.workbench.celleditors.BOrdCE;
import com.tridium.workbench.celleditors.BStringCE;
import com.tridium.workbench.web.browser.BWebWidget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.BPxSideBar;
import javax.baja.px.editor.event.PxComponentEvent;
import javax.baja.px.editor.event.PxEditorEvent;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxLayerEvent;
import javax.baja.px.editor.event.PxListener;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.registry.TypeInfo;
import javax.baja.space.Mark;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BStruct;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BLabel;
import javax.baja.ui.BLayout;
import javax.baja.ui.BMenu;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BValueBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.naming.BWidgetScheme;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.ui.px.BPxInclude;
import javax.baja.ui.px.PxProperty;
import javax.baja.ui.transfer.Clipboard;
import javax.baja.ui.transfer.TransferEnvelope;
import javax.baja.ui.transfer.TransferFormat;
import javax.baja.ui.util.WebProperty;
import javax.baja.util.BConverter;
import javax.baja.util.Lexicon;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "cellModified",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   ), @NiagaraAction(
      name = "expanderChanged",
      parameterType = "BWidgetEvent",
      defaultValue = "new BWidgetEvent()"
   )})
public class BPxCellSheet extends BPxSideBar implements PxListener {
   public static final Action cellModified = newAction(0, new BWidgetEvent(), null);
   public static final Action expanderChanged = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BPxCellSheet.class);
   private static Lexicon LEX = Lexicon.make("pxEditor");
   private static String DESC = LEX.getText("cellsheet.label");
   private static BImage ICON = BImage.make(LEX.getText("cellsheet.icon"));
   private BPxEditor editor;
   private BPxEditorPane editorPane;
   private CellSheetContext context;
   private BEdgePane contents;
   private BWidget widget;
   private BPxCellTable[] cellTables;
   private double columnWidth = 75.0;
   private BWidget[] allWidgets;
   private Class<?> baseClass;
   private BPxCellSheetLabel label;
   private Command addBinding;
   private ToggleCommand alphabetize;
   private ToggleCommand categorize;
   private boolean alphabetical = true;
   private BPxCellTable alphaTable;
   private List<String> links = new ArrayList<>();
   private Map<String, Boolean> expanderStates = new HashMap<>();
   private BStringCE textCE;
   private boolean readonly;

   public void cellModified(BWidgetEvent parameter) {
      this.invoke(cellModified, parameter, null);
   }

   public void expanderChanged(BWidgetEvent parameter) {
      this.invoke(expanderChanged, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPxCellSheet(BPxEditor editor) {
      super(editor);
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
      this.context = new DefaultCellSheetContext(this.editorPane, this);
      editor.addPxListener(this);
      this.label = new BPxCellSheetLabel(this.editorPane, this);
      this.addBinding = new AddBinding(this.editorPane, this);
      this.addBinding.setEnabled(false);
      this.alphabetize = new BPxCellSheet.Alphabetize();
      this.categorize = new BPxCellSheet.Categorize();
      ToggleCommandGroup<ToggleCommand> group = new ToggleCommandGroup();
      group.add(this.alphabetize);
      group.add(this.categorize);
      BToolBar toolbar = new BToolBar();
      toolbar.add(null, new BSeparator());
      toolbar.add(null, this.addBinding);
      toolbar.add(null, this.alphabetize);
      toolbar.add(null, this.categorize);
      BEdgePane e2 = new BEdgePane();
      e2.setCenter(new BBorderPane(this.label, 2.0, 2.0, 2.0, 2.0));
      e2.setRight(toolbar);
      this.contents = new BEdgePane();
      this.contents.setTop(e2);
      this.contents.setCenter(new BScrollPane());
      this.setContent(this.contents);
   }

   public BPxCellSheet(BPxEditor editor, CellSheetContext context) {
      this(editor);
      this.context = context;
   }

   public final boolean isReadonly() {
      return this.readonly;
   }

   public final void setReadonly(boolean readonly) {
      if (this.readonly != readonly) {
         this.readonly = readonly;
         this.addBinding.setEnabled(!readonly);
         this.alphabetize.setEnabled(!readonly);
         this.categorize.setEnabled(!readonly);
      }
   }

   private boolean preventEdits() {
      return this.isReadonly() || !this.editorPane.getLayerManager().allNormal(this.allWidgets);
   }

   public void doCellModified(BWidgetEvent event) {
      this.context.cellModified((BWbCellEditor)event.getWidget());
   }

   public void doExpanderChanged(BWidgetEvent event) {
      BCellSheetExpandablePane exp = (BCellSheetExpandablePane)event.getWidget();
      BWidget sum = exp.getSummary();
      if (sum instanceof BBorderPane) {
         BBorderPane border = (BBorderPane)sum;
         BLabel label = (BLabel)border.getContent();
         String text = label.getText();
         this.expanderStates.put(text, exp.isExpanded());
      } else if (sum instanceof BPxCellSheetBindingLabel) {
         BPxCellSheetBindingLabel lab = (BPxCellSheetBindingLabel)sum;
         this.expanderStates.put(lab.getText(), exp.isExpanded());
      }
   }

   BMenu makePopup(BWbCellEditor ce, CellTableContext tableContext) {
      boolean isSingle = this.allWidgets.length == 1;
      boolean isBound = ce instanceof BConverterCE;
      boolean isGrouped = ce instanceof BPxPropertyCE;
      String propName = ce.getPropertyName();
      BMenu menu = new BMenu();
      BObject to = this.widget.get(propName);
      if (to != null && tableContext.allowAnimate()) {
         BBinding[] bindings = (BBinding[])this.allWidgets[0].getChildren(BValueBinding.class);
         if (isBound) {
            menu.add(null, new AddConverter(this.editorPane, this.context, false, propName, new BBinding[0], new BObject[0], null));
         } else {
            BObject[] from = this.context.resolveBindingTarget(bindings);
            TypeInfo toType = to.getType().getTypeInfo();
            boolean enabled = isSingle && !isGrouped;
            menu.add(
               null,
               ClassUtil.allNull(from)
                  ? this.offlineConverter(enabled, propName, to, toType, bindings)
                  : this.onlineConverter(enabled, propName, to, toType, bindings, from)
            );
         }

         menu.add(null, new DeleteConverter(this.editorPane, this.context, isSingle && isBound, propName, Reflector.converter(propName, bindings)));
      }

      BSubMenuItem groupMenu = new BSubMenuItem();
      groupMenu.setText(BPxEditorPane.text("commands.link.label"));
      groupMenu.setImage(BPxEditorPane.icon("commands.link.icon"));
      boolean hasGroups = this.editor.getPxProperties().length > 0;
      if (!isBound && !isGrouped && hasGroups) {
         BMenu groupSubMenu = this.groupSubMenu(tableContext, propName);
         if (groupSubMenu != null) {
            groupMenu.setMenu(this.groupSubMenu(tableContext, propName));
         } else {
            groupMenu.setEnabled(false);
         }
      } else {
         groupMenu.setEnabled(false);
      }

      menu.add(null, groupMenu);
      menu.add(null, new UnlinkPxProperty(this.editorPane, this.context, tableContext.getGroupableComponents(), propName, isGrouped));
      menu.add(null, new CopyCell(this.editorPane, ce));
      Command paste = new PasteCell(this.editorPane, ce, tableContext);

      try {
         TransferEnvelope envelope = null;
         envelope = Clipboard.getDefault().getContents();
         if (envelope != null) {
            if (envelope.supports(TransferFormat.mark)) {
               Mark mark = (Mark)envelope.getData(TransferFormat.mark);
               if (mark.isPendingMove()) {
                  paste.setEnabled(false);
               } else {
                  BObject[] objects = ((Mark)envelope.getData(TransferFormat.mark)).getValues();
                  if (objects.length != 1 || !ce.getCurrentValue().getClass().isInstance(objects[0])) {
                     paste.setEnabled(false);
                  }
               }
            } else {
               BObject[] objects = ((Mark)envelope.getData(TransferFormat.mark)).getValues();
               if (objects.length != 1 || !ce.getCurrentValue().getClass().isInstance(objects[0])) {
                  paste.setEnabled(false);
               }
            }
         }
      } catch (Exception var15) {
         paste.setEnabled(false);
      }

      menu.add(null, paste);
      if (ce instanceof BOrdCE && this.context.allowGotoOrd()) {
         menu.add(null, new GotoOrd(this, (BOrd)ce.getCurrentValue()));
      }

      return menu;
   }

   private BMenu groupSubMenu(CellTableContext tableContext, String propName) {
      BComponent[] comps = tableContext.getGroupableComponents();
      Property prop = comps[0].getProperty(propName);
      if (prop == null) {
         return null;
      } else {
         Type compType = prop.getType();
         BMenu menu = new BMenu();
         PxProperty[] props = this.editor.getPxProperties();

         for (int i = 0; i < props.length; i++) {
            PxProperty g = props[i];
            boolean enabled = compType.equals(g.getTypeSpec().getResolvedType());
            menu.add(null, new LinkPxProperty(this.editorPane, this.context, g, comps, propName, enabled));
         }

         return menu;
      }
   }

   private AddConverter onlineConverter(boolean enabled, String propName, BObject to, TypeInfo toType, BBinding[] bindings, BObject[] from) {
      List<BBinding> usableBindings = new ArrayList<>();
      List<BObject> usableFrom = new ArrayList<>();

      for (int i = 0; i < bindings.length; i++) {
         if (from[i] != null) {
            TypeInfo fromType = from[i].getType().getTypeInfo();
            TypeInfo[] adapters = Sys.getRegistry().getAdapters(fromType, toType);
            if (adapters.length > 0) {
               usableBindings.add(bindings[i]);
               usableFrom.add(from[i]);
            }
         }
      }

      return new AddConverter(
         this.editorPane,
         this.context,
         enabled && usableBindings.size() > 0,
         propName,
         usableBindings.toArray(new BBinding[0]),
         usableFrom.toArray(new BObject[0]),
         to
      );
   }

   private AddConverter offlineConverter(boolean enabled, String propName, BObject to, TypeInfo toType, BBinding[] bindings) {
      List<BBinding> usableBindings = new ArrayList<>();
      List<BObject> usableFrom = new ArrayList<>();

      for (int i = 0; i < bindings.length; i++) {
         TypeInfo[] adapters = Sys.getRegistry().getAdapters(null, toType);
         if (adapters.length > 0) {
            usableBindings.add(bindings[i]);
            usableFrom.add(null);
         }
      }

      return new AddConverter(
         this.editorPane,
         this.context,
         enabled && usableBindings.size() > 0,
         propName,
         usableBindings.toArray(new BBinding[0]),
         usableFrom.toArray(new BObject[0]),
         to
      );
   }

   @Override
   public void pxEvent(PxEvent event) {
      switch (event.getEventType()) {
         case 0:
            PxEditorEvent ee = (PxEditorEvent)event;
            switch (ee.getEventId()) {
               case 0:
                  this.setReadonly(this.editor.isReadonly());
                  return;
               case 2:
                  if (((String)ee.getEventValue()).equals("preserveIdentities")) {
                     this.refreshLabelText();
                  }

                  return;
               default:
                  return;
            }
         case 1:
            switch (((PxPropertyEvent)event).getEventId()) {
               case 1:
               case 2:
                  this.editWidgets(this.editorPane.getSelectedWidgets().getWidgets());
                  return;
               default:
                  return;
            }
         case 2:
            this.editWidgets(this.editorPane.getSelectedWidgets().getWidgets());
            break;
         case 3:
         case 4:
            boolean update = false;
            if (event instanceof PxComponentEvent) {
               switch (((PxComponentEvent)event).getEventId()) {
                  case 0:
                  case 1:
                     if (!isWebWidgetEvent(event)) {
                        this.editWidgets(this.editorPane.getSelectedWidgets().getWidgets());
                        update = true;
                     }
               }
            }

            if (!update) {
               switch (EventUtil.getEventType((PxComponentEvent)event)) {
                  case 1:
                  case 2:
                  case 3:
                  case 5:
                  case 6:
                  case 7:
                  case 8:
                  case 9:
                  case 10:
                  case 11:
                  case 12:
                     this.editWidgets(this.editorPane.getSelectedWidgets().getWidgets());
                     return;
                  case 4:
                     this.refreshLabelText();
               }
            }
            break;
         case 5:
         case 6:
            this.editWidgets(this.editorPane.getSelectedWidgets().getWidgets());
         case 7:
         default:
            break;
         case 8:
            switch (((PxLayerEvent)event).getEventId()) {
               case 1:
               case 2:
               case 3:
                  this.editWidgets(this.editorPane.getSelectedWidgets().getWidgets());
            }
      }
   }

   private static boolean isWebWidgetEvent(PxEvent event) {
      return !(event instanceof PxWidgetEvent) ? false : ((PxWidgetEvent)event).getWidget() instanceof BWebWidget;
   }

   private void refreshLabelText() {
      if (this.allWidgets != null && this.allWidgets.length == 1 && this.allWidgets[0] != null) {
         this.label.setText(this.widgetName(this.allWidgets[0]));
         this.label.relayout();
      }
   }

   public void editWidgets(BWidget[] w) {
      this.allWidgets = w;
      this.edit();
   }

   public void enableBinding(boolean flag) {
      if (!this.preventEdits()) {
         this.addBinding.setEnabled(flag);
      }
   }

   public void selectTextEditor() {
      if (this.textCE != null) {
         BPxCellTable table = (BPxCellTable)this.textCE.getParent();
         table.getCellController().cellSelected(this.textCE);
         this.textCE.requestTextFocus();
      }
   }

   private void edit() {
      this.unload();
      if (this.editorPane.getTool().isNormal()) {
         if (this.allWidgets != null) {
            if (this.allWidgets.length != 0) {
               if (this.allWidgets.length != 1 || this.allWidgets[0] != null) {
                  if (this.allWidgets.length != 1 || !(this.allWidgets[0] instanceof BNullWidget)) {
                     this.load();
                     this.relayout();
                  }
               }
            }
         }
      }
   }

   private String widgetName(BWidget w) {
      return this.editorPane.getOptions().getPreserveIdentities()
         ? (w.getParentWidget() == null ? Reflector.displayName(w, false) : BWidgetScheme.makeWidgetOrd(w).toString())
         : Reflector.displayName(w, false);
   }

   private BWidget baseClassInstance() {
      try {
         Class<?> cls = AbstractStubGen.getConcreteClass(this.baseClass);
         return (BWidget)cls.getDeclaredConstructor().newInstance();
      } catch (Exception var2) {
         throw new BajaRuntimeException(var2);
      }
   }

   private void unload() {
      this.textCE = null;
      this.contents.setCenter(new BScrollPane());
      this.enableBinding(false);
      String[] names = this.links.toArray(new String[0]);

      for (int i = 0; i < names.length; i++) {
         this.remove(names[i]);
      }

      this.links.clear();
      this.label.setText("");
      this.editorPane.getShell().showStatus("");
      this.relayout();
   }

   private void load() {
      BBinding[] bindings = new BBinding[0];
      if (this.allWidgets.length == 1) {
         this.widget = this.allWidgets[0];
         this.label.setText(this.widgetName(this.widget));
         this.editorPane.getShell().showStatus(Reflector.displayName(this.widget, true));
         this.enableBinding(true);
         bindings = (BBinding[])this.widget.getChildren(BBinding.class);
      } else {
         this.baseClass = ClassUtil.baseClass(ClassUtil.classes(this.allWidgets));
         String str = TextUtil.getClassName(this.baseClass).substring(1);
         str = this.editorPane.getLexicon().getText("cellsheet.multiple") + " " + str + "s";
         this.label.setText(str);
         this.editorPane.getShell().showStatus(str);
         this.widget = this.baseClassInstance();
      }

      this.contents.setCenter(new BScrollPane(this.alphabetical ? this.getAlphaScrollContents(bindings) : this.getCategoryScrollContents(bindings)));
   }

   private BWidget getAlphaScrollContents(BBinding[] bindings) {
      List<BPxCellTable> arrTables = new ArrayList<>();
      this.alphaTable = new BPxCellTable(this.editorPane, this);
      this.makeCellEditors(this.alphaTable, 0, this.properties(), bindings, true);
      BStackedPane multi = new BStackedPane();
      String name = this.widget.getType().getDisplayName(null);
      multi.add(
         null, this.expander(new BCellSheetExpandablePane(), name, new BBorderPane(new BLabel(name), 2.0, 0.0, 0.0, 0.0), this.alphaTable, arrTables.size())
      );
      arrTables.add(this.alphaTable);
      this.loadBindings(multi, arrTables, bindings);
      this.cellTables = arrTables.toArray(new BPxCellTable[0]);
      return multi;
   }

   private BWidget getCategoryScrollContents(BBinding[] bindings) {
      Map<Type, List<Property>> map = new TreeMap<>(new Comparator<Type>() {
         public int compare(Type o1, Type o2) {
            Class<?> c1 = o1.getTypeClass();
            Class<?> c2 = o2.getTypeClass();
            if (c1.equals(c2)) {
               return 0;
            } else {
               return c1.isAssignableFrom(c2) ? -1 : 1;
            }
         }
      });
      Property[] props = this.properties();

      for (int i = 0; i < props.length; i++) {
         Type type = props[i].getDeclaringType();
         List<Property> arr = map.get(type);
         if (arr == null) {
            arr = new ArrayList<>();
            map.put(type, arr);
         }

         arr.add(props[i]);
      }

      BStackedPane multi = new BStackedPane();
      List<BPxCellTable> arrTables = new ArrayList<>();
      int n = 0;

      for (Type type : map.keySet()) {
         List<Property> arr = map.get(type);
         Property[] p = arr.toArray(new Property[0]);
         BPxCellTable table = new BPxCellTable(this.editorPane, this);
         String name = type.getDisplayName(null);
         BCellSheetExpandablePane exp = this.expander(
            new BCellSheetExpandablePane(), name, new BBorderPane(new BLabel(name), 2.0, 0.0, 0.0, 0.0), table, arrTables.size()
         );
         multi.add(null, exp);
         arrTables.add(table);
         this.makeCellEditors(table, n++, p, bindings, type.equals(BWidget.TYPE));
      }

      this.loadBindings(multi, arrTables, bindings);
      this.cellTables = arrTables.toArray(new BPxCellTable[0]);
      return multi;
   }

   private void loadBindings(BStackedPane multi, List<BPxCellTable> arrTables, BBinding[] bindings) {
      if (bindings.length != 0) {
         multi.add(null, new BSeparator());

         for (int i = 0; i < bindings.length; i++) {
            BPxBindingTable bt = new BPxBindingTable(this.editorPane, this, this.widget, bindings[i]);
            BBindingExpandablePane exp = new BBindingExpandablePane(this.editorPane, this.context, this.widget, bindings[i]);
            if (this.preventEdits() || !this.context.allowBindingDelete()) {
               exp.setDeletable(false);
            }

            String name = bindings[i].getType().getDisplayName(null);
            multi.add(null, this.expander(exp, name, new BPxCellSheetBindingLabel(this.editorPane, this, bindings[i], name), bt, arrTables.size()));
            arrTables.add(bt);
         }
      }
   }

   private BCellSheetExpandablePane expander(BCellSheetExpandablePane exp, String summaryText, BWidget summary, BWidget expansion, int n) {
      Boolean b = this.expanderStates.get(summaryText);
      if (b == null) {
         b = Boolean.TRUE;
         this.expanderStates.put(summaryText, b);
      }

      exp.setSummary(summary);
      exp.setExpansion(expansion);
      exp.setExpanderHalign(BHalign.left);
      exp.setExpanded(b);
      exp.setSummaryBackground(Theme.widget().getControlBackground());
      String lnk = "expander" + n;
      this.links.add(lnk);
      this.linkTo(lnk, exp, BCellSheetExpandablePane.expanderEvent, expanderChanged);
      return exp;
   }

   private Property[] properties() {
      Property[] props = this.widget.getPropertiesArray();
      List<Property> arr = new ArrayList<>();

      for (int i = 0; i < props.length; i++) {
         Property prop = props[i];
         BObject val = this.widget.get(prop);
         if ((!prop.isDynamic() || WebProperty.isWebProperty(this.widget, prop) || this.isIncludeProperty(val))
            && !Flags.isHidden(this.widget, prop)
            && !(val instanceof BWidget)
            && (!(val instanceof BLayout) || this.context.allowLayoutEdit(this.allWidgets))) {
            arr.add(prop);
         }
      }

      return arr.toArray(new Property[0]);
   }

   private boolean isIncludeProperty(BObject val) {
      return this.widget instanceof BPxInclude && (val instanceof BSimple || val instanceof BStruct);
   }

   private void makeCellEditors(BPxCellTable table, int tableId, Property[] props, BBinding[] bindings, boolean includeLayers) {
      int arrayLen = props.length;
      if (includeLayers) {
         arrayLen++;
      }

      List<BWbCellEditor> arr = new ArrayList<>(arrayLen);

      for (int i = 0; i < props.length; i++) {
         arr.add(this.makeCellEditor(tableId, i, props[i], bindings));
      }

      if (includeLayers) {
         BPxLayerCE ce = new BPxLayerCE(this.editor.getPxLayers());
         arr.add(ce);
         if (!ce.isReadonly()) {
            ce.setReadonly(this.preventEdits());
         }

         this.linkCellEditor(tableId, props.length, ce);
         ce.loadValue(this.editorPane.getLayerManager().getCommonTag(this.allWidgets));
      }

      Collections.sort(arr, (o1, o2) -> {
         String s1 = o1.getPropertyName();
         String s2 = o2.getPropertyName();
         return s1.compareTo(s2);
      });
      BWbCellEditor[] ce = arr.toArray(new BWbCellEditor[0]);

      for (int i = 0; i < ce.length; i++) {
         ce[i].setRowIndex(i);
         table.addCellEditor(ce[i]);
         if (ce[i].getPropertyName().equals("text") && ce[i] instanceof BStringCE) {
            this.textCE = (BStringCE)ce[i];
         }
      }

      table.sizeColumnsToFit();
   }

   private BWbCellEditor makeCellEditor(int tableId, int row, Property prop, BBinding[] bindings) {
      BWbCellEditor ce = this.newCellEditor(prop, bindings);
      ce.setPropertyName(prop.getName());
      if (!ce.isReadonly()) {
         ce.setReadonly(this.preventEdits() || Flags.isReadonly(this.widget, prop) || this.anyBound(prop.getName()));
      }

      this.linkCellEditor(tableId, row, ce);
      return ce;
   }

   private void linkCellEditor(int tableId, int row, BWbCellEditor ce) {
      String lnk = "table" + tableId + "row" + row;
      this.links.add(lnk);
      this.linkTo(lnk, ce, BWbCellEditor.pluginModified, cellModified);
   }

   private boolean anyBound(String propName) {
      if (this.allWidgets.length == 1) {
         return false;
      } else {
         for (int i = 0; i < this.allWidgets.length; i++) {
            BBinding[] bindings = (BBinding[])this.allWidgets[i].getChildren(BBinding.class);
            if (Reflector.converter(propName, bindings) != null) {
               return true;
            }
         }

         return false;
      }
   }

   private BWbCellEditor newCellEditor(Property prop, BBinding[] bindings) {
      BConverter conv = Reflector.converter(prop.getName(), bindings);
      if (conv == null) {
         PxProperty[] groups = this.context.getPxPropertyComponents().getPxProperties(this.allWidgets, prop.getName());
         return (BWbCellEditor)(groups.length == 0 ? this.editor.getController().getCellEditor(this.widget, prop) : new BPxPropertyCE(groups));
      } else {
         BConverterCE ce = new BConverterCE(this.editorPane, this, conv);
         ce.loadValue(conv.newCopy(), null);
         return ce;
      }
   }

   public CellSheetContext getCellContext() {
      return this.context;
   }

   public BWidget getWidget() {
      return this.widget;
   }

   BWidget[] getAllWidgets() {
      return this.allWidgets;
   }

   BPxCellTable[] getCellTables() {
      return this.cellTables;
   }

   double getColumnWidth() {
      return this.columnWidth;
   }

   void setColumnWidth(double d) {
      this.columnWidth = d;
   }

   boolean canDrop() {
      return this.addBinding.isEnabled();
   }

   @Override
   public BImage getSideBarIcon() {
      return ICON;
   }

   @Override
   public String getSideBarDescription() {
      return DESC;
   }

   class Alphabetize extends ToggleCommand {
      Alphabetize() {
         super(BPxCellSheet.this, BPxEditorPane.lexicon(), "commands.alphabetize");
      }

      public CommandArtifact doInvoke() throws Exception {
         if (!this.isSelected()) {
            return null;
         } else {
            BPxCellSheet.this.alphabetical = true;
            if (BPxCellSheet.this.widget == null) {
               return null;
            } else {
               boolean flag = BPxCellSheet.this.addBinding.isEnabled();
               BPxCellSheet.this.edit();
               BPxCellSheet.this.enableBinding(flag);
               return null;
            }
         }
      }
   }

   class Categorize extends ToggleCommand {
      Categorize() {
         super(BPxCellSheet.this, BPxEditorPane.lexicon(), "commands.categorize");
      }

      public CommandArtifact doInvoke() throws Exception {
         if (!this.isSelected()) {
            return null;
         } else {
            BPxCellSheet.this.alphabetical = false;
            if (BPxCellSheet.this.widget == null) {
               return null;
            } else {
               boolean flag = BPxCellSheet.this.addBinding.isEnabled();
               BPxCellSheet.this.edit();
               BPxCellSheet.this.enableBinding(flag);
               return null;
            }
         }
      }
   }
}
