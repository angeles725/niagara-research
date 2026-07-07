package com.tridium.px.editor.sidebars.tree;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.commands.Delete;
import com.tridium.px.editor.commands.EditPropertiesContext;
import com.tridium.px.editor.commands.Insert;
import com.tridium.px.editor.commands.InsertDynamic;
import com.tridium.px.editor.commands.InsertFrozen;
import com.tridium.px.editor.commands.Rename;
import com.tridium.px.editor.studio.TreeStudio;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.studio.commands.MoveWidget;
import com.tridium.px.editor.studio.commands.PreferredSize;
import com.tridium.px.editor.util.EventUtil;
import com.tridium.px.editor.util.MenuBuilder;
import com.tridium.px.editor.util.Reflector;
import com.tridium.px.editor.util.SelectedWidgets;
import com.tridium.util.ClassUtil;
import java.util.ArrayList;
import java.util.List;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BDrawingTool;
import javax.baja.px.editor.BIPxTransferWidget;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.event.PxComponentEvent;
import javax.baja.px.editor.event.PxCompoundWidgetEvent;
import javax.baja.px.editor.event.PxEditorEvent;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxListener;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.px.editor.event.PxWidgetEvent;
import javax.baja.px.editor.factory.WidgetInserter;
import javax.baja.space.Mark;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.commands.CompoundCommand;
import javax.baja.ui.commands.CopyCommand;
import javax.baja.ui.commands.CutCommand;
import javax.baja.ui.commands.DeleteCommand;
import javax.baja.ui.commands.DuplicateCommand;
import javax.baja.ui.commands.PasteCommand;
import javax.baja.ui.event.BFocusEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BFlowPane;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.pane.BResponsivePane;
import javax.baja.ui.pane.BTabbedPane;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferEnvelope;
import javax.baja.ui.transfer.TransferFormat;
import javax.baja.ui.tree.BTree;
import javax.baja.ui.tree.TreeNode;
import javax.baja.ui.util.UiLexicon;

@NiagaraType
public class BPxTree extends BTree implements BIPxTransferWidget, PxListener {
   public static final Type TYPE = Sys.loadType(BPxTree.class);
   private BPxEditor editor;
   private BPxEditorPane editorPane;
   private SelectedWidgets selected;
   private TreeStudio studio;
   private Artisan artisan = Artisan.instance();
   private PxTreeModel model;
   private PxTreeSelection selModel;
   private boolean duplicating;
   private BWidget duplParent;
   private RectGeom[] duplBounds;
   private WidgetNode popupNode = null;

   public Type getType() {
      return TYPE;
   }

   public BPxTree(BPxEditor editor) {
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
      this.selected = this.editorPane.getSelectedWidgets();
      this.studio = this.editorPane.getTreeStudio();
      editor.addPxListener(this);
      this.setMultipleSelection(true);
      this.setModel(this.model = new PxTreeModel(this.editorPane));
      this.setSelection(this.selModel = new PxTreeSelection(this.editorPane, this.selected));
      this.setController(this.makeController());
   }

   @Override
   public void pxEvent(PxEvent event) {
      switch (event.getEventType()) {
         case 0:
            PxEditorEvent ee = (PxEditorEvent)event;
            switch (ee.getEventId()) {
               case 0:
                  this.model.buildRoot(this.editorPane.getRootContainer().getRoot());
                  return;
               case 1:
               default:
                  return;
               case 2:
                  if (((String)ee.getEventValue()).equals("preserveIdentities")) {
                     this.model.refreshNodeText();
                     this.relayout();
                  }

                  return;
               case 3:
                  this.updateSelection();
                  BDrawingTool tool = (BDrawingTool)ee.getEventValue();
                  this.setEnabled(tool.isNormal());
                  return;
            }
         case 1:
            switch (((PxPropertyEvent)event).getEventId()) {
               case 1:
               case 2:
                  this.model.refreshNodeText();
                  this.relayout();
                  return;
               default:
                  return;
            }
         case 2:
            this.updateSelection();
            break;
         case 3:
         case 4:
            switch (EventUtil.getEventType((PxComponentEvent)event)) {
               case 1:
               case 2:
               case 5:
               case 6:
                  this.getController().setFocus(null);
                  this.model.updateNodes(this.editorPane.getRootContainer().getRoot());
                  this.relayout();
                  this.updateSelection();
                  return;
               case 3:
               case 4:
               case 7:
               case 8:
               case 9:
                  this.model.refreshNodeText();
                  this.relayout();
                  return;
               default:
                  return;
            }
         case 5:
            PxCompoundWidgetEvent cw = (PxCompoundWidgetEvent)event;
            if (cw.getId() == 2 && this.valuesType(cw.getEvents()).is(BWidget.TYPE)) {
               this.getController().setFocus(null);
               this.model.updateNodes(this.editorPane.getRootContainer().getRoot());
               this.relayout();
               this.updateSelection();
            } else {
               this.model.refreshNodeText();
               this.relayout();
            }
            break;
         case 6:
            this.model.refreshNodeText();
            this.relayout();
      }
   }

