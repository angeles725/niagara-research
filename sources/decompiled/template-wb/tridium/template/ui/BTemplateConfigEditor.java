package com.tridium.template.ui;

import com.tridium.platform.BIPlatformConnection;
import com.tridium.template.BConfigBinding;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.application.ApplicationTemplateUtil;
import com.tridium.template.application.NameTree;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.workbench.ord.RefFilter;
import com.tridium.workbench.ord.RefNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.baja.agent.BPxView;
import javax.baja.control.BControlPoint;
import javax.baja.driver.BDevice;
import javax.baja.driver.BDeviceFolder;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BClientCredentials;
import javax.baja.security.BPassword;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.sync.Transaction;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BMarker;
import javax.baja.sys.BModule;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BButton;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BButtonStyle;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BExpandablePane;
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
import javax.baja.ui.table.TableHeaderRenderer.Header;
import javax.baja.ui.tree.BTree;
import javax.baja.ui.tree.TreeController;
import javax.baja.ui.tree.TreeModel;
import javax.baja.ui.tree.TreeNode;
import javax.baja.util.BWsAnnotation;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BTemplateConfigEditor extends BEdgePane {
   public static final Type TYPE = Sys.loadType(BTemplateConfigEditor.class);
   public static final RefFilter configProps = new RefFilter() {
      public boolean accept(BObject parent, Slot slot) {
         if (slot != null && parent != null && slot.isProperty() && parent instanceof BComponent) {
            BValue childValue = parent.asComponent().get(slot.asProperty());
            if (!(childValue instanceof BUsernameAndPassword) || !(parent instanceof BClientCredentials) && !(parent instanceof BIPlatformConnection)) {
               Type slotType = childValue.getType();
               if (slotType.is(BTemplateConfig.TYPE)
                  || slotType.is(BWsAnnotation.TYPE)
                  || slotType.is(BPxView.TYPE)
                  || slotType.is(BMarker.TYPE)
                  || slotType.is(BRelation.TYPE)
                  || slotType.is(BConfigBinding.TYPE)
                  || slotType.is(BPassword.TYPE)) {
                  return false;
               } else if (childValue instanceof BComponent && childValue.asComponent().getSlotCount() == 0) {
                  return false;
               } else {
                  int slotFlags = parent.asComplex().getFlags(slot);
                  return (slotFlags & 36871) != 0 ? false : !slotType.is(BStruct.TYPE) || !TmplUtil.hasPasswordProp(childValue.asStruct());
               }
            } else {
               return true;
            }
         } else {
            return false;
         }
      }
   };
   public static double COL0_MIN_WIDTH = 175.0;
   private static final int COL_NAME = 0;
   private static final int COL_ORD = 1;
   private static final int COL_VALUE = 2;
   private static final int COL_USERTIP = 3;
   private static final int COL_COUNT = 4;
   private static final String USE_NEQL = "useNeql";
   private static final Lexicon lex = Lexicon.make("template");
   private static final Lexicon uiLex = Lexicon.make("bajaui");
   private static final BModule module = Sys.getModuleForClass(BTemplateConfigEditor.class);
   private static final BImage inIcon = BImage.make("module://icons/x16/arrowLeft.png");
   private static final BImage outIcon = BImage.make("module://icons/x16/arrowRight.png");
   private static final BBrush colorProperty = BColor.make(13816554).toBrush();
   private static final BBrush colorAction = BColor.make(12122296).toBrush();
   private static final BBrush colorTopic = BColor.make(14934976).toBrush();
   private static final BIcon ADD_BADGE_ICON = BIcon.std("badges/add.png");
   private static final BIcon REMOVE_BADGE_ICON = BIcon.std("badges/remove.png");
   private static final BIcon OPTIONAL_BADGE_ICON = BIcon.std("badges/ellipsis.png");
   private BTemplateConfig templateConfig;
   private final BComponent templateRoot;
   private final BTree tree;
   private final BTemplateConfigEditor.BBindingTable table;
   private final ArrayList<BTemplateConfigEditor.BindingSlot> removed = new ArrayList<>();
   private final BWidget owner;
   BTemplateView view;
   private BTemplateConfigEditor.Add add;
   private BTemplateConfigEditor.SetValue setValue;
   private BTemplateConfigEditor.SetUserTip setUserTip;
   private BTemplateConfigEditor.Rename rename;
   private BTemplateConfigEditor.Remove remove;
   private BTemplateConfigEditor.MoveUp moveUp;
   private BTemplateConfigEditor.MoveDown moveDown;

   public Type getType() {
      return TYPE;
   }

   public BTemplateConfigEditor() {
      throw new IllegalStateException();
   }

   public BTemplateConfigEditor(BTemplateView view, BComponent templateRoot, BTemplateConfig templateConfig) {
      this.owner = view.getShell();
      this.view = view;
      this.templateConfig = templateConfig;
      this.templateRoot = templateRoot;
      this.tree = new BTree(new BTemplateConfigEditor.Model(this.templateRoot, view.isApplicationTemplate(), view.getManifest()));
      this.tree.setMultipleSelection(true);
      this.tree.setController(new BTemplateConfigEditor.Controller());
      this.tree.setExpanded(this.tree.getModel().getRoot(0), true);
      this.tree.getModel().updateTree();
      BTemplateConfigEditor.BindingModel model = new BTemplateConfigEditor.BindingModel();
      this.table = new BTemplateConfigEditor.BBindingTable(model);
      this.table.setController(new BTemplateConfigEditor.BindingController());
      this.table.setSelection(new BTemplateConfigEditor.BindingSelection());
      this.table.setCellRenderer(new BTemplateConfigEditor.BindingRenderer());
      this.table.setHeaderRenderer(new BTemplateConfigEditor.BindingHeaderRenderer());
      Knob[] knobs = templateConfig.getKnobs();
      boolean modified = knobs.length > 0;
      if (modified) {
         BDialog.info(this.owner, "Converting Template Configuration property Composite Links to BConfigBindings.  Must click Save to complete.");
         view.setInitModified(modified);
      }

      templateConfig.convertToBindings();
      BConfigBinding[] bindings = templateConfig.getConfigBindings();

      for (BConfigBinding binding : bindings) {
         String source = binding.getSourceSlot();
         Property sourceProperty = templateConfig.getProperty(source);
         BComponent targetComponent = null;

         try {
            targetComponent = (BComponent)binding.getTargetOrd().resolve(templateConfig).get();
         } catch (Exception var19) {
            templateConfig.remove(binding);
            if (sourceProperty != null) {
               templateConfig.remove(sourceProperty);
            }
         }

         if (targetComponent != null) {
            String targetSlotPath = targetComponent.getSlotPath().toString();
            String rootSlotPath = this.templateRoot.getSlotPath().toString();
            if (targetSlotPath.startsWith(rootSlotPath)) {
               String target = binding.getTargetSlot();
               String userTip = binding.getUserTip();
               if (sourceProperty != null) {
                  model.add(
                     0,
                     source,
                     targetSlotPath,
                     targetComponent,
                     target,
                     templateConfig.get(sourceProperty),
                     templateConfig.getSlotFacets(sourceProperty),
                     sourceProperty.getType(),
                     false,
                     true,
                     templateConfig.getFlags(sourceProperty),
                     userTip
                  );
               }
            }
         }
      }

      Property[] props = templateConfig.getPropertiesArray();
      int offset = 0;

      for (Property prop : props) {
         for (int j = 0; j < model.getRowCount(); j++) {
            if (prop.getName().equals(model.get(j).name)) {
               model.move(j, offset++);
            }
         }
      }

      BSplitPane split = new BSplitPane();
      split.setDividerPosition(30.0);
      split.setWidget1(this.buildLeftPane());
      split.setWidget2(this.buildRightPane());
      this.setCenter(split);
   }

   private BWidget buildLeftPane() {
      this.add = new BTemplateConfigEditor.Add(this);
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
      this.setValue = new BTemplateConfigEditor.SetValue(this);
      this.setUserTip = new BTemplateConfigEditor.SetUserTip(this);
      this.rename = new BTemplateConfigEditor.Rename(this);
      this.remove = new BTemplateConfigEditor.Remove(this);
      this.moveUp = new BTemplateConfigEditor.MoveUp(this);
      this.moveDown = new BTemplateConfigEditor.MoveDown(this);
      this.setValue.setEnabled(false);
      this.setUserTip.setEnabled(false);
      this.rename.setEnabled(false);
      this.remove.setEnabled(false);
      this.moveUp.setEnabled(false);
      this.moveDown.setEnabled(false);
      BButton b = new BButton(this.rename);
      BButton c = new BButton(this.remove);
      BButton d = new BButton(this.moveUp);
      BButton e = new BButton(this.moveDown);
      BButton f = new BButton(this.setValue);
      b.setButtonStyle(BButtonStyle.toolBar);
      c.setButtonStyle(BButtonStyle.toolBar);
      d.setButtonStyle(BButtonStyle.toolBar);
      e.setButtonStyle(BButtonStyle.toolBar);
      f.setButtonStyle(BButtonStyle.toolBar);
      BGridPane topLeft = new BGridPane(3);
      topLeft.add(null, b);
      topLeft.add(null, c);
      topLeft.add(null, f);
      BGridPane topRight = new BGridPane(2);
      topRight.add(null, d);
      topRight.add(null, e);
      BEdgePane top = new BEdgePane();
      top.setLeft(topLeft);
      top.setRight(topRight);
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(top, 5.0, 0.0, 5.0, 0.0));
      pane.setCenter(new BBorderPane(this.table, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      return pane;
   }

   public void save() {
      this.templateConfig = this.createTemplateConfig();
   }

   public BTemplateConfig createTemplateConfig() {
      Context tx = Transaction.start(this.templateConfig, null);
      BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)this.table.getModel();

      for (int i = 0; i < model.getRowCount(); i++) {
         BTemplateConfigEditor.BindingSlot slot = model.get(i);
         if (slot.backup == null) {
            this.addConfigSlot(tx, slot);
         } else if (slot.dir == slot.backup.dir) {
            this.renameConfigSlot(tx, slot);
         } else {
            slot.backup.backup = slot.backup;
            this.removeConfigSlot(tx, slot.backup);
            this.addConfigSlot(tx, slot);
         }
      }

      for (BTemplateConfigEditor.BindingSlot slot : this.removed) {
         this.removeConfigSlot(tx, slot);
      }

      try {
         Transaction.end(this.templateConfig, tx);
      } catch (Exception var5) {
         BDialog.error(this.getShell(), "Error", "createTemplateConfig failed.", var5);
      }

      this.reorderConfigSlots();
      this.setConfigSlotsValues();
      return this.templateConfig;
   }

   private void addConfigSlot(Context tx, BTemplateConfigEditor.BindingSlot bindingSlot) {
      BComponent targetComp = (BComponent)BOrd.make(bindingSlot.ord).resolve(this.templateConfig).get();
      BValue value = null;
      int flags = 0;
      BFacets facets = BFacets.NULL;
      if (bindingSlot.slotOrAttribute.startsWith("#")) {
         value = BString.DEFAULT;
      } else {
         Slot targetSlot = targetComp.getSlot(bindingSlot.slotOrAttribute);
         if (targetSlot.isProperty()) {
            value = targetComp.get(bindingSlot.slotOrAttribute).newCopy();
            BFacets targetFacets = targetComp.getSlotFacets(targetSlot);
            if (targetFacets != null) {
               facets = targetFacets;
            }

            bindingSlot.childFlags = targetComp.getFlags(targetSlot);
            flags = bindingSlot.flags;
         }
      }

      if (value != null) {
         this.templateConfig.add(bindingSlot.name, value, flags, facets, tx);
         BConfigBinding binding = new BConfigBinding(targetComp.getHandleOrd(), bindingSlot.name, bindingSlot.slotOrAttribute, bindingSlot.userTip);
         this.templateConfig.add(null, binding, 4, BFacets.NULL, tx);
      }
   }

   private void renameConfigSlot(Context tx, BTemplateConfigEditor.BindingSlot slot) {
      if (!slot.backup.name.equals(slot.name)) {
         Property prop = this.templateConfig.getProperty(slot.backup.name);
         BString newName = BString.make(slot.name);
         this.templateConfig.rename(prop, slot.name, tx);
         BConfigBinding[] bindings = this.templateConfig.getConfigBindings();

         for (BConfigBinding binding : bindings) {
            Optional<BComplex> optional = binding.getTarget();
            if (optional.isPresent()) {
               String targetOrdString = optional.get().asComponent().getSlotPathOrd().toString();
               if (targetOrdString.equals(slot.backup.ord) && binding.getSourceSlot().equals(slot.backup.name)) {
                  binding.set(BConfigBinding.sourceSlot, newName, tx);
                  break;
               }
            }
         }
      }
   }

   private void removeConfigSlot(Context tx, BTemplateConfigEditor.BindingSlot slot) {
      BConfigBinding[] bindings = this.templateConfig.getConfigBindings();

      for (BConfigBinding binding : bindings) {
         BComponent target = binding.getTargetOrd().resolve(this.templateConfig).get().asComponent();
         if (target.getSlotPathOrd().toString().equals(slot.backup.ord) && binding.getSourceSlot().equals(slot.backup.name)) {
            this.templateConfig.remove(binding.getPropertyInParent(), tx);
            this.templateConfig.remove(this.templateConfig.getProperty(slot.backup.name), tx);
            return;
         }
      }

      throw new RuntimeException("Could not find config binding for " + slot.backup.name);
   }

   public void reorderConfigSlots() {
      BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)this.table.getModel();
      ArrayList<String> list = new ArrayList<>();

      for (int i = 0; i < model.getRowCount(); i++) {
         list.add(model.get(i).name);
      }

      this.templateConfig.loadSlots();
      SlotCursor<Property> c = this.templateConfig.getProperties();

      while (c.next()) {
         Property p = c.property();
         if (!p.isFrozen() && !list.contains(p.getName())) {
            list.add(p.getName());
         }
      }

      Property[] newProps = new Property[list.size()];

      for (int i = 0; i < list.size(); i++) {
         newProps[i] = this.templateConfig.getProperty(list.get(i));
      }

      if (!list.isEmpty()) {
         this.templateConfig.reorder(newProps);
      }
   }

   public void setConfigSlotsValues() {
      BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)this.table.getModel();
      ArrayList<String> list = new ArrayList<>();

      for (int i = 0; i < model.getRowCount(); i++) {
         list.add(model.get(i).name);
      }

      SlotCursor<Property> c = this.templateConfig.getProperties();

      while (c.next()) {
         Property p = c.property();
         if (!p.isFrozen()) {
            if (p.getType().is(BConfigBinding.TYPE)) {
               int nameIndex = list.indexOf(((BConfigBinding)this.templateConfig.get(p)).getSourceSlot());
               if (nameIndex >= 0) {
                  String userTip = model.get(nameIndex).userTip;
                  ((BConfigBinding)this.templateConfig.get(p)).setUserTip(userTip);
               }
            } else {
               int nameIndex = list.indexOf(p.getName());
               if (nameIndex >= 0) {
                  boolean facetAdded = false;
                  BFacets facets = model.get(nameIndex).facets;
                  if (facets != null) {
                     facetAdded = !this.templateConfig.getSlotFacets(p).equals(facets);
                  }

                  BValue value = model.get(nameIndex).value;
                  if (!this.templateConfig.get(p).equals(value) || facetAdded) {
                     this.templateConfig.set(p, value.newCopy(true));
                     if (facets != null) {
                        this.templateConfig.setFacets(p, facets);
                     }
                  }
               }
            }
         }
      }
   }

   private String getUniqueName(String name) {
      BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)this.table.getModel();
      this.templateConfig.loadSlots();
      Slot[] s = this.templateConfig.getSlotsArray();
      int count = 1;
      String unescapedTempName = SlotPath.unescape(name);
      if (unescapedTempName.chars().filter(ch -> ch == 58).count() == 1L) {
         name = SlotPath.escape(unescapedTempName.replace(':', '_'));
      }

      String tempName = name;

      for (int i = 0; i < s.length + model.getRowCount(); i++) {
         if (i < s.length) {
            if (s[i].getName().equals(tempName)) {
               tempName = name + count;
               count++;
               i = -1;
            }
         } else {
            BTemplateConfigEditor.BindingSlot cs = model.get(i - s.length);
            if (cs.name.equals(tempName)) {
               tempName = name + count;
               count++;
               i = -1;
            }
         }
      }

      return tempName;
   }

   public static BComplex getParentOfType(Type parentType, BComplex base) {
      for (BComplex parent = base.getParent(); parent != null; parent = parent.getParent()) {
         if (parent.getType().is(parentType)) {
            return parent;
         }
      }

      return null;
   }

   public class Add extends Command {
      public Add(BWidget owner) {
         super(owner, BTemplateConfigEditor.module, "controlAppConfigEditor.add");
      }

      public CommandArtifact doInvoke() {
         BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel();
         TreeNode[] nodes = BTemplateConfigEditor.this.tree.getSelection().getNodes();
         ArrayList<Integer> newRows = new ArrayList<>();

         for (TreeNode node : nodes) {
            if (node instanceof BTemplateConfigEditor.AttributeNode) {
               BTemplateConfigEditor.AttributeNode attributeNode = (BTemplateConfigEditor.AttributeNode)node;
               TreeNode parentNode = attributeNode.getParent();
               if (parentNode instanceof BTemplateConfigEditor.NodeGroup) {
                  parentNode = parentNode.getParent();
               }

               BComponent parent = ((BTemplateConfigEditor.Node)parentNode).ref.object.asComponent();
               String ord = parent.getSlotPath().toString();
               boolean hasEntry = false;

               for (int j = 0; j < model.getRowCount(); j++) {
                  BTemplateConfigEditor.BindingSlot temp = model.get(j);
                  if (temp.slotOrAttribute.equals(attributeNode.id()) && temp.ord.equals(ord)) {
                     hasEntry = true;
                     break;
                  }
               }

               if (!hasEntry) {
                  String parentName = parent.getName();
                  String configName = attributeNode.defaultSlotName(parentName);
                  BValue defaultValue = attributeNode.defaultValue(parentName);
                  Type configType = attributeNode.configType();
                  model.add(
                     0,
                     BTemplateConfigEditor.this.getUniqueName(configName),
                     ord,
                     parent,
                     attributeNode.id(),
                     defaultValue,
                     BFacets.NULL,
                     configType,
                     false,
                     false,
                     0,
                     ""
                  );
                  newRows.add(model.getRowCount() - 1);
               }
            } else if (node instanceof BTemplateConfigEditor.Node) {
               BTemplateConfigEditor.Node n = (BTemplateConfigEditor.Node)node;
               if (!(n.ref.object instanceof BComponent) && ((BTemplateConfigEditor.Node)n.getParent()).ref.object instanceof BComponent) {
                  BComponent parent = (BComponent)((BTemplateConfigEditor.Node)n.getParent()).ref.object;
                  String name = n.ref.name;
                  if (parent instanceof BControlPoint) {
                     name = parent.getName() + '_' + name;
                  } else {
                     BControlPoint parentPoint = (BControlPoint)BTemplateConfigEditor.getParentOfType(BControlPoint.TYPE, parent);
                     if (parentPoint != null) {
                        name = parentPoint.getName() + '_' + name;
                     }
                  }

                  String ord = parent.getSlotPath().toString();
                  Slot slot = parent.getSlot(n.ref.name);
                  boolean inExists = false;
                  boolean outExists = false;

                  for (int jx = 0; jx < model.getRowCount(); jx++) {
                     BTemplateConfigEditor.BindingSlot temp = model.get(jx);
                     if ((temp.ord + temp.slotOrAttribute).equals(ord + slot.getName())) {
                        if (temp.dir == 1) {
                           outExists = true;
                        }

                        if (temp.dir == 0) {
                           inExists = true;
                        }
                     }
                  }

                  if (!inExists || !outExists) {
                     boolean readonly = slot.isTopic() || Flags.isReadonly(parent, slot);
                     if (!outExists || !readonly) {
                        int dir = readonly ? 1 : 0;
                        if (outExists) {
                           dir = 0;
                        } else if (inExists) {
                           dir = 1;
                        }

                        Type type = n.ref.object.getType();
                        if (!slot.isTopic() && !slot.isAction()) {
                           BFacets facets = parent.getSlotFacets(slot);
                           model.add(
                              dir,
                              BTemplateConfigEditor.this.getUniqueName(name),
                              ord,
                              parent,
                              n.ref.name,
                              (BValue)n.ref.object,
                              facets,
                              type,
                              readonly,
                              false,
                              parent.getFlags(slot),
                              ""
                           );
                           newRows.add(model.getRowCount() - 1);
                        }
                     }
                  }
               }
            }
         }

         if (newRows.size() > 0) {
            BTemplateConfigEditor.this.table.requestFocus();
            BTemplateConfigEditor.this.table.getSelection().deselectAll();

            for (Integer newRow : newRows) {
               BTemplateConfigEditor.this.table.getSelection().select(newRow);
            }

            BTemplateConfigEditor.this.table.computePreferredSize();
            BTemplateConfigEditor.this.table.relayout();
            BTemplateConfigEditor.Model treeModel = (BTemplateConfigEditor.Model)BTemplateConfigEditor.this.tree.getModel();
            ((BTemplateConfigEditor.Node)treeModel.getRoot(0)).load(true);
            treeModel.updateTree();
            ((BTemplateConfigEditor.BindingController)BTemplateConfigEditor.this.table.getController()).syncTree();
            BTemplateConfigEditor.this.view.templateModified();
         }

         return null;
      }
   }

   class AppExcludedComponentNode extends BTemplateConfigEditor.Node {
      AppExcludedComponentNode(TreeNode parent, RefNode ref) {
         super(parent, ref);
      }

      @Override
      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         return (BTemplateConfigEditor.Node)(!(childRef.object instanceof BINavNode)
            ? super.makeChildNode(childRef)
            : BTemplateConfigEditor.this.new AppExcludedDescendantNode(this, childRef));
      }

      @Override
      public BImage getIcon() {
         return BImage.make(BIcon.make(this.ref.object.getIcon(), BTemplateConfigEditor.REMOVE_BADGE_ICON));
      }
   }

   class AppExcludedDescendantNode extends BTemplateConfigEditor.Node {
      AppExcludedDescendantNode(TreeNode parent, RefNode ref) {
         super(parent, ref);
      }

      @Override
      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         return (BTemplateConfigEditor.Node)(!(childRef.object instanceof BINavNode)
            ? super.makeChildNode(childRef)
            : BTemplateConfigEditor.this.new AppExcludedDescendantNode(this, childRef));
      }

      @Override
      public BImage getIcon() {
         return BImage.make(BIcon.make(this.ref.object.getIcon(), BTemplateConfigEditor.REMOVE_BADGE_ICON));
      }
   }

   class AppIncludedComponentNode extends BTemplateConfigEditor.Node {
      AppIncludedComponentNode(TreeNode parent, RefNode ref) {
         super(parent, ref);
      }

      @Override
      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         if (!(childRef.object instanceof BINavNode)) {
            return super.makeChildNode(childRef);
         } else {
            return (BTemplateConfigEditor.Node)(this.getTree().getModel() instanceof BTemplateConfigEditor.Model
                  && ((BTemplateConfigEditor.Model)this.getTree().getModel()).isOptionalNode((BINavNode)childRef.object)
               ? BTemplateConfigEditor.this.new AppOptionalDescendantNode(this, childRef)
               : BTemplateConfigEditor.this.new AppIncludedDescendantNode(this, childRef));
         }
      }

      @Override
      public BImage getIcon() {
         return BImage.make(BIcon.make(this.ref.object.getIcon(), BTemplateConfigEditor.ADD_BADGE_ICON));
      }
   }

   class AppIncludedDescendantNode extends BTemplateConfigEditor.Node {
      AppIncludedDescendantNode(TreeNode parent, RefNode ref) {
         super(parent, ref);
      }

      @Override
      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         if (!(childRef.object instanceof BINavNode)) {
            return super.makeChildNode(childRef);
         } else {
            return (BTemplateConfigEditor.Node)(this.getTree().getModel() instanceof BTemplateConfigEditor.Model
                  && ((BTemplateConfigEditor.Model)this.getTree().getModel()).isOptionalNode((BINavNode)childRef.object)
               ? BTemplateConfigEditor.this.new AppOptionalDescendantNode(this, childRef)
               : BTemplateConfigEditor.this.new AppIncludedDescendantNode(this, childRef));
         }
      }

      @Override
      public BImage getIcon() {
         return BImage.make(BIcon.make(this.ref.object.getIcon(), BTemplateConfigEditor.ADD_BADGE_ICON));
      }
   }

   class AppMergedContainerNode extends BTemplateConfigEditor.Node {
      final NameTree exclusions;

      AppMergedContainerNode(TreeModel model, RefNode ref, NameTree exclusions) {
         super(model, ref);

         assert exclusions != null;

         this.exclusions = exclusions;
      }

      AppMergedContainerNode(TreeNode parent, RefNode ref, NameTree exclusions) {
         super(parent, ref);

         assert exclusions != null;

         this.exclusions = exclusions;
      }

      @Override
      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         if (!(childRef.object instanceof BINavNode)) {
            return super.makeChildNode(childRef);
         } else if (this.getTree().getModel() instanceof BTemplateConfigEditor.Model
            && ((BTemplateConfigEditor.Model)this.getTree().getModel()).isOptionalNode((BINavNode)childRef.object)) {
            return BTemplateConfigEditor.this.new AppOptionalComponentNode(this, childRef);
         } else if (!this.exclusions.has(childRef.name)) {
            return BTemplateConfigEditor.this.new AppIncludedComponentNode(this, childRef);
         } else {
            NameTree childExclusions = this.exclusions.fetch(childRef.name);
            return (BTemplateConfigEditor.Node)(childExclusions == null
               ? BTemplateConfigEditor.this.new AppExcludedComponentNode(this, childRef)
               : BTemplateConfigEditor.this.new AppMergedContainerNode(this, childRef, childExclusions));
         }
      }
   }

   class AppOptionalComponentNode extends BTemplateConfigEditor.Node {
      AppOptionalComponentNode(TreeNode parent, RefNode ref) {
         super(parent, ref);
      }

      @Override
      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         return (BTemplateConfigEditor.Node)(!(childRef.object instanceof BINavNode)
            ? super.makeChildNode(childRef)
            : BTemplateConfigEditor.this.new AppOptionalDescendantNode(this, childRef));
      }

      @Override
      public BImage getIcon() {
         return BImage.make(BIcon.make(this.ref.object.getIcon(), BTemplateConfigEditor.OPTIONAL_BADGE_ICON));
      }
   }

   class AppOptionalDescendantNode extends BTemplateConfigEditor.Node {
      AppOptionalDescendantNode(TreeNode parent, RefNode ref) {
         super(parent, ref);
      }

      @Override
      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         return (BTemplateConfigEditor.Node)(!(childRef.object instanceof BINavNode)
            ? super.makeChildNode(childRef)
            : BTemplateConfigEditor.this.new AppOptionalDescendantNode(this, childRef));
      }

      @Override
      public BImage getIcon() {
         return BImage.make(BIcon.make(this.ref.object.getIcon(), BTemplateConfigEditor.OPTIONAL_BADGE_ICON));
      }
   }

   abstract class AttributeNode extends TreeNode {
      AttributeNode(TreeNode parent) {
         super(parent);
      }

      public int getChildCount() {
         return 0;
      }

      public TreeNode getChild(int index) {
         return null;
      }

      boolean isExisting() {
         BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel();
         TreeNode parent = this.getParent();
         if (parent instanceof BTemplateConfigEditor.NodeGroup) {
            parent = parent.getParent();
         }

         if (!(parent instanceof BTemplateConfigEditor.Node)) {
            return false;
         } else {
            BObject refParent = ((BTemplateConfigEditor.Node)parent).ref.object;
            return !refParent.isComponent() ? false : model.isExisting(refParent.asComponent(), this.id());
         }
      }

      public abstract String id();

      public abstract String defaultSlotName(String var1);

      public abstract BValue defaultValue(String var1);

      public abstract Type configType();
   }

   public static class BBindingTable extends BTable {
      public BBindingTable(TableModel model) {
         super(model);
      }

      public double getCellWidth(int col) {
         return super.getCellWidth(col);
      }
   }

   class BindingController extends TableController {
      public void keyPressed(BKeyEvent event) {
         super.keyPressed(event);
         if (event.getKeyCode() == 127 && BTemplateConfigEditor.this.remove.isEnabled()) {
            BTemplateConfigEditor.this.remove.invoke();
         }
      }

      protected void handleEnter(BKeyEvent event) {
         event.consume();
         if (BTemplateConfigEditor.this.rename.isEnabled()) {
            BTemplateConfigEditor.this.rename.invoke();
         }

         BTemplateConfigEditor.this.table.repaint();
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         if (column == 2) {
            BTemplateConfigEditor.this.setValue.invoke();
         } else if (column == 3) {
            BTemplateConfigEditor.this.setUserTip.invoke();
         } else if (BTemplateConfigEditor.this.rename.isEnabled()) {
            BTemplateConfigEditor.this.rename.invoke();
         }

         BTemplateConfigEditor.this.table.repaint();
      }

      protected void checkSelection(BMouseEvent event, int row) {
         super.checkSelection(event, row);
         this.syncTree();
      }

      protected void syncTree() {
         TableSelection cacSel = BTemplateConfigEditor.this.table.getSelection();
         int selRow = cacSel.getRow();
         if (selRow >= 0) {
            BTemplateConfigEditor.BindingSlot slot = ((BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel()).get(selRow);
            BComponent child = (BComponent)BOrd.make(slot.ord).resolve(BTemplateConfigEditor.this.templateConfig).get();
            TreeModel model = BTemplateConfigEditor.this.tree.getModel();
            SlotPath initialPath = child.getSlotPath();
            SlotPath rootPath = BTemplateConfigEditor.this.templateRoot.getSlotPath();
            if (initialPath == null) {
               model.getRoot(0).setExpanded(true);
            } else {
               String[] names = initialPath.getNames();
               String[] rootNames = rootPath.getNames();
               BTemplateConfigEditor.Node node = (BTemplateConfigEditor.Node)model.getRoot(0);

               for (int i = rootNames.length; i < names.length; i++) {
                  BTemplateConfigEditor.Node temp = (BTemplateConfigEditor.Node)node.getChild(names[i]);
                  if (temp == null) {
                     break;
                  }

                  node = temp;
               }

               TreeNode selectNode = node;
               if (slot.slotOrAttribute.startsWith("#")) {
                  BTemplateConfigEditor.AttributeNode attributeNode = this.FindChildAttributeNode(node, slot.slotOrAttribute);
                  if (attributeNode != null) {
                     selectNode = attributeNode;
                  }
               } else {
                  BTemplateConfigEditor.Node temp = (BTemplateConfigEditor.Node)node.getChild(slot.slotOrAttribute);
                  if (temp != null) {
                     selectNode = temp;
                  }
               }

               TreeNode[] path = selectNode.getPathFromRoot();
               BTemplateConfigEditor.this.tree.scrollPathToVisible(path);
               TreeNode n = path[path.length - 1];
               BTemplateConfigEditor.this.tree.getSelection().deselectAll();
               BTemplateConfigEditor.this.tree.getSelection().select(n);
               BTemplateConfigEditor.this.tree.getController().setFocus(BTemplateConfigEditor.this.tree.getSelection().getNode());
               n.setExpanded(true);
            }

            model.updateTree();
         }
      }

      private BTemplateConfigEditor.AttributeNode FindChildAttributeNode(TreeNode node, String attributeId) {
         BTemplateConfigEditor.AttributeNode childAttributeNode = null;

         for (int i = 0; childAttributeNode == null && i < node.getChildCount(); i++) {
            TreeNode childNode = node.getChild(i);
            if (childNode instanceof BTemplateConfigEditor.NodeGroup) {
               childAttributeNode = this.FindChildAttributeNode(childNode, attributeId);
            } else if (childNode instanceof BTemplateConfigEditor.AttributeNode) {
               BTemplateConfigEditor.AttributeNode possible = (BTemplateConfigEditor.AttributeNode)childNode;
               if (attributeId.equals(possible.id())) {
                  childAttributeNode = possible;
               }
            }
         }

         return childAttributeNode;
      }
   }

   static class BindingHeaderRenderer extends TableHeaderRenderer {
      public double getPreferredHeaderWidth(Header header) {
         double width = super.getPreferredHeaderWidth(header);
         return header.column == 0 && width < BTemplateConfigEditor.COL0_MIN_WIDTH ? BTemplateConfigEditor.COL0_MIN_WIDTH : width;
      }
   }

   public class BindingModel extends TableModel {
      ArrayList<BTemplateConfigEditor.BindingSlot> kids = new ArrayList<>();

      public int getRowCount() {
         return this.kids.size();
      }

      public int getColumnCount() {
         return 4;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BTemplateConfigEditor.lex.getText("controlAppConfigEditor.slot");
            case 1:
               return BTemplateConfigEditor.lex.getText("controlAppConfigEditor.ord");
            case 2:
               return BTemplateConfigEditor.lex.getText("controlAppConfigEditor.value");
            case 3:
               return BTemplateConfigEditor.lex.getText("controlAppConfigEditor.userTip");
            default:
               return "";
         }
      }

      public Object getValueAt(int row, int col) {
         BTemplateConfigEditor.BindingSlot slot = this.kids.get(row);
         switch (col) {
            case 0:
               return SlotPath.unescape(slot.name);
            case 1:
               String a = BTemplateConfigEditor.this.templateRoot.getSlotPath().toString();
               String b = slot.ord + "/" + slot.slotOrAttribute;
               return b.substring(a.length());
            case 2:
               if (slot.value instanceof BPassword) {
                  return "";
               } else {
                  if (slot.value instanceof BUsernameAndPassword) {
                     return ((BUsernameAndPassword)slot.value).getUsername();
                  }

                  return slot.value.toString(slot.facets);
               }
            case 3:
               return slot.userTip;
            default:
               return "";
         }
      }

      public Object getSubject(int row) {
         return this.kids.get(row);
      }

      public void add(
         int dir,
         String name,
         String ord,
         BComplex parent,
         String slotOrAttribute,
         BValue value,
         BFacets facets,
         Type type,
         boolean readonly,
         boolean backup,
         int flags,
         String userTip
      ) {
         BTemplateConfigEditor.BindingSlot slot = new BTemplateConfigEditor.BindingSlot();
         slot.name = name;
         slot.ord = ord;
         slot.parent = parent;
         slot.slotOrAttribute = slotOrAttribute;
         slot.dir = dir;
         slot.value = value;
         slot.facets = facets;
         slot.type = type;
         slot.readonly = readonly;
         slot.flags = flags;
         slot.userTip = userTip;
         if (backup) {
            slot.backup = new BTemplateConfigEditor.BindingSlot();
            slot.backup.name = name;
            slot.backup.ord = ord;
            slot.backup.parent = parent;
            slot.backup.slotOrAttribute = slotOrAttribute;
            slot.backup.dir = dir;
            slot.backup.value = value;
            slot.backup.type = type;
            slot.backup.readonly = readonly;
            slot.backup.flags = flags;
            slot.backup.userTip = userTip;
         }

         this.kids.add(slot);
      }

      public void remove(int row) {
         this.kids.remove(row);
      }

      public BTemplateConfigEditor.BindingSlot get(int row) {
         return row > this.kids.size() ? null : this.kids.get(row);
      }

      public void move(int row, int newRow) {
         BTemplateConfigEditor.BindingSlot obj = this.kids.remove(row);
         this.kids.add(newRow, obj);
      }

      boolean isExisting(BComponent parent, Slot slot) {
         String slotName = slot == null ? null : slot.getName();
         return this.isExisting(parent, slotName);
      }

      boolean isExisting(BComponent parent, String slotOrAttribute) {
         String parentOrd = parent.getSlotPath().toString();

         for (BTemplateConfigEditor.BindingSlot kid : this.kids) {
            if (kid.ord.equals(parentOrd) && Objects.equals(slotOrAttribute, kid.slotOrAttribute)) {
               return true;
            }
         }

         return false;
      }
   }

   public class BindingRenderer extends TableCellRenderer {
      public BBrush getBackground(Cell cell) {
         return cell.column == 2 ? BColor.white.toBrush() : BTemplateConfigEditor.colorProperty;
      }

      public double getPreferredCellWidth(Cell cell) {
         double width = super.getPreferredCellWidth(cell);
         return cell.column == 0 && width < BTemplateConfigEditor.COL0_MIN_WIDTH ? BTemplateConfigEditor.COL0_MIN_WIDTH : width;
      }
   }

   class BindingSelection extends TableSelection {
      public void updateTable() {
         super.updateTable();
         int[] rows = this.getRows();
         BTemplateConfigEditor.this.rename.setEnabled(rows.length > 0);
         BTemplateConfigEditor.this.remove.setEnabled(rows.length > 0);
         BTemplateConfigEditor.this.moveUp.setEnabled(rows.length > 0);
         BTemplateConfigEditor.this.moveDown.setEnabled(rows.length > 0);
         BTemplateConfigEditor.this.setValue.setEnabled(rows.length > 0);
         BTemplateConfigEditor.this.setUserTip.setEnabled(rows.length > 0);
      }
   }

   public static class BindingSlot {
      public String name;
      public String ord;
      public String slotOrAttribute;
      public BValue value;
      public BFacets facets;
      public BComplex parent;
      public Type type;
      public int dir;
      public boolean readonly = false;
      public String userTip;
      public static final int IN = 0;
      public static final int OUT = 1;
      public BTemplateConfigEditor.BindingSlot backup = null;
      public int flags = 0;
      public int childFlags = 0;
   }

   class Controller extends TreeController {
      protected void doSelectAction(TreeNode target, double x, double y) {
         if (BTemplateConfigEditor.this.add.isEnabled()) {
            BTemplateConfigEditor.this.add.invoke();
         }
      }

      public void setFocus(TreeNode node) {
         super.setFocus(node);
         if (node instanceof BTemplateConfigEditor.Node && !(((BTemplateConfigEditor.Node)node).ref.object instanceof BComponent)) {
            BTemplateConfigEditor.this.add.setEnabled(!((BTemplateConfigEditor.Node)node).isExisting());
         } else if (node instanceof BTemplateConfigEditor.AttributeNode) {
            BTemplateConfigEditor.this.add.setEnabled(!((BTemplateConfigEditor.AttributeNode)node).isExisting());
         }
      }
   }

   class DisplayNameNode extends BTemplateConfigEditor.AttributeNode {
      DisplayNameNode(TreeNode parent) {
         super(parent);
      }

      public String getText() {
         return BTemplateConfigEditor.lex.get("configEditorTree.displayNameAttribute.label");
      }

      public BImage getIcon() {
         return BImage.make(BIcon.std("rename.png"));
      }

      @Override
      public String id() {
         return "#DisplayName";
      }

      @Override
      public String defaultSlotName(String parentName) {
         return BTemplateConfigEditor.lex.getText("controlAppConfigEditor.displayNameAttribute.default", new Object[]{parentName});
      }

      @Override
      public BValue defaultValue(String parentName) {
         return BString.DEFAULT;
      }

      @Override
      public Type configType() {
         return BString.TYPE;
      }
   }

   class Model extends TreeModel {
      final BTemplateConfigEditor.Node root;
      private final TemplateManifest manifest;

      public Model(BComponent rootNode, boolean isApplicationTemplate, TemplateManifest manifest) {
         this.root = (BTemplateConfigEditor.Node)(isApplicationTemplate
            ? BTemplateConfigEditor.this.new AppMergedContainerNode(this, new RefNode(rootNode), ApplicationTemplateUtil.describeDefaultStation(null))
            : BTemplateConfigEditor.this.new Node(this, new RefNode(rootNode)));
         this.manifest = manifest;
      }

      public int getRootCount() {
         return 1;
      }

      public TreeNode getRoot(int index) {
         return this.root;
      }

      boolean isOptionalNode(BINavNode navNode) {
         OrdQuery[] queries = navNode.getNavOrd().parse();

         for (OrdQuery query : queries) {
            if (query instanceof SlotPath && query != null && this.manifest.optional.contains(BOrd.make(query))) {
               return true;
            }
         }

         return false;
      }
   }

   public class MoveDown extends Command {
      public MoveDown(BWidget owner) {
         super(owner, BTemplateConfigEditor.module, "controlAppConfigEditor.moveDown");
      }

      public CommandArtifact doInvoke() {
         BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel();
         int[] rows = model.getSelection().getRows();
         if (rows.length != 0 && rows[rows.length - 1] != model.getRowCount() - 1) {
            for (int i = rows.length - 1; i >= 0; i--) {
               model.getSelection().deselect(rows[i]);
               model.move(rows[i], rows[i] + 1);
               model.getSelection().select(rows[i] + 1);
            }

            BTemplateConfigEditor.this.table.relayout();
            BTemplateConfigEditor.this.view.templateModified();
            return null;
         } else {
            return null;
         }
      }
   }

   public class MoveUp extends Command {
      public MoveUp(BWidget owner) {
         super(owner, BTemplateConfigEditor.module, "controlAppConfigEditor.moveUp");
      }

      public CommandArtifact doInvoke() {
         BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel();
         int[] rows = model.getSelection().getRows();
         if (rows.length != 0 && rows[0] != 0) {
            for (int row : rows) {
               model.getSelection().deselect(row);
               model.move(row, row - 1);
               model.getSelection().select(row - 1);
            }

            BTemplateConfigEditor.this.table.relayout();
            BTemplateConfigEditor.this.view.templateModified();
            return null;
         } else {
            return null;
         }
      }
   }

   class NameNode extends BTemplateConfigEditor.AttributeNode {
      NameNode(TreeNode parent) {
         super(parent);
      }

      public String getText() {
         return BTemplateConfigEditor.lex.get("configEditorTree.nameAttribute.label");
      }

      public BImage getIcon() {
         return BImage.make(BIcon.std("rename.png"));
      }

      @Override
      public String id() {
         return "#Name";
      }

      @Override
      public String defaultSlotName(String parentName) {
         return BTemplateConfigEditor.lex.getText("controlAppConfigEditor.nameAttribute.default", new Object[]{parentName});
      }

      @Override
      public BValue defaultValue(String parentName) {
         return BString.make(parentName);
      }

      @Override
      public Type configType() {
         return BString.TYPE;
      }
   }

   class Node extends TreeNode {
      RefNode ref;
      TreeNode[] kids;

      public Node(TreeModel model, RefNode ref) {
         super(model);
         this.ref = ref;
      }

      public Node(TreeNode parent, RefNode ref) {
         super(parent);
         this.ref = ref;
      }

      public Object getSubject() {
         return this.ref;
      }

      public String getText() {
         return this.ref.text;
      }

      public BImage getIcon() {
         BImage icon = this.ref.icon;
         return this.isExisting() ? icon.getDisabledImage() : icon;
      }

      public boolean hasChildren() {
         if (!(this.ref.object instanceof BComponent)) {
            return false;
         } else {
            return this.kids == null ? true : this.kids.length > 0;
         }
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

         for (TreeNode kid : this.kids) {
            if (kid instanceof BTemplateConfigEditor.Node && ((BTemplateConfigEditor.Node)kid).ref.name.equals(name)) {
               return kid;
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
            List<TreeNode> list = new ArrayList<>();
            if (this.ref.object != null && this.ref.object.isComponent()) {
               BTemplateConfigEditor.NodeGroup attributesNode = this.makeNodeGroup(BTemplateConfigEditor.lex.get("configEditorTree.attributesGroup.label"));
               if (BTemplateConfigEditor.this.view.isApplicationTemplate() && (this.ref.object instanceof BDevice || this.ref.object instanceof BDeviceFolder)) {
                  Property p = this.ref.object.asComponent().getPropertyInParent();
                  if (p != null && p.isDynamic()) {
                     this.makeNameNode(attributesNode);
                  }
               }

               this.makeDisplayNameNode(attributesNode);
               list.add(attributesNode);
            }

            RefNode[] refs = this.ref.getChildren(BTemplateConfigEditor.configProps);

            for (RefNode childRef : refs) {
               list.add(this.makeChildNode(childRef));
            }

            this.kids = list.toArray(new TreeNode[0]);
         }
      }

      boolean isExisting() {
         BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel();
         BObject refObject = this.ref.object;
         return refObject != null && this.ref.parent != null && this.ref.parent instanceof BComponent
            ? model.isExisting(this.ref.parent.asComponent(), this.ref.slot)
            : false;
      }

      BTemplateConfigEditor.Node makeChildNode(RefNode childRef) {
         return BTemplateConfigEditor.this.new Node(this, childRef);
      }

      BTemplateConfigEditor.NodeGroup makeNodeGroup(String name) {
         return new BTemplateConfigEditor.NodeGroup(this, name);
      }

      TreeNode makeNameNode(BTemplateConfigEditor.NodeGroup group) {
         TreeNode node = BTemplateConfigEditor.this.new NameNode((TreeNode)(group == null ? this : group));
         if (group != null) {
            group.addChild(node);
         }

         return node;
      }

      TreeNode makeDisplayNameNode(BTemplateConfigEditor.NodeGroup group) {
         TreeNode node = BTemplateConfigEditor.this.new DisplayNameNode((TreeNode)(group == null ? this : group));
         if (group != null) {
            group.addChild(node);
         }

         return node;
      }
   }

   static class NodeGroup extends TreeNode {
      private final String groupName;
      private final ArrayList<TreeNode> kids = new ArrayList<>();

      public NodeGroup(TreeNode parent, String groupName) {
         super(parent);
         this.groupName = groupName;
      }

      public String getText() {
         return this.groupName;
      }

      public BImage getIcon() {
         return BImage.make(BIcon.std("folder.png"));
      }

      public int getChildCount() {
         return this.kids.size();
      }

      public TreeNode getChild(int index) {
         return this.kids.get(index);
      }

      public void addChild(TreeNode child) {
         this.kids.add(child);
      }
   }

   public class Remove extends Command {
      public Remove(BWidget owner) {
         super(owner, BTemplateConfigEditor.module, "controlAppConfigEditor.remove");
      }

      public CommandArtifact doInvoke() {
         BTemplateConfigEditor.BindingModel model = (BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel();
         int[] rows = model.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            BTemplateConfigEditor.BindingSlot cSlot = model.get(rows[i] - i);
            if (cSlot.backup != null) {
               BTemplateConfigEditor.this.removed.add(cSlot);
            }

            model.remove(rows[i] - i);
         }

         BTemplateConfigEditor.this.table.getSelection().deselectAll();
         BTemplateConfigEditor.this.table.relayout();
         BTemplateConfigEditor.Model treeModel = (BTemplateConfigEditor.Model)BTemplateConfigEditor.this.tree.getModel();
         ((BTemplateConfigEditor.Node)treeModel.getRoot(0)).load(true);
         treeModel.updateTree();
         BTemplateConfigEditor.this.view.templateModified();
         return null;
      }
   }

   public class Rename extends Command {
      public Rename(BWidget owner) {
         super(owner, BTemplateConfigEditor.module, "controlAppConfigEditor.rename");
      }

      public CommandArtifact doInvoke() {
         int[] rows = BTemplateConfigEditor.this.table.getSelection().getRows();

         for (int row : rows) {
            BTemplateConfigEditor.BindingSlot slot = ((BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel()).get(row);
            BTextField field = new BTextField(SlotPath.unescape(slot.name), 25);
            BGridPane grid = new BGridPane(1);
            grid.add(null, field);
            BBorderPane pane = new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0);
            if (1 != BDialog.open(this.getOwner(), BTemplateConfigEditor.lex.getText("controlAppConfigEditor.rename.label"), pane, 3)) {
               break;
            }

            if (!slot.name.equals(SlotPath.escape(field.getText()))) {
               BTemplateConfigEditor.this.templateConfig.loadSlots();
               Slot[] s = BTemplateConfigEditor.this.templateConfig.getSlotsArray();

               for (Slot value : s) {
                  if (value.getName().equals(SlotPath.escape(field.getText()))) {
                     BDialog.error(
                        this.getOwner(),
                        BTemplateConfigEditor.lex.getText("controlAppConfigEditor.rename.label"),
                        BTemplateConfigEditor.lex.getText("controlAppConfigEditor.rename.exists")
                     );
                     return null;
                  }
               }

               for (int i = 0; i < BTemplateConfigEditor.this.table.getModel().getRowCount(); i++) {
                  BTemplateConfigEditor.BindingSlot cs = ((BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel()).get(i);
                  if (cs.name.equals(SlotPath.escape(field.getText()))) {
                     BDialog.error(
                        this.getOwner(),
                        BTemplateConfigEditor.lex.getText("controlAppConfigEditor.rename.label"),
                        BTemplateConfigEditor.lex.getText("controlAppConfigEditor.rename.exists")
                     );
                     return null;
                  }
               }

               slot.name = SlotPath.escape(field.getText());
               BTemplateConfigEditor.this.table.repaint();
            }
         }

         BTemplateConfigEditor.this.view.templateModified();
         return null;
      }
   }

   public class SetUserTip extends Command {
      public SetUserTip(BWidget owner) {
         super(owner, BTemplateConfigEditor.module, "controlAppConfigEditor.setUserTip");
      }

      public CommandArtifact doInvoke() {
         int[] rows = BTemplateConfigEditor.this.table.getSelection().getRows();

         for (int row : rows) {
            BTemplateConfigEditor.BindingSlot slot = ((BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel()).get(row);
            BString valueAt = BString.make(slot.userTip);
            BWbFieldEditor editor = BWbFieldEditor.makeFor(valueAt);
            editor.loadValue(valueAt);
            BGridPane grid = new BGridPane(1);
            grid.add(null, new BLabel(SlotPath.unescape(slot.name)));
            grid.add(null, editor);
            BBorderPane pane = new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0);
            if (1 != BDialog.open(BTemplateConfigEditor.this.view, BTemplateConfigEditor.lex.getText("controlAppConfigEditor.setUserTip.label"), pane, 3)) {
               break;
            }

            BValue newValue = null;

            try {
               newValue = editor.saveValue().asValue();
            } catch (Exception var13) {
               var13.printStackTrace();
            }

            if (newValue != null) {
               String newString = newValue.toString();
               if (!slot.userTip.contentEquals(newString)) {
                  slot.userTip = newString;
                  BTemplateConfigEditor.this.table.repaint();
               }
            }
         }

         BTemplateConfigEditor.this.view.templateModified();
         return null;
      }
   }

   public class SetValue extends Command {
      public SetValue(BWidget owner) {
         super(owner, BTemplateConfigEditor.module, "controlAppConfigEditor.setValue");
      }

      public CommandArtifact doInvoke() {
         int[] rows = BTemplateConfigEditor.this.table.getSelection().getRows();

         for (int row : rows) {
            BTemplateConfigEditor.BindingSlot slot = ((BTemplateConfigEditor.BindingModel)BTemplateConfigEditor.this.table.getModel()).get(row);
            BValue editingValue = slot.value;
            if (!(editingValue instanceof BPassword)) {
               BFacets editorFacets = slot.facets;
               String editorName = slot.name;
               if (editingValue instanceof BUsernameAndPassword) {
                  BUsernameAndPassword usernameAndPassword = (BUsernameAndPassword)editingValue;
                  Property username = usernameAndPassword.getProperty("username");
                  editingValue = usernameAndPassword.get(username);
                  editorFacets = username.getFacets();
                  editorName = username.getName();
               }

               boolean isNiagaraProxyExt = false;
               BComplex parent = slot.parent;
               BCheckBox useNeqlCheckbox = new BCheckBox(BTemplateConfigEditor.lex.getText("controlAppConfigEditor.setValue.useNeql"));
               if ("NiagaraProxyExt".equals(parent.getType().getTypeName())) {
                  isNiagaraProxyExt = true;
               }

               BWbFieldEditor editor = BWbFieldEditor.makeFor(editingValue);
               editor.loadValue(editingValue, editorFacets);
               BWidget content = editor.getContent();
               if (content instanceof BExpandablePane) {
                  ((BExpandablePane)content).setExpanded(true);
               }

               BGridPane grid = new BGridPane(1);
               grid.add(null, new BLabel(editorName));
               grid.add(null, editor);
               BBoolean isUseNeqlSelected = (BBoolean)editorFacets.get("useNeql", BBoolean.FALSE);
               if (isNiagaraProxyExt) {
                  useNeqlCheckbox.setSelected(isUseNeqlSelected.getBoolean());
                  grid.add("useNeql", useNeqlCheckbox);
               }

               BConstrainedPane pane = new BConstrainedPane(grid);
               boolean validEntry = false;
               BValue newValue = null;

               while (!validEntry && 1 == BDialog.open(this.getOwner(), BTemplateConfigEditor.lex.getText("controlAppConfigEditor.setValue.label"), pane, 3)) {
                  try {
                     newValue = editor.saveValue().asValue();
                  } catch (Exception var21) {
                     BDialog.error(
                        BTemplateConfigEditor.this.owner,
                        BTemplateConfigEditor.uiLex.getText("plugin.saveProp.error", new Object[]{SlotPath.unescape(slot.name)})
                           + " "
                           + var21.getLocalizedMessage()
                     );
                     continue;
                  }

                  validEntry = true;
               }

               if (newValue != null && (!editingValue.equals(newValue) || isUseNeqlSelected.getBoolean() != useNeqlCheckbox.isSelected())) {
                  if (slot.value instanceof BUsernameAndPassword) {
                     BUsernameAndPassword usernameAndPassword = (BUsernameAndPassword)slot.value;
                     usernameAndPassword.setUsername(newValue.toString());
                  } else {
                     slot.value = newValue;
                     if (useNeqlCheckbox.isSelected()) {
                        slot.facets = BFacets.make(editorFacets, "useNeql", BBoolean.TRUE);
                     } else {
                        slot.facets = BFacets.makeRemove(editorFacets, "useNeql");
                     }
                  }

                  BTemplateConfigEditor.this.table.repaint();
                  BTemplateConfigEditor.this.view.templateModified();
               }
            }
         }

         return null;
      }
   }
}
