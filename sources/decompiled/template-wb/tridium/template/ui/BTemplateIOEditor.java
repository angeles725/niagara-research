package com.tridium.template.ui;

import com.tridium.sys.tag.ComponentTags;
import com.tridium.tagdictionary.BNiagaraTagDictionary;
import com.tridium.template.BConfigBinding;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.TemplateConst;
import com.tridium.template.ui.tag.TagSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Optional;
import javax.baja.agent.BPxView;
import javax.baja.control.BControlPoint;
import javax.baja.data.BIDataValue;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.SortUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.status.BStatusValue;
import javax.baja.sync.Transaction;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BLink;
import javax.baja.sys.BMarker;
import javax.baja.sys.BModule;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Tag;
import javax.baja.tag.Taggable;
import javax.baja.tag.Tags;
import javax.baja.tag.util.BasicEntity;
import javax.baja.tagdictionary.BTagDictionary;
import javax.baja.ui.BBorder;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BButtonStyle;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BOrientation;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.event.WidgetSubscriber;
import javax.baja.ui.list.BList;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.pane.BTreePane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableHeaderRenderer;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.table.TableController.ResizeColumnsCommand;
import javax.baja.ui.table.TableHeaderRenderer.Header;
import javax.baja.ui.tree.BTree;
import javax.baja.ui.tree.TreeController;
import javax.baja.ui.tree.TreeModel;
import javax.baja.ui.tree.TreeNode;
import javax.baja.ui.tree.TreeSelection;
import javax.baja.util.BCompositeAction;
import javax.baja.util.BCompositeTopic;
import javax.baja.util.BWsAnnotation;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BTemplateIOEditor extends BEdgePane implements TemplateConst {
   public static final Type TYPE = Sys.loadType(BTemplateIOEditor.class);
   private static Lexicon lex = Lexicon.make("template");
   private static BModule module = Sys.getModuleForClass(BTemplateIOEditor.class);
   private static BImage inIcon = BImage.make("module://icons/x16/arrowLeft.png");
   private static BImage outIcon = BImage.make("module://icons/x16/arrowRight.png");
   private static BImage filterIcon = BImage.make("module://icons/x16/filter.png");
   private static BBrush colorProperty = BColor.make(13816554).toBrush();
   private static BBrush colorAction = BColor.make(12122296).toBrush();
   private static BBrush colorTopic = BColor.make(14934976).toBrush();
   private static String TAG_FILTER_ALL = lex.getText("controlAppIOEditor.tagFilter.showAll");
   private static String TAG_FILTER_MARKERS = lex.getText("controlAppIOEditor.tagFilter.markersOnly");
   private static String TAG_FILTER_DICTIONARY = lex.getText("controlAppIOEditor.tagFilter.dictionaryOnly");
   private static Tag NIAGARA_BIND_HINTS_TAG = new Tag(BNiagaraTagDictionary.BIND_HINTS, BString.make(""));
   private static Tag NIAGARA_TARGET_SLOT_HINT_TAG = new Tag(BNiagaraTagDictionary.TARGET_SLOT_HINT, BString.make(""));
   private static Tag NIAGARA_TEMPLATE_INPUT = new Tag(BNiagaraTagDictionary.INPUT, BMarker.MARKER);
   private static Tag NIAGARA_TEMPLATE_OUTPUT = new Tag(BNiagaraTagDictionary.OUTPUT, BMarker.MARKER);
   private static Tag NIAGARA_USER_TIP = new Tag(BNiagaraTagDictionary.USER_TIP, BString.make(""));
   private static Tag NIAGARA_SLOT_PATH_SCOPE = new Tag(BTemplateManager.SLOT_PATH_SCOPE, BString.make(""));
   private static int IN_EXIST = 1;
   private static int OUT_EXIST = 2;
   private static int BOTH_EXIST = 3;
   private static int NONE_EXIST = 0;
   BTemplateView view;
   BSplitPane sp;
   private BComponent root;
   private BTree tree;
   private BTable table;
   private BTable sourceTagTable;
   private BTable ioTagTable;
   private ArrayList<BTemplateIOEditor.CompositeSlot> removed = new ArrayList<>();
   private BTemplateIOEditor.IoWidgetSubscriber tableSubscriber;
   private BListDropDown tagFilterSelect;
   private BListDropDown dictionarySelect;
   private BEdgePane sourceTagPane;
   private BEdgePane ioTagPane;
   private BGridPane addButtonPane;
   private BGridPane delButtonPane;
   private BTagDictionary dictionary;
   private BTextField tagEntry;
   private String tagFilter;
   private BTemplateIOEditor.Add add;
   private BTemplateIOEditor.Reverse reverse;
   private BTemplateIOEditor.Rename rename;
   private BTemplateIOEditor.Remove remove;
   private BTemplateIOEditor.MoveUp moveUp;
   private BTemplateIOEditor.MoveDown moveDown;
   private BTemplateIOEditor.AddTag addTag;
   private BTemplateIOEditor.DeleteTag deleteTag;
   private BTemplateIOEditor.DeleteAllTag deleteAllTag;
   private HashMap<String, Object> dictionaryHash = new HashMap<>();

   public Type getType() {
      return TYPE;
   }

   public BTemplateIOEditor() {
      throw new IllegalStateException();
   }

   public BTemplateIOEditor(BTemplateView view, BComponent root) {
      this.view = view;
      this.root = root;
      this.tree = new BTree(new BTemplateIOEditor.Model(root));
      this.tree.setMultipleSelection(true);
      this.tree.setController(new BTemplateIOEditor.Controller());
      this.tree.setSelection(new BTemplateIOEditor.Selection());
      this.tree.setExpanded(this.tree.getModel().getRoot(0), true);
      this.tree.getModel().updateTree();
      BTemplateIOEditor.CompositeModel model = new BTemplateIOEditor.CompositeModel();
      this.table = new BTable(model);
      this.table.setController(new BTemplateIOEditor.CompositeController());
      this.table.setSelection(new BTemplateIOEditor.CompositeSelection());
      this.table.setCellRenderer(new BTemplateIOEditor.CompositeRenderer());
      this.table.setHeaderRenderer(new BTemplateIOEditor.CompositeHeaderRenderer());
      this.tableSubscriber = new BTemplateIOEditor.IoWidgetSubscriber();
      this.tableSubscriber.subscribe(this.table);
      String rootPathPrefix = root.getSlotPath().toString() + '/';
      BLink[] links = root.getLinks();

      for (BLink link : links) {
         if (!link.getTargetSlot().isFrozen()) {
            BComponent source = null;

            try {
               source = (BComponent)link.getSourceOrd().resolve(root).get();
            } catch (Exception var19) {
               root.remove(link);
            }

            if (source != null && !source.getType().is(BTemplateConfig.TYPE)) {
               String linkSourcePath = source.getSlotPath().toString();
               if (linkSourcePath.startsWith(rootPathPrefix)) {
                  String targetSlotName = link.getTargetSlotName();
                  String sourceSlotName = link.getSourceSlotName();
                  source.lease();
                  Slot sourceSlot = source.getSlot(sourceSlotName);
                  boolean readonly = sourceSlot.isTopic() || Flags.isReadonly(source, sourceSlot);
                  if (root.getProperty(targetSlotName) == null) {
                     root.remove(link.getName());
                  } else {
                     Tags existingTags = link.tags();
                     if (existingTags.isEmpty()) {
                        Taggable taggable = new BComponent();
                        taggable.tags().set(NIAGARA_BIND_HINTS_TAG);
                        taggable.tags().set(NIAGARA_TARGET_SLOT_HINT_TAG);
                        taggable.tags().set(NIAGARA_TEMPLATE_OUTPUT);
                        taggable.tags().set(NIAGARA_USER_TIP);
                        taggable.tags().set(NIAGARA_SLOT_PATH_SCOPE);
                        existingTags = taggable.tags();
                        ComponentTags tags = new ComponentTags(source);
                        existingTags.merge(tags.getAll());
                     }

                     if (!existingTags.contains(NIAGARA_BIND_HINTS_TAG.getId())) {
                        existingTags.set(NIAGARA_BIND_HINTS_TAG);
                     }

                     if (!existingTags.contains(NIAGARA_TARGET_SLOT_HINT_TAG.getId())) {
                        existingTags.set(NIAGARA_TARGET_SLOT_HINT_TAG);
                     }

                     if (!existingTags.contains(NIAGARA_TEMPLATE_OUTPUT.getId())) {
                        existingTags.set(NIAGARA_TEMPLATE_OUTPUT);
                     }

                     if (!existingTags.contains(NIAGARA_USER_TIP.getId())) {
                        existingTags.set(NIAGARA_USER_TIP);
                     }

                     if (!existingTags.contains(NIAGARA_SLOT_PATH_SCOPE.getId())) {
                        existingTags.set(NIAGARA_SLOT_PATH_SCOPE);
                     }

                     Property targetProperty = root.getProperty(targetSlotName);
                     model.add(
                        1,
                        targetSlotName,
                        linkSourcePath,
                        sourceSlotName,
                        targetProperty.getType(),
                        readonly,
                        true,
                        root.getFlags(targetProperty),
                        existingTags
                     );
                  }
               }
            }
         }
      }

      Knob[] knobs = root.getKnobs();

      for (Knob knob : knobs) {
         if (!knob.getSourceSlot().isFrozen()) {
            BComponent target = (BComponent)knob.getTargetOrd().resolve(root).get();
            target.lease();
            String targetPath = target.getSlotPath().toString();
            if (targetPath.startsWith(rootPathPrefix)) {
               String sourceSlotName = knob.getSourceSlotName();
               String targetSlotName = knob.getTargetSlotName();
               if (root.getProperty(sourceSlotName) == null) {
                  target.remove(knob.getLink().getName());
               } else {
                  Slot targetSlot = target.getSlot(targetSlotName);
                  Tags linkTags = knob.getLink().tags();
                  if (linkTags.isEmpty()) {
                     Taggable taggable = new BComponent();
                     taggable.tags().set(NIAGARA_BIND_HINTS_TAG);
                     taggable.tags().set(NIAGARA_TARGET_SLOT_HINT_TAG);
                     taggable.tags().set(NIAGARA_TEMPLATE_INPUT);
                     taggable.tags().set(NIAGARA_USER_TIP);
                     taggable.tags().set(NIAGARA_SLOT_PATH_SCOPE);
                     linkTags = taggable.tags();
                     ComponentTags tags = new ComponentTags(target);
                     linkTags.merge(tags.getAll());
                  }

                  if (!linkTags.contains(NIAGARA_BIND_HINTS_TAG.getId())) {
                     linkTags.set(NIAGARA_BIND_HINTS_TAG);
                  }

                  if (!linkTags.contains(NIAGARA_TARGET_SLOT_HINT_TAG.getId())) {
                     linkTags.set(NIAGARA_TARGET_SLOT_HINT_TAG);
                  }

                  if (!linkTags.contains(NIAGARA_TEMPLATE_INPUT.getId())) {
                     linkTags.set(NIAGARA_TEMPLATE_INPUT);
                  }

                  if (!linkTags.contains(NIAGARA_USER_TIP.getId())) {
                     linkTags.set(NIAGARA_USER_TIP);
                  }

                  if (!linkTags.contains(NIAGARA_SLOT_PATH_SCOPE.getId())) {
                     linkTags.set(NIAGARA_SLOT_PATH_SCOPE);
                  }

                  Property sourceProperty = root.getProperty(sourceSlotName);
                  model.add(0, sourceSlotName, targetPath, targetSlotName, sourceProperty.getType(), false, true, target.getFlags(targetSlot), linkTags);
               }
            }
         }
      }

      Property[] props = root.getPropertiesArray();
      int offset = 0;

      for (Property prop : props) {
         for (int j = 0; j < model.getRowCount(); j++) {
            if (prop.getName().equals(model.get(j).name)) {
               model.move(j, offset++);
            }
         }
      }

      BSplitPane split = new BSplitPane();
      split.setDividerPosition(15.0);
      split.setWidget1(this.buildLeftPane());
      split.setWidget2(this.buildRightPane());
      this.setCenter(split);
      this.updateTagPanes();
   }

   private BWidget buildLeftPane() {
      this.add = new BTemplateIOEditor.Add(this);
      this.add.setEnabled(false);
      BButton a = new BButton(this.add);
      a.setButtonStyle(BButtonStyle.toolBar);
      BGridPane top = new BGridPane(1);
      top.setHalign(BHalign.left);
      top.add(null, a);
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(top, 5.0, 0.0, 5.0, 0.0));
      pane.setCenter(new BBorderPane(new BTreePane(this.tree), BBorder.none, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      return pane;
   }

   private BWidget buildRightPane() {
      this.reverse = new BTemplateIOEditor.Reverse(this);
      this.rename = new BTemplateIOEditor.Rename(this);
      this.remove = new BTemplateIOEditor.Remove(this);
      this.moveUp = new BTemplateIOEditor.MoveUp(this);
      this.moveDown = new BTemplateIOEditor.MoveDown(this);
      this.addTag = new BTemplateIOEditor.AddTag(this);
      this.deleteTag = new BTemplateIOEditor.DeleteTag(this);
      this.deleteAllTag = new BTemplateIOEditor.DeleteAllTag(this);
      this.reverse.setEnabled(false);
      this.rename.setEnabled(false);
      this.remove.setEnabled(false);
      this.moveUp.setEnabled(false);
      this.moveDown.setEnabled(false);
      this.addTag.setEnabled(false);
      this.deleteTag.setEnabled(false);
      this.deleteAllTag.setEnabled(false);
      BButton a = new BButton(this.reverse);
      BButton b = new BButton(this.rename);
      BButton c = new BButton(this.remove);
      BButton d = new BButton(this.moveUp);
      BButton e = new BButton(this.moveDown);
      a.setButtonStyle(BButtonStyle.toolBar);
      b.setButtonStyle(BButtonStyle.toolBar);
      c.setButtonStyle(BButtonStyle.toolBar);
      d.setButtonStyle(BButtonStyle.toolBar);
      e.setButtonStyle(BButtonStyle.toolBar);
      BButton addTagB = new BButton(this.addTag);
      BButton deleteTagB = new BButton(this.deleteTag);
      BButton deleteAllTagB = new BButton(this.deleteAllTag);
      addTagB.setButtonStyle(BButtonStyle.toolBar);
      deleteTagB.setButtonStyle(BButtonStyle.toolBar);
      deleteAllTagB.setButtonStyle(BButtonStyle.toolBar);
      BGridPane topLeft = new BGridPane(3);
      topLeft.add(null, a);
      topLeft.add(null, b);
      topLeft.add(null, c);
      BGridPane topRight = new BGridPane(2);
      topRight.add(null, d);
      topRight.add(null, e);
      BEdgePane top = new BEdgePane();
      top.setLeft(topLeft);
      top.setRight(topRight);
      this.addButtonPane = new BGridPane(2);
      this.addButtonPane.add(null, addTagB);
      this.delButtonPane = new BGridPane(2);
      this.delButtonPane.add(null, deleteTagB);
      this.delButtonPane.add(null, deleteAllTagB);
      this.sp = new BSplitPane();
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(top, 5.0, 0.0, 5.0, 0.0));
      pane.setCenter(new BBorderPane(this.table, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      this.sp.setWidget1(pane);
      this.sp.setOrientation(BOrientation.vertical);
      this.sp.setWidget2(this.buildTagPane());
      return this.sp;
   }

   public boolean hasValidHints() {
      BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)this.table.getModel();

      for (int i = 0; i < model.getRowCount(); i++) {
         BTemplateIOEditor.CompositeSlot slot = model.get(i);
         if (!slot.hasValidBindHints()) {
            return false;
         }
      }

      return true;
   }

   public void save() {
      this.root = this.createComposite();
   }

   private BWidget buildTagPane() {
      this.dictionarySelect = TagSupport.makeDictionarySelect(this.dictionaryHash);
      this.sourceTagTable = new BTable();
      this.sourceTagTable.setModel(new BTemplateIOEditor.TagTableModel());
      this.sourceTagTable.setSelection(new BTemplateIOEditor.TagTableSelection());
      this.sourceTagTable.setController(new BTemplateIOEditor.TagTableController());
      this.tagEntry = new BTextField("");
      this.ioTagTable = new BTable();
      this.ioTagTable.setModel(new BTemplateIOEditor.TagTableModel());
      this.ioTagTable.setSelection(new BTemplateIOEditor.TagTableSelection());
      this.ioTagTable.setController(new BTemplateIOEditor.TagTableController());
      this.tagFilterSelect = this.makeFilterSelect();
      this.tableSubscriber.subscribe(this.tagEntry);
      this.tableSubscriber.subscribe(this.dictionarySelect);
      this.tableSubscriber.subscribe(this.tagFilterSelect);
      this.initDictionary();
      BEdgePane tagPane = new BEdgePane();
      BSplitPane tagSplitPane = new BSplitPane();
      tagSplitPane.setOrientation(BOrientation.horizontal);
      tagPane.setTop(this.buildTagPaneHeader());
      tagPane.setCenter(tagSplitPane);
      this.sourceTagPane = this.buildTagSubPane(lex.getText("controlAppIOEditor.tags"));
      this.ioTagPane = this.buildTagSubPane(lex.getText("controlAppIOEditor.ioTags"));
      tagSplitPane.setWidget1(this.sourceTagPane);
      tagSplitPane.setWidget2(this.ioTagPane);
      new BGridPane(6);
      return tagPane;
   }

   private BBorderPane buildTagPaneHeader() {
      BBorderPane dictnPane = new BBorderPane(new BNullWidget(), BInsets.make(1.0, 0.0, 1.0, 1.0));
      BGridPane dictnSelPane = new BGridPane();
      dictnSelPane.setColumnCount(8);
      dictnSelPane.setHalign(BHalign.left);
      dictnSelPane.add(null, new BLabel(lex.getText("tag.dictionary.select")));
      dictnSelPane.add(null, this.dictionarySelect);
      dictnSelPane.add(null, new BLabel(filterIcon, ""));
      dictnSelPane.add(null, this.tagEntry);
      dictnSelPane.add(null, new BLabel(lex.getText("controlAppIOEditor.tagFilter.label")));
      dictnSelPane.add(null, this.tagFilterSelect);
      dictnPane.setContent(dictnSelPane);
      return dictnPane;
   }

   private BEdgePane buildTagSubPane(String title) {
      BEdgePane tagPane = new BEdgePane();
      BGridPane titlePane = new BGridPane(1);
      titlePane.setHalign(BHalign.left);
      titlePane.add(null, new BLabel(title));
      tagPane.setTop(titlePane);
      return tagPane;
   }

   private void updateTagPanes() {
      TagSupport.updateTagDictionaryTable(
         this.sourceTagTable, this.dictionary, this.tagFilterSelect.getSelectedItem().equals(TAG_FILTER_MARKERS), this.tagFilter
      );
      this.sourceTagPane.setCenter(this.sourceTagTable);
      this.sourceTagPane.setBottom(this.addButtonPane);
      this.updateButtons(this.sourceTagTable, true);
      ((BTemplateIOEditor.TagTableController)this.sourceTagTable.getController()).resizeColumns(this.sourceTagTable);
      this.updateIoTagPane(this.getIoTaggable(), this.ioTagPane, this.ioTagTable, this.delButtonPane);
   }

   private void updateIoTagPane(Taggable taggable, BEdgePane tagPane, BTable tagTable, BGridPane buttons) {
      this.updateTagPane(taggable, tagPane, tagTable, "", buttons);
      this.updateIoTags(taggable);
   }

   private void updateIoTags(Taggable taggable) {
      if (taggable != null) {
         BTemplateIOEditor.TagTableModel ttModel = (BTemplateIOEditor.TagTableModel)this.ioTagTable.getModel();
         int rows = this.ioTagTable.getModel().getRowCount();
         ArrayList<Tag> list = new ArrayList<>();

         for (int i = 0; i < rows; i++) {
            list.add(ttModel.get(i));
         }

         taggable.tags().merge(list);
      }
   }

   private void updateTagPane(Taggable taggable, BEdgePane tagPane, BTable tagTable, String tagFilter, BGridPane buttons) {
      BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)tagTable.getModel();
      if (taggable == null) {
         BLabel noIOSelected = new BLabel(lex.getText("controlAppIOEditor.noIOSelected"));
         BFont font = BFont.DEFAULT;
         noIOSelected.setFont(BFont.make(font, font.getSize() * 2.0));
         tagPane.setCenter(new BBorderPane(noIOSelected, 2.0, 2.0, 2.0, 2.0));
      } else {
         Iterator<Tag> it = null;
         Tags tags = taggable.tags();
         if (this.tagFilterSelect.getSelectedItem().equals(TAG_FILTER_MARKERS)) {
            it = tags.filter(t -> t.getValue() instanceof BMarker).iterator();
         } else if (this.tagFilterSelect.getSelectedItem().equals(TAG_FILTER_DICTIONARY)) {
            it = tags.filter(t -> !t.getId().getDictionary().equals("")).iterator();
         } else {
            it = tags.iterator();
         }

         model.removeAll();

         while (it.hasNext()) {
            Tag tag = it.next();
            if (tagFilter != null && tagFilter.length() != 0) {
               if (tag.getId().getName().equalsIgnoreCase(tagFilter)) {
                  model.add(tag);
                  break;
               }

               if (tag.getId().getName().toLowerCase().startsWith(tagFilter.toLowerCase())) {
                  model.add(tag);
               }
            } else {
               model.add(tag);
            }
         }

         tagPane.setCenter(tagTable);
      }

      if (buttons != null) {
         tagPane.setBottom(buttons);
      }

      this.updateButtons(tagTable, taggable != null);
      model.sortByColumn(0, true);
      tagTable.repaint();
   }

   private void updateButtons(BTable tagTable, boolean enable) {
      if (tagTable.equals(this.sourceTagTable)) {
         BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)this.sourceTagTable.getModel();
         int tagCount = this.sourceTagTable.getModel().getRowCount();
         if (tagCount == 1) {
            this.sourceTagTable.getSelection().select(0);
         }

         int[] rows = this.sourceTagTable.getSelection().getRows();
         boolean isIoSelected = this.table.getSelection().getRows().length > 0;
         this.addTag.setEnabled(enable && isIoSelected && (rows.length > 0 || tagCount == 1));
      } else if (tagTable.equals(this.ioTagTable)) {
         BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)this.ioTagTable.getModel();
         int[] rows = this.ioTagTable.getSelection().getRows();
         this.deleteTag.setEnabled(enable && rows.length > 0);
         this.deleteAllTag.setEnabled(enable && model.getRowCount() > 0);
      }
   }

   public BComponent createComposite() {
      Context tx = Transaction.start(this.root, null);
      BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)this.table.getModel();

      for (int i = 0; i < model.getRowCount(); i++) {
         BTemplateIOEditor.CompositeSlot slot = model.get(i);
         slot.name = SlotPath.escape(slot.name);
         if (slot.backup == null) {
            this.addCompositeSlot(tx, slot);
         } else {
            slot.backup.backup = slot.backup;
            this.removeCompositeSlot(tx, slot.backup);
            this.addCompositeSlot(tx, slot);
         }
      }

      for (int ix = 0; ix < this.removed.size(); ix++) {
         BTemplateIOEditor.CompositeSlot slot = this.removed.get(ix);
         this.removeCompositeSlot(tx, slot);
      }

      try {
         Transaction.end(this.root, tx);
      } catch (Exception var5) {
         BDialog.error(this.getShell(), "Error", "createTemplateConfig failed.", var5);
      }

      this.reorderCompositeSlots();
      return this.root;
   }

   private void addCompositeSlot(Context tx, BTemplateIOEditor.CompositeSlot slot) {
      BComponent child = (BComponent)BOrd.make(slot.ord).resolve(this.root).get();
      Slot childSlot = child.getSlot(slot.slot);
      slot.childFlags = child.getFlags(childSlot);
      BFacets facets = child.getSlotFacets(childSlot);
      if (facets == null) {
         facets = BFacets.NULL;
      }

      BValue value;
      if (!childSlot.isAction() && !childSlot.isTopic()) {
         value = (BValue)slot.type.getInstance();
         value = child.get(slot.slot).newCopy();
      } else {
         value = (BValue)(slot.dir == 0 ? new BCompositeAction() : new BCompositeTopic());
      }

      int flags = slot.dir == 1 ? 4097 : 4096;
      this.root.add(slot.name, value, flags, facets, tx);
      if (slot.dir == 1) {
         BLink link = new BLink(child.getHandleOrd(), slot.slot, slot.name, true);
         link.tags().merge(slot.taggable.tags().getAll());
         this.root.add(null, link, 4096, BFacets.NULL, tx);
      } else {
         BLink link = new BLink(this.root.getHandleOrd(), slot.name, slot.slot, true);
         link.tags().merge(slot.taggable.tags().getAll());
         child.add(null, link, 4096, BFacets.NULL, tx);
      }
   }

   private void renameCompositeSlot(Context tx, BTemplateIOEditor.CompositeSlot slot) {
      if (!slot.backup.name.equals(slot.name)) {
         BComponent c = this.root;
         Property prop = c.getProperty(slot.backup.name);
         BString newName = BString.make(slot.name);
         c.rename(prop, slot.name, tx);
         BLink[] links = c.getLinks(prop);

         for (int i = 0; i < links.length; i++) {
            BLink link = links[i];
            link.set(BLink.targetSlotName, newName, tx);
         }

         Knob[] knobs = c.getKnobs(prop);

         for (int i = 0; i < knobs.length; i++) {
            Knob knob = knobs[i];
            BComponent target = (BComponent)knob.getTargetOrd().get(c);
            target.lease();
            Property targetProp = target.getProperty(knob.getTargetSlotName());
            BLink[] targetLinks = target.getLinks(targetProp);

            for (int j = 0; j < targetLinks.length; j++) {
               BLink link = targetLinks[j];
               if (this.isMatch(knob, link)) {
                  link.set(BLink.sourceSlotName, newName, tx);
                  break;
               }
            }
         }
      }
   }

   private boolean isMatch(Knob knob, BLink link) {
      return link.getSourceOrd().equals(knob.getSourceOrd()) && link.getSourceSlotName().equals(knob.getSourceSlotName());
   }

   private void removeCompositeSlot(Context tx, BTemplateIOEditor.CompositeSlot slot) {
      if (slot.dir == 1) {
         BLink[] links = this.root.getLinks();

         for (int i = 0; i < links.length; i++) {
            if (links[i].getTargetSlotName().equals(slot.backup.name)) {
               this.root.remove(links[i].getPropertyInParent(), tx);
               this.root.remove(this.root.getProperty(slot.backup.name), tx);
               return;
            }
         }
      } else {
         BComponent child = (BComponent)BOrd.make(slot.ord).resolve(this.root).get();
         child.lease();
         BLink[] links = child.getLinks();

         for (int ix = 0; ix < links.length; ix++) {
            if (links[ix].getSourceSlotName().equals(slot.backup.name)) {
               child.remove(links[ix].getPropertyInParent(), tx);
               this.root.remove(this.root.getProperty(slot.backup.name), tx);
               return;
            }
         }
      }

      throw new RuntimeException("Could not find link for " + slot.backup.name);
   }

   public void reorderCompositeSlots() {
      this.root.lease();
      BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)this.table.getModel();
      ArrayList<String> list = new ArrayList<>();

      for (int i = 0; i < model.getRowCount(); i++) {
         list.add(model.get(i).name);
      }

      this.root.loadSlots();
      SlotCursor<Property> c = this.root.getProperties();

      while (c.next()) {
         Property p = c.property();
         if (!p.isFrozen() && !list.contains(p.getName())) {
            list.add(p.getName());
         }
      }

      Property[] newProps = new Property[list.size()];

      for (int i = 0; i < list.size(); i++) {
         newProps[i] = this.root.getProperty(list.get(i));
      }

      this.root.reorder(newProps);
   }

   private String getUniqueName(String name) {
      BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)this.table.getModel();
      this.root.loadSlots();
      Slot[] s = this.root.getSlotsArray();
      int count = 1;
      String tempName = name;

      for (int i = 0; i < s.length; i++) {
         if (s[i].getName().equals(tempName)) {
            tempName = name + count;
            count++;
            i = -1;
         } else {
            for (int j = 0; j < model.getRowCount(); j++) {
               BTemplateIOEditor.CompositeSlot cs = model.get(j);
               if (cs.name.equals(tempName)) {
                  tempName = name + count;
                  count++;
                  j = model.getRowCount();
                  i = -1;
               }
            }
         }
      }

      return tempName;
   }

   private Taggable getSourceTaggable() {
      try {
         BTemplateIOEditor.CompositeSlot slot = this.getIoCompositeSlot();
         return slot == null ? null : (Taggable)BOrd.make(slot.ord).resolve(this.root).get();
      } catch (Exception var2) {
         return null;
      }
   }

   private Tags getSourceTags() {
      return this.getSourceTaggable().tags();
   }

   private Taggable getIoTaggable() {
      BTemplateIOEditor.CompositeSlot slot = this.getIoCompositeSlot();
      return slot == null ? null : this.getIoCompositeSlot().taggable;
   }

   private BTemplateIOEditor.CompositeSlot getIoCompositeSlot() {
      BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)this.table.getModel();
      int[] rows = this.table.getSelection().getRows();
      return rows != null && rows.length != 0 ? model.get(rows[0]) : null;
   }

   private void initDictionary() {
      if (this.dictionarySelect.getSelectedIndex() >= 0 && this.dictionarySelect.getSelectedItem() instanceof String) {
         String dictn = (String)this.dictionarySelect.getSelectedItem();
         Object object = this.dictionaryHash.get(dictn);
         if (object instanceof BTagDictionary) {
            this.dictionary = (BTagDictionary)object;
         } else {
            this.dictionary = null;
         }
      } else {
         this.dictionary = null;
      }
   }

   private BListDropDown makeFilterSelect() {
      BListDropDown filterSel = new BListDropDown();
      BList list = filterSel.getList();
      list.addItem(TAG_FILTER_ALL);
      list.addItem(TAG_FILTER_MARKERS);
      list.addItem(TAG_FILTER_DICTIONARY);
      list.setSelectedItem(TAG_FILTER_ALL);
      return filterSel;
   }

   public class Add extends Command {
      public Add(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.add");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel();
         TreeNode[] nodes = BTemplateIOEditor.this.tree.getSelection().getNodes();
         ArrayList<Integer> newRows = new ArrayList<>();

         for (int i = 0; i < nodes.length; i++) {
            BTemplateIOEditor.Node n = (BTemplateIOEditor.Node)nodes[i];
            if (n.object instanceof BControlPoint) {
               BTemplateIOEditor.Node outNode = (BTemplateIOEditor.Node)n.getChild("out");
               if (outNode != null) {
                  n = outNode;
               }
            }

            if (!(n.object instanceof BComponent)) {
               BComponent parent = (BComponent)((BTemplateIOEditor.Node)n.getParent()).object;
               String name = n.name;
               if (parent.getType().is(BControlPoint.TYPE)) {
                  name = parent.getName() + '_' + name;
               }

               Slot slot = parent.getSlot(n.name);
               int existing = model.canAddToModel(parent, slot);
               if (existing != BTemplateIOEditor.BOTH_EXIST) {
                  boolean inExists = existing >= BTemplateIOEditor.IN_EXIST;
                  boolean outExists = existing >= BTemplateIOEditor.OUT_EXIST;
                  boolean readonly = slot.isTopic() || Flags.isReadonly(parent, slot);
                  if (!outExists || !readonly) {
                     int dir = readonly ? 1 : 0;
                     if (outExists) {
                        dir = 0;
                     } else if (inExists) {
                        dir = 1;
                     }

                     Type type;
                     if (slot.isTopic()) {
                        type = BCompositeTopic.TYPE;
                     } else if (slot.isAction()) {
                        type = dir == 0 ? BCompositeAction.TYPE : BCompositeTopic.TYPE;
                     } else {
                        type = n.object.getType();
                     }

                     Taggable taggable = new BComponent();
                     taggable.tags().set(BTemplateIOEditor.NIAGARA_BIND_HINTS_TAG);
                     taggable.tags().set(BTemplateIOEditor.NIAGARA_USER_TIP);
                     taggable.tags().set(BTemplateIOEditor.NIAGARA_SLOT_PATH_SCOPE);
                     if (dir == 0) {
                        taggable.tags().set(BTemplateIOEditor.NIAGARA_TEMPLATE_INPUT);
                        taggable.tags().set(BTemplateIOEditor.NIAGARA_TARGET_SLOT_HINT_TAG);
                     } else {
                        taggable.tags().set(BTemplateIOEditor.NIAGARA_TEMPLATE_OUTPUT);
                        taggable.tags().set(BTemplateIOEditor.NIAGARA_TARGET_SLOT_HINT_TAG);
                     }

                     Tags tags = taggable.tags();
                     if (parent instanceof BComponent) {
                        ComponentTags parentTags = new ComponentTags(parent);
                        tags.merge(parentTags.getAll());
                     }

                     String ord = parent.getSlotPath().toString();
                     model.add(dir, BTemplateIOEditor.this.getUniqueName(name), ord, n.name, type, readonly, false, 4096, tags);
                     newRows.add(model.getRowCount() - 1);
                  }
               }
            }
         }

         BTemplateIOEditor.this.table.relayout();
         if (newRows.size() > 0) {
            BTemplateIOEditor.this.table.requestFocus();
            BTemplateIOEditor.this.table.getSelection().deselectAll();

            for (int i = 0; i < newRows.size(); i++) {
               BTemplateIOEditor.this.table.getSelection().select(newRows.get(i));
            }
         }

         BTemplateIOEditor.this.view.templateModified();
         return null;
      }
   }

   public class AddFromDictionary extends Command {
      public AddFromDictionary(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.addFromDictionary");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)BTemplateIOEditor.this.ioTagTable.getModel();
         model.getSelection().selectAll();
         return BTemplateIOEditor.this.deleteTag.doInvoke();
      }
   }

   public class AddTag extends Command {
      public AddTag(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.addTag");
      }

      public CommandArtifact doInvoke() {
         return this.doInvoke(true);
      }

      public CommandArtifact doInvoke(boolean update) {
         BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)BTemplateIOEditor.this.sourceTagTable.getModel();
         int[] rows = model.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            Tag tag = model.get(rows[i]);
            ((BTemplateIOEditor.TagTableModel)BTemplateIOEditor.this.ioTagTable.getModel()).add(tag);
         }

         BTemplateIOEditor.this.ioTagTable.repaint();
         ((BTemplateIOEditor.TagTableController)BTemplateIOEditor.this.ioTagTable.getController()).resizeColumns(BTemplateIOEditor.this.ioTagTable);
         BTemplateIOEditor.this.updateButtons(BTemplateIOEditor.this.ioTagTable, true);
         BTemplateIOEditor.this.view.templateModified();
         if (update) {
            BTemplateIOEditor.this.updateIoTags(BTemplateIOEditor.this.getIoTaggable());
         }

         return null;
      }
   }

   class CompositeController extends TableController {
      public void keyPressed(BKeyEvent event) {
         super.keyPressed(event);
         if (event.getKeyCode() == 127 && BTemplateIOEditor.this.remove.isEnabled()) {
            BTemplateIOEditor.this.remove.invoke();
         }
      }

      protected void handleEnter(BKeyEvent event) {
         event.consume();
         if (BTemplateIOEditor.this.rename.isEnabled()) {
            BTemplateIOEditor.this.rename.invoke();
         }

         BTemplateIOEditor.this.table.repaint();
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         if (column == 0) {
            if (BTemplateIOEditor.this.reverse.isEnabled()) {
               BTemplateIOEditor.this.reverse.invoke();
            }
         } else if (BTemplateIOEditor.this.rename.isEnabled()) {
            BTemplateIOEditor.this.rename.invoke();
         }

         BTemplateIOEditor.this.table.repaint();
      }

      protected void checkSelection(BMouseEvent event, int row) {
         super.checkSelection(event, row);
         TableSelection cacSel = BTemplateIOEditor.this.table.getSelection();
         int selRow = cacSel.getRow();
         if (selRow >= 0) {
            BTemplateIOEditor.CompositeSlot slot = ((BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel()).get(cacSel.getRow());
            BComponent child = (BComponent)BOrd.make(slot.ord).resolve(BTemplateIOEditor.this.root).get();
            TreeModel model = BTemplateIOEditor.this.tree.getModel();
            SlotPath initialPath = child.getSlotPath();
            SlotPath rootPath = BTemplateIOEditor.this.root.getSlotPath();
            if (initialPath == null) {
               model.getRoot(0).setExpanded(true);
            } else {
               String[] names = initialPath.getNames();
               String[] rootNames = rootPath.getNames();
               BTemplateIOEditor.Node node = (BTemplateIOEditor.Node)model.getRoot(0);

               for (int i = rootNames.length; i < names.length; i++) {
                  BTemplateIOEditor.Node temp = (BTemplateIOEditor.Node)node.getChild(names[i]);
                  if (temp == null) {
                     break;
                  }

                  node = temp;
               }

               BTemplateIOEditor.Node temp = (BTemplateIOEditor.Node)node.getChild(slot.slot);
               if (temp != null) {
                  node = temp;
               }

               TreeNode[] path = node.getPathFromRoot();
               BTemplateIOEditor.this.tree.scrollPathToVisible(path);
               TreeNode n = path[path.length - 1];
               BTemplateIOEditor.this.tree.getSelection().deselectAll();
               BTemplateIOEditor.this.tree.getSelection().select(n);
               BTemplateIOEditor.this.tree.getController().setFocus(BTemplateIOEditor.this.tree.getSelection().getNode());
               n.setExpanded(true);
            }

            model.updateTree();
         }

         BTemplateIOEditor.this.updateTagPanes();
      }
   }

   class CompositeHeaderRenderer extends TableHeaderRenderer {
      public double getPreferredHeaderWidth(Header header) {
         double width = super.getPreferredHeaderWidth(header);
         return header.column == 1 && width < BTemplateConfigEditor.COL0_MIN_WIDTH ? BTemplateConfigEditor.COL0_MIN_WIDTH : width;
      }
   }

   public class CompositeModel extends TableModel {
      ArrayList<BTemplateIOEditor.CompositeSlot> kids = new ArrayList<>();

      public int getRowCount() {
         return this.kids.size();
      }

      public int getColumnCount() {
         return 4;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BTemplateIOEditor.lex.getText("controlAppConfigEditor.dir");
            case 1:
               return BTemplateIOEditor.lex.getText("controlAppConfigEditor.slot");
            case 2:
               return BTemplateIOEditor.lex.getText("controlAppConfigEditor.ord");
            default:
               return "";
         }
      }

      public int canAddToModel(BComponent parent, Slot slot) {
         int retValue = BTemplateIOEditor.NONE_EXIST;
         String ordStr = parent.getSlotPath().toString();
         boolean isReadOnly = (parent.getFlags(slot) & 1) != 0;
         if (isReadOnly) {
            retValue = BTemplateIOEditor.IN_EXIST;
         }

         for (int j = 0; j < this.getRowCount(); j++) {
            BTemplateIOEditor.CompositeSlot temp = this.get(j);
            if ((temp.ord + temp.slot).equals(ordStr + slot.getName())) {
               if (temp.dir == 1) {
                  retValue |= BTemplateIOEditor.OUT_EXIST;
               }

               if (temp.dir == 0) {
                  retValue |= BTemplateIOEditor.IN_EXIST;
               }
            }

            if (retValue == BTemplateIOEditor.BOTH_EXIST) {
               break;
            }
         }

         return retValue;
      }

      public Object getValueAt(int row, int col) {
         BTemplateIOEditor.CompositeSlot slot = this.kids.get(row);
         switch (col) {
            case 0:
               return slot.dir == 0 ? BTemplateIOEditor.lex.getText("controlAppConfigEditor.in") : BTemplateIOEditor.lex.getText("controlAppConfigEditor.out");
            case 1:
               return slot.name;
            case 2:
               String a = BTemplateIOEditor.this.root.getSlotPath().toString();
               String b = slot.ord + "/" + slot.slot;
               return b.substring(a.length());
            case 3:
               String bindHints = "";
               Optional<BIDataValue> optBindHints = slot.taggable.tags().get(BTemplateIOEditor.NIAGARA_BIND_HINTS_TAG.getId());
               if (optBindHints.isPresent()) {
                  bindHints = optBindHints.get().toString();
               }

               return TagSupport.isNeqlPredicateValid(bindHints, "BindHints");
            default:
               return "";
         }
      }

      public Object getSubject(int row) {
         return this.kids.get(row);
      }

      public BImage getRowIcon(int row) {
         BTemplateIOEditor.CompositeSlot slot = this.kids.get(row);
         return slot.dir == 0 ? BTemplateIOEditor.inIcon : BTemplateIOEditor.outIcon;
      }

      public void add(int dir, String name, String ord, String slotName, Type type, boolean readonly, boolean backup, int flags) {
         this.add(dir, name, ord, slotName, type, readonly, backup, flags, null);
      }

      public void add(int dir, String name, String ord, String slotName, Type type, boolean readonly, boolean backup, int flags, Tags tags) {
         BTemplateIOEditor.CompositeSlot slot = BTemplateIOEditor.this.new CompositeSlot();
         slot.name = SlotPath.unescape(name);
         slot.ord = ord;
         slot.slot = slotName;
         slot.dir = dir;
         slot.type = type;
         slot.readonly = readonly;
         slot.flags = flags;
         if (tags != null) {
            slot.taggable.tags().merge(tags.getAll());
         }

         if (backup) {
            slot.backup = BTemplateIOEditor.this.new CompositeSlot();
            slot.backup.name = name;
            slot.backup.ord = ord;
            slot.backup.slot = slotName;
            slot.backup.dir = dir;
            slot.backup.type = type;
            slot.backup.readonly = readonly;
            slot.backup.flags = flags;
            if (tags != null) {
               slot.backup.taggable.tags().merge(tags.getAll());
            }
         }

         this.kids.add(slot);
      }

      public void remove(int row) {
         this.kids.remove(row);
      }

      public BTemplateIOEditor.CompositeSlot get(int row) {
         return row > this.kids.size() ? null : this.kids.get(row);
      }

      public void move(int row, int newRow) {
         BTemplateIOEditor.CompositeSlot obj = this.kids.remove(row);
         this.kids.add(newRow, obj);
      }
   }

   class CompositeRenderer extends TableCellRenderer {
      public BBrush getForeground(Cell cell) {
         return cell.column == 3 ? BBrush.makeSolid(BColor.red) : super.getForeground(cell);
      }

      public BBrush getBackground(Cell cell) {
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)this.getTable().getModel();
         BTemplateIOEditor.CompositeSlot slot = model.get(cell.row);
         if (slot.type.is(BCompositeAction.TYPE)) {
            return BTemplateIOEditor.colorAction;
         } else {
            return slot.type.is(BCompositeTopic.TYPE) ? BTemplateIOEditor.colorTopic : BTemplateIOEditor.colorProperty;
         }
      }

      public BBrush getSelectionForeground(Cell cell) {
         return cell.column == 0 ? this.getForeground(cell) : super.getSelectionForeground(cell);
      }

      public BBrush getSelectionBackground(Cell cell) {
         return cell.column == 0 ? this.getBackground(cell) : super.getSelectionBackground(cell);
      }

      public double getPreferredCellWidth(Cell cell) {
         double width = super.getPreferredCellWidth(cell);
         return cell.column == 1 && width < BTemplateConfigEditor.COL0_MIN_WIDTH ? BTemplateConfigEditor.COL0_MIN_WIDTH : width;
      }
   }

   class CompositeSelection extends TableSelection {
      public void updateTable() {
         super.updateTable();
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)this.getTable().getModel();
         int[] rows = this.getRows();
         BTemplateIOEditor.this.rename.setEnabled(rows.length > 0);
         BTemplateIOEditor.this.remove.setEnabled(rows.length > 0);
         BTemplateIOEditor.this.moveUp.setEnabled(rows.length > 0);
         BTemplateIOEditor.this.moveDown.setEnabled(rows.length > 0);

         for (int i = 0; i < rows.length; i++) {
            BTemplateIOEditor.CompositeSlot slot = ((BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel()).get(rows[i]);
            if (slot.readonly) {
               BTemplateIOEditor.this.reverse.setEnabled(false);
               return;
            }

            for (int j = 0; j < model.getRowCount(); j++) {
               BTemplateIOEditor.CompositeSlot temp = model.get(j);
               if ((temp.ord + temp.slot).equals(slot.ord + slot.slot) && !temp.name.equals(slot.name)) {
                  BTemplateIOEditor.this.reverse.setEnabled(false);
                  return;
               }
            }
         }

         BTemplateIOEditor.this.reverse.setEnabled(rows.length > 0);
      }
   }

   class CompositeSlot {
      public String name;
      public String ord;
      public String slot;
      public Object handle;
      public Type type;
      public int dir;
      public boolean readonly = false;
      public Taggable taggable = new BasicEntity();
      public static final int IN = 0;
      public static final int OUT = 1;
      public BTemplateIOEditor.CompositeSlot backup = null;
      public int flags = 0;
      public int childFlags = 0;

      private boolean hasValidBindHints() {
         boolean validHints = false;
         Optional<BIDataValue> optBindHints = this.taggable.tags().get(BTemplateIOEditor.NIAGARA_BIND_HINTS_TAG.getId());
         if (optBindHints.isPresent()) {
            String predicateStatus = TagSupport.isNeqlPredicateValid(optBindHints.get().toString(), "BindHints");
            validHints = predicateStatus.equals("");
         }

         return validHints;
      }
   }

   class Controller extends TreeController {
      protected void doSelectAction(TreeNode target, double x, double y) {
         if (BTemplateIOEditor.this.add.isEnabled()) {
            BTemplateIOEditor.this.add.invoke();
         }
      }

      public void setFocus(TreeNode node) {
         super.setFocus(node);
         if (node != null) {
            BTemplateIOEditor.this.add.setEnabled(((BTemplateIOEditor.Node)node).canAdd());
         }
      }
   }

   public class DeleteAllTag extends Command {
      public DeleteAllTag(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.deleteAllTags");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)BTemplateIOEditor.this.ioTagTable.getModel();
         model.getSelection().selectAll();
         return BTemplateIOEditor.this.deleteTag.doInvoke();
      }
   }

   public class DeleteTag extends Command {
      public DeleteTag(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.deleteTag");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)BTemplateIOEditor.this.ioTagTable.getModel();
         int[] rows = model.getSelection().getRows();
         model.remove(rows);
         model.getSelection().deselectAll();
         BTemplateIOEditor.this.ioTagTable.repaint();
         BTemplateIOEditor.this.view.templateModified();
         return null;
      }
   }

   class IoWidgetSubscriber extends WidgetSubscriber {
      public void actionPerformed(BWidgetEvent e) {
         BWidget widget = e.getWidget();
         if (widget.equals(BTemplateIOEditor.this.tagEntry)) {
            TableSelection selection = BTemplateIOEditor.this.sourceTagTable.getSelection();
            BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)BTemplateIOEditor.this.sourceTagTable.getModel();
            if (model.getRowCount() == 1) {
               selection.selectAll();
               BTemplateIOEditor.this.addTag.doInvoke();
            }

            BTemplateIOEditor.this.tagFilter = "";
            BTemplateIOEditor.this.tagEntry.setText("");
            BTemplateIOEditor.this.updateTagPanes();
         }
      }

      public void keyTyped(BKeyEvent event) {
         if (event.getWidget().equals(BTemplateIOEditor.this.tagEntry)) {
            TableSelection selection = BTemplateIOEditor.this.sourceTagTable.getSelection();
            selection.deselectAll();
            BTemplateIOEditor.this.tagFilter = BTemplateIOEditor.this.tagEntry.getText();
            if (BTemplateIOEditor.this.tagFilter.endsWith(":")) {
               String dctnFilter = BTemplateIOEditor.this.tagFilter.substring(0, BTemplateIOEditor.this.tagFilter.length() - 1);
               BList list = BTemplateIOEditor.this.dictionarySelect.getList();

               for (int i = 0; i < list.getItemCount(); i++) {
                  if (list.getItem(i) instanceof TypeInfo && ((TypeInfo)list.getItem(i)).getTypeName().toLowerCase().startsWith(dctnFilter.toLowerCase())) {
                     list.setSelectedIndex(i);
                     BTemplateIOEditor.this.initDictionary();
                     break;
                  }
               }

               BTemplateIOEditor.this.tagFilter = "";
               BTemplateIOEditor.this.tagEntry.setText("");
            }

            BTemplateIOEditor.this.updateTagPanes();
         }
      }

      public void modified(BWidgetEvent e) {
         BWidget widget = e.getWidget();
         if (widget.equals(BTemplateIOEditor.this.table) || widget.equals(BTemplateIOEditor.this.tagFilterSelect)) {
            BTemplateIOEditor.this.updateTagPanes();
         } else if (widget.equals(BTemplateIOEditor.this.dictionarySelect)) {
            BTemplateIOEditor.this.initDictionary();
            BTemplateIOEditor.this.updateTagPanes();
         }
      }
   }

   class Model extends TreeModel {
      BTemplateIOEditor.Node root;

      public Model(BComponent rootNode) {
         this.root = BTemplateIOEditor.this.new Node(this, rootNode);
      }

      public int getRootCount() {
         return 1;
      }

      public TreeNode getRoot(int index) {
         return this.root;
      }
   }

   public class MoveDown extends Command {
      public MoveDown(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.moveDown");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel();
         int[] rows = model.getSelection().getRows();
         if (rows.length != 0 && rows[rows.length - 1] != model.getRowCount() - 1) {
            for (int i = rows.length - 1; i >= 0; i--) {
               model.getSelection().deselect(rows[i]);
               model.move(rows[i], rows[i] + 1);
               model.getSelection().select(rows[i] + 1);
            }

            BTemplateIOEditor.this.table.relayout();
            BTemplateIOEditor.this.view.templateModified();
            return null;
         } else {
            return null;
         }
      }
   }

   public class MoveUp extends Command {
      public MoveUp(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.moveUp");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel();
         int[] rows = model.getSelection().getRows();
         if (rows.length != 0 && rows[0] != 0) {
            for (int i = 0; i < rows.length; i++) {
               model.getSelection().deselect(rows[i]);
               model.move(rows[i], rows[i] - 1);
               model.getSelection().select(rows[i] - 1);
            }

            BTemplateIOEditor.this.table.relayout();
            BTemplateIOEditor.this.view.templateModified();
            return null;
         } else {
            return null;
         }
      }
   }

   class Node extends TreeNode {
      BComplex object;
      String name;
      BTemplateIOEditor.Node[] kids;

      public Node(TreeModel model, BComplex object) {
         super(model);
         this.name = object.getName();
         this.object = object;
      }

      public Node(TreeNode parent, BComplex object) {
         super(parent);
         this.name = object.getName();
         this.object = object;
      }

      public String getText() {
         return this.object.getDisplayName(null);
      }

      public BImage getIcon() {
         BImage icon = BImage.make(this.object.getIcon());
         return this.canAdd() ? icon : icon.getDisabledImage();
      }

      public boolean hasChildren() {
         return this.kids == null ? true : this.kids.length > 0;
      }

      public int getChildCount() {
         if (this.kids == null) {
            this.getChild(-1);
         }

         return this.kids.length;
      }

      public TreeNode getChild(int index) {
         if (this.kids == null) {
            this.load();
         }

         return index == -1 ? null : this.kids[index];
      }

      public TreeNode getChild(String name) {
         if (this.kids == null) {
            this.load();
         }

         for (int i = 0; i < this.kids.length; i++) {
            if (this.kids[i].name.equals(name)) {
               return this.kids[i];
            }
         }

         return null;
      }

      private void load() {
         this.load(false);
      }

      private void load(boolean clear) {
         if (clear) {
            this.kids = null;
         }

         if (this.kids == null) {
            this.loadRefChildren(this.object);
         }

         ArrayList<BTemplateIOEditor.Node> list = new ArrayList<>();

         for (BTemplateIOEditor.Node kid : this.kids) {
            if (kid.getChildCount() > 0) {
               list.add(kid);
            }
         }

         this.kids = list.toArray(new BTemplateIOEditor.Node[0]);
      }

      boolean loadRefChildren(BComplex parent) {
         ArrayList<BTemplateIOEditor.Node> al = new ArrayList<>();
         boolean descendantComp = false;
         boolean isSubtemplate = false;
         if (parent instanceof BComponent && parent.getParent().getParent() != null) {
            isSubtemplate = parent.asComponent().tags().get(TemplateConst.TEMPLATE_ROOT_TAG_ID).isPresent();
         }

         SlotCursor<Property> c = parent.getProperties();

         while (c.next()) {
            Slot slot = c.slot();
            if (this.accept(parent, slot, isSubtemplate)) {
               BComplex v = parent.get(slot.asProperty()).asComplex();
               BTemplateIOEditor.Node rf = BTemplateIOEditor.this.new Node(this, v);
               if (v instanceof BStatusValue) {
                  al.add(rf);
               }

               if (v instanceof BComplex) {
                  boolean isComponent = v instanceof BComponent;
                  boolean containsComp = rf.loadRefChildren(v);
                  if (isComponent || containsComp) {
                     al.add(rf);
                     descendantComp = true;
                  }
               }
            }
         }

         this.kids = al.toArray(new BTemplateIOEditor.Node[0]);
         return descendantComp;
      }

      boolean canAdd() {
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel();
         if (this.getParent() == null) {
            return false;
         } else {
            BComponent parent = (BComponent)((BTemplateIOEditor.Node)this.getParent()).object;
            Slot slot = parent.getSlot(this.name);
            BValue childValue = parent.get(slot.asProperty());
            if (childValue instanceof BControlPoint) {
               parent = childValue.asComponent();
               slot = parent.getSlot("out");
            } else if (childValue instanceof BComponent) {
               return false;
            }

            int existing = model.canAddToModel(parent, slot);
            return existing != BTemplateIOEditor.BOTH_EXIST;
         }
      }

      boolean accept(BComplex parent, Slot slot, boolean isTemplateRoot) {
         if (slot != null && parent != null && slot.isProperty() && !Flags.isHidden(parent, slot)) {
            Type slotType = parent.asComplex().get(slot.asProperty()).getType();
            if (isTemplateRoot) {
               int flags = parent.getFlags(slot);
               return (flags & 4096) != 0;
            } else {
               return !slotType.is(BTemplateConfig.TYPE)
                     && !slotType.is(BWsAnnotation.TYPE)
                     && !slotType.is(BPxView.TYPE)
                     && !slotType.is(BMarker.TYPE)
                     && !slotType.is(BRelation.TYPE)
                     && !slotType.is(BConfigBinding.TYPE)
                  ? slotType.is(BComplex.TYPE)
                  : false;
            }
         } else {
            return false;
         }
      }
   }

   public class Remove extends Command {
      public Remove(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.remove");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel();
         int[] rows = model.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            BTemplateIOEditor.CompositeSlot slot = model.get(rows[i] - i);
            if (slot.backup != null) {
               BTemplateIOEditor.this.removed.add(slot);
            }

            model.remove(rows[i] - i);
         }

         BTemplateIOEditor.this.table.getSelection().deselectAll();
         BTemplateIOEditor.this.table.relayout();
         BTemplateIOEditor.Model treeModel = (BTemplateIOEditor.Model)BTemplateIOEditor.this.tree.getModel();
         ((BTemplateIOEditor.Node)treeModel.getRoot(0)).load(true);
         treeModel.updateTree();
         BTemplateIOEditor.this.view.templateModified();
         return null;
      }
   }

   public class Rename extends Command {
      public Rename(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.rename");
      }

      public CommandArtifact doInvoke() {
         int[] rows = BTemplateIOEditor.this.table.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            BTemplateIOEditor.CompositeSlot slot = ((BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel()).get(rows[i]);
            BTextField field = new BTextField(slot.name, 25);
            BGridPane grid = new BGridPane(1);
            grid.add(null, field);
            BBorderPane pane = new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0);
            if (1 != BDialog.open(this.getOwner(), BTemplateIOEditor.lex.getText("controlAppConfigEditor.rename.label"), pane, 3)) {
               break;
            }

            if (!slot.name.equals(field.getText())) {
               BTemplateIOEditor.this.root.loadSlots();
               Slot[] s = BTemplateIOEditor.this.root.getSlotsArray();

               for (int j = 0; j < s.length; j++) {
                  if (s[j].getName().equals(field.getText())) {
                     BDialog.error(
                        this.getOwner(),
                        BTemplateIOEditor.lex.getText("controlAppConfigEditor.rename.label"),
                        BTemplateIOEditor.lex.getText("controlAppConfigEditor.rename.exists")
                     );
                     return null;
                  }
               }

               for (int jx = 0; jx < BTemplateIOEditor.this.table.getModel().getRowCount(); jx++) {
                  BTemplateIOEditor.CompositeSlot cs = ((BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel()).get(jx);
                  if (cs.name.equals(field.getText())) {
                     BDialog.error(
                        this.getOwner(),
                        BTemplateIOEditor.lex.getText("controlAppConfigEditor.rename.label"),
                        BTemplateIOEditor.lex.getText("controlAppConfigEditor.rename.exists")
                     );
                     return null;
                  }
               }

               slot.name = field.getText();
               BTemplateIOEditor.this.table.repaint();
            }
         }

         BTemplateIOEditor.this.view.templateModified();
         return null;
      }
   }

   public class Reverse extends Command {
      public Reverse(BWidget owner) {
         super(owner, BTemplateIOEditor.module, "controlAppConfigEditor.reverse");
      }

      public CommandArtifact doInvoke() {
         BTemplateIOEditor.CompositeModel model = (BTemplateIOEditor.CompositeModel)BTemplateIOEditor.this.table.getModel();
         int[] rows = BTemplateIOEditor.this.table.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            BTemplateIOEditor.CompositeSlot slot = model.get(rows[i]);
            if (!slot.readonly) {
               slot.dir = slot.dir == 0 ? 1 : 0;
               if (slot.type.is(BCompositeAction.TYPE)) {
                  slot.type = BCompositeTopic.TYPE;
               } else if (!slot.readonly && slot.type.is(BCompositeTopic.TYPE)) {
                  slot.type = BCompositeAction.TYPE;
               }
            }
         }

         BTemplateIOEditor.this.table.repaint();
         BTemplateIOEditor.this.view.templateModified();
         return null;
      }
   }

   class Selection extends TreeSelection {
      public void updateTree() {
         super.updateTree();
         TreeNode[] nodes = this.getNodes();
         boolean b = false;
         if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
               BObject obj = ((BTemplateIOEditor.Node)nodes[i]).object;
               b = obj instanceof BControlPoint || !(obj instanceof BComponent);
            }
         }

         BTemplateIOEditor.this.add.setEnabled(b);
      }
   }

   class TagTableController extends TableController {
      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         if (event.getWidget().equals(BTemplateIOEditor.this.sourceTagTable)) {
            BTemplateIOEditor.this.addTag.doInvoke();
            BTemplateIOEditor.this.ioTagTable.repaint();
         } else if (event.getWidget().equals(BTemplateIOEditor.this.ioTagTable) && column == 3) {
            BTemplateIOEditor.TagTableModel tagModel = (BTemplateIOEditor.TagTableModel)BTemplateIOEditor.this.ioTagTable.getModel();
            Tag tag = tagModel.get(row);
            if (tag.getValue() instanceof BMarker) {
               return;
            }

            BWbFieldEditor editor = BWbFieldEditor.makeFor(tag.getValue().asObject());
            editor.loadValue(tag.getValue().asObject());
            BGridPane grid = new BGridPane(1);
            grid.add(null, new BLabel(tag.getId().toString()));
            grid.add(null, editor);
            BBorderPane pane = new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0);
            if (1 != BDialog.open(BTemplateIOEditor.this.view, BTemplateIOEditor.lex.getText("controlAppConfigEditor.setValue.label"), pane, 3)) {
               return;
            }

            BValue newValue = null;

            try {
               newValue = editor.saveValue().asValue();
            } catch (Exception var12) {
               var12.printStackTrace();
            }

            if (newValue == null) {
               return;
            }

            if (tag.getValue().equals(newValue)) {
               return;
            }

            Tag updateTag = new Tag(tag.getId(), (BIDataValue)newValue);
            tagModel.updateRow(row, updateTag);
            Taggable ioTaggable = BTemplateIOEditor.this.getIoTaggable();
            ioTaggable.tags().set(updateTag);
            BTemplateIOEditor.this.table.relayout();
            BTemplateIOEditor.this.view.templateModified();
         }
      }

      public void resizeColumns(BTable table) {
         new ResizeColumnsCommand(this, table).doInvoke();
      }
   }

   public class TagTableModel extends TableModel {
      Array<Tag> tags = new Array(Tag.class);

      public int getRowCount() {
         return this.tags.size();
      }

      public int getColumnCount() {
         return 4;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BTemplateIOEditor.lex.getText("controlAppIOEditor.namespace");
            case 1:
               return BTemplateIOEditor.lex.getText("controlAppIOEditor.tagname");
            case 2:
               return BTemplateIOEditor.lex.getText("controlAppIOEditor.type");
            case 3:
               return BTemplateIOEditor.lex.getText("controlAppIOEditor.value");
            default:
               return "";
         }
      }

      public boolean isColumnSortable(int col) {
         return col <= 2;
      }

      public synchronized void sortByColumn(int col, boolean ascending) {
         Object[] keys = this.getColumnValues(col);
         Tag[] current = (Tag[])this.tags.trim();
         Tag[] temp = new Tag[current.length];
         System.arraycopy(current, 0, temp, 0, keys.length);
         SortUtil.sort(keys, temp, ascending);
         this.tags.clear();
         this.tags.addAll(temp);
      }

      public Object getValueAt(int row, int col) {
         Tag tag = (Tag)this.tags.get(row);
         switch (col) {
            case 0:
               return tag.getId().getDictionary();
            case 1:
               return tag.getId().getName();
            case 2:
               return tag.getValue().getType().getTypeName();
            case 3:
               return tag.getValue().toString();
            default:
               return "";
         }
      }

      public Object getSubject(int row) {
         return this.tags.get(row);
      }

      public void add(Tag tag) {
         if (!this.tags.contains(tag)) {
            this.tags.add(tag);
         }
      }

      public void removeAll() {
         this.tags = new Array(Tag.class);
      }

      public void remove(Tag tag) {
         if (this.tags.contains(tag)) {
            this.tags.remove(this.tags.indexOf(tag));
         }
      }

      public void remove(int[] rows) {
         Tag[] remTags = new Tag[rows.length];

         for (int i = 0; i < rows.length; i++) {
            remTags[i] = this.get(rows[i]);
         }

         for (int i = 0; i < remTags.length; i++) {
            this.remove(remTags[i]);
            BTemplateIOEditor.this.getIoTaggable().tags().remove(remTags[i]);
         }
      }

      public void remove(int row) {
         this.tags.remove(row);
      }

      public Tag get(int row) {
         return row > this.tags.size() ? null : (Tag)this.tags.get(row);
      }

      public void updateRow(int row, Tag newTag) {
         this.tags.set(row, newTag);
      }

      public int getTagIndex(String startsWith) {
         ListIterator<Tag> listIterator = this.tags.list().listIterator();

         while (listIterator.hasNext()) {
            int index = listIterator.nextIndex();
            Tag tag = listIterator.next();
            String thisName = tag.getId().getName();
            if (tag.getId().getName().startsWith(startsWith)) {
               return index;
            }
         }

         return -1;
      }
   }

   class TagTableSelection extends TableSelection {
      public void updateTable() {
         super.updateTable();
         BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)this.getTable().getModel();
         int[] rows = this.getRows();
         if (this.getTable().equals(BTemplateIOEditor.this.sourceTagTable)) {
            BTemplateIOEditor.this.addTag.setEnabled(rows.length > 0);
         } else if (this.getTable().equals(BTemplateIOEditor.this.ioTagTable)) {
            BTemplateIOEditor.this.deleteTag.setEnabled(rows.length > 0);
            BTemplateIOEditor.this.deleteAllTag.setEnabled(model.getRowCount() > 0);
         }
      }
   }
}