   private Type valuesType(PxWidgetEvent[] events) {
      List<Type> arr = new ArrayList<>();

      for (int i = 0; i < events.length; i++) {
         BValue[] values = events[i].getValues();

         for (int j = 0; j < values.length; j++) {
            arr.add(values[j].getType());
         }
      }

      return ClassUtil.getCommonSuperType(arr.toArray(new Type[0]));
   }

   private void updateSelection() {
      this.selModel.superDeselectAll();
      int n = this.selected.size();

      for (int i = 0; i < n; i++) {
         BWidget widget = this.selected.get(i);
         WidgetNode node = this.model.getNode(widget);
         this.selModel.superSelect(node);
         if (i == n - 1) {
            this.expandToNode(node);
            this.scrollNodeToVisible(node);
         }
      }

      this.repaint();
   }

   public void focusGained(BFocusEvent event) {
      super.focusGained(event);
      this.editor.setTransferWidget(this);
   }

   public CommandArtifact doDuplicate() throws Exception {
      this.duplicating = true;
      return super.doDuplicate();
   }

   public TransferEnvelope getTransferData() throws Exception {
      if (this.duplicating) {
         this.duplParent = this.selected.get(0).getParentWidget();
         BWidget[] w = this.selected.getWidgets();
         this.duplBounds = new RectGeom[w.length];

         for (int i = 0; i < w.length; i++) {
            this.duplBounds[i] = this.artisan.bounds(w[i]);
         }
      }

      return this.selected.envelope();
   }

   public CommandArtifact removeTransferData(TransferContext cx) throws Exception {
      return this.doDelete();
   }

   public CommandArtifact doDelete() throws Exception {
      return this.selected.size() > 0 && this.editorPane.getLayerManager().allNormal(this.selected.getWidgets())
         ? new Delete(this.editorPane).doInvoke()
         : null;
   }

   public int dragOver(TransferContext cx) {
      if (!this.preventEdits() && this.editor.getController().allowDrop(this, cx)) {
         TreeNode node = this.yToTreeNode(cx.getY());
         return node == null ? 0 : ((WidgetNode)node).dragOver();
      } else {
         return 0;
      }
   }

   public CommandArtifact insertTransferData(TransferContext cx) throws Exception {
      BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
      if (objects.length > 1 && this.popupNode != null && this.popupNode.getWidget() instanceof BNullWidget) {
         objects = new BObject[]{objects[0]};
      }

      WidgetInserter ins = this.editor.getController().getWidgetInserter(this, objects);
      if (ins == null) {
         return null;
      } else if (ins.getWidgets() == null) {
         return null;
      } else if (ins.getWidgets().length == 0) {
         return null;
      } else {
         Insert insert = this.makeInsertCommand(ins.getWidgets());
         return this.makeInsertArtifact(insert, ins.getAuxillaryCommand());
      }
   }

