package com.tridium.template.ui;

import com.tridium.template.BRelationInfo;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.template.ui.tag.TagSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sync.Transaction;
import javax.baja.sys.BComponent;
import javax.baja.sys.BModule;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.RelationInfo;
import javax.baja.tag.TagDictionary;
import javax.baja.tag.TagDictionaryService;
import javax.baja.ui.BBorder;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BButtonStyle;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableHeaderRenderer;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.table.TableHeaderRenderer.Header;
import javax.baja.ui.tree.TreeNode;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BTemplateRelationEditor extends BEdgePane {
   public static final Type TYPE = Sys.loadType(BTemplateRelationEditor.class);
   private static Lexicon lex = Lexicon.make("template");
   private static BModule module = Sys.getModuleForClass(BTemplateRelationEditor.class);
   private static BImage inIcon = BImage.make("module://icons/x16/arrowLeft.png");
   private static BImage outIcon = BImage.make("module://icons/x16/arrowRight.png");
   BTemplateView view;
   private BComponent templateConfig;
   private BTable relationTable;
   private BTable selectTable;
   RelationInfo[] list;
   private BWidget owner;
   private BTemplateRelationEditor.Add add;
   private BTemplateRelationEditor.Reverse reverse;
   private BTemplateRelationEditor.Remove remove;

   public Type getType() {
      return TYPE;
   }

   public BTemplateRelationEditor() {
      throw new IllegalStateException();
   }

   public BTemplateRelationEditor(BTemplateView view, BTemplateConfig config) {
      this.owner = view.getShell();
      this.view = view;
      this.templateConfig = config;
      BTemplateRelationEditor.RelateInfoModel model = new BTemplateRelationEditor.RelateInfoModel();
      this.relationTable = new BTable(model);
      this.relationTable.setController(new BTemplateRelationEditor.RelateInfoController());
      this.relationTable.setSelection(new BTemplateRelationEditor.RelateInfoSelection());
      this.relationTable.setHeaderRenderer(new BTemplateRelationEditor.RelateInfoHeaderRenderer());
      this.relationTable.setCellRenderer(new BTemplateRelationEditor.RelateInfoRenderer());
      BTemplateRelationEditor.Model selectModel = new BTemplateRelationEditor.Model();
      this.selectTable = new BTable(selectModel);
      this.selectTable.setMultipleSelection(false);
      this.selectTable.setController(new BTemplateRelationEditor.Controller());
      this.selectTable.setSelection(new BTemplateRelationEditor.Selection());
      this.initRelationTable();
      int i = 0;

      for (RelationInfo relationInfo : this.getRelationInfos()) {
         selectModel.add(i++, relationInfo.getRelationId().getQName());
      }

      BSplitPane split = new BSplitPane();
      split.setDividerPosition(15.0);
      split.setWidget1(this.buildLeftPane());
      split.setWidget2(this.buildRightPane());
      this.setCenter(split);
   }

   private BWidget buildLeftPane() {
      this.add = new BTemplateRelationEditor.Add(this);
      this.add.setEnabled(true);
      BButton a = new BButton(this.add);
      a.setButtonStyle(BButtonStyle.toolBar);
      BGridPane top = new BGridPane(1);
      top.setHalign(BHalign.left);
      top.add(null, a);
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(top, 5.0, 0.0, 5.0, 0.0));
      pane.setCenter(new BBorderPane(this.selectTable, BBorder.none, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      return pane;
   }

   private BWidget buildRightPane() {
      this.reverse = new BTemplateRelationEditor.Reverse(this);
      this.remove = new BTemplateRelationEditor.Remove(this);
      this.reverse.setEnabled(false);
      this.remove.setEnabled(false);
      BButton a = new BButton(this.reverse);
      BButton c = new BButton(this.remove);
      a.setButtonStyle(BButtonStyle.toolBar);
      c.setButtonStyle(BButtonStyle.toolBar);
      BGridPane topLeft = new BGridPane(3);
      topLeft.add(null, a);
      topLeft.add(null, c);
      BGridPane topRight = new BGridPane(2);
      BEdgePane top = new BEdgePane();
      top.setLeft(topLeft);
      top.setRight(topRight);
      BEdgePane pane = new BEdgePane();
      pane.setTop(new BBorderPane(top, 5.0, 0.0, 5.0, 0.0));
      pane.setCenter(new BBorderPane(this.relationTable, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      return pane;
   }

   public boolean hasValidHints() {
      BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)this.relationTable.getModel();
      int rowCount = model.getRowCount();

      for (int i = 0; i < rowCount; i++) {
         BRelationInfo relationInfo = model.get(i);
         String predicateStatus = TagSupport.isNeqlPredicateValid(relationInfo.getRelateHints(), "RelateHints");
         if (!predicateStatus.equals("")) {
            return false;
         }
      }

      return true;
   }

   public void save() {
      this.templateConfig.lease(Integer.MAX_VALUE);
      Context tx = Transaction.start(this.templateConfig, null);
      Property[] properties = this.templateConfig.getPropertiesArray();

      for (Property property : properties) {
         if (this.templateConfig.get(property) instanceof BRelationInfo) {
            this.templateConfig.remove(property, tx);
         }
      }

      BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)this.relationTable.getModel();
      int rowCount = model.getRowCount();

      for (int i = 0; i < rowCount; i++) {
         BRelationInfo relationInfo = model.get(i);
         this.templateConfig.add("relate?", relationInfo, 5, tx);
      }

      try {
         Transaction.end(this.templateConfig, tx);
      } catch (Exception var7) {
         BDialog.error(this.getShell(), "Error", "createTemplateConfig failed.", var7);
      }
   }

   private void initRelationTable() {
      BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)this.relationTable.getModel();

      for (BRelationInfo relationInfo : (BRelationInfo[])this.templateConfig.getChildren(BRelationInfo.class)) {
         model.add((BRelationInfo)relationInfo.newCopy());
      }
   }

   private RelationInfo[] getRelationInfos() {
      if (this.list != null) {
         return this.list;
      } else {
         Collection<TagDictionary> dictionaries = null;
         TagDictionaryService tdService = null;

         try {
            tdService = this.templateConfig.getTagDictionaryService();
         } catch (Exception var8) {
         }

         if (tdService != null) {
            dictionaries = tdService.getTagDictionaries();
            if (dictionaries.size() != 0) {
               Array<RelationInfo> relList = new Array(RelationInfo.class);

               for (TagDictionary tagDictionary : tdService.getTagDictionaries()) {
                  Iterator<RelationInfo> relInfos = tagDictionary.getRelations();

                  while (relInfos.hasNext()) {
                     RelationInfo relInfo = relInfos.next();
                     relList.add(relInfo);
                  }
               }
            }
         } else {
            tdService = TmplUtil.makeTagDictionaryService();
            if (tdService != null) {
               dictionaries = tdService.getTagDictionaries();
            }
         }

         if (dictionaries != null && dictionaries.size() != 0) {
            Array<RelationInfo> relList = new Array(RelationInfo.class);

            for (TagDictionary tagDictionary : dictionaries) {
               Iterator<RelationInfo> relInfos = tagDictionary.getRelations();

               while (relInfos.hasNext()) {
                  RelationInfo relInfo = relInfos.next();
                  relList.add(relInfo);
               }
            }

            this.list = (RelationInfo[])relList.trim();
            return this.list;
         } else {
            throw new RuntimeException(lex.getText("relateCommand.noTagService.error.message"));
         }
      }
   }

   public class Add extends Command {
      public Add(BWidget owner) {
         super(owner, BTemplateRelationEditor.module, "controlAppConfigEditor.add");
      }

      public CommandArtifact doInvoke() {
         BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)BTemplateRelationEditor.this.relationTable.getModel();
         TableSelection tableSelection = BTemplateRelationEditor.this.selectTable.getSelection();
         TableModel selectModel = BTemplateRelationEditor.this.selectTable.getModel();
         int row = tableSelection.getRow();
         if (row < 0) {
            boolean noRelateId = true;

            while (noRelateId) {
               String relateId = BDialog.prompt(
                  BTemplateRelationEditor.this.owner, BTemplateRelationEditor.lex.getText("templateRelationEditor.addHoc.enterRelationId"), "", 32
               );
               if (relateId != null && !relateId.equals("")) {
                  relateId = relateId.trim();
                  String[] split = relateId.split(":", 0);
                  if (split.length != 2) {
                     BDialog.error(BTemplateRelationEditor.this.owner, BTemplateRelationEditor.lex.getText("templateRelationEditor.addHoc.invalidId"));
                  } else if (!model.contains(relateId, true)) {
                     model.add(relateId, true, "", "", "");
                     noRelateId = false;
                     BTemplateRelationEditor.this.view.templateModified();
                  } else if (!model.contains(relateId, false)) {
                     model.add(relateId, false, "", "", "");
                     noRelateId = false;
                     BTemplateRelationEditor.this.view.templateModified();
                  } else {
                     BDialog.error(BTemplateRelationEditor.this.owner, BTemplateRelationEditor.lex.getText("templateRelationEditor.addHoc.alreadyDefined"));
                  }
               } else {
                  noRelateId = false;
               }
            }
         } else if (row >= 0) {
            String relationId = (String)selectModel.getValueAt(row, 0);
            if (!model.contains(relationId, true)) {
               model.add(relationId, true, "", "", "");
            } else if (!model.contains(relationId, false)) {
               model.add(relationId, false, "", "", "");
            }

            tableSelection.updateTable();
            BTemplateRelationEditor.this.relationTable.getSelection().updateTable();
            BTemplateRelationEditor.this.view.templateModified();
         }

         return null;
      }
   }

   class Controller extends TableController {
      protected void doSelectAction(TreeNode target, double x, double y) {
         if (BTemplateRelationEditor.this.add.isEnabled()) {
            BTemplateRelationEditor.this.add.invoke();
         }
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         if (column == 0 && BTemplateRelationEditor.this.add.isEnabled()) {
            BTemplateRelationEditor.this.add.invoke();
         }
      }
   }

   class Model extends TableModel {
      HashMap<Integer, String> choices = new HashMap<>();

      public int getRowCount() {
         return this.choices.size();
      }

      public int getColumnCount() {
         return 1;
      }

      public String getColumnName(int col) {
         return "RelationId";
      }

      public String getValueAt(int row, int col) {
         return this.choices.get(row);
      }

      public void add(int row, String relationId) {
         if (!this.choices.containsValue(relationId)) {
            this.choices.put(row, relationId);
         }
      }

      public int getRowFor(String relationId) {
         if (!this.choices.containsValue(relationId)) {
            return -1;
         } else {
            int row = 0;

            for (String id : this.choices.values()) {
               if (id.equals(relationId)) {
                  return row;
               }

               row++;
            }

            return -1;
         }
      }
   }

   class RelateInfoController extends TableController {
      protected void checkSelection(BMouseEvent event, int row) {
         super.checkSelection(event, row);
         int selRow = BTemplateRelationEditor.this.relationTable.getSelection().getRow();
         if (selRow >= 0) {
            BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)BTemplateRelationEditor.this.relationTable.getModel();
            String relationId = model.get(selRow).getRelationId();
            TableSelection idSelection = BTemplateRelationEditor.this.selectTable.getSelection();
            BTemplateRelationEditor.Model idModel = (BTemplateRelationEditor.Model)BTemplateRelationEditor.this.selectTable.getModel();
            int idRow = idModel.getRowFor(relationId);
            idSelection.deselectAll();
            idSelection.select(idRow);
         }
      }

      public void keyPressed(BKeyEvent event) {
         super.keyPressed(event);
         if (event.getKeyCode() == 127 && BTemplateRelationEditor.this.remove.isEnabled()) {
            BTemplateRelationEditor.this.remove.invoke();
         }
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         if (column == 1 && BTemplateRelationEditor.this.reverse.isEnabled()) {
            BTemplateRelationEditor.this.reverse.invoke();
         }

         if (column == 2) {
            BRelationInfo value = (BRelationInfo)BTemplateRelationEditor.this.relationTable.getModel().getSubject(row);
            BString valueAt = BString.make(value.getRelateHints());
            BValue newValue = this.editCellValue(valueAt, BTemplateRelationEditor.lex.getText("templateRelationEditor.relateHints"));
            if (newValue == null) {
               return;
            }

            value.setRelateHints(newValue.toString());
            BTemplateRelationEditor.this.view.templateModified();
         }

         if (column == 3) {
            BRelationInfo value = (BRelationInfo)BTemplateRelationEditor.this.relationTable.getModel().getSubject(row);
            BString valueAt = BString.make(value.getUserTip());
            BValue newValue = this.editCellValue(valueAt, BTemplateRelationEditor.lex.getText("templateRelationEditor.userTip"));
            if (newValue == null) {
               return;
            }

            value.setUserTip(newValue.toString());
            BTemplateRelationEditor.this.view.templateModified();
         }

         if (column == 4) {
            BRelationInfo value = (BRelationInfo)BTemplateRelationEditor.this.relationTable.getModel().getSubject(row);
            BString valueAt = BString.make(value.getSlotPathScope());
            BValue newValue = this.editCellValue(valueAt, BTemplateRelationEditor.lex.getText("templateRelationEditor.slotPathScope"));
            if (newValue == null) {
               return;
            }

            value.setSlotPathScope(newValue.toString());
            BTemplateRelationEditor.this.view.templateModified();
         }

         BTemplateRelationEditor.this.relationTable.repaint();
      }

      private BValue editCellValue(BString value, String prompt) {
         BWbFieldEditor editor = BWbFieldEditor.makeFor(value);
         editor.loadValue(value);
         BGridPane grid = new BGridPane(1);
         grid.add(null, new BLabel(prompt));
         grid.add(null, editor);
         BBorderPane pane = new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0);
         if (1 != BDialog.open(BTemplateRelationEditor.this.view, BTemplateRelationEditor.lex.getText("controlAppConfigEditor.setValue.label"), pane, 3)) {
            return null;
         } else {
            BValue newValue = null;

            try {
               newValue = editor.saveValue().asValue();
            } catch (Exception var8) {
               BTemplateManager.log.log(Level.WARNING, BTemplateRelationEditor.lex.getText("templateRelationEditor.saveError"), (Throwable)var8);
            }

            return newValue;
         }
      }
   }

   class RelateInfoHeaderRenderer extends TableHeaderRenderer {
      public double getPreferredHeaderWidth(Header header) {
         double width = super.getPreferredHeaderWidth(header);
         return header.column == 1 && width < BTemplateConfigEditor.COL0_MIN_WIDTH ? BTemplateConfigEditor.COL0_MIN_WIDTH : width;
      }
   }

   public class RelateInfoModel extends TableModel {
      ArrayList<BRelationInfo> kids = new ArrayList<>();

      public int getRowCount() {
         return this.kids.size();
      }

      public int getColumnCount() {
         return 6;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BTemplateRelationEditor.lex.getText("templateRelationEditor.relationId");
            case 1:
               return BTemplateRelationEditor.lex.getText("templateRelationEditor.dir");
            case 2:
               return BTemplateRelationEditor.lex.getText("templateRelationEditor.relateHints");
            case 3:
               return BTemplateRelationEditor.lex.getText("templateRelationEditor.userTip");
            case 4:
               return BTemplateRelationEditor.lex.getText("templateRelationEditor.slotPathScope");
            default:
               return "";
         }
      }

      public Object getValueAt(int row, int col) {
         BRelationInfo relationInfo = this.kids.get(row);
         switch (col) {
            case 0:
               return relationInfo.getRelationId();
            case 1:
               return relationInfo.getInbound()
                  ? BTemplateRelationEditor.lex.getText("templateRelationEditor.in")
                  : BTemplateRelationEditor.lex.getText("templateRelationEditor.out");
            case 2:
               return relationInfo.getRelateHints();
            case 3:
               return relationInfo.getUserTip();
            case 4:
               return relationInfo.getSlotPathScope();
            case 5:
               return TagSupport.isNeqlPredicateValid(relationInfo.getRelateHints(), "RelateHints");
            default:
               return "";
         }
      }

      public Object getSubject(int row) {
         return this.kids.get(row);
      }

      public BImage getRowIcon(int row) {
         BRelationInfo relationInfo = this.kids.get(row);
         return relationInfo.getInbound() ? BTemplateRelationEditor.inIcon : BTemplateRelationEditor.outIcon;
      }

      public void add(BRelationInfo relationInfo) {
         this.kids.add(relationInfo);
      }

      public void add(String relationId, boolean inbound, String relateHints, String userTip, String slotPathScope) {
         BRelationInfo relationInfo = BRelationInfo.make(inbound, relationId, relateHints, userTip, slotPathScope);
         this.kids.add(relationInfo);
      }

      public void remove(int row) {
         this.kids.remove(row);
      }

      public BRelationInfo get(int row) {
         return row > this.kids.size() ? null : this.kids.get(row);
      }

      public boolean contains(String relationId, boolean inbound) {
         for (BRelationInfo kid : this.kids) {
            if (kid.getRelationId().equals(relationId) && kid.getInbound() == inbound) {
               return true;
            }
         }

         return false;
      }
   }

   class RelateInfoRenderer extends TableCellRenderer {
      public BBrush getForeground(Cell cell) {
         return cell.column == 5 ? BBrush.makeSolid(BColor.red) : super.getForeground(cell);
      }
   }

   class RelateInfoSelection extends TableSelection {
      public void updateTable() {
         super.updateTable();
         BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)this.getTable().getModel();
         int[] rows = this.getRows();
         BTemplateRelationEditor.this.remove.setEnabled(rows.length > 0);
         boolean revEnable = true;

         for (int i : rows) {
            BRelationInfo relationInfo = model.get(i);
            if (model.contains(relationInfo.getRelationId(), !relationInfo.getInbound())) {
               revEnable = false;
               break;
            }
         }

         BTemplateRelationEditor.this.reverse.setEnabled(revEnable);
      }
   }

   public class Remove extends Command {
      public Remove(BWidget owner) {
         super(owner, BTemplateRelationEditor.module, "controlAppConfigEditor.remove");
      }

      public CommandArtifact doInvoke() {
         BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)BTemplateRelationEditor.this.relationTable.getModel();
         int[] rows = model.getSelection().getRows();

         for (int i = 0; i < rows.length; i++) {
            model.remove(rows[i] - i);
         }

         BTemplateRelationEditor.this.relationTable.getSelection().deselectAll();
         BTemplateRelationEditor.this.relationTable.relayout();
         BTemplateRelationEditor.this.view.templateModified();
         return null;
      }
   }

   public class Reverse extends Command {
      public Reverse(BWidget owner) {
         super(owner, BTemplateRelationEditor.module, "controlAppConfigEditor.reverse");
      }

      public CommandArtifact doInvoke() {
         BTemplateRelationEditor.RelateInfoModel model = (BTemplateRelationEditor.RelateInfoModel)BTemplateRelationEditor.this.relationTable.getModel();
         int[] rows = BTemplateRelationEditor.this.relationTable.getSelection().getRows();

         for (int row : rows) {
            BRelationInfo relationInfo = (BRelationInfo)model.getSubject(row);
            relationInfo.setInbound(!relationInfo.getInbound());
         }

         BTemplateRelationEditor.this.relationTable.repaint();
         BTemplateRelationEditor.this.view.templateModified();
         return null;
      }
   }

   class Selection extends TableSelection {
      public void updateTable() {
         super.updateTable();
         BTemplateRelationEditor.RelateInfoModel relationTableModel = (BTemplateRelationEditor.RelateInfoModel)BTemplateRelationEditor.this.relationTable
            .getModel();
         BTemplateRelationEditor.Model selectModel = (BTemplateRelationEditor.Model)BTemplateRelationEditor.this.selectTable.getModel();
         int i = this.getRow();
         if (i < 0) {
            BTemplateRelationEditor.this.add.setEnabled(true);
         } else if (i >= 0) {
            String relationId = selectModel.getValueAt(i, 0);
            BTemplateRelationEditor.this.add.setEnabled(!relationTableModel.contains(relationId, true) || !relationTableModel.contains(relationId, false));
         }
      }
   }
}
