package com.tridium.px.editor.sidebars.binding;

import com.tridium.json.JSONObject;
import com.tridium.px.editor.BPxEditorOptions;
import com.tridium.ui.theme.Theme;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.baja.entityIo.json.JsonEntityDecoder;
import javax.baja.fox.BFoxProxySession;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.ViewQuery;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BPxEditor;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.BasicRelation;
import javax.baja.tag.Entity;
import javax.baja.tag.Id;
import javax.baja.tag.Relation;
import javax.baja.tag.Tag;
import javax.baja.tag.Tags;
import javax.baja.tag.util.TagSet;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableSubject;
import javax.baja.ui.table.TableCellRenderer.Cell;

@NiagaraType
public class BNeqlizeOrds extends BOrdChanger {
   public static final Type TYPE = Sys.loadType(BNeqlizeOrds.class);
   private final BPxEditorOptions pxEditorOptions;
   private final BComponent baseComponent;
   private Collection<Relation> baseRelations;
   private final BOrd[] path;
   private final Map<BOrd, BNeqlizeOrds.NeqlizeData> neqlizeDataMap = new HashMap<>();
   private static final int PATH_COLUMN = 3;
   private static final double DEFAULT_MAX_COLUMN_WIDTH = 400.0;
   private static final Logger LOGGER = Logger.getLogger("pxEditor");
   private static final Tags EMPTY_TAGS = new TagSet();
   private static final boolean USE_LABEL = true;
   private static final boolean USE_ICON = true;
   private static final Pattern STARTS_WITH_NEQL = Pattern.compile("^neql:");
   private static final Pattern ENDS_WITH_SINGLE = Pattern.compile("\\|single:(.*)$");
   private static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
      @Override
      public int read() {
         return -1;
      }
   };
   private static final JsonEntityDecoder ENTITY_DECODER = new JsonEntityDecoder(EMPTY_INPUT_STREAM);
   private static final BOrd ENTITY_RPC_ORD = BOrd.make("type:entityIo:EntityRpc");
   private static final String ENTITY_RPC_METHOD = "getEntityInfo";
   private static final Map<String, Boolean> RETRIEVE_RELATIONS_OPTIONS = makeRetrieveRelationsOptions();
   private static final BOrd NEQLIZE_RPC_ORD = BOrd.make("type:tagdictionary:NeqlizeRpc");
   private static final String NEQLIZE_RPC_METHOD = "getIdentifyingTagsRelations";

   @Override
   public Type getType() {
      return TYPE;
   }

   public BNeqlizeOrds(BPxEditor editor, BOrd[] before, BComponent baseComponent) {
      super(editor, before);
      this.baseComponent = baseComponent;
      this.path = new BOrd[before.length];
      this.pxEditorOptions = BPxEditorOptions.make();
      this.buildTopPane();
      this.table.setModel(new BNeqlizeOrds.NeqlizeModel());
      this.table.setController(new BNeqlizeOrds.NeqlizeController());
      this.table.setCellRenderer(new BNeqlizeOrds.NeqlizeRenderer());
   }

   private void buildTopPane() {
      BGridPane gridPane = new BGridPane(2);
      gridPane.setColumnGap(10.0);
      gridPane.setColumnAlign(BHalign.right);
      gridPane.setStretchColumn(1);
      gridPane.add(null, new BButton(new BNeqlizeOrds.OpenOptionsEditorCommand(), true, false));
      gridPane.add(null, new BButton(new BNeqlizeOrds.RefreshAllCommand(), true, false));
      this.edge.setTop(new BBorderPane(gridPane, 10.0, 10.0, 10.0, 10.0));
   }

   @Override
   protected void init() {
      this.totalColumns = 4;

      for (int i = 0; i < this.before.length; i++) {
         BNeqlizeOrds.NeqlizeData data = this.resolveBeforeOrd(this.before[i]);
         this.neqlizeDataMap.put(this.before[i], data);
         if (data.component != null) {
            this.path[i] = data.component.getSlotPathOrd();
         }

         this.selectIfSelectable(i, !this.before[i].toString().trim().startsWith("neql:"));
      }

      this.baseRelations = this.retrieveRelations();
   }

   private BNeqlizeOrds.NeqlizeData resolveBeforeOrd(BOrd beforeOrd) {
      BNeqlizeOrds.NeqlizeData data = new BNeqlizeOrds.NeqlizeData();

      try {
         OrdTarget target = beforeOrd.resolve(this.baseComponent, this.editor.getCurrentContext());
         data.component = getComponent(target);
         if (data.component != null) {
            Slot[] slots = target.getPropertyPathInComponent();
            if (slots == null) {
               Slot slot = target.getSlotInComponent();
               if (slot != null) {
                  slots = new Slot[]{slot};
               }
            }

            data.slots = slots;
            data.viewQuery = target.getViewQuery();
            String beforeOrdString = beforeOrd.toString().trim();
            if (beforeOrdString.startsWith("neql:")) {
               data.userQuery = extractNEQLQuery(beforeOrdString);
            }
         } else {
            data.errorMessage = "neqlizeOrds.resolvedButNotComponentBeforeOrd";
         }
      } catch (Exception var6) {
         if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Error resolving " + beforeOrd + " for NeqlizeOrds dialog", (Throwable)var6);
         } else {
            LOGGER.severe("Error resolving " + beforeOrd + " for NeqlizeOrds dialog");
         }

         data.component = null;
         data.errorMessage = "neqlizeOrds.problemResolvingBeforeOrd";
      }

      return data;
   }

   public boolean refresh(boolean initSelections) throws Exception {
      Optional<Map<String, Object>> result = this.runNeqlizeRpc();
      if (!result.isPresent()) {
         BDialog.error(this.getOwner(), lexicon.getText("neqlizeOrds.conversionError.title"), lexicon.getText("neqlizeOrds.conversionError.message"));
         return false;
      } else {
         JSONObject conversionResults = new JSONObject(result.get());
         if (LOGGER.isLoggable(Level.FINE)) {
            logResults(conversionResults);
         }

         this.processConversionResults(conversionResults, !initSelections);
         this.updateAfterOrds(initSelections);
         return true;
      }
   }

   private void processConversionResults(JSONObject conversionResults, boolean resetUserQuery) {
      for (int i = 0; i < this.before.length; i++) {
         this.processConversionResult(conversionResults, this.before[i], resetUserQuery && this.selected[i]);
      }
   }

   private void processConversionResult(JSONObject conversionResults, BOrd beforeOrd, boolean resetUserQuery) {
      BNeqlizeOrds.NeqlizeData data = this.neqlizeDataMap.get(beforeOrd);
      if (data.component != null) {
         if (resetUserQuery) {
            data.userQuery = null;
         }

         data.queryRelation = null;
         data.queryTags = null;
         JSONObject ordConversionResult = conversionResults.optJSONObject(beforeOrd.toString());
         if (ordConversionResult == null) {
            data.errorMessage = "neqlizeOrds.ordConversionNotFound";
         } else if (ordConversionResult.has("error")) {
            String errorType = ordConversionResult.optString("error");
            switch (errorType) {
               case "noTagSetFound":
                  data.errorMessage = "neqlizeOrds.noTagsFound";
                  return;
               case "notEndpointOrDescendant":
                  data.errorMessage = "neqlizeOrds.notEndpointOrDescendant";
                  return;
               default:
                  data.errorMessage = "neqlizeOrds.ordConversionError";
            }
         } else {
            String relationId = ordConversionResult.optString("relationId");
            boolean relationIsInbound = ordConversionResult.optBoolean("relationIsInbound");
            if (relationId != null && !relationId.isEmpty()) {
               try {
                  BasicRelation ordRelation = new BasicRelation(Id.newId(relationId), beforeOrd, EMPTY_TAGS, relationIsInbound);
                  data.queryRelation = ordRelation;
               } catch (Exception var11) {
                  LOGGER.log(Level.WARNING, "Failed to convert relation ID (" + relationId + ") returned by NeqlizeRpc for " + beforeOrd, (Throwable)var11);
                  data.errorMessage = "neqlizeOrds.ordConversionError";
                  return;
               }
            }

            JSONObject tags = ordConversionResult.optJSONObject("tags");
            if (tags == null) {
               if (data.queryRelation == null) {
                  LOGGER.log(Level.WARNING, "No tags returned by NeqlizeRpc for " + beforeOrd + " when relation is not supplied");
                  data.errorMessage = "neqlizeOrds.noTagsFound";
                  return;
               }
            } else {
               try {
                  data.queryTags = ENTITY_DECODER.decodeTags(tags).getAll();
               } catch (IOException var10) {
                  LOGGER.log(Level.WARNING, "Failed to decode tags returned by NeqlizeRpc", (Throwable)var10);
                  data.queryRelation = null;
                  data.errorMessage = "neqlizeOrds.ordConversionError";
                  return;
               }
            }

            data.errorMessage = null;
            if (beforeOrd.equals(makeAfterOrd(data, false))) {
               data.userQuery = null;
            }
         }
      }
   }

   private void updateAfterOrds(boolean initSelections) {
      for (int i = 0; i < this.before.length; i++) {
         boolean select = initSelections ? !this.before[i].toString().trim().startsWith("neql:") : this.selected[i];
         this.selectIfSelectable(i, select);
         this.updateAfterOrd(i);
      }

      this.repaint();
   }

   private void updateAfterOrd(int index) {
      if (this.selected[index]) {
         this.after[index] = this.after(this.before[index]);
      } else {
         this.after[index] = this.before[index];
      }
   }

   private static BComponent getComponent(OrdTarget target) {
      return target.getSpace() instanceof BComponentSpace ? target.getComponent() : null;
   }

   private static String extractNEQLQuery(String query) {
      return ENDS_WITH_SINGLE.matcher(STARTS_WITH_NEQL.matcher(query).replaceFirst("")).replaceFirst("");
   }

   private static void logResults(JSONObject result) {
      LOGGER.fine("Tag-Based Ord Conversion results (key, value):");
      Iterator<String> keys = result.keys();

      while (keys.hasNext()) {
         String key = keys.next();
         JSONObject ordResult = result.optJSONObject(key);
         if (ordResult != null) {
            LOGGER.fine("  " + key + ", " + ordResult);
         } else {
            LOGGER.fine("  Key = " + key + " is not a JSONObject");
         }
      }
   }

   public BNeqlizeOrds.NeqlizeData getNeqlizeData(BOrd ord) {
      return this.neqlizeDataMap.get(ord);
   }

   private Optional<Map<String, Object>> runNeqlizeRpc() throws Exception {
      if (this.before != null && this.before.length != 0 && this.pxEditorOptions != null) {
         ArrayList<String> boundOrdStrings = new ArrayList<>();

         for (BOrd beforeOrd : this.before) {
            BNeqlizeOrds.NeqlizeData data = this.neqlizeDataMap.get(beforeOrd);
            if (data != null && data.component != null) {
               boundOrdStrings.add(beforeOrd.toString());
            }
         }

         JSONObject options = this.buildOptionsJSON();
         return this.callNeqlizeRpc(this.baseComponent, boundOrdStrings, options);
      } else {
         return Optional.empty();
      }
   }

   private JSONObject buildOptionsJSON() {
      JSONObject options = new JSONObject()
         .put("mode", this.pxEditorOptions.getNeqlizeMode().getTag())
         .put("useServiceExcludedRelations", this.pxEditorOptions.getUseServiceExcludedRelations())
         .put("excludedRelations", this.pxEditorOptions.getNeqlizeExcludedRelations())
         .put("useServiceExcludedTags", this.pxEditorOptions.getUseServiceExcludedTags())
         .put("excludedTags", this.pxEditorOptions.getNeqlizeExcludedTags());
      if (LOGGER.isLoggable(Level.FINE)) {
         LOGGER.fine("Neqlize Options = " + options.toString(2));
      }

      return options;
   }

   public Optional<Map<String, Object>> callNeqlizeRpc(BComponent baseComponent, List<String> boundOrdStrings, JSONObject options) throws Exception {
      BFoxProxySession session = (BFoxProxySession)baseComponent.getSession();
      return session.rpc(
         NEQLIZE_RPC_ORD, "getIdentifyingTagsRelations", new Object[]{baseComponent.getSlotPath().toString(), boundOrdStrings, options.toString()}
      );
   }

   private Collection<Relation> retrieveRelations() {
      BFoxProxySession session = (BFoxProxySession)this.baseComponent.getSession();

      try {
         Optional<String> rpcResponse = session.rpc(
            ENTITY_RPC_ORD, "getEntityInfo", new Object[]{this.baseComponent.getOrdInSession().encodeToString(), RETRIEVE_RELATIONS_OPTIONS}
         );
         if (rpcResponse.isPresent()) {
            Entity entity = JsonEntityDecoder.decodeFromString(rpcResponse.get());
            return entity.relations().getAll();
         }
      } catch (IOException var4) {
         LOGGER.severe("Decoding NeqlizeOrds base component relations retrieval response failed: " + var4.getMessage());
      } catch (Exception var5) {
         LOGGER.severe("NeqlizeOrds base component relations retrieval failed: " + var5.getMessage());
      }

      LOGGER.warning("Could not retrieve relations for the PX view's component");
      BDialog.error(
         this.getOwner(),
         lexicon.getText("neqlizeOrds.baseRelationsRetrievalError.title"),
         lexicon.getText("neqlizeOrds.baseRelationsRetrievalError.message", new Object[]{this.baseComponent.getSlotPath()})
      );
      return Collections.emptyList();
   }

   private boolean isRowValid(int row) {
      return this.getDisplayedErrorForRow(row) == null;
   }

   private String getDisplayedErrorForRow(int row) {
      BNeqlizeOrds.NeqlizeData data = this.neqlizeDataMap.get(this.before[row]);
      return data.errorMessage != null ? lexicon.getText(data.errorMessage) : null;
   }

   public static BOrd makeAfterOrd(BNeqlizeOrds.NeqlizeData data, boolean considerUserQuery) {
      if (data.errorMessage != null) {
         return BOrd.NULL;
      } else {
         String userQuery = considerUserQuery && data.userQuery != null ? data.userQuery.trim() : "";
         BOrd query;
         if (!userQuery.isEmpty()) {
            query = BOrd.make("neql:" + userQuery);
         } else {
            String neqlQuery = BNeqlizeOrdEditor.makeNeqlizeQuery(data.queryRelation, data.queryTags);
            if (neqlQuery == null || neqlQuery.isEmpty()) {
               return null;
            }

            query = BOrd.make("neql:" + neqlQuery);
         }

         return BOrd.make(query, getSuffix(data));
      }
   }

   public static BOrd getSuffix(BNeqlizeOrds.NeqlizeData data) {
      BOrd suffix = BOrd.make("single:");
      if (data.slots != null && data.slots.length > 0) {
         StringJoiner sj = new StringJoiner("/", "slot:", "");

         for (Slot slot : data.slots) {
            sj.add(slot.getName());
         }

         suffix = BOrd.make(suffix, sj.toString());
      }

      if (data.viewQuery != null) {
         suffix = BOrd.make(suffix, data.viewQuery.toString());
      }

      return suffix;
   }

   @Override
   protected BOrd after(BOrd oldOrd) {
      BNeqlizeOrds.NeqlizeData data = this.neqlizeDataMap.get(oldOrd);
      BOrd newOrd = makeAfterOrd(data, true);
      return newOrd != null && newOrd != BOrd.NULL ? newOrd : oldOrd;
   }

   @Override
   protected boolean selectable(int row) {
      return this.isRowValid(row);
   }

   protected boolean canEditRow(int row) {
      BNeqlizeOrds.NeqlizeData data = this.neqlizeDataMap.get(this.before[row]);
      return data.component != null;
   }

   private BNeqlizeOrds.NeqlizeData resolveAndValidateAfterOrd(BNeqlizeOrds.NeqlizeData data) {
      data.errorMessage = null;
      BOrd afterOrd = makeAfterOrd(data, true);
      if (afterOrd != null && !afterOrd.equals(BOrd.NULL)) {
         try {
            OrdTarget target = afterOrd.resolve(this.baseComponent, this.editor.getCurrentContext());
            BComponent afterComponent = getComponent(target);
            BComponent beforeComponent = data.component;
            if (afterComponent == null) {
               LOGGER.warning("After Ord " + afterOrd + " did not resolve to a component");
               data.errorMessage = "neqlizeOrds.afterResolutionValueNotMatchBefore";
            } else if (!afterComponent.equals(beforeComponent)) {
               LOGGER.warning(
                  "After Ord "
                     + afterOrd
                     + " resolved to "
                     + afterComponent.getSlotPath()
                     + " instead of Before Ord Component "
                     + beforeComponent.getSlotPath()
               );
               data.errorMessage = "neqlizeOrds.afterResolutionValueNotMatchBefore";
            }
         } catch (Exception var6) {
            data.errorMessage = "neqlizeOrds.problemResolvingAfterOrd";
            if (LOGGER.isLoggable(Level.FINE)) {
               LOGGER.log(Level.FINE, "Exception resolving edited After Ord: " + afterOrd, (Throwable)var6);
            } else {
               LOGGER.warning("Exception resolving edited After Ord: " + afterOrd);
            }
         }

         return data;
      } else {
         data.errorMessage = "neqlizeOrds.problemResolvingAfterOrd";
         LOGGER.warning("After Ord " + afterOrd + " is null");
         return data;
      }
   }

   private static Map<String, Boolean> makeRetrieveRelationsOptions() {
      Map<String, Boolean> options = new HashMap<>();
      options.put("jsonEntityEncoderShouldEncodeRelations", true);
      options.put("jsonEntityEncoderShouldEncodeTags", false);
      return options;
   }

   private class EditOrdCommand extends Command {
      private final int row;

      EditOrdCommand(BTable table, int row) {
         super(table, BOrdChanger.lexicon, "neqlizeOrds.editNeqlOrd");
         this.setEnabled(BNeqlizeOrds.this.canEditRow(row));
         this.row = row;
      }

      public CommandArtifact doInvoke() {
         BNeqlizeOrdEditor ordEditor = new BNeqlizeOrdEditor();
         BNeqlizeOrds.NeqlizeData data = BNeqlizeOrds.this.neqlizeDataMap.get(BNeqlizeOrds.this.before[this.row]);
         String userQuery = data.userQuery;
         ordEditor.init(data.component, BNeqlizeOrds.this.baseRelations, data.queryRelation, data.queryTags, userQuery);
         int result = BDialog.open(this.getShell(), BOrdChanger.lexicon.getText("neqlizeOrds.editNeqlOrd.title"), ordEditor, 3);
         if (result == 1) {
            data.userQuery = ordEditor.getUserQuery();
            data.queryRelation = ordEditor.getQueryRelation();
            data.queryTags = ordEditor.getQueryTags();
            BNeqlizeOrds.this.neqlizeDataMap.put(BNeqlizeOrds.this.before[this.row], BNeqlizeOrds.this.resolveAndValidateAfterOrd(data));
            BNeqlizeOrds.this.after[this.row] = BNeqlizeOrds.this.after(BNeqlizeOrds.this.before[this.row]);
            BNeqlizeOrds.this.selectIfSelectable(this.row, data.errorMessage == null);
            BNeqlizeOrds.this.repaint();
         }

         return null;
      }
   }

   private class NeqlizeController extends BOrdChanger.Controller {
      private NeqlizeController() {
      }

      protected BMenu makePopup(TableSubject subject) {
         BMenu menu = new BMenu();
         menu.add("editOrd", BNeqlizeOrds.this.new EditOrdCommand(BNeqlizeOrds.this.table, subject.getActiveRow()));
         menu.add("refreshRow", BNeqlizeOrds.this.new RefreshRowCommand(BNeqlizeOrds.this.table, subject.getActiveRow()));
         return menu;
      }
   }

   public static class NeqlizeData {
      public BComponent component;
      public Collection<Tag> queryTags;
      public Relation queryRelation;
      public Slot[] slots;
      public ViewQuery viewQuery;
      public String userQuery;
      public String errorMessage;
   }

   private class NeqlizeModel extends BOrdChanger.Model {
      private NeqlizeModel() {
      }

      @Override
      public String getColumnName(int col) {
         return col == 3 ? BOrdChanger.text("neqlizeOrds.path") : super.getColumnName(col);
      }

      @Override
      public Object getValueAt(int row, int col) {
         if (col == 3) {
            String rowError = BNeqlizeOrds.this.getDisplayedErrorForRow(row);
            return rowError != null ? rowError : BNeqlizeOrds.this.path[row];
         } else {
            return super.getValueAt(row, col);
         }
      }
   }

   private class NeqlizeRenderer extends BOrdChanger.Renderer {
      private NeqlizeRenderer() {
      }

      @Override
      public double getPreferredCellWidth(Cell cell) {
         if (cell.column == BNeqlizeOrds.this.afterColumn) {
            return this.getWidthForText(BNeqlizeOrds.this.after[cell.row].toString());
         } else if (cell.column == BNeqlizeOrds.this.beforeColumn) {
            return this.getWidthForText(BNeqlizeOrds.this.before[cell.row].toString());
         } else {
            return cell.column == 3
               ? this.getWidthForText(BNeqlizeOrds.this.table.getModel().getValueAt(cell.row, cell.column).toString())
               : super.getPreferredCellWidth(cell);
         }
      }

      private double getWidthForText(String text) {
         double width = Theme.table().getCellFont().width(text) + 12.0;
         return Math.min(400.0, width);
      }
   }

   private class OpenOptionsEditorCommand extends Command {
      public OpenOptionsEditorCommand() {
         super(BNeqlizeOrds.this, BNeqlizeOrds.TYPE.getModule(), "neqlizeOrds.options");
      }

      public CommandArtifact doInvoke() throws Exception {
         BNeqlizeOptionsEditor.open(BNeqlizeOrds.this, BNeqlizeOrds.this.pxEditorOptions, (BFoxProxySession)BNeqlizeOrds.this.baseComponent.getSession());
         return null;
      }
   }

   private class RefreshAllCommand extends Command {
      public RefreshAllCommand() {
         super(BNeqlizeOrds.this, BNeqlizeOrds.TYPE.getModule(), "neqlizeOrds.refresh");
      }

      public CommandArtifact doInvoke() {
         try {
            if (BNeqlizeOrds.this.refresh(false)) {
               BNeqlizeOrds.this.repaint();
            }

            return null;
         } catch (Exception var2) {
            throw new BajaRuntimeException(BOrdChanger.lexicon.getText("boundOrds.conversionFailed"), var2);
         }
      }
   }

   private class RefreshRowCommand extends Command {
      private final int row;

      RefreshRowCommand(BTable table, int row) {
         super(table, BOrdChanger.lexicon, "neqlizeOrds.refreshRow");
         this.row = row;
         this.setEnabled(BNeqlizeOrds.this.canEditRow(row));
      }

      public CommandArtifact doInvoke() throws Exception {
         BOrd beforeOrd = BNeqlizeOrds.this.before[this.row];
         List<String> ordStrings = Collections.singletonList(beforeOrd.toString());
         JSONObject options = BNeqlizeOrds.this.buildOptionsJSON();
         Optional<Map<String, Object>> neqlizeRpcResult = BNeqlizeOrds.this.callNeqlizeRpc(BNeqlizeOrds.this.baseComponent, ordStrings, options);
         if (!neqlizeRpcResult.isPresent()) {
            BDialog.error(
               this.getOwner(),
               BOrdChanger.lexicon.getText("boundOrds.conversionError.title"),
               BOrdChanger.lexicon.getText("boundOrds.conversionError.message")
            );
            return null;
         } else {
            JSONObject conversionResults = new JSONObject(neqlizeRpcResult.get());
            if (BNeqlizeOrds.LOGGER.isLoggable(Level.FINE)) {
               BNeqlizeOrds.logResults(conversionResults);
            }

            BNeqlizeOrds.this.processConversionResult(conversionResults, beforeOrd, true);
            BNeqlizeOrds.NeqlizeData data = BNeqlizeOrds.this.neqlizeDataMap.get(beforeOrd);
            BNeqlizeOrds.this.selectIfSelectable(this.row, data.errorMessage == null);
            BNeqlizeOrds.this.updateAfterOrd(this.row);
            BNeqlizeOrds.this.repaint();
            return null;
         }
      }
   }
}