   public CommandArtifact drop(TransferContext cx) throws Exception {
      if (!this.preventEdits() && this.editor.getController().allowDrop(this, cx)) {
         BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
         TreeNode tnode = this.yToTreeNode(cx.getY());
         if (tnode == null) {
            return null;
         } else {
            WidgetNode node = (WidgetNode)tnode;
            if (objects.length > 1 && node.getWidget() instanceof BNullWidget) {
               objects = new BObject[]{objects[0]};
            }

            WidgetInserter ins = this.editor.getController().getWidgetInserter(this, objects);
            if (ins == null) {
               return null;
            } else if (ins.getWidgets() == null) {
               return null;
            } else if (ins.getWidgets().length == 0) {
               return null;
            } else {
               Insert insert = this.makeDropCommand(node, ins.getWidgets(), ins.getColumnCount());
               return this.makeInsertArtifact(insert, ins.getAuxillaryCommand());
            }
         }
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   public BMenu getDefaultPopupMenu(BMouseEvent event) {
      this.editorPane.updateToolbar();
      this.editorPane.repaint();
      BCanvasPane canvas = this.studio.getCurrentCanvas();
      BPane freeForm = this.studio.getCurrentFreeForm();
      BMenu menu = new BMenu();
      BSubMenuItem newSubMenu = new BSubMenuItem(UiLexicon.bajaui().getText("menu.new.label"));
      CutCommand cut = new CutCommand(this);
      CopyCommand copy = new CopyCommand(this);
      PasteCommand paste = new PasteCommand(this);
      DuplicateCommand duplicate = new DuplicateCommand(this);
      DeleteCommand del = new DeleteCommand(this);
      EditPropertiesContext edit = new EditPropertiesContext(this.editor);
      PreferredSize prefSize = new PreferredSize(this.editorPane, freeForm);
      ExpandAll expandAll = new ExpandAll(this.editorPane, this, this.popupNode);
      if (this.popupNode.getWidget() instanceof BNullWidget) {
         cut.setEnabled(false);
         copy.setEnabled(false);
         duplicate.setEnabled(false);
         del.setEnabled(false);
         edit.setEnabled(false);
         prefSize.setEnabled(false);
         expandAll.setEnabled(false);
      } else {
         BWidget parent = this.popupNode.getWidget().getParentWidget();
         if (!Reflector.isFreeFormPane(parent)) {
            duplicate.setEnabled(false);
         }

         if (!Reflector.isFreeFormPane(this.popupNode.getWidget())) {
            paste.setEnabled(false);
            newSubMenu.setEnabled(false);
         }
      }

      boolean readOnly = this.preventEdits();
      if (cut.isEnabled()) {
         cut.setEnabled(!readOnly);
      }

      if (paste.isEnabled()) {
         paste.setEnabled(!readOnly);
      }

      if (newSubMenu.isEnabled()) {
         newSubMenu.setEnabled(!readOnly);
      }

      if (duplicate.isEnabled()) {
         duplicate.setEnabled(!readOnly);
      }

      if (del.isEnabled()) {
         del.setEnabled(!readOnly);
      }

      if (edit.isEnabled()) {
         edit.setEnabled(!readOnly);
      }

      if (prefSize.isEnabled()) {
         prefSize.setEnabled(!readOnly);
      }

      if (expandAll.isEnabled()) {
         expandAll.setEnabled(!readOnly);
      }

      if (newSubMenu.isEnabled()) {
         BMenu newMenu = MenuBuilder.newMenu(this.editorPane, this);
         if (newMenu == null) {
            newSubMenu.setEnabled(false);
         } else {
            newSubMenu.setMenu(newMenu);
         }
      }

      BSubMenuItem align = new BSubMenuItem(BPxEditorPane.text("commands.align"), MenuBuilder.alignMenu(this.editorPane, this.studio.alignable(canvas)));
      BSubMenuItem distribute = new BSubMenuItem(
         BPxEditorPane.text("commands.distribute"), MenuBuilder.distributeMenu(this.editorPane, this.studio.distributable(canvas))
      );
      BSubMenuItem reorg = new BSubMenuItem(BPxEditorPane.text("commands.reorder"), MenuBuilder.reorgMenu(this.editorPane, this.studio.reorgable(freeForm)));
      BSubMenuItem border = new BSubMenuItem(
         BPxEditorPane.text("commands.border"), MenuBuilder.borderMenu(this.editorPane, this.editorPane.getSelectedWidgets())
      );
      BSubMenuItem responsive = null;
      if (this.popupNode.getWidget().getParentWidget() instanceof BFlowPane || this.popupNode.getWidget().getParentWidget() instanceof BResponsivePane) {
         responsive = new BSubMenuItem(
            BPxEditorPane.text("commands.responsive"), MenuBuilder.responsiveMenu(this.editorPane, this.editorPane.getSelectedWidgets())
         );
      }

      menu.add(null, newSubMenu);
      menu.add(null, new BSeparator());
      menu.add(null, cut);
      menu.add(null, copy);
      menu.add(null, paste);
      menu.add(null, duplicate);
      menu.add(null, del);
      menu.add(null, new BSeparator());
      menu.add(null, edit);
      menu.add(null, align);
      menu.add(null, distribute);
      menu.add(null, reorg);
      menu.add(null, border);
      if (responsive != null) {
         menu.add(null, responsive);
      }

      menu.add(null, prefSize);
      menu.add(null, expandAll);
      if (this.editorPane.getOptions().getPreserveIdentities()) {
         Rename rename = new Rename(this.editorPane);
         if (this.selected.size() != 1 || !Reflector.isFreeFormPane(this.selected.get(0).getParentWidget())) {
            rename.setEnabled(false);
         }

         if (rename.isEnabled()) {
            rename.setEnabled(!this.preventEdits());
         }

         menu.add(null, rename);
      }

      return menu;
   }

   private CommandArtifact makeInsertArtifact(Insert insert, Command auxCommand) throws Exception {
      if (insert == null) {
         return null;
      } else if (!insert.checkMedia()) {
         return null;
      } else {
         return auxCommand == null ? insert.doInvoke() : new CompoundCommand(this.editor, "compound", new Command[]{insert, auxCommand}).doInvoke();
      }
   }

   private Insert makeInsertCommand(BWidget[] widgets) {
      if (this.selected.get(0) instanceof BNullWidget) {
         if (widgets.length != 1) {
            throw new IllegalStateException();
         } else if (this.duplicating) {
            throw new IllegalStateException();
         } else {
            this.popupNode = null;
            this.artisan.zero(widgets[0]);
            return new InsertFrozen(this.editor, this.editorPane, this.selected.get(0).getParentWidget(), widgets[0], this.selected.get(0).getName());
         }
      } else {
         BWidget parent = this.duplicating ? this.duplParent : this.selected.get(0);
         if (!Reflector.isFreeFormPane(parent)) {
            throw new IllegalStateException();
         } else {
            if (parent instanceof BCanvasPane) {
               if (this.duplicating) {
                  MoveWidget.toZero(widgets, this.artisan);

                  for (int i = 0; i < widgets.length; i++) {
                     this.artisan.move(widgets[i], this.duplBounds[i].x + 10.0, this.duplBounds[i].y + 10.0);
                  }
               } else {
                  RectGeom bnd = this.artisan.bounds(widgets[0]);

                  for (int i = 1; i < widgets.length; i++) {
                     bnd = RectGeom.bounds(this.artisan.bounds(widgets[i]), bnd, bnd);
                  }

                  MoveWidget.shiftWidgets(widgets, this.artisan, -bnd.x, -bnd.y);
               }
            } else if (parent instanceof BTabbedPane) {
               MoveWidget.toZero(widgets, this.artisan);
               widgets = this.editorPane.getTreeStudio().makeTabs(widgets);
            } else {
               MoveWidget.toZero(widgets, this.artisan);
            }

            this.duplicating = false;
            this.popupNode = null;
            return new InsertDynamic(this.editor, this.editorPane, parent, widgets);
         }
      }
   }

   private Insert makeDropCommand(WidgetNode node, BWidget[] widgets, int columnCount) {
      if (node.getWidget() instanceof BNullWidget) {
         if (widgets.length != 1) {
            throw new IllegalStateException();
         } else {
            return new InsertFrozen(this.editor, this.editorPane, node.getWidget().getParentWidget(), widgets[0], node.getText());
         }
      } else if (node instanceof PaneNode) {
         BWidget parent = node.getWidget();
         if (!Reflector.isFreeFormPane(parent)) {
            throw new IllegalStateException();
         } else {
            if (parent instanceof BCanvasPane) {
               this.editorPane.getTreeStudio().dropWidgetsInCanvas(0.0, 0.0, widgets, columnCount);
            } else if (parent instanceof BTabbedPane) {
               widgets = this.editorPane.getTreeStudio().makeTabs(widgets);
            }

            return new InsertDynamic(this.editor, this.editorPane, parent, widgets);
         }
      } else {
         throw new IllegalStateException();
      }
   }

   private boolean preventEdits() {
      return this.editor.isReadonly() || !this.editorPane.getLayerManager().allNormal(this.selected.getWidgets());
   }

   protected PxTreeController makeController() {
      return new PxTreeController(this.editor, this.editorPane, this, this.selected);
   }

   void setPopupNode(WidgetNode node) {
      this.popupNode = node;
   }
}
