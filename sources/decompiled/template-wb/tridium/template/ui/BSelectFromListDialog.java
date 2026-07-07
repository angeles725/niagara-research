package com.tridium.template.ui;

import com.tridium.workbench.shell.BFontSize;
import com.tridium.workbench.shell.BGeneralOptions;
import java.util.ArrayList;
import java.util.Collections;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.gx.IRectGeom;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.WidgetSubscriber;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BFlowPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.util.BTitlePane;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "save"
   ), @NiagaraAction(
      name = "cancel"
   ), @NiagaraAction(
      name = "updateCommands"
   )})
public class BSelectFromListDialog extends BDialog {
   public static final Action save = newAction(0, null);
   public static final Action cancel = newAction(0, null);
   public static final Action updateCommands = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BSelectFromListDialog.class);
   private static Lexicon lex = Lexicon.make("template");
   protected ArrayList choiceList;
   private static BImage searchIcon = BImage.make("module://icons/x16/find.png");
   private static BImage infoIcon = BImage.make("module://icons/x16/info.png");
   protected BFlowPane header;
   private BTextField searchEntryField;
   protected String searchEntry;
   protected BTable choicesTable;
   protected BSelectFromListDialog.SelectModel tableModel;
   private BSelectFromListDialog.FilterSubscriber subscriber;
   private static IRectGeom lastBounds;
   private static String lastSearch;
   private int results = 0;
   protected BButton btnSave;
   protected BButton btnCancel;
   protected boolean isInput;
   protected boolean isRelation;
   protected boolean allowMultiple;
   protected boolean isDeploy = true;
   protected String info;
   private BCheckBox rememberCB;
   private BCheckBox dontAskAgainCB;
   protected static boolean rememberSelected = true;
   protected static boolean dontAskAgainSelected = false;

   public void save() {
      this.invoke(save, null, null);
   }

   public void cancel() {
      this.invoke(cancel, null, null);
   }

   public void updateCommands() {
      this.invoke(updateCommands, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BSelectFromListDialog() {
   }

   public ArrayList openDialog() {
      if (lastBounds == null) {
         this.setBoundsCenteredOnOwner();
         lastBounds = this.getScreenBounds();
      }

      double width = lastBounds.width() < 500.0 ? 500.0 : lastBounds.width();
      double height = lastBounds.height() < 300.0 ? 300.0 : lastBounds.height();
      this.setScreenBounds(lastBounds.x(), lastBounds.y(), width, height);
      if (lastSearch != null) {
         this.setSearchEntry(lastSearch);
      }

      int confirm;
      do {
         this.open();
         if (this.getResult() == 1) {
            return this.tableModel.getChoices();
         }

         if (!this.isDeploy) {
            break;
         }

         confirm = BDialog.confirm(this.getOwner(), lex.getText("templateManager.confirm.abort.deploy"));
      } while (confirm != 4);

      return null;
   }

   public BSelectFromListDialog(
      BWidget parent,
      String title,
      String subtitle,
      String info,
      boolean isInput,
      boolean isRelation,
      boolean allowMultiple,
      ArrayList list,
      boolean allowRemember,
      boolean showDontAsk
   ) {
      super(parent, title, true);
      this.choiceList = this.makeChoiceArray(list);
      this.isInput = isInput;
      this.isRelation = isRelation;
      this.allowMultiple = allowMultiple;
      this.info = info;
      if (parent instanceof BTemplateManager) {
         this.isDeploy = false;
         ((BTemplateManager)parent).setIsDeploy(false);
      }

      BEdgePane rootPane = new BEdgePane();
      BEdgePane headerPane = new BEdgePane();
      BFlowPane infoPane = null;
      if (info != null && !info.isEmpty()) {
         infoPane = new BFlowPane();
         infoPane.add(null, new BLabel(infoIcon, ""));
         infoPane.add(null, new BLabel(info));
      }

      this.header = new BFlowPane();
      this.subscriber = new BSelectFromListDialog.FilterSubscriber(this);
      if (this.showSearch()) {
         this.searchEntryField = new BSelectFromListDialog.BSearchTextField();
         this.searchEntryField.setVisibleColumns(10);
         this.searchEntry = "";
         this.header.add(null, new BLabel(searchIcon, ""));
         this.header.add(null, this.searchEntryField);
         this.subscriber.subscribe(this.searchEntryField);
      }

      this.fillInHeader(this.header);
      if (infoPane != null) {
         headerPane.setTop(infoPane);
      }

      headerPane.setCenter(this.header);
      boolean isLargeFont = BGeneralOptions.make().getFontSize() == BFontSize.large;
      BInsets headerInsets = isLargeFont ? BInsets.make(5.0, 5.0, 5.0, 5.0) : BInsets.make(5.0, 5.0, 10.0, 5.0);
      rootPane.setTop(new BBorderPane(headerPane, headerInsets));
      this.tableModel = this.makeModel(this.choiceList);
      this.choicesTable = this.makeTable(this.tableModel);
      this.choicesTable.setController(this.makeController());
      this.initHeader();
      BTitlePane choicePane = BTitlePane.makePane(subtitle, this.choicesTable);
      rootPane.setCenter(choicePane);
      rootPane.setBottom(this.buildButtonPane());
      this.subscriber.subscribe(this.rememberCB);
      this.subscriber.subscribe(this.dontAskAgainCB);
      this.rememberCB.setVisible(allowRemember);
      this.rememberCB.setSelected(rememberSelected);
      this.dontAskAgainCB.setVisible(showDontAsk);
      this.setContent(rootPane);
   }

   public void setIsDeploy(boolean isDeploy) {
      this.isDeploy = isDeploy;
   }

   protected BSelectFromListDialog.SelectModel makeModel(ArrayList list) {
      return new BSelectFromListDialog.SelectModel(list);
   }

   protected TableController makeController() {
      return new BSelectFromListDialog.SelectController();
   }

   protected BTable makeTable(TableModel model) {
      return new BTable(model);
   }

   protected void initHeader() {
   }

   protected void setLastSearch(String value) {
      lastSearch = value;
   }

   public boolean getRemember() {
      rememberSelected = this.rememberCB.isSelected();
      return rememberSelected;
   }

   public boolean getDontAskAgain() {
      dontAskAgainSelected = this.dontAskAgainCB.isSelected();
      return dontAskAgainSelected;
   }

   protected ArrayList makeChoiceArray(ArrayList choices) {
      return choices;
   }

   protected boolean showSearch() {
      return true;
   }

   protected void fillInHeader(BFlowPane header) {
   }

   public String getSearchEntry() {
      return this.searchEntry;
   }

   public void setSearchEntry(String searchEntry) {
      if (this.searchEntryField != null) {
         this.searchEntry = searchEntry;
         this.searchEntryField.setText(searchEntry);
         this.searchUpdate();
      }
   }

   public ArrayList<BComponent> getSelectedResults() {
      return null;
   }

   public int getResult() {
      return this.results;
   }

   public BTable getTable() {
      return this.choicesTable;
   }

   protected void searchUpdate() {
   }

   public void doSave() {
      this.results = 1;
      this.close();
   }

   public void doCancel() {
      this.results = 2;
      this.close();
   }

   private BPane buildButtonPane() {
      BEdgePane edgePane = new BEdgePane();
      BGridPane gPane = new BGridPane(1);
      this.rememberCB = new BCheckBox(lex.getText("templateManager.reuseSelection"));
      this.rememberCB.setVisible(false);
      gPane.add(null, this.rememberCB);
      String slotType = this.isRelation ? lex.getText("slot.relation") : (this.isInput ? lex.getText("slot.input") : lex.getText("slot.output"));
      this.dontAskAgainCB = new BCheckBox(lex.getText("templateManager.dontAskAgain") + ' ' + slotType);
      this.dontAskAgainCB.setVisible(false);
      gPane.add(null, this.dontAskAgainCB);
      edgePane.setTop(gPane);
      BFlowPane pane = new BFlowPane();
      pane.setAlign(BHalign.center);
      pane.add(null, this.makeSaveButton());
      pane.add(null, this.makeCancelButton());
      edgePane.setBottom(pane);
      return edgePane;
   }

   protected BButton makeCancelButton() {
      this.btnCancel = new BButton(new Command(this, lex, "templateManager.cancel"));
      this.linkTo(this.btnCancel, BButton.actionPerformed, cancel);
      return this.btnCancel;
   }

   protected BButton makeSaveButton() {
      this.btnSave = new BButton(new Command(this, lex, "templateManager.save"));
      this.linkTo(this.btnSave, BButton.actionPerformed, save);
      this.btnSave.setEnabled(true);
      return this.btnSave;
   }

   public void close() {
      lastBounds = this.getScreenBounds();
      if (lastBounds.width() < 350.0) {
         lastBounds = new RectGeom(lastBounds.x(), lastBounds.y(), 350.0, lastBounds.height());
      }

      lastSearch = this.getSearchEntry();
      super.close();
   }

   public void doUpdateCommands() {
   }

   private static class BSearchTextField extends BTextField {
      BSearchTextField() {
      }
   }

   class FilterSubscriber extends WidgetSubscriber {
      BWidget owner;

      FilterSubscriber(BWidget owner) {
         this.owner = owner;
      }

      public void mousePressed(BMouseEvent event) {
         BWidget widget = event.getWidget();
         if (widget.equals(BSelectFromListDialog.this.rememberCB)) {
            BSelectFromListDialog.this.dontAskAgainCB.setEnabled(BSelectFromListDialog.this.rememberCB.isSelected());
            if (!BSelectFromListDialog.this.dontAskAgainCB.getEnabled()) {
               BSelectFromListDialog.this.dontAskAgainCB.setSelected(false);
            }
         } else if (widget.equals(BSelectFromListDialog.this.dontAskAgainCB) && !BSelectFromListDialog.this.dontAskAgainCB.isSelected()) {
            BSelectFromListDialog.this.rememberCB.setSelected(false);
         }
      }

      public void keyTyped(BKeyEvent event) {
         if (event.getWidget().equals(BSelectFromListDialog.this.searchEntryField)) {
            BSelectFromListDialog.this.searchEntry = BSelectFromListDialog.this.searchEntryField.getText();
            BSelectFromListDialog.this.searchUpdate();
         }
      }
   }

   protected class SelectController extends TableController {
   }

   protected class SelectModel extends TableModel {
      private ArrayList choices;

      public SelectModel() {
      }

      public SelectModel(ArrayList list) {
         this.choices = list;
      }

      public Object getSubject(int row) {
         return this.choices.get(row);
      }

      public void setSelectChoices(ArrayList choices) {
         this.choices = choices;
      }

      public ArrayList getChoices() {
         return this.choices;
      }

      public int getRowCount() {
         return this.choices.size();
      }

      public int getColumnCount() {
         return 0;
      }

      public String getColumnName(int col) {
         return null;
      }

      public Object getValueAt(int row, int col) {
         return null;
      }

      public boolean isColumnSortable(int col) {
         return col == 0;
      }

      public synchronized void sortByColumn(int col, boolean ascending) {
         if (this.choices != null) {
            if (ascending) {
               Collections.sort(this.choices);
            } else {
               Collections.reverse(this.choices);
            }
         }
      }
   }
}
