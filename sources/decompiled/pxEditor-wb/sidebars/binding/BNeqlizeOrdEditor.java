package com.tridium.px.editor.sidebars.binding;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.Reflector;
import com.tridium.ui.theme.Theme;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Logger;
import javax.baja.data.BIDataValue;
import javax.baja.entityIo.json.JsonEntityDecoder;
import javax.baja.fox.BFoxProxySession;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.gx.Graphics;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BMarker;
import javax.baja.sys.BNumber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.Relation;
import javax.baja.tag.Tag;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BBorder;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BMenu;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "editDirectlyChanged"
   ), @NiagaraAction(
      name = "openQueryFieldMenu",
      parameterType = "BMouseEvent",
      defaultValue = "new BMouseEvent()"
   )})
public class BNeqlizeOrdEditor extends BDialog {
   public static final Action editDirectlyChanged = newAction(0, null);
   public static final Action openQueryFieldMenu = newAction(0, new BMouseEvent(), null);
   public static final Type TYPE = Sys.loadType(BNeqlizeOrdEditor.class);
   private static final Comparator<Relation> RELATION_COMPARATOR = new BNeqlizeOrdEditor.RelationComparator();
   private static final Comparator<Tag> TAG_COMPARATOR = new BNeqlizeOrdEditor.TagComparator();
   private static final BImage CHECK_MARK = BPxEditorPane.icon("boundOrds.ok");
   private static final int CHECK_MARK_HEIGHT = 16;
   private static final double CENTER_PANE_DIVIDER_POSITION = 40.0;
   private static final double CENTER_BORDER_WIDTH = 1.0;
   private static final double CENTER_PADDING = 5.0;
   private static final double CENTER_TOP_MARGIN = 5.0;
   private static final int CHECK_MARK_COL = 0;
   private static final int ID_COL = 1;
   private static final int VALUE_COL = 2;
   private static final int MAX_AUTO_DIALOG_HEIGHT = 450;
   private static final int MIN_AUTO_DIALOG_HEIGHT = 225;
   private static final int MAX_AUTO_DIALOG_WIDTH = 750;
   private static final int MIN_AUTO_DIALOG_WIDTH = 450;
   private static final Tag[] EMPTY_TAG_ARRAY = new Tag[0];
   private final BTextField queryTextField = new BTextField();
   private final BCheckBox editDirectly = new BCheckBox(BPxEditorPane.lexicon().getText("neqlizeOrdEditor.editDirectly"));
   private BTable relationsTable = null;
   private BTable tagsTable = null;
   private boolean[] selectedTags = null;
   private int selectedRelation = -1;
   private String userQuery = "";
   private List<Tag> tags;
   private List<Relation> relations;
   private final BNeqlizeOrdEditor.ResetQueryCommand resetQueryCommand = new BNeqlizeOrdEditor.ResetQueryCommand();
   private static final BOrd ENTITY_RPC_ORD = BOrd.make("type:entityIo:EntityRpc");
   private static final String ENTITY_RPC_METHOD = "getEntityInfo";
   private static final Map<String, Boolean> RETRIEVE_TAGS_OPTIONS = makeRetrieveTagsOptions();
   private static final Lexicon LEXICON = Lexicon.make("pxEditor");
   private static final Logger LOGGER = Logger.getLogger("pxEditor");

   public void editDirectlyChanged() {
      this.invoke(editDirectlyChanged, null, null);
   }

