package com.tridium.template.ui.tag;

import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.ui.theme.Theme;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Tag;
import javax.baja.ui.BButton;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BTextField;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BFlowPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableHeaderRenderer;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.table.TableHeaderRenderer.Header;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraAction(
   name = "selectTemplate"
)
public class BTemplateTagChooser extends BEdgePane {
   public static final Action selectTemplate = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BTemplateTagChooser.class);
   private Map<BWbDeployableNtplFile, HashSet<Tag>> tagMap;
   private BTemplateTagChooser.TemplateTagTableModel completeTableModel;
   private BTemplateTagChooser.TemplateTagTableModel selectedTableModel;
   private BTemplateTagChooser.TemplateResultsTableModel resultsTableModel;
   private BListDropDown templateList;
   private BTextField defaultTagId;
   private BTextField defaultValue;
   private static final Lexicon lex = Lexicon.make("template");
   private static BImage redBall = BImage.make("module://icons/x16/shapes/circleRed.png");
   private static BImage greenBall = BImage.make("module://icons/x16/shapes/circleGreen.png");
   private final String LEX_SELECTED = lex.getText("bulkDeploy.excelExport.columnSelect");
   private final String LEX_TEMPLATE = lex.getText("bulkDeploy.excelExport.columnFile");
   private final String LEX_TAG = lex.getText("bulkDeploy.excelExport.columnTag");
   private final String LEX_DEFAULT = lex.getText("bulkDeploy.excelExport.columnDefault");
   private static final int SELECTED_COL = 0;
   private static final int TEMPLATE_COL = 1;
   private static final int TAG_COL = 2;
   private static final int DEFAULT_COL = 3;
   private static final int VISIBLE_ROWS = 5;

   public void selectTemplate() {
      this.invoke(selectTemplate, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BTemplateTagChooser(Map<BWbDeployableNtplFile, HashSet<Tag>> tagMap) {
      if (tagMap != null && !tagMap.isEmpty()) {
         this.tagMap = tagMap;
         this.completeTableModel = new BTemplateTagChooser.TemplateTagTableModel();
         this.completeTableModel.loadFromTagMap();
         this.selectedTableModel = new BTemplateTagChooser.TemplateTagTableModel();
         this.resultsTableModel = new BTemplateTagChooser.TemplateResultsTableModel();
         this.templateList = new BListDropDown();
         tagMap.keySet().forEach(templateFile -> this.templateList.getList().addItem(templateFile.getTitle()));
         this.linkTo(this.templateList, BListDropDown.valueModified, selectTemplate);
         BLabel helpLabel = new BLabel(lex.getText("bulkDeploy.excelExport.selectTemplatesHelp"));
         helpLabel.setHalign(BHalign.left);
         this.setTop(helpLabel);
         BInsets defaultInsets = BInsets.make(7.0);
         BFlowPane leftPane = new BFlowPane();
         leftPane.add(null, this.templateList);
         this.setLeft(new BBorderPane(leftPane, defaultInsets));
         if (this.templateList.getList().getItemCount() > 0) {
            this.templateList.setSelectedIndex(0);
         }

         BTemplateTagChooser.TemplateSelectTable selectTable = new BTemplateTagChooser.TemplateSelectTable();
         BConstrainedPane selectPane = new BConstrainedPane(selectTable);
         selectPane.setMinHeight(selectTable.getCellRenderer().getCellHeight() * 5.0);
         selectPane.setMaxHeight(selectTable.getCellRenderer().getCellHeight() * 5.0);
         this.setCenter(new BBorderPane(selectPane, defaultInsets));
         BTemplateTagChooser.TemplateResultsTable resultsTable = new BTemplateTagChooser.TemplateResultsTable();
         BConstrainedPane resultsTablePane = new BConstrainedPane(resultsTable);
         resultsTablePane.setMinHeight(resultsTable.getCellRenderer().getCellHeight() * 5.0);
         resultsTablePane.setMaxHeight(resultsTable.getCellRenderer().getCellHeight() * 5.0);
         BGridPane resultsPane = new BGridPane(1);
         resultsPane.add(null, new BBorderPane(resultsTablePane, defaultInsets));
         BFlowPane defaultsPane = new BFlowPane();
         defaultsPane.add(null, new BLabel(lex.getText("bulkDeploy.excelExport.tagIdLabel")));
         this.defaultTagId = new BTextField("", 16, false);
         defaultsPane.add(null, this.defaultTagId);
         defaultsPane.add(null, new BLabel(lex.getText("bulkDeploy.excelExport.defaultLabel")));
         this.defaultValue = new BTextField("", 16, true);
         defaultsPane.add(null, this.defaultValue);
         BTemplateTagChooser.SetDefault setDefaultCommand = new BTemplateTagChooser.SetDefault();
         defaultsPane.add(null, new BButton(setDefaultCommand, true, false));
         resultsPane.add(null, defaultsPane);
         this.setBottom(new BBorderPane(resultsPane, defaultInsets));
      }
   }

   public void doSelectTemplate() {
      String item = (String)this.templateList.getSelectedItem();
      this.selectedTableModel.setSelectedTemplate(item);
      this.selectedTableModel.updateTable(true);
   }

   public Map<BWbDeployableNtplFile, HashSet<Tag>> getResult() {
      if (this.resultsTableModel == null) {
         return null;
      } else {
         Map<BWbDeployableNtplFile, HashSet<Tag>> tagResults = new HashMap<>();

         for (int i = 0; i < this.resultsTableModel.getRowCount(); i++) {
            boolean isSelected = (Boolean)this.resultsTableModel.getValueAt(i, 0);
            if (isSelected) {
               BWbDeployableNtplFile templateFile = (BWbDeployableNtplFile)this.resultsTableModel.getValueAt(i, 1);
               String tagName = (String)this.resultsTableModel.getValueAt(i, 2);
               String defaultValue = (String)this.resultsTableModel.getValueAt(i, 3);
               HashSet<Tag> selectedTemplateTags;
               if (!tagResults.containsKey(templateFile)) {
                  selectedTemplateTags = new HashSet<>();
                  tagResults.put(templateFile, selectedTemplateTags);
               } else {
                  selectedTemplateTags = tagResults.get(templateFile);
               }

               selectedTemplateTags.add(Tag.newTag(tagName, defaultValue));
            }
         }

         return tagResults;
      }
   }

   private String getTagValue(Tag tag) {
      try {
         return tag.getValue().encodeToString();
      } catch (IOException var3) {
         return "";
      }
   }

   public class SetDefault extends Command {
      public SetDefault() {
         super(BTemplateTagChooser.this, BTemplateTagChooser.lex.getText("bulkDeploy.excelExport.setDefaultValue"));
      }

      public CommandArtifact doInvoke() throws Exception {
         if (BTemplateTagChooser.this.defaultTagId.getText() == null || BTemplateTagChooser.this.defaultTagId.getText().length() == 0) {
            return null;
         } else if (BTemplateTagChooser.this.resultsTableModel.getSelection() != null
            && BTemplateTagChooser.this.resultsTableModel.getSelection().getRow() >= 0) {
            String newValue = BTemplateTagChooser.this.defaultValue.getText();
            BTemplateTagChooser.TemplateTagRow tableRow = BTemplateTagChooser.this.resultsTableModel
               .getRow(BTemplateTagChooser.this.resultsTableModel.getSelection().getRow());
            tableRow.setDefaultValue(newValue);
            BTemplateTagChooser.this.resultsTableModel.updateTable(true);
            return null;
         } else {
            return null;
         }
      }
   }

   class TemplateCellRenderer extends TableCellRenderer {
      public String getCellText(Cell cell) {
         BTemplateTagChooser.TemplateTagTableModel model = (BTemplateTagChooser.TemplateTagTableModel)this.getModel();
         BTemplateTagChooser.TemplateTagRow row = model.getRow(cell.row);
         switch (cell.column) {
            case 0:
               if (row.isSelected()) {
                  return "true";
               }

               return "false";
            case 1:
               return row.getTemplateFile().getTitle();
            case 2:
               return row.getTagName();
            case 3:
               return row.getDefaultValue();
            default:
               return "";
         }
      }
   }

   class TemplateHeaderRenderer extends TableHeaderRenderer {
      public double getPreferredHeaderWidth(Header header) {
         if (header.column == 1) {
            double sw = Theme.table().getHeaderFont().width(header.name);

            for (int i = 0; i < BTemplateTagChooser.this.templateList.getList().getItemCount(); i++) {
               String item = (String)BTemplateTagChooser.this.templateList.getList().getItem(i);
               double itemWidth = Theme.table().getHeaderFont().width(item);
               if (itemWidth > sw) {
                  sw = itemWidth;
               }
            }

            return sw + 6.0;
         } else {
            return super.getPreferredHeaderWidth(header);
         }
      }
   }

   class TemplateResultsController extends TableController {
      public void cellPulsed(BMouseEvent event, int row, int col) {
         super.cellDoubleClicked(event, row, col);
         BTemplateTagChooser.TemplateTagRow selectedRow = ((BTemplateTagChooser.TemplateResultsTableModel)this.getModel()).getRow(row);
         String selectedTagName = selectedRow.getTagName();
         String selectedDefaultValue = selectedRow.getDefaultValue();
         BTemplateTagChooser.this.defaultTagId.setText(selectedTagName);
         BTemplateTagChooser.this.defaultValue.setText(selectedDefaultValue);
      }
   }

   class TemplateResultsTable extends BTable {
      public TemplateResultsTable() {
         this.setModel(BTemplateTagChooser.this.resultsTableModel);
         this.setController(BTemplateTagChooser.this.new TemplateResultsController());
         this.setVscrollBarVisible(true);
         this.setCellRenderer(BTemplateTagChooser.this.new TemplateCellRenderer());
         this.setHeaderRenderer(BTemplateTagChooser.this.new TemplateHeaderRenderer());
      }
   }

   class TemplateResultsTableModel extends BTemplateTagChooser.TemplateTagTableModel {
      private TemplateResultsTableModel() {
      }

      public void addResultsRow(BTemplateTagChooser.TemplateTagRow addRow) {
         this.rows.add(addRow);
      }

      public void removeResultsRow(BTemplateTagChooser.TemplateTagRow removeRow) {
         this.rows.remove(removeRow);
      }

      @Override
      public int getColumnCount() {
         return 4;
      }

      @Override
      public Object getValueAt(int row, int col) {
         switch (col) {
            case 0:
               return this.rows.get(row).isSelected();
            case 1:
               return this.rows.get(row).getTemplateFile();
            case 2:
               return this.rows.get(row).getTagName();
            case 3:
               return this.rows.get(row).getDefaultValue();
            default:
               return null;
         }
      }

      @Override
      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BTemplateTagChooser.this.LEX_SELECTED;
            case 1:
               return BTemplateTagChooser.this.LEX_TEMPLATE;
            case 2:
               return BTemplateTagChooser.this.LEX_TAG;
            case 3:
               return BTemplateTagChooser.this.LEX_DEFAULT;
            default:
               return "?";
         }
      }
   }

   class TemplateSelectController extends TableController {
      public void cellPulsed(BMouseEvent event, int row, int col) {
         super.cellDoubleClicked(event, row, col);
         BTemplateTagChooser.TemplateTagRow selectedRow = ((BTemplateTagChooser.TemplateTagTableModel)this.getModel()).getRow(row);
         BWbDeployableNtplFile selectedFile = selectedRow.getTemplateFile();
         boolean selected = selectedRow.isSelected();
         if (!selected) {
            selectedRow.setSelected(true);
         } else {
            selectedRow.setSelected(false);
         }

         this.getModel().updateTable(true);

         for (int i = 0; i < BTemplateTagChooser.this.completeTableModel.getRowCount(); i++) {
            BTemplateTagChooser.TemplateTagRow tableRow = BTemplateTagChooser.this.completeTableModel.getRow(i);
            BWbDeployableNtplFile tableFile = tableRow.getTemplateFile();
            String tableTagName = tableRow.getTagName();
            if (tableFile.getTitle().contentEquals(selectedFile.getTitle()) && tableTagName.equals(selectedRow.getTagName())) {
               tableRow.setSelected(!selected);
               break;
            }
         }

         BTemplateTagChooser.this.completeTableModel.updateTable(true);
         boolean resultRowFound = false;

         for (int ix = 0; ix < BTemplateTagChooser.this.resultsTableModel.getRowCount(); ix++) {
            BTemplateTagChooser.TemplateTagRow tableRow = BTemplateTagChooser.this.resultsTableModel.getRow(ix);
            BWbDeployableNtplFile tableFile = tableRow.getTemplateFile();
            String tableTagName = tableRow.getTagName();
            if (tableFile.getTitle().contentEquals(selectedFile.getTitle()) && tableTagName.equals(selectedRow.getTagName())) {
               resultRowFound = true;
               if (selected) {
                  BTemplateTagChooser.this.resultsTableModel.removeResultsRow(selectedRow);
               }

               BTemplateTagChooser.this.defaultTagId.setText("");
               BTemplateTagChooser.this.defaultValue.setText("");
               break;
            }
         }

         if (!resultRowFound && !selected) {
            BTemplateTagChooser.this.resultsTableModel.addResultsRow(selectedRow);
            BTemplateTagChooser.this.defaultTagId.setText("");
            BTemplateTagChooser.this.defaultValue.setText("");
         }

         BTemplateTagChooser.this.resultsTableModel.updateTable(true);
      }
   }

   class TemplateSelectTable extends BTable {
      public TemplateSelectTable() {
         this.setModel(BTemplateTagChooser.this.selectedTableModel);
         this.setController(BTemplateTagChooser.this.new TemplateSelectController());
         this.setVscrollBarVisible(true);
         this.setCellRenderer(BTemplateTagChooser.this.new TemplateCellRenderer());
         this.setHeaderRenderer(BTemplateTagChooser.this.new TemplateHeaderRenderer());
      }
   }

   class TemplateTagRow {
      private boolean selected = false;
      private BWbDeployableNtplFile templateFile;
      private String qName;
      private String defaultValue;

      public TemplateTagRow(BWbDeployableNtplFile templateFile, Tag tag) {
         this.templateFile = templateFile;
         this.qName = tag.getId().getQName();
         this.defaultValue = "";
      }

      public void setDefaultValue(Tag tag) {
         String tagValue = BTemplateTagChooser.this.getTagValue(tag);
         this.setDefaultValue(tagValue);
      }

      public void setDefaultValue(String tagValue) {
         this.defaultValue = tagValue;
      }

      public String getDefaultValue() {
         return this.defaultValue;
      }

      public boolean isSelected() {
         return this.selected;
      }

      public BWbDeployableNtplFile getTemplateFile() {
         return this.templateFile;
      }

      public String getTagName() {
         return this.qName;
      }

      public void setSelected(boolean selected) {
         this.selected = selected;
      }
   }

   class TemplateTagTableModel extends TableModel {
      ArrayList<BTemplateTagChooser.TemplateTagRow> rows = new ArrayList<>();

      protected TemplateTagTableModel() {
      }

      public void loadFromTagMap() {
         BTemplateTagChooser.this.tagMap.forEach((templateFile, tags) -> {
            if (!tags.isEmpty()) {
               tags.forEach(tag -> {
                  BTemplateTagChooser.TemplateTagRow row = this.getRow(templateFile, tag);
                  if (row == null) {
                     this.rows.add(BTemplateTagChooser.this.new TemplateTagRow(templateFile, tag));
                  }
               });
            }
         });
      }

      public void setSelectedTemplate(String selectedTitle) {
         this.rows.clear();

         for (int i = 0; i < BTemplateTagChooser.this.completeTableModel.getRowCount(); i++) {
            BTemplateTagChooser.TemplateTagRow tableRow = BTemplateTagChooser.this.completeTableModel.getRow(i);
            BWbDeployableNtplFile tableFile = tableRow.getTemplateFile();
            if (tableFile.getTitle().contentEquals(selectedTitle)) {
               this.rows.add(tableRow);
            }
         }
      }

      public int getRowCount() {
         return this.rows.size();
      }

      public int getColumnCount() {
         return 3;
      }

      public Object getValueAt(int row, int col) {
         switch (col) {
            case 0:
               return this.rows.get(row).isSelected();
            case 1:
               return this.rows.get(row).getTemplateFile();
            case 2:
               return this.rows.get(row).getTagName();
            default:
               return null;
         }
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BTemplateTagChooser.this.LEX_SELECTED;
            case 1:
               return BTemplateTagChooser.this.LEX_TEMPLATE;
            case 2:
               return BTemplateTagChooser.this.LEX_TAG;
            default:
               return "?";
         }
      }

      public BTemplateTagChooser.TemplateTagRow getRow(int row) {
         return row >= this.rows.size() ? null : this.rows.get(row);
      }

      public BTemplateTagChooser.TemplateTagRow getRow(BWbDeployableNtplFile file, Tag tag) {
         String tagName = tag.getId().getQName();
         return this.getRow(file, tagName);
      }

      public BTemplateTagChooser.TemplateTagRow getRow(BWbDeployableNtplFile file, String tagName) {
         if (this.rows.size() == 0) {
            return null;
         } else {
            AtomicInteger tagIndex = new AtomicInteger(-1);
            this.rows.forEach(row -> {
               if (row.templateFile.getTitle().contentEquals(file.getTitle()) && row.qName.contentEquals(tagName)) {
                  tagIndex.set(this.rows.indexOf(row));
               }
            });
            return tagIndex.get() >= 0 ? this.rows.get(tagIndex.get()) : null;
         }
      }

      public synchronized BImage getRowIcon(int row) {
         return this.getRow(row).isSelected() ? BTemplateTagChooser.greenBall : BTemplateTagChooser.redBall;
      }

      public boolean containsTag(Tag tag) {
         if (this.rows.size() == 0) {
            return false;
         } else {
            String tagName = tag.getId().getQName();
            AtomicBoolean tagExists = new AtomicBoolean(false);
            this.rows.forEach(row -> {
               if (row.qName.contentEquals(tagName)) {
                  tagExists.set(true);
               }
            });
            return tagExists.get();
         }
      }
   }
}