   public void openQueryFieldMenu(BMouseEvent parameter) {
      this.invoke(openQueryFieldMenu, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void init(BComponent component, Collection<Relation> baseRelations, Relation queryRelation, Collection<Tag> queryTags, String userQuery) {
      this.tags = this.retrieveTags(component);
      this.tags.sort(TAG_COMPARATOR);
      this.relations = processRelations(component, baseRelations);
      this.relations.sort(RELATION_COMPARATOR);
      this.initSelectedRelation(queryRelation);
      this.initSelectedTags(queryTags);
      BEdgePane mainPane = new BEdgePane();
      mainPane.setTop(this.makeTop());
      mainPane.setCenter(this.makeCenter());
      BConstrainedPane constrainedMainPane = new BConstrainedPane(mainPane);
      constrainedMainPane.setMaxHeight(450.0);
      constrainedMainPane.setMinHeight(225.0);
      constrainedMainPane.setMaxWidth(750.0);
      constrainedMainPane.setMinWidth(450.0);
      this.setContent(constrainedMainPane);
      this.linkTo(this.editDirectly, BAbstractButton.actionPerformed, editDirectlyChanged);
      if (this.tags.isEmpty() && this.relations.isEmpty()) {
         this.editDirectly.setSelected(true);
         this.editDirectly.setEnabled(false);
      } else {
         this.editDirectly.setSelected(userQuery != null);
      }

      this.doEditDirectlyChanged();
      if (userQuery != null) {
         this.queryTextField.setText(userQuery);
         this.userQuery = userQuery;
      } else {
         this.updateQueryTextField();
         this.userQuery = null;
      }
   }

   private static List<Relation> processRelations(BComponent component, Collection<Relation> baseRelations) {
      List<Relation> relations = new ArrayList<>();
      BOrd componentOrd = component.getOrdToEntity().orElse(BOrd.NULL).relativizeToSession();

      for (Relation relation : baseRelations) {
         if (componentOrd.equals(relation.getEndpointOrd().relativizeToSession())) {
            relations.add(relation);
         }
      }

      return relations;
   }

   private List<Tag> retrieveTags(BComponent component) {
      BFoxProxySession session = (BFoxProxySession)component.getSession();

      try {
         Optional<String> response = session.rpc(
            ENTITY_RPC_ORD, "getEntityInfo", new Object[]{component.getOrdInSession().encodeToString(), RETRIEVE_TAGS_OPTIONS}
         );
         if (response.isPresent()) {
            Entity entity = JsonEntityDecoder.decodeFromString(response.get());
            return new ArrayList<>(entity.tags().getAll());
         }
      } catch (IOException var5) {
         LOGGER.severe("Decoding NeqlizeOrdEditor tags retrieval response failed: " + var5.getMessage());
      } catch (Exception var6) {
         LOGGER.severe("NeqlizeOrdEditor tags retrieval failed: " + var6.getMessage());
      }

      LOGGER.warning("Could not retrieve tags for component " + component.getSlotPath());
      BDialog.error(
         this.getOwner(),
         LEXICON.getText("neqlizeOrdEditor.tagsRetrievalError.title"),
         LEXICON.getText("neqlizeOrdEditor.tagsRetrievalError.message", new Object[]{component.getSlotPath()})
      );
      return Collections.emptyList();
   }

   private void initSelectedRelation(Relation queryRelation) {
      if (queryRelation != null) {
         for (int i = 0; i < this.relations.size(); i++) {
            Relation relation = this.relations.get(i);
            if (relation.getId().equals(queryRelation.getId()) && relation.isInbound() == queryRelation.isInbound()) {
               this.selectedRelation = i;
               break;
            }
         }
      }
   }

   private void initSelectedTags(Collection<Tag> queryTags) {
      this.selectedTags = new boolean[this.tags.size()];
      if (queryTags != null) {
         for (int i = 0; i < this.tags.size(); i++) {
            this.selectedTags[i] = queryTags.contains(this.tags.get(i));
         }
      }
   }

   private BWidget makeTop() {
      BGridPane gridPane = new BGridPane(2);
      gridPane.add(null, new BLabel(BPxEditorPane.lexicon().getText("neqlizeOrdEditor.resultingOrd.label"), BHalign.left));
      gridPane.add(null, this.editDirectly);
      gridPane.setHalign(BHalign.fill);
      gridPane.setStretchColumn(0);
      BEdgePane edgePane = new BEdgePane();
      edgePane.setTop(gridPane);
      edgePane.setCenter(this.queryTextField);
      this.linkTo(this.queryTextField, BWidget.mouseEvent, openQueryFieldMenu);
      return edgePane;
   }

   private BWidget makeCenter() {
      BSplitPane splitPane = new BSplitPane();
      splitPane.setWidget1(this.makeCenterLeft());
      splitPane.setWidget2(this.makeCenterRight());
      splitPane.setDividerPosition(40.0);
      BBorderPane borderPane = new BBorderPane(splitPane);
      borderPane.setBorder(BBorder.make(1.0, 1, BColor.black.toBrush()));
      borderPane.setPadding(BInsets.make(5.0));
      borderPane.setMargin(BInsets.make(5.0, 0.0, 0.0, 0.0));
      return borderPane;
   }

   private BWidget makeCenterLeft() {
      BEdgePane edgePane = new BEdgePane();
      edgePane.setTop(new BLabel(BPxEditorPane.lexicon().getText("neqlizeOrdEditor.relations"), BHalign.left));
      edgePane.setCenter(this.makeRelationsTable());
      return edgePane;
   }

   private BWidget makeCenterRight() {
      BEdgePane edgePane = new BEdgePane();
      edgePane.setTop(new BLabel(BPxEditorPane.lexicon().getText("neqlizeOrdEditor.tags"), BHalign.left));
      edgePane.setCenter(this.makeTagsTable());
      return edgePane;
   }

   private BTable makeRelationsTable() {
      this.relationsTable = new BTable(new BNeqlizeOrdEditor.RelationsTableModel());
      this.relationsTable.sizeColumnsToFit();
      this.relationsTable.setMultipleSelection(false);
      this.relationsTable.setController(new BNeqlizeOrdEditor.RelationsTableController());
      this.relationsTable.setCellRenderer(new BNeqlizeOrdEditor.RelationsTableCellRenderer());
      this.relationsTable.computePreferredSize();
      return this.relationsTable;
   }

   private BTable makeTagsTable() {
      this.tagsTable = new BTable(new BNeqlizeOrdEditor.TagsTableModel());
      this.tagsTable.sizeColumnsToFit();
      this.tagsTable.setMultipleSelection(true);
      this.tagsTable.setController(new BNeqlizeOrdEditor.TagsTableController());
      this.tagsTable.setCellRenderer(new BNeqlizeOrdEditor.TagsTableCellRenderer());
      this.tagsTable.computePreferredSize();
      return this.tagsTable;
   }

   public void doEditDirectlyChanged() {
      if (this.editDirectly.isSelected()) {
         this.userQuery = this.userQuery != null && !this.userQuery.trim().isEmpty() ? this.userQuery : this.queryTextField.getText();
         this.queryTextField.setText(this.userQuery);
         this.queryTextField.setEditable(true);
         this.relationsTable.setEnabled(false);
         this.tagsTable.setEnabled(false);
      } else {
         this.userQuery = this.queryTextField.getText();
         this.queryTextField.setEditable(false);
         this.updateQueryTextField();
         if (this.userQuery.equals(this.queryTextField.getText())) {
            this.userQuery = null;
         }

         this.relationsTable.setEnabled(!this.relations.isEmpty());
         this.tagsTable.setEnabled(!this.tags.isEmpty());
      }
   }

   public void doOpenQueryFieldMenu(BMouseEvent event) {
      if (this.editDirectly.isSelected() && event.isPopupTrigger()) {
         BMenu menu = new BMenu();
         menu.add("resetQuery", this.resetQueryCommand);
         menu.open(event);
      }
   }

   private void updateQueryTextField() {
      if (!this.editDirectly.isSelected()) {
         this.queryTextField.setText(this.getGeneratedQuery());
      }
   }

   private String getGeneratedQuery() {
      return makeNeqlizeQuery(this.getQueryRelation(), this.getQueryTags());
   }

   public static String makeNeqlizeQuery(Relation relation, Collection<Tag> tags) {
      String query = "";
      if (relation != null) {
         query = query + "traverse " + relation.getId() + (relation.isInbound() ? "<-" : "->");
      }

      if (tags != null && !tags.isEmpty()) {
         if (!query.isEmpty()) {
            query = query + " where ";
         }

         Tag[] sortedTags = tags.toArray(EMPTY_TAG_ARRAY);
         Arrays.sort(sortedTags, TAG_COMPARATOR);
         StringJoiner joiner = new StringJoiner(" and ");

         for (Tag tag : sortedTags) {
            BIDataValue value = tag.getValue();
            if (value instanceof BMarker) {
               joiner.add(tag.getId().toString());
            } else {
               try {
                  joiner.add(tag.getId() + " = " + (value instanceof BNumber ? value.encodeToString() : '"' + value.encodeToString() + '"'));
               } catch (IOException var11) {
               }
            }
         }

         query = query + joiner.toString();
      }

      return query;
   }

   private static Map<String, Boolean> makeRetrieveTagsOptions() {
      Map<String, Boolean> options = new HashMap<>();
      options.put("jsonEntityEncoderShouldEncodeRelations", false);
      options.put("jsonEntityEncoderShouldEncodeTags", true);
      return options;
   }

   public Relation getQueryRelation() {
      return this.selectedRelation != -1 ? this.relations.get(this.selectedRelation) : null;
   }

   public List<Tag> getQueryTags() {
      List<Tag> queryTags = new ArrayList<>();

      for (int i = 0; i < this.selectedTags.length; i++) {
         if (this.selectedTags[i]) {
            queryTags.add(this.tags.get(i));
         }
      }

      return queryTags;
   }

   public String getUserQuery() {
      return this.editDirectly.isSelected() ? this.queryTextField.getText() : null;
   }

   private static class RelationComparator implements Comparator<Relation> {
      private RelationComparator() {
      }

      public int compare(Relation o1, Relation o2) {
         int idComparison = o1.getId().compareTo(o2.getId());
         return idComparison != 0 ? idComparison : Boolean.compare(o1.isInbound(), o2.isInbound());
      }
   }

   private class RelationsTableCellRenderer extends TableCellRenderer {
      private RelationsTableCellRenderer() {
      }

      public BBrush getSelectionForeground(Cell cell) {
         return this.getForeground(cell);
      }

      public BBrush getSelectionBackground(Cell cell) {
         return this.getBackground(cell);
      }

      public BBrush getForeground(Cell cell) {
         return BNeqlizeOrdEditor.this.selectedRelation == cell.row ? super.getForeground(cell) : Theme.widget().getControlShadow();
      }

      public void paintCell(Graphics g, Cell cell) {
         if (cell.column == 0) {
            this.paintCellBackground(g, cell);
            double x = 2.0;
            double y = (cell.height - 16.0) / 2.0;
            g.drawImage(BNeqlizeOrdEditor.this.selectedRelation == cell.row ? BNeqlizeOrdEditor.CHECK_MARK : BImage.NULL, x, y);
         } else {
            super.paintCell(g, cell);
         }
      }

      public double getPreferredCellWidth(Cell cell) {
         return cell.column == 0 ? 20.0 : super.getPreferredCellWidth(cell);
      }
   }

   private class RelationsTableController extends TableController {
      private RelationsTableController() {
      }

      protected void cellPressed(BMouseEvent event, int row, int column) {
         BNeqlizeOrdEditor.this.selectedRelation = BNeqlizeOrdEditor.this.selectedRelation != row ? row : -1;
         BNeqlizeOrdEditor.this.repaint();
         BNeqlizeOrdEditor.this.updateQueryTextField();
      }

      protected BMenu makeOptionsMenu() {
         return Reflector.optionsMenu(this.getTable());
      }
   }

   private class RelationsTableModel extends TableModel {
      private RelationsTableModel() {
      }

      public int getRowCount() {
         return BNeqlizeOrdEditor.this.relations.size();
      }

      public int getColumnCount() {
         return 2;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return "  ";
            case 1:
               return BPxEditorPane.lexicon().getText("neqlizeOrdEditor.relationId");
            default:
               return "";
         }
      }

      public Object getValueAt(int row, int col) {
         if (col == 1) {
            Relation relation = BNeqlizeOrdEditor.this.relations.get(row);
            String direction = relation.isInbound() ? "<-" : "->";
            return relation.getId() + direction;
         } else {
            return "";
         }
      }

      public Object getSubject(int row) {
         return BNeqlizeOrdEditor.this.relations.get(row);
      }
   }

   private class ResetQueryCommand extends Command {
      public ResetQueryCommand() {
         super(BNeqlizeOrdEditor.this.queryTextField, BPxEditorPane.lexicon(), "neqlizeOrdEditor.resetQuery");
      }

      public CommandArtifact doInvoke() {
         BNeqlizeOrdEditor.this.queryTextField.setText(BNeqlizeOrdEditor.this.getGeneratedQuery());
         return null;
      }
   }

   private static class TagComparator implements Comparator<Tag> {
      private TagComparator() {
      }

      public int compare(Tag o1, Tag o2) {
         return o1.getId().compareTo(o2.getId());
      }
   }

   private class TagsTableCellRenderer extends TableCellRenderer {
      private TagsTableCellRenderer() {
      }

      public BBrush getSelectionForeground(Cell cell) {
         return this.getForeground(cell);
      }

      public BBrush getSelectionBackground(Cell cell) {
         return this.getBackground(cell);
      }

      public BBrush getForeground(Cell cell) {
         return BNeqlizeOrdEditor.this.selectedTags[cell.row] ? super.getForeground(cell) : Theme.widget().getControlShadow();
      }

      public void paintCell(Graphics g, Cell cell) {
         if (cell.column == 0) {
            this.paintCellBackground(g, cell);
            double x = 2.0;
            double y = (cell.height - 16.0) / 2.0;
            g.drawImage(BNeqlizeOrdEditor.this.selectedTags[cell.row] ? BNeqlizeOrdEditor.CHECK_MARK : BImage.NULL, x, y);
         } else {
            super.paintCell(g, cell);
         }
      }

      public double getPreferredCellWidth(Cell cell) {
         return cell.column == 0 ? 20.0 : super.getPreferredCellWidth(cell);
      }
   }

   private class TagsTableController extends TableController {
      private TagsTableController() {
      }

      protected void cellPressed(BMouseEvent event, int row, int column) {
         if (column == 0) {
            BNeqlizeOrdEditor.this.selectedTags[row] = !BNeqlizeOrdEditor.this.selectedTags[row];
            BNeqlizeOrdEditor.this.repaint();
            BNeqlizeOrdEditor.this.updateQueryTextField();
         }
      }

      protected BMenu makeOptionsMenu() {
         return Reflector.optionsMenu(this.getTable());
      }
   }

   private class TagsTableModel extends TableModel {
      private TagsTableModel() {
      }

      public int getRowCount() {
         return BNeqlizeOrdEditor.this.tags.size();
      }

      public int getColumnCount() {
         return 3;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return "  ";
            case 1:
               return BPxEditorPane.lexicon().getText("neqlizeOrdEditor.id");
            case 2:
               return BPxEditorPane.lexicon().getText("neqlizeOrdEditor.value");
            default:
               return "";
         }
      }

      public Object getValueAt(int row, int col) {
         switch (col) {
            case 1:
               return BNeqlizeOrdEditor.this.tags.get(row).getId();
            case 2:
               BIDataValue value = BNeqlizeOrdEditor.this.tags.get(row).getValue();
               return value instanceof BMarker ? "" : value;
            default:
               return "";
         }
      }

      public Object getSubject(int row) {
         return BNeqlizeOrdEditor.this.tags.get(row);
      }
   }
}
